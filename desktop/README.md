# PaperSolver Desktop

这个目录是独立的桌面端工程，只负责 Electron 外壳和安装包构建，不把 Web 前端代码复制进来。

## 目录关系

- `../front`：现有 Vue 前端，继续作为唯一 UI 源码。
- `../backend`：现有 Spring Boot 后端，继续作为云端 API。
- `./desktop`：Electron 桌面壳和打包配置。
- `./build/icon.png`：桌面端应用图标源图，已生成 `icon.icns` 和 `icon.ico` 供 macOS / Windows 打包使用。

## 本地启动

第一次进入桌面端目录后安装依赖：

```bash
cd desktop
npm install
```

启动桌面端开发版：

```bash
npm run dev
```

默认后端 API 地址是：

```text
http://127.0.0.1:8080
```

桌面端登录弹窗中有“连接设置”，可以在不重新打包的情况下切换到线上后端，例如：

```text
https://api.papersolver.cn
```

连接设置里可以先点“测试连接”，确认后端可达后再保存。

连接设置里会检测 PaperSolver 本机能力。本机能力只安装在用户电脑本机，用于沉浸翻译、PDF 解析、公式识别和对照阅读；实际处理从 Electron 主进程发起，不把 PDF 原文传到后端。对照翻译会优先调用本机能力生成双语 PDF，并把结果缓存到用户电脑。

当前阶段使用统一安装路线：客户端读取一个很小的安装清单，再按清单安装完整本机能力。清单由 `papersolver.cn` 承载，大文件建议放在 ModelScope 数据集直链。

桌面端不会在后端不可达时自动进入本地 demo 模式；登录、聊天大厅、AI 调用等能力都需要后端连接成功。

如果要连接线上后端：

```bash
PAPER_SOLVER_API_BASE=https://你的后端域名 npm run dev
```

## 打包

macOS：

```bash
PAPER_SOLVER_API_BASE=https://你的后端域名 npm run dist:mac
```

Windows：

```bash
PAPER_SOLVER_API_BASE=https://你的后端域名 npm run dist:win
```

生成文件会在：

```text
desktop/release/
```

更多下载分发说明见 [DISTRIBUTION.md](./DISTRIBUTION.md)。

## 本机能力

客户端支持安装 PaperSolver 本机能力。默认会先读取 `PAPER_SOLVER_DEPENDENCY_MANIFEST` 指向的安装清单，清单不可用时才使用兼容旧版的单包地址。上线包必须是完整离线能力包，用户安装后只做解压、校验和启动自检，不再现场安装 Python 依赖。本机能力包内部至少需要：

```text
papersolver-dependency.json
bin/start-papersolver-dependency
.runtime-venv/
.runtime-venv/bin/mineru
services/pdf/server.py
bin/mineru
models/
```

`papersolver-dependency.json` 需要声明 `offlineReady: true`、`installMode: "offline"`、`requiredCapabilities: ["pdf2", "structured", "formula"]` 和模型目录校验规则。真实依赖准备好后，可以这样打包：

```bash
cd desktop
PAPER_SOLVER_DEPENDENCY_SOURCE=/path/to/your/local-dependency npm run build:dependency
```

如果依赖还没有整理成标准目录，可以先生成标准源目录。这里传入的是已经预构建好的可迁移 Python 环境和模型目录，不是让用户电脑再安装：

```bash
cd desktop
PAPER_SOLVER_MODEL_SOURCE_DIR="$HOME/.cache/modelscope/hub/models/OpenDataLab/PDF-Extract-Kit-1___0" npm run prepare:models
npm run prepare:runtime

PAPER_SOLVER_DEPENDENCY_VENV_DIR=/path/to/.runtime-venv \
PAPER_SOLVER_DEPENDENCY_PDF_SERVICE_DIR=/path/to/pdf-service \
PAPER_SOLVER_DEPENDENCY_STRUCTURED_SERVICE_DIR=/path/to/structured-service \
PAPER_SOLVER_DEPENDENCY_STRUCTURED_PARSER=/path/to/mineru \
PAPER_SOLVER_DEPENDENCY_MODEL_DIR=/path/to/models \
PAPER_SOLVER_DEPENDENCY_OFFLINE_READY=1 \
PAPER_SOLVER_DEPENDENCY_SOURCE=/path/to/output-local-dependency \
npm run prepare:dependency
```

本机实测 macOS arm64 离线包：原始目录约 `1.77 GB`，压缩后约 `1.19 GB`。其中精简模型约 `1010.5 MB`，包含 Layout、OCR、公式识别 MFR 和 MinerU 的小型表格模型；不包含 MinerU2.5-Pro。解析运行时与主包合并，不再重复下载第二套 PyTorch。

2026-08-17 按 beta.73 发布物重新核算（均为二进制 GiB）：

| 平台 | 桌面安装包 | 主依赖下载 | 解析补充包 | 首次完整下载 | 依赖解压上限 |
| --- | ---: | ---: | ---: | ---: | ---: |
| macOS arm64 | 0.10 GiB | 1.19 GiB | 已合并 | 1.19 GiB | 1.77 GiB |
| Windows x64 | 0.09 GiB | 1.45 GiB | 已合并 | 1.45 GiB | 3.00 GiB |

