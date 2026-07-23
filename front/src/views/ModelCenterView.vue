<template>
  <main class="membership-page">
    <!-- Ambient atmosphere orbs -->
    <div class="ambient-orb orb-1"></div>
    <div class="ambient-orb orb-2"></div>

    <!-- ── Page Header ──────────────────────────────────────── -->
    <header class="membership-topbar">
      <div>
        <div class="topbar-badge-row">
          <span class="page-chip">用量与重置中心</span>
          <span class="reset-tag">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/><path d="M16 16h5v5"/></svg>
            订阅周期到期自动复位
          </span>
        </div>
        <h1>科研用量按任务智能结算，周期自动重置透明可控。</h1>
        <p>基础文献导入与翻译全量开放；论文综述、组会 PPT、AI 对话按套餐用量扣减，按订阅周期自动全额重置。</p>
      </div>
      <button class="ghost-button refresh-button" :disabled="loading" @click="load">
        <span aria-hidden="true" :class="{ spinning: loading }">↻</span>
        {{ loading ? "更新中" : "刷新用量" }}
      </button>
    </header>

    <!-- ── Current Usage & Member Card Banner ─────────────────── -->
    <section class="current-strip-linear">
      <!-- 💳 Horizontal Membership Status Banner -->
      <div class="horizontal-membership-bar">
        <div class="vip-status-main">
          <div class="vip-badge-glow">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M5 16L3 5l5.5 5L12 4l3.5 6L21 5l-2 11H5zm14 3c0 .6-.4 1-1 1H6c-.6 0-1-.4-1-1v-1h14v1z"/></svg>
            <strong>{{ membership.active ? memberPeriodLabel : "体验会员" }}</strong>
          </div>
          <div class="vip-details">
            <span class="vip-name">{{ membership.name }}</span>
            <span class="vip-expire">{{ membership.active ? `有效期至 ${formatFullDate(membership.expiresAt)}` : "当前未开通高级套餐" }}</span>
          </div>
        </div>

        <div class="vip-metrics">
          <div class="metric-item">
            <small>剩余可用</small>
            <strong>{{ remainingDays }} <span class="unit">天</span></strong>
          </div>
          <div class="metric-divider"></div>
          <div class="metric-item">
            <small>订阅状态</small>
            <strong class="status-active">{{ membership.active ? "🟢 正常使用中" : "⚪ 未订阅" }}</strong>
          </div>
          <div class="metric-divider"></div>
          <div class="metric-item">
            <small>自动重置</small>
            <strong>到期自动复位</strong>
          </div>
        </div>

        <button type="button" class="upgrade-vip-btn" @click="scrollToPlans">
          套餐方案与开通 →
        </button>
      </div>

      <!-- 📏 Full-Width Linear Rows Dashboard -->
      <div class="entitlement-linear-panel">
        <div class="panel-header">
          <div>
            <span class="panel-tag">当期用量仪表盘</span>
            <h3>当期权益指标与重置进度</h3>
          </div>
          <span class="reset-cycle-pill">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/><path d="M16 16h5v5"/></svg>
            到期自动复位
          </span>
        </div>

        <div class="entitlement-linear-rows">
          <div v-for="item in benefitItems" :key="item.key" class="linear-row">
            <div class="row-left">
              <span class="homepage-icon-box" :class="benefitBoxClass(item.key)">
                <svg v-if="item.key === 'translation'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
                <svg v-else-if="item.key === 'review'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/><line x1="9" y1="7" x2="15" y2="7"/><line x1="9" y1="11" x2="13" y2="11"/></svg>
                <svg v-else-if="item.key === 'ppt'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="3"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/><path d="M7 12l3-3 3 3 4-4"/></svg>
                <svg v-else-if="item.key === 'chat'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
              </span>
              <div class="row-title-block">
                <strong>{{ item.label }}</strong>
                <small v-if="!item.unlimited">当期额度 {{ item.quota }} 次</small>
                <small v-else>全量开放免扣减</small>
              </div>
            </div>

            <div class="row-center-meter">
              <div v-if="!item.unlimited" class="linear-meter-track">
                <b :style="{ width: `${quotaPercent(item)}%` }"></b>
              </div>
              <span v-else class="linear-unlimited-label">✓ 不限次数</span>
            </div>

            <div class="row-right">
              <span class="row-stat-text">{{ benefitUsageLabel(item) }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ── Plan Workbench ──────────────────────────────────────── -->
    <section class="plan-workbench">
      <div class="plan-heading">
        <div>
          <h2>套餐开通与重置</h2>
          <p>用量按成功完成后扣减，每个重置周期自动恢复当期额度。PPT 生成包含完整组会 Agent 流程。</p>
        </div>
        <div class="cycle-switch" role="tablist" aria-label="套餐周期">
          <button v-for="option in cycles" :key="option.id" :class="{ active: selectedCycle === option.id }" @click="selectedCycle = option.id">
            {{ option.label }}
            <small v-if="option.badge">{{ option.badge }}</small>
          </button>
        </div>
      </div>

      <div class="plan-groups">
        <section v-for="group in planGroups" :key="group.key" class="plan-group">
          <div class="plan-group-title">
            <span>{{ group.label }}</span>
            <p>{{ group.description }}</p>
          </div>
          <div class="plan-cards" :class="`plan-cards-${group.key}`">
            <article
              v-for="plan in group.plans"
              :key="plan.id"
              class="plan-card"
              :class="[plan.id, { active: selectedPlan === plan.id }]"
              @click="selectedPlan = plan.id"
            >
              <header>
                <div>
                  <div class="plan-title-row">
                    <h3>{{ planCardTitle(plan.id) }}</h3>
                    <em>{{ planBadgeLabel(plan.id) }}</em>
                  </div>
                  <p>{{ planCopy(plan.id) }}</p>
                </div>
                <span class="plan-icon-badge">{{ planBadge(plan.id) }}</span>
              </header>

              <div class="price-line">
                <strong>¥{{ planPrice(plan) }}</strong>
                <span>/ {{ cycleLabel(selectedCycle) }}</span>
              </div>

              <ul class="center-plan-features">
                <li v-for="row in planRows(plan)" :key="row.label" :class="{ excluded: !row.included }">
                  <span class="feature-check" :class="{ excluded: !row.included }">{{ row.included ? "✓" : "×" }}</span>
                  <div>
                    <strong>{{ row.label }}：{{ row.value }}</strong>
                    <small>{{ row.description }}</small>
                  </div>
                </li>
              </ul>
              <p class="settlement-note">每个重置周期用量独立计算，次月重置或续费后自动充沛额度。</p>

              <button class="plan-buy-button" @click.stop="selectAndCheckout(plan.id)">
                开通该套餐
              </button>
            </article>
          </div>
        </section>
      </div>
    </section>

    <!-- ── Floating Checkout Bar ───────────────────────────────── -->
    <section class="checkout-bar">
      <div>
        <span>本次开通</span>
        <strong>{{ selectedPlanInfo.name }} · {{ cycleLabel(selectedCycle) }}</strong>
        <p>{{ checkoutDescription }}</p>
      </div>
      <div class="checkout-actions">
        <div class="pay-methods">
          <button :class="{ active: provider === 'alipay' }" @click="provider = 'alipay'"><i>支</i>支付宝</button>
          <button :class="{ active: provider === 'wechat' }" @click="provider = 'wechat'"><i>微</i>微信支付</button>
        </div>
        <button class="primary-button" :disabled="paying" @click="checkout">
          {{ paying ? "正在创建订单..." : `¥${planPrice(selectedPlanInfo)} 去支付` }}
        </button>
      </div>
      <p v-if="paymentMessage" class="payment-message">{{ paymentMessage }}</p>
    </section>

    <!-- ── Orders Section ──────────────────────────────────────── -->
    <section class="orders-section">
      <div class="section-title">
        <div>
          <span>订单信息</span>
          <h2>套餐订单与售后</h2>
        </div>
        <button class="ghost-button" :disabled="ordersLoading" @click="loadOrders">{{ ordersLoading ? "刷新中" : "刷新订单" }}</button>
      </div>

      <div v-if="orders.length" class="orders-table">
        <div class="order-head"><span>订单号</span><span>开通套餐</span><span>订单金额</span><span>状态</span><span>售后操作</span></div>
        <div v-for="order in orders" :key="order.orderNo" class="order-item">
          <div><strong>{{ order.orderNo }}</strong><small>{{ formatDate(order.createdAt) }}</small></div>
          <span>{{ orderPlanName(order) }}</span>
          <strong>¥{{ Number(order.amount || 0).toFixed(2) }}</strong>
          <span class="status" :class="order.status">{{ statusLabel(order.status) }}</span>
          <button class="ticket-button" @click="openTicket(order)">申请售后</button>
        </div>
      </div>
      <div v-else class="orders-empty">还没有套餐订单。开通后这里会显示支付状态、工单和退款进度。</div>
    </section>

    <!-- Ticket Dialog -->
    <dialog ref="ticketDialog" class="ticket-dialog">
      <form method="dialog" @submit.prevent="submitTicket">
        <div class="dialog-heading">
          <div><span>售后工单</span><h2>{{ ticket.orderNo }}</h2></div>
          <button class="close-button" value="cancel" aria-label="关闭">×</button>
        </div>
        <label>工单类型<select v-model="ticket.type"><option value="support">支付与开通问题</option><option value="refund">退款申请</option></select></label>
        <label>问题标题<input v-model.trim="ticket.subject" placeholder="例如：支付后会员未生效" /></label>
        <label>具体说明<textarea v-model.trim="ticket.detail" rows="5" placeholder="请写明订单、发生时间、问题现象和希望处理方式。"></textarea></label>
        <p v-if="ticketError" class="ticket-error">{{ ticketError }}</p>
        <button class="primary-button" :disabled="ticketSubmitting">{{ ticketSubmitting ? "提交中..." : "提交工单" }}</button>
      </form>
    </dialog>
  </main>
</template>

<script setup>
import { useScrollReveal } from "../composables/useScrollReveal";
useScrollReveal(".model-center-page");
import { computed, onMounted, ref } from "vue";
import { useUsageStore } from "../stores/usage";
import { paperpilotApi } from "../services/paperpilotApi";
import goldCardReference from "../assets/membership/gold-card-cropped.jpg";

const usageStore = useUsageStore();
const loading = ref(false);
const paying = ref(false);
const ordersLoading = ref(false);
const provider = ref("alipay");
const selectedCycle = ref("monthly");
const selectedPlan = ref("plus");
const orders = ref([]);
const paymentMessage = ref("");
const ticketDialog = ref(null);
const ticketSubmitting = ref(false);
const ticketError = ref("");
const ticket = ref({ orderNo: "", type: "support", subject: "", detail: "" });

const cycles = [
  { id: "monthly", label: "月付" },
  { id: "quarterly", label: "季付", badge: "9 折" },
  { id: "yearly", label: "年付", badge: "75 折" },
];

const defaultPlans = [
  { id: "lite", name: "个人 Lite", monthlyPrice: 9.9, reviewQuota: 3, pptQuota: 0, chatQuota: 60, teamSeats: 0, teamShared: false },
  { id: "plus", name: "个人 Plus", monthlyPrice: 19.9, reviewQuota: 10, pptQuota: 1, chatQuota: 180, teamSeats: 0, teamShared: false },
  { id: "pro", name: "个人 Pro", monthlyPrice: 39.9, reviewQuota: 25, pptQuota: 4, chatQuota: 500, teamSeats: 0, teamShared: false },
  { id: "max", name: "个人 Max", monthlyPrice: 69.9, reviewQuota: 60, pptQuota: 10, chatQuota: 1200, teamSeats: 0, teamShared: false },
  { id: "team_plus", name: "团队 Plus", monthlyPrice: 129, reviewQuota: 120, pptQuota: 16, chatQuota: 2600, teamSeats: 8, teamShared: true },
  { id: "team_pro", name: "团队 Pro", monthlyPrice: 229, reviewQuota: 260, pptQuota: 36, chatQuota: 6000, teamSeats: 15, teamShared: true },
];

const plans = computed(() => usageStore.state.plans || []);
const planOrder = ["lite", "plus", "pro", "max", "team_plus", "team_pro"];
const displayPlans = computed(() => {
  const byId = new Map(defaultPlans.map((plan) => [plan.id, plan]));
  return planOrder.map((id) => byId.get(id)).filter(Boolean);
});
const planGroups = computed(() => [
  {
    key: "personal",
    label: "个人套餐",
    description: "适合个人论文阅读、综述、问答与组会 PPT。",
    plans: displayPlans.value.filter((item) => ["lite", "plus", "pro", "max"].includes(item.id)),
  },
  {
    key: "team",
    label: "团队套餐",
    description: "导师开通，全队共享额度与团队席位。",
    plans: displayPlans.value.filter((item) => ["team_plus", "team_pro"].includes(item.id)),
  },
]);
const membership = computed(() => usageStore.state.membership || { id: "free", name: "未开通会员", benefits: {} });
const selectedPlanInfo = computed(() => displayPlans.value.find((item) => item.id === selectedPlan.value) || displayPlans.value[0] || { name: "研读会员", monthlyPrice: 19.9, reviewQuota: 10, pptQuota: 2, chatQuota: 80 });
const remainingDays = computed(() => {
  if (!membership.value.active || !membership.value.expiresAt) return 0;
  const expires = parseDateValue(membership.value.expiresAt);
  if (!expires) return 0;
  const diff = expires.getTime() - Date.now();
  return Math.max(0, Math.ceil(diff / 86400000));
});
const memberPeriodLabel = computed(() => {
  const cycle = membership.value.cycle || "monthly";
  if (cycle === "yearly") return "年卡365天";
  if (cycle === "quarterly") return "季卡90天";
  return "月卡30天";
});
function benefitIcon(key) {
  return {
    translation: "🌐",
    review: "📄",
    ppt: "📊",
    chat: "💬",
    team_seats: "👥"
  }[key] || "✨";
}

function benefitBoxClass(key) {
  return {
    translation: "box-blue",
    review: "box-purple",
    ppt: "box-emerald",
    chat: "box-amber",
    team_seats: "box-blue"
  }[key] || "box-purple";
}

const benefitItems = computed(() => {
  const benefits = membership.value.benefits || {};
  const row = (key, label) => ({ key, label, ...(benefits[key] || { quota: 0, used: 0, remaining: 0 }) });
  return [
    { key: "translation", label: "导入/翻译", unlimited: true },
    row("review", "论文综述"),
    row("ppt", "组会 PPT"),
    row("chat", "AI 对话"),
    { key: "teamSeats", label: "团队席位", ...(benefits.teamSeats || { quota: 8, shared: false }) },
  ];
});
const checkoutDescription = computed(() => [
  `论文综述 ${selectedPlanInfo.value.reviewQuota || 0} 次`,
  `组会 PPT ${selectedPlanInfo.value.pptQuota || 0} 次`,
  `AI 文章对话 ${selectedPlanInfo.value.chatQuota || 0} 次`,
  selectedPlanInfo.value.teamShared ? `团队共享 ${selectedPlanInfo.value.teamSeats || 20} 席` : "",
].filter(Boolean).join(" · "));

onMounted(() => {
  load();
  loadOrders();
});

async function load() {
  loading.value = true;
  try {
    await usageStore.fetchSummary();
    selectedPlan.value = normalizePlanId(selectedPlan.value);
    if (!displayPlans.value.some((item) => item.id === selectedPlan.value)) selectedPlan.value = displayPlans.value[1]?.id || displayPlans.value[0]?.id || "plus";
  } finally {
    loading.value = false;
  }
}

async function loadOrders() {
  ordersLoading.value = true;
  try {
    orders.value = (await paperpilotApi.getPaymentOrders()).orders || [];
  } finally {
    ordersLoading.value = false;
  }
}

function planPrice(plan) {
  const factor = selectedCycle.value === "quarterly" ? 2.7 : selectedCycle.value === "yearly" ? 9 : 1;
  return (Number(plan.monthlyPrice || 0) * factor).toFixed(2);
}

function quotaPercent(item) {
  return item.quota ? Math.max(0, Math.min(100, (item.remaining / item.quota) * 100)) : 0;
}

function benefitRemaining(item) {
  const quota = Number(item.quota || 0);
  const remaining = Number(item.remaining);
  if (Number.isFinite(remaining)) return Math.max(0, remaining);
  return Math.max(0, quota - Number(item.used || 0));
}

function benefitUsageLabel(item) {
  if (item.unlimited) return "不限次";
  const unit = item.key === "teamSeats" ? "席" : "次";
  return `${benefitRemaining(item)} / ${Number(item.quota || 0)} ${unit}`;
}

function quotaTone(item) {
  if (item.unlimited) return "quota-unlimited";
  const percent = quotaPercent({ ...item, remaining: benefitRemaining(item) });
  if (percent >= 60) return "quota-good";
  if (percent >= 25) return "quota-mid";
  return "quota-low";
}

function cycleLabel(cycle) {
  return ({ monthly: "月度会员", quarterly: "季度会员", yearly: "年度会员" })[cycle] || "月度会员";
}

function cycleShortLabel(cycle) {
  return ({ monthly: "月", quarterly: "季", yearly: "年" })[cycle] || "月";
}

function planBadge(id) {
  return ({ lite: "L", plus: "P+", pro: "P", max: "M", team_plus: "T+", team_pro: "TP" })[normalizePlanId(id)] || "M";
}

function planBadgeLabel(id) {
  return ({ lite: "入门", plus: "热销", pro: "进阶", max: "满配", team_plus: "团队Plus", team_pro: "团队Pro" })[normalizePlanId(id)] || "套餐";
}

function planCardTitle(id) {
  return ({ lite: "个人 Lite", plus: "个人 Plus", pro: "个人 Pro", max: "个人 Max", team_plus: "团队 Plus", team_pro: "团队 Pro" })[normalizePlanId(id)] || "会员套餐";
}

function planCopy(id) {
  id = normalizePlanId(id);
  return ({
    lite: "适合个人轻量阅读、基础问答和偶尔生成综述。",
    plus: "适合课程论文、周会准备和稳定的论文问答。",
    pro: "适合课题高频推进，包含更多 PPT 与论坛特权。",
    max: "个人满配额度，适合密集综述、问答和组会输出。",
    team_plus: "导师一人开通，全队共享 8 个席位与组会生成权益。",
    team_pro: "面向更大的实验室团队，15 席并拥有最高次数与论坛特权。",
  })[id] || "按任务次数使用。";
}

function planRows(plan) {
  const id = normalizePlanId(plan.id);
  const premiumForum = ["pro", "max", "team_pro"].includes(id);
  const forumIdentity = premiumForum ? "彩色姓名 + 发帖波浪" : "未含";
  const hasTeamSeats = Number(plan.teamSeats || 0) > 0;
  return [
    { label: "论文导入与基础翻译", description: "文献入库、PDF 管理、基础翻译", value: "不限次", included: true },
    { label: "论文综述生成", description: "规范分点综述，可保存复用", value: `${plan.reviewQuota || 0} 次`, included: Number(plan.reviewQuota || 0) > 0 },
    { label: "组会 PPT 生成", description: "PPT Master Agent 重任务流程", value: Number(plan.pptQuota || 0) > 0 ? `${plan.pptQuota} 次` : "未包含", included: Number(plan.pptQuota || 0) > 0 },
    { label: "AI 文章对话", description: "围绕论文内容连续追问", value: `${plan.chatQuota || 0} 次`, included: Number(plan.chatQuota || 0) > 0 },
    { label: "论坛身份与发帖特效", description: "彩色姓名、发帖列表波浪等社区权益", value: forumIdentity, included: premiumForum },
    { label: "团队共享席位", description: hasTeamSeats ? "导师开通，全队共享权益" : "个人套餐不开放扩展席位", value: hasTeamSeats ? `${plan.teamSeats} 席` : "未开放", included: hasTeamSeats },
  ];
}

async function selectAndCheckout(planId) {
  selectedPlan.value = normalizePlanId(planId);
  await checkout();
}

function normalizePlanId(id) {
  return ({ light: "lite", study: "plus", lab: "pro", team: "team_plus" })[id] || id || "free";
}

function formatDate(value) {
  if (!value) return "-";
  if (Array.isArray(value)) return `${value[0]}-${String(value[1]).padStart(2, "0")}-${String(value[2]).padStart(2, "0")}`;
  return String(value).replace("T", " ").slice(0, 16);
}

function formatFullDate(value) {
  const date = parseDateValue(value);
  if (!date) return formatDate(value);
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function parseDateValue(value) {
  if (!value) return null;
  if (Array.isArray(value)) {
    const [year, month, day, hour = 23, minute = 59, second = 59] = value;
    const date = new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute), Number(second));
    return Number.isNaN(date.getTime()) ? null : date;
  }
  const date = new Date(String(value).replace(" ", "T"));
  return Number.isNaN(date.getTime()) ? null : date;
}

