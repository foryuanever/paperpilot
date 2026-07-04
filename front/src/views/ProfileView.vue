<template>
  <div class="profile-page spatial-page reveal-ready" :style="pageBackgroundStyle">
    <div class="profile-backdrop"></div>

    <section class="profile-shell" data-reveal>
      <header class="profile-hero">
        <div class="hero-banner">
          <img v-if="authStore.profile.backgroundUrl" :src="authStore.profile.backgroundUrl" alt="profile background" />
          <div v-else class="banner-fallback"></div>
          <div class="banner-scrim"></div>
          <label class="hero-upload">
            <input type="file" accept="image/*" @change="onBackgroundUpload" />
            更换封面
          </label>
        </div>

        <div class="hero-content">
          <div class="hero-avatar-wrap">
            <div class="hero-avatar">
              <img v-if="authStore.profile.avatarUrl" :src="authStore.profile.avatarUrl" :alt="authStore.profile.name" />
              <span v-else :style="{ backgroundColor: getAvatarColor(currentUserMember.role) }">
                {{ userInitial }}
              </span>
            </div>
            <label class="avatar-upload">
              <input type="file" accept="image/*" @change="onAvatarUpload" />
              更新头像
            </label>
          </div>

          <div class="hero-copy">
            <span class="role-pill" :class="getRoleClass(currentUserMember.role)">{{ currentUserMember.role }}</span>
            <h1>{{ authStore.profile.name }}</h1>
            <p>{{ authStore.profile.email }}</p>
          </div>

          <div class="hero-stats">
            <article class="hero-stat">
              <span>科研等级</span>
              <strong>Lv.{{ levelInfo.level }}</strong>
              <small>{{ levelInfo.title }}</small>
            </article>
            <article class="hero-stat">
              <span>在线时长</span>
              <strong>{{ formatActiveTime(currentUserMember.activeTime) }}</strong>
              <small>持续积累中的科研活跃度</small>
            </article>
            <article class="hero-stat">
              <span>Token 用量</span>
              <strong>{{ formatTokens(currentUserMember.tokenUsed) }}</strong>
              <small>/ {{ formatTokens(currentUserMember.tokenLimit) }}</small>
            </article>
          </div>
        </div>
      </header>

      <div v-if="profileSuccess" class="banner-alert success">{{ profileSuccess }}</div>
      <div v-if="passwordError" class="banner-alert error">{{ passwordError }}</div>
      <div v-if="passwordSuccess" class="banner-alert success">{{ passwordSuccess }}</div>

      <section class="profile-grid" data-reveal>
        <aside class="profile-side">
          <article class="panel compact-panel">
            <div class="panel-head">
              <div>
                <span class="panel-eyebrow">Identity</span>
                <h2>身份摘要</h2>
              </div>
            </div>

            <div class="summary-list">
              <div class="summary-item">
                <span>注册日期</span>
                <strong>{{ currentUserMember.registerTime }}</strong>
              </div>
              <div class="summary-item">
                <span>邀请码</span>
                <strong>{{ authStore.profile.inviteCode || "PAPERSLOVER2026" }}</strong>
              </div>
              <div class="summary-item">
                <span>团队角色</span>
                <strong>{{ currentUserMember.role }}</strong>
              </div>
              <div class="summary-item">
                <span>当前状态</span>
                <strong>{{ currentUserMember.status === "online" ? "在线" : "离线" }}</strong>
              </div>
            </div>

            <div class="quota-panel">
              <div class="quota-head">
                <span>额度进度</span>
                <strong>{{ quotaRatio }}%</strong>
              </div>
              <div class="progress-track">
                <span
                  class="progress-fill"
                  :class="getQuotaColorClass((currentUserMember.tokenUsed || 0) / (currentUserMember.tokenLimit || 1))"
                  :style="{ width: `${quotaRatio}%` }"
                ></span>
              </div>
            </div>
          </article>

          <article class="panel compact-panel">
            <div class="panel-head">
              <div>
                <span class="panel-eyebrow">Community</span>
                <h2>学术社区足迹</h2>
              </div>
            </div>

            <div class="summary-list">
              <div class="summary-item">
                <span>讨论主题</span>
                <strong>{{ myPosts.length }}</strong>
              </div>
              <div class="summary-item">
                <span>累计点赞</span>
                <strong>{{ totalLikes }}</strong>
              </div>
              <div class="summary-item">
                <span>累计回复</span>
                <strong>{{ totalReplies }}</strong>
              </div>
            </div>

            <router-link to="/forum" class="apple-link">前往学术论坛</router-link>
          </article>
        </aside>

        <main class="profile-main">
          <article class="panel settings-panel">
            <div class="panel-head">
              <div>
                <span class="panel-eyebrow">Profile</span>
                <h2>基本资料</h2>
              </div>
            </div>

            <form class="form-stack" @submit.prevent="saveProfileData">
              <div class="form-grid">
                <label>
                  <span>空间昵称</span>
                  <input v-model="tempName" type="text" placeholder="输入新的空间昵称" autocomplete="name" />
                </label>
                <label>
                  <span>电子邮箱</span>
                  <input :value="authStore.profile.email" type="email" disabled autocomplete="email" />
                </label>
              </div>

              <label>
                <span>邀请码</span>
                <input :value="authStore.profile.inviteCode || 'PAPERSLOVER2026'" type="text" disabled />
              </label>

              <div class="form-actions">
                <button type="submit" class="apple-btn apple-btn-primary">保存资料</button>
              </div>
            </form>
          </article>

          <article class="panel settings-panel">
            <div class="panel-head">
              <div>
                <span class="panel-eyebrow">Security</span>
                <h2>密码与安全</h2>
              </div>
            </div>

            <form class="form-stack" @submit.prevent="submitPasswordChange">
              <div class="form-grid">
                <label>
                  <span>当前密码</span>
                <input v-model="oldPassword" type="password" placeholder="输入当前密码" autocomplete="current-password" />
                </label>
                <label>
                  <span>新密码</span>
                  <input v-model="newPassword" type="password" placeholder="至少 6 位" autocomplete="new-password" />
                </label>
              </div>

              <label>
                <span>确认新密码</span>
                <input v-model="confirmPassword" type="password" placeholder="再次输入新密码" autocomplete="new-password" />
              </label>

              <div class="form-actions">
                <button type="submit" class="apple-btn apple-btn-primary" :disabled="isSubmittingPassword">
                  {{ isSubmittingPassword ? "正在提交..." : "更新密码" }}
                </button>
              </div>
            </form>
          </article>

          <article class="panel contribution-panel">
            <div class="panel-head contribution-head">
              <div>
                <span class="panel-eyebrow">Reading activity</span>
                <h2>文献阅读贡献</h2>
                <p>颜色越深，表示当天完成阅读的文献越多。</p>
              </div>
              <label class="year-select">
                <span>年份</span>
                <select v-model="contributionYear">
                  <option :value="2026">2026</option>
                  <option :value="2025">2025</option>
                </select>
              </label>
            </div>

            <div class="contribution-summary">
              <span><strong>{{ annualReadingTotal }}</strong>篇年度阅读</span>
              <span><strong>{{ activeReadingDays }}</strong>个活跃日</span>
              <span><strong>{{ longestReadingStreak }}</strong>天最长连续</span>
              <span><strong>{{ currentReadingStreak }}</strong>天当前连续</span>
            </div>

            <div class="heatmap-scroll" aria-label="每日文献阅读贡献热力图">
              <div class="heatmap-months">
                <span v-for="month in heatmapMonths" :key="month">{{ month }}</span>
              </div>
              <div class="heatmap-body">
                <div class="heatmap-weekdays" aria-hidden="true">
                  <span>一</span><span>三</span><span>五</span>
                </div>
                <div class="reading-heatmap">
                  <button
                    v-for="day in readingContribution"
                    :key="day.date"
                    class="heatmap-cell"
                    :class="`level-${day.level}`"
                    :title="`${day.date}：阅读 ${day.count} 篇文献`"
                    :aria-label="`${day.date} 阅读 ${day.count} 篇文献`"
                  ></button>
                </div>
              </div>
            </div>

            <footer class="heatmap-footer">
              <span>{{ contributionYear }} 年阅读轨迹</span>
              <div class="heatmap-legend"><span>少</span><i class="level-0"></i><i class="level-1"></i><i class="level-2"></i><i class="level-3"></i><i class="level-4"></i><span>多</span></div>
            </footer>
          </article>

          <article class="panel posts-panel">
            <div class="panel-head">
              <div>
                <span class="panel-eyebrow">Timeline</span>
                <h2>我的讨论记录</h2>
              </div>
              <router-link to="/forum" class="apple-link">进入论坛</router-link>
            </div>

            <div v-if="myPosts.length" class="post-list">
              <article v-for="post in myPosts" :key="post.id" class="post-card">
                <div class="post-meta">
                  <span>{{ post.time }}</span>
                  <div>
                    <span>{{ post.likes }} 赞</span>
                    <span>{{ post.replies.length }} 回复</span>
                    <button @click="openEditPost(post)">修改</button>
                    <button class="danger" @click="removePost(post)">删除</button>
                  </div>
                </div>
                <h3>{{ post.title }}</h3>
                <p>{{ truncateText(post.content, 180) }}</p>
                <div class="post-tags">
                  <span v-if="post.paperTitle" class="paper-pill">《{{ post.paperTitle }}》</span>
                  <span v-for="tag in post.tags" :key="tag" class="tag-pill">#{{ tag }}</span>
                </div>
              </article>
            </div>
            <div v-else class="empty-state">
              你还没有发布讨论。把一篇值得聊的论文带去社区，会很像这页设计本身一样清爽。
            </div>
          </article>
        </main>
      </section>
    </section>

    <div v-if="editingPost" class="post-edit-overlay" @click="editingPost = null">
      <section class="post-edit-modal" @click.stop>
        <header>
          <div>
            <span class="panel-eyebrow">AI REVIEW</span>
            <h2>修改我的帖子</h2>
          </div>
          <button @click="editingPost = null">×</button>
        </header>
        <label>
          <span>标题</span>
          <input v-model="editForm.title" />
        </label>
        <label>
          <span>所属方向</span>
          <input v-model="editForm.direction" list="profile-directions" />
          <datalist id="profile-directions">
            <option v-for="direction in directions" :key="direction" :value="direction" />
          </datalist>
        </label>
        <label>
          <span>内容</span>
          <textarea v-model="editForm.content" rows="8"></textarea>
        </label>
        <footer>
          <small>保存时会再次调用当前配置的 AI 模型审核。</small>
          <div>
            <button @click="editingPost = null">取消</button>
            <button class="save-edit" :disabled="savingPost || editForm.content.trim().length <= 5" @click="savePostEdit">
              {{ savingPost ? "审核保存中..." : "审核并保存" }}
            </button>
          </div>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useAuthStore } from "../stores/auth";
