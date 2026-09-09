import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { createHash } from "node:crypto";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const desktopRoot = path.resolve(__dirname, "..");
const templateDir = path.join(desktopRoot, "dependency-template");
const sourceDir = path.resolve(process.env.PAPER_SOLVER_DEPENDENCY_SOURCE || templateDir);
const releaseDir = path.resolve(process.env.PAPER_SOLVER_DEPENDENCY_OUTPUT || path.join(desktopRoot, "release", "dependencies"));
const platformName = process.env.PAPER_SOLVER_DEPENDENCY_PLATFORM || normalizePlatform(process.platform);
const archName = process.env.PAPER_SOLVER_DEPENDENCY_ARCH || normalizeArch(process.arch);
const outputName = `papersolver-local-dependency-${platformName}-${archName}.zip`;
const outputPath = path.join(releaseDir, outputName);
const modelScopeRepo = process.env.PAPER_SOLVER_MODELSCOPE_REPO || "foryuanever/papersolver-dependencies";

await ensureDependencySource(sourceDir);
if (sourceDir === path.resolve(templateDir) && process.env.PAPER_SOLVER_ALLOW_TEMPLATE_DEPENDENCY !== "1") {
  throw new Error("Refusing to package the template dependency. Set PAPER_SOLVER_DEPENDENCY_SOURCE to the real dependency directory.");
}
await fs.promises.mkdir(releaseDir, { recursive: true });
await fs.promises.rm(outputPath, { force: true });
const { root: packageRoot, temporary } = await preparePackageRoot(sourceDir, platformName, archName);
try {
  await createZip(packageRoot, outputPath);
} finally {
  if (temporary) await fs.promises.rm(packageRoot, { recursive: true, force: true }).catch(() => {});
}
const stat = await fs.promises.stat(outputPath);
const sha256 = await fileSha256(outputPath);

console.log(`PaperSolver local dependency package created:`);
console.log(outputPath);
console.log(`Size: ${formatBytes(stat.size)}`);
console.log(`SHA256: ${sha256}`);
console.log("");
console.log("Manifest package item:");
console.log(JSON.stringify({
  id: "runtime-full",
  platforms: [`${platformName}-${archName}`],
  size: stat.size,
  weight: stat.size,
  sha256,
  urls: [`https://modelscope.cn/datasets/${modelScopeRepo}/resolve/master/${outputName}`]
}, null, 2));

async function preparePackageRoot(root, platform, arch) {
  if (platform !== "macos" || arch !== "arm64") return { root, temporary: false };

  // A uv venv normally contains absolute symlinks to the builder's Python and
  // relies on that machine's stdlib. Build a relocatable copy for end users.
  const stagingRoot = await fs.promises.mkdtemp(path.join(os.tmpdir(), "papersolver-macos-dependency-"));
  await fs.promises.cp(root, stagingRoot, { recursive: true, dereference: false });

  const sourcePythonLink = path.join(root, ".runtime-venv", "bin", "python3.12");
  const realPython = await fs.promises.realpath(sourcePythonLink).catch(() => "");
  if (!realPython || !fs.existsSync(realPython)) {
    throw new Error(`Mac runtime is missing the real Python executable: ${sourcePythonLink}`);
  }
  const baseRoot = path.resolve(path.dirname(realPython), "..");
  const baseStdlib = path.join(baseRoot, "lib", "python3.12");
  if (!fs.existsSync(path.join(baseStdlib, "encodings", "__init__.py"))) {
    throw new Error(`Mac runtime is missing the Python standard library: ${baseStdlib}`);
  }
  if (!fs.existsSync(path.join(baseStdlib, "os.py"))) {
    throw new Error(`Mac runtime is missing the Python standard library module os.py: ${baseStdlib}`);
  }

  const runtimeRoot = path.join(stagingRoot, ".runtime-venv");
  const runtimeBin = path.join(runtimeRoot, "bin");
  const runtimeLib = path.join(runtimeRoot, "lib", "python3.12");
  await fs.promises.mkdir(runtimeBin, { recursive: true });
  await fs.promises.mkdir(runtimeLib, { recursive: true });
  for (const entry of await fs.promises.readdir(baseStdlib, { withFileTypes: true })) {
    if (entry.name === "site-packages") continue;
    const source = path.join(baseStdlib, entry.name);
    const target = path.join(runtimeLib, entry.name);
    await fs.promises.rm(target, { recursive: true, force: true }).catch(() => {});
    await fs.promises.cp(source, target, { recursive: true, dereference: true });
  }
  for (const name of ["python", "python3", "python3.12"]) {
    await fs.promises.rm(path.join(runtimeBin, name), { force: true }).catch(() => {});
  }
  await fs.promises.copyFile(realPython, path.join(runtimeBin, "python3.12"));
  await fs.promises.symlink("python3.12", path.join(runtimeBin, "python"));
  await fs.promises.symlink("python", path.join(runtimeBin, "python3"));
  const cfgPath = path.join(runtimeRoot, "pyvenv.cfg");
  const cfg = await fs.promises.readFile(cfgPath, "utf8").catch(() => "include-system-site-packages = false\nversion_info = 3.12\n");
  await fs.promises.writeFile(cfgPath, cfg.replace(/^home = .*$/m, "home = ."), "utf8");

  const stdlibMarker = path.join(runtimeLib, "encodings", "__init__.py");
  if (!fs.existsSync(stdlibMarker)) throw new Error("Failed to bundle the Mac Python standard library.");
  if (!fs.existsSync(path.join(runtimeLib, "os.py"))) throw new Error("Failed to bundle the Mac Python standard library module os.py.");
  return { root: stagingRoot, temporary: true };
}

