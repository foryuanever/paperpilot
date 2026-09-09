import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const desktopRoot = path.resolve(__dirname, "..");
const sourceDir = path.resolve(process.env.PAPER_SOLVER_DEPENDENCY_SOURCE || path.join(desktopRoot, "local-dependency-source"));
const platformName = String(process.env.PAPER_SOLVER_DEPENDENCY_PLATFORM || normalizePlatform(process.platform)).toLowerCase();
const isWindowsTarget = platformName === "windows";
const maxPackageBytes = numberEnv("PAPER_SOLVER_DEPENDENCY_MAX_RAW_BYTES", 2.4 * 1024 * 1024 * 1024);

const manifestPath = path.join(sourceDir, "papersolver-dependency.json");
const manifest = JSON.parse(await fs.promises.readFile(manifestPath, "utf8"));
const findings = [];

await checkRequiredPaths();
await checkRuntimePackages();
await checkModels();
await checkGpuArtifacts();
const topEntries = await topLevelSizes(sourceDir);
const totalBytes = topEntries.reduce((sum, item) => sum + item.bytes, 0);

if (totalBytes > maxPackageBytes) {
  findings.push({
    level: "warn",
    message: `raw package is large: ${formatBytes(totalBytes)} > ${formatBytes(maxPackageBytes)}`
  });
}

async function checkRuntimePackages() {
  const sitePackages = isWindowsTarget
    ? path.join(sourceDir, ".runtime-venv", "Lib", "site-packages")
    : await findUnixSitePackages();
  expectFile(path.join(sitePackages, "pdftext", "pdf", "chars.py"), "pdftext runtime");
  expectFile(path.join(sitePackages, "ftfy", "__init__.py"), "formula text runtime");
  expectFile(path.join(sitePackages, "wcwidth", "__init__.py"), "formula text dependency");
  expectFile(path.join(sitePackages, "torch", "__init__.py"), "MinerU pipeline torch runtime");
  expectFile(path.join(sitePackages, "torch", "cuda", "__init__.py"), "CPU PyTorch torch.cuda compatibility module");
  expectFile(path.join(sitePackages, "torch", "backends", "cudnn", "__init__.py"), "CPU PyTorch torch.backends.cudnn compatibility module");
  expectFile(path.join(sitePackages, "torchvision", "__init__.py"), "MinerU pipeline torchvision runtime");
  expectFile(path.join(sitePackages, "torch-2.6.0.dist-info", "METADATA"), "PyTorch pinned runtime 2.6.0");
  expectFile(path.join(sitePackages, "torchvision-0.21.0.dist-info", "METADATA"), "torchvision pinned runtime 0.21.0");
  expectFile(path.join(sitePackages, "transformers", "__init__.py"), "MinerU pipeline transformers runtime");
  expectFile(path.join(sitePackages, "huggingface_hub", "__init__.py"), "MinerU-compatible Hugging Face runtime");
  expectFile(path.join(sitePackages, "tokenizers", "__init__.py"), "MinerU pipeline tokenizers runtime");
  expectFile(path.join(sitePackages, "safetensors", "__init__.py"), "MinerU pipeline safetensors runtime");
  if (!fs.existsSync(path.join(sitePackages, "pdftext-0.6.3.dist-info", "METADATA"))) {
    findings.push({ level: "error", message: "pdftext must be pinned to 0.6.3 for MinerU 3.4.0 compatibility" });
  }
  if (!fs.existsSync(path.join(sitePackages, "huggingface_hub-0.36.0.dist-info", "METADATA"))) {
    findings.push({ level: "error", message: "huggingface-hub must be pinned below 1.0 for transformers 4.57.3" });
  }
  if (!fs.existsSync(path.join(sitePackages, "pypdfium2-4.30.0.dist-info", "METADATA"))) {
    findings.push({ level: "error", message: "pypdfium2 must be pinned to 4.30.0 for pdftext 0.6.3" });
  }
}

async function findUnixSitePackages() {
  const libRoot = path.join(sourceDir, ".runtime-venv", "lib");
  const entries = await fs.promises.readdir(libRoot, { withFileTypes: true }).catch(() => []);
  const pythonDir = entries.find((entry) => entry.isDirectory() && /^python\d/i.test(entry.name));
  return pythonDir ? path.join(libRoot, pythonDir.name, "site-packages") : path.join(libRoot, "site-packages");
}

console.log("PaperSolver offline dependency audit");
console.log(`Source: ${sourceDir}`);
console.log(`Raw size: ${formatBytes(totalBytes)}`);
console.log("");
console.log("Top-level size:");
for (const item of topEntries) {
  console.log(`- ${item.name}: ${formatBytes(item.bytes)}`);
}
console.log("");

if (findings.length) {
  console.log("Findings:");
  for (const finding of findings) {
    console.log(`- [${finding.level.toUpperCase()}] ${finding.message}`);
  }
} else {
  console.log("Findings: none");
}

const hasError = findings.some((item) => item.level === "error");
if (hasError) {
  process.exitCode = 1;
}

async function checkRequiredPaths() {
  expectAnyFile(isWindowsTarget
    ? [
      path.join(sourceDir, ".runtime-venv", "python.exe"),
      path.join(sourceDir, ".runtime-venv", "Scripts", "python.exe")
    ]
    : [path.join(sourceDir, ".runtime-venv", "bin", "python")], "Python runtime");
  expectAnyFile(isWindowsTarget
    ? [
      path.join(sourceDir, ".runtime-venv", "Scripts", "mineru.exe"),
      path.join(sourceDir, ".runtime-venv", "Lib", "site-packages", "mineru", "cli", "client.py")
    ]
    : [path.join(sourceDir, ".runtime-venv", "bin", "mineru")], "MinerU entrypoint");

  const service = Array.isArray(manifest.services)
    ? manifest.services.find((item) => item && (item.id === "pdfmath" || item.default !== false))
    : null;
  expectFile(path.join(sourceDir, service?.command || ""), "PDF2 service command");
  expectFile(path.join(sourceDir, manifest.structuredParser?.command || ""), "structured parser command");
  expectFile(path.join(sourceDir, "services", "pdf", "server.py"), "PDF2 server");
}

