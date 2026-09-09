import fs from "node:fs";
import path from "node:path";
import { createHash } from "node:crypto";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const desktopRoot = path.resolve(__dirname, "..");
const uvBin = path.resolve(process.env.PAPER_SOLVER_UV_BINARY || path.join(desktopRoot, "local-dependency-source", "tools", "uv"));
const workRoot = path.resolve(process.env.PAPER_SOLVER_PARSER_SUPPLEMENT_ROOT || path.join(desktopRoot, "scratch", "parser-runtime-supplements"));
const releaseRoot = path.join(workRoot, "release");
const packages = [
  "torch==2.6.0",
  "torchvision==0.21.0",
  "transformers==4.57.3",
  "huggingface-hub==0.36.0",
  "ftfy==6.3.1",
  "wcwidth==0.8.2",
  "pdftext==0.6.3",
  "pypdfium2==4.30.0",
  "tokenizers==0.22.1",
  "safetensors==0.7.0",
  "sympy==1.13.1",
  "mpmath==1.3.0"
];
const targets = [
  {
    platform: "windows",
    arch: "x64",
    pythonPlatform: "x86_64-pc-windows-msvc",
    sitePackages: [".runtime-venv", "Lib", "site-packages"]
  },
  {
    platform: "macos",
    arch: "arm64",
    pythonPlatform: "aarch64-apple-darwin",
    sitePackages: [".runtime-venv", "lib", "python3.12", "site-packages"]
  }
];
const modelSourceRoot = path.resolve(
  process.env.PAPER_SOLVER_PARSER_MODEL_SOURCE
    || path.join(desktopRoot, "scratch", "offline-dependency-source", "modelscope-cache", "models", "OpenDataLab", "PDF-Extract-Kit-1___0")
);
const tableModelFiles = [
  "models/TabCls/paddle_table_cls/PP-LCNet_x1_0_table_cls.onnx",
  "models/TabRec/SlanetPlus/slanet-plus.onnx",
  "models/TabRec/UnetStructure/unet.onnx"
];

if (!fs.existsSync(uvBin)) throw new Error(`uv not found: ${uvBin}`);
await fs.promises.rm(workRoot, { recursive: true, force: true });
await fs.promises.mkdir(releaseRoot, { recursive: true });

const manifestItems = [];
for (const target of targets) {
  manifestItems.push(await buildTarget(target));
}

console.log("\nParser runtime manifest items:");
console.log(JSON.stringify(manifestItems, null, 2));

async function buildTarget(target) {
  const targetName = `${target.platform}-${target.arch}`;
  const sourceRoot = path.join(workRoot, targetName);
  const sitePackages = path.join(sourceRoot, ...target.sitePackages);
  await fs.promises.mkdir(sitePackages, { recursive: true });
  await run(uvBin, [
    "pip", "install",
    "--target", sitePackages,
    "--python-version", "3.12",
    "--python-platform", target.pythonPlatform,
    "--only-binary", ":all:",
    "--no-deps",
    ...packages,
    "--index-url", process.env.UV_INDEX_URL || "https://pypi.tuna.tsinghua.edu.cn/simple",
    "--extra-index-url", "https://pypi.org/simple"
  ]);
  await cleanup(sitePackages);
  await audit(sitePackages, targetName);
  await copyTableModels(sourceRoot, targetName);
  if (target.platform === "windows") await copyWindowsCppRuntime(sourceRoot, targetName);
  await fs.promises.writeFile(path.join(sourceRoot, "papersolver-parser-runtime.json"), JSON.stringify({
    id: "layout-formula-runtime",
    platform: targetName,
    python: "3.12",
    packages,
    createdAt: new Date().toISOString()
  }, null, 2));

  const fileName = `papersolver-parser-runtime-${targetName}.zip`;
  const outputPath = path.join(releaseRoot, fileName);
  await run("ditto", ["-c", "-k", "--norsrc", ".", outputPath], { cwd: sourceRoot });
  await run("unzip", ["-tq", outputPath]);
  const stat = await fs.promises.stat(outputPath);
  const sha256 = await hashFile(outputPath);
  console.log(`${targetName}: ${outputPath}`);
  console.log(`  size=${stat.size} sha256=${sha256}`);
  return {
    id: "layout-formula-runtime",
    platforms: [targetName],
    size: stat.size,
    weight: stat.size,
    sha256,
    urls: [
      `https://modelscope.cn/datasets/foryuanever/papersolver-dependencies/resolve/master/${fileName}`
    ]
  };
}

