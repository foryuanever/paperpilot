import fs from "node:fs/promises";
import path from "node:path";
import { performance } from "node:perf_hooks";

const BASE_URL = process.env.PAPERPILOT_BASE_URL || "http://localhost:8080";
const ADMIN_USER_ID = process.env.PAPERPILOT_ADMIN_USER_ID || "6";
const VUS = Number(process.env.PAPERPILOT_HEAVY_VUS || 500);
const TIMEOUT_MS = Number(process.env.PAPERPILOT_HEAVY_TIMEOUT_MS || 35_000);
const STEP_MS = Number(process.env.PAPERPILOT_HEAVY_STEP_MS || 20_000);
const AI_ROUTE_CAP = Number(process.env.PAPERPILOT_HEAVY_AI_ROUTE_CAP || 500);
const AI_BUSINESS_CAP = Number(process.env.PAPERPILOT_HEAVY_AI_BUSINESS_CAP || 120);
const TRANSLATE_CAP = Number(process.env.PAPERPILOT_HEAVY_TRANSLATE_CAP || 500);
const READ_CAP = Number(process.env.PAPERPILOT_HEAVY_READ_CAP || 500);
const PROBE_LIMIT = Number(process.env.PAPERPILOT_HEAVY_PROBE_LIMIT || 12);

const aiScenes = [
  {
    id: "paper_review",
    name: "文献综述模型通道",
    prompt: "论文主题：AI辅助论文阅读。请严格返回JSON：{\"points\":[\"研究背景\",\"研究方法\"]}，不要解释。",
  },
  {
    id: "paper_qa",
    name: "AI研读问答模型通道",
    prompt: "用户问：这篇论文的研究问题是什么？请用一句中文回答，80字以内。",
  },
];

const translateProbeText = [
  "Artificial intelligence is reshaping academic reading workflows.",
  "The proposed method improves document parsing accuracy.",
  "This study evaluates model performance across multiple datasets.",
].join(" ");

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
    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = { raw: text };
    }
    const latencyMs = Math.round(performance.now() - started);
    if (!response.ok) {
      return {
        ok: false,
        status: response.status,
        latencyMs,
        error: data?.message || data?.error || text || `HTTP ${response.status}`,
        data,
      };
    }
    return { ok: true, status: response.status, latencyMs, data };
  } catch (error) {
    return {
      ok: false,
      status: 0,
      latencyMs: Math.round(performance.now() - started),
      error: error.name === "AbortError" ? "timeout" : error.message,
    };
  } finally {
    clearTimeout(timer);
  }
}

async function getLibraryPapers() {
  const result = await requestJson(`${BASE_URL}/api/library/papers`, {
    method: "GET",
    headers: headers(),
    timeoutMs: 30_000,
  });
  if (!result.ok || !Array.isArray(result.data)) return [];
  return result.data.filter(item => item.workspaceId && String(item.paperUrl || "").includes(".pdf"));
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
      modelName: row.modelName,
      scene,
      active: Boolean(row.active),
      poolStatus: row.status || "unknown",
      lastLatencyMs: row.latencyMs ?? null,
    }));
}

async function testModelRoute(route) {
  const result = await requestJson(
    `${BASE_URL}/api/admin/model-config/pool/${route.id}/test-model?modelName=${encodeURIComponent(route.modelName)}`,
    { method: "POST", headers: headers() },
  );
  const message = String(result.data?.message || result.error || "");
  const ok = Boolean(
    result.ok
      && result.data?.success !== false
      && /可用|成功|success/i.test(message)
      && !/HTTP\s*(4|5)\d\d|超时|timeout|为空|失败|不可用|end of life|gone/i.test(message)
  );
  return {
    ...route,
    ok,
    status: result.status,
    latencyMs: result.latencyMs,
    error: ok ? "" : message,
  };
}

