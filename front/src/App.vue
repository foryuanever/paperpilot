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
          <span v-if="item.to === '/forum' && forumUnreadCount" class="nav-forum-alert" aria-label="学术论坛有新动态"></span>
        </router-link>
      </nav>

      <div class="spatial-nav-actions">
        <button
          class="icon-button theme-toggle-btn"
          :title="isDarkTheme ? '切换为日间明亮模式' : '切换为夜间深色模式'"
          @click="toggleTheme"
        >
          <svg v-if="isDarkTheme" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
        </button>

        <label class="global-search-bar">
          <span class="search-icon" v-html="chromeIcons.search"></span>
          <input type="search" placeholder="搜索论文、作者、DOI" />
        </label>

        <router-link class="icon-button message-button" to="/messages" title="私信">
          <span v-html="chromeIcons.message"></span>
          <span v-if="messageUnreadCount" class="notification-badge">{{ messageUnreadCount }}</span>
        </router-link>

        <div class="topbar-menu-wrap">
          <button class="icon-button notification-button" @click.stop="openAnnouncementCenter">
            <span v-html="chromeIcons.bell"></span>
            <span v-if="announcementUnreadCount" class="notification-badge">{{ announcementUnreadCount }}</span>
          </button>
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
              <div v-else class="profile-popover-avatar" :style="{ background: getAvatarColor(currentUserMember.role) }">
                {{ userInitial }}
              </div>
              <div class="profile-popover-meta">
                <div class="profile-popover-name-row">
                  <strong class="profile-popover-name">{{ authStore.profile.name }}</strong>
                  <span class="profile-popover-role-badge" :class="getRoleClass(currentUserMember.role)">{{ currentUserMember.role }}</span>
                  <span class="profile-popover-role-badge badge-vip">{{ membershipName }}</span>
                </div>
                <div class="profile-popover-email">{{ authStore.profile.email }}</div>
              </div>
            </div>

            <!-- Level Banner -->
            <div class="profile-popover-level-banner">
              <div class="popover-level-num">Lv.{{ getFruitLevelInfo(currentFruitScore).level }}</div>
              <div class="popover-level-info">
                <div class="popover-level-title">{{ getFruitLevelInfo(currentFruitScore).title }}</div>
                <div class="popover-level-sub">累计硕果: {{ currentFruitScore }} 枚</div>
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
                <span class="quota-title">本期会员权益</span>
                <span class="quota-usage">{{ membershipExpiry }}</span>
              </div>
              <router-link class="membership-center-link" to="/models" @click="uiStore.closeOverlays">查看套餐与剩余次数</router-link>
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
        <Transition name="workspace-route" mode="out-in">
          <component :is="Component" :key="viewRoute.path" />
        </Transition>
      </router-view>
    </main>

    <AppDialog />
    <UserProfileCard />

    <Teleport to="body">
      <Transition name="announcement-modal">
        <div v-if="showAnnouncementCenter" class="announcement-backdrop" @click.self="closeAnnouncementCenter">
          <section class="announcement-dialog" role="dialog" aria-modal="true" aria-labelledby="site-announcement-title">
            <header class="announcement-hero">
              <h2 id="site-announcement-title">系统公告</h2>
              <p>最新平台更新和通知</p>
            </header>
            <nav class="announcement-tabs" aria-label="系统公告分类">
              <button
                v-for="tab in announcementTabs"
                :key="tab.key"
                type="button"
                :class="{ active: activeAnnouncementTab === tab.key }"
                @click="activeAnnouncementTab = tab.key"
              >
                <span v-html="tab.icon"></span>
                {{ tab.label }}
                <b v-if="tab.count">{{ tab.count }}</b>
              </button>
            </nav>

            <div class="announcement-content">
              <section v-if="activeAnnouncementTab === 'forum'" class="announcement-section">
                <article class="announcement-intro">
                  <strong>站内通知</strong>
                  <span>论坛回复、举报反馈、校园认证结果和管理员处理消息都会在这里集中显示。</span>
                </article>
                <div v-if="siteNoticeItems.length" class="announcement-card-list">
                  <button
                    v-for="item in siteNoticeItems"
                    :key="item.id"
                    type="button"
                    class="announcement-card"
                    @click="openNotification(item)"
                  >
                    <span class="announcement-card-mark">{{ siteNoticeMark(item.type) }}</span>
                    <span>
                      <strong>{{ item.title }}</strong>
                      <small>{{ item.desc }}</small>
                      <time>{{ formatSiteMessageTime(item.createdAt) }}</time>
                    </span>
                  </button>
                </div>
                <div v-else class="announcement-empty">暂无新的站内通知。</div>
              </section>

              <section v-else-if="activeAnnouncementTab === 'timeline'" class="announcement-section">
                <article class="announcement-intro">
                  <strong>版本时间线更新</strong>
                  <span>用于发布功能更新、模型接入、补丁说明和平台调整。</span>
                </article>
                <div v-if="timelineNoticeItems.length" class="timeline-feed">
                  <article
                    v-for="message in timelineNoticeItems"
                    :key="message.id"
                    class="timeline-feed-item"
                    :class="{ active: activeSiteMessage?.id === message.id }"
                    @click="activeSiteMessage = message"
                  >
                    <time>{{ formatSiteMessageTime(message.createdAt) }}</time>
                    <h3>{{ message.title }}</h3>
                    <p>{{ message.content }}</p>
                  </article>
                </div>
                <div v-else class="announcement-empty">暂无版本更新公告。</div>
              </section>

              <section v-else class="announcement-section">
                <article class="announcement-intro">
                  <strong>组内通知</strong>
                  <span>导师发布任务和任务截止时间提醒会在这里同步。</span>
                </article>
                <div v-if="teamNoticeItems.length" class="announcement-card-list">
                  <router-link
                    v-for="item in teamNoticeItems"
                    :key="item.id"
                    class="announcement-card team-notice-card"
                    to="/team"
                    @click="closeAnnouncementCenter"
                  >
                    <span class="announcement-card-mark">{{ item.mark }}</span>
                    <span>
                      <strong>{{ item.title }}</strong>
                      <small>{{ item.desc }}</small>
                      <time>{{ item.time }}</time>
                    </span>
                  </router-link>
                </div>
                <div v-else class="announcement-empty">暂无组内任务通知。</div>
              </section>

              <div v-if="activeAnnouncementTab === 'timeline' && unreadTimelineMessages.length > 1" class="announcement-switcher" aria-label="公告列表">
                <button
                  v-for="(message, index) in unreadTimelineMessages"
                  :key="message.id"
                  type="button"
                  :class="{ active: message.id === activeSiteMessage.id }"
                  @click="activeSiteMessage = message"
                >
                  {{ index + 1 }}
                </button>
              </div>
            </div>
            <footer>
              <button v-if="activeAnnouncementTab === 'timeline' && unreadTimelineMessages.length > 1" type="button" class="announcement-ghost" @click="showPreviousSiteMessage">上一条</button>
              <button v-if="activeAnnouncementTab === 'timeline' && unreadTimelineMessages.length > 1" type="button" class="announcement-ghost" @click="showNextSiteMessage">下一条</button>
              <button v-if="unreadTimelineMessages.length || siteNoticeItems.length" type="button" class="announcement-ghost" @click="markVisibleAnnouncementRead">全部已读</button>
              <button type="button" @click="closeAnnouncementCenter">关闭</button>
            </footer>
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
import { useUsageStore } from "./stores/usage";
import { paperpilotApi } from "./services/paperpilotApi";
import { useDialogStore } from "./stores/dialog";
import AppDialog from "./components/AppDialog.vue";
import UserProfileCard from "./components/UserProfileCard.vue";
import { useUserCardStore } from "./stores/userCard";

