import fs from "node:fs/promises";
import path from "node:path";
import { performance } from "node:perf_hooks";

const BASE_URL = process.env.PAPERPILOT_BASE_URL || "http://localhost:8080";
const ADMIN_USER_ID = process.env.PAPERPILOT_ADMIN_USER_ID || "6";
const DURATION_MS = Number(process.env.PAPERPILOT_BENCH_DURATION_MS || 60_000);
const STEP_MS = Number(process.env.PAPERPILOT_BENCH_STEP_MS || 15_000);
const MAX_CONCURRENCY = Number(process.env.PAPERPILOT_BENCH_MAX_CONCURRENCY || 6);
const MAX_REQUESTS_PER_TARGET = Number(process.env.PAPERPILOT_BENCH_MAX_REQUESTS || 120);
const TIMEOUT_MS = Number(process.env.PAPERPILOT_BENCH_TIMEOUT_MS || 90_000);
const PROBE_LIMIT = Number(process.env.PAPERPILOT_BENCH_PROBE_LIMIT || 12);

const scenes = [
  { id: "paper_review", name: "文献综述", prompt: "论文主题：AI辅助论文阅读。请用JSON返回两个中文要点，30字内。" },
  { id: "paper_qa", name: "AI研读问答", prompt: "用户问：这篇论文的研究问题是什么？请用一句中文回答。" },
  { id: "meeting_deck", name: "PPT生成/组会汇报", prompt: "meeting report deck agent: 请输出3页PPT大纲JSON，每页标题不超过8字。" },
  { id: "forum_moderation", name: "论坛审核", prompt: "审核帖子：分享一篇机器学习论文的实验复现过程。只返回 approved 或 rejected。" },
  { id: "topic_research", name: "选题调研", prompt: "deep-research 选题调研：请给出一个AI教育方向可执行选题，JSON格式。" },
];

const translateProbeText = "This paper proposes a lightweight method for academic reading and evidence-based review.";

function headers(extra = {}) {
  return {
    "Content-Type": "application/json",
    "X-PaperPilot-User-Id": ADMIN_USER_ID,
    ...extra,
  };
}

async function requestJson(url, options = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), options.timeoutMs || TIMEOUT_MS);
  const started = performance.now();
  try {
    const response = await fetch(url, { ...options, signal: controller.signal });
    const text = await response.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = { raw: text }; }
    const latencyMs = Math.round(performance.now() - started);
    if (!response.ok) {
      return { ok: false, status: response.status, latencyMs, error: data?.message || data?.error || text || `HTTP ${response.status}`, data };
    }
    return { ok: true, status: response.status, latencyMs, data };
  } catch (error) {
    return { ok: false, status: 0, latencyMs: Math.round(performance.now() - started), error: error.name === "AbortError" ? "timeout" : error.message };
  } finally {
    clearTimeout(timer);
  }
}

async function getPool(scene) {
  const result = await requestJson(`${BASE_URL}/api/admin/model-config/pool?scene=${encodeURIComponent(scene)}`, {
    method: "GET",
    headers: headers(),
  });
  if (!result.ok || !Array.isArray(result.data)) return [];
  return result.data
    .filter(row => !row.template && row.id && row.modelName && row.keyConfigured !== false)
    .map(row => ({
      id: row.id,
      providerName: row.providerName,
      baseUrl: row.baseUrl,
      modelName: row.modelName,
      apiFormat: row.apiFormat || "openai_chat",
      authType: row.authType || "bearer",
      active: Boolean(row.active),
      status: row.status || "unknown",
      latencyMs: row.latencyMs ?? null,
    }));
}

async function testModelRoute(route) {
  const result = await requestJson(
    `${BASE_URL}/api/admin/model-config/pool/${route.id}/test-model?modelName=${encodeURIComponent(route.modelName)}`,
    { method: "POST", headers: headers() },
  );
  const message = String(result.data?.message || result.error || "");
  const available = Boolean(
    result.ok
    && result.data?.success !== false
    && message.includes("可用")
    && !/HTTP\s*(4|5)\d\d|超时|timeout|为空|失败|不可用|end of life|gone/i.test(message)
  );
  return {
    ...route,
    ok: available,
    latencyMs: result.latencyMs,
    status: result.status,
    error: message,
    raw: result.data,
  };
}

async function testTranslateProvider(provider) {
  const result = await requestJson(`${BASE_URL}/api/translate`, {
    method: "POST",
    headers: headers(),
    body: JSON.stringify({
      provider: provider.id,
      text: translateProbeText,
      sourceLang: "en",
      targetLang: "zh-CN",
    }),
    timeoutMs: 45_000,
  });
  return {
    ...provider,
    ok: Boolean(result.ok && result.data?.translatedText),
    latencyMs: result.latencyMs,
    status: result.status,
    error: result.ok ? "" : result.error,
  };
}

async function runOnce(target) {
  if (target.kind === "model") {
    return testModelRoute(target.route);
  }
  return testTranslateProvider(target.provider);
}

