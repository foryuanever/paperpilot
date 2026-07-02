import axios from "axios";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8080";
const AUTH_STORAGE_KEY = "paperpilot-auth";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15000,
});

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