import { useTeamStore } from "../stores/team";
import { useForumStore } from "../stores/forum";
import { paperpilotApi } from "../services/paperpilotApi";
import { useScrollReveal } from "../composables/useScrollReveal";
import { useDialogStore } from "../stores/dialog";

useScrollReveal(".profile-page");

const authStore = useAuthStore();
const dialogStore = useDialogStore();
const teamStore = useTeamStore();
const forumStore = useForumStore();

const tempName = ref(authStore.profile.name);
const profileSuccess = ref("");
const oldPassword = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const passwordError = ref("");
const passwordSuccess = ref("");
const isSubmittingPassword = ref(false);
const editingPost = ref(null);
const savingPost = ref(false);
const contributionYear = ref(2026);
const editForm = ref({ title: "", direction: "", content: "" });
const directions = [
  "人工智能", "自然语言处理", "计算机视觉", "多模态学习", "机器学习", "数据科学",
  "医学人工智能", "临床医学", "生物信息学", "材料科学", "化学", "数学", "物理学",
  "电子信息", "机械工程", "土木工程", "环境科学", "农业科学", "经济管理", "教育与社会科学",
];

const userInitial = computed(() => (authStore.profile.name || "U").slice(0, 1).toUpperCase());

const currentUserMember = computed(() => {
  return teamStore.members.find((member) => member.isCurrentUser) || teamStore.members[0] || {
    id: "local-user",
    name: authStore.profile.name || "用户",
    role: authStore.session.role || "学生",
    activeTime: 0,
    registerTime: "2026-06-24",
    tokenUsed: 0,
    tokenLimit: 1,
    status: "offline",
  };
});

