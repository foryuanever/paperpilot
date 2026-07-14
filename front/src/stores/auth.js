import { computed, reactive } from "vue";
import { defineStore } from "pinia";
import { paperpilotApi } from "../services/paperpilotApi";

const STORAGE_KEY = "paperpilot-auth";

function readJson(key, fallback) {
  const raw = localStorage.getItem(key);
  if (!raw) return fallback;
  try {
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

export const useAuthStore = defineStore("auth", () => {
  const session = reactive(
    readJson(STORAGE_KEY, {
      isAuthenticated: false,
      user: null,
      notifications: [],
    }),
  );

  if (session.isAuthenticated && !session.user?.userId) {
    session.isAuthenticated = false;
    session.user = null;
  }
  if (session.isAuthenticated && !session.loginSerial) {
    session.loginSerial = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  const profile = computed(() => session.user || { name: "Guest", email: "", inviteCode: "", avatarUrl: "", backgroundUrl: "", schoolName: "", campusVerified: false });
  const unreadCount = computed(() => session.notifications.length);

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  async function updateProfileFields(payload) {
    if (session.user) {
      if (payload.name !== undefined) session.user.name = payload.name;
      if (payload.avatarUrl !== undefined) session.user.avatarUrl = payload.avatarUrl;
      if (payload.backgroundUrl !== undefined) session.user.backgroundUrl = payload.backgroundUrl;
      persist();
      try {
        const saved = await paperpilotApi.updateProfile(payload);
        session.user = {
          ...session.user,
          userId: saved.userId,
          name: saved.name,
          email: saved.email,
          inviteCode: saved.inviteCode,
          role: saved.role || session.user.role || "学生",
          avatarUrl: saved.avatarUrl || "",
          backgroundUrl: saved.backgroundUrl || "",
          fruitScore: saved.fruitScore || session.user.fruitScore || 0,
          schoolName: saved.schoolName || session.user.schoolName || "",
          campusVerified: Boolean(saved.campusVerified ?? session.user.campusVerified),
        };
        session.role = session.user.role;
        persist();
      } catch (error) {
        console.error("Failed to persist profile fields:", error);
        throw error;
      }
    }
  }

  function applySession(user) {
    session.isAuthenticated = true;
    session.loginSerial = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    session.user = {
      userId: user.userId,
      name: user.name,
      email: user.email,
      inviteCode: user.inviteCode,
      role: user.role || "学生",
      avatarUrl: user.avatarUrl || "",
      backgroundUrl: user.backgroundUrl || "",
      fruitScore: user.fruitScore || 0,
      schoolName: user.schoolName || "",
      campusVerified: Boolean(user.campusVerified),
    };
    // Provide a direct shortcut for role checks used throughout the app
    session.role = session.user.role;
    persist();
    refreshNotifications().catch(error => console.error("Failed to load notifications:", error));
  }

  function createDemoUser(payload = {}) {
    const email = payload.email || "demo@paperpilot.app";
    const baseName = String(payload.name || email.split("@")[0] || "Demo User").trim();
    let defaultRole = "学生";
    if (email.toLowerCase().includes("admin")) {
      defaultRole = "管理员";
    } else if (email.toLowerCase().includes("tutor")) {
      defaultRole = "导师";
    }
    return {
      userId: `demo-${Date.now()}`,
      name: baseName || "Demo User",
      email,
      inviteCode: "DEMO MODE",
      role: payload.role || defaultRole,
      schoolName: "",
      campusVerified: false,
    };
  }

  async function login({ email, password }) {
    try {
      const user = await paperpilotApi.login({ email, password });
      applySession(user);
    } catch (error) {
      if (error?.message === "Network Error" || error?.code === "ECONNABORTED") {
        applySession(createDemoUser({ email }));
        addNotification({
          title: "已进入本地演示模式",
          desc: "后端当前不可达，已自动使用本地 demo 登录。",
        });
        return;
      }
      throw error;
    }
  }

  async function register({ inviteCode, name, email, password, role, mentorInviteCode, verificationCode }) {
    try {
      const user = await paperpilotApi.register({
        inviteCode,
        name,
        email,
        password,
        role,
        mentorInviteCode,
        verificationCode,
      });
      applySession(user);
    } catch (error) {
      if (error?.message === "Network Error" || error?.code === "ECONNABORTED") {
        applySession(createDemoUser({ name, email, role }));
        addNotification({
          title: "已进入本地演示模式",
          desc: "注册接口当前不可达，已自动创建本地 demo 会话。",
        });
        return;
      }
      throw error;
    }
  }

  function logout() {
    session.isAuthenticated = false;
    session.user = null;
    persist();
  }

  function addNotification(payload) {
    session.notifications.unshift({
      id: `n-${Date.now()}`,
      title: payload.title,
      desc: payload.desc,
    });
    persist();
  }

  async function refreshNotifications() {
    if (!session.isAuthenticated) {
      session.notifications = [];
      return;
    }
    const notifications = await paperpilotApi.getNotifications();
    session.notifications = notifications.map(item => ({
      id: item.id,
      title: item.title,
      desc: item.description,
      type: item.type,
      referenceId: item.referenceId,
      actorUserId: item.actorUserId,
      createdAt: item.createdAt,
    }));
    persist();
  }

  async function markNotificationRead(id) {
    if (!String(id).startsWith("n-")) {
      await paperpilotApi.markNotificationRead(id);
    }
    session.notifications = session.notifications.filter((item) => item.id !== id);
    persist();
  }

  return {
    addNotification,
    profile,
    session,
    unreadCount,
    login,
    logout,
    markNotificationRead,
    refreshNotifications,
    register,
    updateProfileFields,
    persist,
  };
});