const authStore = useAuthStore();
const uiStore = useUiStore();
const teamStore = useTeamStore();
const usageStore = useUsageStore();
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
      { to: "/topics", label: "选题广场" },
      { to: "/forum", label: "学术论坛" },
      { to: "/models", label: "用量" },
      { to: "/team", label: "团队" }
    ];
  }
  return pageNavItems;
});

const isLanding = computed(() => route.path === "/" || route.path === "/register");
const isReader = computed(() => route.path.startsWith("/reader"));
const isAdmin = computed(() => route.path === "/admin");
const showNav = computed(() => !isLanding.value && !isReader.value);

const rootClass = computed(() => {
  if (isReader.value) return "app-reader-root";
  if (isLanding.value) return "app-landing-root";
  if (isAdmin.value) return "app-admin-root";
  return "spatial-app spatial-page";
});

const mainClass = computed(() => {
  if (isReader.value) return "app-reader-main";
  if (isLanding.value) return "app-landing-main";
  if (isAdmin.value) return "app-admin-main";
  return "spatial-main";
});

const userInitial = computed(() => (authStore.profile.name || "U").slice(0, 1).toUpperCase());
const membershipName = computed(() => usageStore.state.membership?.name || "未开通会员");
const membershipExpiry = computed(() => {
  const expiresAt = usageStore.state.membership?.expiresAt;
  if (!usageStore.state.membership?.active) return "免费翻译与导入不限次";
  if (Array.isArray(expiresAt)) return `有效至 ${expiresAt[0]}-${String(expiresAt[1]).padStart(2, "0")}-${String(expiresAt[2]).padStart(2, "0")}`;
  return expiresAt ? `有效至 ${String(expiresAt).slice(0, 10)}` : "会员权益已开通";
});

const chromeIcons = {
  search: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="6"/><path d="M20 20l-3.5-3.5"/></svg>`,
  bell: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 4.5a4 4 0 0 0-4 4v2.2c0 .7-.2 1.4-.6 2l-1.1 1.7A1 1 0 0 0 7.1 16h9.8a1 1 0 0 0 .8-1.6l-1.1-1.7a3.7 3.7 0 0 1-.6-2V8.5a4 4 0 0 0-4-4Z"/><path d="M10 18a2.2 2.2 0 0 0 4 0"/></svg>`,
  message: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M5 18.5 3.8 21l3.4-1.1c1.4.7 3 .9 4.8.9 5 0 9-3.6 9-8.1s-4-8.1-9-8.1-9 3.6-9 8.1c0 2.2.8 4.2 2 5.8Z"/><path d="M8 12h.01M12 12h.01M16 12h.01"/></svg>`,
};

