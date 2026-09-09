param(
  [string]$ModelDir = "",
  [string]$OutputDir = ""
)

$ErrorActionPreference = "Stop"

$DesktopRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $DesktopRoot

if (-not $ModelDir) {
  $ModelDir = Join-Path $DesktopRoot "scratch\offline-models\PDF-Extract-Kit-1___0"
}
if (-not $OutputDir) {
  $OutputDir = Join-Path $DesktopRoot "scratch\offline-release"
}

if (-not (Test-Path $ModelDir)) {
  throw "Model directory not found: $ModelDir. Copy scratch\offline-models\PDF-Extract-Kit-1___0 from macOS or run npm run prepare:models first."
}

npm run prepare:runtime

$VcRedistPath = Join-Path $DesktopRoot "scratch\vc_redist.x64.exe"
Invoke-WebRequest -Uri "https://aka.ms/vs/17/release/vc_redist.x64.exe" -OutFile $VcRedistPath
if ((Get-Item $VcRedistPath).Length -lt 10MB) {
  throw "Downloaded VC++ redistributable looks incomplete: $VcRedistPath"
}

$env:PAPER_SOLVER_DEPENDENCY_VENV_DIR = Join-Path $DesktopRoot "scratch\offline-runtime\.runtime-venv"
$env:PAPER_SOLVER_DEPENDENCY_MODEL_DIR = $ModelDir
$env:PAPER_SOLVER_DEPENDENCY_PDF_SERVICE_DIR = Join-Path $DesktopRoot "dependency-service\pdf"
$env:PAPER_SOLVER_DEPENDENCY_ENABLE_STRUCTURED_BOOTSTRAP = "1"
$env:PAPER_SOLVER_DEPENDENCY_OFFLINE_READY = "1"
$env:PAPER_SOLVER_DEPENDENCY_WINDOWS_VC_REDIST = $VcRedistPath
$env:PAPER_SOLVER_DEPENDENCY_SOURCE = Join-Path $DesktopRoot "scratch\offline-dependency-source-windows-x64"
$env:PAPER_SOLVER_DEPENDENCY_PLATFORM = "windows"
$env:PAPER_SOLVER_DEPENDENCY_ARCH = "x64"
npm run prepare:dependency

npm run audit:dependency

$env:PAPER_SOLVER_DEPENDENCY_OUTPUT = $OutputDir
npm run build:dependency
