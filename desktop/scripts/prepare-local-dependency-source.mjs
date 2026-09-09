import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const desktopRoot = path.resolve(__dirname, "..");
const outputDir = path.resolve(process.env.PAPER_SOLVER_DEPENDENCY_SOURCE || path.join(desktopRoot, "local-dependency-source"));
const targetPlatform = normalizePlatform(process.env.PAPER_SOLVER_DEPENDENCY_PLATFORM || process.platform);
const targetIsWin = targetPlatform === "windows";

const venvDir = envPath("PAPER_SOLVER_DEPENDENCY_VENV_DIR");
const runtimeDir = envPath("PAPER_SOLVER_DEPENDENCY_RUNTIME_DIR");
const uvBinary = envPath("PAPER_SOLVER_DEPENDENCY_UV_BINARY");
const pdfServiceDir = envPath("PAPER_SOLVER_DEPENDENCY_PDF_SERVICE_DIR");
const structuredServiceDir = envPath("PAPER_SOLVER_DEPENDENCY_STRUCTURED_SERVICE_DIR");
const structuredParserPath = envPath("PAPER_SOLVER_DEPENDENCY_STRUCTURED_PARSER");
const modelDir = envPath("PAPER_SOLVER_DEPENDENCY_MODEL_DIR");
const windowsVcRedist = envPath("PAPER_SOLVER_DEPENDENCY_WINDOWS_VC_REDIST");
const enableStructuredBootstrap = isTruthy(process.env.PAPER_SOLVER_DEPENDENCY_ENABLE_STRUCTURED_BOOTSTRAP);
const offlineReady = isTruthy(process.env.PAPER_SOLVER_DEPENDENCY_OFFLINE_READY) || Boolean(venvDir && modelDir);

if (!venvDir && !runtimeDir && !pdfServiceDir && !structuredParserPath && !structuredServiceDir && !enableStructuredBootstrap) {
  throw new Error([
    "No dependency inputs provided.",
    "Set at least one of:",
    "  PAPER_SOLVER_DEPENDENCY_VENV_DIR",
    "  PAPER_SOLVER_DEPENDENCY_RUNTIME_DIR",
    "  PAPER_SOLVER_DEPENDENCY_PDF_SERVICE_DIR",
    "  PAPER_SOLVER_DEPENDENCY_STRUCTURED_SERVICE_DIR",
    "  PAPER_SOLVER_DEPENDENCY_STRUCTURED_PARSER",
    "  PAPER_SOLVER_DEPENDENCY_ENABLE_STRUCTURED_BOOTSTRAP=1"
  ].join("\n"));
}

await fs.promises.rm(outputDir, { recursive: true, force: true });
await fs.promises.mkdir(path.join(outputDir, "bin"), { recursive: true });

if (venvDir) {
  await copyInto(venvDir, path.join(outputDir, ".runtime-venv"));
  if (targetIsWin) await promoteWindowsCppRuntime(outputDir);
}
if (runtimeDir) {
  await copyInto(runtimeDir, path.join(outputDir, "runtime"));
}
if (uvBinary) {
  const target = path.join(outputDir, "tools", targetIsWin ? "uv.exe" : "uv");
  await copyInto(uvBinary, target);
  await makeExecutable(target);
}
if (pdfServiceDir) {
  await copyInto(pdfServiceDir, path.join(outputDir, "services", "pdf"));
}
if (structuredServiceDir) {
  await copyInto(structuredServiceDir, path.join(outputDir, "services", "structured"));
}
if (enableStructuredBootstrap) {
  const defaultStructuredServiceDir = path.join(desktopRoot, "dependency-service", "structured");
  await copyInto(defaultStructuredServiceDir, path.join(outputDir, "services", "structured"));
  await writeStructuredParserBootstrap(outputDir);
}
if (modelDir) {
  const modelTargetRoot = path.join(outputDir, "modelscope-cache", "models", "OpenDataLab", "PDF-Extract-Kit-1___0");
  if (fs.existsSync(path.join(modelDir, "models"))) {
    await copyInto(modelDir, modelTargetRoot);
  } else {
    await copyInto(modelDir, path.join(modelTargetRoot, "models"));
  }
}
if (targetIsWin && windowsVcRedist) {
  await copyInto(windowsVcRedist, path.join(outputDir, "tools", "vc_redist.x64.exe"));
}
if (structuredParserPath) {
  const target = path.join(outputDir, "bin", targetIsWin ? "mineru.exe" : "mineru");
  await copyInto(structuredParserPath, target);
  await makeExecutable(target);
}

