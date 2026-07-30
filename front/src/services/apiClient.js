import axios from "axios";

const API_BASE_STORAGE_KEY = "papersolver-api-base-url";
const AUTH_STORAGE_KEY = "paperpilot-auth";

export let API_BASE_URL = resolveInitialApiBaseUrl();

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15000,
});

export async function initializeApiBaseUrl() {
  if (!window.paperSolverDesktop?.getBackendConfig) {
    setApiBaseUrl(resolveInitialApiBaseUrl(), { persist: false });
    return API_BASE_URL;
  }
  try {
    const config = await window.paperSolverDesktop.getBackendConfig();
    const nextUrl = normalizeApiBaseUrl(config?.apiBaseUrl) || resolveInitialApiBaseUrl();
    setApiBaseUrl(nextUrl, { persist: true });
  } catch {
    setApiBaseUrl(resolveInitialApiBaseUrl(), { persist: false });
  }
  return API_BASE_URL;
}

export function getCurrentApiBaseUrl() {
  return API_BASE_URL;
}

export function setApiBaseUrl(url, options = {}) {
  const nextUrl = normalizeApiBaseUrl(url);
  if (!nextUrl) return API_BASE_URL;
  API_BASE_URL = nextUrl;
  apiClient.defaults.baseURL = nextUrl;
  if (options.persist !== false) {
    localStorage.setItem(API_BASE_STORAGE_KEY, nextUrl);
  }
  return API_BASE_URL;
}

export function normalizeApiBaseUrl(url) {
  const text = String(url || "").trim().replace(/\/+$/, "");
  if (!/^https?:\/\/[^/]+/i.test(text)) return "";
  return text;
}

export async function testApiBaseUrl(url) {
  const apiBaseUrl = normalizeApiBaseUrl(url);
  if (!apiBaseUrl) {
    throw new Error("请输入有效地址，例如 https://api.papersolver.cn");
  }
  try {
    const { data } = await axios.get(`${apiBaseUrl}/api/health`, {
      timeout: 6000,
      headers: {
        "Accept": "application/json",
      },
    });
    return {
      ok: true,
      apiBaseUrl,
      service: data?.service || "PaperSolver Backend",
    };
  } catch (error) {
    if (error?.response?.status === 404) {
      try {
        const { data } = await axios.get(`${apiBaseUrl}/api/tutorials`, {
          timeout: 6000,
          headers: {
            "Accept": "application/json",
          },
        });
        return {
          ok: true,
          apiBaseUrl,
          count: Array.isArray(data) ? data.length : 0,
        };
      } catch (fallbackError) {
        error = fallbackError;
      }
    }
    if (error?.response?.status) {
      throw new Error(`后端已响应，但接口返回 ${error.response.status}`);
    }
    if (error?.code === "ECONNABORTED") {
      throw new Error("连接超时，请检查后端是否启动或服务器安全组是否放行。");
    }
    if (/^https?:\/\/(127\.0\.0\.1|localhost)(:\d+)?$/i.test(apiBaseUrl)) {
      throw new Error("本地后端未启动：请先启动后端服务，或把地址改成线上 API。");
    }
    throw new Error("连接失败，请检查后端地址、HTTPS 配置或服务器是否在线。");
  }
}

function resolveInitialApiBaseUrl() {
  const stored = localStorage.getItem(API_BASE_STORAGE_KEY);
  const normalizedStored = normalizeApiBaseUrl(stored);
  if (normalizedStored) return normalizedStored;
  return normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL) || "http://127.0.0.1:8080";
}

apiClient.interceptors.request.use((config) => {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) return config;
  try {
    const session = JSON.parse(raw);
    const userId = session?.user?.userId;
    if (userId) {
      config.headers["X-PaperPilot-User-Id"] = userId;
    }
  } catch {
    return config;
  }
  return config;
});
