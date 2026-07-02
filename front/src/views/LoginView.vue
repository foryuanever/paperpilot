<template>
  <div class="spatial-landing spatial-page">
    <!-- Ambient glowing backdrops -->
    <div class="spatial-orb spatial-orb-blue" style="width:550px;height:550px;top:-10%;right:-8%"></div>
    <div class="spatial-orb spatial-orb-warm" style="width:380px;height:380px;bottom:25%;left:-6%"></div>
    <div class="spatial-orb spatial-orb-blue" style="width:400px;height:400px;top:40%;right:15%;opacity:0.35"></div>

    <header class="spatial-landing-nav">
      <router-link class="spatial-landing-brand" to="/">
        <img class="spatial-landing-brand-logo" src="/brand/papersolver-mark-v2.png" alt="" />
        PaperSolver
      </router-link>
      <nav class="spatial-landing-links">
        <a href="#chapter-flow">流程</a>
        <a href="#chapter-space">空间</a>
        <a href="#chapter-pricing">定价</a>
        <a href="#" @click.prevent="openModal('login')">进入</a>
      </nav>
      <div class="spatial-landing-auth">
        <span class="spatial-nav-link active" @click="openModal('login')" style="cursor:pointer">登录</span>
        <button class="spatial-btn spatial-btn-accent" @click="openModal('register')">注册</button>
      </div>
    </header>

    <section class="spatial-hero">
      <span class="spatial-hero-eyebrow" data-reveal>未来研究界面</span>
      <h1 class="spatial-hero-title spatial-hero-title-offset" data-reveal data-reveal-delay="1">
        论文<br />不是模块<br />是空间
      </h1>
      <p class="spatial-hero-lead" data-reveal data-reveal-delay="2">
        进入 PaperSlover，像走进一座数字文献馆——搜索、阅读、翻译与综述在一条流动的研究叙事中展开，而非堆叠在后台面板里。
      </p>
      <div class="spatial-hero-actions" data-reveal data-reveal-delay="3">
        <button class="spatial-btn spatial-btn-primary" @click="openModal('login')">进入系统</button>
        <button class="spatial-btn spatial-btn-ghost" @click="openModal('register')">邀请注册</button>
      </div>
    </section>

    <!-- Chapter 1: Research Flow (Interactive Visual) -->
    <section id="chapter-flow" class="spatial-chapter">
      <div class="spatial-chapter-inner spatial-chapter-split">
        <div class="spatial-chapter-copy" data-reveal="left">
          <span class="spatial-chapter-eyebrow">研究流程</span>
          <h2 class="spatial-chapter-title">从检索到精读，一条向下展开的研究镜头。</h2>
          <p class="spatial-chapter-lead">
            学术搜索找到 PDF，文献库沉淀资产，阅读器左原文右译文同步滚动——每一步都是连续场景，而不是跳转页面。
          </p>
        </div>
        <div class="spatial-chapter-visual" data-reveal="right">
          <!-- Flow board visual replaces the old blank monument -->
          <div class="flow-interactive-board">
            <div class="flow-track-line">
              <div class="flow-pulse-dot"></div>
            </div>
            
            <div class="flow-board-card active">
              <div class="flow-card-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
              </div>
              <div class="flow-card-info">
                <strong>学术搜索</strong>
                <span>一键检索多源 PDF 瞬间导入</span>
              </div>
            </div>

            <div class="flow-board-card">
              <div class="flow-card-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
                </svg>
              </div>
              <div class="flow-card-info">
                <strong>文献沉淀</strong>
                <span>学术期刊标签化档案管理</span>
              </div>
            </div>

            <div class="flow-board-card">
              <div class="flow-card-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
                  <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
                </svg>
              </div>
              <div class="flow-card-info">
                <strong>同步精读</strong>
                <span>左原文右学术译文流动对照</span>
              </div>
            </div>

            <div class="flow-board-card">
              <div class="flow-card-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"></path>
                  <path d="M15.5 12a3.5 3.5 0 1 1-7 0 3.5 3.5 0 0 1 7 0z"></path>
                </svg>
              </div>
              <div class="flow-card-info">
                <strong>综述总结</strong>
                <span>AI 自动提取创新点与大纲</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Chapter 2: Space -->
    <section id="chapter-space" class="spatial-chapter spatial-chapter-dark">
      <div class="spatial-chapter-inner">
        <span class="spatial-chapter-eyebrow" data-reveal>空间设计</span>
        <h2 class="spatial-chapter-title" data-reveal data-reveal-delay="1" style="color:#f5f5f7;max-width:16ch">
          内容优先，界面退后。
        </h2>
        <div class="spatial-drift-band" style="margin-top:56px">
          <article class="spatial-drift-item spatial-glass-panel-dark" data-reveal data-reveal-delay="1">
            <span class="spatial-drift-label">阅读器</span>
            <strong class="spatial-drift-value" style="color:#f5f5f7">双栏同步</strong>
            <p class="spatial-drift-detail" style="color:rgba(245,245,247,0.62)">左 PDF 右译文，段落锚点一一对应。</p>
          </article>
          <article class="spatial-drift-item spatial-glass-panel-dark" data-reveal data-reveal-delay="2">
            <span class="spatial-drift-label">文献库</span>
            <strong class="spatial-drift-value" style="color:#f5f5f7">研究档案</strong>
            <p class="spatial-drift-detail" style="color:rgba(245,245,247,0.62)">阅读进度、标注与期刊标签在同一工作区维护。</p>
          </article>
          <article class="spatial-drift-item spatial-glass-panel-dark" data-reveal data-reveal-delay="3">
            <span class="spatial-drift-label">模型中心</span>
            <strong class="spatial-drift-value" style="color:#f5f5f7">独立网关</strong>
            <p class="spatial-drift-detail" style="color:rgba(245,245,247,0.62)">模型配置不污染阅读空间。</p>
          </article>
        </div>
      </div>
    </section>

    <!-- Chapter 3: Pricing (Imported from center) -->
    <section id="chapter-pricing" class="spatial-chapter">
      <div class="spatial-chapter-inner">
        <span class="spatial-chapter-eyebrow" data-reveal>订阅定价</span>
        <h2 class="spatial-chapter-title" data-reveal style="margin-bottom:48px;max-width:20ch">透明额度方案，限制终结于自由创作</h2>
        
        <div class="home-pricing-grid" data-reveal data-reveal-delay="1">
          <article
            v-for="plan in billingPlans"
            :key="plan.id"
            class="home-pricing-card"
            :class="{ featured: plan.highlight }"
          >
            <span class="plan-drift-label">{{ plan.oneTime ? "加量包" : "订阅方案" }}</span>
            <h3 class="plan-name">{{ plan.name }}</h3>
            <div class="plan-amount">
              <strong>{{ plan.price }}</strong>
              <span>{{ plan.period }}</span>
            </div>
            <p class="plan-quota">{{ formatTokens(plan.tokenQuota) }} Token</p>
            <ul class="plan-features">
              <li v-for="feat in plan.features" :key="feat">
                <span class="feat-check">✓</span> {{ feat }}
              </li>
            </ul>
            <button
              class="spatial-btn plan-btn"
              :class="plan.highlight ? 'spatial-btn-accent' : 'spatial-btn-ghost'"
              @click="openModal('register')"
            >
              {{ plan.oneTime ? "购买加量包" : "立即免费注册" }}
            </button>
          </article>
        </div>
      </div>
    </section>
  </div>

  <!-- Glassmorphic Auth Modal -->
  <div v-if="showAuthModal" class="spatial-modal-backdrop" @click="closeModal">
    <div class="spatial-modal-content" @click.stop>
      <button class="spatial-modal-close" @click="closeModal">&times;</button>
      
      <div v-if="authMode === 'login'">
        <span class="spatial-chapter-eyebrow" style="color: var(--spatial-accent, #0066ff)">LOG IN</span>
        <h3 class="modal-title">继续进入</h3>
        <div class="form-grid auth-popover-form">
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">邮箱</label>
            <input v-model="email" type="email" placeholder="输入邮箱 (e.g. student@paperslover.app)" />
          </div>
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">密码</label>
            <div class="password-input-wrapper">
              <input v-model="password" :type="showPassword ? 'text' : 'password'" placeholder="输入密码" />
              <button type="button" class="password-toggle-btn" @click="showPassword = !showPassword">
                <span v-html="showPassword ? eyeOffIcon : eyeIcon"></span>
              </button>
            </div>
          </div>
          <div v-if="errorText" class="auth-error">{{ errorText }}</div>
          <button class="spatial-btn spatial-btn-accent auth-submit" :disabled="loading" @click="submitLogin">
            {{ loading ? "登录中..." : "进入 PaperSlover" }}
          </button>
          <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 16px; width: 100%;">
            <a href="#" class="auth-link" style="margin: 0;" @click.prevent="authMode = 'register'">没有账号？去注册</a>
            <a href="#" class="auth-link" style="margin: 0; color: #64748b;" @click.prevent="authMode = 'forgot_password'">忘记密码？</a>
          </div>
        </div>
      </div>

      <div v-else-if="authMode === 'register'">
        <span class="spatial-chapter-eyebrow" style="color: var(--spatial-accent, #0066ff)">REGISTER</span>
        <h3 class="modal-title">内测邀请注册</h3>
        <div class="form-grid auth-popover-form" style="max-height: 70vh; overflow-y: auto; padding-right: 8px;">
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">邀请码</label>
            <input v-model="inviteCode" placeholder="PAPERSLOVER2026" />
          </div>
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">昵称</label>
            <input v-model="name" placeholder="你的昵称" />
          </div>
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">邮箱</label>
            <input v-model="email" type="email" placeholder="you@example.com" />
          </div>
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">密码</label>
            <div class="password-input-wrapper">
              <input v-model="password" :type="showPassword ? 'text' : 'password'" placeholder="设置密码" />
              <button type="button" class="password-toggle-btn" @click="showPassword = !showPassword">
                <span v-html="showPassword ? eyeOffIcon : eyeIcon"></span>
              </button>
            </div>
          </div>
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">身份角色</label>
            <div class="role-selector-capsule">
              <button
                type="button"
                class="role-btn"
                :class="{ active: role === '学生' }"
                @click="role = '学生'"
              >
                学生
              </button>
              <button
                type="button"
                class="role-btn"
                :class="{ active: role === '导师' }"
                @click="role = '导师'"
              >
                导师
              </button>
              <button
                type="button"
                class="role-btn"
                :class="{ active: role === '管理员' }"
                @click="role = '管理员'"
              >
                管理员
              </button>
            </div>
          </div>
          <div v-if="role === '导师'" class="form-group transition-input">
            <label style="color: #1e293b; font-weight: 600;">导师专属邀请码</label>
            <input v-model="mentorInviteCode" type="password" placeholder="请输入导师特权邀请码 (TUTOR2026)" />
          </div>
          <div v-if="role === '管理员'" class="form-group transition-input">
            <label style="color: #1e293b; font-weight: 600;">管理员专属邀请码</label>
            <input v-model="mentorInviteCode" type="password" placeholder="请输入管理员特权邀请码 (ADMIN2026)" />
          </div>
          <div v-if="errorText" class="auth-error">{{ errorText }}</div>
          <button class="spatial-btn spatial-btn-accent auth-submit" :disabled="loading" @click="submitRegister">
            {{ loading ? "注册中..." : "创建账号并进入" }}
          </button>
          <a href="#" class="auth-link" @click.prevent="authMode = 'login'">已有账号？直接登录</a>
        </div>
      </div>

      <div v-else-if="authMode === 'forgot_password'">
        <span class="spatial-chapter-eyebrow" style="color: var(--spatial-accent, #0066ff)">RESET PASSWORD</span>
        <h3 class="modal-title">找回密码</h3>
        <div class="form-grid auth-popover-form">
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">邮箱</label>
            <div style="display: flex; gap: 8px; align-items: flex-end;">
              <input v-model="forgotEmail" type="email" placeholder="you@paperslover.app" style="flex: 1;" />
              <button type="button" class="spatial-btn spatial-btn-accent compact-btn" style="min-height: 44px; padding: 0 12px; font-size: 0.85rem;" :disabled="sendingCode" @click="sendForgotCode">
                {{ sendingCode ? '发送中...' : '获取验证码' }}
              </button>
            </div>
          </div>
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">6位验证码</label>
            <input v-model="forgotCode" placeholder="输入验证码 (查看控制台日志)" />
          </div>
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">新密码</label>
            <div class="password-input-wrapper">
              <input v-model="forgotNewPassword" :type="showPassword ? 'text' : 'password'" placeholder="设置新密码（至少 6 位）" />
              <button type="button" class="password-toggle-btn" @click="showPassword = !showPassword">
                <span v-html="showPassword ? eyeOffIcon : eyeIcon"></span>
              </button>
            </div>
          </div>
          <div v-if="forgotSuccessText" style="color: #10b981; font-size: 0.85rem; font-weight: 500; text-align: center; margin-top: 8px;">{{ forgotSuccessText }}</div>
          <div v-if="errorText" class="auth-error">{{ errorText }}</div>
          <button class="spatial-btn spatial-btn-accent auth-submit" :disabled="loading" @click="submitResetPassword">
            {{ loading ? "重置中..." : "确认重置密码" }}
          </button>
          <a href="#" class="auth-link" @click.prevent="authMode = 'login'">返回登录</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useScrollReveal } from "../composables/useScrollReveal";
