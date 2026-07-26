const input = document.getElementById("apiBase");
const statusEl = document.getElementById("status");
const accountEl = document.getElementById("account");
const versionEl = document.getElementById("version");
const desktopStateEl = document.getElementById("desktopState");

versionEl.textContent = `v${chrome.runtime.getManifest().version}`;

chrome.storage.sync.get(["apiBase", "userId", "userName"], (data) => {
  input.value = data.apiBase || "http://127.0.0.1:8080";
  accountEl.textContent = "请打开并登录 PaperSolver 桌面客户端完成绑定。";
});

checkDesktopState();

document.getElementById("save").addEventListener("click", () => {
  const value = input.value.trim() || "http://127.0.0.1:8080";
  chrome.storage.sync.set({ apiBase: value }, () => {
    statusEl.textContent = "已保存";
    setTimeout(() => {
      statusEl.textContent = "";
    }, 1600);
  });
});

async function checkDesktopState() {
  desktopStateEl.classList.remove("bad");
  desktopStateEl.textContent = "正在检测桌面端本机保存服务...";
  try {
    const response = await fetch("http://127.0.0.1:18765/health", { method: "GET" });
    const data = await response.json();
    if (!response.ok || !data?.ok) throw new Error();
    if (data.apiBaseUrl) {
      input.value = data.apiBaseUrl;
      await chrome.storage.sync.set({ apiBase: data.apiBaseUrl });
    }
    if (data.session?.userId) {
      await chrome.storage.sync.set({
        userId: String(data.session.userId),
        userName: data.session.userName || "PaperSolver 用户",
        appUrl: "papersolver-desktop"
      });
      accountEl.textContent = `已绑定客户端账号：${data.session.userName || "PaperSolver 用户"}（ID ${data.session.userId}）`;
      desktopStateEl.textContent = `桌面端已连接，PDF 将保存到：${data.pdfStorageDir || "已配置目录"}`;
    } else {
      desktopStateEl.classList.add("bad");
      desktopStateEl.textContent = "桌面端已打开，但未检测到客户端登录账号。请先在客户端登录一次。";
    }
  } catch {
    desktopStateEl.classList.add("bad");
    desktopStateEl.textContent = "未连接到桌面端。请打开最新版 PaperSolver 桌面端，并先配置 PDF 保存目录。";
  }
}

document.getElementById("test").addEventListener("click", async () => {
  const apiBase = input.value.trim() || "http://127.0.0.1:8080";
  statusEl.classList.remove("error");
  statusEl.textContent = "正在测试...";
  try {
    const { userId } = await chrome.storage.sync.get(["userId"]);
    const headers = {};
    if (/^\d+$/.test(String(userId || ""))) {
      headers["X-PaperPilot-User-Id"] = String(userId);
    }
    const response = await fetch(`${apiBase.replace(/\/$/, "")}/api/library/papers`, { headers });
    if (!response.ok) {
      throw new Error(`后端返回 HTTP ${response.status}`);
    }
    const papers = await response.json();
    statusEl.textContent = `连接正常，当前账号文献 ${Array.isArray(papers) ? papers.length : 0} 篇`;
  } catch (error) {
    statusEl.classList.add("error");
    statusEl.textContent = error?.message || "连接失败，请确认后端 8080 已启动";
  }
});
