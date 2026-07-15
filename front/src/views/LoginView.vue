<template>
  <div class="login-route-root">
    <div class="flow-landing spatial-page">
      <header class="flow-landing-nav">
      <router-link class="flow-brand" to="/">
        <img class="flow-brand-mark" src="/brand/papersolver-mark-v2.png" alt="" />
        <strong>PaperSolver</strong>
      </router-link>
      <nav class="flow-nav-links">
        <a href="#reading-path">阅读路径</a>
        <a href="#real-surfaces">功能入口</a>
        <a href="#chapter-pricing">定价</a>
        <a href="#" @click.prevent="openModal('login')">进入</a>
      </nav>
      <button class="flow-download-btn" type="button" @click="openModal('register')">免费注册</button>
    </header>

    <section class="flow-hero">
      <div class="flow-hero-copy">
        <div class="flow-pill-row" data-reveal>
          <span>读论文</span>
          <span>写综述</span>
          <span>讲组会</span>
        </div>
        <h1 data-reveal data-reveal-delay="1">
          把论文读薄，<br />
          把汇报讲清。
        </h1>
        <p data-reveal data-reveal-delay="2">
          PDF 不是终点。PaperSolver 把阅读、标注、翻译、综述和组会材料放在同一条路上，让每一次打开论文都有下文。
        </p>
        <div class="flow-keywords" data-reveal data-reveal-delay="3">
          <b>文献精读</b>
          <b>论文综述</b>
          <b>组会汇报</b>
          <b>校园圈</b>
          <b>模型路由</b>
        </div>
        <div class="flow-hero-actions" data-reveal data-reveal-delay="4">
          <button type="button" class="flow-primary-btn" @click="openModal('login')">进入工作台</button>
          <button type="button" class="flow-secondary-btn" @click="openModal('register')">创建账号</button>
        </div>
      </div>

      <div class="flow-product-stage" data-reveal="right">
        <div class="flow-step-grid" aria-label="首页功能切换">
          <button
            v-for="item in landingPanels"
            :key="item.id"
            type="button"
            :class="{ active: activeLandingPanel === item.id }"
            @click="activeLandingPanel = item.id"
          >
            <span>{{ item.index }}</span>{{ item.tab }}
          </button>
        </div>
        <article class="product-window product-module-panel" :key="currentLandingPanel.id">
          <header class="product-module-head">
            <span>{{ currentLandingPanel.index }}</span>
            <strong>{{ currentLandingPanel.title }}</strong>
            <button type="button" @click="openModal('login')">进入</button>
          </header>
          <div class="product-module-body">
            <p>{{ currentLandingPanel.description }}</p>
            <div class="module-proof-list">
              <button
                v-for="point in currentLandingPanel.points"
                :key="point.label"
                type="button"
                @click="activeLandingPanel = currentLandingPanel.id"
              >
                <small>{{ point.label }}</small>
                <span>{{ point.text }}</span>
              </button>
            </div>
          </div>
          <footer class="product-module-foot">
            <span>{{ currentLandingPanel.footer }}</span>
            <a :href="currentLandingPanel.href" @click.prevent="openModal('login')">{{ currentLandingPanel.link }}</a>
          </footer>
        </article>
      </div>
    </section>

    <section id="reading-path" class="flow-section flow-section-split">
      <div class="flow-section-copy" data-reveal="left">
        <span class="flow-section-index">从一页 PDF 开始</span>
        <h2>先读懂，再产出。</h2>
        <p>
          一篇论文可以变成标注、翻译、问答、综述，也可以继续长成组会讲稿。页面不替你假装努力，只把下一步放在你伸手够得到的位置。
        </p>
        <div class="flow-section-tags">
          <span>选中文字就能批注</span>
          <span>综述逐段留痕</span>
          <span>组会可合并多篇</span>
        </div>
      </div>
      <div class="flow-timeline" data-reveal="right">
        <article><b>收进来</b><span>上传 PDF、Zotero 导入、学术搜索结果入库。</span></article>
        <article><b>读下去</b><span>逐段翻译、全文翻译、片段标注、论文内容详解。</span></article>
        <article><b>写出来</b><span>论文综述保留分点结构，数字和英文重点突出。</span></article>
        <article><b>讲清楚</b><span>单篇或多篇文献生成组会综述和 PPT。</span></article>
      </div>
    </section>

    <section id="real-surfaces" class="flow-showcase-section">
      <div data-reveal>
        <span class="flow-section-index">真正会用到的入口</span>
        <h2>少一点摆设，多一点顺手。</h2>
      </div>
      <div class="flow-showcase-grid">
        <article v-for="(surface, index) in homeSurfaces" :key="surface.title" data-reveal :data-reveal-delay="index + 1">
          <span>{{ surface.kicker }}</span>
          <strong>{{ surface.title }}</strong>
          <p>{{ surface.text }}</p>
        </article>
      </div>
    </section>

    <section id="chapter-pricing" class="flow-pricing-section">
      <div class="spatial-chapter-inner">
        <span class="spatial-chapter-eyebrow" data-reveal>订阅定价</span>
        <h2 class="spatial-chapter-title" data-reveal style="margin-bottom:48px;max-width:20ch">按月开通会员，按功能次数使用</h2>
        
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
            <p class="plan-quota">{{ plan.tier }}会员权益</p>
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
        <h3 class="modal-title">QQ 邮箱注册</h3>
        <div class="form-grid auth-popover-form" style="max-height: 70vh; overflow-y: auto; padding-right: 8px;">
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">QQ 邮箱</label>
            <div class="register-code-row">
              <input v-model="email" type="email" placeholder="123456@qq.com" />
              <button
                type="button"
                class="spatial-btn spatial-btn-ghost register-code-btn"
                :disabled="sendingRegisterCode || registerCodeCooldown > 0"
                @click="sendRegisterCode"
              >
                {{ registerCodeButtonText }}
              </button>
            </div>
          </div>
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">6 位验证码</label>
            <input v-model="verificationCode" inputmode="numeric" maxlength="6" placeholder="输入邮箱验证码" />
          </div>
          <div class="form-group">
            <label style="color: #1e293b; font-weight: 600;">昵称</label>
            <input v-model="name" placeholder="你的昵称" />
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
            <label style="color: #1e293b; font-weight: 600;">邀请码 <span style="color:#94a3b8;font-weight:500;">可选</span></label>
            <input v-model="inviteCode" placeholder="有邀请码可填写，没有可留空" />
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
          <div v-if="registerSuccessText" style="color: #10b981; font-size: 0.85rem; font-weight: 500; text-align: center; margin-top: 8px;">{{ registerSuccessText }}</div>
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
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from "vue";
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
const inviteCode = ref("");
const name = ref("");
const role = ref("学生");
const mentorInviteCode = ref("");
const verificationCode = ref("");
const errorText = ref("");
const loading = ref(false);
const sendingRegisterCode = ref(false);
const registerCodeCooldown = ref(0);
const registerSuccessText = ref("");
let registerCodeTimer = null;