await writeStartScript(outputDir);
await writeManifest(outputDir);

console.log("PaperSolver dependency source prepared:");
console.log(outputDir);
console.log("");
console.log("Next:");
console.log(`PAPER_SOLVER_DEPENDENCY_SOURCE="${outputDir}" npm run build:dependency`);

function envPath(name) {
  const value = String(process.env[name] || "").trim();
  if (!value) return "";
  return path.resolve(value);
}

function isTruthy(value) {
  return ["1", "true", "yes", "on"].includes(String(value || "").trim().toLowerCase());
}

function normalizePlatform(platform) {
  const value = String(platform || "").trim().toLowerCase();
  if (value === "win32" || value === "win" || value === "windows") return "windows";
  if (value === "darwin" || value === "mac" || value === "macos") return "macos";
  return value || "linux";
}

async function copyInto(from, to) {
  const stat = await fs.promises.stat(from).catch(() => null);
  if (!stat) throw new Error(`Dependency input not found: ${from}`);
  await fs.promises.mkdir(path.dirname(to), { recursive: true });
  await fs.promises.cp(from, to, {
    recursive: true,
    force: true,
    dereference: false,
    filter: (source) => {
      const name = path.basename(source);
      const lower = source.replace(/\\/g, "/").toLowerCase();
      if ([
        ".DS_Store",
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
      ].includes(name)) {
        return false;
      }
      if (/\.(pyc|pyo|log|tmp|temp)$/i.test(name)) return false;
      if (lower.includes("/site-packages/torch/test/")) return false;
      if (lower.includes("/site-packages/torchvision/datasets/")) return false;
      if (lower.includes("/site-packages/paddle/test/")) return false;
      if (lower.includes("/site-packages/cv2/data/")) return false;
      // Keep torch/cuda and torch/backends/cudnn: these are Python packages
      // imported by CPU-only PyTorch too. Only remove optional NVIDIA payloads.
      if (lower.includes("/site-packages/nvidia/")) return false;
      return true;
    }
  });
}

async function promoteWindowsCppRuntime(root) {
  const venvRoot = path.join(root, ".runtime-venv");
  const runtimeDlls = [
    {
      target: "msvcp140.dll",
      pattern: /^msvcp140(?:-[^.]+)?\.dll$/i
    }
  ];
  const sourceDirs = [
    path.join(venvRoot, "Lib", "site-packages", "numpy.libs"),
    path.join(venvRoot, "Lib", "site-packages", "torch", "lib")
  ];
  for (const runtime of runtimeDlls) {
    const targetPath = path.join(venvRoot, runtime.target);
    if (fs.existsSync(targetPath)) continue;
    for (const sourceDir of sourceDirs) {
      const entries = await fs.promises.readdir(sourceDir).catch(() => []);
      const sourceName = entries.find((name) => runtime.pattern.test(name));
      if (!sourceName) continue;
      await fs.promises.copyFile(path.join(sourceDir, sourceName), targetPath);
      break;
    }
    if (!fs.existsSync(targetPath)) {
      throw new Error(`Windows dependency is missing native runtime DLL ${runtime.target}.`);
    }
  }
}