async function getTranslateProviders() {
  const result = await requestJson(`${BASE_URL}/api/translate/providers`, {
    method: "GET",
    headers: headers(),
    timeoutMs: 30_000,
  });
  if (!result.ok || !Array.isArray(result.data)) return [];
  return result.data.map(item => ({ id: item.id, label: item.label || item.id }));
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
    status: result.status,
    latencyMs: result.latencyMs,
    error: result.ok ? "" : result.error,
  };
}

async function runTargetOnce(target) {
  if (target.kind === "modelRoute") return testModelRoute(target.route);
  if (target.kind === "translate") return testTranslateProvider(target.provider);
  if (target.kind === "paperQa") {
    const result = await requestJson(`${BASE_URL}/api/meeting-reports/${target.workspaceId}/ask`, {
      method: "POST",
      headers: headers(),
      body: JSON.stringify({
        question: "请用两句话概括这篇论文的研究问题和主要贡献。",
        selection: "",
        paragraph: "",
      }),
      timeoutMs: 120_000,
    });
    return {
      ok: Boolean(result.ok && result.data?.answer),
      status: result.status,
      latencyMs: result.latencyMs,
      error: result.ok ? "" : result.error,
    };
  }
  if (target.kind === "paperReviewStatus") {
    const result = await requestJson(`${BASE_URL}/api/meeting-reports/${target.workspaceId}/generate/status`, {
      method: "GET",
      headers: headers(),
      timeoutMs: 30_000,
    });
    return {
      ok: result.ok,
      status: result.status,
      latencyMs: result.latencyMs,
      error: result.ok ? "" : result.error,
    };
  }
  if (target.kind === "pdfMathStatus") {
    const result = await requestJson(`${BASE_URL}/api/pdfmathtranslate/${target.workspaceId}/status`, {
      method: "GET",
      headers: headers(),
      timeoutMs: 30_000,
    });
    return {
      ok: result.ok || result.status === 404,
      status: result.status,
      latencyMs: result.latencyMs,
      error: result.ok || result.status === 404 ? "" : result.error,
    };
  }
  if (target.kind === "mineruStatus") {
    const result = await requestJson(`${BASE_URL}/api/mineru/${target.workspaceId}/status`, {
      method: "GET",
      headers: headers(),
      timeoutMs: 30_000,
    });
    return {
      ok: result.ok,
      status: result.status,
      latencyMs: result.latencyMs,
      error: result.ok ? "" : result.error,
    };
  }
  if (target.kind === "mineruDocument") {
    const result = await requestJson(`${BASE_URL}/api/mineru/${target.workspaceId}/document`, {
      method: "GET",
      headers: headers(),
      timeoutMs: 30_000,
    });
    return {
      ok: result.ok || result.status === 404,
      status: result.status,
      latencyMs: result.latencyMs,
      error: result.ok || result.status === 404 ? "" : result.error,
    };
  }
  throw new Error(`Unknown target kind: ${target.kind}`);
}

async function runStep(target, concurrency, requestCap) {
  const results = [];
  const started = performance.now();
  let issued = 0;
  let active = 0;

  await new Promise(resolve => {
    const pump = () => {
      while (active < concurrency && issued < requestCap) {
        active++;
        issued++;
        runTargetOnce(target)
          .then(result => results.push(result))
          .catch(error => results.push({ ok: false, status: 0, latencyMs: 0, error: error.message }))
          .finally(() => {
            active--;
            if (issued >= requestCap && active === 0) {
              resolve();
            } else {
              pump();
            }
          });
      }
      if (issued >= requestCap && active === 0) resolve();
    };
    pump();
  });

  return summarize(results, Math.max(1, performance.now() - started), concurrency);
}

