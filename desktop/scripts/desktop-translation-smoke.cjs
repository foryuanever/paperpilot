const path = require("node:path");
const { app, BrowserWindow } = require("electron");

const providers = ["google", "bing", "youdao", "huoshanweb"];
const timeoutMs = 35_000;

app.setPath("userData", path.join(__dirname, "..", "scratch", "translation-smoke-user-data"));
require("../src/main.cjs");

app.whenReady().then(async () => {
  try {
    const window = await waitForWindow();
    await waitForLoad(window);
    const results = [];
    for (const provider of providers) {
      const startedAt = Date.now();
      try {
        const result = await window.webContents.executeJavaScript(`
          Promise.race([
            window.paperSolverDesktop.testTranslationProvider({ provider: ${JSON.stringify(provider)} }),
            new Promise((_, reject) => setTimeout(() => reject(new Error("strict provider timeout")), ${timeoutMs}))
          ])
        `);
        results.push({
          provider,
          ok: true,
          elapsedMs: Date.now() - startedAt,
          translatedText: result?.translatedText || ""
        });
      } catch (error) {
        results.push({
          provider,
          ok: false,
          elapsedMs: Date.now() - startedAt,
          error: error?.message || String(error)
        });
      }
    }
    process.stdout.write(`${JSON.stringify(results, null, 2)}\n`);
    app.exit(results.every((item) => item.ok) ? 0 : 1);
  } catch (error) {
    process.stderr.write(`${error?.stack || error?.message || String(error)}\n`);
    app.exit(1);
  }
});

async function waitForWindow() {
  const deadline = Date.now() + 20_000;
  while (Date.now() < deadline) {
    const window = BrowserWindow.getAllWindows()[0];
    if (window) return window;
    await delay(100);
  }
  throw new Error("PaperSolver window was not created");
}

async function waitForLoad(window) {
  if (!window.webContents.isLoadingMainFrame()) return;
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("PaperSolver renderer load timed out")), 30_000);
    window.webContents.once("did-finish-load", () => {
      clearTimeout(timer);
      resolve();
    });
  });
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