import { useAuthStore } from "../stores/auth";
import { billingPlans } from "../constants/pages";
import { paperpilotApi } from "../services/paperpilotApi";

useScrollReveal(".spatial-page");

const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();

// Auth States
const showAuthModal = ref(false);
const authMode = ref("login"); // 'login' or 'register'
const email = ref("");
const password = ref("");
const inviteCode = ref("PAPERSLOVER2026");
const name = ref("");
const role = ref("学生");
const mentorInviteCode = ref("");
const errorText = ref("");
const loading = ref(false);

// Forgot Password States
const forgotEmail = ref("");
const forgotCode = ref("");
const forgotNewPassword = ref("");
const sendingCode = ref(false);
const forgotSuccessText = ref("");

async function sendForgotCode() {
  if (!forgotEmail.value) {
    errorText.value = "请输入邮箱";
    return;
  }
  sendingCode.value = true;
  errorText.value = "";
  forgotSuccessText.value = "";
  try {
    await paperpilotApi.sendForgotPasswordCode(forgotEmail.value);
    forgotSuccessText.value = "验证码已成功生成，请在管理员后台日志或终端中查看！";
  } catch (error) {
    errorText.value = error.response?.data?.message || error.message;
  } finally {
    sendingCode.value = false;
  }
}

async function submitResetPassword() {
  if (!forgotEmail.value || !forgotCode.value || !forgotNewPassword.value) {
    errorText.value = "请填写所有必填字段";
    return;
  }
  loading.value = true;
  errorText.value = "";
  forgotSuccessText.value = "";
  try {
    await paperpilotApi.resetPasswordWithCode({
      email: forgotEmail.value,
      code: forgotCode.value,
      newPassword: forgotNewPassword.value,
    });
    forgotSuccessText.value = "密码重置成功！即将返回登录页面...";
    setTimeout(() => {
      authMode.value = "login";
      forgotSuccessText.value = "";
      // Pre-fill the new password for login convenience
      email.value = forgotEmail.value;
      password.value = forgotNewPassword.value;
    }, 2000);
  } catch (error) {
    errorText.value = error.response?.data?.message || error.message;
  } finally {
    loading.value = false;
  }
}

