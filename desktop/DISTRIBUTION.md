# PaperSolver 桌面端分发流程

## 1. 本地测试包

macOS 当前没有 Apple Developer 账号也可以先打测试包：

```bash
cd desktop
PAPER_SOLVER_API_BASE=http://127.0.0.1:8080 npm run dist:mac
```

生成的 `.dmg` 在：

```text
desktop/release/
```

## 2. 正式连接线上后端

先把后端部署到服务器，并准备一个 HTTPS 域名，例如：

```text
https://api.papersolver.cn
```

然后打包：

```bash
cd desktop
PAPER_SOLVER_API_BASE=https://api.papersolver.cn npm run dist:mac
```

Windows：

```bash
PAPER_SOLVER_API_BASE=https://api.papersolver.cn npm run dist:win
```

## 3. 用户下载方式

第一阶段可以把安装包放到：

- 官网下载页
- 阿里云 OSS / 腾讯 COS / Cloudflare R2
- GitHub Releases
- 内测阶段也可以临时用网盘

## 4. 没有签名时的提示

macOS 无 Apple Developer 签名时，用户可能看到“无法验证开发者”。

内测用户打开方式：

1. 打开 `.dmg`。
2. 把 `PaperSolver.app` 拖到 Applications。
3. 如果系统阻止，右键点击应用，选择“打开”。
4. 或到“系统设置 -> 隐私与安全性”里允许打开。

Windows 无代码签名证书时，可能出现 SmartScreen 提示。

内测用户打开方式：

1. 点击“更多信息”。
2. 点击“仍要运行”。

## 5. 当前桌面端架构

- 桌面端只打包 Vue 前端和 Electron 外壳。
- 登录、聊天大厅、AI 调用、管理员后台继续走云端后端。
- 不把数据库、AI Key、模型池密钥打包到用户电脑。
- Zotero 本机同步由 Electron 主进程访问用户本机 Zotero，不再让云端后端访问 `localhost:23119`。
- Zotero PDF 附件会由桌面端从用户电脑读取，再调用云端接口上传到对应文献。
- 本地 PDF 缓存、离线阅读属于下一阶段增强能力。
