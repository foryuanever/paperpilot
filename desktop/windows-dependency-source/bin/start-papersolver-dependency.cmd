@echo off
setlocal
set ROOT_DIR=%~dp0..
set PORT=%PAPER_SOLVER_PORT%
if "%PORT%"=="" set PORT=11008
set VENV_DIR=%ROOT_DIR%\.runtime-venv
set PYTHON_BIN=%VENV_DIR%\Scripts\python.exe
set UV_BIN=%ROOT_DIR%\tools\uv.exe

if not exist "%PYTHON_BIN%" (
  if exist "%UV_BIN%" (
    "%UV_BIN%" python install 3.12
    "%UV_BIN%" venv --python 3.12 "%VENV_DIR%"
  ) else (
    python -m venv "%VENV_DIR%"
  )
)
if not exist "%VENV_DIR%\.papersolver-pdf-ready" (
  if exist "%UV_BIN%" (
    "%UV_BIN%" pip install --python "%PYTHON_BIN%" -r "%ROOT_DIR%\services\pdf\requirements.txt"
  ) else (
    "%PYTHON_BIN%" -m pip install --upgrade pip
    "%PYTHON_BIN%" -m pip install -r "%ROOT_DIR%\services\pdf\requirements.txt"
  )
  echo ready > "%VENV_DIR%\.papersolver-pdf-ready"
)

set PAPER_SOLVER_MODELS_DIR=%ROOT_DIR%\models
"%PYTHON_BIN%" "%ROOT_DIR%\services\pdf\server.py" --host 127.0.0.1 --port %PORT%
