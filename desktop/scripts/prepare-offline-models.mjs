import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const desktopRoot = path.resolve(__dirname, "..");
const defaultSource = path.join(process.env.HOME || "", ".cache", "modelscope", "hub", "models", "OpenDataLab", "PDF-Extract-Kit-1___0");
const sourceRoot = path.resolve(process.env.PAPER_SOLVER_MODEL_SOURCE_DIR || defaultSource);
const outputRoot = path.resolve(process.env.PAPER_SOLVER_MODEL_OUTPUT_DIR || path.join(desktopRoot, "scratch", "offline-models", "PDF-Extract-Kit-1___0"));

const requiredDirs = [
  "models/Layout",
  "models/OCR",
  "models/MFR",
  // MinerU's pipeline runtime is already shipped in the main package. Keep
  // its small table models beside the other offline models so no second
  // parser runtime archive is needed.
  "models/TabCls",
  "models/TabRec"
];
const excludedPatterns = [
  /(^|\/)TabRec(\/|$)/i,
  /(^|\/)TabCls(\/|$)/i,
  /MinerU2\.5-Pro/i,
  /SlanetPlus/i,
  /checkpoint/i,
  /train/i
];

await ensureSource();
await fs.promises.rm(outputRoot, { recursive: true, force: true });
await fs.promises.mkdir(outputRoot, { recursive: true });

for (const metadata of [".mdl", ".msc", ".mv"]) {
  const sourcePath = path.join(sourceRoot, metadata);
  if (fs.existsSync(sourcePath)) {
    await fs.promises.copyFile(sourcePath, path.join(outputRoot, metadata));
  }
}

for (const relativeDir of requiredDirs) {
  await copyFiltered(path.join(sourceRoot, relativeDir), path.join(outputRoot, relativeDir));
}

const totalBytes = await directorySize(outputRoot);
const formulaBytes = await directorySize(path.join(outputRoot, "models", "MFR"));
const layoutBytes = await directorySize(path.join(outputRoot, "models", "Layout"));
const ocrBytes = await directorySize(path.join(outputRoot, "models", "OCR"));

if (formulaBytes < 700 * 1024 * 1024) {
  throw new Error(`Formula model looks incomplete: ${formatBytes(formulaBytes)}`);
}

console.log("PaperSolver offline models prepared:");
console.log(outputRoot);
console.log(`Total: ${formatBytes(totalBytes)}`);
console.log(`Layout: ${formatBytes(layoutBytes)}`);
console.log(`OCR: ${formatBytes(ocrBytes)}`);
console.log(`Formula: ${formatBytes(formulaBytes)}`);
console.log("");
console.log("Next:");
console.log(`PAPER_SOLVER_DEPENDENCY_MODEL_DIR="${outputRoot}" npm run prepare:dependency`);

async function ensureSource() {
  if (!fs.existsSync(sourceRoot)) {
    throw new Error(`PDF-Extract-Kit source not found: ${sourceRoot}`);
  }
  for (const relativeDir of requiredDirs) {
    const target = path.join(sourceRoot, relativeDir);
    if (!fs.existsSync(target)) {
      throw new Error(`Required model directory not found: ${target}`);
    }
  }
}

async function copyFiltered(from, to) {
  await fs.promises.mkdir(path.dirname(to), { recursive: true });
  await fs.promises.cp(from, to, {
    recursive: true,
    force: true,
    dereference: false,
    filter: (source) => {
      const relative = path.relative(sourceRoot, source);
      if (excludedPatterns.some((pattern) => pattern.test(relative))) return false;
      const name = path.basename(source);
      if ([".DS_Store", "__pycache__", ".cache", ".git"].includes(name)) return false;
      if (/\.(pyc|pyo|log|tmp|temp)$/i.test(name)) return false;
      return true;
    }
  });
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