function normalizePlatform(platform) {
  if (platform === "darwin") return "macos";
  if (platform === "win32") return "windows";
  return "linux";
}

function normalizeArch(arch) {
  return arch === "arm64" ? "arm64" : "x64";
}

async function ensureDependencySource(root) {
  const manifestPath = path.join(root, "papersolver-dependency.json");
  const manifestRaw = await fs.promises.readFile(manifestPath, "utf8").catch(() => "");
  if (!manifestRaw) {
    throw new Error(`Missing dependency manifest: ${manifestPath}`);
  }
  let manifest;
  try {
    manifest = JSON.parse(manifestRaw);
  } catch (error) {
    throw new Error(`Invalid dependency manifest JSON: ${error.message}`);
  }
  if (!Array.isArray(manifest.services) || manifest.services.length === 0) {
    throw new Error("Dependency manifest must include at least one service.");
  }
  for (const service of manifest.services) {
    if (!service?.id || !service?.command) {
      throw new Error("Each dependency service must include id and command.");
    }
    const commandPath = path.join(root, service.command);
    if (!fs.existsSync(commandPath)) {
      throw new Error(`Dependency service command not found: ${commandPath}`);
    }
    if (process.platform !== "win32") {
      await fs.promises.chmod(commandPath, 0o755).catch(() => {});
    }
  }
  if (manifest.offlineReady === true || String(manifest.installMode || "").toLowerCase() === "offline") {
    await ensureOfflineDependencySource(root, manifest);
  } else if (process.env.PAPER_SOLVER_ALLOW_BOOTSTRAP_DEPENDENCY !== "1") {
    throw new Error("Refusing to package a bootstrap dependency. Release packages must set offlineReady=true.");
  }
}