async function stairCapacity(target, requestCap) {
  const levels = [1, 5, 10, 25, 50, 100, 200, VUS]
    .filter((value, index, array) => value <= VUS && array.indexOf(value) === index);
  const steps = [];
  let remaining = requestCap;

  for (const concurrency of levels) {
    if (remaining <= 0) break;
    const cap = Math.min(remaining, Math.max(concurrency * 2, Math.ceil(requestCap / levels.length)));
    const step = await runStep(target, concurrency, cap);
    steps.push(step);
    remaining -= step.requests;
    if (step.requests >= concurrency && step.successRate < 85) break;
    if (step.topErrors.some(item => /429|rate|quota|too many|请求过于频繁/i.test(item.message))) break;
  }

  const stable = steps
    .filter(step => step.requests >= 2 && step.successRate >= 95)
    .sort((a, b) => b.observedRpm - a.observedRpm)[0] || null;
  const peak = [...steps].sort((a, b) => b.observedRpm - a.observedRpm)[0] || null;
  return {
    requestCap,
    virtualUsers: VUS,
    steps,
    stableConcurrency: stable?.concurrency || 0,
    stableRpm: stable?.observedRpm || 0,
    peakObservedRpm: peak?.observedRpm || 0,
    peakConcurrency: peak?.concurrency || 0,
  };
}

function summarize(results, elapsedMs, concurrency) {
  const success = results.filter(item => item.ok);
  const failed = results.filter(item => !item.ok);
  const latencies = success.map(item => item.latencyMs).filter(value => Number.isFinite(value) && value > 0);
  return {
    concurrency,
    elapsedMs: Math.round(elapsedMs),
    requests: results.length,
    success: success.length,
    failed: failed.length,
    successRate: results.length ? Number((success.length * 100 / results.length).toFixed(1)) : 0,
    observedRpm: Number((success.length * 60_000 / elapsedMs).toFixed(1)),
    avgMs: avg(latencies),
    p50Ms: percentile(latencies, 50),
    p95Ms: percentile(latencies, 95),
    p99Ms: percentile(latencies, 99),
    statusCodes: counts(results.map(item => String(item.status ?? 0))),
    topErrors: topErrors(failed),
  };
}

function avg(values) {
  return values.length ? Math.round(values.reduce((sum, value) => sum + value, 0) / values.length) : null;
}

function percentile(values, p) {
  if (!values.length) return null;
  const sorted = [...values].sort((a, b) => a - b);
  return sorted[Math.min(sorted.length - 1, Math.ceil(sorted.length * p / 100) - 1)];
}

function counts(values) {
  const map = new Map();
  for (const value of values) map.set(value, (map.get(value) || 0) + 1);
  return Object.fromEntries([...map.entries()].sort((a, b) => Number(a[0]) - Number(b[0])));
}

function topErrors(items) {
  const map = new Map();
  for (const item of items) {
    const key = String(item.error || `HTTP ${item.status}` || "unknown").slice(0, 180);
    map.set(key, (map.get(key) || 0) + 1);
  }
  return [...map.entries()].sort((a, b) => b[1] - a[1]).slice(0, 5).map(([message, count]) => ({ message, count }));
}

function suitabilityScore(route) {
  const text = `${route.providerName || ""} ${route.modelName || ""}`.toLowerCase();
  let score = 100;
  if (/safety|guard|moderation/.test(text)) score -= 80;
  if (/translate|riva/.test(text)) score -= 55;
  if (/sql|coder|code/.test(text)) score -= 25;
  if (/calibration|ising/.test(text)) score -= 45;
  if (/vision|vl/.test(text)) score -= 10;
  if (/deepseek|qwen|glm|mistral|llama|gpt|nemotron|gemma|claude/.test(text)) score += 15;
  if (/flash|instant|nano|mini|8b|4b|3b/.test(text)) score += 8;
  if (/thinking|reasoning|120b|235b|90b|large|v4-pro/.test(text)) score -= 8;
  return score;
}

function pickBest(probes) {
  return probes
    .filter(item => item.ok)
    .sort((a, b) => {
      const score = suitabilityScore(b) - suitabilityScore(a);
      if (score !== 0) return score;
      return Number(a.latencyMs || 999_999) - Number(b.latencyMs || 999_999);
    })[0] || null;
}

