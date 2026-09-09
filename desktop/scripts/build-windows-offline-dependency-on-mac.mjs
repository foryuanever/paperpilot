import fs from "node:fs";
import path from "node:path";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const desktopRoot = path.resolve(__dirname, "..");
const uvBin = path.resolve(process.env.PAPER_SOLVER_UV_BINARY || path.join(desktopRoot, "local-dependency-source", "tools", "uv"));
const workRoot = path.resolve(process.env.PAPER_SOLVER_WINDOWS_BUILD_ROOT || path.join(desktopRoot, "scratch", "windows-cross"));
const runtimeDir = path.join(workRoot, ".runtime-venv");
const modelDir = path.resolve(process.env.PAPER_SOLVER_DEPENDENCY_MODEL_DIR || path.join(desktopRoot, "scratch", "offline-models", "PDF-Extract-Kit-1___0"));
const sourceDir = path.join(workRoot, "offline-dependency-source-windows-x64");
const releaseDir = path.join(workRoot, "offline-release");
const pythonUrl = process.env.PAPER_SOLVER_WINDOWS_PYTHON_URL || "https://releases.astral.sh/github/python-build-standalone/releases/download/20260623/cpython-3.12.13%2B20260623-x86_64-pc-windows-msvc-install_only_stripped.tar.gz";
const vcRedistUrl = process.env.PAPER_SOLVER_WINDOWS_VC_REDIST_URL || "https://aka.ms/vs/17/release/vc_redist.x64.exe";
const vcRedistPath = path.join(workRoot, "vc_redist.x64.exe");

if (process.platform !== "darwin") {
  throw new Error("This helper is intended for building the Windows package from macOS.");
}
if (!fs.existsSync(uvBin)) {
  throw new Error(`uv not found: ${uvBin}`);
}
if (!fs.existsSync(modelDir)) {
  throw new Error(`Offline models not found: ${modelDir}. Run npm run prepare:models first.`);
}

await fs.promises.rm(workRoot, { recursive: true, force: true });
await fs.promises.mkdir(workRoot, { recursive: true });

await prepareWindowsPython();
await downloadWindowsVcRuntime();
await installWindowsPackages(path.join(desktopRoot, "dependency-service", "pdf", "requirements.txt"));
await installWindowsPackages(path.join(desktopRoot, "dependency-service", "structured", "requirements.txt"));
await writeRuntimeMarkers();
await cleanupRuntime(runtimeDir);
await prepareDependencySource();
await auditDependencySource();
await packageDependencySource();

async function prepareWindowsPython() {
  const archivePath = path.join(workRoot, "python-windows.tar.gz");
  await run("curl", ["-L", "--fail", "--retry", "3", "-o", archivePath, pythonUrl]);
  await run("tar", ["-xzf", archivePath, "-C", workRoot]);
  const extracted = path.join(workRoot, "python");
  if (!fs.existsSync(path.join(extracted, "python.exe"))) {
    throw new Error("Windows Python archive did not contain python.exe.");
  }
  await fs.promises.rename(extracted, runtimeDir);
  await fs.promises.mkdir(path.join(runtimeDir, "Lib", "site-packages"), { recursive: true });
}

async function downloadWindowsVcRuntime() {
  await run("curl", ["-L", "--fail", "--retry", "3", "-o", vcRedistPath, vcRedistUrl]);
  const stat = await fs.promises.stat(vcRedistPath).catch(() => null);
  if (!stat || stat.size < 10 * 1024 * 1024) {
    throw new Error(`Downloaded VC++ redistributable looks incomplete: ${vcRedistPath}`);
  }
}

async function installWindowsPackages(requirementsPath) {
  await run(uvBin, [
    "pip", "install",
    "--target", path.join(runtimeDir, "Lib", "site-packages"),
    "--python-version", "3.12",
    "--python-platform", "x86_64-pc-windows-msvc",
    "--only-binary", ":all:",
    "-r", requirementsPath,
    "--index-url", process.env.UV_INDEX_URL || "https://pypi.tuna.tsinghua.edu.cn/simple",
    "--extra-index-url", "https://pypi.org/simple"
  ]);
}

async function writeRuntimeMarkers() {
  await fs.promises.mkdir(path.join(runtimeDir, "Scripts"), { recursive: true });
  await fs.promises.writeFile(path.join(runtimeDir, ".papersolver-pdf-ready"), new Date().toISOString(), "utf8");
  await fs.promises.writeFile(path.join(runtimeDir, ".papersolver-structured-ready"), new Date().toISOString(), "utf8");
}

async function prepareDependencySource() {
  await run("npm", ["run", "prepare:dependency"], {
    cwd: desktopRoot,
    env: {
      ...process.env,
      PAPER_SOLVER_DEPENDENCY_PLATFORM: "windows",
      PAPER_SOLVER_DEPENDENCY_ARCH: "x64",
      PAPER_SOLVER_DEPENDENCY_VENV_DIR: runtimeDir,
      PAPER_SOLVER_DEPENDENCY_MODEL_DIR: modelDir,
      PAPER_SOLVER_DEPENDENCY_PDF_SERVICE_DIR: path.join(desktopRoot, "dependency-service", "pdf"),
      PAPER_SOLVER_DEPENDENCY_ENABLE_STRUCTURED_BOOTSTRAP: "1",
      PAPER_SOLVER_DEPENDENCY_OFFLINE_READY: "1",
      PAPER_SOLVER_DEPENDENCY_WINDOWS_VC_REDIST: vcRedistPath,
      PAPER_SOLVER_DEPENDENCY_SOURCE: sourceDir
    }
  });
}

async function auditDependencySource() {
  await run("npm", ["run", "audit:dependency"], {
    cwd: desktopRoot,
    env: {
      ...process.env,
      PAPER_SOLVER_DEPENDENCY_PLATFORM: "windows",
      PAPER_SOLVER_DEPENDENCY_ARCH: "x64",
      PAPER_SOLVER_DEPENDENCY_SOURCE: sourceDir
    }
  });
}

async function packageDependencySource() {
  await run("npm", ["run", "build:dependency"], {
    cwd: desktopRoot,
    env: {
      ...process.env,
      PAPER_SOLVER_DEPENDENCY_PLATFORM: "windows",
      PAPER_SOLVER_DEPENDENCY_ARCH: "x64",
      PAPER_SOLVER_DEPENDENCY_SOURCE: sourceDir,
      PAPER_SOLVER_DEPENDENCY_OUTPUT: releaseDir
    }
  });
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
    const lower = filePath.replace(/\\/g, "/").toLowerCase();
    if (entry.isDirectory()) {
      if (removableNames.has(name) || lower.includes("/site-packages/torch/test/")) {
        await fs.promises.rm(filePath, { recursive: true, force: true }).catch(() => {});
      }
      return;
    }
    if (entry.isFile() && /\.(pyc|pyo|log|tmp|temp)$/i.test(name)) {
      await fs.promises.rm(filePath, { force: true }).catch(() => {});
      return;
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

function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      stdio: "inherit",
      cwd: options.cwd || desktopRoot,
      env: options.env || {
        ...process.env,
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