async function runStep(target, concurrency, durationMs, requestCap) {
  const results = [];
  const endAt = Date.now() + durationMs;
  let issued = 0;
  let active = 0;

  await new Promise(resolve => {
    const pump = () => {
      while (active < concurrency && Date.now() < endAt && issued < requestCap) {
        active++;
        issued++;
        runOnce(target)
          .then(result => results.push(result))
          .catch(error => results.push({ ok: false, latencyMs: 0, error: error.message }))
          .finally(() => {
            active--;
            if ((Date.now() >= endAt || issued >= requestCap) && active === 0) {
              resolve();
            } else {
              pump();
            }
          });
      }
      if ((Date.now() >= endAt || issued >= requestCap) && active === 0) resolve();
    };
    pump();
  });

  return summarizeResults(results, durationMs, concurrency);
}

function percentile(values, p) {
  if (!values.length) return null;
  const sorted = [...values].sort((a, b) => a - b);
  const index = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
  return sorted[index];
}

function summarizeResults(results, durationMs, concurrency) {
  const ok = results.filter(item => item.ok);
  const failed = results.filter(item => !item.ok);
  const latencies = ok.map(item => Number(item.latencyMs || 0)).filter(Boolean);
  const successRate = results.length ? ok.length / results.length : 0;
  return {
    concurrency,
    requests: results.length,
    success: ok.length,
    failed: failed.length,
    successRate: Number((successRate * 100).toFixed(1)),
    observedRpm: Number((ok.length * 60_000 / durationMs).toFixed(1)),
    avgLatencyMs: latencies.length ? Math.round(latencies.reduce((a, b) => a + b, 0) / latencies.length) : null,
    p50LatencyMs: percentile(latencies, 50),
    p95LatencyMs: percentile(latencies, 95),
    topErrors: topErrors(failed),
  };
}

function topErrors(items) {
  const counts = new Map();
  for (const item of items) {
    const key = String(item.error || item.raw?.message || "unknown").slice(0, 120);
    counts.set(key, (counts.get(key) || 0) + 1);
  }
  return [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, 3).map(([message, count]) => ({ message, count }));
}

async function capacityTest(target) {
  const steps = [];
  const levels = [1, 2, 4, MAX_CONCURRENCY].filter((v, i, arr) => v <= MAX_CONCURRENCY && arr.indexOf(v) === i);
  const stepDuration = Math.max(5_000, Math.floor(DURATION_MS / levels.length));
  let remainingCap = MAX_REQUESTS_PER_TARGET;
  for (const concurrency of levels) {
    if (remainingCap <= 0) break;
    const summary = await runStep(target, concurrency, stepDuration, remainingCap);
    steps.push(summary);
    remainingCap -= summary.requests;
    if (summary.requests > 0 && summary.successRate < 90) break;
  }
  const stable = steps
    .filter(step => step.requests >= 2 && step.successRate >= 95)
    .sort((a, b) => b.observedRpm - a.observedRpm)[0]
    || steps.filter(step => step.successRate >= 90).sort((a, b) => b.observedRpm - a.observedRpm)[0]
    || null;
  return {
    steps,
    recommendedConcurrency: stable?.concurrency || 1,
    sustainableRpmWithinCap: stable?.observedRpm || 0,
    note: MAX_REQUESTS_PER_TARGET ? `单目标最多 ${MAX_REQUESTS_PER_TARGET} 次请求，避免烧额度；该值是当前上限内的保守吞吐。` : "",
  };
}

async function loadTranslationProviders() {
  const result = await requestJson(`${BASE_URL}/api/translate/providers`, { method: "GET", headers: headers() });
  if (!result.ok || !Array.isArray(result.data)) return [];
  return result.data.map(item => ({ id: item.id, label: item.label || item.id }));
}

function pickBest(probes) {
  return probes
    .filter(item => item.ok)
    .sort((a, b) => {
      const suitability = suitabilityScore(b) - suitabilityScore(a);
      if (suitability !== 0) return suitability;
      return Number(a.latencyMs || 99_999) - Number(b.latencyMs || 99_999);
    })[0] || null;
}

function suitabilityScore(route) {
  const text = `${route.providerName || ""} ${route.modelName || ""}`.toLowerCase();
  let score = 100;
  if (/safety|guard|moderation/.test(text)) score -= 70;
  if (/translate|riva/.test(text)) score -= 55;
  if (/sql|coder|code/.test(text)) score -= 25;
  if (/calibration|ising/.test(text)) score -= 45;
  if (/vision|vl/.test(text)) score -= 10;
  if (/deepseek|qwen|glm|mistral|llama|gpt|nemotron/.test(text)) score += 12;
  if (/flash|instant|nano|mini|8b|4b/.test(text)) score += 8;
  if (/thinking|reasoning|120b|235b|90b|large|v4-pro/.test(text)) score -= 8;
  return score;
}

