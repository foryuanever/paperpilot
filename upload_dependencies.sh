#!/bin/bash
echo "========================================="
echo "📤 开始上传 PaperSolver 本机依赖包到服务器"
echo "========================================="

# 1. 建立临时安全通道
echo "🔑 正在建立 SSH 连接通道，请扫描微信二维码并输入密码："
ssh -M -S /tmp/ssh_mux -o ControlPath=/tmp/ssh_mux -o ControlPersist=5m -o IdentitiesOnly=yes -fN root@106.53.136.108

# 确认通道是否成功建立
if [ ! -S /tmp/ssh_mux ]; then
    echo "❌ SSH 通道建立失败，上传终止。"
    exit 1
fi

# 2. 创建服务器上的依赖目录
echo "📁 正在服务器创建依赖存储目录..."
ssh -o ControlPath=/tmp/ssh_mux root@106.53.136.108 "mkdir -p /www/wwwroot/papersolver/downloads/dependencies"

# 3. 上传 Windows x64 依赖包
WIN_ZIP="/Users/yuan/Desktop/Solve Paper/desktop/release/dependencies/papersolver-local-dependency-windows-x64.zip"
if [ -f "$WIN_ZIP" ]; then
    echo "📦 正在上传 Windows x64 依赖包 (约 11.9MB)..."
    scp -o ControlPath=/tmp/ssh_mux "$WIN_ZIP" root@106.53.136.108:/www/wwwroot/papersolver/downloads/dependencies/
    echo "  ✅ Windows 依赖包上传完成"
else
    echo "  ❌ 未找到 Windows 依赖包，请先在本地 desktop 目录下打包依赖"
fi

# 4. 上传 macOS arm64 依赖包
MAC_ZIP="/Users/yuan/Desktop/Solve Paper/desktop/release/dependencies/papersolver-local-dependency-macos-arm64.zip"
if [ -f "$MAC_ZIP" ]; then
    echo "📦 正在上传 macOS arm64 依赖包 (约 21.0MB)..."
    scp -o ControlPath=/tmp/ssh_mux "$MAC_ZIP" root@106.53.136.108:/www/wwwroot/papersolver/downloads/dependencies/
    echo "  ✅ macOS 依赖包上传完成"
else
    echo "  ❌ 未找到 macOS 依赖包"
fi

# 5. 关闭临时安全通道
ssh -O exit -S /tmp/ssh_mux root@106.53.136.108

echo "========================================="
echo "✨ 依赖包部署成功！"
echo "========================================="