“依赖解压上限”是主 ZIP 的解压体积；合并后不再把同一套 PyTorch 运行时重复计算。PPT Master 在服务器运行，不进入用户本机依赖包。

Windows 包需要在 Windows x64 机器上生成，不能复用 macOS 的 `.runtime-venv`。模型文件是跨平台的，可以复用 `scratch/offline-models/PDF-Extract-Kit-1___0`，但 runtime 必须在 Windows 重新构建：

```powershell
cd desktop

npm run prepare:runtime

$env:PAPER_SOLVER_DEPENDENCY_VENV_DIR = "$PWD\scratch\offline-runtime\.runtime-venv"
$env:PAPER_SOLVER_DEPENDENCY_MODEL_DIR = "$PWD\scratch\offline-models\PDF-Extract-Kit-1___0"
$env:PAPER_SOLVER_DEPENDENCY_PDF_SERVICE_DIR = "$PWD\dependency-service\pdf"
$env:PAPER_SOLVER_DEPENDENCY_ENABLE_STRUCTURED_BOOTSTRAP = "1"
$env:PAPER_SOLVER_DEPENDENCY_OFFLINE_READY = "1"
$env:PAPER_SOLVER_DEPENDENCY_SOURCE = "$PWD\scratch\offline-dependency-source-windows-x64"
npm run prepare:dependency

$env:PAPER_SOLVER_DEPENDENCY_SOURCE = "$PWD\scratch\offline-dependency-source-windows-x64"
$env:PAPER_SOLVER_DEPENDENCY_PLATFORM = "windows"
$env:PAPER_SOLVER_DEPENDENCY_ARCH = "x64"
npm run audit:dependency

$env:PAPER_SOLVER_DEPENDENCY_OUTPUT = "$PWD\scratch\offline-release"
npm run build:dependency
```

如果 Windows 机器没有 `scratch/offline-models/PDF-Extract-Kit-1___0`，先把 macOS 已生成的这个目录复制过去；或者在 Windows 上配置 `PAPER_SOLVER_MODEL_SOURCE_DIR` 指向 ModelScope 缓存里的 `PDF-Extract-Kit-1___0` 后运行 `npm run prepare:models`。

打包前先审计，确认 PDF2、结构化解析、公式识别、CPU 运行环境都在包里，并检查是否误带 GPU/CUDA 依赖：

```bash
cd desktop
PAPER_SOLVER_DEPENDENCY_SOURCE=/path/to/output-local-dependency npm run audit:dependency
PAPER_SOLVER_DEPENDENCY_SOURCE=/path/to/output-local-dependency npm run build:dependency
```

注意：不要直接把当前开发机上的 Python venv 粗暴打包给用户。普通 venv 往往包含开发机绝对路径，例如 `/opt/miniconda3` 或用户目录，换到其他电脑会启动失败。正式能力包需要使用可迁移 runtime 或独立服务目录。

产物会生成到：

```text
desktop/release/dependencies/papersolver-local-dependency-macos-arm64.zip
```

如果不想把本机能力内置进客户端，正式发布建议使用安装清单：

```bash
PAPER_SOLVER_DEPENDENCY_MANIFEST=https://papersolver.cn/downloads/dependencies/dependency-manifest.json npm run dist:mac
```

清单示例见 `desktop/dependency-manifest.example.json`。大文件 URL 可以放到 ModelScope 数据集直链，`papersolver.cn` 只承载清单和校验值；客户端安装页只展示“本机阅读环境、版面解析能力、文字识别能力、公式识别能力”等产品进度，不展示下载平台、包名或模型细节。

## 当前第一阶段设计

- 桌面端复用 Web 前端。
- 登录、聊天大厅、AI 调用、管理员后台仍连接云端后端。
- Zotero 本机同步已经通过 Electron 主进程读取用户电脑上的 `http://127.0.0.1:23119`。
- Zotero 导入会先同步题录，再自动读取 Zotero PDF 附件并保存到桌面端本机缓存，不默认上传服务器。
- 桌面端会把 Zotero PDF 和用户手动关联的 PDF 保存在本机，后端只记录 `desktop-cache://workspaceId` 这类本机 PDF 状态标记；登录页连接设置里可以查看占用、打开缓存目录或清理 PDF 缓存。
- 阅读器、对照阅读和 AI 问答会优先读取本机缓存。AI 问答的聊天消息只保存在当前页面内存中，刷新后消失；论文笔记、标注、解析结果等仍按原逻辑保存。
- 翻译引擎列表只展示当前可用的引擎；未配置或不可用的本机服务不会显示给用户。
- 桌面端的划词翻译会优先通过 Electron 主进程从用户电脑发起请求，不再占用后端 `/api/translate`。
- 桌面端对照翻译会优先读取本机 PDF 缓存并调用 PaperSolver 本机能力，生成的双语 PDF 同样保存在本机缓存目录，避免把大 PDF 和重翻译任务压到 2 核 4G 服务器上。
- 不在用户电脑里运行 MySQL，也不把 AI Key 放进客户端。