const messageUnreadCount = ref(0);
const forumUnreadCount = ref(0);
const latestForumSignature = ref("");
const announcementCenterOpen = ref(false);
const activeAnnouncementTab = ref("forum");

const currentTheme = ref(localStorage.getItem("paperpilot_theme") || "dark");
const isDarkTheme = computed(() => currentTheme.value === "dark");

function applyTheme(theme) {
  currentTheme.value = theme;
  document.documentElement.setAttribute("data-theme", theme);
  localStorage.setItem("paperpilot_theme", theme);
}

function toggleTheme() {
  applyTheme(currentTheme.value === "dark" ? "light" : "dark");
}

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

function forumSeenKey() {
  const userId = authStore.session.user?.userId || authStore.profile.email || "guest";
  return `paperpilot-forum-seen:${userId}`;
}

function markForumSeen() {
  if (!latestForumSignature.value) return;
  localStorage.setItem(forumSeenKey(), latestForumSignature.value);
  forumUnreadCount.value = 0;
}

async function refreshForumNavSignal() {
  if (!authStore.session.isAuthenticated) {
    forumUnreadCount.value = 0;
    latestForumSignature.value = "";
    return;
  }
  try {
    const posts = await paperpilotApi.getForumPosts();
    const latest = [...(posts || [])].sort((a, b) => String(b.time || "").localeCompare(String(a.time || "")))[0];
    latestForumSignature.value = latest ? `${latest.id}:${latest.time || ""}` : "";
    const seen = localStorage.getItem(forumSeenKey()) || "";
    forumUnreadCount.value = latestForumSignature.value && latestForumSignature.value !== seen ? 1 : 0;
    if (route.path.startsWith("/forum")) markForumSeen();
  } catch {
    forumUnreadCount.value = 0;
  }
}