function scrollToPlans() {
  document.querySelector(".plan-workbench")?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function orderPlanName(order) {
  return displayPlans.value.find((item) => item.id === normalizePlanId(order.planId))?.name || (order.planId === "custom-recharge" ? "历史余额订单" : "会员套餐");
}

function statusLabel(status) {
  return ({ config_required: "待支付配置", pending_payment: "待支付", paid: "已生效", created: "已创建", failed: "支付失败" })[status] || "处理中";
}

async function checkout() {
  paying.value = true;
  paymentMessage.value = "";
  try {
    const order = await paperpilotApi.createPaymentOrder({ planId: selectedPlan.value, planCycle: selectedCycle.value, provider: provider.value });
    paymentMessage.value = order.message || "订单已创建。";
    if (order.paymentUrl) window.open(order.paymentUrl, "_blank", "noopener,noreferrer");
    await loadOrders();
  } catch (error) {
    paymentMessage.value = error?.response?.data?.message || "创建订单失败，请稍后重试。";
  } finally {
    paying.value = false;
  }
}

function openTicket(order) {
  ticket.value = { orderNo: order.orderNo, type: "support", subject: "", detail: "" };
  ticketError.value = "";
  ticketDialog.value?.showModal();
}

async function submitTicket() {
  if (ticket.value.detail.length < 6) {
    ticketError.value = "请把遇到的情况写具体一些。";
    return;
  }
  ticketSubmitting.value = true;
  try {
    await paperpilotApi.createPaymentTicket(ticket.value);
    ticketDialog.value?.close();
    paymentMessage.value = "售后工单已提交，管理员处理后会同步更新。";
  } catch (error) {
    ticketError.value = error?.response?.data?.message || "提交失败，请稍后重试。";
  } finally {
    ticketSubmitting.value = false;
  }
}
</script>

<style scoped>
/* ════════════════════════════════════════════════════════════
   USAGE & RESET CENTER — Clean Horizontal & Linear Design
   ════════════════════════════════════════════════════════════ */

.membership-page {
  --c-bg:       #f8fafc;
  --c-surface:  #ffffff;
  --c-border:   #e2e8f0;
  --c-text:     #0f172a;
  --c-muted:    #475569;
  --c-subtle:   #94a3b8;
  --c-accent:   #6366f1;
  --c-accent2:  #a855f7;
  --r: 20px; --r-sm: 12px; --r-pill: 999px;
  --sh-sm: 0 2px 10px rgba(15,23,42,.04), 0 8px 24px rgba(15,23,42,.03);
  --sh-md: 0 10px 32px rgba(15,23,42,.08);
  --sh-lg: 0 20px 60px rgba(15,23,42,.14);

  position: relative;
  min-height: 100vh;
  background: var(--c-bg);
  color: var(--c-text);
  padding: 36px clamp(16px, 4vw, 56px) 130px;
  font-family: Inter, "PingFang SC", system-ui, sans-serif;
  transition: background 0.3s ease, color 0.3s ease;
  width: 100%;
  box-sizing: border-box;
  margin: 0 auto;
}

:root[data-theme="dark"] .membership-page {
  --c-bg:       #09090e;
  --c-surface:  #111827;
  --c-border:   rgba(255, 255, 255, 0.08);
  --c-text:     #f1f5f9;
  --c-muted:    #94a3b8;
  --c-subtle:   #64748b;
  --sh-sm: 0 2px 10px rgba(0,0,0,.3), 0 8px 24px rgba(0,0,0,.25);
  --sh-md: 0 10px 32px rgba(0,0,0,.45);
  --sh-lg: 0 20px 60px rgba(0,0,0,.65);
}

/* Ambient Orbs */
.ambient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
  opacity: 0.22;
}
.orb-1 { top: -100px; left: 10%; width: 400px; height: 400px; background: radial-gradient(circle, #818cf8, #c084fc); }
.orb-2 { top: 200px; right: 5%; width: 500px; height: 500px; background: radial-gradient(circle, #38bdf8, #818cf8); }

/* ── Topbar ─────────────────────────────────────────────── */
.membership-topbar {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}

.topbar-badge-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.page-chip {
  padding: 4px 14px;
  border-radius: var(--r-pill);
  background: rgba(99, 102, 241, 0.1);
  color: var(--c-accent);
  font-size: 12px;
  font-weight: 800;
  border: 1px solid rgba(99, 102, 241, 0.2);
}
:root[data-theme="dark"] .page-chip {
  background: rgba(99, 102, 241, 0.18);
  color: #818cf8;
}

.reset-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: var(--r-pill);
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  font-size: 12px;
  font-weight: 800;
  border: 1px solid rgba(16, 185, 129, 0.25);
}

.membership-topbar h1 {
  margin: 0 0 8px;
  font-size: clamp(22px, 2.5vw, 30px);
  font-weight: 900;
  color: var(--c-text);
  line-height: 1.25;
  letter-spacing: -0.5px;
}

.membership-topbar p {
  margin: 0;
  font-size: 14px;
  color: var(--c-muted);
  max-width: 760px;
  line-height: 1.6;
}

.ghost-button {
  height: 42px;
  padding: 0 20px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-surface);
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 750;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: var(--sh-sm);
  white-space: nowrap;
}
.ghost-button:hover {
  border-color: var(--c-accent);
  color: var(--c-accent);
  transform: translateY(-1px);
}
.spinning { display: inline-block; animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }

/* ── Current Strip (Horizontal Banner + Linear Rows) ────── */
.current-strip-linear {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 44px;
}

/* 💳 Horizontal Membership Status Banner (Subtle Glass Bar) */
.horizontal-membership-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 18px 28px;
  border-radius: var(--r);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: var(--sh-sm);
}
@media (max-width: 900px) {
  .horizontal-membership-bar { flex-direction: column; align-items: flex-start; }
}

.vip-status-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.vip-badge-glow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--r-pill);
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.18), rgba(245, 158, 11, 0.12));
  border: 1px solid rgba(251, 191, 36, 0.35);
  color: #d97706;
  font-size: 13.5px;
  font-weight: 850;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.15);
}
:root[data-theme="dark"] .vip-badge-glow {
  color: #fbbf24;
}