const showPassword = ref(false);
const eyeIcon = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 18px; height: 18px;"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`;
const eyeOffIcon = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 18px; height: 18px;"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>`;

function formatTokens(n) {
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`;
  if (n >= 1000) return `${Math.round(n / 1000)}K`;
  return String(n);
}

function openModal(mode) {
  authMode.value = mode;
  errorText.value = "";
  showPassword.value = false;
  // Seed default credentials for easier user testing
  if (mode === "login") {
    email.value = "student@paperslover.app";
    password.value = "Student2026!";
  } else {
    email.value = "";
    password.value = "";
  }
  showAuthModal.value = true;
}

function closeModal() {
  showAuthModal.value = false;
  showPassword.value = false;
  errorText.value = "";
}

async function submitLogin() {
  loading.value = true;
  errorText.value = "";
  try {
    await authStore.login({ email: email.value, password: password.value });
    if (authStore.session.role === "管理员") {
      router.push("/admin");
    } else {
      router.push("/library");
    }
  } catch (error) {
    errorText.value = error.response?.data?.message || error.message;
  } finally {
    loading.value = false;
  }
}

async function submitRegister() {
  loading.value = true;
  errorText.value = "";
  try {
    await authStore.register({
      inviteCode: inviteCode.value,
      name: name.value,
      email: email.value,
      password: password.value,
      role: role.value,
      mentorInviteCode: mentorInviteCode.value,
    });
    if (authStore.session.role === "管理员") {
      router.push("/admin");
    } else {
      router.push("/library");
    }
  } catch (error) {
    errorText.value = error.response?.data?.message || error.message;
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  if (route.query.auth === "register" || route.query.show === "register") {
    openModal("register");
  } else if (route.query.auth === "login" || route.query.show === "login") {
    openModal("login");
  }
});
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Inter:wght@300;400;500;600;700&display=swap');

.spatial-landing {
  font-family: 'Inter', -apple-system, sans-serif;
  overflow-x: hidden;
}

/* Metallic gradient title text */
.spatial-hero-title {
  font-family: 'Outfit', 'Georgia', serif !important;
  background: linear-gradient(135deg, #0f172a 20%, #0066ff 65%, #60a5fa);
  -webkit-background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
  background-clip: text !important;
  font-weight: 600 !important;
  letter-spacing: -0.05em !important;
}

.spatial-chapter-title {
  font-family: 'Outfit', 'Georgia', serif !important;
  background: linear-gradient(135deg, #1e293b 30%, #0284c7 80%);
  -webkit-background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
  background-clip: text !important;
  font-weight: 500 !important;
  letter-spacing: -0.04em !important;
}

/* Visual timeline board */
.flow-interactive-board {
  position: relative;
  width: 100%;
  max-width: 460px;
  margin: 0 auto;
  padding: 24px 0 24px 48px;
  background: rgba(255, 255, 255, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  box-shadow: 0 32px 80px rgba(10, 10, 12, 0.03);
  backdrop-filter: blur(16px);
}

.flow-track-line {
  position: absolute;
  top: 48px;
  left: 28px;
  bottom: 48px;
  width: 2px;
  background: rgba(0, 102, 255, 0.08);
}

.flow-pulse-dot {
  position: absolute;
  top: 0;
  left: -4px;
  width: 10px;
  height: 10px;
  background: #0066ff;
  border-radius: 50%;
  box-shadow: 0 0 12px #0066ff, 0 0 0 4px rgba(0, 102, 255, 0.18);
  animation: flow-run 8s linear infinite;
}

@keyframes flow-run {
  0% { top: 0%; opacity: 0; }
  5% { opacity: 1; }
  95% { opacity: 1; }
  100% { top: 100%; opacity: 0; }
}

.flow-board-card {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(10, 10, 12, 0.02);
  transition: transform 0.3s, border-color 0.3s, box-shadow 0.3s;
  position: relative;
  z-index: 2;
}

.flow-board-card::before {
  content: "";
  position: absolute;
  top: 50%;
  left: -26px;
  width: 10px;
  height: 10px;
  background: #fff;
  border: 2px solid rgba(0, 102, 255, 0.25);
  border-radius: 50%;
  transform: translateY(-50%);
  z-index: 3;
  transition: background 0.3s, border-color 0.3s;
}

.flow-board-card:last-child {
  margin-bottom: 0;
}

.flow-board-card:hover {
  transform: translateX(6px) scale(1.02);
  border-color: rgba(0, 102, 255, 0.2);
  box-shadow: 0 12px 32px rgba(0, 102, 255, 0.05);
}

.flow-board-card.active::before {
  background: #0066ff;
  border-color: #0066ff;
  box-shadow: 0 0 8px rgba(0, 102, 255, 0.4);
}

.flow-card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: rgba(0, 102, 255, 0.06);
  border-radius: 10px;
}

.flow-card-icon svg {
  width: 20px;
  height: 20px;
  color: #0066ff;
}

.flow-card-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.flow-card-info strong {
  font-size: 14px;
  color: #1e293b;
  font-weight: 600;
}

.flow-card-info span {
  font-size: 12px;
  color: #64748b;
}

/* Pricing Grid */
.home-pricing-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  width: 100%;
}

.home-pricing-card {
  padding: 32px 28px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 16px 40px rgba(10, 10, 12, 0.03);
  display: flex;
  flex-direction: column;
  transition: transform 0.3s, border-color 0.3s, box-shadow 0.3s;
}

.home-pricing-card.featured {
  border-color: rgba(0, 102, 255, 0.25);
  background: linear-gradient(180deg, rgba(0, 102, 255, 0.02) 0%, rgba(255, 255, 255, 0.85) 100%);
  box-shadow: 0 24px 56px rgba(0, 102, 255, 0.07);
}

.home-pricing-card:hover {
  transform: translateY(-4px);
  border-color: rgba(0, 102, 255, 0.18);
  box-shadow: 0 24px 56px rgba(10, 10, 12, 0.05);
}

.plan-drift-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #0066ff;
  text-transform: uppercase;
  margin-bottom: 8px;
}

.plan-name {
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 12px;
}

.plan-amount {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 8px;
}

.plan-amount strong {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: #0f172a;
}

.plan-amount span {
  font-size: 13px;
  color: #64748b;
}

.plan-quota {
  font-size: 13px;
  font-weight: 600;
  color: #0066ff;
  margin-bottom: 24px;
}

.plan-features {
  list-style: none;
  padding: 0;
  margin: 0 0 32px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.plan-features li {
  font-size: 13px;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 8px;
}

.feat-check {
  color: #10b981;
  font-weight: bold;
}

.plan-btn {
  width: 100%;
}

/* Modal Overlay Styles */
.spatial-modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(10, 10, 12, 0.45);
  backdrop-filter: blur(20px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s ease-out;
}

.spatial-modal-content {
  position: relative;
  width: 100%;
  max-width: 480px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 32px 80px rgba(10, 10, 12, 0.15);
  backdrop-filter: blur(40px);
  animation: modalScaleUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.spatial-modal-close {
  position: absolute;
  top: 20px;
  right: 24px;
  background: none;
  border: none;
  font-size: 2rem;
  color: #64748b;
  cursor: pointer;
  line-height: 1;
  transition: color 0.2s;
}

.spatial-modal-close:hover {
  color: #0066ff;
}

.modal-title {
  margin: 8px 0 24px;
  font-family: var(--spatial-font-display);
  font-size: 1.8rem;
  font-weight: 400;
  letter-spacing: -0.03em;
  color: #0f172a;
}

/* Role Selector Capsule inside Modal */
.role-selector-capsule {
  display: flex;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 12px;
  padding: 4px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  margin-top: 6px;
}

.role-btn {
  flex: 1;
  border: none;
  background: none;
  color: #64748b;
  padding: 10px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.role-btn:hover {
  color: #0066ff;
}

.role-btn.active {
  background: #0066ff;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 102, 255, 0.2);
}

.transition-input {
  animation: slideDown 0.25s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes modalScaleUp {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.password-input-wrapper {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
}

.password-input-wrapper input {
  width: 100%;
  padding-right: 42px !important;
}

.password-toggle-btn {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  transition: color 0.2s;
}

.password-toggle-btn:hover {
  color: #0066ff;
}
</style>
