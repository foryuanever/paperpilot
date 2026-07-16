<template>
  <div class="login-route-root">
    <div class="ps-home spatial-page">
      <header class="ps-nav">
        <router-link class="ps-brand" to="/">
          <img src="/brand/papersolver-mark-v2.png" alt="" />
          <strong>PaperSolver</strong>
        </router-link>
        <nav aria-label="首页导航">
          <a href="#product-flow">流程</a>
          <a href="#advantages">优势</a>
          <a href="#chapter-pricing">定价</a>
          <a href="#" @click.prevent="openModal('login')">登录</a>
        </nav>
        <button type="button" @click="openModal('register')">免费注册</button>
      </header>

      <section class="ps-hero">
        <div class="ps-hero-copy" data-reveal>
          <div class="ps-note-line">
            <span>文献阅读</span>
            <span>论文综述</span>
            <span>组会 PPT</span>
          </div>
          <h1>读薄论文，讲清组会。</h1>
          <p>
            PaperSolver 把导入、阅读、标注、综述、组会汇报和额度管理放进一条连续流程。你从 PDF 开始，不用在多个工具之间来回搬材料。
          </p>
          <div class="ps-actions">
            <button class="ps-primary" type="button" @click="openModal('login')">进入工作台</button>
            <button class="ps-secondary" type="button" @click="openModal('register')">创建账号</button>
          </div>
        </div>

        <div class="ps-console" data-reveal="right">
          <div class="ps-console-tabs">
            <button
              v-for="step in productFlowSteps"
              :key="step.id"
              type="button"
              :class="{ active: activeLandingPanel === step.id }"
              @click="activeLandingPanel = step.id"
            >
              <span>{{ step.index }}</span>
              {{ step.tab }}
            </button>
          </div>

          <article class="ps-console-window" :key="currentProductFlow.id">
            <header>
              <div class="ps-window-dots"><i></i><i></i><i></i></div>
              <strong>{{ currentProductFlow.title }}</strong>
              <span>{{ currentProductFlow.status }}</span>
            </header>
            <div class="ps-console-body">
              <div class="ps-flow-diagram" aria-label="产品流程动画">
                <svg viewBox="0 0 520 220" fill="none" aria-hidden="true">
                  <path d="M54 112 C128 42 196 42 260 112 S392 182 466 112" />
                  <path d="M54 112 C134 168 190 168 260 112 S386 56 466 112" />
                </svg>
                <span
                  v-for="(node, index) in currentProductFlow.nodes"
                  :key="node"
                  :style="{ '--i': index }"
                >{{ node }}</span>
              </div>
              <div class="ps-live-panel">
                <div class="ps-shot-placeholder">
                  <b>{{ currentProductFlow.previewTitle }}</b>
                  <em>等待接入真实截图</em>
                </div>
                <div class="ps-live-rows">
                  <div v-for="row in currentProductFlow.rows" :key="row.label">
                    <small>{{ row.label }}</small>
                    <strong>{{ row.text }}</strong>
                  </div>
                </div>
                <div class="ps-meter"><i :style="{ width: currentProductFlow.progress }"></i></div>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section id="product-flow" class="ps-flow-section">
        <div class="ps-section-head" data-reveal>
          <span>产品流程</span>
          <h2>从 PDF 到汇报，不拆成孤岛。</h2>
          <p>流程线不是装饰。每一步都对应项目里真实存在的页面和操作。</p>
        </div>
        <div class="ps-flow-chain" data-reveal data-reveal-delay="1">
          <article v-for="step in productFlowSteps" :key="`chain-${step.id}`">
            <span>{{ step.index }}</span>
            <h3>{{ step.tab }}</h3>
            <p>{{ step.summary }}</p>
          </article>
        </div>
      </section>

      <section id="advantages" class="ps-compare-section">
        <div class="ps-section-head" data-reveal>
          <span>产品优势</span>
          <h2>和常见工具相比，少掉的是搬运成本。</h2>
          <p>不是多放几个 AI 按钮，而是让文献、笔记、综述、组会和论坛之间互通。</p>
        </div>
        <div class="ps-comparison" data-reveal data-reveal-delay="1">
          <div class="ps-comparison-head">
            <strong>能力</strong>
            <strong>普通阅读器 / AI 对话</strong>
            <strong>PaperSolver</strong>
          </div>
          <div v-for="row in comparisonRows" :key="row.feature" class="ps-comparison-row">
            <b>{{ row.feature }}</b>
            <span>{{ row.market }}</span>
            <span>{{ row.paperSolver }}</span>
          </div>
        </div>
      </section>

      <section id="chapter-pricing" class="ps-pricing-section">
        <div class="ps-section-head" data-reveal>
          <span>产品定价</span>
          <h2>基础开放，高成本任务按套餐扣减。</h2>
          <p>导入和阅读保持轻量；论文综述、组会 PPT、AI 对话这些消耗模型的任务按次数展示。</p>
        </div>
        <div class="ps-pricing-grid" data-reveal data-reveal-delay="1">
          <article
            v-for="plan in billingPlans"
            :key="plan.id"
            :class="{ featured: plan.highlight }"
          >
            <span>{{ plan.oneTime ? "加量包" : "订阅方案" }}</span>
            <h3>{{ plan.name }}</h3>
            <div class="ps-price">
              <strong>{{ plan.price }}</strong>
              <small>{{ plan.period }}</small>
            </div>
            <p>{{ plan.tier }} 会员权益</p>
            <ul>
              <li v-for="feat in plan.features.slice(0, 4)" :key="feat">{{ feat }}</li>
            </ul>
            <button type="button" @click="openModal('register')">
              {{ plan.oneTime ? "购买加量包" : "开始使用" }}
            </button>
          </article>
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