.vip-details {
  display: flex;
  flex-direction: column;
}
.vip-name {
  font-size: 16px;
  font-weight: 900;
  color: var(--c-text);
}
.vip-expire {
  font-size: 12px;
  color: var(--c-muted);
}

.vip-metrics {
  display: flex;
  align-items: center;
  gap: 20px;
}
.metric-item {
  display: flex;
  flex-direction: column;
}
.metric-item small {
  font-size: 11px;
  color: var(--c-subtle);
}
.metric-item strong {
  font-size: 16px;
  font-weight: 900;
  color: var(--c-text);
}
.metric-item strong .unit {
  font-size: 12px;
  font-weight: 700;
  color: var(--c-muted);
}
.status-active { color: #10b981 !important; }

.metric-divider {
  width: 1px;
  height: 28px;
  background: var(--c-border);
}

.upgrade-vip-btn {
  height: 38px;
  padding: 0 20px;
  border-radius: var(--r-pill);
  border: none;
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #ffffff;
  font-size: 13px;
  font-weight: 850;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
  transition: all 0.2s ease;
  white-space: nowrap;
}
.upgrade-vip-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

/* 📏 Full-Width Linear Rows Panel (No Box Tiles!) */
.entitlement-linear-panel {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--r);
  padding: 24px 30px;
  box-shadow: var(--sh-sm);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--c-border);
}
.panel-tag {
  display: block;
  font-size: 10.5px;
  font-weight: 900;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--c-accent);
  margin-bottom: 2px;
}
.panel-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: var(--c-text);
}

