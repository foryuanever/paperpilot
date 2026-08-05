#!/bin/bash
echo "========================================="
echo "🚀 开始一键部署 PaperSolver 到远程服务器"
echo "========================================="

# 1. 建立临时安全通道（只需在此步骤扫码并输入密码一次）
echo "🔑 正在建立 SSH 连接通道，请扫描微信二维码并输入密码 @QPalzm1105："
ssh -M -S /tmp/ssh_mux -o ControlPath=/tmp/ssh_mux -o ControlPersist=5m -o IdentitiesOnly=yes -fN root@106.53.136.108

# 确认通道是否成功建立
if [ ! -S /tmp/ssh_mux ]; then
    echo "❌ SSH 通道建立失败，部署终止。"
    exit 1
fi

# 2. 创建服务器上的站点根目录
ssh -o ControlPath=/tmp/ssh_mux root@106.53.136.108 "mkdir -p /www/wwwroot/papersolver/downloads"

# 3. 部署前端静态网页文件 (等同于 scp -r dist/*)
echo "🧹 正在清理服务器旧版静态资源缓存..."
ssh -o ControlPath=/tmp/ssh_mux root@106.53.136.108 "rm -rf /www/wwwroot/papersolver/assets/*"

echo "📤 正在上传前端静态网页..."
scp -o ControlPath=/tmp/ssh_mux -r "/Users/yuan/Desktop/Solve Paper/front/dist/"* root@106.53.136.108:/www/wwwroot/papersolver/

# 4. 配置 Nginx 虚拟主机并重载服务
echo "⚙️ 正在配置服务器 Nginx 站点规则..."
scp -o ControlPath=/tmp/ssh_mux "/Users/yuan/.gemini/antigravity/brain/368b68e2-c2cb-490b-bcef-66f576fed54e/scratch/papersolver.conf" root@106.53.136.108:/www/server/panel/vhost/nginx/
ssh -o ControlPath=/tmp/ssh_mux root@106.53.136.108 "nginx -s reload || /etc/init.d/nginx reload"

# 4.5 上传桌面端安装包与本地依赖引导程序
echo "💾 正在上传桌面客户端安装包与依赖引导程序..."
DMG_FILE=$(ls /Users/yuan/Desktop/PaperSolverReleases/*.dmg 2>/dev/null | head -1)
EXE_FILE=$(ls /Users/yuan/Desktop/PaperSolverReleases/*.exe 2>/dev/null | head -1)
if [ -n "$DMG_FILE" ]; then
    scp -o ControlPath=/tmp/ssh_mux "$DMG_FILE" root@106.53.136.108:/www/wwwroot/papersolver/downloads/PaperSolver.dmg
    echo "  ✅ macOS DMG 上传完成"
else
    echo "  ⚠️  未找到 DMG 文件，跳过"
fi
if [ -n "$EXE_FILE" ]; then
    scp -o ControlPath=/tmp/ssh_mux "$EXE_FILE" root@106.53.136.108:/www/wwwroot/papersolver/downloads/PaperSolver.exe
    echo "  ✅ Windows EXE 上传完成"
else
    echo "  ⚠️  未找到 EXE 文件，跳过"
fi

# 创建依赖存放目录并上传 zip 依赖包
ssh -o ControlPath=/tmp/ssh_mux root@106.53.136.108 "mkdir -p /www/wwwroot/papersolver/downloads/dependencies"
if [ -d "/Users/yuan/Desktop/Solve Paper/desktop/release/dependencies" ]; then
    echo "📤 正在上传依赖引导程序到服务器..."
    scp -o ControlPath=/tmp/ssh_mux "/Users/yuan/Desktop/Solve Paper/desktop/release/dependencies/"*.zip root@106.53.136.108:/www/wwwroot/papersolver/downloads/dependencies/
    echo "  ✅ 依赖引导程序（Zip）上传完成"
else
    echo "  ⚠️ 未找到依赖压缩包目录，跳过"
fi

# 5. 上传后端 Java 服务 JAR 包
echo "📤 正在上传后端 Java 程序包..."
scp -o ControlPath=/tmp/ssh_mux "/Users/yuan/Desktop/Solve Paper/backend/target/paperpilot-server-0.0.1-SNAPSHOT.jar" root@106.53.136.108:/www/wwwroot/

# 6. 杀死旧的 Java 进程并后台重新启动
echo "🔄 正在服务器重启后端服务..."
ssh -o ControlPath=/tmp/ssh_mux root@106.53.136.108 "kill -9 \$(pgrep -f paperpilot-server) 2>/dev/null; nohup bash -c 'set -a; [ -f /etc/papersolver/papersolver.env ] && . /etc/papersolver/papersolver.env; set +a; exec java -Xms800m -Xmx1536m -jar /www/wwwroot/paperpilot-server-0.0.1-SNAPSHOT.jar' > /www/wwwroot/backend.log 2>&1 &"

# 7. 关闭临时安全通道
ssh -O exit -S /tmp/ssh_mux root@106.53.136.108

echo "========================================="
echo "✨ 部署成功！您可以直接访问 http://papersolver.cn"
echo "========================================="