const productFlowSteps = [
  {
    id: "library",
    index: "01",
    tab: "文献导入",
    title: "文献先进入同一个地方",
    status: "PDF / Zotero / 元数据",
    previewTitle: "文献库截图槽位",
    progress: "74%",
    summary: "上传 PDF、导入 Zotero、补齐题录和标签，后续阅读和组会都从同一份文献继续。",
    nodes: ["PDF", "Zotero", "元数据", "文献库"],
    rows: [
      { label: "PDF", text: "本地论文入库，保留阅读进度" },
      { label: "Zotero", text: "批量题录导入，减少手动整理" },
      { label: "TAG", text: "期刊、会议、待读状态可筛选" },
    ],
  },
  {
    id: "reader",
    index: "02",
    tab: "阅读解析",
    title: "读的时候把证据留下",
    status: "翻译 / 标注 / 解析",
    previewTitle: "阅读器截图槽位",
    progress: "52%",
    summary: "原文、翻译、内容详解和精确批注并排工作，不把整段误标成一片。",
    nodes: ["原文", "翻译", "选区标注", "解析"],
    rows: [
      { label: "TEXT", text: "选中句子后只标记选区" },
      { label: "AI", text: "背景、方法、数据、局限分开看" },
      { label: "NOTE", text: "批注可单独删除，也可全局清除" },
    ],
  },
  {
    id: "review",
    index: "03",
    tab: "论文综述",
    title: "把一篇论文写成可复用材料",
    status: "分段 / 重点 / 保存",
    previewTitle: "论文综述截图槽位",
    progress: "88%",
    summary: "基本信息、研究问题、主要发现、汇报价值拆开写，英文和数字重点突出。",
    nodes: ["基本信息", "研究问题", "主要发现", "汇报价值"],
    rows: [
      { label: "INFO", text: "作者、年份、期刊、研究对象" },
      { label: "FIND", text: "英文、数字和百分号重点标红" },
      { label: "USE", text: "保存后可导入组会" },
    ],
  },
  {
    id: "meeting",
    index: "04",
    tab: "组会汇报",
    title: "单篇能讲，多篇也能融合",
    status: "最多 3 篇 / PPT",
    previewTitle: "组会汇报截图槽位",
    progress: "66%",
    summary: "最多三篇文献合成一条汇报主线，再生成组会重点、关键问题、导师建议和 PPT。",
    nodes: ["选文献", "融合主线", "讲稿", "PPT"],
    rows: [
      { label: "FOCUS", text: "组会重点内容先融合" },
      { label: "GOAL", text: "汇报目标与关键问题单独生成" },
      { label: "PPT", text: "多篇文献合并生成 1 个 PPT" },
    ],
  },
  {
    id: "forum",
    index: "05",
    tab: "校园圈",
    title: "问题回到同校和同行",
    status: "认证 / 审核 / 通知",
    previewTitle: "校园圈截图槽位",
    progress: "58%",
    summary: "校园认证后按学校筛选帖子，发帖先过 AI 审核，回复和认证结果进入站内通知。",
    nodes: ["校园认证", "发帖审核", "帖子流", "通知"],
    rows: [
      { label: "SCHOOL", text: "认证后进入同校帖子筛选" },
      { label: "POST", text: "发帖进入 AI 审核队列" },
      { label: "MSG", text: "回复、置顶、认证反馈进站内通知" },
    ],
  },
  {
    id: "models",
    index: "06",
    tab: "模型额度",
    title: "贵模型只用在该用的地方",
    status: "路由 / 成本 / 套餐",
    previewTitle: "模型中心截图槽位",
    progress: "79%",
    summary: "PPT 生成使用高阶模型；综述、问答、发帖审核可以走更便宜的模型，成本分开算。",
    nodes: ["综述", "问答", "PPT", "审核"],
    rows: [
      { label: "PPT", text: "组会 PPT 走高阶模型" },
      { label: "LOW", text: "审核和问答可走低成本模型" },
      { label: "PLAN", text: "个人和团队套餐分开展示" },
    ],
  },
];

const comparisonRows = [
  {
    feature: "文献到产出",
    market: "阅读、总结、PPT 往往分散在多个工具里。",
    paperSolver: "导入、阅读、综述、组会汇报沿同一份文献继续。",
  },
  {
    feature: "标注与解析",
    market: "很多工具只给整段摘要，标注和正文脱节。",
    paperSolver: "选区批注、全文翻译、内容详解围绕正文展开。",
  },
  {
    feature: "组会准备",
    market: "需要手动把多篇论文拼成讲稿和 PPT。",
    paperSolver: "最多三篇文献融合成汇报主线，再生成 PPT。",
  },
  {
    feature: "交流与通知",
    market: "讨论在群聊里丢失，学校身份不清楚。",
    paperSolver: "校园认证、校园圈筛选、站内通知和论坛审核打通。",
  },
  {
    feature: "模型成本",
    market: "所有任务常走同一模型，成本不可控。",
    paperSolver: "按入口路由模型，PPT 用强模型，审核和问答可降成本。",
  },
];

const currentProductFlow = computed(
  () => productFlowSteps.find((item) => item.id === activeLandingPanel.value) || productFlowSteps[0],
);

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
    nodes: ["PDF", "Zotero", "元数据", "标签", "阅读器"],
    screenTitle: "文献库",
    screenMeta: "导入与整理",
    status: "PDF / Zotero / 元数据",
    shotTitle: "LibraryView",
    progress: "72%",
    previewRows: [
      { label: "PDF", text: "Semantic frameworks to support implementation...", kind: "active" },
      { label: "Zotero", text: "批量题录导入，保留来源与作者", kind: "soft" },
      { label: "TAG", text: "期刊、会议、待读状态分开筛选", kind: "warm" },
    ],
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
    nodes: ["原文", "翻译", "选区", "批注", "解析"],
    screenTitle: "AI 阅读器",
    screenMeta: "原文旁边读",
    status: "全文翻译已就绪",
    shotTitle: "ReaderView",
    progress: "46%",
    previewRows: [
      { label: "TEXT", text: "选中句子，标注只落在选区", kind: "active" },
      { label: "AI", text: "研究背景、方法、局限独立展开", kind: "soft" },
      { label: "NOTE", text: "单条批注可删除，可清空全局记号", kind: "cool" },
    ],
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
    nodes: ["基本信息", "问题", "发现", "价值", "局限"],
    screenTitle: "论文综述",
    screenMeta: "一篇一篇生成",
    status: "分段综述已保存",
    shotTitle: "ReviewPanel",
    progress: "88%",
    previewRows: [
      { label: "INFO", text: "作者、年份、期刊、研究对象", kind: "active" },
      { label: "FIND", text: "英文、数字和百分号会重点标红", kind: "warm" },
      { label: "USE", text: "保存后可一键导入组会", kind: "soft" },
    ],
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
    nodes: ["最多三篇", "融合", "讲稿", "PPT", "导师建议"],
    screenTitle: "组会汇报",
    screenMeta: "单篇或多篇",
    status: "3 篇以内可融合",
    shotTitle: "MeetingReport",
    progress: "64%",
    previewRows: [
      { label: "FOCUS", text: "组会重点内容先合并成主线", kind: "active" },
      { label: "GOAL", text: "汇报目标与关键问题单独生成", kind: "soft" },
      { label: "PPT", text: "多篇文献合并生成一个 PPT", kind: "cool" },
    ],
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
    nodes: ["发帖", "审核", "校园认证", "回复", "通知"],
    screenTitle: "学术论坛",
    screenMeta: "帖子先过 AI 审核",
    status: "校园圈需认证",
    shotTitle: "ForumView",
    progress: "54%",
    previewRows: [
      { label: "POST", text: "新帖从最新列表上方滑入", kind: "active" },
      { label: "SCHOOL", text: "校园圈按学校筛选，只显示小水印", kind: "warm" },
      { label: "MSG", text: "回复、置顶、认证反馈进站内通知", kind: "soft" },
    ],
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
    nodes: ["综述", "问答", "PPT", "审核", "成本"],
    screenTitle: "模型与额度",
    screenMeta: "按入口路由",
    status: "PPT 使用高阶模型",
    shotTitle: "ModelCenter",
    progress: "76%",
    previewRows: [
      { label: "GPT", text: "PPT 生成保留高阶模型", kind: "active" },
      { label: "LOW", text: "审核、问答可切换便宜模型", kind: "soft" },
      { label: "PLAN", text: "个人 Lite / Plus / Pro / Max 与团队分开", kind: "cool" },
    ],
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

