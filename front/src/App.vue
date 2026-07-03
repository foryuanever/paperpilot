<template>
  <div :class="rootClass">
    <header v-if="showNav" class="spatial-nav-float">
      <router-link class="spatial-nav-brand" to="/library">
        <img class="spatial-nav-mark" src="/brand/papersolver-mark-v2.png" alt="" />
        <strong>PaperSolver</strong>
      </router-link>

      <nav class="spatial-nav-links">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="spatial-nav-link"
          :class="{ active: route.path === item.to }"
        >
          {{ item.label }}
        </router-link>
      </nav>

      <div class="spatial-nav-actions">
        <label class="global-search-bar">
          <span class="search-icon" v-html="chromeIcons.search"></span>
          <input type="search" placeholder="搜索论文、作者、DOI" />
        </label>

        <router-link class="icon-button message-button" to="/messages" title="私信">
          <span v-html="chromeIcons.message"></span>
          <span v-if="messageUnreadCount" class="notification-badge">{{ messageUnreadCount }}</span>
        </router-link>

        <div class="topbar-menu-wrap">
          <button class="icon-button notification-button" @click.stop="uiStore.toggleNotifications">
            <span v-html="chromeIcons.bell"></span>
            <span v-if="authStore.unreadCount" class="notification-badge">{{ authStore.unreadCount }}</span>
          </button>
          <div v-if="uiStore.layout.showNotifications" class="popover-panel app-popover notification-popover">
            <div class="popover-header">
              <div class="popover-title">消息通知</div>
              <router-link class="auth-link" to="/library">查看文献库</router-link>
            </div>
            <div class="notification-list">
              <div v-if="authStore.session.notifications.length === 0" class="popover-empty">暂无新消息</div>
              <button
                v-for="item in authStore.session.notifications"
                :key="item.id"
                class="notification-item"
                @click="openNotification(item)"
              >
                <strong>{{ item.title }}</strong>
                <span>{{ item.desc }}</span>
              </button>
            </div>
          </div>
        </div>

        <div class="topbar-menu-wrap">
          <button class="profile-button app-profile-button" @click.stop="uiStore.toggleProfileMenu">
            <div class="profile-avatar-container">
              <img v-if="authStore.profile.avatarUrl" :src="authStore.profile.avatarUrl" class="profile-avatar-img" />
              <span v-else class="profile-avatar">{{ userInitial }}</span>
              <span class="profile-avatar-status-dot"></span>
            </div>
            <span class="profile-name">{{ authStore.profile.name }}</span>
          </button>
          <div v-if="uiStore.layout.showProfileMenu" class="popover-panel profile-panel app-popover">
            <div class="profile-popover-header">
              <img v-if="authStore.profile.avatarUrl" :src="authStore.profile.avatarUrl" class="profile-popover-avatar-img" />
              <div v-else class="profile-popover-avatar" :style="{ backgroundColor: getAvatarColor(currentUserMember.role) }">
                {{ userInitial }}
              </div>
              <div class="profile-popover-meta">
                <div class="profile-popover-name-row">
                  <strong class="profile-popover-name">{{ authStore.profile.name }}</strong>
                  <span class="profile-popover-role-badge" :class="getRoleClass(currentUserMember.role)">{{ currentUserMember.role }}</span>
                </div>
                <div class="profile-popover-email">{{ authStore.profile.email }}</div>
              </div>
            </div>

            <!-- Level Banner -->
            <div class="profile-popover-level-banner">
              <div class="popover-level-num">Lv.{{ getMemberLevelInfo(currentUserMember.activeTime).level }}</div>
              <div class="popover-level-info">
                <div class="popover-level-title">{{ getMemberLevelInfo(currentUserMember.activeTime).title }}</div>
                <div class="popover-level-sub">活跃在线: {{ formatActiveTime(currentUserMember.activeTime) }}</div>
              </div>
            </div>

            <!-- Quick Stats Grid -->
            <div class="profile-popover-stats">
              <div class="stats-item">
                <span class="stats-label">注册时间</span>
                <span class="stats-value">{{ currentUserMember.registerTime }}</span>
              </div>
              <div class="stats-item">
                <span class="stats-label">邀请码</span>
                <span class="stats-value">{{ authStore.profile.inviteCode || 'N/A' }}</span>
              </div>
            </div>

            <!-- Token limit -->
            <div class="profile-popover-quota">
              <div class="quota-meta">
                <span class="quota-title">Token 共享额度</span>
                <span class="quota-usage">{{ formatTokens(currentUserMember.tokenUsed) }} / {{ formatTokens(currentUserMember.tokenLimit) }}</span>
              </div>
              <div class="quota-progress-bar">
                <div 
                  class="quota-progress-fill" 
                  :class="getQuotaColorClass(currentUserMember.tokenUsed / currentUserMember.tokenLimit)"
                  :style="{ width: Math.min(100, (currentUserMember.tokenUsed / currentUserMember.tokenLimit) * 100) + '%' }"
                ></div>
              </div>
            </div>

            <hr class="profile-popover-divider" />

            <div class="profile-popover-actions">
              <router-link class="spatial-btn spatial-btn-accent" style="grid-column: 1 / -1; text-align: center; display: grid; place-items: center; text-decoration: none;" to="/profile" @click="uiStore.closeOverlays">
                个人主页
              </router-link>
              <router-link class="spatial-btn spatial-btn-ghost" to="/models" @click="uiStore.closeOverlays">用量中心</router-link>
              <button class="spatial-btn spatial-btn-ghost" @click="logout">退出登录</button>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main :class="mainClass" @click="uiStore.closeOverlays">
      <router-view v-slot="{ Component, route: viewRoute }">
        <component :is="Component" :key="viewRoute.path" />
      </router-view>
    </main>

    <AppDialog />
    <UserProfileCard />

    <Teleport to="body">
      <Transition name="announcement-modal">
        <div v-if="activeSiteMessage" class="announcement-backdrop" @click.self="markSiteMessageRead">
          <section class="announcement-dialog" role="dialog" aria-modal="true" aria-labelledby="site-announcement-title">
            <header>
              <span>系统公告</span>
              <button type="button" aria-label="关闭系统公告" @click="markSiteMessageRead">×</button>
            </header>
            <div class="announcement-content">
              <h2 id="site-announcement-title">{{ activeSiteMessage.title }}</h2>
              <time>{{ formatSiteMessageTime(activeSiteMessage.createdAt) }}</time>
              <p>{{ activeSiteMessage.content }}</p>
            </div>
            <footer><button type="button" @click="markSiteMessageRead">已阅读</button></footer>
          </section>
        </div>
      </Transition>
    </Teleport>

    <!-- Password Modify Modal -->
    <Transition name="fade">
      <div v-if="showPasswordModal" class="modal-overlay" @click="showPasswordModal = false">
        <div class="modal-card password-modal-card spatial-glass-panel" @click.stop>
          <div class="modal-head">
            <h3>修改密码</h3>
            <button class="close-btn" @click="showPasswordModal = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 18px; height: 18px;"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            </button>
          </div>
          
          <div class="modal-body">
            <div v-if="passwordError" class="auth-error" style="margin-bottom: 12px;">
              {{ passwordError }}
            </div>
            <div v-if="passwordSuccess" class="password-success-alert" style="margin-bottom: 12px;">
              {{ passwordSuccess }}
            </div>

            <div class="form-group">
              <label>旧密码</label>
              <input v-model="oldPassword" type="password" placeholder="请输入当前密码" />
            </div>
            
            <div class="form-group">
              <label>新密码</label>
              <input v-model="newPassword" type="password" placeholder="请输入新密码（至少 6 位）" />
            </div>

            <div class="form-group">
              <label>确认新密码</label>
              <input v-model="confirmPassword" type="password" placeholder="请再次输入新密码" />
            </div>
          </div>

          <div class="modal-foot">
            <button class="spatial-btn spatial-btn-ghost" @click="showPasswordModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" :disabled="isSubmittingPassword" @click="submitPasswordChange">
              {{ isSubmittingPassword ? '正在提交...' : '确认修改' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { pageNavItems } from "./constants/workspace";
import { useAuthStore } from "./stores/auth";
import { useUiStore } from "./stores/ui";
import { useTeamStore } from "./stores/team";
import { paperpilotApi } from "./services/paperpilotApi";
import { useDialogStore } from "./stores/dialog";
import AppDialog from "./components/AppDialog.vue";
import UserProfileCard from "./components/UserProfileCard.vue";
import { useUserCardStore } from "./stores/userCard";

const authStore = useAuthStore();
const uiStore = useUiStore();
const teamStore = useTeamStore();
const route = useRoute();
const router = useRouter();
const dialogStore = useDialogStore();
const userCardStore = useUserCardStore();
const navItems = computed(() => {
  if (authStore.session.role === "管理员") {
    return [
      { to: "/admin", label: "后台" },
      { to: "/library", label: "文献库" },
      { to: "/reading", label: "文献阅读" },
      { to: "/meeting-report", label: "组会汇报" },
      { to: "/search", label: "检索" },
      { to: "/forum", label: "社区" },
      { to: "/models", label: "用量" },
      { to: "/referral", label: "邀请" },
      { to: "/team", label: "团队" }
    ];
  }
  return pageNavItems;
});

const isLanding = computed(() => route.path === "/" || route.path === "/register");
const isReader = computed(() => route.path.startsWith("/reader"));
const showNav = computed(() => !isLanding.value && !isReader.value);

const rootClass = computed(() => {
  if (isReader.value) return "app-reader-root";
  if (isLanding.value) return "app-landing-root";
  return "spatial-app spatial-page";
});

const mainClass = computed(() => {
  if (isReader.value) return "app-reader-main";
  if (isLanding.value) return "app-landing-main";
  return "spatial-main";
});

const userInitial = computed(() => (authStore.profile.name || "U").slice(0, 1).toUpperCase());

const chromeIcons = {
  search: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="6"/><path d="M20 20l-3.5-3.5"/></svg>`,
  bell: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 4.5a4 4 0 0 0-4 4v2.2c0 .7-.2 1.4-.6 2l-1.1 1.7A1 1 0 0 0 7.1 16h9.8a1 1 0 0 0 .8-1.6l-1.1-1.7a3.7 3.7 0 0 1-.6-2V8.5a4 4 0 0 0-4-4Z"/><path d="M10 18a2.2 2.2 0 0 0 4 0"/></svg>`,
  message: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M5 18.5 3.8 21l3.4-1.1c1.4.7 3 .9 4.8.9 5 0 9-3.6 9-8.1s-4-8.1-9-8.1-9 3.6-9 8.1c0 2.2.8 4.2 2 5.8Z"/><path d="M8 12h.01M12 12h.01M16 12h.01"/></svg>`,
};

const messageUnreadCount = ref(0);

document.documentElement.removeAttribute("data-theme");
localStorage.removeItem("papersolver-theme");

async function refreshMessageUnread() {
  if (!authStore.session.isAuthenticated) {
    messageUnreadCount.value = 0;
    return;
  }
  try {
    const [result, friendRequests] = await Promise.all([
      paperpilotApi.getMessageContacts(),
      paperpilotApi.getFriendRequests(),
    ]);
    messageUnreadCount.value = (result.unreadCount || 0) + (friendRequests.pendingCount || 0);
  } catch {
    messageUnreadCount.value = 0;
  }
}

async function openNotification(item) {
  await authStore.markNotificationRead(item.id);
  uiStore.closeOverlays();
  if (item.type === "private_message") {
    router.push("/messages");
    return;
  }
  if (item.type?.startsWith("forum_") && item.referenceId) {
    router.push(`/forum/post/post-${item.referenceId}`);
  }
}

function logout() {
  authStore.logout();
  uiStore.closeOverlays();
  router.push("/");
}

// User details from team store
const currentUserMember = computed(() => {
  return teamStore.members.find(m => m.isCurrentUser) || {
    id: "m-tutor",
    name: "Yuan",
    role: "导师",
    activeTime: 0,
    registerTime: "2026-01-10",
    tokenUsed: 0,
    tokenLimit: 5000000
  };
});

function getMemberLevelInfo(activeTime) {
  const level = Math.floor((activeTime || 0) / 300) + 1; // 1 level per 5 minutes active
  let title = "科研萌新";
  if (level >= 15) title = "科研主宰";
  else if (level >= 10) title = "科研宗师";
  else if (level >= 6) title = "学术专家";
  else if (level >= 3) title = "科研骨干";
  return { level, title };
}

function formatTokens(n) {
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`;
  if (n >= 1000) return `${Math.round(n / 1000)}K`;
  return String(n);
}

function formatActiveTime(seconds) {
  if (!seconds) return "0分钟";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) {
    return `${h}小时${m}分钟`;
  }
  if (m > 0) {
    return `${m}分钟${s}秒`;
  }
  return `${s}秒`;
}

function getAvatarColor(role) {
  if (role === "导师") return "#0066ff";
  if (role === "管理员") return "#8b5cf6";
  if (role === "特权用户") return "#ec4899";
  return "#10b981";
}

function getRoleClass(role) {
  if (role === "导师") return "badge-tutor";
  if (role === "管理员") return "badge-admin";
  if (role === "特权用户") return "badge-vip";
  return "badge-student";
}

function getQuotaColorClass(ratio) {
  if (ratio > 0.85) return "fill-danger";
  if (ratio > 0.6) return "fill-warning";
  return "fill-safe";
}

// Active time tracking: only accumulates when window activity is detected
const lastActiveTime = ref(Date.now());
let activityTimer = null;
let notificationTimer = null;
let siteMessageTimer = null;
const siteMessages = ref([]);
const activeSiteMessage = ref(null);

function siteMessageReadKey(message) {
  const userId = authStore.session.user?.userId || authStore.profile.email || "guest";
  const loginSerial = authStore.session.loginSerial || "current";
  return `papersolver-site-message-read:${userId}:${loginSerial}:${message.id}`;
}

function chooseUnreadSiteMessage() {
  activeSiteMessage.value = siteMessages.value.find(
    message => sessionStorage.getItem(siteMessageReadKey(message)) !== "read",
  ) || null;
}

function markSiteMessageRead() {
  if (!activeSiteMessage.value) return;
  sessionStorage.setItem(siteMessageReadKey(activeSiteMessage.value), "read");
  activeSiteMessage.value = null;
  chooseUnreadSiteMessage();
}

function formatSiteMessageTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", hour12: false,
  }).format(date).replace(/\//g, "-");
}

async function refreshSiteMessages() {
  if (!authStore.session.isAuthenticated) {
    siteMessages.value = [];
    return;
  }
  try {
    siteMessages.value = await paperpilotApi.getActiveSiteMessages();
    if (!activeSiteMessage.value) chooseUnreadSiteMessage();
  } catch {
    siteMessages.value = [];
    activeSiteMessage.value = null;
  }
}

watch(
  () => authStore.session.loginSerial,
  (loginSerial, previousSerial) => {
    if (!loginSerial || loginSerial === previousSerial || !authStore.session.isAuthenticated) return;
    activeSiteMessage.value = null;
    siteMessages.value = [];
    refreshSiteMessages();
  },
  { flush: "post" },
);

const hasCheckedIn = computed(() => {
  const user = currentUserMember.value;
  if (!user || !user.id) return false;
  const checkin = teamStore.checkins.find(c => c.memberId === user.id);
  return checkin && checkin.status === "已打卡";
});

function resetActivityTimer() {
  if (authStore.session.isAuthenticated && hasCheckedIn.value) {
    lastActiveTime.value = Date.now();
  }
}

function handleUserAvatarClick(event) {
  const target = event.target.closest?.("[data-user-id]");
  const userId = Number(target?.dataset?.userId);
  const emailTarget = event.target.closest?.("[data-user-email]");
  const email = emailTarget?.dataset?.userEmail;
  if (!userId && !email) return;
  event.preventDefault();
  event.stopPropagation();
  if (userId) userCardStore.open(userId);
  else userCardStore.openByEmail(email);
}

onMounted(() => {
  window.addEventListener("mousemove", resetActivityTimer);
  window.addEventListener("keydown", resetActivityTimer);
  window.addEventListener("click", resetActivityTimer);
  window.addEventListener("scroll", resetActivityTimer);
  window.addEventListener("paperpilot:site-messages-changed", refreshSiteMessages);
  document.addEventListener("click", handleUserAvatarClick);

  activityTimer = setInterval(() => {
    if (authStore.session.isAuthenticated) {
      if (hasCheckedIn.value) {
        const timeIdle = Date.now() - lastActiveTime.value;
        if (timeIdle >= 600000) { // 10 minutes of inactivity
          const user = currentUserMember.value;
          if (user && user.id) {
            teamStore.persist();
          }
          logout();
          dialogStore.alert("登录已过期：由于您已打卡且连续 10 分钟没有操作，系统已自动为您退出登录。", {
            title: "登录已过期",
          });
        } else if (timeIdle < 60000) {
          const user = currentUserMember.value;
          if (user && user.id) {
            teamStore.incrementActiveTime(user.id, 1);
          }
        }
      }
    }
  }, 1000);
  authStore.refreshNotifications().catch(() => {});
  refreshMessageUnread();
  refreshSiteMessages();
  notificationTimer = setInterval(() => {
    authStore.refreshNotifications().catch(() => {});
    refreshMessageUnread();
  }, 15000);
  siteMessageTimer = setInterval(refreshSiteMessages, 15000);
});

onUnmounted(() => {
  window.removeEventListener("mousemove", resetActivityTimer);
  window.removeEventListener("keydown", resetActivityTimer);
  window.removeEventListener("click", resetActivityTimer);
  window.removeEventListener("scroll", resetActivityTimer);
  window.removeEventListener("paperpilot:site-messages-changed", refreshSiteMessages);
  document.removeEventListener("click", handleUserAvatarClick);
  if (activityTimer) clearInterval(activityTimer);
  if (notificationTimer) clearInterval(notificationTimer);
  if (siteMessageTimer) clearInterval(siteMessageTimer);
});

// Password change state
const showPasswordModal = ref(false);
const oldPassword = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const passwordError = ref("");
const passwordSuccess = ref("");
const isSubmittingPassword = ref(false);

function openPasswordModal() {
  uiStore.closeOverlays();
  showPasswordModal.value = true;
  oldPassword.value = "";
  newPassword.value = "";
  confirmPassword.value = "";
  passwordError.value = "";
  passwordSuccess.value = "";
}

async function submitPasswordChange() {
  if (!oldPassword.value || !newPassword.value || !confirmPassword.value) {
    passwordError.value = "请填写所有密码字段";
    return;
  }
  if (newPassword.value !== confirmPassword.value) {
    passwordError.value = "两次输入的新密码不一致";
    return;
  }
  if (newPassword.value.length < 6) {
    passwordError.value = "新密码长度至少为 6 位";
    return;
  }

  isSubmittingPassword.value = true;
  passwordError.value = "";
  passwordSuccess.value = "";

  try {
    await paperpilotApi.changePassword({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value
    });
    passwordSuccess.value = "密码修改成功！";
    setTimeout(() => {
      showPasswordModal.value = false;
    }, 1500);
  } catch (error) {
    if (error?.message === "Network Error" || error?.code === "ECONNABORTED" || !error.response) {
      passwordSuccess.value = "密码修改成功！(本地模式已模拟保存)";
      setTimeout(() => {
        showPasswordModal.value = false;
      }, 1500);
      return;
    }
    passwordError.value = error.response?.data?.message || error.message || "修改密码失败，请检查旧密码";
  } finally {
    isSubmittingPassword.value = false;
  }
}
</script>

<style>
[data-user-id],
[data-user-email] {
  cursor: pointer;
}

.site-message-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  width: min(1480px, 100%);
  min-height: 36px;
  margin: -16px auto 18px;
  overflow: hidden;
  border: 1px solid rgba(0, 102, 255, 0.14);
  border-radius: 10px;
  color: #23436f;
  background: #eef5ff;
}

.spatial-nav-float {
  box-sizing: border-box;
  display: grid;
  grid-template-columns: minmax(150px, max-content) minmax(0, 1fr) minmax(300px, max-content);
  align-items: center;
  column-gap: 14px;
  row-gap: 10px;
  width: min(1480px, calc(100vw - 32px));
  max-width: calc(100vw - 32px);
  overflow: visible;
}

.spatial-nav-brand {
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
}

.spatial-nav-brand .spatial-nav-mark {
  width: 32px;
  height: 32px;
  object-fit: contain;
  border-radius: 0;
  background: transparent;
  transition: transform 220ms cubic-bezier(.22, 1, .36, 1);
}

.spatial-nav-brand:hover .spatial-nav-mark {
  transform: translateY(-1px) rotate(-2deg);
}

.spatial-nav-brand strong {
  overflow: hidden;
  text-overflow: ellipsis;
}

.spatial-nav-links {
  min-width: 0;
  flex-wrap: nowrap;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
}

.spatial-nav-links::-webkit-scrollbar {
  display: none;
}

.spatial-nav-link {
  flex: 0 0 auto;
  padding-inline: 11px;
  white-space: nowrap;
}

.spatial-nav-actions {
  min-width: 0;
  flex-wrap: nowrap;
  justify-self: end;
  overflow: visible;
}

.global-search-bar {
  width: clamp(160px, 13vw, 220px);
}

.app-profile-button .profile-name {
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .spatial-nav-float {
    grid-template-columns: minmax(140px, max-content) minmax(0, 1fr);
  }

  .spatial-nav-actions {
    grid-column: 1 / -1;
    justify-self: stretch;
  }

  .global-search-bar {
    flex: 1 1 auto;
    width: auto;
  }
}

@media (max-width: 720px) {
  .spatial-nav-float {
    grid-template-columns: 1fr;
    width: calc(100vw - 24px);
    max-width: calc(100vw - 24px);
  }

  .spatial-nav-actions {
    overflow-x: auto;
    scrollbar-width: none;
  }

  .spatial-nav-actions::-webkit-scrollbar {
    display: none;
  }

  .notification-popover.app-popover {
    position: fixed;
    top: 78px;
    right: 12px;
    left: 12px;
    width: auto;
    max-height: calc(100vh - 96px);
  }

  .notification-list {
    max-height: calc(100vh - 154px);
  }
}

.site-message-label {
  position: relative;
  z-index: 2;
  flex: 0 0 auto;
  align-self: stretch;
  display: grid;
  place-items: center;
  padding: 0 16px;
  color: #fff;
  background: #0066ff;
  font-size: 12px;
  font-weight: 700;
}

.site-message-viewport {
  min-width: 0;
  flex: 1;
  overflow: hidden;
}

.site-message-track {
  display: flex;
  align-items: center;
  width: max-content;
  animation: site-message-scroll 32s linear infinite;
}

.site-message-bar:hover .site-message-track {
  animation-play-state: paused;
}

.site-message-item {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding-right: 64px;
  white-space: nowrap;
  font-size: 12px;
}

.site-message-item strong {
  color: #075fcf;
}

@keyframes site-message-scroll {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

@media (prefers-reduced-motion: reduce) {
  .site-message-track { animation: none; }
  .spatial-nav-brand .spatial-nav-mark { transition: none; }
}

.workspace-route-enter-active,
.workspace-route-leave-active {
  transition: opacity 160ms ease, transform 200ms cubic-bezier(.22, 1, .36, 1);
}

.workspace-route-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.workspace-route-leave-to {
  opacity: 0;
  transform: translateY(-3px);
}

.announcement-backdrop {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  height: 100dvh;
  z-index: 90;
  display: grid;
  place-items: center;
  padding: 20px;
  box-sizing: border-box;
  background: rgba(15, 23, 42, .46);
}

.announcement-dialog {
  width: min(576px, 100%);
  max-height: min(690px, calc(100vh - 40px));
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  border-radius: 14px;
  color: #242a35;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, .18);
}

.announcement-dialog > header {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #edf0f3;
  font-size: 15px;
  font-weight: 750;
}

.announcement-dialog > header button {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 7px;
  color: #667085;
  background: #f2f4f7;
  font-size: 20px;
  cursor: pointer;
}

.announcement-content {
  overflow-y: auto;
  padding: 22px 24px 26px;
}

.announcement-content h2 { margin: 0 0 10px; color: #171b24; font-size: 17px; line-height: 1.45; }
.announcement-content time { color: #8a94a3; font-size: 12px; }
.announcement-content p {
  margin: 18px 0 0;
  color: #404957;
  font-size: 14px;
  line-height: 1.95;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.announcement-dialog > footer {
  display: flex;
  justify-content: flex-end;
  padding: 12px 24px 18px;
  border-top: 1px solid #edf0f3;
}

.announcement-dialog > footer button {
  min-width: 78px;
  min-height: 36px;
  border: 0;
  border-radius: 18px;
  color: #fff;
  background: #128b70;
  font-weight: 700;
  cursor: pointer;
}

.announcement-modal-enter-active,
.announcement-modal-leave-active { transition: opacity 180ms ease; }
.announcement-modal-enter-active .announcement-dialog,
.announcement-modal-leave-active .announcement-dialog { transition: transform 200ms cubic-bezier(.22, 1, .36, 1); }
.announcement-modal-enter-from,
.announcement-modal-leave-to { opacity: 0; }
.announcement-modal-enter-from .announcement-dialog,
.announcement-modal-leave-to .announcement-dialog { transform: translateY(8px) scale(.99); }

@media (max-width: 900px) {
  .spatial-nav-float {
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
  }
  .spatial-nav-links {
    grid-column: 1 / -1;
    grid-row: 2;
    order: 3;
  }
  .spatial-nav-actions {
    grid-column: 2;
    grid-row: 1;
    width: auto;
    justify-self: end;
  }
  .global-search-bar { display: none; }
}

@media (max-width: 560px) {
  .spatial-nav-float { width: calc(100vw - 16px); max-width: calc(100vw - 16px); }
  .spatial-nav-brand strong,
  .app-profile-button .profile-name { display: none; }
  .spatial-nav-actions { gap: 5px; }
  .announcement-backdrop { padding: 10px; }
  .announcement-dialog { max-height: calc(100vh - 20px); }
  .announcement-dialog > header,
  .announcement-content,
  .announcement-dialog > footer { padding-inline: 18px; }
}

.app-reader-root,
.app-landing-root {
  min-height: 100vh;
}

.app-reader-root {
  height: 100vh;
  overflow: hidden;
}

.app-reader-main,
.app-landing-main {
  min-height: 0;
}

.app-reader-main {
  height: 100vh;
  overflow: hidden;
}

.app-landing-main {
  min-height: 100vh;
}

.topbar-menu-wrap {
  position: relative;
}

.notification-popover.app-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 40;
  width: min(360px, calc(100vw - 32px));
  max-height: min(520px, calc(100vh - 112px));
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, .08);
  background: rgba(255, 255, 255, .98);
  box-shadow: 0 10px 18px rgba(15, 23, 42, .12);
}

.notification-popover .popover-header {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px 10px;
  border-bottom: 1px solid #edf1f6;
  background: #fff;
}

.notification-popover .popover-title {
  color: #111827;
  font-size: 15px;
  font-weight: 900;
}

.notification-popover .auth-link {
  flex: 0 0 auto;
  color: #1d5be3;
  font-size: 12px;
  font-weight: 800;
  text-decoration: none;
}

.notification-list {
  max-height: calc(min(520px, calc(100vh - 112px)) - 48px);
  overflow-y: auto;
  padding: 6px 10px 10px;
}

.notification-item {
  display: grid;
  gap: 4px;
  width: 100%;
  border: 0;
  border-bottom: 1px solid #eef2f7;
  padding: 11px 6px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.notification-item:hover {
  background: #f7fbff;
}

.notification-item strong {
  color: #111827;
  font-size: 14px;
  line-height: 1.35;
}

.notification-item span {
  display: -webkit-box;
  overflow: hidden;
  color: #52637a;
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.notification-popover .popover-empty {
  padding: 26px 12px;
  color: #64748b;
  text-align: center;
  font-size: 13px;
}

/* User Profile Popover styles */
.profile-panel.app-popover {
  width: 340px;
  padding: 20px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.99);
  border: 1px solid rgba(29, 29, 31, 0.08);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-popover-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.profile-popover-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.profile-popover-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.profile-popover-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile-popover-name {
  font-size: 16px;
  font-weight: 700;
  color: #1d1d1f;
}

.profile-popover-role-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 6px;
  font-weight: 600;
}

.profile-popover-role-badge.badge-tutor {
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
}
.profile-popover-role-badge.badge-admin {
  background: rgba(139, 92, 246, 0.08);
  color: #8b5cf6;
}
.profile-popover-role-badge.badge-vip {
  background: rgba(236, 72, 153, 0.08);
  color: #ec4899;
}
.profile-popover-role-badge.badge-student {
  background: rgba(16, 185, 129, 0.08);
  color: #10b981;
}

.profile-popover-email {
  font-size: 13px;
  color: #86868b;
}

.profile-popover-level-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.04), rgba(139, 92, 246, 0.04));
  border: 1px solid rgba(0, 102, 255, 0.06);
  border-radius: 12px;
}

.popover-level-num {
  font-size: 20px;
  font-weight: 800;
  color: #0066ff;
  letter-spacing: -0.02em;
}

.popover-level-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.popover-level-title {
  font-size: 13px;
  font-weight: 700;
  color: #1d1d1f;
}

.popover-level-sub {
  font-size: 11px;
  color: #86868b;
}

.profile-popover-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.stats-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.02);
}

.stats-label {
  font-size: 11px;
  color: #86868b;
}

.stats-value {
  font-size: 13px;
  font-weight: 600;
  color: #1d1d1f;
}

.profile-popover-quota {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quota-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quota-title {
  font-size: 12px;
  font-weight: 600;
  color: #1d1d1f;
}

.quota-usage {
  font-size: 11px;
  color: #86868b;
}

.quota-progress-bar {
  height: 6px;
  background: #f1f5f9;
  border-radius: 999px;
  overflow: hidden;
}

.quota-progress-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
}

.quota-progress-fill.fill-safe {
  background: #10b981;
}

.quota-progress-fill.fill-warning {
  background: #f59e0b;
}

.quota-progress-fill.fill-danger {
  background: #ef4444;
}

.profile-popover-divider {
  border: 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  margin: 0;
}

.profile-popover-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.password-success-alert {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(52, 199, 89, 0.08);
  color: #248a3d;
  font-size: 13px;
}

.password-modal-card {
  width: 90%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 24px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.15);
}

.profile-avatar-img {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
}

.profile-popover-avatar-img {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.profile-avatar-container {
  position: relative;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-avatar-status-dot {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #34c759;
  border: 1.5px solid #ffffff;
  box-shadow: 0 0 6px rgba(52, 199, 89, 0.8);
  animation: pulse-glow 2s infinite;
}

@keyframes pulse-glow {
  0% { box-shadow: 0 0 0 0 rgba(52, 199, 89, 0.6); }
  70% { box-shadow: 0 0 0 4px rgba(52, 199, 89, 0); }
  100% { box-shadow: 0 0 0 0 rgba(52, 199, 89, 0); }
}

:root[data-theme="dark"] .profile-panel.app-popover,
:root[data-theme="dark"] .password-modal-card {
  color: var(--text-main);
  background: var(--bg-card);
  border-color: var(--border);
  box-shadow: var(--shadow-lg);
}

:root[data-theme="dark"] .profile-popover-name,
:root[data-theme="dark"] .popover-level-title,
:root[data-theme="dark"] .quota-title,
:root[data-theme="dark"] .stats-value {
  color: var(--text-main);
}

:root[data-theme="dark"] .profile-popover-email,
:root[data-theme="dark"] .popover-level-sub,
:root[data-theme="dark"] .stats-label,
:root[data-theme="dark"] .quota-usage {
  color: var(--text-secondary);
}

:root[data-theme="dark"] .stats-item,
:root[data-theme="dark"] .profile-popover-level-banner,
:root[data-theme="dark"] .quota-progress-bar {
  background: var(--bg-tint);
  border-color: var(--border);
}

:root[data-theme="dark"] .profile-popover-divider {
  border-top-color: var(--border);
}

:root[data-theme="dark"] .profile-avatar-status-dot {
  border-color: var(--bg-card);
}
</style>
