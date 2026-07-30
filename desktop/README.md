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

连接设置里会检测 PaperSolver 本机依赖包。依赖包只安装在用户电脑本机，用于沉浸翻译、PDF 解析和对照阅读；实际处理从 Electron 主进程发起，不把 PDF 原文传到后端。对照翻译会优先调用本机依赖包生成双语 PDF，并把结果缓存到用户电脑。

当前阶段推荐把依赖包内置进 DMG，不需要额外下载地址。后续如果想把 DMG 做小，也可以再配置备用下载地址。

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

## 本机依赖包

客户端支持安装 PaperSolver 本机依赖包。默认会优先使用 DMG 内置的依赖 zip；只有内置包不存在时，才尝试使用备用下载地址。依赖包是一个 zip/tar.gz，内部至少需要：

```text
papersolver-dependency.json
bin/start-papersolver-dependency
```

`papersolver-dependency.json` 示例见 `dependency-template/`。真实依赖准备好后，可以这样打包：

```bash
cd desktop
PAPER_SOLVER_DEPENDENCY_SOURCE=/path/to/your/local-dependency npm run build:dependency
```

如果依赖还没有整理成标准目录，可以先生成标准源目录：

```bash
cd desktop
PAPER_SOLVER_DEPENDENCY_RUNTIME_DIR=/path/to/portable-runtime \
PAPER_SOLVER_DEPENDENCY_PDF_SERVICE_DIR=/path/to/pdf-service \
PAPER_SOLVER_DEPENDENCY_STRUCTURED_PARSER=/path/to/mineru \
PAPER_SOLVER_DEPENDENCY_MODEL_DIR=/path/to/models \
PAPER_SOLVER_DEPENDENCY_SOURCE=/path/to/output-local-dependency \
npm run prepare:dependency
```

注意：不要直接把当前开发机上的 Python venv 粗暴打包给用户。普通 venv 往往包含开发机绝对路径，例如 `/opt/miniconda3` 或用户目录，换到其他电脑会启动失败。正式依赖包需要使用可迁移 runtime 或独立服务目录。

产物会生成到：

```text
desktop/release/dependencies/papersolver-local-dependency-macos-arm64.zip
```

如果不想把依赖包内置进客户端，也可以把 zip 上传到下载服务器后，打包客户端时配置备用下载地址：

```bash
PAPER_SOLVER_DEPENDENCY_URL=https://你的下载域名/papersolver-local-dependency-macos-arm64.zip npm run dist:mac
```

当前阶段推荐使用内置依赖包，不需要 CDN 或 OSS。

## 当前第一阶段设计

- 桌面端复用 Web 前端。
- 登录、聊天大厅、AI 调用、管理员后台仍连接云端后端。
- Zotero 本机同步已经通过 Electron 主进程读取用户电脑上的 `http://127.0.0.1:23119`。
- Zotero 导入会先同步题录，再自动读取 Zotero PDF 附件并保存到桌面端本机缓存，不默认上传服务器。
- 桌面端会把 Zotero PDF 和用户手动关联的 PDF 保存在本机，后端只记录 `desktop-cache://workspaceId` 这类本机 PDF 状态标记；登录页连接设置里可以查看占用、打开缓存目录或清理 PDF 缓存。
- 阅读器、对照阅读和 AI 问答会优先读取本机缓存。AI 问答的聊天消息只保存在当前页面内存中，刷新后消失；论文笔记、标注、解析结果等仍按原逻辑保存。
- 翻译引擎列表只展示当前可用的引擎；未配置或不可用的本机服务不会显示给用户。
- 桌面端的划词翻译会优先通过 Electron 主进程从用户电脑发起请求，不再占用后端 `/api/translate`。
- 桌面端对照翻译会优先读取本机 PDF 缓存并调用 PaperSolver 本机依赖包，生成的双语 PDF 同样保存在本机缓存目录，避免把大 PDF 和重翻译任务压到 2 核 4G 服务器上。
- 不在用户电脑里运行 MySQL，也不把 AI Key 放进客户端。
