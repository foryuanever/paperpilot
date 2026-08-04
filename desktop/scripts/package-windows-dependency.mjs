import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const desktopRoot = path.resolve(__dirname, "..");
const winSourceDir = path.join(desktopRoot, "windows-dependency-source");
const releaseDir = path.join(desktopRoot, "release", "dependencies");

async function main() {
  console.log("Preparing Windows dependency source...");
  await fs.promises.rm(winSourceDir, { recursive: true, force: true }).catch(() => {});
  await fs.promises.mkdir(path.join(winSourceDir, "bin"), { recursive: true });
  await fs.promises.mkdir(path.join(winSourceDir, "tools"), { recursive: true });
  await fs.promises.mkdir(path.join(winSourceDir, "services", "pdf"), { recursive: true });
  await fs.promises.mkdir(path.join(winSourceDir, "services", "structured"), { recursive: true });

  // 1. Copy uv.exe from temp-uv-win/uv.exe
  const tempUvExe = path.join(desktopRoot, "temp-uv-win", "uv.exe");
  if (!fs.existsSync(tempUvExe)) {
    throw new Error("Missing Windows uv.exe at " + tempUvExe);
  }
  await fs.promises.copyFile(tempUvExe, path.join(winSourceDir, "tools", "uv.exe"));

  // 2. Copy Python service files from desktop/dependency-service
  const pdfServerPy = path.join(desktopRoot, "dependency-service", "pdf", "server.py");
  const pdfReqs = path.join(desktopRoot, "dependency-service", "pdf", "requirements.txt");
  const structuredReqs = path.join(desktopRoot, "dependency-service", "structured", "requirements.txt");

  await fs.promises.copyFile(pdfServerPy, path.join(winSourceDir, "services", "pdf", "server.py"));
  await fs.promises.copyFile(pdfReqs, path.join(winSourceDir, "services", "pdf", "requirements.txt"));
  await fs.promises.copyFile(structuredReqs, path.join(winSourceDir, "services", "structured", "requirements.txt"));

  // 3. Write mineru.cmd
  const mineruCmd = `@echo off
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
  await fs.promises.writeFile(path.join(winSourceDir, "bin", "mineru.cmd"), mineruCmd, "utf8");

  // 4. Write start-papersolver-dependency.cmd
  const startCmd = `@echo off
setlocal
set ROOT_DIR=%~dp0..
set PORT=%PAPER_SOLVER_PORT%
if "%PORT%"=="" set PORT=11008
set VENV_DIR=%ROOT_DIR%\\.runtime-venv
set PYTHON_BIN=%VENV_DIR%\\Scripts\\python.exe
set UV_BIN=%ROOT_DIR%\\tools\\uv.exe

if not exist "%PYTHON_BIN%" (
  if exist "%UV_BIN%" (
    "%UV_BIN%" python install 3.12
    "%UV_BIN%" venv --python 3.12 "%VENV_DIR%"
  ) else (
    python -m venv "%VENV_DIR%"
  )
)
if not exist "%VENV_DIR%\\.papersolver-pdf-ready" (
  if exist "%UV_BIN%" (
    "%UV_BIN%" pip install --python "%PYTHON_BIN%" -r "%ROOT_DIR%\\services\\pdf\\requirements.txt"
  ) else (
    "%PYTHON_BIN%" -m pip install --upgrade pip
    "%PYTHON_BIN%" -m pip install -r "%ROOT_DIR%\\services\\pdf\\requirements.txt"
  )
  echo ready > "%VENV_DIR%\\.papersolver-pdf-ready"
)

set PAPER_SOLVER_MODELS_DIR=%ROOT_DIR%\\models
"%PYTHON_BIN%" "%ROOT_DIR%\\services\\pdf\\server.py" --host 127.0.0.1 --port %PORT%
`;
  await fs.promises.writeFile(path.join(winSourceDir, "bin", "start-papersolver-dependency.cmd"), startCmd, "utf8");

  // 5. Write papersolver-dependency.json
  const manifest = {
    name: "PaperSolver Local Dependency",
    version: "0.1.0",
    services: [
      {
        id: "pdfmath",
        command: "bin/start-papersolver-dependency.cmd",
        args: [],
        cwd: ".",
        healthUrl: "http://127.0.0.1:11008"
      }
    ],
    structuredParser: {
      command: "bin/mineru.cmd"
    }
  };
  await fs.promises.writeFile(path.join(winSourceDir, "papersolver-dependency.json"), JSON.stringify(manifest, null, 2), "utf8");

  // 6. Zip the directory to releaseDir/papersolver-local-dependency-windows-x64.zip
  console.log("Zipping Windows dependency package...");
  const outputPath = path.join(releaseDir, "papersolver-local-dependency-windows-x64.zip");
  await fs.promises.mkdir(releaseDir, { recursive: true });
  await fs.promises.rm(outputPath, { force: true });

  const zipResult = spawnSync("zip", ["-r", outputPath, "."], { cwd: winSourceDir });
  if (zipResult.status !== 0) {
    throw new Error("Failed to zip: " + zipResult.stderr.toString());
  }

  console.log("Successfully packaged Windows dependency to: " + outputPath);
}

main().catch(console.error);