async function writeStartScript(root) {
  if (targetIsWin) {
    const scriptPath = path.join(root, "bin", "start-papersolver-dependency.cmd");
    const script = `@echo off
setlocal
set ROOT_DIR=%~dp0..
set PORT=%PAPER_SOLVER_PORT%
if "%PORT%"=="" set PORT=11008
set PYTHON_BIN=%ROOT_DIR%\\.runtime-venv\\python.exe
if not exist "%PYTHON_BIN%" set PYTHON_BIN=%ROOT_DIR%\\.runtime-venv\\Scripts\\python.exe
set PAPER_SOLVER_MODELS_DIR=%ROOT_DIR%\\modelscope-cache\\models\\OpenDataLab\\PDF-Extract-Kit-1___0\\models
set MODELSCOPE_CACHE=%ROOT_DIR%\\modelscope-cache
set MODELSCOPE_HUB_FILE_LOCK=false
set PYTHONUNBUFFERED=1

if exist "%PYTHON_BIN%" if exist "%ROOT_DIR%\\services\\pdf\\server.py" (
  "%PYTHON_BIN%" "%ROOT_DIR%\\services\\pdf\\server.py" --host 127.0.0.1 --port %PORT%
  exit /b %ERRORLEVEL%
)

echo PaperSolver local dependency package is incomplete: missing .runtime-venv or services\\pdf\\server.py 1>&2
exit /b 1
`;
    await fs.promises.writeFile(scriptPath, script, "utf8");
    return;
  }

  const scriptPath = path.join(root, "bin", "start-papersolver-dependency");
  const script = `#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
PORT="\${PAPER_SOLVER_PORT:-11008}"

  if [ -x "$ROOT_DIR/.runtime-venv/bin/python" ] && [ -f "$ROOT_DIR/services/pdf/server.py" ]; then
  export PAPER_SOLVER_MODELS_DIR="$ROOT_DIR/modelscope-cache/models/OpenDataLab/PDF-Extract-Kit-1___0/models"
  export MODELSCOPE_CACHE="$ROOT_DIR/modelscope-cache"
  export MODELSCOPE_HUB_FILE_LOCK=false
  exec "$ROOT_DIR/.runtime-venv/bin/python" "$ROOT_DIR/services/pdf/server.py" --host 127.0.0.1 --port "$PORT"
fi

if [ -x "$ROOT_DIR/services/pdf/start" ]; then
  exec "$ROOT_DIR/services/pdf/start" "$PORT"
fi

if [ -x "$ROOT_DIR/runtime/bin/python" ] && [ -f "$ROOT_DIR/services/pdf/server.py" ]; then
  export PAPER_SOLVER_MODELS_DIR="$ROOT_DIR/models"
  exec "$ROOT_DIR/runtime/bin/python" "$ROOT_DIR/services/pdf/server.py" --host 127.0.0.1 --port "$PORT"
fi

echo "PaperSolver local dependency package is incomplete: missing services/pdf/start or services/pdf/server.py" >&2
exit 1
`;
  await fs.promises.writeFile(scriptPath, script, "utf8");
  await makeExecutable(scriptPath);
}