// Forgot Password States
const forgotEmail = ref("");
const forgotCode = ref("");
const forgotNewPassword = ref("");
const sendingCode = ref(false);
const forgotSuccessText = ref("");
const activeLandingPanel = ref("library");

const landingPanels = [
  {
    id: "library",
    index: "01",
    tab: "文献入库",
    title: "先把论文收稳",
    description: "文献库负责把 PDF、Zotero、搜索结果和手动补全的元数据放到同一个地方。阅读进度、期刊标签、导入源头都能继续追踪。",
    href: "/library",
    link: "打开文献库",
    footer: "适合从一堆 PDF 里先理出秩序。",
    points: [
      { label: "导入", text: "本地 PDF、Zotero、URL 与学术搜索" },
      { label: "整理", text: "文献类型、期刊标签、来源和笔记" },
      { label: "衔接", text: "直接进入双栏翻译或逐段阅读" },
    ],
  },
  {
    id: "reader",
    index: "02",
    tab: "精读解析",
    title: "在正文旁边读懂它",
    description: "阅读器把原文、翻译、内容详解和标注放在同一个阅读现场。批注精确到选中的句子，不再把整段都涂成一片。",
    href: "/reading",
    link: "进入阅读",
    footer: "适合需要边读、边问、边做笔记的论文。",
    points: [
      { label: "翻译", text: "全文翻译、逐段翻译、原文 PDF 切换" },
      { label: "标注", text: "选取片段后保存，可单独删除" },
      { label: "解析", text: "研究背景、方法、数据、局限分开看" },
    ],
  },
  {
    id: "review",
    index: "03",
    tab: "论文综述",
    title: "把一篇论文写成能复用的综述",
    description: "综述不是把摘要再说一遍。它会拆成基本信息、研究问题、主要发现、价值与局限，并保留分段和重点标记。",
    href: "/library",
    link: "生成综述",
    footer: "适合课程论文、开题准备和组会前的材料整理。",
    points: [
      { label: "分段", text: "每个要点单独成段，不堆成一坨" },
      { label: "重点", text: "英文、数字和百分号会突出显示" },
      { label: "复用", text: "综述可继续导入组会汇报" },
    ],
  },
  {
    id: "meeting",
    index: "04",
    tab: "组会汇报",
    title: "单篇能讲，多篇也能合并讲",
    description: "组会汇报支持最多三篇文献合并。它不是简单拼接，而是先融合研究问题、汇报目标和关键问题，再生成 PPT。",
    href: "/meeting-report",
    link: "准备组会",
    footer: "适合导师会、课程展示和小组讨论。",
    points: [
      { label: "融合", text: "多篇文献合并成一条汇报主线" },
      { label: "讲稿", text: "生成主讲综述和导师建议修改" },
      { label: "PPT", text: "组会 PPT 单独走更强模型" },
    ],
  },
  {
    id: "forum",
    index: "05",
    tab: "校园论坛",
    title: "让问题流到同校和同行那里",
    description: "论坛承接求助、科研羊毛、论文期刊、研究讨论和校园圈。校园认证后，帖子可以按学校筛选，个人主页显示学校。",
    href: "/forum",
    link: "进入论坛",
    footer: "适合问数据、问投稿、问工具，也适合组内通知。",
    points: [
      { label: "审核", text: "发帖先进入 AI 审核，减少垃圾内容" },
      { label: "校园圈", text: "认证后进入同校帖子筛选" },
      { label: "消息", text: "回复、置顶、封禁、认证反馈进站内通知" },
    ],
  },
  {
    id: "models",
    index: "06",
    tab: "模型与额度",
    title: "贵模型只用在该用的地方",
    description: "不同入口可以单独配置模型：PPT 走更强模型，综述、问答和发帖审核可以用更便宜的模型，成本和体验分开算。",
    href: "/models",
    link: "查看额度",
    footer: "适合控制成本，也适合管理员做模型路由。",
    points: [
      { label: "场景", text: "综述、问答、PPT、审核分别配置" },
      { label: "额度", text: "按任务次数显示剩余，不让用户猜" },
      { label: "会员", text: "个人和团队套餐分别展示" },
    ],
  },
];

