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
                <svg v-if="item.key === 'import'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                <svg v-else-if="item.key === 'translation'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
                <svg v-else-if="item.key === 'immersive'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>
                <svg v-else-if="item.key === 'review'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/><line x1="9" y1="7" x2="15" y2="7"/><line x1="9" y1="11" x2="13" y2="11"/></svg>
                <svg v-else-if="item.key === 'chat'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                <svg v-else-if="item.key === 'ppt'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="3"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/><path d="M7 12l3-3 3 3 4-4"/></svg>
                <svg v-else-if="item.key === 'forumBadge'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                <svg v-else-if="item.key === 'forumTop'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/></svg>
                <svg v-else-if="item.key === 'peakPriority'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
              </span>
              <div class="row-title-block">
                <strong>{{ item.label }}</strong>
                <small v-if="item.unlimited">全量开放免扣减</small>
                <small v-else-if="item.isFeature">{{ item.included ? '已开通特权' : '当期未包含' }}</small>
                <small v-else-if="item.isTeam && !item.quota">个人套餐未开通</small>
                <small v-else>当期额度 {{ item.quota }} {{ item.unit }}</small>
              </div>
            </div>

            <div class="row-center-meter">
              <div v-if="!item.unlimited && !item.isFeature && item.quota > 0" class="linear-meter-track">
                <b :style="{ width: `${quotaPercent(item)}%` }"></b>
              </div>
              <span v-else-if="item.unlimited" class="linear-unlimited-label">✓ 不限次数</span>
              <span v-else-if="item.isFeature" class="linear-feature-badge" :class="{ active: item.included }">
                {{ item.included ? '✓ ' + (item.value || '包含') : '× 未包含' }}
              </span>
              <span v-else-if="item.isTeam && !item.quota" class="linear-disabled-label">未开放</span>
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

              <div v-if="isSeckillActive(plan)" class="plan-sale-strip">
                <span>{{ plan.seckillLabel || "限时秒杀" }}</span>
                <strong>{{ formatSeckillCountdown(plan) }}</strong>
              </div>
              <div class="price-line" :class="{ sale: isSeckillActive(plan) }">
                <span v-if="isSeckillActive(plan)" class="origin-price">¥{{ originalPlanPrice(plan) }}</span>
                <strong>¥{{ planPrice(plan) }}</strong>
                <span>/ {{ cycleLabel(selectedCycle) }}{{ plan.teamShared ? ` (${teamMemberCount}人总计)` : '' }}</span>
                <span v-if="plan.id === 'lite'" class="luckin-tag">
                  <img :src="luckinLogo" alt="瑞幸" class="luckin-icon" />
                  相当于一个月一杯瑞幸咖啡～
                </span>
              </div>

              <div v-if="plan.teamShared" class="team-seats-selector" @click.stop>
                <span class="seats-label">团队人数</span>
                <div class="seats-counter">
                  <button type="button" class="counter-btn" :disabled="teamMemberCount <= 2" @click="teamMemberCount = Math.max(2, teamMemberCount - 1)">-</button>
                  <div class="counter-value-box">
                    <input
                      type="text"
                      inputmode="numeric"
                      pattern="[0-9]*"
                      :value="teamMemberCount"
                      @input="onTeamCountInput"
                      class="counter-input"
                    />
                    <span class="counter-unit">人</span>
                  </div>
                  <button type="button" class="counter-btn" :disabled="teamMemberCount >= 100" @click="teamMemberCount = Math.min(100, teamMemberCount + 1)">+</button>
                </div>
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

              <button class="plan-buy-button" :class="{ 'free-button': plan.id === 'free' }" :disabled="plan.id === 'free'" @click.stop="selectAndCheckout(plan.id)">
                {{ plan.id === 'free' ? '免费版使用中' : '开通该套餐' }}
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
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useUsageStore } from "../stores/usage";
import { paperpilotApi } from "../services/paperpilotApi";
import goldCardReference from "../assets/membership/gold-card-cropped.jpg";
import luckinLogo from "../assets/luckin-logo.png";

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
const nowTick = ref(Date.now());
let saleTimer = null;
const ticket = ref({ orderNo: "", type: "support", subject: "", detail: "" });
const teamMemberCount = ref(5);

function onTeamCountInput(e) {
  const raw = String(e.target.value).replace(/\D/g, "");
  if (!raw) return;
  const val = parseInt(raw, 10);
  if (!isNaN(val)) {
    teamMemberCount.value = Math.min(100, Math.max(2, val));
  }
}