async function ensureOfflineDependencySource(root, manifest) {
  const isWindowsTarget = String(process.env.PAPER_SOLVER_DEPENDENCY_PLATFORM || "").toLowerCase() === "windows" || process.platform === "win32";
  const pythonPath = firstExistingPath(isWindowsTarget
    ? [
      path.join(root, ".runtime-venv", "python.exe"),
      path.join(root, ".runtime-venv", "Scripts", "python.exe")
    ]
    : [path.join(root, ".runtime-venv", "bin", "python")]);
  if (!fs.existsSync(pythonPath)) {
    throw new Error(`Offline dependency package is missing Python runtime: ${pythonPath}`);
  }
  if (isWindowsTarget) {
    const stdlibRoot = path.join(root, ".runtime-venv", "Lib");
    for (const requiredModule of ["encodings/__init__.py", "os.py"]) {
      if (!fs.existsSync(path.join(stdlibRoot, requiredModule))) {
        throw new Error(`Offline dependency package is missing Python standard library module: ${path.join(stdlibRoot, requiredModule)}`);
      }
    }
    const vcRedistPath = path.join(root, "tools", "vc_redist.x64.exe");
    if (!fs.existsSync(vcRedistPath)) {
      throw new Error(`Offline Windows dependency package is missing Microsoft VC++ runtime installer: ${vcRedistPath}`);
    }
    const appLocalMsvcp = path.join(root, ".runtime-venv", "msvcp140.dll");
    if (!fs.existsSync(appLocalMsvcp)) {
      throw new Error(`Offline Windows dependency package is missing app-local MSVCP140 runtime: ${appLocalMsvcp}`);
    }
  }
  const service = manifest.services.find((item) => item && (item.id === "pdfmath" || item.default !== false));
  const serviceCommand = service?.command ? path.join(root, service.command) : "";
  if (!serviceCommand || !fs.existsSync(serviceCommand)) {
    throw new Error("Offline dependency package is missing the PDF service command.");
  }
  const parserCommand = manifest.structuredParser?.command ? path.join(root, manifest.structuredParser.command) : "";
  if (!parserCommand || !fs.existsSync(parserCommand)) {
    throw new Error("Offline dependency package is missing the structured parser command.");
  }
  const mineruEntry = firstExistingPath(isWindowsTarget
    ? [
      path.join(root, ".runtime-venv", "Scripts", "mineru.exe"),
      path.join(root, ".runtime-venv", "Lib", "site-packages", "mineru", "cli", "client.py")
    ]
    : [path.join(root, ".runtime-venv", "bin", "mineru")]);
  if (!fs.existsSync(mineruEntry)) {
    throw new Error(`Offline dependency package is missing the MinerU entrypoint: ${mineruEntry}`);
  }
  const modelRoot = path.join(root, manifest.models?.path || "modelscope-cache/models/OpenDataLab/PDF-Extract-Kit-1___0/models");
  if (!fs.existsSync(modelRoot)) {
    throw new Error(`Offline dependency package is missing models: ${modelRoot}`);
  }
  const requiredModelPaths = Array.isArray(manifest.models?.requiredPaths)
    ? manifest.models.requiredPaths.map(String).filter(Boolean)
    : [];
  for (const relativePath of requiredModelPaths) {
    const target = path.join(modelRoot, relativePath);
    if (!fs.existsSync(target)) {
      throw new Error(`Offline dependency package is missing required model path: ${target}`);
    }
  }
  const minimumBytes = Number(manifest.models?.minimumBytes) || 850 * 1024 * 1024;
  const modelBytes = await directorySize(modelRoot);
  if (modelBytes < minimumBytes) {
    throw new Error(`Offline dependency models are too small: ${formatBytes(modelBytes)} < ${formatBytes(minimumBytes)}`);
  }
}

function firstExistingPath(candidates) {
  return candidates.find((candidate) => fs.existsSync(candidate)) || candidates[0];
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

async function createZip(root, target) {
  if (process.platform === "darwin") {
    await run("ditto", ["-c", "-k", "--norsrc", ".", target], { cwd: root });
    return;
  }
  if (process.platform === "win32") {
    await run("powershell.exe", [
      "-NoProfile",
      "-ExecutionPolicy", "Bypass",
      "-Command",
      [
        "Add-Type -AssemblyName System.IO.Compression.FileSystem;",
        `$source = ${JSON.stringify(path.resolve(root))};`,
        `$target = ${JSON.stringify(path.resolve(target))};`,
        "if (Test-Path $target) { Remove-Item -LiteralPath $target -Force; }",
        "[System.IO.Compression.ZipFile]::CreateFromDirectory($source, $target, [System.IO.Compression.CompressionLevel]::Optimal, $false);"
      ].join(" ")
    ]);
    return;
  }
  await run("zip", ["-r", target, "."], { cwd: root });
}

function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      stdio: "inherit",
      ...options
    });
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) resolve();
      else reject(new Error(`${command} exited with code ${code}`));
    });
  });
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

function fileSha256(filePath) {
  return new Promise((resolve, reject) => {
    const hash = createHash("sha256");
    const stream = fs.createReadStream(filePath);
    stream.on("data", (chunk) => hash.update(chunk));
    stream.on("error", reject);
    stream.on("end", () => resolve(hash.digest("hex")));
  });
}