const currentLandingPanel = computed(
  () => landingPanels.find((item) => item.id === activeLandingPanel.value) || landingPanels[0],
);

const homeSurfaces = [
  {
    kicker: "文献库",
    title: "先把资料放对地方",
    text: "导入、检索、标签、阅读进度和笔记都在这里开始，后续阅读器和组会不会断线。",
  },
  {
    kicker: "阅读器",
    title: "左边解析，右边正文",
    text: "翻译、标注、论文内容详解围绕正文展开，像认真读书，不像后台拼面板。",
  },
  {
    kicker: "组会",
    title: "把多篇论文讲成一条线",
    text: "单篇综述可以进入组会，多篇文献也能融合成汇报目标、关键问题和 PPT。",
  },
];

const registerCodeButtonText = computed(() => {
  if (sendingRegisterCode.value) return "发送中";
  if (registerCodeCooldown.value > 0) return `${registerCodeCooldown.value}s`;
  return "获取验证码";
});

function isQqEmail(value) {
  return /^[1-9][0-9]{4,11}@qq\.com$/i.test(String(value || "").trim());
}

function startRegisterCodeCooldown() {
  registerCodeCooldown.value = 60;
  if (registerCodeTimer) clearInterval(registerCodeTimer);
  registerCodeTimer = setInterval(() => {
    registerCodeCooldown.value -= 1;
    if (registerCodeCooldown.value <= 0) {
      clearInterval(registerCodeTimer);
      registerCodeTimer = null;
    }
  }, 1000);
}

