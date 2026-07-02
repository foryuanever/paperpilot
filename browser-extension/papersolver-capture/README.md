# PaperSolver Capture

由 yuan 设计。

这个浏览器插件配合 PaperSolver 的官网检索模式使用：

1. 在 PaperSolver 搜索页选择文献源并点击“检索文献”。
2. PaperSolver 跳转到对应官网检索结果页。
3. 你在官网打开论文详情页或 PDF 页面。
4. 插件自动识别 DOI、标题、作者、PDF 链接，并在右下角提示“导入 PaperSolver”。

## 安装

Chrome / Edge:

1. 打开 `chrome://extensions` 或 `edge://extensions`。
2. 开启“开发者模式”。
3. 点击“加载已解压的扩展程序”。
4. 选择目录：`browser-extension/papersolver-capture`。

## 配置

默认后端地址：

```text
http://127.0.0.1:8080
```

如果后端地址不同，点击浏览器工具栏里的 PaperSolver Capture 图标，在弹窗里修改。

## 支持识别

- arXiv、ACL Anthology、PubMed、Semantic Scholar、ScienceDirect 等论文详情页
- 直接打开的 PDF 页面
- 页面里的 `citation_*` 元数据
- DOI 文本
- PDF 下载链接
