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

连接设置里也可以填写本机翻译服务地址：

```text
DeepLX:          http://127.0.0.1:1188
LibreTranslate: http://127.0.0.1:5000
MTranServer:    http://127.0.0.1:8989
```

这些服务地址只保存在用户电脑本机。填了地址后，对应翻译引擎才会在翻译列表里显示为可用；实际翻译请求由 Electron 主进程从用户电脑直接发出，不经过后端。

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

## 当前第一阶段设计

- 桌面端复用 Web 前端。
- 登录、聊天大厅、AI 调用、管理员后台仍连接云端后端。
- Zotero 本机同步已经通过 Electron 主进程读取用户电脑上的 `http://127.0.0.1:23119`。
- Zotero 导入会先同步题录，再自动读取 Zotero PDF 附件并保存到桌面端本机缓存，不默认上传服务器。
- 桌面端会把 Zotero PDF 和用户手动关联的 PDF 保存在本机，后端只记录 `desktop-cache://workspaceId` 这类本机 PDF 状态标记；登录页连接设置里可以查看占用、打开缓存目录或清理 PDF 缓存。
- 阅读器、对照阅读和 AI 问答会优先读取本机缓存。AI 问答的聊天消息只保存在当前页面内存中，刷新后消失；论文笔记、标注、解析结果等仍按原逻辑保存。
- 翻译引擎池已扩展到 Google 网页、Google(API)、百度、有道、DeepLX、DeepL、必应、CNKI、火山网页、腾讯 TranSmart、海词、LibreTranslate、MTranServer、微软、腾讯；其中 Google 网页无需密钥，百度/DeepL/微软/腾讯/API 类需要密钥，DeepLX/LibreTranslate/MTranServer 需要本机或自部署服务 endpoint。
- 桌面端的 Google 网页翻译、DeepLX、LibreTranslate、MTranServer 会通过 Electron 主进程从用户电脑发起请求，不再占用后端 `/api/translate`。
- 不在用户电脑里运行 MySQL，也不把 AI Key 放进客户端。