async function sendRegisterCode() {
  const normalizedEmail = email.value.trim().toLowerCase();
  if (!isQqEmail(normalizedEmail)) {
    errorText.value = "请填写 QQ 邮箱，例如 123456@qq.com";
    return;
  }
  sendingRegisterCode.value = true;
  errorText.value = "";
  registerSuccessText.value = "";
  try {
    await paperpilotApi.sendRegisterCode(normalizedEmail);
    email.value = normalizedEmail;
    registerSuccessText.value = "验证码已发送到 QQ 邮箱，10 分钟内有效。";
    startRegisterCodeCooldown();
  } catch (error) {
    errorText.value = error.response?.data?.message || error.message;
  } finally {
    sendingRegisterCode.value = false;
  }
}

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
  registerSuccessText.value = "";
  forgotSuccessText.value = "";
  showPassword.value = false;
  // Seed default credentials for easier user testing
  if (mode === "login") {
    email.value = "student@paperslover.app";
    password.value = "Student2026!";
  } else {
    email.value = "";
    password.value = "";
    verificationCode.value = "";
    inviteCode.value = "";
    name.value = "";
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
  if (!isQqEmail(email.value)) {
    errorText.value = "注册必须使用 QQ 邮箱，例如 123456@qq.com";
    return;
  }
  if (!verificationCode.value || verificationCode.value.length !== 6) {
    errorText.value = "请输入 6 位邮箱验证码";
    return;
  }
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
      verificationCode: verificationCode.value,
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

.flow-landing {
  min-height: 100vh;
  overflow-x: hidden;
  color: #111827;
  background:
    radial-gradient(circle at 78% 14%, rgba(248, 181, 88, .18), transparent 28%),
    radial-gradient(circle at 20% 18%, rgba(74, 144, 226, .14), transparent 26%),
    radial-gradient(circle at 62% 54%, rgba(124, 58, 237, .08), transparent 30%),
    linear-gradient(180deg, #f7faff 0%, #f8fafc 44%, #f3f6fb 100%);
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

.flow-landing::before {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: .18;
  background-image:
    linear-gradient(rgba(37, 99, 235, .06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, .06) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: linear-gradient(180deg, #000 0%, transparent 76%);
  content: "";
}

.flow-landing > * {
  position: relative;
  z-index: 1;
}

.flow-landing-nav {
  width: min(1160px, calc(100vw - 64px));
  height: 66px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.flow-brand,
.flow-nav-links,
.flow-landing-auth {
  display: flex;
  align-items: center;
}

.flow-brand {
  gap: 12px;
  color: #101827;
  text-decoration: none;
  font-size: 18px;
  font-weight: 850;
}

.flow-brand-mark {
  width: 36px;
  height: 36px;
  object-fit: contain;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(38, 59, 96, .1);
}

.flow-nav-links {
  gap: 30px;
  margin-left: auto;
}

.flow-nav-links a {
  color: rgba(17, 24, 39, .72);
  font-size: 14px;
  font-weight: 750;
  text-decoration: none;
}

.flow-nav-links a:hover {
  color: #2563eb;
}

.flow-download-btn,
.flow-primary-btn,
.flow-secondary-btn {
  border: 0;
  cursor: pointer;
  font-weight: 850;
  transition: transform 180ms ease, box-shadow 180ms ease, background 180ms ease;
}

.flow-download-btn {
  height: 38px;
  padding: 0 22px;
  border-radius: 999px;
  color: #fff;
  background: #111827;
  box-shadow: 0 14px 28px rgba(17, 24, 39, .16);
}

.flow-download-btn:hover,
.flow-primary-btn:hover,
.flow-secondary-btn:hover {
  transform: translateY(-1px);
}

.flow-hero {
  width: min(1160px, calc(100vw - 64px));
  min-height: calc(100vh - 66px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(420px, .92fr) minmax(520px, 1.08fr);
  align-items: center;
  gap: 56px;
  padding: 34px 0 72px;
}

.flow-hero-copy {
  max-width: 610px;
}

.flow-pill-row,
.flow-keywords,
.flow-hero-actions,
.flow-section-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.flow-pill-row span,
.flow-keywords b,
.flow-section-tags span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 16px;
  border: 1px solid rgba(16, 185, 129, .2);
  border-radius: 999px;
  color: #1d4ed8;
  background: rgba(255, 255, 255, .78);
  box-shadow: 0 8px 20px rgba(37, 99, 235, .05);
  font-size: 13px;
  font-weight: 850;
}

.flow-pill-row span::before,
.flow-keywords b::before,
.flow-section-tags span::before {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #f59e0b;
  content: "";
}

.flow-hero h1 {
  margin: 24px 0 20px;
  color: #101827;
  font-size: clamp(48px, 6vw, 76px);
  line-height: .98;
  letter-spacing: 0;
  font-weight: 900;
  text-wrap: balance;
}

.flow-hero h1::first-line {
  color: #2563eb;
}

.flow-hero p {
  max-width: 56ch;
  margin: 0;
  color: rgba(17, 24, 39, .66);
  font-size: 18px;
  line-height: 1.9;
  font-weight: 550;
}

.flow-keywords {
  margin-top: 22px;
}

.flow-keywords b {
  min-height: 30px;
  padding: 0 12px;
  border-color: transparent;
  background: rgba(37, 99, 235, .08);
  font-size: 14px;
}

.flow-hero-actions {
  margin-top: 32px;
}

.flow-primary-btn,
.flow-secondary-btn {
  height: 58px;
  padding: 0 28px;
  border-radius: 12px;
  font-size: 16px;
}

.flow-primary-btn {
  color: #fff;
  background: linear-gradient(135deg, #111827, #2563eb);
  box-shadow: 0 24px 44px rgba(37, 99, 235, .22);
}

.flow-secondary-btn {
  border: 1px solid rgba(37, 99, 235, .16);
  color: #111827;
  background: rgba(255, 255, 255, .78);
}

.flow-product-stage {
  display: grid;
  gap: 18px;
}

.flow-step-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.flow-step-grid button {
  min-height: 58px;
  display: grid;
  align-content: center;
  gap: 3px;
  padding: 10px 12px;
  border: 1px solid rgba(30, 41, 59, .1);
  border-radius: 9px;
  color: rgba(17, 24, 39, .72);
  background: rgba(255, 255, 255, .78);
  text-align: left;
  font-size: 13px;
  font-weight: 850;
  box-shadow: 0 10px 24px rgba(30, 41, 59, .06);
}

.flow-step-grid button span {
  color: rgba(17, 24, 39, .44);
  font-size: 11px;
}

.flow-step-grid button.active {
  color: #fff;
  background: #2563eb;
  border-color: #2563eb;
  transform: translateY(-2px);
}

.flow-step-grid button.active span {
  color: rgba(255, 255, 255, .72);
}

.product-window {
  overflow: hidden;
  border: 1px solid rgba(30, 41, 59, .1);
  border-radius: 16px;
  background: rgba(255, 255, 255, .78);
  box-shadow: 0 34px 78px rgba(30, 41, 59, .13);
  animation: product-float 5.5s ease-in-out infinite;
}

.product-module-panel {
  animation: product-float 5.5s ease-in-out infinite, module-swap 320ms cubic-bezier(.22, 1, .36, 1);
}

.product-module-head {
  height: auto !important;
  min-height: 74px;
  align-items: center;
  padding: 18px 22px !important;
}

.product-module-head span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  margin: 0;
  border-radius: 999px;
  background: #eef4ff;
  color: #2563eb !important;
  font-size: 13px !important;
  font-weight: 950;
}

.product-module-head strong {
  margin-left: 4px !important;
  color: #111827 !important;
  font-size: 19px !important;
}

.product-module-head button {
  margin-left: auto;
  height: 38px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: #111827;
  font-weight: 900;
  cursor: pointer;
}

.product-module-body {
  padding: 26px;
  background:
    radial-gradient(circle at 92% 8%, rgba(245, 158, 11, .14), transparent 30%),
    linear-gradient(135deg, rgba(255, 255, 255, .96), rgba(239, 246, 255, .72));
}

.product-module-body p {
  max-width: 56ch;
  margin: 0 0 22px;
  color: rgba(17, 24, 39, .72);
  font-size: 16px;
  line-height: 1.85;
  font-weight: 650;
}

.module-proof-list {
  display: grid;
  gap: 12px;
}

.module-proof-list button {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  min-height: 58px;
  padding: 14px 16px;
  border: 1px solid rgba(37, 99, 235, .1);
  border-radius: 12px;
  background: rgba(255, 255, 255, .72);
  color: #111827;
  text-align: left;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}

.module-proof-list button:hover {
  transform: translateX(4px);
  border-color: rgba(37, 99, 235, .28);
  background: #fff;
}

.module-proof-list small {
  color: #2563eb;
  font-size: 12px;
  font-weight: 950;
}

.module-proof-list span {
  color: rgba(17, 24, 39, .76);
  font-weight: 800;
}

.product-module-foot {
  min-height: 64px !important;
}

.product-module-foot span {
  color: rgba(17, 24, 39, .58);
  font-weight: 700;
}

.product-window header {
  height: 50px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(30, 41, 59, .1);
}

.product-window header i {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #ff6159;
}

.product-window header i:nth-child(2) { background: #ffc43d; }
.product-window header i:nth-child(3) { background: #29c06f; }

.product-window header strong {
  margin-left: 8px;
  color: #2563eb;
  font-size: 13px;
}

.product-window header span {
  margin-left: auto;
  color: rgba(17, 24, 39, .48);
  font-size: 12px;
}

.product-window-body {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  min-height: 265px;
  background:
    radial-gradient(circle at 80% 20%, rgba(245, 158, 11, .12), transparent 28%),
    linear-gradient(135deg, rgba(239, 246, 255, .78), rgba(255, 255, 255, .92));
}

.product-window-body aside {
  padding: 24px;
  border-right: 1px solid rgba(30, 41, 59, .1);
}

.product-window-body aside b,
.flow-showcase-grid span {
  color: #2563eb;
  font-size: 12px;
  font-weight: 900;
}

.product-window-body aside strong {
  display: block;
  margin: 12px 0 8px;
  color: #101827;
  font-size: 20px;
  line-height: 1.35;
}

.product-window-body aside span,
.product-window footer,
.flow-showcase-grid p {
  color: rgba(17, 24, 39, .56);
  font-size: 13px;
  line-height: 1.7;
}

.product-window-body main {
  display: grid;
  align-content: center;
  gap: 14px;
  padding: 24px;
}

.paper-row {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, .74);
}

.paper-row span {
  color: #2563eb;
  font-size: 11px;
  font-weight: 900;
}

.paper-row b {
  min-width: 0;
  overflow: hidden;
  color: #111827;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.paper-row.active {
  box-shadow: inset 3px 0 0 #2563eb;
}

.stage-progress {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(37, 99, 235, .12);
}

.stage-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2563eb, #f59e0b);
  animation: progress-breathe 2.8s ease-in-out infinite;
}

.product-window footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 58px;
  padding: 0 18px;
  border-top: 1px solid rgba(30, 41, 59, .1);
}

.product-window footer a {
  color: #2563eb;
  font-weight: 900;
  text-decoration: none;
}

.flow-section,
.flow-showcase-section,
.flow-pricing-section {
  width: min(1160px, calc(100vw - 64px));
  margin: 0 auto;
  padding: 92px 0;
}

.flow-section-split {
  display: grid;
  grid-template-columns: minmax(320px, .86fr) minmax(420px, 1fr);
  gap: 72px;
  align-items: center;
  border-top: 1px solid rgba(30, 41, 59, .1);
}

.flow-section-index {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: #2563eb;
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid rgba(37, 99, 235, .16);
  border-radius: 999px;
  background: rgba(255, 255, 255, .72);
  font-size: 14px;
  line-height: 1;
  font-weight: 900;
}

.flow-section-copy h2,
.flow-showcase-section h2 {
  max-width: 760px;
  margin: 18px 0;
  color: #101827;
  font-size: clamp(34px, 4vw, 54px);
  line-height: 1.08;
  letter-spacing: 0;
}

.flow-section-copy p {
  max-width: 60ch;
  margin: 0;
  color: rgba(17, 24, 39, .64);
  font-size: 17px;
  line-height: 1.9;
}

.flow-section-tags {
  margin-top: 24px;
}

.flow-timeline {
  display: grid;
  gap: 16px;
}

.flow-timeline article,
.flow-showcase-grid article {
  padding: 24px;
  border: 1px solid rgba(30, 41, 59, .1);
  border-radius: 14px;
  background: rgba(255, 255, 255, .74);
  box-shadow: 0 18px 44px rgba(30, 41, 59, .06);
}

.flow-timeline article {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  align-items: center;
}

.flow-timeline b {
  color: #1d4ed8;
  font-size: 18px;
}

.flow-timeline span {
  color: rgba(17, 24, 39, .64);
}

.flow-showcase-section {
  border-top: 1px solid rgba(30, 41, 59, .1);
}

.flow-showcase-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 34px;
}

.flow-showcase-grid strong {
  display: block;
  margin: 10px 0 8px;
  color: #101827;
  font-size: 21px;
}

.flow-pricing-section {
  border-top: 1px solid rgba(30, 41, 59, .1);
}

@keyframes product-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

@keyframes module-swap {
  from {
    opacity: .55;
    transform: translateY(10px) scale(.985);
    filter: blur(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}

@keyframes progress-breathe {
  0%, 100% { filter: saturate(1); }
  50% { filter: saturate(1.35) brightness(1.08); }
}

@media (max-width: 980px) {
  .flow-landing-nav,
  .flow-hero,
  .flow-section,
  .flow-showcase-section,
  .flow-pricing-section {
    width: min(100% - 28px, 760px);
  }

  .flow-nav-links {
    display: none;
  }

  .flow-hero,
  .flow-section-split {
    grid-template-columns: 1fr;
    gap: 36px;
  }

  .flow-hero {
    padding-top: 28px;
  }

  .flow-step-grid,
  .flow-showcase-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .flow-hero h1 {
    font-size: 42px;
  }

  .flow-step-grid,
  .product-window-body,
  .flow-showcase-grid {
    grid-template-columns: 1fr;
  }

  .product-window-body aside {
    border-right: 0;
    border-bottom: 1px solid rgba(30, 41, 59, .1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .product-window,
  .stage-progress i {
    animation: none;
  }
}

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

.register-code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 118px;
  gap: 10px;
  align-items: center;
}

.register-code-btn {
  min-height: 44px;
  padding: 0 14px;
  white-space: nowrap;
  font-size: 0.86rem;
  border-radius: 12px;
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
