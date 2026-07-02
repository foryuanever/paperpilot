const input = document.getElementById("apiBase");
const statusEl = document.getElementById("status");
const accountEl = document.getElementById("account");
const versionEl = document.getElementById("version");

versionEl.textContent = `v${chrome.runtime.getManifest().version}`;

chrome.storage.sync.get(["apiBase", "userId", "userName"], (data) => {
  input.value = data.apiBase || "http://127.0.0.1:8080";
  if (data.userId) {
    accountEl.textContent = `已绑定账号：${data.userName || "PaperSolver 用户"}（ID ${data.userId}）`;
  }
});

document.getElementById("save").addEventListener("click", () => {
  const value = input.value.trim() || "http://127.0.0.1:8080";
  chrome.storage.sync.set({ apiBase: value }, () => {
    statusEl.textContent = "已保存";
    setTimeout(() => {
      statusEl.textContent = "";
    }, 1600);
  });
});

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