const cycles = [
  { id: "monthly", label: "月付" },
];

const defaultPlans = [
  { id: "free", name: "个人 Free", subtitle: "永久免费版", monthlyPrice: 0, reviewQuotaDaily: 3, pptQuotaMonthly: 0, chatQuotaDaily: 5, translateQuotaDaily: 5, immersiveQuotaDaily: 3, forumSpecial: false, forumTopDaily: 0, peakPriority: false, teamShared: false },
  { id: "lite", name: "个人 Lite", subtitle: "一杯瑞幸咖啡价", monthlyPrice: 9.9, reviewQuotaDaily: 15, pptQuotaMonthly: 2, chatQuotaDaily: 30, translateQuotaDaily: 10, immersiveQuotaDaily: 10, forumSpecial: false, forumTopDaily: 0, peakPriority: false, teamShared: false },
  { id: "plus", name: "个人 Plus", subtitle: "热销推荐", monthlyPrice: 19.9, reviewQuotaDaily: 30, pptQuotaMonthly: 4, chatQuotaDaily: 60, translateQuotaDaily: 20, immersiveQuotaDaily: 20, forumSpecial: true, forumTopDaily: 0, peakPriority: false, teamShared: false },
  { id: "pro", name: "个人 Pro", subtitle: "极速进阶", monthlyPrice: 29.9, reviewQuotaDaily: 60, pptQuotaMonthly: 6, chatQuotaDaily: 120, translateQuotaDaily: 50, immersiveQuotaDaily: 50, forumSpecial: true, forumTopDaily: 1, peakPriority: true, teamShared: false },
  { id: "team_plus", name: "课题组团队 Plus", subtitle: "导师购买分配 (9折)", monthlyPrice: 17.91, perUserPrice: 19.9, reviewQuotaDaily: 30, pptQuotaMonthly: 4, chatQuotaDaily: 60, translateQuotaDaily: 20, immersiveQuotaDaily: 20, forumSpecial: true, forumTopDaily: 0, peakPriority: true, teamShared: true, teamSeats: 10 },
  { id: "team_pro", name: "课题组团队 Pro", subtitle: "实验室旗舰 (9折)", monthlyPrice: 26.91, perUserPrice: 29.9, reviewQuotaDaily: 60, pptQuotaMonthly: 6, chatQuotaDaily: 120, translateQuotaDaily: 50, immersiveQuotaDaily: 50, forumSpecial: true, forumTopDaily: 1, peakPriority: true, teamShared: true, teamSeats: 20 },
];