async function checkModels() {
  const models = manifest.models && typeof manifest.models === "object" ? manifest.models : {};
  const modelRoot = path.join(sourceDir, models.path || "modelscope-cache/models/OpenDataLab/PDF-Extract-Kit-1___0/models");
  expectDir(modelRoot, "models");
  const requiredModelPaths = Array.isArray(models.requiredPaths)
    ? models.requiredPaths.map(String).filter(Boolean)
    : [];
  for (const relativePath of requiredModelPaths) {
    const target = path.join(modelRoot, relativePath);
    if (!fs.existsSync(target)) {
      findings.push({ level: "error", message: `missing required model path: ${target}` });
    }
  }
  if (fs.existsSync(modelRoot)) {
    const modelBytes = await directorySize(modelRoot);
    const minimumBytes = Number(models.minimumBytes) || 850 * 1024 * 1024;
    if (modelBytes < minimumBytes) {
      findings.push({
        level: "error",
        message: `models are too small for formula recognition: ${formatBytes(modelBytes)} < ${formatBytes(minimumBytes)}`
      });
    }
    const suspiciousModelNames = await findNames(modelRoot, [
      /MinerU2\.5-Pro/i,
      /TabRec/i,
      /SlanetPlus/i,
      /train/i,
      /checkpoint/i
    ]);
    for (const item of suspiciousModelNames.slice(0, 20)) {
      findings.push({ level: "warn", message: `review nonessential model artifact: ${path.relative(sourceDir, item)}` });
    }
  }
}

async function checkGpuArtifacts() {
  const gpuArtifacts = await findNames(sourceDir, [
    /cuda/i,
    /cudnn/i,
    /nvidia/i,
    /onnxruntime_gpu/i,
    /torch.*cu\d+/i,
    /paddle.*gpu/i
  ]);
  for (const item of gpuArtifacts.slice(0, 40)) {
    const stat = await fs.promises.stat(item).catch(() => null);
    if (!stat?.isFile()) continue;
    const extension = path.extname(item).toLowerCase();
    if (![".dll", ".dylib", ".pyd", ".so", ".whl"].includes(extension)) continue;
    findings.push({ level: "error", message: `GPU/CUDA artifact should not be in CPU package: ${path.relative(sourceDir, item)}` });
  }
}

function expectFile(filePath, label) {
  if (!filePath || !fs.existsSync(filePath) || !fs.statSync(filePath).isFile()) {
    findings.push({ level: "error", message: `missing ${label}: ${filePath}` });
  }
}

function expectAnyFile(filePaths, label) {
  if (!filePaths.some((filePath) => filePath && fs.existsSync(filePath) && fs.statSync(filePath).isFile())) {
    findings.push({ level: "error", message: `missing ${label}: ${filePaths.join(" or ")}` });
  }
}

function expectDir(dirPath, label) {
  if (!dirPath || !fs.existsSync(dirPath) || !fs.statSync(dirPath).isDirectory()) {
    findings.push({ level: "error", message: `missing ${label}: ${dirPath}` });
  }
}

async function topLevelSizes(root) {
  const entries = await fs.promises.readdir(root, { withFileTypes: true }).catch(() => []);
  const sizes = [];
  for (const entry of entries) {
    const filePath = path.join(root, entry.name);
    const bytes = entry.isDirectory()
      ? await directorySize(filePath)
      : (await fs.promises.stat(filePath).catch(() => ({ size: 0 }))).size;
    sizes.push({ name: entry.name, bytes });
  }
  return sizes.sort((a, b) => b.bytes - a.bytes);
}

async function directorySize(root) {
  let total = 0;
  const entries = await fs.promises.readdir(root, { withFileTypes: true }).catch(() => []);
  for (const entry of entries) {
    const filePath = path.join(root, entry.name);
    if (entry.isDirectory()) {
      total += await directorySize(filePath);
    } else if (entry.isFile()) {
      const stat = await fs.promises.stat(filePath).catch(() => null);
      if (stat) total += stat.size;
    }
  }
  return total;
}

async function findNames(root, patterns) {
  const matches = [];
  await walk(root, async (filePath) => {
    const relative = path.relative(root, filePath);
    if (patterns.some((pattern) => pattern.test(relative))) {
      matches.push(filePath);
    }
  });
  return matches;
}

async function walk(root, visitor) {
  const entries = await fs.promises.readdir(root, { withFileTypes: true }).catch(() => []);
  for (const entry of entries) {
    const filePath = path.join(root, entry.name);
    await visitor(filePath);
    if (entry.isDirectory()) {
      await walk(filePath, visitor);
    }
  }
}

function normalizePlatform(platform) {
  if (platform === "darwin") return "macos";
  if (platform === "win32") return "windows";
  return "linux";
}

function numberEnv(name, fallback) {
  const value = Number(process.env[name]);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

function formatBytes(bytes) {
  const value = Number(bytes) || 0;
  if (value < 1024) return `${value} B`;
  const units = ["KB", "MB", "GB", "TB"];
  let size = value / 1024;
  let index = 0;
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024;
    index += 1;
  }
  return `${size >= 10 ? size.toFixed(1) : size.toFixed(2)} ${units[index]}`;
}
