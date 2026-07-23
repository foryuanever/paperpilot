<template>
  <div class="home-root">

    <!-- ═══ NAV ═══ -->
    <header class="home-nav" :class="{ scrolled: navScrolled }">
      <div class="nav-inner">
        <router-link class="brand" to="/">
          <div class="brand-mark">
            <svg viewBox="0 0 40 40" fill="none">
              <rect width="40" height="40" rx="10" fill="url(#bg)"/>
              <path d="M11 29 L15.5 11 L25 11 Q32 11 32 18 Q32 24 25 25 L20 25 L22.5 29Z" fill="white" opacity="0.92"/>
              <path d="M15.5 24 L18 29" stroke="white" stroke-width="2" stroke-linecap="round" opacity="0.5"/>
              <defs>
                <linearGradient id="bg" x1="0" y1="0" x2="40" y2="40">
                  <stop offset="0%" stop-color="#2563eb"/><stop offset="100%" stop-color="#7c3aed"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <span class="brand-name">PaperSlover</span>
          <span class="brand-tag">beta</span>
        </router-link>
        <nav class="nav-links">
          <a href="#features">功能</a>
          <a href="#workflow">使用流程</a>
          <a href="#why">为什么选我们</a>
          <a href="#pricing">价格</a>
        </nav>
        <div class="nav-ctas">
          <button class="btn-ghost" @click="openModal('login')">登录</button>
          <button class="btn-solid" @click="openModal('register')">
            免费开始
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
          </button>
        </div>
      </div>
    </header>

    <!-- ═══ HERO — Long, Softly-lit Headline with Unequal Line Lengths ═══ -->
    <section class="hero">
      <div class="hero-bg-grid"></div>
      <div class="hero-glow glow-1"></div>
      <div class="hero-glow glow-2"></div>
      <div class="hero-glow glow-3"></div>

      <div class="hero-center" :class="{ in: heroIn }">

        <!-- Pill -->
        <div class="hero-pill">
          <span class="pill-dot"></span>
          AI 学术科研工作台 · 读写研一体化
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
        </div>

        <!-- Longer, Softly Glowing Headline with Unequal Lengths -->
        <h1 class="hero-h1">
          <div class="h1-line-top">从海量 PDF 文献中</div>
          <div class="h1-line-bot">一站式完成精读、综述与组会汇报</div>
        </h1>

        <!-- Sub caption -->
        <p class="hero-sub">
          导入 PDF、精读标注、智能综述、准备汇报，<br class="hide-sm">
          整条工作流连续衔接，无需在多个工具之间来回搬运。
        </p>

        <!-- CTAs -->
        <div class="hero-actions">
          <button class="cta-primary" @click="openModal('register')">
            免费开始使用
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
          </button>
          <button class="cta-outline" @click="elScrollTo('#workflow')">了解工作流程</button>
        </div>

        <!-- Social proof -->
        <div class="hero-proof">
          <div class="proof-avatars">
            <span v-for="c in ['#3b82f6','#8b5cf6','#ec4899','#f59e0b','#10b981']" :key="c" :style="{ background: c }"></span>
          </div>
          <span>已有 <strong>10,000+</strong> 科研学者在用 · 北大、清华、浙大校园认证用户已加入</span>
        </div>

        <!-- Stats bar -->
        <div class="hero-stats">
          <div class="stat"><strong>10,000+</strong><span>活跃用户</span></div>
          <div class="stat-div"></div>
          <div class="stat"><strong>6 大</strong><span>核心工作模块</span></div>
          <div class="stat-div"></div>
          <div class="stat"><strong>3 秒</strong><span>快速提炼综述</span></div>
          <div class="stat-div"></div>
          <div class="stat"><strong>永久免费</strong><span>基础功能</span></div>
        </div>

      </div>
    </section>

    <!-- ═══ FEATURES ═══ -->
    <section id="features" class="section" ref="featRef">
      <div class="section-wrap">
        <div class="s-head" :class="{ in: featIn }">
          <span class="eyebrow"><i class="ew-dot"></i>六大模块</span>
          <h2>从入库到汇报，每一步都有对应工具</h2>
          <p>不是把功能堆在一起，而是让每一步的结果自然流入下一步</p>
        </div>
        <div class="feat-grid" :class="{ in: featIn }">
          <div v-for="(f, i) in features" :key="f.title" class="feat-card" :style="{ '--c': f.color, '--delay': i * 0.06 + 's' }">
            <div class="fc-icon">
              <svg :viewBox="f.vb" fill="none" stroke="currentColor" stroke-width="1.7" v-html="f.path"></svg>
            </div>
            <div class="fc-body">
              <h3>{{ f.title }}</h3>
              <p>{{ f.desc }}</p>
              <div class="fc-chips">
                <span v-for="t in f.chips" :key="t">{{ t }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ WORKFLOW — Full-Page Unbounded Slide Carousel ═══ -->
    <section id="workflow" class="workflow-section-full">
      <div class="section-wrap">
        <div class="s-head" ref="wfHeadRef" :class="{ in: wfHeadIn }">
          <span class="eyebrow"><i class="ew-dot"></i>使用流程</span>
          <h2>五步，从一篇 PDF 到一场汇报</h2>
          <p>每一步都可以单独使用，也可以顺着走完整条研读链条</p>
        </div>
      </div>

      <!-- FULL-PAGE UNCONTAINED CAROUSEL (No borders, 100vw unbounded slides) -->
      <div class="full-carousel-viewport">
        <div class="full-carousel-track" :style="{ transform: `translateX(-${wfActive * 100}vw)` }">
          <div
            v-for="(step, i) in workflowSteps"
            :key="step.id"
            class="full-slide-item"
            :class="{ active: wfActive === i }"
          >
            <!-- Left Side: Large Viewport Screenshot -->
            <div class="full-slide-media">
              <div class="media-container">
                <img :src="step.img" :alt="step.title" loading="lazy" />
                <div class="media-fade-overlay"></div>
                <div class="media-step-badge">STEP 0{{ i + 1 }}</div>
              </div>
            </div>

            <!-- Right Side: Detailed Copy with Staggered Entrance Animations -->
            <div class="full-slide-info">
              <div class="info-content-box">
                <div class="info-meta">
                  <span class="info-counter">0{{ i + 1 }} / 0{{ workflowSteps.length }}</span>
                  <span class="info-label">核心环节</span>
                </div>

                <h3 class="info-title">{{ step.title }}</h3>
                <p class="info-desc">{{ step.desc }}</p>

                <!-- Detailed Highlighting Bullet Points -->
                <div class="info-highlights-list">
                  <div v-for="(hl, idx) in step.highlights" :key="idx" class="info-hl-row">
                    <span class="info-hl-icon">✦</span>
                    <span class="info-hl-text" v-html="hl"></span>
                  </div>
                </div>

                <!-- Tags -->
                <div class="info-tags">
                  <span v-for="t in step.tags" :key="t" class="info-tag-item">{{ t }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Floating Controls Bar -->
        <div class="full-carousel-controls">
          <button class="fc-arrow-btn prev" @click="wfGo(wfActive - 1)" aria-label="上一页">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5"/><path d="m12 5-7 7 7 7"/></svg>
          </button>

          <div class="fc-dots-wrap">
            <span
              v-for="(_, i) in workflowSteps"
              :key="i"
              class="fc-dot-item"
              :class="{ active: wfActive === i }"
              @click="wfGo(i)"
            ></span>
          </div>

          <div class="fc-progress-indicator" title="自动播放中">
            <svg viewBox="0 0 36 36">
              <circle cx="18" cy="18" r="14" fill="none" stroke="rgba(255,255,255,.1)" stroke-width="2.5"/>
              <circle cx="18" cy="18" r="14" fill="none" stroke="#3b82f6" stroke-width="2.5"
                stroke-dasharray="88" :stroke-dashoffset="88 - 88 * wfProgress / 100"
                stroke-linecap="round" transform="rotate(-90 18 18)"/>
            </svg>
          </div>

          <button class="fc-arrow-btn next" @click="wfGo(wfActive + 1)" aria-label="下一页">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
          </button>
        </div>
      </div>
    </section>

    <!-- ═══ WHY CHOOSE US ═══ -->
    <section id="why" class="section" ref="whyRef">
      <div class="section-wrap">
        <div class="s-head" :class="{ in: whyIn }">
          <span class="eyebrow"><i class="ew-dot"></i>为什么选择我们</span>
          <h2>科研路上，你值得更好的工具</h2>
          <p>不只是一个 PDF 阅读器，而是一套从阅读到汇报的完整工作台</p>
        </div>

        <div class="why-claims" :class="{ in: whyIn }">
          <div v-for="c in whyClaims" :key="c.title" class="why-card" :style="{ '--c': c.color }">
            <div class="wc-icon">
              <svg :viewBox="c.vb" fill="none" stroke="currentColor" stroke-width="1.7" v-html="c.path"></svg>
            </div>
            <strong class="wc-title">{{ c.title }}</strong>
            <p class="wc-desc">{{ c.desc }}</p>
          </div>
        </div>

        <!-- Quote banner -->
        <div class="why-banner" :class="{ in: whyIn }">
          <div class="wb-left">
            <div class="wb-q">"</div>
            <p class="wb-text">做学术本来就很难了，<br>工具不应该再给你添堵。</p>
            <p class="wb-by">— PaperSlover 产品理念</p>
          </div>
          <div class="wb-right">
            <div v-for="b in whyBadges" :key="b.label" class="wb-badge" :style="{ '--c': b.color }">
              <svg :viewBox="b.vb" fill="none" stroke="currentColor" stroke-width="2" v-html="b.path"></svg>
              <span>{{ b.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ COMPARISON ═══ -->
    <section class="section section-alt" ref="cmpRef">
      <div class="section-wrap">
        <div class="s-head" :class="{ in: cmpIn }">
          <span class="eyebrow"><i class="ew-dot"></i>横向对比</span>
          <h2>少掉的，是整理材料的那段时间</h2>
          <p>让你把时间花在真正重要的地方，而不是在工具之间反复搬运</p>
        </div>
        <div class="cmp-table" :class="{ in: cmpIn }">
          <div class="cmp-header">
            <span>你在意的</span>
            <span class="cmp-bad-h">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M18 6 6 18M6 6l12 12"/></svg>
              以前的方式
            </span>
            <span class="cmp-good-h">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M20 6 9 17l-5-5"/></svg>
              PaperSlover
            </span>
          </div>
          <div v-for="row in comparisons" :key="row.feature" class="cmp-row">
            <b>{{ row.feature }}</b>
            <span class="cmp-bad">
              <span class="ci bad"><svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M18 6 6 18M6 6l12 12"/></svg></span>
              {{ row.before }}
            </span>
            <span class="cmp-good">
              <span class="ci good"><svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M20 6 9 17l-5-5"/></svg></span>
              {{ row.after }}
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ PRICING ═══ -->
    <section id="pricing" class="section" ref="priceRef">
      <div class="section-wrap">
        <div class="s-head" :class="{ in: priceIn }">
          <span class="eyebrow"><i class="ew-dot"></i>价格方案</span>
          <h2>基础功能永久免费，高消耗任务按套餐</h2>
          <p>导入和阅读不计费；综述生成、组会 PPT、深度解析这些重量级操作按套餐使用</p>
        </div>
        <div class="price-grid" :class="{ in: priceIn }">
          <div v-for="plan in billingPlans" :key="plan.id" class="price-card" :class="{ featured: plan.highlight }">
            <div v-if="plan.highlight" class="price-badge">最受欢迎</div>
            <div class="price-type">{{ plan.oneTime ? '加量包' : '订阅方案' }}</div>
            <div class="price-name">{{ plan.name }}</div>
            <div class="price-amount"><strong>{{ plan.price }}</strong><small>{{ plan.period }}</small></div>
            <ul class="price-list">
              <li v-for="f in plan.features.slice(0, 4)" :key="f">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="2.5"><path d="M20 6 9 17l-5-5"/></svg>{{ f }}
              </li>
            </ul>
            <button class="price-btn" :class="{ 'feat-btn': plan.highlight }" @click="openModal('register')">
              {{ plan.oneTime ? '购买加量包' : '开始使用' }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ FOOTER ═══ -->
    <footer class="home-footer">
      <div class="footer-inner">
        <span class="footer-logo">PaperSlover</span>
        <div class="footer-links">
          <a href="#features">功能</a>
          <a href="#workflow">流程</a>
          <a href="#why">为什么选我们</a>
          <a href="#pricing">价格</a>
          <a href="#" @click.prevent="openModal('login')">登录</a>
        </div>
        <p class="footer-copy">© 2026 PaperSlover · 为每一位科研人设计</p>
      </div>
    </footer>

    <!-- ═══ AUTH MODAL ═══ -->
    <Transition name="mfade">
      <div v-if="showAuthModal" class="modal-mask" @click="closeModal">
        <div class="modal-box" @click.stop>
          <button class="modal-x" @click="closeModal">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
          </button>
          <!-- LOGIN -->
          <div v-if="authMode === 'login'" class="auth-pane">
            <div class="auth-label">LOG IN</div>
            <h3 class="auth-title">进入工作台</h3>
            <div class="auth-form">
              <label>邮箱</label>
              <input v-model="email" type="email" placeholder="student@paperslover.app" />
              <label>密码</label>
              <div class="pw-wrap">
                <input v-model="password" :type="showPassword ? 'text' : 'password'" placeholder="输入密码" />
                <button type="button" class="pw-eye" @click="showPassword = !showPassword" v-html="showPassword ? eyeOffIcon : eyeIcon"></button>
              </div>
              <div v-if="errorText" class="auth-err">{{ errorText }}</div>
              <button class="auth-submit" :disabled="loading" @click="submitLogin">{{ loading ? '登录中…' : '进入 PaperSlover' }}</button>
              <div class="auth-links">
                <a href="#" @click.prevent="authMode='register'">没有账号？去注册</a>
                <a href="#" @click.prevent="authMode='forgot_password'" class="dim">忘记密码</a>
              </div>
            </div>
          </div>
          <!-- REGISTER -->
          <div v-else-if="authMode === 'register'" class="auth-pane">
            <div class="auth-label">REGISTER</div>
            <h3 class="auth-title">创建账号</h3>
            <div class="auth-form scroll-form">
              <label>QQ 邮箱</label>
              <div class="row-input">
                <input v-model="email" type="email" placeholder="123456@qq.com" />
                <button type="button" class="code-btn" :disabled="sendingRegisterCode || registerCodeCooldown > 0" @click="sendRegisterCode">{{ registerCodeButtonText }}</button>
              </div>
              <label>验证码</label>
              <input v-model="verificationCode" inputmode="numeric" maxlength="6" placeholder="6 位邮箱验证码" />
              <label>昵称</label>
              <input v-model="name" placeholder="你的昵称" />
              <label>密码</label>
              <div class="pw-wrap">
                <input v-model="password" :type="showPassword ? 'text' : 'password'" placeholder="设置密码" />
                <button type="button" class="pw-eye" @click="showPassword = !showPassword" v-html="showPassword ? eyeOffIcon : eyeIcon"></button>
              </div>
              <label>邀请码 <span class="opt">可选</span></label>
              <input v-model="inviteCode" placeholder="有邀请码可填" />
              <label>身份</label>
              <div class="role-group">
                <button type="button" :class="{ on: role==='学生' }" @click="role='学生'">学生</button>
                <button type="button" :class="{ on: role==='导师' }" @click="role='导师'">导师</button>
                <button type="button" :class="{ on: role==='管理员' }" @click="role='管理员'">管理员</button>
              </div>
              <template v-if="role==='导师'">
                <label>导师邀请码</label>
                <input v-model="mentorInviteCode" type="password" placeholder="TUTOR2026" />
              </template>
              <template v-if="role==='管理员'">
                <label>管理员邀请码</label>
                <input v-model="mentorInviteCode" type="password" placeholder="ADMIN2026" />
              </template>
              <div v-if="registerSuccessText" class="auth-ok">{{ registerSuccessText }}</div>
              <div v-if="errorText" class="auth-err">{{ errorText }}</div>
              <button class="auth-submit" :disabled="loading" @click="submitRegister">{{ loading ? '创建中…' : '创建账号并进入' }}</button>
              <div class="auth-links"><a href="#" @click.prevent="authMode='login'">已有账号？登录</a></div>
            </div>
          </div>
          <!-- FORGOT -->
          <div v-else-if="authMode === 'forgot_password'" class="auth-pane">
            <div class="auth-label">RESET</div>
            <h3 class="auth-title">找回密码</h3>
            <div class="auth-form">
              <label>邮箱</label>
              <div class="row-input">
                <input v-model="forgotEmail" type="email" placeholder="you@paperslover.app" />
                <button type="button" class="code-btn" :disabled="sendingCode" @click="sendForgotCode">{{ sendingCode ? '发送中…' : '获取验证码' }}</button>
              </div>
              <label>验证码</label>
              <input v-model="forgotCode" placeholder="输入验证码" />
              <label>新密码</label>
              <div class="pw-wrap">
                <input v-model="forgotNewPassword" :type="showPassword ? 'text' : 'password'" placeholder="至少 6 位" />
                <button type="button" class="pw-eye" @click="showPassword = !showPassword" v-html="showPassword ? eyeOffIcon : eyeIcon"></button>
              </div>
              <div v-if="forgotSuccessText" class="auth-ok">{{ forgotSuccessText }}</div>
              <div v-if="errorText" class="auth-err">{{ errorText }}</div>
              <button class="auth-submit" :disabled="loading" @click="submitResetPassword">{{ loading ? '重置中…' : '确认重置' }}</button>
              <div class="auth-links"><a href="#" @click.prevent="authMode='login'">返回登录</a></div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { billingPlans } from '../constants/pages'
import { paperpilotApi } from '../services/paperpilotApi'

const authStore = useAuthStore()
const router    = useRouter()
const route     = useRoute()

/* ── Nav scroll ── */
const navScrolled = ref(false)
const onScroll = () => { navScrolled.value = window.scrollY > 40 }

/* ── Hero entrance ── */
const heroIn = ref(false)

/* ── Scroll reveal ── */
const featRef    = ref(null); const featIn    = ref(false)
const wfHeadRef  = ref(null); const wfHeadIn  = ref(false)
const whyRef     = ref(null); const whyIn     = ref(false)
const cmpRef     = ref(null); const cmpIn     = ref(false)
const priceRef   = ref(null); const priceIn   = ref(false)

let io
function initIO() {
  io = new IntersectionObserver(entries => {
    entries.forEach(e => {
      if (!e.isIntersecting) return
      if (e.target === featRef.value)   featIn.value   = true
      if (e.target === wfHeadRef.value) wfHeadIn.value = true
      if (e.target === whyRef.value)    whyIn.value    = true
      if (e.target === cmpRef.value)    cmpIn.value    = true
      if (e.target === priceRef.value)  priceIn.value  = true
    })
  }, { threshold: 0.1 })
  ;[featRef, wfHeadRef, whyRef, cmpRef, priceRef].forEach(r => r.value && io.observe(r.value))
}

/* ── Workflow full-slide carousel ── */
const wfActive   = ref(0)
const wfProgress = ref(0)
const WF_DURATION = 5000   // ms per slide
const TICK        = 60     // progress update interval

let wfSlideTimer    = null
let wfProgressTimer = null

function wfGo(i) {
  const len = workflowSteps.length
  wfActive.value = ((i % len) + len) % len
  wfProgress.value = 0
  resetWfAuto()
}

function resetWfAuto() {
  clearTimeout(wfSlideTimer)
  clearInterval(wfProgressTimer)
  let elapsed = 0
  wfProgressTimer = setInterval(() => {
    elapsed += TICK
    wfProgress.value = Math.min(100, (elapsed / WF_DURATION) * 100)
  }, TICK)
  wfSlideTimer = setTimeout(() => {
    wfGo(wfActive.value + 1)
  }, WF_DURATION)
}

/* ── Misc helpers ── */
function elScrollTo(hash) {
  document.querySelector(hash)?.scrollIntoView({ behavior: 'smooth' })
}

/* ── Auth ── */
const showAuthModal      = ref(false)
const authMode           = ref('login')
const email              = ref('')
const password           = ref('')
const name               = ref('')
const inviteCode         = ref('')
const role               = ref('学生')
const mentorInviteCode   = ref('')
const verificationCode   = ref('')
const errorText          = ref('')
const loading            = ref(false)
const sendingRegisterCode= ref(false)
const registerCodeCooldown=ref(0)
const registerSuccessText= ref('')
const forgotEmail        = ref('')
const forgotCode         = ref('')
const forgotNewPassword  = ref('')
const sendingCode        = ref(false)
const forgotSuccessText  = ref('')
const showPassword       = ref(false)
let rcTimer = null

const eyeIcon    = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:18px;height:18px"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`
const eyeOffIcon = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:18px;height:18px"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>`

const registerCodeButtonText = computed(() => {
  if (sendingRegisterCode.value) return '发送中'
  if (registerCodeCooldown.value > 0) return `${registerCodeCooldown.value}s`
  return '获取验证码'
})
function isQqEmail(v) { return /^[1-9][0-9]{4,11}@qq\.com$/i.test(String(v||'').trim()) }
function startCooldown() {
  registerCodeCooldown.value = 60
  if (rcTimer) clearInterval(rcTimer)
  rcTimer = setInterval(() => { registerCodeCooldown.value -= 1; if (registerCodeCooldown.value <= 0) { clearInterval(rcTimer); rcTimer = null } }, 1000)
}
async function sendRegisterCode() {
  const e = email.value.trim().toLowerCase()
  if (!isQqEmail(e)) { errorText.value = '请填写 QQ 邮箱，例如 123456@qq.com'; return }
  sendingRegisterCode.value = true; errorText.value = ''; registerSuccessText.value = ''
  try { await paperpilotApi.sendRegisterCode(e); email.value = e; registerSuccessText.value = '验证码已发送，10 分钟内有效。'; startCooldown() }
  catch (err) { errorText.value = err.response?.data?.message || err.message }
  finally { sendingRegisterCode.value = false }
}
async function sendForgotCode() {
  if (!forgotEmail.value) { errorText.value = '请输入邮箱'; return }
  sendingCode.value = true; errorText.value = ''; forgotSuccessText.value = ''
  try { await paperpilotApi.sendForgotPasswordCode(forgotEmail.value); forgotSuccessText.value = '验证码已生成，请在系统日志中查看。' }
  catch (err) { errorText.value = err.response?.data?.message || err.message }
  finally { sendingCode.value = false }
}
async function submitResetPassword() {
  if (!forgotEmail.value || !forgotCode.value || !forgotNewPassword.value) { errorText.value = '请填写全部字段'; return }
  loading.value = true; errorText.value = ''; forgotSuccessText.value = ''
  try {
    await paperpilotApi.resetPasswordWithCode({ email: forgotEmail.value, code: forgotCode.value, newPassword: forgotNewPassword.value })
    forgotSuccessText.value = '密码已重置，即将跳转登录…'
    setTimeout(() => { authMode.value = 'login'; forgotSuccessText.value = ''; email.value = forgotEmail.value; password.value = forgotNewPassword.value }, 2000)
  } catch (err) { errorText.value = err.response?.data?.message || err.message }
  finally { loading.value = false }
}
function openModal(mode) {
  authMode.value = mode; errorText.value = ''; registerSuccessText.value = ''; forgotSuccessText.value = ''; showPassword.value = false
  if (mode === 'login') { email.value = 'student@paperslover.app'; password.value = 'Student2026!' }
  else { email.value = ''; password.value = ''; verificationCode.value = ''; inviteCode.value = ''; name.value = '' }
  showAuthModal.value = true
}
function closeModal() { showAuthModal.value = false; showPassword.value = false; errorText.value = '' }
async function submitLogin() {
  loading.value = true; errorText.value = ''
  try { await authStore.login({ email: email.value, password: password.value }); router.push(authStore.session.role === '管理员' ? '/admin' : '/library') }
  catch (err) { errorText.value = err.response?.data?.message || err.message }
  finally { loading.value = false }
}
async function submitRegister() {
  if (!isQqEmail(email.value)) { errorText.value = '请使用 QQ 邮箱注册'; return }
  if (!verificationCode.value || verificationCode.value.length !== 6) { errorText.value = '请输入 6 位验证码'; return }
  loading.value = true; errorText.value = ''
  try { await authStore.register({ inviteCode: inviteCode.value, name: name.value, email: email.value, password: password.value, role: role.value, mentorInviteCode: mentorInviteCode.value, verificationCode: verificationCode.value }); router.push(authStore.session.role === '管理员' ? '/admin' : '/library') }
  catch (err) { errorText.value = err.response?.data?.message || err.message }
  finally { loading.value = false }
}

/* ── Static data ── */
const features = [
  { title:'文献库',   desc:'上传 PDF、批量导入 Zotero，补全元数据，在同一个地方统一管理所有论文，进度和标签永不丢失。',  vb:'0 0 24 24', path:'<path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H19a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H6.5a1 1 0 0 1 0-5H20"/><line x1="8" y1="7" x2="16" y2="7"/><line x1="8" y1="11" x2="13" y2="11"/>',  color:'#3b82f6', chips:['PDF 上传','Zotero 导入','标签筛选'] },
  { title:'逐段精读', desc:'左侧原文、右侧翻译，选中句子保存批注，背景、方法、局限分开展开。批注落在选区，不会把整段都涂乱。', vb:'0 0 24 24', path:'<path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>',                                                             color:'#8b5cf6', chips:['全文翻译','精确标注','内容拆解'] },
  { title:'论文综述', desc:'研究问题、主要发现、汇报价值分段生成，数字和英文关键词重点标出，保存后可直接导入组会使用。',     vb:'0 0 24 24', path:'<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/>', color:'#10b981', chips:['分段整理','重点标红','一键导入'] },
  { title:'组会汇报', desc:'最多三篇文献合并成一条主线，生成组会讲稿、导师建议，并输出完整 PPT。单篇同样支持。',             vb:'0 0 24 24', path:'<rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/><path d="m7 10 3 3 5-5"/>',                                                                                                   color:'#f59e0b', chips:['多篇融合','讲稿生成','PPT 导出'] },
  { title:'校园论坛', desc:'学校认证后按学校筛选帖子，发帖先过内容检查，回复和通知推送到站内消息，不需要守着群聊。',           vb:'0 0 24 24', path:'<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><line x1="9" y1="10" x2="15" y2="10"/><line x1="9" y1="14" x2="13" y2="14"/>',                               color:'#ec4899', chips:['学校认证','帖子筛选','站内消息'] },
  { title:'额度管理', desc:'不同操作走不同配置：生成 PPT 用高配，综述和检查可选低成本，按使用量计费，剩余次数随时查。',       vb:'0 0 24 24', path:'<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',                                                                                                                                    color:'#06b6d4', chips:['按量计费','模型配置','套餐方案'] },
]

const workflowSteps = [
  {
    id: 'lib',
    title: '统一文献归纳与题录沉淀',
    desc: '支持快速导入本地 PDF 论文，或一键导入 Zotero 题录数据。系统会自动补全作者、期刊与出版年份，保持元数据完整。',
    highlights: [
      '⚡️ <b>无缝接轨 Zotero</b>：一键同步导出文件，免去手动输录',
      '🏷️ <b>多维度标签分类</b>：按开题、复现、组会自定义标记',
      '📂 <b>全局云端同步</b>：阅读进度与分类跨终端时刻保存'
    ],
    tags: ['PDF 导入', 'Zotero 同步', '元数据自动补全'],
    img: '/workflow/library.png'
  },
  {
    id: 'read',
    title: '双栏对齐与选区精准批注',
    desc: '双栏并行对比阅读，左侧展示原文，右侧实时对照机器翻译。支持句子级别的选区精准高亮，不会涂乱正文版面。',
    highlights: [
      '🔍 <b>选区级高亮划线</b>：精确锁定到句子，随时撤销独立批注',
      '🧩 <b>结构化拆解视图</b>：背景、方法、实验结果与局限分开阅读',
      '🌐 <b>即时高保真翻译</b>：保持学术专业词汇表达准确'
    ],
    tags: ['原文对照', '选区划线批注', '结构化深度解析'],
    img: '/workflow/reader.png'
  },
  {
    id: 'review',
    title: '核心要点整理与智能综述',
    desc: '系统自动提炼研究背景、关键发现、方法亮点与汇报价值。对核心数字、百分比及英文指标进行智能高亮显示。',
    highlights: [
      '📊 <b>关键指标高亮</b>：关键数据集与实验增幅一目了然',
      '📝 <b>结构化综述生成</b>：包含主要贡献、局限与可拓展点',
      '🔗 <b>直接流转至组会</b>：一键导入组会模块，免去复制粘贴'
    ],
    tags: ['结构化综述', '关键数据高亮', '一键转流组会'],
    img: '/workflow/review.png'
  },
  {
    id: 'meet',
    title: '多篇文献融合与 PPT 自动生成',
    desc: '打破单篇文献局限，支持最多同时选择三篇论文合成为统一的组会汇报主线，并自动输出讲稿、导师 Q&A 与完整 PPT。',
    highlights: [
      '🔀 <b>多论文主线融合</b>：自动提取多篇文献的关联与对比逻辑',
      '🎙️ <b>讲稿与问题准备</b>：预估导师可能提问并生成答辩建议',
      '🖥️ <b>一键导出 PPTX</b>：生成排版规范的高质量演示文稿'
    ],
    tags: ['多篇文献融合', '汇报讲稿提炼', '一键导出 PPT'],
    img: '/workflow/meeting.png'
  },
  {
    id: 'forum',
    title: '实名校园圈与学术讨论交流',
    desc: '完成高校校园认证后，可进入专属校园板块参与同校同行的深度学术探讨。内置智能审核机制与站内实时通知。',
    highlights: [
      '🎓 <b>高校认证身份</b>：保障学术圈子真实纯粹',
      '🛡️ <b>智能合规审核</b>：自动化过滤垃圾信息与敏感内容',
      '🔔 <b>站内消息直达</b>：回复、点赞与认证提醒即时送达'
    ],
    tags: ['校园认证', '同行学术探讨', '站内实时通知'],
    img: '/workflow/forum.png'
  }
]

const whyClaims = [
  { title:'整条流程一条线，不再分散',    desc:'从导入 PDF 到输出 PPT，每一步的结果自动流入下一步，节省大量重复劳动。',              color:'#3b82f6', vb:'0 0 24 24', path:'<path d="M5 12h14"/><path d="m12 5 7 7-7 7"/><circle cx="5" cy="12" r="2"/><circle cx="19" cy="12" r="2"/>'                          },
  { title:'批注精确到句子，不涂乱整段',  desc:'选中任意一段文字即可保存批注，只落在选区，可单独删除，也可一键清空。',                color:'#8b5cf6', vb:'0 0 24 24', path:'<path d="m12 19 7-7 3 3-7 7-3-3z"/><path d="m18 13-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/><path d="m2 2 7.586 7.586"/><circle cx="11" cy="11" r="2"/>'},
  { title:'组会材料不需要手动拼接',      desc:'最多三篇文献合并成汇报主线，讲稿、关键问题和 PPT 一起生成，不需要手动复制摘要。',    color:'#10b981', vb:'0 0 24 24', path:'<rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/><path d="m7 10 3 3 5-5"/>'                     },
  { title:'学校认证，讨论不再丢失',      desc:'按学校筛选帖子，同校同行的经验更容易找到。回复和认证反馈全部推送到站内消息。',        color:'#ec4899', vb:'0 0 24 24', path:'<path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.15 13a19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 3.06 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 21 16.92z"/>'},
  { title:'按实际使用计费，看得清',      desc:'不同操作用不同配置，基础阅读免费，综述和 PPT 按次数展示。剩余次数随时查看。',        color:'#f59e0b', vb:'0 0 24 24', path:'<line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>'                               },
  { title:'数据留在你自己手里',          desc:'批注、综述、汇报讲稿全部保存在你的账号下，随时导出。你的研究材料永远属于你。',        color:'#06b6d4', vb:'0 0 24 24', path:'<rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>'                                     },
]

const whyBadges = [
  { label:'永久免费基础功能',    color:'#22c55e', vb:'0 0 24 24', path:'<path d="M20 6 9 17l-5-5"/>'                                                                                    },
  { label:'无需安装，浏览器直用', color:'#3b82f6', vb:'0 0 24 24', path:'<circle cx="12" cy="12" r="10"/><path d="M2 12h20"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>' },
  { label:'校园认证用户专属权益', color:'#8b5cf6', vb:'0 0 24 24', path:'<path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/>'                                },
  { label:'数据安全，随时导出',   color:'#f59e0b', vb:'0 0 24 24', path:'<rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>'             },
]

const comparisons = [
  { feature:'文献到产出',  before:'阅读、总结、PPT 分散在多个工具，每步都要重新开始',        after:'导入、阅读、综述、汇报衔接同一份文献，结果自然流入下一步'    },
  { feature:'标注与批注',  before:'很多工具只给整段摘要，标注和正文容易对不上',              after:'选区精确到句子，批注可单独删除，也可以全局清空'                },
  { feature:'组会准备',    before:'需要手动把多篇论文拼成讲稿，再去做 PPT',                  after:'最多三篇文献融合成主线，讲稿和 PPT 一起生成'                  },
  { feature:'校园交流',    before:'讨论在群里丢失，发的东西没人管，也找不回来',              after:'学校认证 + 按校筛选 + 站内通知全面替代群聊'                   },
  { feature:'使用成本',    before:'不清楚每个功能消耗多少，月底账单看不懂',                  after:'不同操作按实际消耗计费，剩余次数随时查，不超出预期'           },
]

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  setTimeout(() => { heroIn.value = true }, 80)
  setTimeout(initIO, 200)
  setTimeout(resetWfAuto, 1200)
  if (route.query.auth === 'register' || route.query.show === 'register') openModal('register')
  else if (route.query.auth === 'login'  || route.query.show === 'login')  openModal('login')
})
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  io?.disconnect()
  clearTimeout(wfSlideTimer)
  clearInterval(wfProgressTimer)
  if (rcTimer) clearInterval(rcTimer)
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Geist:wght@100;200;300;400;500;600;700;800;900&family=Geist+Mono:wght@400;500;600&display=swap');

*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0 }
.home-root {
  min-height: 100vh;
  background: #08080c;
  color: #e2e2e6;
  font-family: 'Geist', system-ui, -apple-system, sans-serif;
  -webkit-font-smoothing: antialiased;
  overflow-x: hidden;
}

/* ══ NAV ══ */
.home-nav {
  position: fixed; top: 0; left: 0; right: 0; z-index: 200;
  transition: background .3s, border-color .3s;
  border-bottom: 1px solid transparent;
}
.home-nav.scrolled { background: rgba(8,8,12,.84); backdrop-filter: blur(22px); -webkit-backdrop-filter: blur(22px); border-bottom-color: rgba(255,255,255,.055); }
.nav-inner { max-width: 100%; margin: 0 auto; padding: 0 48px; height: 62px; display: flex; align-items: center; gap: 24px; }
.brand { display: flex; align-items: center; gap: 10px; text-decoration: none; color: inherit; flex-shrink: 0; }
.brand-mark svg { width: 36px; height: 36px; }
.brand-name { font-size: 1.05rem; font-weight: 700; color: #f0f0f4; letter-spacing: -.025em; }
.brand-tag  { font-size: .6rem; font-weight: 600; text-transform: uppercase; letter-spacing: .1em; color: #71717a; border: 1px solid rgba(255,255,255,.1); border-radius: 99px; padding: 2px 7px; }
.nav-links  { display: flex; gap: 2px; flex: 1; }
.nav-links a { color: #71717a; text-decoration: none; font-size: .875rem; font-weight: 500; padding: 5px 13px; border-radius: 99px; transition: color .2s, background .2s; }
.nav-links a:hover { color: #f0f0f4; background: rgba(255,255,255,.05); }
.nav-ctas { display: flex; align-items: center; gap: 8px; margin-left: auto; }
.btn-ghost { background: transparent; border: 1px solid rgba(255,255,255,.1); color: #a1a1aa; font-size: .875rem; font-weight: 500; padding: 7px 18px; border-radius: 8px; cursor: pointer; transition: border-color .2s, color .2s; font-family: inherit; }
.btn-ghost:hover { border-color: rgba(255,255,255,.22); color: #f0f0f4; }
.btn-solid { display: inline-flex; align-items: center; gap: 6px; background: #fff; color: #09090b; border: none; font-size: .875rem; font-weight: 650; padding: 7px 20px; border-radius: 8px; cursor: pointer; transition: background .2s, transform .15s; font-family: inherit; }
.btn-solid:hover { background: #e4e4e7; transform: translateY(-1px); }

/* ══ HERO — Longer, Softly Glowing Headline ══ */
.hero {
  position: relative;
  min-height: 100svh;
  display: flex; align-items: center; justify-content: center;
  padding: 120px 28px 80px;
  text-align: center;
  overflow: hidden;
}

.hero-bg-grid {
  position: absolute; inset: 0; z-index: 0; pointer-events: none;
  background-image:
    linear-gradient(rgba(255,255,255,.018) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,.018) 1px, transparent 1px);
  background-size: 80px 80px;
  mask-image: linear-gradient(180deg, transparent 0%, #000 12%, #000 72%, transparent 100%);
}
.hero-glow  { position: absolute; border-radius: 50%; pointer-events: none; z-index: 0; filter: blur(110px); }
.glow-1 { width: 750px; height: 750px; top: -200px; left: -200px; background: radial-gradient(circle,rgba(37,99,235,.22) 0%,transparent 70%);  animation: gf 9s ease-in-out infinite; }
.glow-2 { width: 650px; height: 650px; top:  100px; right: -160px; background: radial-gradient(circle,rgba(124,58,237,.18) 0%,transparent 70%); animation: gf 11s ease-in-out infinite reverse; }
.glow-3 { width: 550px; height: 550px; bottom: 0;  left:   25%;   background: radial-gradient(circle,rgba(5,150,105,.12) 0%,transparent 70%);  animation: gf 13s ease-in-out infinite; animation-delay:-4s; }
@keyframes gf { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-30px)} }

.hero-center {
  position: relative; z-index: 1;
  max-width: 900px; width: 100%;
  opacity: 0; transform: translateY(28px);
  transition: opacity .85s cubic-bezier(.22,1,.36,1), transform .85s cubic-bezier(.22,1,.36,1);
}
.hero-center.in { opacity: 1; transform: translateY(0); }

/* Pill */
.hero-pill {
  display: inline-flex; align-items: center; gap: 9px;
  border: 1px solid rgba(255,255,255,.09); background: rgba(255,255,255,.04);
  backdrop-filter: blur(10px); border-radius: 99px;
  padding: 7px 18px 7px 10px; font-size: .82rem; color: #a1a1aa;
  margin-bottom: 32px; cursor: default; transition: border-color .3s;
}
.hero-pill:hover { border-color: rgba(255,255,255,.18); }
.hero-pill svg   { color: #52525b; }
.pill-dot { width: 7px; height: 7px; border-radius: 50%; background: #22c55e; box-shadow: 0 0 8px #22c55e88; animation: blink 2.4s ease infinite; }
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:.3} }

/* ── Hero Headline: Long, Softly-lit text with unequal line lengths ── */
.hero-h1 {
  margin-bottom: 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  line-height: 1.12;
}

/* Line 1: Shorter line, softly muted white (淡淡半透明显示) */
.h1-line-top {
  font-size: clamp(2.4rem, 5.5vw, 3.8rem);
  font-weight: 750;
  letter-spacing: -.03em;
  color: rgba(255, 255, 255, 0.48);  /* 淡淡半透明显示 */
  white-space: nowrap;
}

/* Line 2: Longer line, soft gradient glow (淡淡高质感渐变) */
.h1-line-bot {
  font-size: clamp(2.8rem, 6.8vw, 4.8rem);
  font-weight: 850;
  letter-spacing: -.04em;
  background: linear-gradient(135deg, rgba(255,255,255,0.92) 0%, rgba(147,197,253,0.85) 50%, rgba(192,132,252,0.85) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
}

.hero-sub { font-size: 1.05rem; line-height: 1.75; color: #71717a; margin: 0 auto 38px; max-width: 580px; }

.hero-actions { display: flex; gap: 14px; justify-content: center; margin-bottom: 38px; flex-wrap: wrap; }
.cta-primary  { display: inline-flex; align-items: center; gap: 9px; background: #fff; color: #09090b; border: none; font-size: 1rem; font-weight: 700; padding: 14px 32px; border-radius: 99px; cursor: pointer; font-family: inherit; transition: background .2s, transform .2s, box-shadow .2s; }
.cta-primary:hover { background: #e4e4e7; transform: translateY(-2px); box-shadow: 0 12px 36px rgba(255,255,255,.08); }
.cta-primary:hover svg { transform: translateX(3px); }
.cta-primary svg { transition: transform .2s; }
.cta-outline  { display: inline-flex; align-items: center; background: transparent; border: 1px solid rgba(255,255,255,.12); color: #a1a1aa; font-size: 1rem; font-weight: 500; padding: 14px 32px; border-radius: 99px; cursor: pointer; font-family: inherit; transition: border-color .2s, color .2s, background .2s; }
.cta-outline:hover { border-color: rgba(255,255,255,.24); color: #d4d4d8; background: rgba(255,255,255,.03); }

.hero-proof { display: flex; align-items: center; justify-content: center; gap: 12px; font-size: .82rem; color: #52525b; margin-bottom: 32px; }
.proof-avatars { display: flex; }
.proof-avatars span { width: 28px; height: 28px; border-radius: 50%; border: 2px solid #08080c; display: block; margin-left: -7px; }
.proof-avatars span:first-child { margin-left: 0; }
.hero-proof strong { color: #a1a1aa; font-weight: 600; }

.hero-stats { display: flex; align-items: center; justify-content: center; gap: 24px; padding: 16px 28px; border: 1px solid rgba(255,255,255,.07); background: rgba(255,255,255,.02); border-radius: 14px; backdrop-filter: blur(8px); flex-wrap: wrap; }
.stat { text-align: center; }
.stat strong { display: block; font-size: 1.35rem; font-weight: 800; color: #f0f0f4; letter-spacing: -.04em; }
.stat span   { font-size: .72rem; color: #52525b; font-weight: 500; margin-top: 2px; display: block; }
.stat-div    { width: 1px; height: 32px; background: rgba(255,255,255,.08); }

/* ══ SECTIONS ══ */
.section { padding: 88px 28px; }
.section-alt { background: rgba(255,255,255,.013); }
.section-wrap { max-width: 100%; margin: 0 auto; }
.s-head { text-align: center; margin-bottom: 48px; opacity: 0; transform: translateY(20px); transition: opacity .65s ease, transform .65s ease; }
.s-head.in { opacity: 1; transform: translateY(0); }
.eyebrow { display: inline-flex; align-items: center; gap: 8px; font-size: .68rem; font-weight: 600; text-transform: uppercase; letter-spacing: .2em; color: #3f3f46; border: 1px solid rgba(255,255,255,.06); background: rgba(255,255,255,.025); padding: 5px 14px; border-radius: 99px; margin-bottom: 20px; }
.ew-dot  { width: 6px; height: 6px; border-radius: 50%; background: #22c55e; }
.s-head h2 { font-size: clamp(1.65rem, 3.4vw, 2.5rem); font-weight: 750; color: #f0f0f4; letter-spacing: -.03em; line-height: 1.2; margin-bottom: 14px; }
.s-head p  { font-size: .95rem; color: #52525b; max-width: 500px; margin: 0 auto; line-height: 1.75; }

/* ══ FEATURES ══ */
.feat-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 14px; opacity: 0; transform: translateY(18px); transition: opacity .65s ease .1s, transform .65s ease .1s; }
.feat-grid.in { opacity: 1; transform: translateY(0); }
.feat-card { background: rgba(14,14,20,.75); border: 1px solid rgba(255,255,255,.055); border-radius: 16px; padding: 22px; display: flex; gap: 16px; align-items: flex-start; position: relative; overflow: hidden; transition: border-color .25s, transform .25s, box-shadow .25s; animation: fcIn .5s ease calc(var(--delay,0s)) both; }
@keyframes fcIn { from{opacity:0;transform:translateY(12px)} to{opacity:1;transform:none} }
.feat-card:hover { border-color: rgba(255,255,255,.1); transform: translateY(-3px); box-shadow: 0 16px 48px rgba(0,0,0,.28); }
.fc-icon { width: 42px; height: 42px; border-radius: 11px; flex-shrink: 0; background: color-mix(in srgb,var(--c) 10%,transparent); border: 1px solid color-mix(in srgb,var(--c) 20%,transparent); display: flex; align-items: center; justify-content: center; color: var(--c); }
.fc-icon svg { width: 19px; height: 19px; }
.fc-body h3 { font-size: .9rem; font-weight: 650; color: #e2e2e6; margin-bottom: 7px; }
.fc-body p  { font-size: .8rem; color: #52525b; line-height: 1.65; margin-bottom: 12px; }
.fc-chips { display: flex; gap: 5px; flex-wrap: wrap; }
.fc-chips span { font-size: .65rem; font-weight: 500; padding: 2px 8px; border-radius: 99px; color: var(--c); border: 1px solid color-mix(in srgb,var(--c) 20%,transparent); background: color-mix(in srgb,var(--c) 7%,transparent); }

/* ══ WORKFLOW — FULL PAGE UNBOUNDED CAROUSEL (NO BORDER BOX) ══ */
.workflow-section-full {
  padding: 88px 0;
  background: rgba(255,255,255,.013);
  overflow: hidden;
}

.full-carousel-viewport {
  position: relative;
  width: 100%;
  margin-top: 40px;
}

.full-carousel-track {
  display: flex;
  width: 100%;
  transition: transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

.full-slide-item {
  min-width: 100vw;
  width: 100vw;
  flex-shrink: 0;
  display: grid;
  grid-template-columns: 55% 45%;
  align-items: center;
  padding: 0 max(28px, calc((100vw - 1160px) / 2));
  box-sizing: border-box;
}

/* Left Side: Unbounded Page Image */
.full-slide-media {
  padding-right: 48px;
}
.media-container {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 30px 100px rgba(0, 0, 0, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.07);
}
.media-container img {
  width: 100%;
  height: 100%;
  max-height: 480px;
  object-fit: cover;
  object-position: top left;
  display: block;
}
.media-fade-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to right, transparent 75%, rgba(8, 8, 12, 0.95) 100%);
  pointer-events: none;
}
.media-step-badge {
  position: absolute;
  top: 20px; left: 20px;
  font-family: 'Geist Mono', monospace;
  font-size: .78rem; font-weight: 600;
  color: rgba(255,255,255,.45);
  background: rgba(0,0,0,.6);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,.1);
  border-radius: 8px; padding: 6px 12px;
}

/* Right Side: Staggered Entrance Animations for Info Copy */
.full-slide-info {
  display: flex;
  align-items: center;
  padding-left: 12px;
}
.info-content-box {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 460px;
}

/* Entrance Animation Base States */
.full-slide-item .info-meta,
.full-slide-item .info-title,
.full-slide-item .info-desc,
.full-slide-item .info-hl-row,
.full-slide-item .info-tags {
  opacity: 0;
  transform: translateY(22px);
  transition: opacity 0.55s cubic-bezier(0.22, 1, 0.36, 1), transform 0.55s cubic-bezier(0.22, 1, 0.36, 1);
}

/* Slide Active Entrance Staggered Delays */
.full-slide-item.active .info-meta {
  opacity: 1; transform: translateY(0); transition-delay: 0.12s;
}
.full-slide-item.active .info-title {
  opacity: 1; transform: translateY(0); transition-delay: 0.20s;
}
.full-slide-item.active .info-desc {
  opacity: 1; transform: translateY(0); transition-delay: 0.28s;
}
.full-slide-item.active .info-hl-row:nth-child(1) {
  opacity: 1; transform: translateY(0); transition-delay: 0.36s;
}
.full-slide-item.active .info-hl-row:nth-child(2) {
  opacity: 1; transform: translateY(0); transition-delay: 0.44s;
}
.full-slide-item.active .info-hl-row:nth-child(3) {
  opacity: 1; transform: translateY(0); transition-delay: 0.52s;
}
.full-slide-item.active .info-tags {
  opacity: 1; transform: translateY(0); transition-delay: 0.60s;
}

/* Text & Highlight Styling */
.info-meta {
  display: flex; align-items: center; gap: 8px;
  font-family: 'Geist Mono', monospace; font-size: .78rem;
}
.info-counter { color: #60a5fa; font-weight: 700; background: rgba(59,130,246,.12); padding: 3px 10px; border-radius: 99px; }
.info-label   { color: #52525b; text-transform: uppercase; letter-spacing: .1em; font-size: .7rem; }

.info-title { font-size: 1.85rem; font-weight: 750; color: #f0f0f4; letter-spacing: -.03em; line-height: 1.25; }
.info-desc  { font-size: .92rem; color: #80808a; line-height: 1.7; }

/* Detailed Highlights Bullet List */
.info-highlights-list {
  display: flex;
  flex-direction: column;
  gap: 11px;
  margin: 6px 0;
  background: rgba(255,255,255,0.02);
  border-left: 3px solid #3b82f6;
  padding: 14px 18px;
  border-radius: 0 12px 12px 0;
}
.info-hl-row { display: flex; align-items: flex-start; gap: 9px; font-size: .86rem; color: #d4d4d8; line-height: 1.5; }
.info-hl-icon { color: #60a5fa; font-size: .8rem; margin-top: 1px; flex-shrink: 0; }
.info-hl-text :deep(b) { color: #93c5fd; font-weight: 600; background: rgba(59,130,246,0.12); padding: 1px 6px; border-radius: 4px; }

.info-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.info-tag-item { font-size: .72rem; font-weight: 500; padding: 4px 12px; border-radius: 99px; color: #a1a1aa; border: 1px solid rgba(255,255,255,.08); background: rgba(255,255,255,.025); }

/* Floating Carousel Control Bar */
.full-carousel-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-top: 48px;
}
.fc-arrow-btn {
  width: 42px; height: 42px; border-radius: 99px;
  background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.1);
  color: #a1a1aa; display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: background .2s, color .2s; font-family: inherit;
}
.fc-arrow-btn:hover { background: rgba(255,255,255,.12); color: #f0f0f4; }
.fc-dots-wrap { display: flex; gap: 8px; }
.fc-dot-item { width: 7px; height: 7px; border-radius: 99px; background: rgba(255,255,255,.14); cursor: pointer; transition: background .25s, width .3s cubic-bezier(.22,1,.36,1); }
.fc-dot-item.active { background: #3b82f6; width: 28px; }
.fc-progress-indicator { width: 34px; height: 34px; position: relative; }
.fc-progress-indicator svg { width: 100%; height: 100%; }
.fc-progress-indicator circle:last-child { transition: stroke-dashoffset .08s linear; }

/* ══ WHY CHOOSE US ══ */
.why-claims { display: grid; grid-template-columns: repeat(3,1fr); gap: 16px; margin-bottom: 40px; opacity: 0; transform: translateY(18px); transition: opacity .65s ease .1s, transform .65s ease .1s; }
.why-claims.in { opacity: 1; transform: translateY(0); }
.why-card { display: flex; flex-direction: column; gap: 12px; background: rgba(14,14,20,.7); border: 1px solid rgba(255,255,255,.06); border-radius: 16px; padding: 22px; transition: border-color .25s, transform .25s; }
.why-card:hover { border-color: rgba(255,255,255,.1); transform: translateY(-3px); }
.wc-icon { width: 42px; height: 42px; border-radius: 11px; background: color-mix(in srgb,var(--c) 10%,transparent); border: 1px solid color-mix(in srgb,var(--c) 20%,transparent); display: flex; align-items: center; justify-content: center; color: var(--c); }
.wc-icon svg { width: 19px; height: 19px; }
.wc-title { font-size: .93rem; font-weight: 650; color: #e2e2e6; }
.wc-desc  { font-size: .8rem; color: #52525b; line-height: 1.68; }

.why-banner { display: flex; gap: 48px; align-items: center; background: linear-gradient(135deg,rgba(37,99,235,.1),rgba(124,58,237,.08)); border: 1px solid rgba(59,130,246,.18); border-radius: 20px; padding: 40px 48px; opacity: 0; transform: translateY(16px); transition: opacity .65s ease .2s, transform .65s ease .2s; }
.why-banner.in { opacity: 1; transform: translateY(0); }
.wb-left { flex: 1; }
.wb-q    { font-size: 5rem; font-weight: 800; line-height: .6; color: rgba(96,165,250,.25); margin-bottom: 10px; font-family: Georgia, serif; }
.wb-text { font-size: 1.22rem; font-weight: 650; color: #e2e2e6; line-height: 1.55; letter-spacing: -.02em; margin-bottom: 14px; }
.wb-by   { font-size: .82rem; color: #52525b; }
.wb-right { display: flex; flex-direction: column; gap: 14px; flex-shrink: 0; }
.wb-badge { display: flex; align-items: center; gap: 10px; font-size: .85rem; font-weight: 500; color: #a1a1aa; }
.wb-badge svg { width: 16px; height: 16px; color: var(--c); flex-shrink: 0; }

/* ══ COMPARISON ══ */
.cmp-table { border: 1px solid rgba(255,255,255,.07); border-radius: 18px; overflow: hidden; opacity: 0; transform: translateY(18px); transition: opacity .65s ease .1s, transform .65s ease .1s; }
.cmp-table.in { opacity: 1; transform: translateY(0); }
.cmp-header { display: grid; grid-template-columns: 1fr 1.4fr 1.4fr; padding: 13px 24px; background: rgba(255,255,255,.022); border-bottom: 1px solid rgba(255,255,255,.055); font-size: .7rem; font-weight: 600; text-transform: uppercase; letter-spacing: .12em; align-items: center; color: #3f3f46; }
.cmp-bad-h  { display: flex; align-items: center; gap: 6px; color: #f87171; }
.cmp-good-h { display: flex; align-items: center; gap: 6px; color: #4ade80; }
.cmp-row { display: grid; grid-template-columns: 1fr 1.4fr 1.4fr; padding: 16px 24px; border-bottom: 1px solid rgba(255,255,255,.038); align-items: start; transition: background .2s; }
.cmp-row:last-child { border-bottom: none; }
.cmp-row:hover { background: rgba(255,255,255,.014); }
.cmp-row b { font-size: .87rem; font-weight: 600; color: #d4d4d8; }
.cmp-bad, .cmp-good { display: flex; gap: 8px; font-size: .82rem; line-height: 1.55; align-items: flex-start; padding-right: 12px; }
.cmp-bad  { color: #9ca3af; }
.cmp-good { color: #86efac; }
.ci { width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-top: 1px; }
.ci.bad  { background: rgba(239,68,68,.12);  color: #f87171; border: 1px solid rgba(239,68,68,.22); }
.ci.good { background: rgba(34,197,94,.1);   color: #4ade80; border: 1px solid rgba(34,197,94,.22); }

/* ══ PRICING ══ */
.price-grid { display: grid; grid-template-columns: repeat(auto-fit,minmax(230px,1fr)); gap: 14px; opacity: 0; transform: translateY(18px); transition: opacity .65s ease .1s, transform .65s ease .1s; }
.price-grid.in { opacity: 1; transform: translateY(0); }
.price-card { background: rgba(14,14,20,.8); border: 1px solid rgba(255,255,255,.07); border-radius: 20px; padding: 26px 22px; position: relative; transition: border-color .25s, transform .25s; }
.price-card:hover { border-color: rgba(255,255,255,.12); transform: translateY(-3px); }
.price-card.featured { border-color: rgba(59,130,246,.35); box-shadow: 0 0 0 1px rgba(59,130,246,.18), 0 18px 60px rgba(59,130,246,.1); }
.price-badge { position: absolute; top: -11px; left: 50%; transform: translateX(-50%); background: linear-gradient(120deg,#2563eb,#7c3aed); color: #fff; font-size: .68rem; font-weight: 700; padding: 3px 13px; border-radius: 99px; white-space: nowrap; }
.price-type   { font-size: .65rem; font-weight: 600; text-transform: uppercase; letter-spacing: .12em; color: #3f3f46; margin-bottom: 10px; }
.price-name   { font-size: 1.15rem; font-weight: 700; color: #f0f0f4; margin-bottom: 14px; }
.price-amount { display: flex; align-items: baseline; gap: 5px; margin-bottom: 6px; }
.price-amount strong { font-size: 2rem; font-weight: 800; color: #f0f0f4; letter-spacing: -.04em; }
.price-amount small  { font-size: .78rem; color: #52525b; }
.price-list { list-style: none; display: flex; flex-direction: column; gap: 9px; margin: 14px 0 22px; }
.price-list li { display: flex; align-items: flex-start; gap: 8px; font-size: .82rem; color: #71717a; line-height: 1.4; }
.price-btn { width: 100%; background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.1); color: #d4d4d8; font-size: .875rem; font-weight: 600; padding: 11px; border-radius: 10px; cursor: pointer; font-family: inherit; transition: background .2s, color .2s; }
.price-btn:hover { background: rgba(255,255,255,.09); color: #fff; }
.feat-btn { background: linear-gradient(120deg,#2563eb,#7c3aed); border-color: transparent; color: #fff; box-shadow: 0 4px 20px rgba(59,130,246,.28); }
.feat-btn:hover { background: linear-gradient(120deg,#3b82f6,#8b5cf6); }

/* ══ FOOTER ══ */
.home-footer { border-top: 1px solid rgba(255,255,255,.05); padding: 44px 28px; }
.footer-inner { max-width: 100%; margin: 0 auto; padding: 0 48px; display: flex; flex-direction: column; align-items: center; gap: 20px; text-align: center; }
.footer-logo  { font-size: 1rem; font-weight: 700; color: #f0f0f4; }
.footer-links { display: flex; gap: 24px; flex-wrap: wrap; justify-content: center; }
.footer-links a { font-size: .82rem; color: #52525b; text-decoration: none; transition: color .2s; }
.footer-links a:hover { color: #a1a1aa; }
.footer-copy  { font-size: .75rem; color: #27272a; }

/* ══ MODAL ══ */
.mfade-enter-active,.mfade-leave-active{transition:opacity .22s ease}
.mfade-enter-from,.mfade-leave-to{opacity:0}
.modal-mask { position: fixed; inset: 0; z-index: 500; background: rgba(0,0,0,.72); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: center; padding: 24px; }
.modal-box  { background: rgba(12,12,18,.96); border: 1px solid rgba(255,255,255,.1); border-radius: 20px; padding: 36px; width: 100%; max-width: 410px; position: relative; box-shadow: 0 48px 120px rgba(0,0,0,.65); }
.modal-x    { position: absolute; top: 14px; right: 14px; width: 30px; height: 30px; background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.08); border-radius: 7px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #71717a; transition: color .2s, background .2s; font-family: inherit; }
.modal-x:hover { color: #e2e2e6; background: rgba(255,255,255,.08); }
.auth-label { font-size: .62rem; font-weight: 700; letter-spacing: .18em; text-transform: uppercase; color: #2563eb; margin-bottom: 8px; }
.auth-title { font-size: 1.45rem; font-weight: 750; color: #f0f0f4; margin-bottom: 26px; letter-spacing: -.025em; }
.auth-form  { display: flex; flex-direction: column; gap: 12px; }
.scroll-form { max-height: 62vh; overflow-y: auto; padding-right: 4px; }
.scroll-form::-webkit-scrollbar { width: 3px; }
.scroll-form::-webkit-scrollbar-thumb { background: rgba(255,255,255,.1); border-radius: 99px; }
.auth-form label { font-size: .78rem; font-weight: 600; color: #71717a; margin-bottom: -4px; }
.auth-form input { background: rgba(255,255,255,.04); border: 1px solid rgba(255,255,255,.08); border-radius: 10px; color: #f0f0f4; font-size: .9rem; padding: 11px 14px; width: 100%; font-family: inherit; outline: none; transition: border-color .2s, background .2s; }
.auth-form input:focus { border-color: rgba(59,130,246,.5); background: rgba(59,130,246,.04); }
.auth-form input::placeholder { color: #3f3f46; }
.pw-wrap { position: relative; }
.pw-wrap input { padding-right: 42px; }
.pw-eye { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); background: transparent; border: none; color: #52525b; cursor: pointer; padding: 0; display: flex; align-items: center; transition: color .2s; }
.pw-eye:hover { color: #a1a1aa; }
.row-input { display: flex; gap: 8px; }
.row-input input { flex: 1; min-width: 0; }
.code-btn { background: rgba(59,130,246,.1); border: 1px solid rgba(59,130,246,.22); color: #60a5fa; font-size: .76rem; font-weight: 600; padding: 0 13px; border-radius: 99px; cursor: pointer; white-space: nowrap; font-family: inherit; transition: background .2s; }
.code-btn:hover:not(:disabled) { background: rgba(59,130,246,.18); }
.code-btn:disabled { opacity: .45; cursor: not-allowed; }
.opt { color: #3f3f46; font-weight: 400; }
.role-group { display: flex; background: rgba(255,255,255,.03); border: 1px solid rgba(255,255,255,.07); border-radius: 10px; padding: 3px; gap: 3px; }
.role-group button { flex: 1; background: transparent; border: none; color: #52525b; font-size: .82rem; font-weight: 500; padding: 7px; border-radius: 7px; cursor: pointer; font-family: inherit; transition: all .2s; }
.role-group button.on { background: rgba(255,255,255,.07); color: #f0f0f4; }
.auth-err { font-size: .8rem; color: #f87171; background: rgba(239,68,68,.08); border: 1px solid rgba(239,68,68,.14); padding: 9px 13px; border-radius: 8px; }
.auth-ok  { font-size: .8rem; color: #86efac; background: rgba(34,197,94,.07); border: 1px solid rgba(34,197,94,.14); padding: 9px 13px; border-radius: 8px; }
.auth-submit { background: #fff; color: #09090b; border: none; font-size: .93rem; font-weight: 700; padding: 13px; border-radius: 10px; cursor: pointer; font-family: inherit; margin-top: 2px; transition: background .2s, transform .15s; }
.auth-submit:hover:not(:disabled) { background: #e4e4e7; transform: translateY(-1px); }
.auth-submit:disabled { opacity: .45; cursor: not-allowed; }
.auth-links { display: flex; justify-content: space-between; }
.auth-links a { font-size: .78rem; color: #71717a; text-decoration: none; transition: color .2s; }
.auth-links a:hover { color: #a1a1aa; }
.auth-links a.dim { color: #3f3f46; }

/* ══ RESPONSIVE ══ */
@media (max-width: 1000px) {
  .nav-links { display: none; }
  .feat-grid { grid-template-columns: repeat(2,1fr); }
  .why-claims { grid-template-columns: repeat(2,1fr); }
  .why-banner { flex-direction: column; gap: 28px; padding: 28px; }
  .full-slide-item { grid-template-columns: 1fr; padding: 0 24px; }
  .full-slide-media { padding-right: 0; margin-bottom: 24px; }
  .full-slide-info { padding-left: 0; }
  .info-title { font-size: 1.5rem; }
}
@media (max-width: 640px) {
  .feat-grid { grid-template-columns: 1fr; }
  .why-claims { grid-template-columns: 1fr; }
  .price-grid { grid-template-columns: 1fr; }
  .cmp-header { grid-template-columns: 1fr; }
  .cmp-header span:not(:first-child) { display: none; }
  .cmp-row { grid-template-columns: 1fr; gap: 8px; }
  .hero-stats { gap: 16px; }
  .stat-div { display: none; }
  .h1-line-top { font-size: clamp(1.8rem, 7vw, 2.6rem); }
  .h1-line-bot { font-size: clamp(2.1rem, 8.5vw, 3.2rem); }
}
</style>