async function writeStructuredParserBootstrap(root) {
  const scriptPath = path.join(root, "bin", targetIsWin ? "mineru.cmd" : "mineru");
if (targetIsWin) {
    const script = `@echo off
setlocal
set ROOT_DIR=%~dp0..
set VENV_DIR=%ROOT_DIR%\\.runtime-venv
set PYTHON_BIN=%VENV_DIR%\\python.exe
if not exist "%PYTHON_BIN%" set PYTHON_BIN=%VENV_DIR%\\Scripts\\python.exe
set UV_BIN=%ROOT_DIR%\\tools\\uv.exe
set PIP_DISABLE_PIP_VERSION_CHECK=1
set PYTHONUNBUFFERED=1
set PAPER_SOLVER_MODELS_DIR=%ROOT_DIR%\\modelscope-cache\\models\\OpenDataLab\\PDF-Extract-Kit-1___0\\models
set MODELSCOPE_CACHE=%ROOT_DIR%\\modelscope-cache
set MODELSCOPE_HUB_FILE_LOCK=false
if "%MINERU_MODEL_SOURCE%"=="" set MINERU_MODEL_SOURCE=modelscope
if exist "%VENV_DIR%\\Scripts\\mineru.exe" (
  "%VENV_DIR%\\Scripts\\mineru.exe" %*
  exit /b %ERRORLEVEL%
)
if exist "%VENV_DIR%\\Lib\\site-packages\\mineru\\cli\\client.py" (
  "%PYTHON_BIN%" -m mineru.cli.client %*
  exit /b %ERRORLEVEL%
)
if not exist "%PYTHON_BIN%" (
  if exist "%UV_BIN%" (
    "%UV_BIN%" python install 3.12
    "%UV_BIN%" venv --python 3.12 "%VENV_DIR%"
  ) else (
    python -m venv "%VENV_DIR%"
  )
)
if not exist "%VENV_DIR%\\.papersolver-structured-ready" (
  if exist "%UV_BIN%" (
    "%UV_BIN%" pip install --python "%PYTHON_BIN%" -r "%ROOT_DIR%\\services\\structured\\requirements.txt"
  ) else (
    "%PYTHON_BIN%" -m pip install --upgrade pip
    "%PYTHON_BIN%" -m pip install -r "%ROOT_DIR%\\services\\structured\\requirements.txt"
  )
  echo ready > "%VENV_DIR%\\.papersolver-structured-ready"
)
"%VENV_DIR%\\Scripts\\mineru.exe" %*
`;
    await fs.promises.writeFile(scriptPath, script, "utf8");
    return;
  }
  const script = `#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
VENV_DIR="$ROOT_DIR/.runtime-venv"
PYTHON_BIN="$VENV_DIR/bin/python"
UV_BIN="$ROOT_DIR/tools/uv"

export PIP_DISABLE_PIP_VERSION_CHECK=1
export PYTHONUNBUFFERED=1
export PAPER_SOLVER_MODELS_DIR="\${PAPER_SOLVER_MODELS_DIR:-$ROOT_DIR/modelscope-cache/models/OpenDataLab/PDF-Extract-Kit-1___0/models}"
export MODELSCOPE_CACHE="\${MODELSCOPE_CACHE:-$ROOT_DIR/modelscope-cache}"
export MODELSCOPE_HUB_FILE_LOCK=false
export MINERU_MODEL_SOURCE="\${MINERU_MODEL_SOURCE:-modelscope}"

if [ -x "$VENV_DIR/bin/mineru" ]; then
  exec "$VENV_DIR/bin/mineru" "$@"
fi

if [ ! -x "$PYTHON_BIN" ]; then
  if [ -x "$UV_BIN" ]; then
    "$UV_BIN" python install 3.12
    "$UV_BIN" venv --python 3.12 "$VENV_DIR"
  elif command -v python3 >/dev/null 2>&1; then
    python3 -m venv "$VENV_DIR"
  else
    echo "PaperSolver dependency needs Python runtime." >&2
    exit 1
  fi
fi

if [ ! -f "$VENV_DIR/.papersolver-structured-ready" ]; then
  if [ -x "$UV_BIN" ]; then
    "$UV_BIN" pip install --python "$PYTHON_BIN" -r "$ROOT_DIR/services/structured/requirements.txt"
  else
    "$PYTHON_BIN" -m pip install --upgrade pip
    "$PYTHON_BIN" -m pip install -r "$ROOT_DIR/services/structured/requirements.txt"
  fi
  date > "$VENV_DIR/.papersolver-structured-ready"
fi

exec "$VENV_DIR/bin/mineru" "$@"
`;
  await fs.promises.writeFile(scriptPath, script, "utf8");
  await makeExecutable(scriptPath);
}

async function writeManifest(root) {
  const structuredCommand = targetIsWin ? "bin/mineru.cmd" : "bin/mineru";
  const serviceCommand = targetIsWin ? "bin/start-papersolver-dependency.cmd" : "bin/start-papersolver-dependency";
  const manifest = {
    name: "PaperSolver Local Dependency",
    version: String(process.env.PAPER_SOLVER_DEPENDENCY_VERSION || "0.1.0"),
    installMode: offlineReady ? "offline" : "bootstrap",
    offlineReady,
    requiredCapabilities: ["pdf2", "structured", "formula"],
    models: {
      path: "modelscope-cache/models/OpenDataLab/PDF-Extract-Kit-1___0/models",
      minimumBytes: Number(process.env.PAPER_SOLVER_DEPENDENCY_MIN_MODEL_BYTES) || 850 * 1024 * 1024
    },
    services: [
      {
        id: "pdfmath",
        command: serviceCommand,
        args: [],
        cwd: ".",
        healthUrl: "http://127.0.0.1:11008"
      }
    ],
  };
  if (fs.existsSync(path.join(root, structuredCommand))) {
    manifest.structuredParser = { command: structuredCommand };
  }
  await fs.promises.writeFile(path.join(root, "papersolver-dependency.json"), JSON.stringify(manifest, null, 2), "utf8");
}

async function makeExecutable(filePath) {
  if (targetIsWin) return;
  await fs.promises.chmod(filePath, 0o755).catch(() => {});
}
