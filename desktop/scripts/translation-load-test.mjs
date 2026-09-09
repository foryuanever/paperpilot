#!/usr/bin/env node

import http from "node:http";
import process from "node:process";
import { performance } from "node:perf_hooks";

const args = new Map();
for (let index = 2; index < process.argv.length; index += 1) {
  const value = process.argv[index];
  if (!value.startsWith("--")) continue;
  const [key, inline] = value.slice(2).split("=", 2);
  args.set(key, inline ?? process.argv[index + 1] ?? "true");
  if (inline === undefined) index += 1;
}

const numberArg = (name, fallback, min = 1, max = 5000) => {
  const value = Number(args.get(name) ?? fallback);
  if (!Number.isFinite(value) || value < min || value > max) {
    throw new Error(`--${name} 必须在 ${min}-${max} 之间`);
  }
  return Math.floor(value);
};

const levels = String(args.get("users") ?? "100,300,500")
  .split(",")
  .map(value => Number(value.trim()))
  .filter(value => Number.isFinite(value) && value > 0);
const rounds = numberArg("rounds", 1, 1, 20);
const mockLatency = numberArg("mock-latency", 120, 0, 60000);
const durationSeconds = numberArg("duration-seconds", 0, 0, 3600);
const target = String(args.get("target") ?? "").trim();

if (target && !/^https?:\/\/127\.0\.0\.1(?::\d+)?\//.test(target)) {
  throw new Error("为了避免误压公共服务，--target 只允许指向 127.0.0.1");
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function percentile(values, ratio) {
  if (!values.length) return 0;
  const sorted = [...values].sort((a, b) => a - b);
  return sorted[Math.min(sorted.length - 1, Math.floor(sorted.length * ratio))];
}

function startMockServer() {
  const server = http.createServer(async (request, response) => {
    if (request.method !== "POST" || request.url !== "/translate") {
      response.writeHead(404).end();
      return;
    }
    let body = "";
    request.setEncoding("utf8");
    for await (const chunk of request) body += chunk;
    await sleep(mockLatency);
    response.writeHead(200, { "Content-Type": "application/json" });
    response.end(JSON.stringify({
      ok: true,
      provider: "local-load-test-mock",
      translatedText: `模拟译文: ${JSON.parse(body || "{}").text || ""}`
    }));
  });
  return new Promise(resolve => server.listen(0, "127.0.0.1", () => {
    resolve({ server, url: `http://127.0.0.1:${server.address().port}/translate` });
  }));
}

async function request(url, user, round) {
  const started = performance.now();
  try {
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        provider: "google",
        sourceLang: "en",
        targetLang: "zh-CN",
        text: `Virtual user ${user}, round ${round}: PaperSolver load test`
      }),
      signal: AbortSignal.timeout(45000)
    });
    const payload = await response.json().catch(() => ({}));
    const latency = performance.now() - started;
    return { ok: response.ok && Boolean(payload.translatedText), latency };
  } catch (error) {
    return { ok: false, latency: performance.now() - started, error: error.message };
  }
}

async function runLevel(url, users, round) {
  const started = performance.now();
  const results = await Promise.all(
    Array.from({ length: users }, async (_, index) => {
      if (durationSeconds > 0) {
        await sleep(Math.round(index * durationSeconds * 1000 / users));
      }
      return request(url, index + 1, round);
    })
  );
  const elapsed = performance.now() - started;
  const successful = results.filter(item => item.ok);
  const failed = results.filter(item => !item.ok);
  const latencies = results.map(item => item.latency);
  return {
    users,
    success: successful.length,
    failed: failed.length,
    elapsedMs: Math.round(elapsed),
    throughputPerMinute: Math.round(successful.length * 60000 / Math.max(elapsed, 1)),
    p50Ms: Math.round(percentile(latencies, 0.5)),
    p95Ms: Math.round(percentile(latencies, 0.95)),
    firstError: failed[0]?.error || ""
  };
}

const mock = target ? null : await startMockServer();
const url = target || mock.url;
console.log(`模式: ${target ? `本机目标 ${target}` : `离线模拟器 ${url}`}`);
console.log(`每档用户数: ${levels.join(", ")}；每用户请求轮数: ${rounds}`);
if (durationSeconds > 0) console.log(`请求将在 ${durationSeconds} 秒内平滑到达，而不是瞬间同时到达。`);
if (!target) console.log(`模拟单请求延迟: ${mockLatency}ms；不会访问任何外部翻译服务。`);

for (const users of levels) {
  for (let round = 0; round < rounds; round += 1) {
    const result = await runLevel(url, users, round + 1);
    console.log(JSON.stringify({ round: round + 1, ...result }));
  }
}

mock?.server.close();