async function triggerHeavyJobs(workspaceId) {
  const results = {};
  results.paperReviewGenerate = await requestJson(`${BASE_URL}/api/meeting-reports/${workspaceId}/generate`, {
    method: "POST",
    headers: headers(),
    timeoutMs: 30_000,
  });
  results.pdfMathTranslateStart = await requestJson(`${BASE_URL}/api/pdfmathtranslate/${workspaceId}/translate`, {
    method: "POST",
    headers: headers(),
    body: JSON.stringify({ service: "google" }),
    timeoutMs: 60_000,
  });
  results.mineruParseStart = await requestJson(`${BASE_URL}/api/mineru/${workspaceId}/parse?force=false`, {
    method: "POST",
    headers: headers(),
    timeoutMs: 30_000,
  });
  return Object.fromEntries(Object.entries(results).map(([key, value]) => [key, {
    ok: value.ok,
    status: value.status,
    latencyMs: value.latencyMs,
    state: value.data?.state || value.data?.status || "",
    error: value.ok ? "" : value.error,
  }]));
}

function formatTable(rows, columns) {
  const header = `| ${columns.map(column => column.title).join(" | ")} |`;
  const sep = `| ${columns.map(() => "---").join(" | ")} |`;
  const body = rows.map(row => `| ${columns.map(column => String(column.value(row) ?? "").replaceAll("\n", " ").replaceAll("|", "\\|")).join(" | ")} |`);
  return [header, sep, ...body].join("\n");
}

function renderMarkdown(report) {
  const lines = [];
  lines.push("# PaperPilot 500 虚拟用户 AI / 翻译 / PDF 解析压测报告");
  lines.push("");
  lines.push(`- 生成时间：${report.generatedAt}`);
  lines.push(`- 后端地址：${BASE_URL}`);
  lines.push(`- 虚拟用户：${VUS}`);
  lines.push(`- AI 模型通道单目标请求上限：${AI_ROUTE_CAP}`);
  lines.push(`- AI 真实业务接口单目标请求上限：${AI_BUSINESS_CAP}`);
  lines.push(`- 翻译接口单目标请求上限：${TRANSLATE_CAP}`);
  lines.push(`- PDF 解析/状态读取单目标请求上限：${READ_CAP}`);
  lines.push("");
  lines.push("> 说明：文献综述生成、AI 问答、PDF 双栏翻译、MinerU 解析都会消耗上游模型或本机重计算。模型通道和翻译按 500 虚拟用户压测；真实 AI 问答默认限制 120 次调用并保留 500 VU 阶梯，用于判断上线瓶颈，避免无意义耗光额度。文献综述生成、PDF 双栏翻译、MinerU 解析只触发一次真实任务并压测状态/读取接口；否则 500 个用户重复提交同一篇 PDF 会制造无意义队列拥塞。");
  lines.push("");

  lines.push("## 结论总览");
  lines.push("");
  lines.push(formatTable(report.summary, [
    { title: "模块", value: row => row.name },
    { title: "最佳/目标", value: row => row.target },
    { title: "稳定并发", value: row => row.stableConcurrency },
    { title: "稳定 RPM", value: row => row.stableRpm },
    { title: "峰值 RPM", value: row => row.peakObservedRpm },
    { title: "主要问题", value: row => row.issue || "" },
  ]));
  lines.push("");

  lines.push("## 模型池探测");
  for (const scene of report.aiScenes) {
    lines.push("");
    lines.push(`### ${scene.name}`);
    lines.push(`最佳模型：${scene.best ? `${scene.best.providerName} / ${scene.best.modelName}` : "无可用模型"}`);
    lines.push(formatTable(scene.probes, [
      { title: "中转商", value: row => row.providerName },
      { title: "模型", value: row => row.modelName },
      { title: "可用", value: row => row.ok ? "是" : "否" },
      { title: "延迟", value: row => row.latencyMs == null ? "-" : `${row.latencyMs}ms` },
      { title: "错误", value: row => row.error || "" },
    ]));
  }
  lines.push("");

  lines.push("## 详细阶梯数据");
  for (const detail of report.details) {
    lines.push("");
    lines.push(`### ${detail.name}`);
    lines.push(formatTable(detail.capacity.steps, [
      { title: "并发", value: row => row.concurrency },
      { title: "请求", value: row => row.requests },
      { title: "成功", value: row => row.success },
      { title: "失败", value: row => row.failed },
      { title: "成功率", value: row => `${row.successRate}%` },
      { title: "观测RPM", value: row => row.observedRpm },
      { title: "平均", value: row => row.avgMs == null ? "-" : `${row.avgMs}ms` },
      { title: "P95", value: row => row.p95Ms == null ? "-" : `${row.p95Ms}ms` },
      { title: "P99", value: row => row.p99Ms == null ? "-" : `${row.p99Ms}ms` },
      { title: "状态码", value: row => JSON.stringify(row.statusCodes) },
      { title: "Top错误", value: row => row.topErrors.map(item => `${item.count}x ${item.message}`).join("; ") },
    ]));
  }
  lines.push("");

  lines.push("## 重任务创建入口");
  lines.push("");
  lines.push(formatTable(Object.entries(report.heavyJobs).map(([name, value]) => ({ name, ...value })), [
    { title: "任务", value: row => row.name },
    { title: "可提交", value: row => row.ok ? "是" : "否" },
    { title: "HTTP", value: row => row.status },
    { title: "延迟", value: row => `${row.latencyMs}ms` },
    { title: "状态", value: row => row.state || "" },
    { title: "错误", value: row => row.error || "" },
  ]));
  lines.push("");

  lines.push("## 原始配置");
  lines.push("");
  lines.push("```json");
  lines.push(JSON.stringify(report.config, null, 2));
  lines.push("```");
  return lines.join("\n");
}

