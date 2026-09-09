import fs from "node:fs";
import path from "node:path";
import { createHash } from "node:crypto";
import { spawnSync } from "node:child_process";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const asar = require("@electron/asar");
const desktopRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const repoRoot = path.resolve(desktopRoot, "..");
const packageJson = readJson(path.join(desktopRoot, "package.json"));
const version = packageJson.version;
const failures = [];
const checks = [];

await checkSourceContract();
await checkWindowsDependencyArchive();
await checkPackagedApplication();

if (process.argv.includes("--online")) {
  await checkOnlineDependencyManifest();
}

for (const check of checks) console.log(`[PASS] ${check}`);
for (const failure of failures) console.error(`[FAIL] ${failure}`);
if (failures.length) {
  process.exitCode = 1;
} else {
  console.log(`\nPaperSolver ${version} release smoke test passed (${checks.length} checks).`);
}

async function checkSourceContract() {
  const mainSource = await fs.promises.readFile(path.join(desktopRoot, "src", "main.cjs"), "utf8");
  const preloadSource = await fs.promises.readFile(path.join(desktopRoot, "src", "preload.cjs"), "utf8");
  const frontSource = await fs.promises.readFile(path.join(repoRoot, "front", "src", "views", "LoginView.vue"), "utf8");
  const authController = await fs.promises.readFile(path.join(repoRoot, "backend", "src", "main", "java", "com", "paperpilot", "server", "controller", "AuthController.java"), "utf8");
  const requirements = await fs.promises.readFile(path.join(desktopRoot, "dependency-service", "structured", "requirements.txt"), "utf8");
  expect(mainSource.includes('"-f", "true"'), "structured parsing enables formula recognition");
  expect(!mainSource.includes('"-f", "false"'), "desktop source contains no disabled formula invocation");
  expect(mainSource.includes('"--client-side-output-generation", "true"'), "MinerU content lists are regenerated client-side");
  expect(mainSource.includes("runStructuredParserRuntimePreflight"), "structured parser validates runtime package availability before real PDF readiness");
  expect(mainSource.includes("installWindowsVcRuntimeIfAvailable"), "Windows parser preflight can install bundled VC++ runtime");
  expect(mainSource.includes("structuredParserSpawnCommand"), "Windows structured parser bypasses the cmd wrapper");
  expect(mainSource.includes("extractArchiveWithTarProgress"), "Windows extraction reports real progress");
  expect(mainSource.includes("enqueuePdfTranslationBridge"), "PDF translation requests are queued to prevent provider bursts");
  expect(mainSource.includes("pdfContentFingerprint") && mainSource.includes("已命中同一原始 PDF 的双语译文缓存"), "PDF translation cache is keyed by original PDF content");
  expect(mainSource.includes("PDF_TRANSLATION_BRIDGE_CONCURRENCY = Math.max(1, Math.min(2"), "PDF translation uses bounded two-way concurrency");
  expect(mainSource.includes("已复用上次完整校验的下载文件"), "verified dependency downloads are reusable after failure");
  expect(mainSource.includes("prepareQqLocalOAuthCallback") && mainSource.includes('server.listen(0, "127.0.0.1"'), "desktop QQ OAuth creates a one-time localhost callback");
  expect(preloadSource.includes("prepareQQOAuthLocalCallback"), "desktop preload exposes the QQ localhost callback bridge");
  expect(frontSource.includes("desktop_local_") && frontSource.includes("prepareQQOAuthLocalCallback"), "desktop login requests a localhost QQ callback before opening the browser");
  expect(authController.includes("parseDesktopLocalOAuthCallback") && authController.includes("http://127.0.0.1:"), "backend returns desktop QQ OAuth to the verified localhost callback");
  expect(frontSource.includes('/downloads/PaperSolver.exe'), "homepage points to the canonical current Windows installer");
  expect(requirements.includes("pdftext==0.6.3"), "pdftext is pinned to the MinerU-compatible version");
  expect(requirements.includes("ftfy==6.3.1"), "formula text runtime is pinned");
  expect(requirements.includes("mineru[pipeline]==3.4.0"), "MinerU pipeline dependencies are explicitly required");
  expect(requirements.includes("huggingface-hub==0.36.0"), "transformers-compatible huggingface-hub is pinned");
  expect(requirements.includes("pypdfium2==4.30.0"), "pdftext-compatible pypdfium2 is pinned");
  const pdfService = await fs.promises.readFile(path.join(desktopRoot, "dependency-service", "pdf", "server.py"), "utf8");
  expect(pdfService.includes("pdfTranslationBridge"), "PDF service marks high-volume bridge requests for queueing");
  expect(pdfService.includes("translation watchdog expired"), "PDF service has a terminal timeout instead of indefinite polling");
  expect(pdfService.includes("TRANSLATION_STALL_TIMEOUT_SECONDS") && pdfService.includes("watch_translate_task"), "PDF service uses an activity watchdog instead of a short fixed-duration kill");
  expect(pdfService.includes("lastActivityAt") && pdfService.includes("touch_translate_task"), "PDF service records bridge activity for long documents");
  for (const relativePath of [
    "dependency-service/structured/pdftext-0.6.3/pdftext/pdf/chars.py",
    "dependency-service/structured/formula-compat/ftfy/__init__.py",
    "dependency-service/structured/formula-compat/wcwidth/__init__.py",
    "dependency-service/structured/formula-compat/formula-smoke.pdf"
  ]) {
    expectFile(path.join(desktopRoot, relativePath), `bundled compatibility resource ${relativePath}`);
  }
}

