import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const desktopRoot = path.resolve(__dirname, "..");
const outputDir = path.resolve(process.env.PAPER_SOLVER_DEPENDENCY_SOURCE || path.join(desktopRoot, "local-dependency-source"));

const runtimeDir = envPath("PAPER_SOLVER_DEPENDENCY_RUNTIME_DIR");
const uvBinary = envPath("PAPER_SOLVER_DEPENDENCY_UV_BINARY");
const pdfServiceDir = envPath("PAPER_SOLVER_DEPENDENCY_PDF_SERVICE_DIR");
const structuredServiceDir = envPath("PAPER_SOLVER_DEPENDENCY_STRUCTURED_SERVICE_DIR");
const structuredParserPath = envPath("PAPER_SOLVER_DEPENDENCY_STRUCTURED_PARSER");
const modelDir = envPath("PAPER_SOLVER_DEPENDENCY_MODEL_DIR");
const enableStructuredBootstrap = isTruthy(process.env.PAPER_SOLVER_DEPENDENCY_ENABLE_STRUCTURED_BOOTSTRAP);

if (!runtimeDir && !pdfServiceDir && !structuredParserPath && !structuredServiceDir && !enableStructuredBootstrap) {
  throw new Error([
    "No dependency inputs provided.",
    "Set at least one of:",
    "  PAPER_SOLVER_DEPENDENCY_RUNTIME_DIR",
    "  PAPER_SOLVER_DEPENDENCY_PDF_SERVICE_DIR",
    "  PAPER_SOLVER_DEPENDENCY_STRUCTURED_SERVICE_DIR",
    "  PAPER_SOLVER_DEPENDENCY_STRUCTURED_PARSER",
    "  PAPER_SOLVER_DEPENDENCY_ENABLE_STRUCTURED_BOOTSTRAP=1"
  ].join("\n"));
}

await fs.promises.rm(outputDir, { recursive: true, force: true });
await fs.promises.mkdir(path.join(outputDir, "bin"), { recursive: true });

if (runtimeDir) {
  await copyInto(runtimeDir, path.join(outputDir, "runtime"));
}
if (uvBinary) {
  const target = path.join(outputDir, "tools", process.platform === "win32" ? "uv.exe" : "uv");
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
  await copyInto(modelDir, path.join(outputDir, "models"));
}
if (structuredParserPath) {
  const target = path.join(outputDir, "bin", process.platform === "win32" ? "mineru.exe" : "mineru");
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
      return ![
        ".DS_Store",
        "__pycache__",
        ".pytest_cache",
        ".mypy_cache",
        ".ruff_cache"
      ].includes(name);
    }
  });
}

async function writeStartScript(root) {
  const scriptPath = path.join(root, "bin", "start-papersolver-dependency");
  const script = `#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
PORT="\${PAPER_SOLVER_PORT:-11008}"

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
  const scriptPath = path.join(root, "bin", process.platform === "win32" ? "mineru.cmd" : "mineru");
  if (process.platform === "win32") {
    const script = `@echo off
setlocal
set ROOT_DIR=%~dp0..
set VENV_DIR=%ROOT_DIR%\\.runtime-venv
set PYTHON_BIN=%VENV_DIR%\\Scripts\\python.exe
set UV_BIN=%ROOT_DIR%\\tools\\uv.exe
set PIP_DISABLE_PIP_VERSION_CHECK=1
set PYTHONUNBUFFERED=1
if "%MINERU_MODEL_SOURCE%"=="" set MINERU_MODEL_SOURCE=modelscope
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
export PAPER_SOLVER_MODELS_DIR="\${PAPER_SOLVER_MODELS_DIR:-$ROOT_DIR/models}"
export MINERU_MODEL_SOURCE="\${MINERU_MODEL_SOURCE:-modelscope}"

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
  const structuredCommand = process.platform === "win32" ? "bin/mineru.cmd" : "bin/mineru";
  const manifest = {
    name: "PaperSolver Local Dependency",
    version: String(process.env.PAPER_SOLVER_DEPENDENCY_VERSION || "0.1.0"),
    services: [
      {
        id: "pdfmath",
        command: "bin/start-papersolver-dependency",
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
  if (process.platform === "win32") return;
  await fs.promises.chmod(filePath, 0o755).catch(() => {});
}