async function main() {
  console.log(`[bench] 后端健康检查 ${BASE_URL}`);
  const health = await requestJson(`${BASE_URL}/actuator/health`, { method: "GET", headers: headers(), timeoutMs: 10_000 });
  if (!health.ok) throw new Error(`后端不可用：${health.error}`);

  console.log("[bench] 读取文献库，寻找带 PDF 的测试文献");
  const papers = await getLibraryPapers();
  const paper = papers[0];
  if (!paper) throw new Error("没有找到带 PDF 的文献，无法压测真实文献业务接口。");

  const report = {
    generatedAt: new Date().toISOString(),
    config: {
      BASE_URL,
      ADMIN_USER_ID,
      VUS,
      TIMEOUT_MS,
      AI_ROUTE_CAP,
      AI_BUSINESS_CAP,
      TRANSLATE_CAP,
      READ_CAP,
      PROBE_LIMIT,
      workspaceId: paper.workspaceId,
      title: paper.title,
    },
    aiScenes: [],
    details: [],
    heavyJobs: {},
    summary: [],
  };

  for (const scene of aiScenes) {
    console.log(`[bench] 探测模型池：${scene.name}`);
    const pool = await getPool(scene.id);
    const candidates = pool
      .sort((a, b) => {
        const active = Number(b.active) - Number(a.active);
        if (active !== 0) return active;
        return Number(a.lastLatencyMs || 999_999) - Number(b.lastLatencyMs || 999_999);
      })
      .slice(0, PROBE_LIMIT);
    const probes = [];
    for (const [index, route] of candidates.entries()) {
      console.log(`[bench]   ${index + 1}/${candidates.length} ${route.providerName} / ${route.modelName}`);
      probes.push(await testModelRoute(route));
    }
    const best = pickBest(probes);
    report.aiScenes.push({ ...scene, candidates: candidates.length, probes, best });
    if (best) {
      console.log(`[bench] 压测最佳模型：${scene.name} => ${best.providerName} / ${best.modelName}`);
      const capacity = await stairCapacity({ kind: "modelRoute", route: best }, AI_ROUTE_CAP);
      const name = scene.name;
      report.details.push({ name, capacity });
      report.summary.push({
        name,
        target: `${best.providerName} / ${best.modelName}`,
        stableConcurrency: capacity.stableConcurrency,
        stableRpm: capacity.stableRpm,
        peakObservedRpm: capacity.peakObservedRpm,
        issue: issueFromCapacity(capacity),
      });
    } else {
      report.summary.push({ name: scene.name, target: "无可用", stableConcurrency: 0, stableRpm: 0, peakObservedRpm: 0, issue: "模型池无可用路由" });
    }
  }

  const translations = await getTranslateProviders();
  for (const provider of translations) {
    console.log(`[bench] 压测翻译接口：${provider.label}`);
    const probe = await testTranslateProvider(provider);
    const capacity = probe.ok
      ? await stairCapacity({ kind: "translate", provider }, TRANSLATE_CAP)
      : { requestCap: TRANSLATE_CAP, virtualUsers: VUS, steps: [summarize([probe], probe.latencyMs || 1, 1)], stableConcurrency: 0, stableRpm: 0, peakObservedRpm: 0, peakConcurrency: 0 };
    const name = `划词翻译：${provider.label}`;
    report.details.push({ name, capacity });
    report.summary.push({
      name,
      target: provider.id,
      stableConcurrency: capacity.stableConcurrency,
      stableRpm: capacity.stableRpm,
      peakObservedRpm: capacity.peakObservedRpm,
      issue: probe.ok ? issueFromCapacity(capacity) : probe.error,
    });
  }

  console.log("[bench] 触发一次重任务入口：文献综述 / 对照翻译 / MinerU");
  report.heavyJobs = await triggerHeavyJobs(paper.workspaceId);

  const businessTargets = [
    { name: "AI研读问答真实接口", kind: "paperQa", cap: AI_BUSINESS_CAP },
    { name: "文献综述生成状态接口", kind: "paperReviewStatus", cap: READ_CAP },
    { name: "对照翻译 PDFMathTranslate 状态接口", kind: "pdfMathStatus", cap: READ_CAP },
    { name: "沉浸翻译 MinerU 解析状态接口", kind: "mineruStatus", cap: READ_CAP },
    { name: "沉浸翻译 MinerU 文档读取接口", kind: "mineruDocument", cap: READ_CAP },
  ];
  for (const target of businessTargets) {
    console.log(`[bench] 压测业务接口：${target.name}`);
    const capacity = await stairCapacity({ kind: target.kind, workspaceId: paper.workspaceId }, target.cap);
    report.details.push({ name: target.name, capacity });
    report.summary.push({
      name: target.name,
      target: paper.workspaceId,
      stableConcurrency: capacity.stableConcurrency,
      stableRpm: capacity.stableRpm,
      peakObservedRpm: capacity.peakObservedRpm,
      issue: issueFromCapacity(capacity),
    });
  }

  const stamp = new Date().toISOString().replaceAll(":", "-").replaceAll(".", "-");
  const outDir = path.resolve("reports");
  await fs.mkdir(outDir, { recursive: true });
  const jsonPath = path.join(outDir, `ai-heavy-500-${stamp}.json`);
  const mdPath = path.join(outDir, `ai-heavy-500-${stamp}.md`);
  await fs.writeFile(jsonPath, JSON.stringify(report, null, 2));
  await fs.writeFile(mdPath, renderMarkdown(report));
  console.log(JSON.stringify({ jsonPath, mdPath, summary: report.summary }, null, 2));
}

function issueFromCapacity(capacity) {
  const badStep = capacity.steps.find(step => step.successRate < 95);
  if (!badStep) return "";
  const errors = badStep.topErrors.map(item => item.message).join("; ");
  if (/429|rate|quota|too many|请求过于频繁/i.test(errors)) return `并发 ${badStep.concurrency} 出现限流`;
  if (/timeout/i.test(errors)) return `并发 ${badStep.concurrency} 出现超时`;
  return `并发 ${badStep.concurrency} 成功率降至 ${badStep.successRate}%`;
}

main().catch(error => {
  console.error(error);
  process.exit(1);
});