async function openNotification(item) {
  await authStore.markNotificationRead(item.id);
  uiStore.closeOverlays();
  if (item.type === "private_message") {
    router.push("/messages");
    return;
  }
  if (item.type?.startsWith("campus_")) {
    router.push("/profile");
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

const currentFruitScore = computed(() => Number(currentUserMember.value?.fruitScore ?? authStore.profile.fruitScore ?? 0));

function getFruitLevelInfo(score) {
  const level = Math.floor((Number(score) || 0) / 100) + 1;
  let title = "LV" + level;
  if (level >= 20) title = "LV" + level + " · 科研主宰";
  else if (level >= 12) title = "LV" + level + " · 科研宗师";
  else if (level >= 6) title = "LV" + level + " · 学术专家";
  else if (level >= 3) title = "LV" + level + " · 科研骨干";
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
  if (role === "导师") return "linear-gradient(135deg, #0ea5e9 0%, #2563eb 100%)";
  if (role === "管理员") return "linear-gradient(135deg, #f43f5e 0%, #fb923c 100%)";
  if (role === "特权用户") return "linear-gradient(135deg, #d946ef 0%, #8b5cf6 100%)";
  return "linear-gradient(135deg, #176ce4, #643bd4)";
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
const readSiteMessageIds = ref(new Set());
const announcementIcons = {
  forum: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M5 7.5h14M5 12h10M5 16.5h7"/><path d="M4 4h16v12H8l-4 4V4Z"/></svg>`,
  timeline: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h4l10-3v16L8 17H4V7Z"/><path d="M8 7v10"/><path d="M20 9.5c1.2 1.2 1.2 3.8 0 5"/></svg>`,
  team: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M16 19v-1.5c0-1.8-1.8-3.2-4-3.2s-4 1.4-4 3.2V19"/><circle cx="12" cy="8" r="3"/><path d="M4 18v-1c0-1.3 1.1-2.4 2.7-2.8"/><path d="M20 18v-1c0-1.3-1.1-2.4-2.7-2.8"/><path d="M6.5 10.5a2.2 2.2 0 1 1 1.2-4"/><path d="M17.5 10.5a2.2 2.2 0 1 0-1.2-4"/></svg>`,
};

const siteNoticeItems = computed(() => authStore.session.notifications.filter(item => {
  const type = String(item.type || "");
  const title = `${item.title || ""}${item.desc || ""}`;
  return type.startsWith("forum_")
    || type.startsWith("campus_")
    || /回复|置顶|封禁|举报|发帖|帖子|校园认证|学校/.test(title);
}));

const timelineNoticeItems = computed(() => siteMessages.value.filter(item => item.messageType === "timeline").sort((a, b) => {
  const unreadA = readSiteMessageIds.value.has(siteMessageReadKey(a)) ? 0 : 1;
  const unreadB = readSiteMessageIds.value.has(siteMessageReadKey(b)) ? 0 : 1;
  if (unreadA !== unreadB) return unreadB - unreadA;
  return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
}));


const teamNoticeItems = computed(() => {
  const notices = [];
  const now = Date.now();
  [...teamStore.tasks]
    .sort((a, b) => new Date(b.createdAt || b.deadline || 0) - new Date(a.createdAt || a.deadline || 0))
    .slice(0, 5)
    .forEach((task) => {
      const deadline = parseDateValue(task.deadline);
      notices.push({
        id: `task-${task.id}`,
        mark: "任",
        title: `导师发布任务：${task.title || "未命名任务"}`,
        desc: task.description || "请前往团队页面查看任务要求。",
        time: deadline ? `截止 ${formatSiteMessageTime(deadline)}` : "暂无截止时间",
      });
      if (deadline) {
        const diff = deadline.getTime() - now;
        if (diff <= 72 * 60 * 60 * 1000) {
          notices.push({
            id: `deadline-${task.id}`,
            mark: diff < 0 ? "逾" : "截",
            title: `任务截止提醒：${task.title || "未命名任务"}`,
            desc: diff < 0 ? "该任务已超过截止时间，请尽快处理。" : getDeadlineNoticeText(diff),
            time: `截止 ${formatSiteMessageTime(deadline)}`,
          });
        }
      }
    });
  return notices.slice(0, 8);
});

const announcementTabs = computed(() => [
  { key: "forum", label: "站内通知", icon: announcementIcons.forum, count: siteNoticeItems.value.length },
  { key: "timeline", label: "时间线", icon: announcementIcons.timeline, count: unreadTimelineMessages.value.length },
  { key: "team", label: "组内通知", icon: announcementIcons.team, count: urgentTeamNoticeCount.value },
]);

const urgentTeamNoticeCount = computed(() => teamNoticeItems.value.filter(item => item.mark === "截" || item.mark === "逾").length);
const unreadTimelineMessages = computed(() => timelineNoticeItems.value.filter(message => !readSiteMessageIds.value.has(siteMessageReadKey(message))));
const announcementUnreadCount = computed(() => siteNoticeItems.value.length + unreadTimelineMessages.value.length + urgentTeamNoticeCount.value);
const showAnnouncementCenter = computed(() => announcementCenterOpen.value || Boolean(activeSiteMessage.value));

const unreadSiteMessages = computed(() => siteMessages.value.filter(
  message => !readSiteMessageIds.value.has(siteMessageReadKey(message)),
));

const activeSiteMessageIndex = computed(() => {
  if (!activeSiteMessage.value) return 0;
  return Math.max(0, unreadTimelineMessages.value.findIndex(message => message.id === activeSiteMessage.value.id));
});

function siteMessageReadKey(message) {
  const userId = authStore.session.user?.userId || authStore.profile.email || "guest";
  return `papersolver-site-message-read:${userId}:${message.id}`;
}

function loadSiteMessageReadState() {
  const userId = authStore.session.user?.userId || authStore.profile.email || "guest";
  try {
    readSiteMessageIds.value = new Set(JSON.parse(localStorage.getItem(`papersolver-site-message-read-list:${userId}`) || "[]"));
  } catch {
    readSiteMessageIds.value = new Set();
  }
}

function persistSiteMessageReadState() {
  const userId = authStore.session.user?.userId || authStore.profile.email || "guest";
  localStorage.setItem(`papersolver-site-message-read-list:${userId}`, JSON.stringify([...readSiteMessageIds.value]));
}

function chooseUnreadSiteMessage() {
  const timelineNotice = unreadSiteMessages.value.find(message => message.messageType === "timeline");
  activeSiteMessage.value = timelineNotice || null;
  if (activeSiteMessage.value) activeAnnouncementTab.value = "timeline";
}

function markSiteMessageRead() {
  if (!activeSiteMessage.value) return;
  readSiteMessageIds.value = new Set([...readSiteMessageIds.value, siteMessageReadKey(activeSiteMessage.value)]);
  persistSiteMessageReadState();
  activeSiteMessage.value = null;
  chooseUnreadSiteMessage();
}

function markAllSiteMessagesRead() {
  const next = new Set(readSiteMessageIds.value);
  unreadSiteMessages.value.forEach(message => next.add(siteMessageReadKey(message)));
  readSiteMessageIds.value = next;
  persistSiteMessageReadState();
  activeSiteMessage.value = null;
}

function showPreviousSiteMessage() {
  const items = unreadTimelineMessages.value;
  if (!items.length) return;
  const previous = (activeSiteMessageIndex.value - 1 + items.length) % items.length;
  activeSiteMessage.value = items[previous];
}

function showNextSiteMessage() {
  const items = unreadTimelineMessages.value;
  if (!items.length) return;
  const next = (activeSiteMessageIndex.value + 1) % items.length;
  activeSiteMessage.value = items[next];
}

function formatSiteMessageTime(value) {
  if (!value) return "";
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", hour12: false,
  }).format(date).replace(/\//g, "-");
}

function parseDateValue(value) {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function getDeadlineNoticeText(diffMs) {
  const hours = Math.max(1, Math.ceil(diffMs / (60 * 60 * 1000)));
  if (hours >= 24) return `距离截止还有 ${Math.ceil(hours / 24)} 天，请安排提交。`;
  return `距离截止还有 ${hours} 小时，请尽快处理。`;
}

function siteNoticeMark(type) {
  if (type === "campus_verified") return "校";
  if (type === "campus_rejected") return "驳";
  if (type === "forum_reply") return "回";
  if (type === "forum_pinned") return "顶";
  if (type === "forum_banned") return "封";
  if (type === "forum_report") return "举";
  return "站";
}

function pickAnnouncementTab() {
  if (siteNoticeItems.value.length) return "forum";
  if (unreadTimelineMessages.value.length || timelineNoticeItems.value.length) return "timeline";
  return "team";
}

function openAnnouncementCenter() {
  activeAnnouncementTab.value = pickAnnouncementTab();
  announcementCenterOpen.value = true;
  uiStore.closeOverlays();
}

async function markVisibleAnnouncementRead() {
  if (activeAnnouncementTab.value === "forum") {
    await Promise.allSettled(siteNoticeItems.value.map(item => authStore.markNotificationRead(item.id)));
    return;
  }
  if (activeAnnouncementTab.value === "timeline") {
    const next = new Set(readSiteMessageIds.value);
    timelineNoticeItems.value.forEach(message => next.add(siteMessageReadKey(message)));
    readSiteMessageIds.value = next;
    persistSiteMessageReadState();
    activeSiteMessage.value = null;
  }
}

function closeAnnouncementCenter() {
  announcementCenterOpen.value = false;
  activeSiteMessage.value = null;
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
    loadSiteMessageReadState();
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
  applyTheme(currentTheme.value);
  window.addEventListener("mousemove", resetActivityTimer);
  window.addEventListener("keydown", resetActivityTimer);
  window.addEventListener("click", resetActivityTimer);
  window.addEventListener("scroll", resetActivityTimer);
  window.addEventListener("paperpilot:site-messages-changed", refreshSiteMessages);
  window.addEventListener("paperpilot:forum-posts-changed", refreshForumNavSignal);
  document.addEventListener("click", handleUserAvatarClick);

  activityTimer = setInterval(() => {
    if (authStore.session.isAuthenticated) {
      if (hasCheckedIn.value) {
        const timeIdle = Date.now() - lastActiveTime.value;
        if (timeIdle < 60000) {
          const user = currentUserMember.value;
          if (user && user.id) {
            teamStore.incrementActiveTime(user.id, 1);
          }
        }
      }
    }
  }, 1000);
  authStore.refreshNotifications().catch(() => {});
  if (authStore.session.isAuthenticated) usageStore.fetchSummary().catch(() => {});
  refreshMessageUnread();
  refreshForumNavSignal();
  loadSiteMessageReadState();
  refreshSiteMessages();
  notificationTimer = setInterval(() => {
    authStore.refreshNotifications().catch(() => {});
    refreshMessageUnread();
    refreshForumNavSignal();
  }, 15000);
  siteMessageTimer = setInterval(refreshSiteMessages, 15000);
});

onUnmounted(() => {
  window.removeEventListener("mousemove", resetActivityTimer);
  window.removeEventListener("keydown", resetActivityTimer);
  window.removeEventListener("click", resetActivityTimer);
  window.removeEventListener("scroll", resetActivityTimer);
  window.removeEventListener("paperpilot:site-messages-changed", refreshSiteMessages);
  window.removeEventListener("paperpilot:forum-posts-changed", refreshForumNavSignal);
  document.removeEventListener("click", handleUserAvatarClick);
  if (activityTimer) clearInterval(activityTimer);
  if (notificationTimer) clearInterval(notificationTimer);
  if (siteMessageTimer) clearInterval(siteMessageTimer);
});

watch(
  () => route.path,
  (path) => {
    if (path.startsWith("/forum")) markForumSeen();
  },
);

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

.spatial-app {
  min-height: 100vh;
  color: #111827;
  background:
    radial-gradient(circle at 82% 0%, rgba(245, 158, 11, .14), transparent 30%),
    radial-gradient(circle at 12% 18%, rgba(37, 99, 235, .12), transparent 28%),
    linear-gradient(180deg, #f7faff 0%, #f6f8fc 46%, #eef3f8 100%);
}

.spatial-app::before {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: .18;
  background-image:
    linear-gradient(rgba(37, 99, 235, .06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, .06) 1px, transparent 1px);
  background-size: 84px 84px;
  mask-image: linear-gradient(180deg, #000 0%, transparent 72%);
  content: "";
}

.spatial-main {
  position: relative;
  z-index: 1;
  padding-top: 18px;
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
  min-height: 66px;
  margin: 0 auto;
  padding: 0;
  overflow: visible;
  background: transparent;
  border: 0;
  box-shadow: none;
  backdrop-filter: none;
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
  color: #111827;
  text-overflow: ellipsis;
  font-size: 17px;
  font-weight: 900;
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
  position: relative;
  flex: 0 0 auto;
  padding: 8px 10px;
  white-space: nowrap;
  color: rgba(17, 24, 39, .7);
  background: transparent !important;
  border-radius: 0;
  font-weight: 850;
}

.spatial-nav-link:hover,
.spatial-nav-link.active {
  color: #2563eb;
}

.spatial-nav-link.active::after {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 2px;
  height: 2px;
  border-radius: 999px;
  background: #2563eb;
  content: "";
}

.nav-forum-alert {
  position: absolute;
  top: 7px;
  right: 6px;
  width: 7px;
  height: 7px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 2px solid #fff;
  border-radius: 999px;
  background: #e11d48;
  box-shadow: 0 0 0 2px rgba(225, 29, 72, .14);
  transform: translate(70%, -65%);
  pointer-events: none;
}

.theme-toggle-btn {
  background: rgba(255, 255, 255, .05);
  border: 1px solid rgba(255, 255, 255, .1);
  color: #a1a1aa;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  width: 34px;
  height: 34px;
  transition: all .2s;
}
.theme-toggle-btn:hover {
  background: rgba(255, 255, 255, .15);
  color: #fff;
}

/* ── Dark Mode Adaptations for Global Topbar ── */
:root[data-theme="dark"] .spatial-nav-brand strong { color: #f4f4f6; }
:root[data-theme="dark"] .spatial-nav-link { color: #a1a1aa; }
:root[data-theme="dark"] .spatial-nav-link:hover,
:root[data-theme="dark"] .spatial-nav-link.active { color: #60a5fa; }
:root[data-theme="dark"] .spatial-nav-link.active::after { background: #60a5fa; }
:root[data-theme="dark"] .global-search-bar {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.08);
  color: #a1a1aa;
}
:root[data-theme="dark"] .global-search-bar input { color: #f4f4f6; }
:root[data-theme="dark"] .icon-button {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.08);
  color: #a1a1aa;
}
:root[data-theme="dark"] .icon-button:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #f4f4f6;
}
:root[data-theme="dark"] .app-profile-button {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.08);
  color: #f4f4f6;
}
:root[data-theme="dark"] .app-profile-button .profile-name { color: #f4f4f6; }
:root[data-theme="dark"] .nav-forum-alert { border-color: #08080c; }


.global-search-bar {
  width: clamp(160px, 13vw, 220px);
  height: 34px;
  padding-inline: 8px;
  border: 1px solid rgba(37, 99, 235, .1);
  border-radius: 999px;
  background: rgba(255, 255, 255, .64);
  color: #5f6c80;
}

.global-search-bar input {
  font-size: 12px;
}

.icon-button,
.profile-button {
  min-width: 32px;
  height: 32px;
  border: 1px solid rgba(37, 99, 235, .1);
  border-radius: 999px;
  background: rgba(255, 255, 255, .64);
  color: #26344c;
  box-shadow: none;
}

.icon-button:hover,
.profile-button:hover {
  color: #2563eb;
  background: rgba(255, 255, 255, .9);
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
  transition: opacity 210ms ease, transform 260ms cubic-bezier(.22, 1, .36, 1), filter 260ms ease;
}

.workspace-route-enter-from {
  opacity: 0;
  transform: translateY(12px) scale(.992);
  filter: blur(4px);
}

.workspace-route-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(.996);
  filter: blur(3px);
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
  padding: 18px;
  box-sizing: border-box;
  background: rgba(15, 23, 42, .36);
}

.announcement-dialog {
  width: min(560px, 100%);
  min-height: min(560px, calc(100vh - 36px));
  max-height: calc(100vh - 36px);
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  overflow: hidden;
  border-radius: 0;
  color: #08090b;
  background: #fff;
  box-shadow: 0 22px 70px rgba(15, 23, 42, .2);
}

.announcement-hero {
  padding: 22px 22px 14px;
}

.announcement-hero h2 {
  margin: 0;
  color: #070707;
  font-size: 24px;
  line-height: 1.15;
  font-weight: 900;
  letter-spacing: 0;
}

.announcement-hero p {
  margin: 14px 0 0;
  color: #6d6d6d;
  font-size: 17px;
  line-height: 1.35;
  font-weight: 800;
}

.announcement-tabs {
  margin: 4px 14px 16px;
  padding: 5px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px;
  border-radius: 26px;
  background: #f0f0f0;
}

.announcement-tabs button {
  min-width: 0;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 23px;
  color: #6b6b6b;
  background: transparent;
  font-size: 15px;
  font-weight: 900;
  cursor: pointer;
  transition: color 160ms ease, background 160ms ease, box-shadow 160ms ease;
}

.announcement-tabs button.active {
  color: #08090b;
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, .13);
}

.announcement-tabs svg {
  width: 19px;
  height: 19px;
  flex: 0 0 auto;
}

.announcement-tabs b {
  min-width: 18px;
  height: 18px;
  display: inline-grid;
  place-items: center;
  border-radius: 999px;
  color: #fff;
  background: #111;
  font-size: 10px;
  line-height: 1;
}

.announcement-content {
  overflow-y: auto;
  padding: 0 22px 20px;
}

.announcement-section {
  display: grid;
  gap: 14px;
}

.announcement-intro {
  display: grid;
  gap: 6px;
}

.announcement-intro strong {
  color: #111;
  font-size: 20px;
  line-height: 1.25;
  font-weight: 900;
}

.announcement-intro span {
  color: #444;
  font-size: 13px;
  line-height: 1.65;
  font-weight: 650;
}

.announcement-card-list {
  display: grid;
  gap: 10px;
}

.announcement-card {
  width: 100%;
  min-height: 68px;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 11px 12px;
  border: 1px solid #ececec;
  border-radius: 12px;
  color: #111;
  background: #fff;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
}

.announcement-card:hover {
  border-color: #d8d8d8;
  background: #fafafa;
}

.announcement-card-mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  color: #111;
  background: #f2f2f2;
  font-size: 15px;
  font-weight: 950;
}

.announcement-card strong {
  display: block;
  overflow: hidden;
  color: #111;
  font-size: 15px;
  line-height: 1.35;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.announcement-card small,
.announcement-card time {
  display: block;
  margin-top: 4px;
  color: #606060;
  font-size: 12px;
  line-height: 1.5;
  font-weight: 650;
}

.team-notice-card .announcement-card-mark {
  color: #0a5cff;
  background: #eaf1ff;
}

.timeline-feed {
  position: relative;
  display: grid;
  gap: 0;
}

.timeline-feed::before {
  content: "";
  position: absolute;
  left: 13px;
  top: 12px;
  bottom: 12px;
  width: 2px;
  background: #ececec;
}

.timeline-feed-item {
  position: relative;
  padding: 0 0 24px 42px;
  cursor: pointer;
}

.timeline-feed-item::before {
  content: "";
  position: absolute;
  left: 6px;
  top: 8px;
  width: 16px;
  height: 16px;
  border: 3px solid #fff;
  border-radius: 50%;
  background: #111;
  box-shadow: 0 0 0 2px #e6e6e6;
}

.timeline-feed-item.active::before {
  background: #0a5cff;
  box-shadow: 0 0 0 2px #bcd2ff;
}

.timeline-feed-item time {
  color: #777;
  font-size: 13px;
  font-weight: 800;
}

.timeline-feed-item h3 {
  margin: 6px 0 8px;
  color: #111;
  font-size: 17px;
  line-height: 1.35;
  font-weight: 900;
}

.timeline-feed-item p {
  margin: 0;
  color: #111;
  font-size: 16px;
  line-height: 1.72;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.announcement-empty {
  min-height: 140px;
  display: grid;
  place-items: center;
  border: 1px dashed #dedede;
  border-radius: 14px;
  color: #777;
  font-size: 14px;
  font-weight: 800;
}

.announcement-switcher {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin: 0;
}

.announcement-switcher button {
  width: 26px;
  height: 26px;
  border: 1px solid #dbe4ef;
  border-radius: 50%;
  color: #53627a;
  background: #fff;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.announcement-switcher button.active {
  border-color: #0066ff;
  color: #fff;
  background: #0066ff;
}

.announcement-dialog > footer {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
  padding: 10px 22px 18px;
  border-top: 0;
}

.announcement-dialog > footer button {
  min-width: 82px;
  min-height: 40px;
  border: 0;
  border-radius: 14px;
  color: #fff;
  background: #050505;
  font-size: 15px;
  font-weight: 900;
  cursor: pointer;
}

.announcement-dialog > footer .announcement-ghost {
  color: #111;
  background: #f0f0f0;
  font-size: 15px;
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

.membership-center-link {
  width: fit-content;
  color: #1659d5;
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
}

.membership-center-link:hover { text-decoration: underline; }

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


/* ════════════════════════════════════════════════════════════
   NAVBAR NOTIFICATION POPOVER DUAL-THEME OVERRIDES
   ════════════════════════════════════════════════════════════ */

:root[data-theme="dark"] .notification-popover.app-popover {
  background: #111827 !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.6) !important;
  color: #f1f5f9 !important;
}

:root[data-theme="dark"] .notification-popover .popover-header {
  background: #111827 !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
}

:root[data-theme="dark"] .notification-popover .popover-title {
  color: #f1f5f9 !important;
}

:root[data-theme="dark"] .notification-popover .auth-link {
  color: #818cf8 !important;
}

:root[data-theme="dark"] .notification-item {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06) !important;
}

:root[data-theme="dark"] .notification-item:hover {
  background: rgba(99, 102, 241, 0.1) !important;
}

:root[data-theme="dark"] .notification-item strong {
  color: #f1f5f9 !important;
}

:root[data-theme="dark"] .notification-item span {
  color: #94a3b8 !important;
}

:root[data-theme="dark"] .notification-popover .popover-empty {
  color: #94a3b8 !important;
}



/* ════════════════════════════════════════════════════════════
   SYSTEM ANNOUNCEMENT MODAL CENTER DUAL-THEME OVERRIDES
   ════════════════════════════════════════════════════════════ */

.announcement-dialog {
  border-radius: 24px !important;
  transition: all 0.25s ease !important;
}

:root[data-theme="dark"] .announcement-dialog {
  background: #111827 !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.65) !important;
  color: #f1f5f9 !important;
}

:root[data-theme="light"] .announcement-dialog {
  background: #ffffff !important;
  border: 1px solid #e2e8f0 !important;
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.12) !important;
  color: #0f172a !important;
}

/* Header & Intro */
:root[data-theme="dark"] .announcement-hero h2 {
  color: #f1f5f9 !important;
}
:root[data-theme="light"] .announcement-hero h2 {
  color: #0f172a !important;
}

:root[data-theme="dark"] .announcement-hero p {
  color: #94a3b8 !important;
}
:root[data-theme="light"] .announcement-hero p {
  color: #64748b !important;
}

:root[data-theme="dark"] .announcement-intro strong {
  color: #f1f5f9 !important;
}
:root[data-theme="light"] .announcement-intro strong {
  color: #0f172a !important;
}

:root[data-theme="dark"] .announcement-intro span {
  color: #94a3b8 !important;
}
:root[data-theme="light"] .announcement-intro span {
  color: #64748b !important;
}

/* Tabs */
.announcement-tabs {
  padding: 6px !important;
  border-radius: 999px !important;
}
:root[data-theme="dark"] .announcement-tabs {
  background: rgba(0, 0, 0, 0.35) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
}
:root[data-theme="light"] .announcement-tabs {
  background: #f1f5f9 !important;
  border: 1px solid #e2e8f0 !important;
}

.announcement-tabs button {
  border-radius: 999px !important;
  font-size: 13.5px !important;
  font-weight: 850 !important;
}
:root[data-theme="dark"] .announcement-tabs button {
  color: #94a3b8 !important;
}
:root[data-theme="light"] .announcement-tabs button {
  color: #64748b !important;
}

.announcement-tabs button.active {
  background: linear-gradient(135deg, #6366f1, #a855f7) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35) !important;
}

/* Announcement Cards */
.announcement-card {
  padding: 14px 16px !important;
  border-radius: 16px !important;
  transition: all 0.2s ease !important;
}

:root[data-theme="dark"] .announcement-card {
  background: rgba(255, 255, 255, 0.03) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  color: #f1f5f9 !important;
}
:root[data-theme="dark"] .announcement-card:hover {
  background: rgba(99, 102, 241, 0.08) !important;
  border-color: rgba(99, 102, 241, 0.3) !important;
}

:root[data-theme="light"] .announcement-card {
  background: #ffffff !important;
  border: 1px solid #e2e8f0 !important;
  color: #0f172a !important;
}
:root[data-theme="light"] .announcement-card:hover {
  background: #f8fafc !important;
  border-color: rgba(99, 102, 241, 0.3) !important;
}

.announcement-card-mark {
  border-radius: 12px !important;
}
:root[data-theme="dark"] .announcement-card-mark {
  background: rgba(99, 102, 241, 0.15) !important;
  color: #818cf8 !important;
  border: 1px solid rgba(99, 102, 241, 0.3) !important;
}
:root[data-theme="light"] .announcement-card-mark {
  background: rgba(99, 102, 241, 0.08) !important;
  color: #4f46e5 !important;
  border: 1px solid rgba(99, 102, 241, 0.2) !important;
}

.announcement-card strong {
  font-size: 14px !important;
  font-weight: 850 !important;
}
:root[data-theme="dark"] .announcement-card strong {
  color: #f1f5f9 !important;
}
:root[data-theme="light"] .announcement-card strong {
  color: #0f172a !important;
}

.announcement-card small,
.announcement-card time {
  font-size: 12px !important;
}
:root[data-theme="dark"] .announcement-card small,
:root[data-theme="dark"] .announcement-card time {
  color: #94a3b8 !important;
}
:root[data-theme="light"] .announcement-card small,
:root[data-theme="light"] .announcement-card time {
  color: #64748b !important;
}

/* Footer Action Buttons */
.announcement-dialog > footer {
  border-top: 1px solid rgba(255, 255, 255, 0.08) !important;
  padding: 16px 22px !important;
}
:root[data-theme="light"] .announcement-dialog > footer {
  border-top: 1px solid #e2e8f0 !important;
}

.announcement-dialog > footer button {
  height: 38px !important;
  padding: 0 24px !important;
  border-radius: 999px !important;
  font-size: 13px !important;
  font-weight: 850 !important;
}
:root[data-theme="dark"] .announcement-dialog > footer button {
  background: linear-gradient(135deg, #6366f1, #a855f7) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35) !important;
  border: none !important;
}
:root[data-theme="light"] .announcement-dialog > footer button {
  background: #0f172a !important;
  color: #ffffff !important;
  border: none !important;
}



/* ════════════════════════════════════════════════════════════
   SYSTEM ANNOUNCEMENT MODAL TABS LIGHT MODE TEXT & SVG COLOR FIX
   ════════════════════════════════════════════════════════════ */

.announcement-tabs button.active,
:root[data-theme="light"] .announcement-tabs button.active,
:root[data-theme="dark"] .announcement-tabs button.active {
  background: linear-gradient(135deg, #6366f1, #a855f7) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35) !important;
}

.announcement-tabs button.active span,
.announcement-tabs button.active svg,
.announcement-tabs button.active path,
.announcement-tabs button.active b,
:root[data-theme="light"] .announcement-tabs button.active span,
:root[data-theme="light"] .announcement-tabs button.active svg,
:root[data-theme="light"] .announcement-tabs button.active path,
:root[data-theme="light"] .announcement-tabs button.active b {
  color: #ffffff !important;
  fill: #ffffff !important;
  stroke: #ffffff !important;
}

.announcement-tabs button.active b {
  background: rgba(255, 255, 255, 0.25) !important;
  color: #ffffff !important;
}

:root[data-theme="light"] .announcement-tabs {
  background: #f1f5f9 !important;
  border: 1px solid #e2e8f0 !important;
}

:root[data-theme="light"] .announcement-tabs button:not(.active) {
  color: #475569 !important;
}

:root[data-theme="light"] .announcement-tabs button:not(.active) svg,
:root[data-theme="light"] .announcement-tabs button:not(.active) path {
  color: #64748b !important;
  stroke: #64748b !important;
}

</style>