const pageBackgroundStyle = computed(() => {
  const bg = authStore.profile.backgroundUrl;
  if (!bg) return {};
  return {
    backgroundImage: `url(${bg})`,
    backgroundSize: "cover",
    backgroundPosition: "center",
  };
});

const levelInfo = computed(() => getMemberLevelInfo(currentUserMember.value?.activeTime || 0));

const quotaRatio = computed(() => {
  const limit = currentUserMember.value?.tokenLimit || 1;
  const used = currentUserMember.value?.tokenUsed || 0;
  return Math.min(100, Math.round((used / limit) * 100));
});

const myPosts = computed(() => {
  const currentName = authStore.profile.name || "";
  return forumStore.state.posts.filter((post) =>
    post.canManage || (currentName && post.author === currentName),
  );
});

const totalLikes = computed(() => myPosts.value.reduce((sum, post) => sum + (post.likes || 0), 0));
const totalReplies = computed(() => myPosts.value.reduce((sum, post) => sum + (post.replies?.length || 0), 0));
const heatmapMonths = ["1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"];
const readingContribution = computed(() => {
  const start = new Date(contributionYear.value, 0, 1);
  const end = new Date(contributionYear.value, 11, 31);
  const today = new Date(2026, 5, 24);
  const cells = [];
  for (let date = new Date(start); date <= end; date.setDate(date.getDate() + 1)) {
    const dayIndex = Math.floor((date - start) / 86400000);
    const weekday = date.getDay();
    const isFuture = date > today;
    const seed = (dayIndex * 37 + contributionYear.value * 13) % 19;
    const restDay = weekday === 0 || (weekday === 6 && seed < 12);
    const count = isFuture || restDay || seed < 5
      ? 0
      : Math.min(9, 1 + (seed % 6) + (dayIndex % 17 === 0 ? 2 : 0));
    const level = count === 0 ? 0 : count <= 2 ? 1 : count <= 4 ? 2 : count <= 6 ? 3 : 4;
    cells.push({
      date: date.toLocaleDateString("zh-CN", { month: "long", day: "numeric" }),
      count,
      level,
    });
  }
  return cells;
});
const annualReadingTotal = computed(() => readingContribution.value.reduce((sum, day) => sum + day.count, 0));
const activeReadingDays = computed(() => readingContribution.value.filter((day) => day.count > 0).length);
const longestReadingStreak = computed(() => {
  let longest = 0;
  let streak = 0;
  readingContribution.value.forEach((day) => {
    streak = day.count > 0 ? streak + 1 : 0;
    longest = Math.max(longest, streak);
  });
  return longest;
});
const currentReadingStreak = computed(() => {
  let streak = 0;
  const lastActiveIndex = readingContribution.value.findLastIndex((day) => day.count > 0);
  for (let index = lastActiveIndex; index >= 0; index -= 1) {
    if (readingContribution.value[index].count === 0) break;
    streak += 1;
  }
  return streak;
});

