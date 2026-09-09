import { spawnSync } from "node:child_process";
import { cpSync, existsSync, rmSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const desktopDir = path.resolve(__dirname, "..");
const repoRoot = path.resolve(desktopDir, "..");
const frontDir = path.join(repoRoot, "front");

const apiBase = process.env.PAPER_SOLVER_API_BASE
  || process.env.VITE_API_BASE_URL
  || "https://papersolver.cn/api";

if (!existsSync(path.join(frontDir, "package.json"))) {
  console.error("没有找到 front/package.json，请确认 desktop 文件夹位于项目根目录下。");
  process.exit(1);
}

const result = spawnSync("npm", ["run", "build"], {
  cwd: frontDir,
  stdio: "inherit",
  env: {
    ...process.env,
    VITE_API_BASE_URL: apiBase,
    VITE_DESKTOP_APP: "true"
  }
});

if (result.status !== 0) {
  process.exit(result.status || 1);
}

const desktopFrontDist = path.join(desktopDir, "front-dist");
rmSync(desktopFrontDist, { recursive: true, force: true });
cpSync(path.join(frontDir, "dist"), desktopFrontDist, { recursive: true });

console.log(`PaperSolver front built for desktop. API: ${apiBase}`);