async function checkWindowsDependencyArchive() {
  const manifestPath = path.join(desktopRoot, "scratch", "manifest", "dependency-manifest.json");
  const manifest = readJson(manifestPath);
  expect(manifest.version && String(manifest.version).endsWith("-offline-full-pipeline"), "local dependency manifest declares the offline runtime");
  const item = manifest.packages.find((entry) => entry?.id === "runtime-full" && entry?.platforms?.includes("windows-x64"));
  if (!item) {
    fail("local manifest has no windows-x64 dependency package");
    return;
  }
  const manifestArchiveName = (() => {
    try {
      return path.basename(new URL(String(item.urls?.[0] || "")).pathname);
    } catch {
      return "";
    }
  })();
  const versionedArchivePath = manifestArchiveName
    ? path.join(desktopRoot, "scratch", "windows-cross", "offline-release", manifestArchiveName)
    : "";
  const genericArchivePath = path.join(desktopRoot, "scratch", "windows-cross", "offline-release", "papersolver-local-dependency-windows-x64.zip");
  const archivePath = versionedArchivePath && fs.existsSync(versionedArchivePath)
    ? versionedArchivePath
    : genericArchivePath;
  expectFile(archivePath, "Windows offline dependency archive exists");
  if (!fs.existsSync(archivePath)) return;
  const stat = await fs.promises.stat(archivePath);
  expect(stat.size === Number(item.size), `Windows dependency size matches manifest (${stat.size} bytes)`);
  const sha256 = await hashFile(archivePath);
  expect(sha256 === String(item.sha256).toLowerCase(), `Windows dependency SHA256 matches manifest (${sha256})`);
  const zipTest = spawnSync("unzip", ["-tq", archivePath], { encoding: "utf8", maxBuffer: 16 * 1024 * 1024 });
  expect(zipTest.status === 0, "Windows dependency ZIP passes a complete integrity scan");
  const listing = spawnSync("unzip", ["-Z1", archivePath], { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });
  if (listing.status !== 0) {
    fail(`cannot list Windows dependency ZIP: ${listing.stderr || listing.stdout}`);
    return;
  }
  const entries = listing.stdout.split(/\r?\n/).filter(Boolean);
  for (const required of [
    ".runtime-venv/python.exe",
    ".runtime-venv/Lib/site-packages/mineru/cli/client.py",
    ".runtime-venv/Lib/site-packages/pdftext-0.6.3.dist-info/METADATA",
    ".runtime-venv/msvcp140.dll",
    "bin/start-papersolver-dependency.cmd",
    "bin/mineru.cmd",
    "tools/vc_redist.x64.exe",
    "services/pdf/server.py",
    "papersolver-dependency.json"
  ]) {
    expect(entries.includes(required), `Windows dependency contains ${required}`);
  }
  const forbiddenProductFront = entries.filter((entry) => /^(front-dist|resources\/app|dist\/assets)\//i.test(entry));
  expect(forbiddenProductFront.length === 0, "downloaded dependency contains no PaperSolver frontend bundle");
  const longest = entries.reduce((current, entry) => entry.length > current.length ? entry : current, "");
  expect(longest.length < 240, `dependency paths remain below the Windows MAX_PATH budget (longest archive path: ${longest.length})`);
  const sourceRoot = path.join(desktopRoot, "scratch", "windows-cross", "offline-dependency-source-windows-x64");
  expect(readMagic(path.join(sourceRoot, ".runtime-venv", "python.exe"), 2).toString("ascii") === "MZ", "Windows runtime Python is a PE executable");
  expect(readMagic(path.join(sourceRoot, ".runtime-venv", "msvcp140.dll"), 2).toString("ascii") === "MZ", "Windows app-local MSVCP140 runtime is a PE executable");
  expect(readMagic(path.join(sourceRoot, "tools", "vc_redist.x64.exe"), 2).toString("ascii") === "MZ", "Windows VC++ redistributable is bundled as a PE executable");
  expectFile(path.join(sourceRoot, ".runtime-venv", "Lib", "site-packages", "torch", "lib", "c10.dll"), "Windows dependency contains PyTorch c10.dll");
  const c10Imports = spawnSync("objdump", ["-p", path.join(sourceRoot, ".runtime-venv", "Lib", "site-packages", "torch", "lib", "c10.dll")], { encoding: "utf8", maxBuffer: 8 * 1024 * 1024 });
  expect(c10Imports.status === 0 && /DLL Name:\s+MSVCP140\.dll/i.test(c10Imports.stdout), "Windows c10.dll imports MSVCP140.dll and therefore needs the bundled VC++ runtime");
  const modelRoot = path.join(sourceRoot, "modelscope-cache", "models", "OpenDataLab", "PDF-Extract-Kit-1___0", "models");
  expectDir(path.join(modelRoot, "MFR", "unimernet_hf_small_2503"), "formula recognition model is present");
  const modelBytes = await directorySize(modelRoot);
  expect(modelBytes >= 891289600, `formula/layout/OCR model payload is complete (${modelBytes} bytes)`);
  // Table recognition is intentionally omitted from the Windows package. The
  // desktop smoke path disables table post-processing to avoid a second model
  // download and its extra memory pressure; formula/layout/OCR are the required
  // parser capabilities for this release.
}

async function checkPackagedApplication() {
  const unpackedRoot = path.join(desktopRoot, "release", "win-unpacked");
  const asarPath = path.join(unpackedRoot, "resources", "app.asar");
  const installerPath = path.join(desktopRoot, "release", `PaperSolver Setup ${version}.exe`);
  expectFile(asarPath, "Windows app.asar exists");
  expectFile(installerPath, `Windows ${version} installer exists`);
  if (fs.existsSync(asarPath)) {
    const packedJson = JSON.parse(asar.extractFile(asarPath, "package.json").toString("utf8"));
    const packedMain = asar.extractFile(asarPath, "src/main.cjs").toString("utf8");
    const packedPdfService = asar.extractFile(asarPath, "dependency-service/pdf/server.py").toString("utf8");
    expect(packedJson.version === version, `packaged app version is ${version}`);
    expect(packedMain.includes('"-f", "true"') && !packedMain.includes('"-f", "false"'), "packaged app enables formula recognition");
    expect(packedMain.includes("extractArchiveWithTarProgress"), "packaged app includes fresh-install extraction logic");
    expect(packedMain.includes("installWindowsVcRuntimeIfAvailable"), "packaged app includes bundled VC++ runtime installer hook");
    expect(packedMain.includes("enqueuePdfTranslationBridge"), "packaged app queues PDF translation bridge requests");
    expect(packedMain.includes("pdfContentFingerprint") && packedMain.includes("已命中同一原始 PDF 的双语译文缓存"), "packaged app caches translations by original PDF content");
    expect(packedPdfService.includes("pdfTranslationBridge"), "packaged app marks PDF bridge translation requests");
    expect(packedPdfService.includes("translation watchdog expired"), "packaged app has a terminal PDF translation timeout");
    expect(packedPdfService.includes("TRANSLATION_STALL_TIMEOUT_SECONDS") && packedPdfService.includes("watch_translate_task"), "packaged app uses the activity watchdog for long PDF translations");
  }
  for (const relativePath of [
    "resources/compat/pdftext-0.6.3/pdftext/pdf/chars.py",
    "resources/compat/formula/ftfy/__init__.py",
    "resources/compat/formula/wcwidth/__init__.py",
    "resources/compat/formula/formula-smoke.pdf"
  ]) {
    expectFile(path.join(unpackedRoot, relativePath), `packaged app contains ${relativePath}`);
  }
  if (fs.existsSync(installerPath)) {
    const stat = await fs.promises.stat(installerPath);
    expect(stat.size > 70 * 1024 * 1024, `Windows installer has a plausible size (${stat.size} bytes)`);
    expect(readMagic(installerPath, 2).toString("ascii") === "MZ", "Windows installer has a PE header");
    checks.push(`Windows installer SHA256 ${await hashFile(installerPath)}`);
  }
}

async function checkOnlineDependencyManifest() {
  const response = await fetch("https://papersolver.cn/downloads/dependencies/dependency-manifest.json", { signal: AbortSignal.timeout(15000) });
  expect(response.ok, `online dependency manifest responds HTTP ${response.status}`);
  if (!response.ok) return;
  const manifest = await response.json();
  expect(manifest.version === `${version}-offline-full-pipeline`, `online dependency manifest is bound to ${version}`);
  const localPackages = readJson(path.join(desktopRoot, "scratch", "manifest", "dependency-manifest.json")).packages
    .filter((entry) => entry?.platforms?.includes("windows-x64"));
  for (const local of localPackages) {
    const online = manifest.packages?.find((entry) => entry?.id === local.id && entry?.platforms?.includes("windows-x64"));
    expect(online?.size === local?.size && online?.sha256 === local?.sha256, `online Windows ${local.id} manifest matches the verified local archive`);
    if (!online?.urls?.[0]) continue;
    const head = await fetch(online.urls[0], { method: "HEAD", redirect: "follow", signal: AbortSignal.timeout(20000) });
    expect(head.ok, `ModelScope Windows ${local.id} package responds HTTP ${head.status}`);
    const contentLength = Number(head.headers.get("content-length")) || 0;
    expect(!contentLength || contentLength === Number(online.size), `ModelScope ${local.id} Content-Length matches manifest (${contentLength || "not provided"})`);
  }
}

function expect(condition, message) {
  if (condition) checks.push(message);
  else failures.push(message);
}

function fail(message) {
  failures.push(message);
}

function expectFile(filePath, label) {
  expect(fs.existsSync(filePath) && fs.statSync(filePath).isFile(), label);
}

function expectDir(dirPath, label) {
  expect(fs.existsSync(dirPath) && fs.statSync(dirPath).isDirectory(), label);
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function readMagic(filePath, bytes) {
  if (!fs.existsSync(filePath)) return Buffer.alloc(0);
  const handle = fs.openSync(filePath, "r");
  const buffer = Buffer.alloc(bytes);
  fs.readSync(handle, buffer, 0, bytes, 0);
  fs.closeSync(handle);
  return buffer;
}

function hashFile(filePath) {
  return new Promise((resolve, reject) => {
    const hash = createHash("sha256");
    const stream = fs.createReadStream(filePath);
    stream.on("data", (chunk) => hash.update(chunk));
    stream.on("error", reject);
    stream.on("end", () => resolve(hash.digest("hex")));
  });
}

async function directorySize(root) {
  let total = 0;
  const entries = await fs.promises.readdir(root, { withFileTypes: true }).catch(() => []);
  for (const entry of entries) {
    const filePath = path.join(root, entry.name);
    if (entry.isDirectory()) total += await directorySize(filePath);
    else if (entry.isFile()) total += (await fs.promises.stat(filePath)).size;
  }
  return total;
}