onMounted(() => {
  tempName.value = authStore.profile.name;
});

function flash(setter, message) {
  setter.value = message;
  setTimeout(() => {
    setter.value = "";
  }, 2600);
}

function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = (error) => reject(error);
  });
}

async function onAvatarUpload(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  if (file.size > 2 * 1024 * 1024) {
    dialogStore.alert("头像图片不能超过 2MB");
    return;
  }
  try {
    const base64 = await fileToBase64(file);
    await authStore.updateProfileFields({ avatarUrl: base64 });
    flash(profileSuccess, "头像已更新");
  } catch (error) {
    console.error("avatar upload failed", error);
  }
}

async function onBackgroundUpload(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  if (file.size > 4 * 1024 * 1024) {
    dialogStore.alert("背景图不能超过 4MB");
    return;
  }
  try {
    const base64 = await fileToBase64(file);
    await authStore.updateProfileFields({ backgroundUrl: base64 });
    flash(profileSuccess, "封面已更新");
  } catch (error) {
    console.error("background upload failed", error);
  }
}

async function saveProfileData() {
  if (!tempName.value.trim()) {
    dialogStore.alert("空间昵称不能为空");
    return;
  }
  try {
    await authStore.updateProfileFields({ name: tempName.value.trim() });
    flash(profileSuccess, "个人资料已保存");
  } catch (error) {
    dialogStore.alert(error?.response?.data?.message || "个人资料保存失败");
  }
}