async function cleanup(root) {
  const removable = new Set(["__pycache__", "tests", "test"]);
  async function visit(current) {
    const entries = await fs.promises.readdir(current, { withFileTypes: true }).catch(() => []);
    for (const entry of entries) {
      const target = path.join(current, entry.name);
      if (entry.isDirectory() && removable.has(entry.name)) {
        await fs.promises.rm(target, { recursive: true, force: true });
      } else if (entry.isDirectory()) {
        await visit(target);
      } else if (/\.(pyc|pyo)$/i.test(entry.name)) {
        await fs.promises.rm(target, { force: true });
      }
    }
  }
  await visit(root);
}

async function audit(sitePackages, targetName) {
  for (const relativePath of [
    ["torch", "__init__.py"],
    ["torchvision", "__init__.py"],
    ["transformers", "__init__.py"],
    ["huggingface_hub", "__init__.py"],
    ["ftfy", "__init__.py"],
    ["wcwidth", "__init__.py"],
    ["pdftext", "pdf", "chars.py"],
    ["pypdfium2", "__init__.py"],
    ["tokenizers", "__init__.py"],
    ["safetensors", "__init__.py"],
    ["sympy", "__init__.py"],
    ["mpmath", "__init__.py"]
  ]) {
    const target = path.join(sitePackages, ...relativePath);
    if (!fs.existsSync(target)) throw new Error(`${targetName} supplement missing ${relativePath.join("/")}`);
  }
  const nativeExtension = targetName.startsWith("windows")
    ? path.join(sitePackages, "torch", "_C.cp312-win_amd64.pyd")
    : path.join(sitePackages, "torch", "_C.cpython-312-darwin.so");
  if (!fs.existsSync(nativeExtension)) throw new Error(`${targetName} supplement missing native torch extension: ${nativeExtension}`);
}

async function copyTableModels(sourceRoot, targetName) {
  const targetModelRoot = path.join(sourceRoot, "modelscope-cache", "models", "OpenDataLab", "PDF-Extract-Kit-1___0");
  for (const relativePath of tableModelFiles) {
    const source = path.join(modelSourceRoot, relativePath);
    if (!fs.existsSync(source)) throw new Error(`${targetName} supplement missing table model source: ${source}`);
    const target = path.join(targetModelRoot, relativePath);
    await fs.promises.mkdir(path.dirname(target), { recursive: true });
    await fs.promises.copyFile(source, target);
  }
}

async function copyWindowsCppRuntime(sourceRoot, targetName) {
  const windowsRuntimeRoot = path.resolve(
    process.env.PAPER_SOLVER_WINDOWS_RUNTIME_SOURCE
      || path.join(desktopRoot, "scratch", "windows-cross", "offline-dependency-source-windows-x64", ".runtime-venv")
  );
  const numpyLibs = path.join(windowsRuntimeRoot, "Lib", "site-packages", "numpy.libs");
  const candidates = await fs.promises.readdir(numpyLibs).catch(() => []);
  const sourceName = candidates.find((name) => /^msvcp140-[^.]+\.dll$/i.test(name));
  if (!sourceName) throw new Error(`${targetName} supplement cannot find MSVCP140 runtime in ${numpyLibs}`);
  await fs.promises.copyFile(path.join(numpyLibs, sourceName), path.join(sourceRoot, ".runtime-venv", "msvcp140.dll"));
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

function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd: options.cwd || desktopRoot,
      env: { ...process.env, ...(options.env || {}) },
      stdio: "inherit"
    });
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) resolve();
      else reject(new Error(`${command} exited with code ${code}`));
    });
  });
}