const homeJourney = [
  {
    step: "01",
    title: "文献先进入同一个地方",
    text: "上传 PDF、导入 Zotero、补全题录和标签。后面阅读器、综述和组会用的是同一份文献，不需要反复搬运。",
    panel: "文献库",
    slot: "文献卡片、筛选栏、阅读进度截图槽位",
    tags: ["PDF", "Zotero", "标签"],
    lines: ["上传 PDF", "补全作者与期刊", "进入阅读器"],
  },
  {
    step: "02",
    title: "读的时候就把证据留下",
    text: "选中一句话做批注，逐段翻译，左侧查看研究背景、方法、数据和局限。标注落在选区，不把整段涂乱。",
    panel: "阅读器",
    slot: "双栏阅读、精确标注、内容详解截图槽位",
    tags: ["选区批注", "全文翻译", "论文解析"],
    lines: ["选中文字", "保存批注", "查看解析"],
  },
  {
    step: "03",
    title: "综述不是摘要复制",
    text: "基本信息、研究问题、主要发现、汇报价值分开保存。英文、数字和百分号突出显示，后续能直接进入组会材料。",
    panel: "论文综述",
    slot: "综述分段、重点标记、一键导入组会截图槽位",
    tags: ["基本信息", "主要发现", "汇报价值"],
    lines: ["生成综述", "保存编辑", "导入组会"],
  },
  {
    step: "04",
    title: "单篇能讲，多篇也能融合",
    text: "组会支持最多三篇文献一起准备。先融合组会重点内容、汇报目标和关键问题，再生成主讲综述和 PPT。",
    panel: "组会汇报",
    slot: "多文献选择、融合进度、PPT 生成截图槽位",
    tags: ["最多三篇", "融合主线", "PPT 进度"],
    lines: ["选择文献", "融合汇报", "生成 PPT"],
  },
  {
    step: "05",
    title: "问题可以流回同校和同行",
    text: "校园认证后进入校园圈，同校帖子单独筛选。发帖先进入 AI 审核，回复、置顶和认证反馈进入站内通知。",
    panel: "校园圈",
    slot: "认证入口、帖子流、站内通知截图槽位",
    tags: ["校园认证", "AI 审核", "站内通知"],
    lines: ["认证学校", "发布问题", "收到回复"],
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
  font-family: "Outfit", "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  font-size: clamp(52px, 5.6vw, 78px);
  line-height: .96;
  letter-spacing: 0;
  font-weight: 900;
  text-wrap: balance;
}

.flow-hero h1 .hero-brand-word {
  display: block;
  color: #2563eb;
  font-size: 1.04em;
}

.flow-hero h1 .hero-title-line {
  display: block;
  white-space: nowrap;
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
  transition: transform 220ms cubic-bezier(.22, 1, .36, 1), border-color 180ms ease, background 180ms ease, color 180ms ease;
}

.flow-step-grid button span {
  color: rgba(17, 24, 39, .44);
  font-size: 11px;
}

.flow-step-grid button.active {
  color: #fff;
  background: #2563eb;
  border-color: #2563eb;
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 18px 36px rgba(37, 99, 235, .2);
}

.flow-step-grid button:not(.active):hover {
  transform: translateY(-3px);
  border-color: rgba(37, 99, 235, .24);
  background: #fff;
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

.product-flow-panel {
  --panel-accent: #2563eb;
  --panel-accent-soft: rgba(37, 99, 235, .12);
  --panel-warm: rgba(245, 158, 11, .13);
  min-height: 440px;
  animation: product-float 5.5s ease-in-out infinite, module-swap 420ms cubic-bezier(.16, 1, .3, 1);
}

.product-flow-panel-reader { --panel-accent: #4f46e5; --panel-accent-soft: rgba(79, 70, 229, .13); --panel-warm: rgba(16, 185, 129, .12); }
.product-flow-panel-review { --panel-accent: #0f766e; --panel-accent-soft: rgba(15, 118, 110, .12); --panel-warm: rgba(245, 158, 11, .14); }
.product-flow-panel-meeting { --panel-accent: #7c3aed; --panel-accent-soft: rgba(124, 58, 237, .12); --panel-warm: rgba(14, 165, 233, .12); }
.product-flow-panel-forum { --panel-accent: #db2777; --panel-accent-soft: rgba(219, 39, 119, .11); --panel-warm: rgba(16, 185, 129, .12); }
.product-flow-panel-models { --panel-accent: #111827; --panel-accent-soft: rgba(17, 24, 39, .1); --panel-warm: rgba(37, 99, 235, .1); }

.product-flow-head {
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(30, 41, 59, .08);
}

.product-flow-head div {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.product-flow-head span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: var(--panel-accent-soft);
  color: var(--panel-accent);
  font-size: 12px;
  font-weight: 950;
}

.product-flow-head strong {
  min-width: 0;
  color: #111827;
  font-size: 19px;
  line-height: 1.2;
}

.product-flow-head button {
  flex: 0 0 auto;
  height: 38px;
  padding: 0 16px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: var(--panel-accent);
  font-weight: 900;
  cursor: pointer;
}

.product-flow-body {
  position: relative;
  min-height: 302px;
  display: grid;
  grid-template-columns: minmax(210px, .82fr) minmax(270px, 1fr);
  gap: 20px;
  padding: 22px;
  background:
    radial-gradient(circle at 78% 4%, var(--panel-warm), transparent 30%),
    linear-gradient(135deg, rgba(248, 251, 255, .98), rgba(255, 255, 255, .9));
}

.flow-map {
  position: relative;
  min-height: 238px;
  overflow: hidden;
  border: 1px solid rgba(30, 41, 59, .08);
  border-radius: 14px;
  background: rgba(255, 255, 255, .58);
}

.flow-map-lines {
  position: absolute;
  inset: 18px 12px;
  width: calc(100% - 24px);
  height: calc(100% - 36px);
}

.flow-map-lines path {
  stroke: var(--panel-accent);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-dasharray: 10 13;
  opacity: .34;
  animation: flow-dash 1.7s linear infinite;
}

.flow-node {
  position: absolute;
  left: calc(11% + var(--x) * 28%);
  top: calc(28px + (var(--x) * 44px));
  min-width: 70px;
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border: 1px solid rgba(30, 41, 59, .1);
  border-radius: 999px;
  color: #111827;
  background: #fff;
  box-shadow: 0 10px 22px rgba(30, 41, 59, .08);
  font-size: 12px;
  font-weight: 900;
  animation: flow-node-in 420ms cubic-bezier(.16, 1, .3, 1) both, node-float 4.2s ease-in-out infinite;
  animation-delay: var(--delay), calc(var(--delay) + 600ms);
}

.flow-node:nth-of-type(3) { top: 128px; left: 18%; }
.flow-node:nth-of-type(4) { top: 146px; left: 49%; }
.flow-node:nth-of-type(5) { top: 96px; left: 69%; }

.flow-screen {
  display: grid;
  align-content: center;
  gap: 14px;
  min-width: 0;
}

.flow-screen-top {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.flow-screen-top b {
  color: #111827;
  font-size: 20px;
  line-height: 1.25;
}

.flow-screen-top span {
  color: rgba(17, 24, 39, .52);
  font-size: 12px;
  font-weight: 850;
  white-space: nowrap;
}

.flow-screen-list {
  display: grid;
  gap: 11px;
}

.flow-screen-row {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 54px;
  padding: 12px 14px;
  border: 1px solid rgba(30, 41, 59, .08);
  border-radius: 12px;
  background: rgba(255, 255, 255, .72);
  animation: row-fly-in 360ms cubic-bezier(.16, 1, .3, 1) both;
  animation-delay: calc(var(--row) * 90ms + 80ms);
}

.flow-screen-row small {
  color: var(--panel-accent);
  font-size: 11px;
  font-weight: 950;
}

.flow-screen-row strong {
  min-width: 0;
  overflow: hidden;
  color: rgba(17, 24, 39, .84);
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 850;
}

.flow-screen-row.active {
  border-color: var(--panel-accent);
  box-shadow: inset 3px 0 0 var(--panel-accent);
}

.flow-screen-row.warm {
  background: rgba(255, 251, 235, .74);
}

.flow-screen-row.cool {
  background: rgba(239, 246, 255, .78);
}

.flow-screen-progress {
  margin-top: 4px;
}

.product-flow-foot {
  min-height: 66px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0 20px;
  border-top: 1px solid rgba(30, 41, 59, .08);
}

.product-flow-foot span {
  color: rgba(17, 24, 39, .58);
  font-size: 13px;
  line-height: 1.6;
  font-weight: 750;
}

.product-flow-foot div {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.product-flow-foot b {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  color: var(--panel-accent);
  background: var(--panel-accent-soft);
  font-size: 12px;
}

.product-window:not(.product-flow-panel) header {
  height: 50px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(30, 41, 59, .1);
}

.product-window:not(.product-flow-panel) header i {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #ff6159;
}

.product-window:not(.product-flow-panel) header i:nth-child(2) { background: #ffc43d; }
.product-window:not(.product-flow-panel) header i:nth-child(3) { background: #29c06f; }

.product-window:not(.product-flow-panel) header strong {
  margin-left: 8px;
  color: #2563eb;
  font-size: 13px;
}

.product-window:not(.product-flow-panel) header span {
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
    transform: translateX(24px) translateY(8px) scale(.985);
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

@keyframes flow-dash {
  to { stroke-dashoffset: -46; }
}

@keyframes flow-node-in {
  from {
    opacity: 0;
    transform: translateX(-18px) scale(.92);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

@keyframes node-float {
  0%, 100% { translate: 0 0; }
  50% { translate: 0 -8px; }
}

@keyframes row-fly-in {
  from {
    opacity: 0;
    transform: translateX(-24px);
    filter: blur(5px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
    filter: blur(0);
  }
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

  .product-flow-body {
    grid-template-columns: 1fr;
  }

  .flow-map {
    min-height: 210px;
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

  .product-flow-head,
  .product-flow-foot {
    align-items: flex-start;
    flex-direction: column;
  }

  .product-flow-foot div {
    justify-content: flex-start;
  }

  .flow-screen-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .product-window-body aside {
    border-right: 0;
    border-bottom: 1px solid rgba(30, 41, 59, .1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .product-window,
  .stage-progress i,
  .flow-map-lines path,
  .flow-node,
  .flow-screen-row {
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

/* PaperSolver home redesign: real workflow, soft light, motion-rich. */
.flow-landing {
  min-height: 100vh;
  overflow-x: clip;
  color: #101827;
  background:
    linear-gradient(120deg, rgba(235, 246, 255, .82), rgba(255, 255, 255, .94) 38%, rgba(246, 243, 255, .72) 72%, rgba(236, 253, 245, .64)),
    #f8fbff;
}

.flow-landing::before {
  position: fixed;
  inset: -20% -10% auto;
  height: 58vh;
  pointer-events: none;
  background:
    radial-gradient(circle at 14% 32%, rgba(37, 99, 235, .15), transparent 34%),
    radial-gradient(circle at 74% 28%, rgba(124, 58, 237, .11), transparent 32%),
    radial-gradient(circle at 55% 78%, rgba(20, 184, 166, .1), transparent 30%);
  filter: blur(22px);
  opacity: .9;
  content: "";
  animation: home-ambient 12s ease-in-out infinite alternate;
}

.flow-landing-nav {
  position: sticky;
  top: 14px;
  z-index: 20;
  width: min(1180px, calc(100vw - 56px));
  min-height: 58px;
  margin: 14px auto 0;
  padding: 8px 10px 8px 12px;
  border: 1px solid rgba(148, 163, 184, .22);
  border-radius: 18px;
  background: rgba(255, 255, 255, .74);
  backdrop-filter: blur(18px);
  box-shadow: 0 12px 34px rgba(37, 99, 235, .08);
}

.flow-brand {
  gap: 10px;
}

.flow-brand strong {
  color: #101827;
  font-size: 17px;
  letter-spacing: 0;
}

.flow-brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(37, 99, 235, .14);
}

.flow-nav-links {
  gap: 2px;
  margin-left: auto;
  padding: 4px;
  border-radius: 999px;
  background: rgba(241, 245, 249, .62);
}

.flow-nav-links a {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  color: rgba(15, 23, 42, .62);
  font-size: 13px;
  font-weight: 850;
  transition: color 180ms ease, background 180ms ease, transform 180ms ease;
}

.flow-nav-links a.active,
.flow-nav-links a:hover {
  color: #1d4ed8;
  background: rgba(255, 255, 255, .86);
  transform: translateY(-1px);
}

.flow-landing-auth {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 12px;
}

.flow-login-btn,
.flow-download-btn {
  height: 40px;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 900;
}

.flow-login-btn {
  padding: 0 14px;
  color: rgba(15, 23, 42, .68);
  background: transparent;
}

.flow-download-btn {
  padding: 0 20px;
  color: #fff;
  background: linear-gradient(135deg, #122143, #2563eb);
  box-shadow: 0 14px 28px rgba(37, 99, 235, .2);
}

.flow-hero {
  width: min(1180px, calc(100vw - 56px));
  min-height: calc(100vh - 86px);
  grid-template-columns: minmax(420px, .88fr) minmax(560px, 1.12fr);
  gap: 58px;
  padding: 54px 0 76px;
}

.flow-hero-copy {
  max-width: 640px;
}

.flow-pill-row span,
.flow-keywords b,
.flow-section-tags span {
  min-height: 32px;
  border: 1px solid rgba(20, 184, 166, .22);
  background: rgba(255, 255, 255, .66);
  box-shadow: none;
}

.flow-hero h1 {
  margin: 22px 0 22px;
  max-width: 620px;
  font-size: clamp(52px, 4.9vw, 72px);
  line-height: .96;
  letter-spacing: -.025em;
}

.flow-hero h1 .hero-brand-word {
  color: #2563eb;
}

.flow-hero h1 .hero-title-line {
  color: #101827;
}

.flow-hero p {
  max-width: 58ch;
  color: rgba(15, 23, 42, .68);
  font-size: 18px;
  line-height: 1.9;
}

.flow-route-ribbon {
  position: relative;
  width: min(100%, 560px);
  min-height: 46px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 24px;
  padding: 8px 12px;
  border: 1px solid rgba(37, 99, 235, .12);
  border-radius: 999px;
  background: rgba(255, 255, 255, .64);
  overflow: hidden;
}

.flow-route-ribbon::after {
  position: absolute;
  inset: 0 auto 0 -35%;
  width: 32%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, .86), transparent);
  content: "";
  animation: home-sheen 3.8s ease-in-out infinite;
}

.flow-route-ribbon span {
  position: relative;
  z-index: 1;
  color: #1f3f82;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.flow-route-ribbon i {
  flex: 1 1 22px;
  height: 2px;
  min-width: 16px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(37, 99, 235, .22), rgba(20, 184, 166, .45));
  background-size: 200% 100%;
  animation: route-run 1.7s linear infinite;
}

.flow-primary-btn,
.flow-secondary-btn {
  height: 56px;
  border-radius: 14px;
  transition: transform 180ms ease, box-shadow 180ms ease, background 180ms ease;
}

.flow-primary-btn:hover,
.flow-secondary-btn:hover,
.flow-download-btn:hover {
  transform: translateY(-2px);
}

.flow-product-stage {
  gap: 14px;
}

.flow-step-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.flow-step-grid button {
  min-height: 58px;
  border: 0;
  border-radius: 14px;
  background: rgba(255, 255, 255, .72);
  box-shadow: 0 12px 30px rgba(15, 23, 42, .07);
}

.flow-step-grid button.active {
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
}

.product-flow-panel {
  --panel-accent: #2563eb;
  --panel-accent-soft: rgba(37, 99, 235, .1);
  --panel-warm: rgba(20, 184, 166, .09);
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, .2);
  border-radius: 20px;
  background: rgba(255, 255, 255, .78);
  box-shadow: 0 30px 80px rgba(15, 23, 42, .12);
  animation: product-float 6s ease-in-out infinite, module-swap 420ms cubic-bezier(.16, 1, .3, 1);
}

.product-window-dots {
  display: inline-flex;
  gap: 7px;
}

.product-window-dots i {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: #fb7185;
}

.product-window-dots i:nth-child(2) {
  background: #fbbf24;
}

.product-window-dots i:nth-child(3) {
  background: #34d399;
}

.product-flow-head {
  min-height: 72px;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(148, 163, 184, .18);
}

.product-flow-title {
  min-width: 0;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 5px 10px;
}

.product-flow-title span {
  grid-row: span 2;
}

.product-flow-title small {
  color: rgba(15, 23, 42, .48);
  font-size: 12px;
  font-weight: 850;
}

.product-flow-head button {
  background: var(--panel-accent);
  transition: transform 180ms ease, filter 180ms ease;
}

.product-flow-head button:hover {
  transform: translateY(-1px);
  filter: brightness(1.04);
}

.product-flow-body {
  grid-template-columns: minmax(220px, .78fr) minmax(300px, 1fr);
  gap: 18px;
  min-height: 370px;
  padding: 20px;
  background:
    radial-gradient(circle at 72% 8%, var(--panel-warm), transparent 34%),
    linear-gradient(135deg, rgba(248, 251, 255, .94), rgba(255, 255, 255, .86));
}

.flow-map {
  min-height: 318px;
  border: 1px solid rgba(148, 163, 184, .18);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, .72), rgba(248, 251, 255, .82));
}

.flow-node {
  border-color: rgba(148, 163, 184, .18);
  box-shadow: 0 12px 28px rgba(15, 23, 42, .08);
}

.flow-screen {
  align-content: stretch;
}

.flow-screen-top b {
  font-size: 22px;
}

.flow-screen-top span {
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  color: var(--panel-accent);
  background: var(--panel-accent-soft);
}

.screenshot-slot {
  position: relative;
  min-height: 136px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, .2);
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, .9), rgba(241, 245, 249, .72)),
    #fff;
}

.screenshot-slot::after {
  position: absolute;
  inset: 0 auto 0 -45%;
  width: 40%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, .75), transparent);
  content: "";
  animation: home-sheen 4.2s ease-in-out infinite;
}

.screenshot-toolbar {
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 12px;
  border-bottom: 1px solid rgba(148, 163, 184, .16);
}

.screenshot-toolbar span {
  color: #0f172a;
  font-size: 12px;
  font-weight: 950;
}

.screenshot-toolbar em {
  color: rgba(15, 23, 42, .46);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.screenshot-skeleton {
  display: grid;
  gap: 10px;
  padding: 16px;
}

.screenshot-skeleton i {
  height: 16px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(37, 99, 235, .12), rgba(20, 184, 166, .14), rgba(124, 58, 237, .1));
}

.screenshot-skeleton i:nth-child(2) {
  width: 72%;
}

.screenshot-skeleton i:nth-child(3) {
  width: 46%;
}

.flow-screen-row {
  min-height: 50px;
  border-color: rgba(148, 163, 184, .18);
  border-radius: 14px;
}

.product-flow-foot {
  border-top-color: rgba(148, 163, 184, .18);
}

.journey-section {
  position: relative;
  width: min(1180px, calc(100vw - 56px));
  margin: 0 auto;
  padding: 86px 0 98px;
}

.journey-section-head {
  max-width: 760px;
  margin-bottom: 34px;
}

.journey-section-head span {
  color: #2563eb;
  font-size: 14px;
  font-weight: 950;
}

.journey-section-head h2 {
  max-width: 16ch;
  margin: 12px 0 16px;
  color: #101827;
  font-size: 42px;
  line-height: 1.12;
  letter-spacing: -.02em;
}

.journey-section-head p {
  margin: 0;
  color: rgba(15, 23, 42, .62);
  font-size: 17px;
  line-height: 1.8;
}

.journey-track {
  display: grid;
  gap: 22px;
}

.journey-step {
  display: grid;
  grid-template-columns: minmax(280px, .72fr) minmax(420px, 1fr);
  gap: 28px;
  align-items: stretch;
  padding: 26px;
  border: 1px solid rgba(148, 163, 184, .18);
  border-radius: 22px;
  background: rgba(255, 255, 255, .66);
  box-shadow: 0 20px 60px rgba(15, 23, 42, .06);
}

.journey-copy span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  color: #1d4ed8;
  background: rgba(37, 99, 235, .1);
  font-weight: 950;
}

.journey-copy h3 {
  margin: 18px 0 12px;
  color: #101827;
  font-size: 27px;
  line-height: 1.24;
}

.journey-copy p {
  margin: 0;
  color: rgba(15, 23, 42, .64);
  font-size: 16px;
  line-height: 1.85;
}

.journey-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.journey-actions b {
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  color: #1d4ed8;
  background: rgba(37, 99, 235, .08);
  font-size: 12px;
}

.journey-preview {
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, .2);
  border-radius: 18px;
  background: rgba(255, 255, 255, .74);
}

.journey-preview-bar {
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  border-bottom: 1px solid rgba(148, 163, 184, .16);
}

.journey-preview-bar i {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #fb7185;
}

.journey-preview-bar i:nth-child(2) {
  background: #fbbf24;
}

.journey-preview-bar i:nth-child(3) {
  background: #34d399;
}

.journey-preview-bar strong {
  margin-left: 8px;
  color: #0f172a;
  font-size: 13px;
}

.journey-shot-slot {
  position: relative;
  min-height: 150px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(239, 246, 255, .84), rgba(255, 255, 255, .9));
}

.journey-shot-slot::before {
  position: absolute;
  inset: 18px;
  border: 1px dashed rgba(37, 99, 235, .18);
  border-radius: 14px;
  content: "";
}

.journey-shot-slot span {
  position: relative;
  z-index: 1;
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 950;
}

.journey-shot-slot small {
  position: relative;
  z-index: 1;
  margin-top: -48px;
  color: rgba(15, 23, 42, .48);
  font-size: 12px;
  font-weight: 800;
}

.journey-lines {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.journey-lines em {
  min-height: 36px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  border-radius: 12px;
  color: rgba(15, 23, 42, .7);
  background: rgba(248, 250, 252, .9);
  font-size: 13px;
  font-style: normal;
  font-weight: 850;
}

@keyframes home-ambient {
  from { transform: translate3d(-1.5%, -1%, 0) scale(1); }
  to { transform: translate3d(1.5%, 1%, 0) scale(1.04); }
}

@keyframes home-sheen {
  0%, 48% { transform: translateX(0); opacity: 0; }
  58% { opacity: 1; }
  100% { transform: translateX(460%); opacity: 0; }
}

@keyframes route-run {
  to { background-position: -200% 0; }
}

@media (max-width: 980px) {
  .flow-landing-nav,
  .flow-hero,
  .journey-section,
  .flow-pricing-section {
    width: min(100% - 28px, 760px);
  }

  .flow-nav-links {
    display: none;
  }

  .flow-hero {
    min-height: auto;
    grid-template-columns: 1fr;
    padding-top: 42px;
  }

  .product-flow-body,
  .journey-step {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .flow-landing-nav {
    top: 10px;
    min-height: 56px;
    border-radius: 16px;
  }

  .flow-brand strong {
    font-size: 15px;
  }

  .flow-login-btn {
    display: none;
  }

  .flow-download-btn {
    padding: 0 16px;
  }

  .flow-hero h1 {
    font-size: 40px;
    line-height: .98;
  }

  .flow-hero h1 .hero-title-line {
    white-space: normal;
  }

  .flow-hero p {
    font-size: 16px;
  }

  .flow-route-ribbon {
    align-items: flex-start;
    flex-direction: column;
    border-radius: 18px;
  }

  .flow-route-ribbon i {
    width: 32px;
    flex: 0 0 2px;
  }

  .flow-step-grid {
    grid-template-columns: 1fr;
  }

  .product-flow-head,
  .product-flow-foot {
    align-items: flex-start;
    flex-direction: column;
  }

  .flow-map {
    min-height: 240px;
  }

  .screenshot-toolbar {
    align-items: flex-start;
    flex-direction: column;
    padding: 10px 12px;
  }

  .journey-section-head h2 {
    font-size: 32px;
  }

  .journey-step {
    padding: 18px;
    border-radius: 18px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .flow-landing::before,
  .flow-route-ribbon::after,
  .flow-route-ribbon i,
  .screenshot-slot::after {
    animation: none !important;
  }
}

.ps-home {
  min-height: 100vh;
  overflow-x: clip;
  color: #0f172a;
  background:
    radial-gradient(circle at 12% 18%, rgba(56, 189, 248, .16), transparent 30%),
    radial-gradient(circle at 84% 12%, rgba(129, 140, 248, .16), transparent 28%),
    linear-gradient(135deg, #f6fbff 0%, #ffffff 46%, #f6f8ff 100%);
}

.ps-nav {
  position: sticky;
  top: 18px;
  z-index: 30;
  width: min(1180px, calc(100vw - 56px));
  min-height: 64px;
  display: flex;
  align-items: center;
  gap: 18px;
  margin: 18px auto 0;
  padding: 10px 12px;
  border: 1px solid rgba(148, 163, 184, .22);
  border-radius: 20px;
  background: rgba(255, 255, 255, .78);
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 46px rgba(15, 23, 42, .08);
}

.ps-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #0f172a;
  text-decoration: none;
}

.ps-brand img {
  width: 38px;
  height: 38px;
  border-radius: 12px;
}

.ps-brand strong {
  font-size: 18px;
  font-weight: 950;
}

.ps-nav nav {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  padding: 4px;
  border-radius: 999px;
  background: rgba(241, 245, 249, .72);
}

.ps-nav nav a {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  padding: 0 14px;
  border-radius: 999px;
  color: rgba(15, 23, 42, .66);
  font-size: 14px;
  font-weight: 850;
  text-decoration: none;
  transition: color 180ms ease, background 180ms ease, transform 180ms ease;
}

.ps-nav nav a:hover {
  color: #1d4ed8;
  background: #fff;
  transform: translateY(-1px);
}

.ps-nav > button,
.ps-actions button,
.ps-pricing-grid button {
  border: 0;
  cursor: pointer;
  font-weight: 950;
}

.ps-nav > button {
  height: 42px;
  padding: 0 20px;
  border-radius: 999px;
  color: #fff;
  background: linear-gradient(135deg, #13244a, #2563eb);
  box-shadow: 0 16px 30px rgba(37, 99, 235, .2);
}

.ps-hero {
  width: min(1180px, calc(100vw - 56px));
  min-height: calc(100vh - 92px);
  display: grid;
  grid-template-columns: minmax(390px, .86fr) minmax(560px, 1.14fr);
  align-items: center;
  gap: 62px;
  margin: 0 auto;
  padding: 58px 0 78px;
}

.ps-note-line {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 22px;
}

.ps-note-line span {
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 13px;
  border: 1px solid rgba(20, 184, 166, .22);
  border-radius: 999px;
  color: #1d4ed8;
  background: rgba(255, 255, 255, .72);
  font-size: 13px;
  font-weight: 900;
}

.ps-note-line span::before {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #f59e0b;
  content: "";
}

.ps-hero h1 {
  max-width: 13ch;
  margin: 0 0 22px;
  color: #0f172a;
  font-size: 76px;
  line-height: .95;
  letter-spacing: -.035em;
  font-weight: 950;
  text-wrap: balance;
}

.ps-hero p {
  max-width: 58ch;
  margin: 0;
  color: rgba(15, 23, 42, .68);
  font-size: 18px;
  line-height: 1.9;
  font-weight: 650;
}

.ps-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 30px;
}

.ps-actions button {
  height: 56px;
  padding: 0 28px;
  border-radius: 14px;
  font-size: 16px;
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.ps-actions button:hover,
.ps-pricing-grid button:hover {
  transform: translateY(-2px);
}

.ps-primary {
  color: #fff;
  background: linear-gradient(135deg, #13244a, #2563eb);
  box-shadow: 0 22px 44px rgba(37, 99, 235, .22);
}

.ps-secondary {
  color: #0f172a;
  background: #fff;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, .28);
}

.ps-console {
  display: grid;
  gap: 14px;
}

.ps-console-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.ps-console-tabs button {
  min-height: 58px;
  display: grid;
  align-content: center;
  gap: 3px;
  padding: 10px 13px;
  border: 0;
  border-radius: 14px;
  color: rgba(15, 23, 42, .68);
  background: rgba(255, 255, 255, .78);
  text-align: left;
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 14px 32px rgba(15, 23, 42, .07);
  cursor: pointer;
  transition: transform 180ms ease, background 180ms ease, color 180ms ease;
}

.ps-console-tabs button span {
  color: rgba(15, 23, 42, .42);
  font-size: 11px;
}

.ps-console-tabs button.active {
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  transform: translateY(-3px);
}

.ps-console-tabs button.active span {
  color: rgba(255, 255, 255, .72);
}

.ps-console-window {
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, .22);
  border-radius: 20px;
  background: rgba(255, 255, 255, .82);
  box-shadow: 0 32px 84px rgba(15, 23, 42, .12);
  animation: ps-window-in 360ms cubic-bezier(.16, 1, .3, 1), ps-float 6s ease-in-out infinite;
}

.ps-console-window header {
  min-height: 62px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(148, 163, 184, .18);
}

.ps-window-dots {
  display: inline-flex;
  gap: 7px;
}

.ps-window-dots i {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: #fb7185;
}

.ps-window-dots i:nth-child(2) { background: #fbbf24; }
.ps-window-dots i:nth-child(3) { background: #34d399; }

.ps-console-window header strong {
  color: #0f172a;
  font-size: 17px;
}

.ps-console-window header span {
  margin-left: auto;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 900;
}

.ps-console-body {
  display: grid;
  grid-template-columns: minmax(240px, .82fr) minmax(320px, 1fr);
  gap: 20px;
  padding: 22px;
}

.ps-flow-diagram {
  position: relative;
  min-height: 320px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, .18);
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(248, 251, 255, .92), rgba(255, 255, 255, .8));
}

.ps-flow-diagram svg {
  position: absolute;
  inset: 38px 18px;
  width: calc(100% - 36px);
  height: calc(100% - 76px);
}

.ps-flow-diagram path {
  stroke: #2563eb;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-dasharray: 10 14;
  opacity: .36;
  animation: ps-dash 1.7s linear infinite;
}

.ps-flow-diagram span {
  position: absolute;
  left: calc(10% + var(--i) * 21%);
  top: calc(46% + (var(--i) % 2) * 42px);
  min-width: 76px;
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border: 1px solid rgba(148, 163, 184, .18);
  border-radius: 999px;
  color: #0f172a;
  background: #fff;
  font-size: 12px;
  font-weight: 900;
  box-shadow: 0 14px 28px rgba(15, 23, 42, .08);
  animation: ps-node-in 360ms cubic-bezier(.16, 1, .3, 1) both, ps-node-float 4s ease-in-out infinite;
  animation-delay: calc(var(--i) * 90ms), calc(var(--i) * 120ms + 600ms);
}

.ps-live-panel {
  display: grid;
  align-content: center;
  gap: 13px;
}

.ps-shot-placeholder {
  position: relative;
  min-height: 132px;
  display: grid;
  align-content: center;
  gap: 8px;
  overflow: hidden;
  padding: 22px;
  border: 1px solid rgba(148, 163, 184, .2);
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(239, 246, 255, .84), rgba(255, 255, 255, .9));
}

.ps-shot-placeholder::after {
  position: absolute;
  inset: 0 auto 0 -38%;
  width: 34%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, .82), transparent);
  content: "";
  animation: ps-sheen 3.8s ease-in-out infinite;
}

.ps-shot-placeholder b {
  color: #0f172a;
  font-size: 15px;
}

.ps-shot-placeholder em {
  color: rgba(15, 23, 42, .48);
  font-style: normal;
  font-size: 13px;
  font-weight: 800;
}

.ps-live-rows {
  display: grid;
  gap: 10px;
}

.ps-live-rows div {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 48px;
  padding: 10px 13px;
  border: 1px solid rgba(148, 163, 184, .18);
  border-radius: 13px;
  background: rgba(255, 255, 255, .72);
  animation: ps-row-in 280ms cubic-bezier(.16, 1, .3, 1) both;
}

.ps-live-rows small {
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 950;
}

.ps-live-rows strong {
  min-width: 0;
  overflow: hidden;
  color: rgba(15, 23, 42, .82);
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.ps-meter {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(37, 99, 235, .12);
}

.ps-meter i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2563eb, #14b8a6);
  animation: ps-meter 2.8s ease-in-out infinite;
}

.ps-flow-section,
.ps-compare-section,
.ps-pricing-section {
  width: min(1180px, calc(100vw - 56px));
  margin: 0 auto;
  padding: 88px 0;
}

.ps-section-head {
  max-width: 760px;
  margin-bottom: 34px;
}

.ps-section-head span {
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 950;
}

.ps-section-head h2 {
  max-width: 18ch;
  margin: 12px 0 14px;
  color: #0f172a;
  font-size: 44px;
  line-height: 1.12;
  letter-spacing: -.025em;
}

.ps-section-head p {
  margin: 0;
  color: rgba(15, 23, 42, .62);
  font-size: 17px;
  line-height: 1.8;
}

.ps-flow-chain {
  position: relative;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  padding: 18px;
  border: 1px solid rgba(148, 163, 184, .18);
  border-radius: 22px;
  background: rgba(255, 255, 255, .62);
}

.ps-flow-chain::before {
  position: absolute;
  left: 34px;
  right: 34px;
  top: 58px;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, #2563eb, #14b8a6, #818cf8, #2563eb);
  background-size: 200% 100%;
  content: "";
  animation: ps-route 2.4s linear infinite;
}

.ps-flow-chain article {
  position: relative;
  z-index: 1;
  min-height: 190px;
  padding: 18px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, .78);
}

.ps-flow-chain article span {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  color: #fff;
  background: #2563eb;
  font-size: 13px;
  font-weight: 950;
}

.ps-flow-chain h3 {
  margin: 18px 0 8px;
  color: #0f172a;
  font-size: 18px;
}

.ps-flow-chain p {
  margin: 0;
  color: rgba(15, 23, 42, .6);
  font-size: 13px;
  line-height: 1.7;
}

.ps-comparison {
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, .2);
  border-radius: 20px;
  background: rgba(255, 255, 255, .72);
}

.ps-comparison-head,
.ps-comparison-row {
  display: grid;
  grid-template-columns: .72fr 1.1fr 1.1fr;
  gap: 0;
}

.ps-comparison-head {
  background: rgba(241, 245, 249, .76);
}

.ps-comparison-head strong,
.ps-comparison-row > * {
  padding: 18px 20px;
  border-bottom: 1px solid rgba(148, 163, 184, .16);
}

.ps-comparison-head strong {
  color: #0f172a;
  font-size: 14px;
}

.ps-comparison-row b {
  color: #0f172a;
}

.ps-comparison-row span {
  color: rgba(15, 23, 42, .62);
  line-height: 1.7;
}

.ps-comparison-row span:last-child {
  color: #14532d;
  background: rgba(236, 253, 245, .46);
  font-weight: 800;
}

.ps-pricing-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.ps-pricing-grid article {
  display: grid;
  gap: 14px;
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, .2);
  border-radius: 20px;
  background: rgba(255, 255, 255, .74);
}

.ps-pricing-grid article.featured {
  border-color: rgba(37, 99, 235, .4);
  background: linear-gradient(180deg, rgba(239, 246, 255, .86), rgba(255, 255, 255, .78));
  box-shadow: 0 24px 58px rgba(37, 99, 235, .12);
}

.ps-pricing-grid article > span {
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 950;
}

.ps-pricing-grid h3 {
  margin: 0;
  color: #0f172a;
  font-size: 26px;
}

.ps-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.ps-price strong {
  color: #0f172a;
  font-size: 34px;
}

.ps-price small,
.ps-pricing-grid p {
  color: rgba(15, 23, 42, .58);
  font-weight: 750;
}

.ps-pricing-grid ul {
  display: grid;
  gap: 9px;
  min-height: 116px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.ps-pricing-grid li {
  color: rgba(15, 23, 42, .68);
  font-size: 14px;
  line-height: 1.55;
}

.ps-pricing-grid li::before {
  color: #16a34a;
  font-weight: 950;
  content: "✓ ";
}

.ps-pricing-grid button {
  height: 46px;
  border-radius: 12px;
  color: #fff;
  background: #0f172a;
  transition: transform 180ms ease, filter 180ms ease;
}

@keyframes ps-dash {
  to { stroke-dashoffset: -48; }
}

@keyframes ps-route {
  to { background-position: -200% 0; }
}

@keyframes ps-window-in {
  from { opacity: 0; transform: translateY(16px) scale(.985); filter: blur(8px); }
  to { opacity: 1; transform: translateY(0) scale(1); filter: blur(0); }
}

@keyframes ps-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-7px); }
}

@keyframes ps-node-in {
  from { opacity: 0; transform: translateX(-12px) scale(.94); }
  to { opacity: 1; transform: translateX(0) scale(1); }
}

@keyframes ps-node-float {
  0%, 100% { translate: 0 0; }
  50% { translate: 0 -7px; }
}

@keyframes ps-sheen {
  0%, 45% { transform: translateX(0); opacity: 0; }
  55% { opacity: 1; }
  100% { transform: translateX(430%); opacity: 0; }
}

@keyframes ps-row-in {
  from { opacity: 0; transform: translateX(-14px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes ps-meter {
  0%, 100% { filter: saturate(1); }
  50% { filter: saturate(1.28) brightness(1.06); }
}

@media (max-width: 1080px) {
  .ps-hero,
  .ps-console-body,
  .ps-comparison-head,
  .ps-comparison-row {
    grid-template-columns: 1fr;
  }

  .ps-flow-chain {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .ps-flow-chain::before {
    display: none;
  }
}

@media (max-width: 680px) {
  .ps-nav,
  .ps-hero,
  .ps-flow-section,
  .ps-compare-section,
  .ps-pricing-section {
    width: min(100% - 28px, 760px);
  }

  .ps-nav nav {
    display: none;
  }

  .ps-nav {
    top: 10px;
  }

  .ps-brand strong {
    font-size: 16px;
  }

  .ps-nav > button {
    margin-left: auto;
    padding: 0 16px;
  }

  .ps-hero {
    min-height: auto;
    grid-template-columns: 1fr;
    gap: 34px;
    padding: 44px 0 70px;
  }

  .ps-hero h1 {
    max-width: 10ch;
    font-size: 48px;
  }

  .ps-console-tabs,
  .ps-flow-chain {
    grid-template-columns: 1fr;
  }

  .ps-flow-diagram {
    min-height: 240px;
  }

  .ps-flow-diagram span {
    left: calc(8% + var(--i) * 20%);
    min-width: 62px;
    padding: 0 8px;
  }

  .ps-section-head h2 {
    font-size: 34px;
  }

  .ps-comparison-row > *,
  .ps-comparison-head strong {
    padding: 14px 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ps-console-window,
  .ps-flow-diagram path,
  .ps-flow-diagram span,
  .ps-shot-placeholder::after,
  .ps-meter i,
  .ps-flow-chain::before {
    animation: none !important;
  }
}
</style>