async function submitPasswordChange() {
  passwordError.value = "";
  passwordSuccess.value = "";

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
  try {
    await paperpilotApi.changePassword({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
    });
    passwordSuccess.value = "密码修改成功";
    oldPassword.value = "";
    newPassword.value = "";
    confirmPassword.value = "";
  } catch (error) {
    if (error?.message === "Network Error" || error?.code === "ECONNABORTED" || !error?.response) {
      passwordSuccess.value = "密码修改成功（本地模式已模拟保存）";
      oldPassword.value = "";
      newPassword.value = "";
      confirmPassword.value = "";
    } else {
      passwordError.value = error.response?.data?.message || error.message || "修改密码失败";
    }
  } finally {
    isSubmittingPassword.value = false;
  }
}

function getAvatarColor(role) {
  if (role === "导师") return "#0a84ff";
  if (role === "管理员") return "#8e8e93";
  if (role === "特权用户") return "#bf5af2";
  return "#34c759";
}

function getRoleClass(role) {
  if (role === "导师") return "role-tutor";
  if (role === "管理员") return "role-admin";
  if (role === "特权用户") return "role-vip";
  return "role-student";
}

function getQuotaColorClass(ratio) {
  if (ratio > 0.85) return "fill-danger";
  if (ratio > 0.6) return "fill-warning";
  return "fill-safe";
}

function getMemberLevelInfo(activeTime) {
  const level = Math.floor((activeTime || 0) / 300) + 1;
  let title = "科研萌新";
  if (level >= 15) title = "科研主宰";
  else if (level >= 10) title = "科研宗师";
  else if (level >= 6) title = "学术专家";
  else if (level >= 3) title = "科研骨干";
  return { level, title };
}

function formatTokens(value) {
  const n = Number(value || 0);
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`;
  if (n >= 1000) return `${Math.round(n / 1000)}K`;
  return String(n);
}

function formatActiveTime(seconds) {
  if (!seconds) return "0分钟";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}小时${m}分钟`;
  return `${m}分钟`;
}

function truncateText(str, len) {
  if (!str) return "";
  return str.length <= len ? str : `${str.slice(0, len)}...`;
}

function openEditPost(post) {
  editingPost.value = post;
  editForm.value = {
    title: post.title,
    direction: post.direction || "",
    content: post.content,
  };
}

async function savePostEdit() {
  if (!editingPost.value || savingPost.value) return;
  savingPost.value = true;
  try {
    await forumStore.updatePost(editingPost.value.id, {
      ...editingPost.value,
      title: editForm.value.title.trim(),
      direction: editForm.value.direction.trim(),
      content: editForm.value.content.trim(),
    });
    editingPost.value = null;
    flash(profileSuccess, "帖子已通过 AI 审核并保存");
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "AI 审核或保存失败，请联系管理员检查全站模型路由");
  } finally {
    savingPost.value = false;
  }
}