.reset-cycle-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: var(--r-pill);
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  font-size: 12px;
  font-weight: 800;
  border: 1px solid rgba(16, 185, 129, 0.25);
}

/* Linear Row List */
.entitlement-linear-rows {
  display: flex;
  flex-direction: column;
}

.linear-row {
  display: grid;
  grid-template-columns: 240px 1fr 140px;
  align-items: center;
  gap: 24px;
  padding: 16px 12px;
  border-bottom: 1px solid var(--c-border);
  transition: background 0.18s ease;
}
.linear-row:last-child {
  border-bottom: none;
}
.linear-row:hover {
  background: rgba(99, 102, 241, 0.035);
  border-radius: var(--r-sm);
}
@media (max-width: 768px) {
  .linear-row { grid-template-columns: 1fr; gap: 10px; }
}

.row-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.row-title-block strong {
  display: block;
  font-size: 14.5px;
  font-weight: 850;
  color: var(--c-text);
}
.row-title-block small {
  font-size: 11.5px;
  color: var(--c-muted);
}

.row-center-meter {
  width: 100%;
}

.linear-meter-track {
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  overflow: hidden;
}
.linear-meter-track b {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #6366f1, #a855f7);
  transition: width 0.4s ease;
}

.linear-unlimited-label {
  font-size: 12px;
  color: #10b981;
  font-weight: 800;
}