const plans = computed(() => usageStore.state.plans || []);
const planOrder = ["free", "lite", "plus", "pro", "team_plus", "team_pro"];
const displayPlans = computed(() => {
  const byId = new Map(defaultPlans.map((plan) => [plan.id, plan]));
  (plans.value || []).forEach((plan) => {
    const id = normalizePlanId(plan.id);
    byId.set(id, { ...(byId.get(id) || {}), ...plan, id });
  });
  return Array.from(byId.values())
    .filter((plan) => plan && plan.activeFlag !== false)
    .sort((a, b) => {
      const ia = planOrder.includes(a.id) ? planOrder.indexOf(a.id) : Number(a.sortOrder ?? 99);
      const ib = planOrder.includes(b.id) ? planOrder.indexOf(b.id) : Number(b.sortOrder ?? 99);
      return ia === ib ? String(a.name || "").localeCompare(String(b.name || ""), "zh-CN") : ia - ib;
    });
});
const planGroups = computed(() => [
  {
    key: "personal",
    label: "个人套餐",
    description: "包含永久免费版、基础版、热销版与进阶版，满足不同阶段科研需求。",
    plans: displayPlans.value.filter((item) => !item.teamShared),
  },
  {
    key: "team",
    label: "课题组团队套餐",
    description: "导师购买按人数计费（享 9 折），包含团队分配与全员特权。",
    plans: displayPlans.value.filter((item) => item.teamShared),
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
    import: "box-blue",
    translation: "box-blue",
    immersive: "box-cyan",
    review: "box-purple",
    chat: "box-amber",
    ppt: "box-emerald",
    forumBadge: "box-purple",
    forumTop: "box-amber",
    peakPriority: "box-emerald",
    teamSeats: "box-blue",
  }[key] || "box-purple";
}

const benefitItems = computed(() => {
  const benefits = membership.value.benefits || {};
  const currentPlanId = normalizePlanId(membership.value.id || "free");
  const planInfo = displayPlans.value.find((p) => p.id === currentPlanId) || defaultPlans[0];

  return [
    { key: "import", label: "论文插件导入", unlimited: true },
    { key: "translation", label: "对照翻译", unlimited: false, quota: planInfo.translateQuotaDaily || 5, used: benefits.translation?.used || 0, unit: "篇/天" },
    { key: "immersive", label: "沉浸翻译", unlimited: false, quota: planInfo.immersiveQuotaDaily || 3, used: benefits.immersive?.used || 0, unit: "篇/天" },
    { key: "review", label: "AI 论文综述", unlimited: false, quota: planInfo.reviewQuotaDaily || 3, used: benefits.review?.used || 0, unit: "次/天" },
    { key: "chat", label: "研读对话", unlimited: false, quota: planInfo.chatQuotaDaily || 5, used: benefits.chat?.used || 0, unit: "次/天" },
    { key: "ppt", label: "组会 PPT", unlimited: false, quota: planInfo.pptQuotaMonthly || 0, used: benefits.ppt?.used || 0, unit: "次/月" },
    { key: "forumBadge", label: "论坛会员标识", isFeature: true, included: planInfo.forumSpecial },
    { key: "forumTop", label: "发帖置顶", isFeature: true, included: Number(planInfo.forumTopDaily || 0) > 0, value: planInfo.forumTopDaily ? `${planInfo.forumTopDaily} 次/天` : "未包含" },
    { key: "peakPriority", label: "高峰期优先通道", isFeature: true, included: planInfo.peakPriority, value: planInfo.peakPriority ? "优先通道" : "标准通道" },
    { key: "teamSeats", label: "团队席位", unlimited: false, quota: planInfo.teamSeats || (benefits.teamSeats?.quota || 0), used: benefits.teamSeats?.used || 0, unit: "席", isTeam: true },
  ];
});

const checkoutDescription = computed(() => {
  const plan = selectedPlanInfo.value;
  const count = plan.teamShared ? teamMemberCount.value : 0;
  return [
    `对照 ${plan.translateQuotaDaily || 10} 篇/天`,
    `沉浸 ${plan.immersiveQuotaDaily || 10} 篇/天`,
    `综述 ${plan.reviewQuotaDaily || 15} 次/天`,
    `PPT ${plan.pptQuotaMonthly || 2} 次/月`,
    plan.teamShared ? `团队 ${count} 人席位` : "",
  ].filter(Boolean).join(" · ");
});

onMounted(() => {
  saleTimer = window.setInterval(() => {
    nowTick.value = Date.now();
  }, 1000);
  load();
  loadOrders();
});

onBeforeUnmount(() => {
  if (saleTimer) window.clearInterval(saleTimer);
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
  const monthly = isSeckillActive(plan) ? Number(plan.seckillPrice ?? plan.effectiveMonthlyPrice ?? plan.monthlyPrice ?? 0) : Number(plan.effectiveMonthlyPrice ?? plan.monthlyPrice ?? 0);
  return totalPlanPrice(monthly, plan);
}

function originalPlanPrice(plan) {
  const monthly = Math.max(Number(plan.originalMonthlyPrice || 0), Number(plan.monthlyPrice || 0), Number(plan.effectiveMonthlyPrice || 0));
  return totalPlanPrice(monthly, plan);
}

function totalPlanPrice(monthly, plan) {
  const factor = selectedCycle.value === "quarterly" ? 2.7 : selectedCycle.value === "yearly" ? 9 : 1;
  const isTeam = plan.teamShared;
  const count = isTeam ? Math.max(1, teamMemberCount.value) : 1;
  return (Number(monthly || 0) * factor * count).toFixed(2);
}

function isSeckillActive(plan) {
  nowTick.value;
  if (!plan || !plan.seckillEnabled) return false;
  const start = parseDateValue(plan.seckillStartsAt);
  const end = parseDateValue(plan.seckillEndsAt);
  const now = Date.now();
  return Number(plan.seckillPrice) >= 0 && (!start || start.getTime() <= now) && (!end || end.getTime() > now);
}

function seckillRemainingMs(plan) {
  nowTick.value;
  const end = parseDateValue(plan?.seckillEndsAt);
  return end ? Math.max(0, end.getTime() - Date.now()) : 0;
}

function formatSeckillCountdown(plan) {
  const ms = seckillRemainingMs(plan);
  if (!ms) return "进行中";
  const total = Math.floor(ms / 1000);
  const days = Math.floor(total / 86400);
  const hours = Math.floor((total % 86400) / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const seconds = total % 60;
  if (days > 0) return `${days}天 ${String(hours).padStart(2, "0")}小时 ${String(minutes).padStart(2, "0")}分 ${String(seconds).padStart(2, "0")}秒`;
  return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function quotaPercent(item) {
  const quota = Number(item.quota || 0);
  const used = Number(item.used || 0);
  return quota ? Math.max(0, Math.min(100, (used / quota) * 100)) : 0;
}

function benefitUsageLabel(item) {
  if (item.unlimited) return "不限次";
  if (item.isFeature) return item.included ? (item.value || "包含") : "未包含";
  if (item.isTeam && !item.quota) return "未开放";
  const unit = item.unit || "次";
  const quota = Number(item.quota || 0);
  const used = Number(item.used || 0);
  return `${used} / ${quota} ${unit}`;
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
  return ({ free: "F", lite: "L", plus: "P+", pro: "P", team_plus: "T+", team_pro: "TP" })[normalizePlanId(id)] || "P";
}

function planBadgeLabel(id) {
  return ({ free: "永久免费", lite: "瑞幸咖啡价", plus: "热销推荐", pro: "极速进阶", team_plus: "团队 Plus (9折)", team_pro: "团队 Pro (9折)" })[normalizePlanId(id)] || "套餐";
}

function planCardTitle(id) {
  return ({ free: "个人 Free", lite: "个人 Lite", plus: "个人 Plus", pro: "个人 Pro", team_plus: "课题组团队 Plus", team_pro: "课题组团队 Pro" })[normalizePlanId(id)] || "会员套餐";
}

function planCopy(id) {
  id = normalizePlanId(id);
  return ({
    free: "基础科研体验，满足日常小量文献阅读与对话。",
    lite: "一杯瑞幸咖啡的价格，轻松开启智能论文阅读与基础综述。",
    plus: "适合课程论文、周会准备和日常科研高频问答。",
    pro: "面向课题高压推进，高额度并享发帖置顶与高峰优先。",
    team_plus: "导师按人数购买分配 (19.9元/人×9折)，全员特权与席位。",
    team_pro: "实验室全员极速旗舰 (29.9元/人×9折)，顶配额度与全特权。",
  })[id] || "按套餐次数与功能权益使用。";
}

function planRows(plan) {
  const id = normalizePlanId(plan.id);

  if (id === "free") {
    return [
      { label: "论文插件导入", description: "文献一键入库与管理", value: "不限次", included: true },
      { label: "对照翻译", description: "双栏对照翻译阅读", value: "每天 5 篇", included: true },
      { label: "沉浸翻译", description: "全页版式保留沉浸翻译", value: "每天 3 篇", included: true },
      { label: "AI 论文综述", description: "结构化文献综述生成", value: "每天 3 次", included: true },
      { label: "论文解析与研读对话", description: "针对论文深度问答与推演", value: "每天 5 次", included: true },
      { label: "组会 PPT 汇报制作", description: "PPT Agent 自动生成组会汇报", value: "未包含", included: false },
      { label: "论坛会员特效与标识", description: "彩色姓名与专属标识", value: "未包含", included: false },
      { label: "高峰期优先响应", description: "高并发时段优先通道", value: "否", included: false },
    ];
  }
  if (id === "lite") {
    return [
      { label: "论文插件导入", description: "文献一键入库与管理", value: "不限次", included: true },
      { label: "对照翻译", description: "双栏对照翻译阅读", value: "每天 10 篇", included: true },
      { label: "沉浸翻译", description: "全页版式保留沉浸翻译", value: "每天 10 篇", included: true },
      { label: "AI 论文综述", description: "结构化文献综述生成", value: "每天 15 次", included: true },
      { label: "论文解析与研读对话", description: "针对论文深度问答与推演", value: "每天 30 次", included: true },
      { label: "组会 PPT 汇报制作", description: "PPT Agent 自动生成组会汇报", value: "每月 2 次", included: true },
      { label: "论坛会员特效与标识", description: "彩色姓名与专属标识", value: "未包含", included: false },
      { label: "高峰期优先响应", description: "高并发时段优先通道", value: "否", included: false },
    ];
  }
  if (id === "plus") {
    return [
      { label: "论文插件导入", description: "文献一键入库与管理", value: "不限次", included: true },
      { label: "对照翻译", description: "双栏对照翻译阅读", value: "每天 20 篇", included: true },
      { label: "沉浸翻译", description: "全页版式保留沉浸翻译", value: "每天 20 篇", included: true },
      { label: "AI 论文综述", description: "结构化文献综述生成", value: "每天 30 次", included: true },
      { label: "论文解析与研读对话", description: "针对论文深度问答与推演", value: "每天 60 次", included: true },
      { label: "组会 PPT 汇报制作", description: "PPT Agent 自动生成组会汇报", value: "每月 4 次", included: true },
      { label: "论坛会员特效与标识", description: "彩色姓名与专属标识", value: "包含", included: true },
      { label: "高峰期优先响应", description: "高并发时段优先通道", value: "否", included: false },
    ];
  }
  if (id === "pro") {
    return [
      { label: "论文插件导入", description: "文献一键入库与管理", value: "不限次", included: true },
      { label: "对照翻译", description: "双栏对照翻译阅读", value: "每天 50 篇", included: true },
      { label: "沉浸翻译", description: "全页版式保留沉浸翻译", value: "每天 50 篇", included: true },
      { label: "AI 论文综述", description: "结构化文献综述生成", value: "每天 60 次", included: true },
      { label: "论文解析与研读对话", description: "针对论文深度问答与推演", value: "每天 120 次", included: true },
      { label: "组会 PPT 汇报制作", description: "PPT Agent 自动生成组会汇报", value: "每月 6 次", included: true },
      { label: "论坛会员特效与标识", description: "会员特效 + 每日1次发帖置顶", value: "包含 (每日1次置顶)", included: true },
      { label: "高峰期优先响应", description: "高并发时段优先通道", value: "优先", included: true },
    ];
  }
  if (id === "team_plus") {
    return [
      { label: "论文插件导入", description: "全员文献入库与 PDF 管理", value: "不限次", included: true },
      { label: "对照翻译", description: "全员双栏对照翻译阅读", value: "每人每天 20 篇", included: true },
      { label: "沉浸翻译", description: "全员全页版式沉浸翻译", value: "每人每天 20 篇", included: true },
      { label: "AI 论文综述", description: "全员结构化综述生成", value: "每人每天 30 次", included: true },
      { label: "论文解析与研读对话", description: "全员学术问答与推演", value: "每人每天 60 次", included: true },
      { label: "组会 PPT 汇报制作", description: "全员 PPT Agent 自动汇报", value: "每人每月 4 次", included: true },
      { label: "论坛会员特效与标识", description: "全员尊享会员标识", value: "全员包含", included: true },
      { label: "导师购买统一分配", description: "按人数结算享 9 折优惠", value: "¥17.91 / 人 / 月", included: true },
      { label: "高峰期优先响应", description: "全员享受极速优先通道", value: "优先", included: true },
    ];
  }
  return [
    { label: "论文插件导入", description: "全员文献入库与 PDF 管理", value: "不限次", included: true },
    { label: "对照翻译", description: "全员双栏对照翻译阅读", value: "每人每天 50 篇", included: true },
    { label: "沉浸翻译", description: "全员全页版式沉浸翻译", value: "每人每天 50 篇", included: true },
    { label: "AI 论文综述", description: "全员结构化综述生成", value: "每人每天 60 次", included: true },
    { label: "论文解析与研读对话", description: "全员学术问答与推演", value: "每人每天 120 次", included: true },
    { label: "组会 PPT 汇报制作", description: "全员 PPT Agent 自动汇报", value: "每人每月 6 次", included: true },
    { label: "论坛会员特效与标识", description: "全员会员标识 + 每人每日1次置顶", value: "全员包含 (每日1次置顶)", included: true },
    { label: "导师购买统一分配", description: "按人数结算享 9 折优惠", value: "¥26.91 / 人 / 月", included: true },
    { label: "高峰期优先响应", description: "全员享受极速优先通道", value: "优先", included: true },
  ];
}

async function selectAndCheckout(planId) {
  selectedPlan.value = normalizePlanId(planId);
  await checkout();
}

function normalizePlanId(id) {
  return ({ light: "lite", study: "plus", lab: "pro", team: "team_plus", max: "pro" })[id] || id || "free";
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
    const order = await paperpilotApi.createPaymentOrder({
      planId: selectedPlan.value,
      planCycle: selectedCycle.value,
      provider: provider.value,
      quantity: teamMemberCount.value,
      teamMemberCount: teamMemberCount.value
    });
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

.linear-feature-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 850;
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
}
.linear-feature-badge.active {
  background: rgba(16, 185, 129, 0.14);
  color: #10b981;
}

.linear-disabled-label {
  font-size: 12px;
  color: var(--c-subtle);
  font-weight: 700;
}

.homepage-icon-box.box-cyan {
  background: rgba(6, 182, 212, 0.08);
  border: 1.5px solid rgba(6, 182, 212, 0.3);
  color: #0891b2;
}
:root[data-theme="dark"] .homepage-icon-box.box-cyan {
  background: rgba(6, 182, 212, 0.12);
  border-color: rgba(6, 182, 212, 0.4);
  color: #22d3ee;
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
  flex-wrap: wrap;
  gap: 6px 9px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--c-border);
}

.plan-sale-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 0 0 10px;
  padding: 8px 10px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.14), rgba(245, 158, 11, 0.14));
  border: 1px solid rgba(248, 113, 113, 0.24);
  color: #b91c1c;
  font-size: 11px;
  font-weight: 900;
}

