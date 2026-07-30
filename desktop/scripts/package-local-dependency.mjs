import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const desktopRoot = path.resolve(__dirname, "..");
const templateDir = path.join(desktopRoot, "dependency-template");
const sourceDir = path.resolve(process.env.PAPER_SOLVER_DEPENDENCY_SOURCE || templateDir);
const releaseDir = path.resolve(process.env.PAPER_SOLVER_DEPENDENCY_OUTPUT || path.join(desktopRoot, "release", "dependencies"));
const platformName = process.env.PAPER_SOLVER_DEPENDENCY_PLATFORM || normalizePlatform(process.platform);
const archName = process.env.PAPER_SOLVER_DEPENDENCY_ARCH || normalizeArch(process.arch);
const outputName = `papersolver-local-dependency-${platformName}-${archName}.zip`;
const outputPath = path.join(releaseDir, outputName);

await ensureDependencySource(sourceDir);
if (sourceDir === path.resolve(templateDir) && process.env.PAPER_SOLVER_ALLOW_TEMPLATE_DEPENDENCY !== "1") {
  throw new Error("Refusing to package the template dependency. Set PAPER_SOLVER_DEPENDENCY_SOURCE to the real dependency directory.");
}
await fs.promises.mkdir(releaseDir, { recursive: true });
await fs.promises.rm(outputPath, { force: true });
await createZip(sourceDir, outputPath);
const stat = await fs.promises.stat(outputPath);

console.log(`PaperSolver local dependency package created:`);
console.log(outputPath);
console.log(`Size: ${formatBytes(stat.size)}`);

function normalizePlatform(platform) {
  if (platform === "darwin") return "macos";
  if (platform === "win32") return "windows";
  return "linux";
}

function normalizeArch(arch) {
  return arch === "arm64" ? "arm64" : "x64";
}

async function ensureDependencySource(root) {
  const manifestPath = path.join(root, "papersolver-dependency.json");
  const manifestRaw = await fs.promises.readFile(manifestPath, "utf8").catch(() => "");
  if (!manifestRaw) {
    throw new Error(`Missing dependency manifest: ${manifestPath}`);
  }
  let manifest;
  try {
    manifest = JSON.parse(manifestRaw);
  } catch (error) {
    throw new Error(`Invalid dependency manifest JSON: ${error.message}`);
  }
  if (!Array.isArray(manifest.services) || manifest.services.length === 0) {
    throw new Error("Dependency manifest must include at least one service.");
  }
  for (const service of manifest.services) {
    if (!service?.id || !service?.command) {
      throw new Error("Each dependency service must include id and command.");
    }
    const commandPath = path.join(root, service.command);
    if (!fs.existsSync(commandPath)) {
      throw new Error(`Dependency service command not found: ${commandPath}`);
    }
    if (process.platform !== "win32") {
      await fs.promises.chmod(commandPath, 0o755).catch(() => {});
    }
  }
}

async function createZip(root, target) {
  if (process.platform === "darwin") {
    await run("ditto", ["-c", "-k", "--norsrc", ".", target], { cwd: root });
    return;
  }
  await run("zip", ["-r", target, "."], { cwd: root });
}

function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      stdio: "inherit",
      ...options
    });
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) resolve();
      else reject(new Error(`${command} exited with code ${code}`));
    });
  });
}

function formatBytes(bytes) {
  const value = Number(bytes) || 0;
  if (value < 1024) return `${value} B`;
  const units = ["KB", "MB", "GB", "TB"];
  let size = value / 1024;
  let index = 0;
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024;
    index += 1;
  }
  return `${size >= 10 ? size.toFixed(1) : size.toFixed(2)} ${units[index]}`;
}