.row-right {
  text-align: right;
}
.row-stat-text {
  font-size: 15px;
  font-weight: 950;
  color: var(--c-accent);
  font-variant-numeric: tabular-nums;
}

/* 🎨 Homepage Micro-Glow Icon Box */
.homepage-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.homepage-icon-box.box-blue {
  background: rgba(37, 99, 235, 0.08);
  border: 1.5px solid rgba(37, 99, 235, 0.3);
  color: #2563eb;
}
:root[data-theme="dark"] .homepage-icon-box.box-blue {
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.4);
  color: #60a5fa;
}

.homepage-icon-box.box-purple {
  background: rgba(147, 51, 234, 0.08);
  border: 1.5px solid rgba(147, 51, 234, 0.3);
  color: #9333ea;
}
:root[data-theme="dark"] .homepage-icon-box.box-purple {
  background: rgba(168, 85, 247, 0.12);
  border-color: rgba(168, 85, 247, 0.4);
  color: #c084fc;
}

.homepage-icon-box.box-emerald {
  background: rgba(16, 185, 129, 0.08);
  border: 1.5px solid rgba(16, 185, 129, 0.3);
  color: #10b981;
}
:root[data-theme="dark"] .homepage-icon-box.box-emerald {
  background: rgba(16, 185, 129, 0.12);
  border-color: rgba(16, 185, 129, 0.4);
  color: #34d399;
}

