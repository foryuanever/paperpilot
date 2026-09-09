import fs from "node:fs";
import path from "node:path";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const desktopRoot = path.resolve(__dirname, "..");
const uvBin = resolveUvBinary();
const outputDir = path.resolve(process.env.PAPER_SOLVER_RUNTIME_OUTPUT_DIR || path.join(desktopRoot, "scratch", "offline-runtime", ".runtime-venv"));
const pdfRequirements = path.join(desktopRoot, "dependency-service", "pdf", "requirements.txt");
const structuredRequirements = path.join(desktopRoot, "dependency-service", "structured", "requirements.txt");
const pythonBin = path.join(outputDir, process.platform === "win32" ? "Scripts" : "bin", process.platform === "win32" ? "python.exe" : "python");

if (uvBin !== "uv" && !fs.existsSync(uvBin)) {
  throw new Error(`uv not found: ${uvBin}`);
}

await fs.promises.rm(outputDir, { recursive: true, force: true });
await fs.promises.mkdir(path.dirname(outputDir), { recursive: true });

await run(uvBin, ["python", "install", "3.12"]);
await run(uvBin, ["venv", "--python", "3.12", outputDir]);
await installRequirements(pdfRequirements);
await installRequirements(structuredRequirements);
await verifyRuntime();
await cleanupRuntime(outputDir);

const totalBytes = await directorySize(outputDir);
console.log("PaperSolver offline runtime prepared:");
console.log(outputDir);
console.log(`Size: ${formatBytes(totalBytes)}`);
console.log("");
console.log("Next:");
console.log(`PAPER_SOLVER_DEPENDENCY_VENV_DIR="${outputDir}" npm run prepare:dependency`);

async function installRequirements(requirementsPath) {
  await run(uvBin, [
    "pip", "install",
    "--python", pythonBin,
    "-r", requirementsPath,
    "--index-url", process.env.UV_INDEX_URL || "https://pypi.tuna.tsinghua.edu.cn/simple",
    "--extra-index-url", "https://pypi.org/simple"
  ]);
}

async function verifyRuntime() {
  await run(pythonBin, ["-c", [
    "from pdf2zh import translate_stream",
    "from tencentcloud.tmt.v20180321.models import TextTranslateRequest",
    "import mineru",
    "import onnxruntime",
    "print('runtime imports ok')"
  ].join("; ")]);
  const mineruEntry = path.join(outputDir, process.platform === "win32" ? "Scripts" : "bin", process.platform === "win32" ? "mineru.exe" : "mineru");
  if (!fs.existsSync(mineruEntry)) {
    throw new Error(`MinerU entrypoint not found: ${mineruEntry}`);
  }
}

async function cleanupRuntime(root) {
  const removableNames = new Set([
    "__pycache__",
    ".pytest_cache",
    ".mypy_cache",
    ".ruff_cache",
    ".cache",
    "tests",
    "test",
    "docs",
    "doc",
    "examples",
    "example",
    "samples",
    "sample"
  ]);
  await walk(root, async (filePath, entry) => {
    const name = entry.name;
    const lower = filePath.toLowerCase();
    if (entry.isDirectory()) {
      if (removableNames.has(name) || lower.includes("/site-packages/torch/test/")) {
        await fs.promises.rm(filePath, { recursive: true, force: true }).catch(() => {});
      }
      return;
    }
    if (entry.isFile() && /\.(pyc|pyo|log|tmp|temp)$/i.test(name)) {
      await fs.promises.rm(filePath, { force: true }).catch(() => {});
    }
  });
}

async function walk(root, visitor) {
  const entries = await fs.promises.readdir(root, { withFileTypes: true }).catch(() => []);
  for (const entry of entries) {
    const filePath = path.join(root, entry.name);
    await visitor(filePath, entry);
    if (entry.isDirectory() && fs.existsSync(filePath)) {
      await walk(filePath, visitor);
    }
  }
}

function run(command, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      stdio: "inherit",
      env: {
        ...process.env,
        UV_PYTHON_INSTALL_MIRROR: process.env.UV_PYTHON_INSTALL_MIRROR || "https://mirror.nju.edu.cn/github-release/astral-sh/python-build-standalone/",
        UV_INDEX_URL: process.env.UV_INDEX_URL || "https://pypi.tuna.tsinghua.edu.cn/simple",
        PIP_DISABLE_PIP_VERSION_CHECK: "1"
      }
    });
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) resolve();
      else reject(new Error(`${command} exited with code ${code}`));
    });
  });
}

function resolveUvBinary() {
  if (process.env.PAPER_SOLVER_UV_BINARY) {
    return path.resolve(process.env.PAPER_SOLVER_UV_BINARY);
  }
  const fileName = process.platform === "win32" ? "uv.exe" : "uv";
  const candidates = [
    path.join(desktopRoot, "local-dependency-source", "tools", fileName),
    path.join(desktopRoot, "windows-dependency-source", "tools", fileName),
    path.join(desktopRoot, "temp-uv-win", fileName)
  ];
  const found = candidates.find((candidate) => fs.existsSync(candidate));
  return found || "uv";
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
