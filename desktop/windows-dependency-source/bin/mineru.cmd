@echo off
setlocal
set ROOT_DIR=%~dp0..
set VENV_DIR=%ROOT_DIR%\.runtime-venv
set PYTHON_BIN=%VENV_DIR%\Scripts\python.exe
set UV_BIN=%ROOT_DIR%\tools\uv.exe
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
if not exist "%VENV_DIR%\.papersolver-structured-ready" (
  if exist "%UV_BIN%" (
    "%UV_BIN%" pip install --python "%PYTHON_BIN%" -r "%ROOT_DIR%\services\structured\requirements.txt"
  ) else (
    "%PYTHON_BIN%" -m pip install --upgrade pip
    "%PYTHON_BIN%" -m pip install -r "%ROOT_DIR%\services\structured\requirements.txt"
  )
  echo ready > "%VENV_DIR%\.papersolver-structured-ready"
)
"%VENV_DIR%\Scripts\mineru.exe" %*