.homepage-icon-box.box-amber {
  background: rgba(245, 158, 11, 0.08);
  border: 1.5px solid rgba(245, 158, 11, 0.3);
  color: #d97706;
}
:root[data-theme="dark"] .homepage-icon-box.box-amber {
  background: rgba(245, 158, 11, 0.12);
  border-color: rgba(245, 158, 11, 0.4);
  color: #fbbf24;
}

.linear-row:hover .homepage-icon-box {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

/* ── Plan Workbench ──────────────────────────────────────── */
.plan-workbench {
  position: relative;
  z-index: 2;
  margin-bottom: 44px;
}

.plan-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}
.plan-heading h2 {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 900;
  color: var(--c-text);
}
.plan-heading p {
  margin: 0;
  font-size: 13.5px;
  color: var(--c-muted);
}

/* Cycle Switch Pill */
.cycle-switch {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border-radius: var(--r-pill);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: var(--sh-sm);
}
.cycle-switch button {
  height: 36px;
  padding: 0 18px;
  border-radius: var(--r-pill);
  border: none;
  background: transparent;
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.cycle-switch button.active {
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
}
.cycle-switch button small {
  padding: 1px 6px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  font-size: 10px;
  font-weight: 900;
}
.cycle-switch button.active small {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

/* Plan Groups & Cards Grid */
.plan-groups {
  display: flex;
  flex-direction: column;
  gap: 36px;
}
.plan-group-title {
  margin-bottom: 18px;
}
.plan-group-title span {
  font-size: 17px;
  font-weight: 900;
  color: var(--c-text);
  display: block;
}
.plan-group-title p {
  margin: 3px 0 0;
  font-size: 13px;
  color: var(--c-muted);
}

.plan-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.plan-cards-team {
  grid-template-columns: repeat(2, 1fr);
}
@media (max-width: 1120px) {
  .plan-cards { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .plan-cards, .plan-cards-team { grid-template-columns: 1fr; }
}

.plan-card {
  position: relative;
  border-radius: var(--r);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: var(--sh-sm);
  padding: 26px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.plan-card:hover {
  transform: translateY(-5px);
  border-color: rgba(99, 102, 241, 0.35);
  box-shadow: var(--sh-md);
}
.plan-card.active {
  border-color: var(--c-accent) !important;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.18), var(--sh-md) !important;
  background: rgba(99, 102, 241, 0.03) !important;
}

.plan-card header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}
.plan-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.plan-title-row h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: var(--c-text);
}
.plan-title-row em {
  font-style: normal;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.1);
  color: var(--c-accent);
  font-size: 10.5px;
  font-weight: 800;
}
.plan-card p {
  margin: 0;
  font-size: 12px;
  color: var(--c-muted);
  line-height: 1.55;
}
.plan-icon-badge {
  font-size: 20px;
}