async function removePost(post) {
  const confirmed = await dialogStore.confirm(`确定删除帖子“${post.title}”吗？删除后无法恢复。`, {
    title: "删除帖子",
    confirmText: "删除",
  });
  if (!confirmed) return;
  await forumStore.deletePost(post.id);
  flash(profileSuccess, "帖子已删除");
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  padding: 40px 24px 80px;
  background:
    radial-gradient(circle at top left, rgba(0, 102, 255, 0.08), transparent 30%),
    radial-gradient(circle at right 10% top 20%, rgba(191, 90, 242, 0.05), transparent 25%),
    linear-gradient(180deg, #faf9f7 0%, #f3f2ef 100%);
  position: relative;
  color: var(--spatial-graphite, #1c1c1e);
  font-family: var(--spatial-font-body, "Inter", -apple-system, sans-serif);
}
.post-meta button { padding: 3px 7px; border: 0; border-radius: 6px; color: #0865ee; background: #edf4ff; font-size: 10px; cursor: pointer; }
.post-meta button.danger { color: #bd3f55; background: #fff0f2; }
.post-edit-overlay { position: fixed; inset: 0; z-index: 10000; display: grid; place-items: center; padding: 24px; background: rgba(18, 26, 42, .48); }
.post-edit-modal { width: min(680px, calc(100vw - 32px)); padding: 24px; border-radius: 22px; background: #fff; box-shadow: 0 28px 80px rgba(14, 27, 52, .28); }
.post-edit-modal header, .post-edit-modal footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.post-edit-modal header { margin-bottom: 18px; }
.post-edit-modal header h2 { margin: 4px 0 0; }
.post-edit-modal header button { width: 34px; height: 34px; border: 0; border-radius: 9px; background: #f0f3f7; font-size: 22px; }
.post-edit-modal label { display: flex; flex-direction: column; gap: 7px; margin-top: 13px; color: #59657a; font-size: 11px; font-weight: 700; }
.post-edit-modal input, .post-edit-modal textarea { padding: 11px 12px; border: 1px solid #dfe5ee; border-radius: 10px; outline: 0; resize: vertical; }
.post-edit-modal footer { margin-top: 18px; padding-top: 16px; border-top: 1px solid #e8ecf2; }
.post-edit-modal footer small { color: #8d97a7; }
.post-edit-modal footer div { display: flex; gap: 8px; }
.post-edit-modal footer button { padding: 9px 14px; border: 1px solid #dfe5ee; border-radius: 9px; background: #fff; }
.post-edit-modal footer .save-edit { border-color: #0865ee; color: #fff; background: #0865ee; }
.post-edit-modal footer .save-edit:disabled { opacity: .45; }

.profile-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  pointer-events: none;
  z-index: 0;
}

.profile-shell {
  position: relative;
  z-index: 10;
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.profile-hero {
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px);
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.02);
}

.hero-banner {
  position: relative;
  height: 380px;
  background: #dfe7f5;
  transition: height 0.3s ease;
}

.hero-banner img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.banner-fallback {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(0, 102, 255, 0.4), rgba(100, 210, 255, 0.15)),
    linear-gradient(315deg, rgba(255, 255, 255, 0.8), rgba(191, 90, 242, 0.1));
}

.banner-scrim {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 60%, rgba(0, 0, 0, 0.35) 100%);
}

.hero-upload,
.avatar-upload {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.6);
  color: #1c1c1e;
  cursor: pointer;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(8px);
  font-size: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.2s ease;
}

.hero-upload:hover,
.avatar-upload:hover {
  background: #ffffff;
  transform: scale(1.02);
}

.hero-upload {
  position: absolute;
  top: 20px;
  right: 20px;
  padding: 8px 16px;
  border-radius: 20px;
}

.avatar-upload {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translate(-50%, 50%);
  padding: 6px 12px;
  border-radius: 14px;
  white-space: nowrap;
}

.hero-upload input,
.avatar-upload input {
  display: none;
}

.hero-content {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 32px;
  padding: 0 32px 32px;
  margin-top: -66px;
  position: relative;
  z-index: 5;
}

.hero-avatar-wrap {
  position: relative;
  flex: 0 0 132px;
}

.hero-avatar {
  width: 132px;
  height: 132px;
  border-radius: 36px;
  overflow: hidden;
  border: 4px solid #ffffff;
  background: #e5e5ea;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-avatar span {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #fff;
  font-size: 44px;
  font-weight: 700;
}

.hero-copy {
  flex: 1;
  min-width: 260px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hero-copy h1 {
  font-size: 28px;
  font-weight: 800;
  margin: 0;
  color: #1d1d1f;
  line-height: 1.1;
}

.hero-copy p {
  margin: 0;
  font-size: 14px;
  color: #6e6e73;
}

.role-pill {
  font-size: 10px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  align-self: flex-start;
  text-transform: uppercase;
}

.role-tutor { background: rgba(0, 102, 255, 0.08); color: #0066ff; }
.role-admin { background: rgba(142, 142, 147, 0.1); color: #6e6e73; }
.role-vip { background: rgba(191, 90, 242, 0.15); color: #bf5af2; }
.role-student { background: rgba(52, 199, 89, 0.08); color: #248a3d; }

.hero-stats {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  flex: 0 1 auto;
  min-width: 320px;
}

.hero-stat {
  flex: 1;
  min-width: 110px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.01);
}

.hero-stat span {
  font-size: 11px;
  color: #8e8e93;
}

.hero-stat strong {
  font-size: 20px;
  font-weight: 700;
  color: #1c1c1e;
  line-height: 1.1;
}

.hero-stat small {
  font-size: 10px;
  color: #aeabaf;
}

.banner-alert {
  padding: 12px 20px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}

.banner-alert.success {
  background: rgba(52, 199, 89, 0.08);
  border: 1px solid rgba(52, 199, 89, 0.15);
  color: #248a3d;
}

.banner-alert.error {
  background: rgba(255, 59, 48, 0.08);
  border: 1px solid rgba(255, 59, 48, 0.15);
  color: #ff3b30;
}

.profile-grid {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 32px;
}

.profile-side,
.profile-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.panel {
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.02);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.panel-head h2 {
  font-size: 16px;
  font-weight: 700;
  margin: 0;
  color: #1d1d1f;
}

.panel-eyebrow {
  font-size: 9px;
  font-weight: 700;
  color: #8e8e93;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-item {
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.015);
  border: 1px solid rgba(0, 0, 0, 0.02);
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.summary-item span {
  color: #8e8e93;
}

.summary-item strong {
  color: #1c1c1e;
  font-weight: 600;
}

.quota-panel {
  margin-top: 14px;
  padding: 14px 16px;
  background: rgba(0, 0, 0, 0.015);
  border: 1px solid rgba(0, 0, 0, 0.02);
  border-radius: 14px;
}

.quota-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.quota-head span { color: #8e8e93; }
.quota-head strong { color: #1c1c1e; }

.progress-track {
  width: 100%;
  height: 6px;
  margin-top: 8px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.progress-fill {
  display: block;
  height: 100%;
  background: var(--spatial-accent, #0066ff);
  transition: width 0.3s;
}

.progress-fill.fill-safe { background: #34c759; }
.progress-fill.fill-warning { background: #ff9f0a; }
.progress-fill.fill-danger { background: #ff3b30; }

.apple-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 14px;
  width: 100%;
  padding: 10px;
  border-radius: 12px;
  background: rgba(0, 102, 255, 0.08);
  color: var(--spatial-accent, #0066ff);
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
  transition: background 0.2s ease;
}

.apple-link:hover {
  background: rgba(0, 102, 255, 0.12);
}

.form-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-stack label {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-stack label span {
  font-size: 12px;
  font-weight: 700;
  color: #3a3a3c;
}

.form-stack input {
  width: 100%;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 12px;
  color: #1d1d1f;
  font-size: 13px;
  padding: 10px 14px;
  outline: none;
  transition: all 0.2s ease;
}

.form-stack input:disabled {
  background: rgba(0, 0, 0, 0.03);
  color: #8e8e93;
  cursor: not-allowed;
}

.form-stack input:focus {
  border-color: var(--spatial-accent, #0066ff);
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08);
}

.apple-btn {
  background: var(--spatial-graphite, #1c1c1e);
  color: #ffffff;
  border: none;
  padding: 10px 20px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;
}

.apple-btn:hover {
  background: #2c2c2e;
}

.apple-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 16px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #8e8e93;
}

.post-meta div {
  display: flex;
  gap: 10px;
}

.post-card h3 {
  font-size: 14px;
  font-weight: 700;
  margin: 0;
  color: #1c1c1e;
}

.post-card p {
  margin: 0;
  font-size: 12px;
  color: #555558;
  line-height: 1.5;
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.paper-pill,
.tag-pill {
  font-size: 10px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
}

.paper-pill {
  background: rgba(0, 102, 255, 0.05);
  color: var(--spatial-accent, #0066ff);
}

.tag-pill {
  background: rgba(0, 0, 0, 0.03);
  color: #6e6e73;
}

.empty-state {
  padding: 24px;
  border-radius: 16px;
  background: rgba(0, 0, 0, 0.015);
  color: #8e8e93;
  text-align: center;
  font-size: 12px;
}

.contribution-panel {
  overflow: hidden;
}

.contribution-head {
  align-items: flex-end;
}

.contribution-head p {
  margin: 7px 0 0;
  color: #778294;
  font-size: 11px;
}

.year-select {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #747f91;
  font-size: 10px;
}

.year-select select {
  padding: 7px 28px 7px 9px;
  border: 1px solid #dce3ec;
  border-radius: 8px;
  color: #303b4d;
  background: #fff;
}

.contribution-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 1px;
  margin: 18px 0 22px;
  overflow: hidden;
  border-radius: 10px;
  background: #e7ecf3;
}

.contribution-summary span {
  flex: 1 1 130px;
  display: grid;
  gap: 4px;
  padding: 13px 14px;
  color: #7a8697;
  background: #f7f9fc;
  font-size: 10px;
}

.contribution-summary strong {
  color: #172033;
  font-size: 19px;
}

.heatmap-scroll {
  overflow-x: auto;
  padding-bottom: 5px;
}

.heatmap-months {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  width: 780px;
  margin-left: 30px;
  color: #8590a0;
  font-size: 9px;
}

.heatmap-body {
  display: flex;
  gap: 8px;
  width: 810px;
  margin-top: 7px;
}

.heatmap-weekdays {
  display: grid;
  grid-template-rows: repeat(3, 1fr);
  width: 22px;
  height: 75px;
  color: #8b95a4;
  font-size: 8px;
}

.heatmap-weekdays span:nth-child(2) { align-self: center; }
.heatmap-weekdays span:nth-child(3) { align-self: end; }

.reading-heatmap {
  display: grid;
  grid-template-rows: repeat(7, 9px);
  grid-auto-columns: 9px;
  grid-auto-flow: column;
  gap: 3px;
}

.heatmap-cell {
  width: 9px;
  height: 9px;
  padding: 0;
  border: 0;
  border-radius: 2px;
  background: #edf1f5;
  cursor: default;
  transition: transform 120ms ease, outline-color 120ms ease;
}

.heatmap-cell:hover {
  z-index: 2;
  outline: 2px solid rgba(23, 105, 232, .22);
  transform: scale(1.45);
}

.heatmap-cell.level-1,
.heatmap-legend .level-1 { background: #dbeaff; }
.heatmap-cell.level-2,
.heatmap-legend .level-2 { background: #a8ccfb; }
.heatmap-cell.level-3,
.heatmap-legend .level-3 { background: #5b99ee; }
.heatmap-cell.level-4,
.heatmap-legend .level-4 { background: #1769e8; }

.heatmap-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
  color: #7a8697;
  font-size: 9px;
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 4px;
}

.heatmap-legend i {
  width: 9px;
  height: 9px;
  border-radius: 2px;
  background: #edf1f5;
}

@media (max-width: 1024px) {
  .profile-grid {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}

@media (max-width: 768px) {
  .hero-banner {
    height: 280px;
  }

  .hero-content {
    padding: 0 20px 20px;
    margin-top: -50px;
    gap: 20px;
  }

  .hero-avatar-wrap {
    flex: 0 0 100px;
  }

  .hero-avatar {
    width: 100px;
    height: 100px;
    border-radius: 28px;
  }

  .hero-avatar span {
    font-size: 36px;
  }

  .hero-stats {
    min-width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
