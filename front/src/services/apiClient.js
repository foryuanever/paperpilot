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
    let nextUrl = normalizeApiBaseUrl(config?.apiBaseUrl);
    if (!nextUrl || nextUrl.includes("127.0.0.1:8080") || nextUrl.includes("localhost:8080")) {
      nextUrl = "https://papersolver.cn";
    }
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
  let text = String(url || "").trim().replace(/\/+$/, "");
  if (!/^https?:\/\/[^/]+/i.test(text)) return "";
  text = text.replace(/\/api$/i, "");
  return text;
}

async function refreshStoredAuthSession(session, accessToken) {
  if (!session?.isAuthenticated || !session?.user || !accessToken) return session;
  try {
    const { data } = await axios.patch(`${API_BASE_URL}/api/auth/profile`, {}, {
      headers: {
        "Content-Type": "application/json",
        "X-PaperPilot-Session": accessToken,
      },
      timeout: 8000,
    });
    if (!data) return session;
    const refreshed = {
      ...session,
      user: {
        ...session.user,
        userId: data.userId,
        name: data.name,
        email: data.email,
        numericId: data.numericId || session.user.numericId || "",
        inviteCode: data.inviteCode,
        role: data.role || session.user.role || "普通用户",
        avatarUrl: data.avatarUrl || "",
        backgroundUrl: data.backgroundUrl || "",
        fruitScore: data.fruitScore !== undefined ? data.fruitScore : session.user.fruitScore || 0,
        checkinScore: data.checkinScore !== undefined ? data.checkinScore : session.user.checkinScore || 0,
        schoolName: data.schoolName || session.user.schoolName || "",
        campusVerified: Boolean(data.campusVerified ?? session.user.campusVerified),
        qq: data.qq || "",
        wechat: data.wechat || "",
        qqOpenid: data.qqOpenid || session.user.qqOpenid || "",
        registerTime: data.registerTime || session.user.registerTime || "",
        accessToken: data.accessToken || session.user.accessToken || "",
      },
    };
    refreshed.role = refreshed.user.role;
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(refreshed));
    return refreshed;
  } catch {
    return session;
  }
}

export async function testApiBaseUrl(url) {
  const apiBaseUrl = normalizeApiBaseUrl(url);
  if (!apiBaseUrl) {
    throw new Error("请输入有效地址，例如 https://papersolver.cn");
  }
  try {
    // POST /auth/login with empty body: returns 400/401 when server is alive (not 404/500)
    const resp = await axios.post(`${apiBaseUrl}/api/auth/login`, {}, {
      timeout: 6000,
      validateStatus: (status) => status < 500,
    });
    return { ok: true, apiBaseUrl, service: "PaperSolver Backend" };
  } catch (error) {
    if (error?.response?.status && error.response.status < 500) {
      return { ok: true, apiBaseUrl, service: "PaperSolver Backend" };
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
  if (normalizedStored) {
    if (normalizedStored.includes("127.0.0.1:8080") || normalizedStored.includes("localhost:8080")) {
      return "https://papersolver.cn";
    }
    return normalizedStored;
  }
  const envUrl = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL);
  if (envUrl) {
    if (envUrl.includes("127.0.0.1:8080") || envUrl.includes("localhost:8080")) {
      return "https://papersolver.cn";
    }
    return envUrl;
  }
  return "https://papersolver.cn";
}

apiClient.interceptors.request.use(async (config) => {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) return config;
  try {
    const session = JSON.parse(raw);
    const user = session?.user;
    const accessToken = user?.accessToken;
    if (accessToken) {
      config.headers["X-PaperPilot-Session"] = accessToken;
    }

    // AI endpoints and required points check. Bilingual PDF translation uses
    // the membership "对照翻译" counter and is checked in DualTranslateView.
    const AI_ENDPOINTS_POINTS = [
      { pattern: /\/meeting-reports\/[^/]+\/meeting-note/i, points: 3 },
      { pattern: /\/meeting-reports\/[^/]+\/generate-section/i, points: 1 },
      { pattern: /\/meeting-reports\/[^/]+\/generate/i, points: 1 },
      { pattern: /\/meeting-reports\/[^/]+\/ask/i, points: 1 },
      { pattern: /\/meeting-reports\/deck\/analyze/i, points: 1 },
      { pattern: /\/meeting-reports\/fuse/i, points: 1 },
      { pattern: /^\/api\/translate(?:$|[/?#])/i, points: 1 },
      { pattern: /\/mineru\/[^/]+\/parse/i, points: 1 },
    ];

    const url = config.url || "";
    const match = AI_ENDPOINTS_POINTS.find(item => item.pattern.test(url));
    if (match) {
      let latestSession = session;
      let latestUser = user;
      let currentPoints = latestUser?.fruitScore !== undefined ? Number(latestUser.fruitScore) : 0;
      let isAdmin = latestUser?.role === "管理员";
      if (!isAdmin && currentPoints < match.points) {
        latestSession = await refreshStoredAuthSession(session, accessToken);
        latestUser = latestSession?.user || latestUser;
        currentPoints = latestUser?.fruitScore !== undefined ? Number(latestUser.fruitScore) : 0;
        isAdmin = latestUser?.role === "管理员";
      }
      if (!isAdmin && currentPoints < match.points) {
        try {
          const { useDialogStore } = await import("../stores/dialog");
          const dialogStore = useDialogStore();
          dialogStore.alert(`当前积分不足，无法使用该 AI 功能（该功能需要 ${match.points} 积分，当前剩余 ${currentPoints} 积分）。请先去个人页签到。`);
        } catch (dialogErr) {
          console.error("Failed to show dialog:", dialogErr);
          alert(`当前积分不足，该功能需要 ${match.points} 积分，当前剩余 ${currentPoints} 积分。`);
        }
        return Promise.reject(new Error("当前积分不足，请先签到或充值"));
      }
    }
  } catch (err) {
    if (err.message === "当前积分不足，请先签到或充值") {
      return Promise.reject(err);
    }
    return config;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      const authRaw = localStorage.getItem(AUTH_STORAGE_KEY);
      if (authRaw) {
        try {
          const session = JSON.parse(authRaw);
          if (session?.isAuthenticated) {
            session.isAuthenticated = false;
            session.user = null;
            localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
          }
        } catch {
          localStorage.removeItem(AUTH_STORAGE_KEY);
        }
      }
    }
    return Promise.reject(error);
  }
);