.price-line {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--c-border);
}
.price-line strong {
  font-size: 32px;
  font-weight: 950;
  color: var(--c-text);
  font-family: tabular-nums;
  line-height: 1;
}
.price-line span {
  font-size: 12px;
  color: var(--c-muted);
  font-weight: 700;
}

/* Feature list */
.center-plan-features {
  list-style: none;
  padding: 0;
  margin: 0 0 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.center-plan-features li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.feature-check {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
  font-size: 11px;
  font-weight: 900;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  margin-top: 1px;
}
.feature-check.excluded {
  background: rgba(148, 163, 184, 0.12);
  color: var(--c-subtle);
}
.center-plan-features li div strong {
  display: block;
  font-size: 13px;
  font-weight: 800;
  color: var(--c-text);
}
.center-plan-features li div small {
  display: block;
  font-size: 11.5px;
  color: var(--c-muted);
}

.settlement-note {
  font-size: 11px;
  color: var(--c-subtle);
  margin-bottom: 16px;
  line-height: 1.5;
}

.plan-buy-button {
  width: 100%;
  height: 42px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-text);
  font-size: 13.5px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
}
.plan-card:hover .plan-buy-button,
.plan-card.active .plan-buy-button {
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.38);
}

/* ── Checkout Bar ────────────────────────────────────────── */
.checkout-bar {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  width: min(1020px, calc(100vw - 32px));
  padding: 16px 26px;
  border-radius: var(--r);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: var(--sh-lg);
  backdrop-filter: blur(24px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}
@media (max-width: 700px) {
  .checkout-bar { flex-direction: column; align-items: stretch; }
}

.checkout-bar > div:first-child span {
  font-size: 11px;
  font-weight: 800;
  color: var(--c-subtle);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.checkout-bar > div:first-child strong {
  display: block;
  font-size: 16px;
  font-weight: 900;
  color: var(--c-text);
}
.checkout-bar > div:first-child p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--c-muted);
}