function markdown(report) {
  const lines = [];
  lines.push("# PaperPilot AI/翻译容量压测报告");
  lines.push("");
  lines.push(`- 时间：${report.generatedAt}`);
  lines.push(`- 后端：${BASE_URL}`);
  lines.push(`- 单目标压测窗口：${Math.round(DURATION_MS / 1000)} 秒`);
  lines.push(`- 单目标请求上限：${MAX_REQUESTS_PER_TARGET}`);
  lines.push("");
  lines.push("## 模块最佳模型");
  lines.push("");
  lines.push("| 模块 | 最佳模型 | 中转商 | 首次延迟 | 建议并发 | 保守承载 RPM | 备注 |");
  lines.push("|---|---|---|---:|---:|---:|---|");
  for (const item of report.modules) {
    lines.push(`| ${item.name} | ${item.best?.modelName || "无可用"} | ${item.best?.providerName || "-"} | ${item.best?.latencyMs ?? "-"}ms | ${item.capacity?.recommendedConcurrency ?? "-"} | ${item.capacity?.sustainableRpmWithinCap ?? 0} | ${item.error || ""} |`);
  }
  lines.push("");
  lines.push("## 翻译引擎");
  lines.push("");
  lines.push("| 引擎 | 首次延迟 | 建议并发 | 保守承载 RPM | 状态 |");
  lines.push("|---|---:|---:|---:|---|");
  for (const item of report.translations) {
    lines.push(`| ${item.label} | ${item.probe?.latencyMs ?? "-"}ms | ${item.capacity?.recommendedConcurrency ?? "-"} | ${item.capacity?.sustainableRpmWithinCap ?? 0} | ${item.probe?.ok ? "可用" : (item.probe?.error || "不可用")} |`);
  }
  lines.push("");
  lines.push("## 逐步压测明细");
  lines.push("");
  for (const item of [...report.modules, ...report.translations]) {
    lines.push(`### ${item.name || item.label}`);
    for (const step of item.capacity?.steps || []) {
      lines.push(`- 并发 ${step.concurrency}: ${step.success}/${step.requests} 成功，成功率 ${step.successRate}%，RPM ${step.observedRpm}，平均 ${step.avgLatencyMs ?? "-"}ms，P95 ${step.p95LatencyMs ?? "-"}ms`);
      for (const error of step.topErrors || []) lines.push(`  - 错误：${error.count} 次，${error.message}`);
    }
    lines.push("");
  }
  return lines.join("\n");
}

async function main() {
  const report = {
    generatedAt: new Date().toISOString(),
    config: { baseUrl: BASE_URL, durationMs: DURATION_MS, maxConcurrency: MAX_CONCURRENCY, maxRequestsPerTarget: MAX_REQUESTS_PER_TARGET },
    modules: [],
    translations: [],
  };

  for (const scene of scenes) {
    console.log(`\n[AI] ${scene.name} (${scene.id})`);
    const routes = await getPool(scene.id);
    const configured = routes
      .filter(route => route.id && !String(route.id).startsWith("template:"))
      .slice(0, PROBE_LIMIT);
    const probes = [];
    for (const route of configured) {
      const probe = await testModelRoute(route);
      probes.push(probe);
      console.log(`  - ${probe.ok ? "OK" : "NO"} ${route.providerName} / ${route.modelName}: ${probe.latencyMs}ms ${probe.error || ""}`);
    }
    const best = pickBest(probes);
    let capacity = null;
    let error = "";
    if (best) {
      console.log(`  => best ${best.providerName} / ${best.modelName}, running capacity...`);
      capacity = await capacityTest({ kind: "model", route: best });
    } else {
      error = "没有可用模型或模型池未配置";
    }
    report.modules.push({ ...scene, routes: configured, probes, best, capacity, error });
  }

  const providers = await loadTranslationProviders();
  for (const provider of providers) {
    console.log(`\n[Translate] ${provider.label}`);
    const probe = await testTranslateProvider(provider);
    console.log(`  - ${probe.ok ? "OK" : "NO"} ${provider.id}: ${probe.latencyMs}ms ${probe.error || ""}`);
    let capacity = null;
    if (probe.ok) {
      capacity = await capacityTest({ kind: "translate", provider });
    }
    report.translations.push({ ...provider, name: provider.label, probe, capacity });
  }

  const outDir = path.resolve("reports");
  await fs.mkdir(outDir, { recursive: true });
  const stamp = new Date().toISOString().replace(/[:.]/g, "-");
  const jsonPath = path.join(outDir, `ai-capacity-${stamp}.json`);
  const mdPath = path.join(outDir, `ai-capacity-${stamp}.md`);
  await fs.writeFile(jsonPath, JSON.stringify(report, null, 2), "utf8");
  await fs.writeFile(mdPath, markdown(report), "utf8");
  console.log(`\nReport written:\n- ${jsonPath}\n- ${mdPath}`);
}

main().catch(error => {
  console.error(error);
  process.exit(1);
});