:root[data-theme="dark"] .plan-sale-strip {
  color: #fecaca;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.18), rgba(245, 158, 11, 0.16));
  border-color: rgba(251, 191, 36, 0.28);
}

.plan-sale-strip strong {
  flex-shrink: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: #ef4444;
  font-size: 10.5px;
  letter-spacing: 0;
}

:root[data-theme="dark"] .plan-sale-strip strong {
  color: #fbbf24;
}

.price-line.sale {
  align-items: baseline;
}

.price-line .origin-price {
  color: var(--c-muted);
  text-decoration: line-through;
  text-decoration-thickness: 2px;
  font-size: 13px;
  min-width: 0;
}

.price-line.sale strong {
  color: #ef4444;
}

:root[data-theme="dark"] .price-line.sale strong {
  color: #fb7185;
}

.price-line strong {
  font-size: clamp(28px, 2.5vw, 32px);
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

.team-seats-selector {
  margin: 0 0 18px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 12px;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(129, 140, 248, 0.3);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.seats-label {
  font-size: 13px;
  font-weight: 850;
  color: var(--c-text);
  white-space: nowrap;
}
.seats-counter {
  display: flex;
  align-items: center;
  gap: 8px;
}
.counter-btn {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid rgba(129, 140, 248, 0.35);
  background: rgba(99, 102, 241, 0.14);
  color: var(--c-text);
  font-size: 16px;
  font-weight: 900;
  display: grid;
  place-items: center;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
}
.counter-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.counter-btn:not(:disabled):hover {
  background: rgba(99, 102, 241, 0.28);
  border-color: rgba(129, 140, 248, 0.6);
  transform: scale(1.05);
}

.counter-value-box {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.45);
  border: 1px solid rgba(129, 140, 248, 0.35);
}
:root[data-theme="light"] .counter-value-box {
  background: #ffffff;
}

.counter-input {
  width: 36px;
  height: 24px;
  border: none;
  background: transparent;
  color: var(--c-text);
  text-align: center;
  font-size: 15px;
  font-weight: 900;
  outline: none;
  padding: 0;
  margin: 0;
  font-family: inherit;
}
.counter-unit {
  font-size: 13px;
  font-weight: 850;
  color: #818cf8;
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
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(16, 185, 129, 0.16);
  color: #10b981;
  font-size: 12px;
  font-weight: 900;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  margin-top: 1px;
}
.feature-check.excluded {
  background: rgba(239, 68, 68, 0.16);
  color: #ef4444;
}

.luckin-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  width: fit-content;
  max-width: 100%;
  flex: 0 0 100%;
  margin-top: 6px;
  padding: 3px 11px 3px 5px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 850;
  color: #a5b4fc;
  background: rgba(49, 46, 129, 0.45);
  border: 1px solid rgba(129, 140, 248, 0.35);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.18);
  white-space: normal;
}
.luckin-icon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  object-fit: cover;
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