.checkout-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.pay-methods {
  display: flex;
  gap: 6px;
}
.pay-methods button {
  height: 38px;
  padding: 0 16px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.18s;
}
.pay-methods button.active {
  border-color: var(--c-accent);
  color: var(--c-accent);
  background: rgba(99, 102, 241, 0.08);
}
.pay-methods button i {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(99, 102, 241, 0.15);
  font-style: normal;
  font-size: 10px;
  font-weight: 900;
  display: grid;
  place-items: center;
}

.primary-button {
  height: 44px;
  padding: 0 26px;
  border-radius: var(--r-pill);
  border: none;
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #ffffff;
  font-size: 14px;
  font-weight: 850;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.35);
  transition: all 0.2s ease;
  white-space: nowrap;
}
.primary-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.45);
}
.primary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* ── Orders Section ──────────────────────────────────────── */
.orders-section {
  position: relative;
  z-index: 2;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--r);
  padding: 26px 30px;
  box-shadow: var(--sh-sm);
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.section-title span {
  font-size: 11px;
  font-weight: 800;
  color: var(--c-subtle);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.section-title h2 {
  margin: 2px 0 0;
  font-size: 18px;
  font-weight: 900;
  color: var(--c-text);
}

.orders-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.order-head {
  display: grid;
  grid-template-columns: 1.5fr 1.2fr 1fr 1fr 1fr;
  padding: 8px 16px;
  font-size: 12px;
  font-weight: 750;
  color: var(--c-subtle);
  border-bottom: 1px solid var(--c-border);
}
.order-item {
  display: grid;
  grid-template-columns: 1.5fr 1.2fr 1fr 1fr 1fr;
  align-items: center;
  padding: 12px 16px;
  border-radius: var(--r-sm);
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  font-size: 13px;
  color: var(--c-text);
}
.order-item strong { font-weight: 800; }
.order-item small { color: var(--c-muted); display: block; font-size: 11px; }

.status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: var(--r-pill);
  font-size: 11px;
  font-weight: 800;
  width: fit-content;
}
.status.paid, .status.success { background: rgba(16, 185, 129, 0.12); color: #10b981; }
.status.pending { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
.status.failed, .status.cancelled { background: rgba(239, 68, 68, 0.12); color: #ef4444; }

.ticket-button {
  height: 30px;
  padding: 0 12px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: transparent;
  color: var(--c-muted);
  font-size: 12px;
  font-weight: 750;
  cursor: pointer;
  transition: all 0.18s;
  width: fit-content;
}
.ticket-button:hover { border-color: var(--c-accent); color: var(--c-accent); }

.orders-empty {
  padding: 36px;
  text-align: center;
  color: var(--c-subtle);
  font-size: 13px;
  background: var(--c-bg);
  border-radius: var(--r-sm);
  border: 1px dashed var(--c-border);
}

/* ── Ticket Dialog ───────────────────────────────────────── */
.ticket-dialog {
  border: 1px solid var(--c-border);
  border-radius: var(--r);
  background: var(--c-surface);
  color: var(--c-text);
  box-shadow: var(--sh-lg);
  padding: 28px 32px;
  width: min(480px, calc(100vw - 32px));
}
.ticket-dialog::backdrop {
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(6px);
}
.dialog-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}
.dialog-heading span { font-size: 11px; font-weight: 800; color: var(--c-accent); text-transform: uppercase; }
.dialog-heading h2 { margin: 2px 0 0; font-size: 18px; font-weight: 900; }
.close-button { border: none; background: transparent; color: var(--c-muted); font-size: 20px; cursor: pointer; }

.ticket-dialog label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
  font-size: 12.5px;
  font-weight: 750;
  color: var(--c-muted);
}
.ticket-dialog input, .ticket-dialog select, .ticket-dialog textarea {
  padding: 10px 14px;
  border-radius: var(--r-sm);
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-text);
  font-size: 13.5px;
  outline: none;
}
.ticket-dialog input:focus, .ticket-dialog select:focus, .ticket-dialog textarea:focus {
  border-color: var(--c-accent);
}
</style>