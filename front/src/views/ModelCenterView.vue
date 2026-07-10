<template>
  <main class="membership-page">
    <header class="membership-topbar">
      <div>
        <span class="page-chip">会员与套餐</span>
        <h1>按研究任务买套餐，次数清楚，成本可控。</h1>
        <p>基础导入与翻译保持开放；论文综述、组会 PPT、AI 文章对话按套餐次数扣减。</p>
      </div>
      <button class="ghost-button refresh-button" :disabled="loading" @click="load">
        <span aria-hidden="true" :class="{ spinning: loading }">↻</span>
        {{ loading ? "更新中" : "刷新" }}
      </button>
    </header>

    <section class="current-strip">
      <div class="member-rank">
        <span>{{ planInitial }}</span>
        <div>
          <small>{{ membership.active ? "当前会员" : "当前状态" }}</small>
          <strong>{{ membership.name }}</strong>
          <p>{{ membership.active ? `有效至 ${formatDate(membership.expiresAt)} · ${cycleLabel(membership.cycle)}` : "当前未购买套餐，仅保留免费导入、文献管理、基础翻译" }}</p>
        </div>
      </div>
      <div class="entitlement-line">
        <div v-for="item in benefitItems" :key="item.key" class="entitlement-item">
          <span>{{ item.label }}</span>
          <strong v-if="item.unlimited">不限次</strong>
          <strong v-else>{{ item.remaining }} / {{ item.quota }} 次</strong>
          <i v-if="!item.unlimited"><b :style="{ width: `${quotaPercent(item)}%` }"></b></i>
        </div>
      </div>
    </section>

    <section class="plan-workbench">
      <div class="plan-heading">
        <div>
          <h2>套餐选择</h2>
          <p>次数按成功完成后扣减。PPT 生成使用更重的组会 Agent 流程，因此额度比综述更谨慎。</p>
        </div>
        <div class="cycle-switch" role="tablist" aria-label="套餐周期">
          <button v-for="option in cycles" :key="option.id" :class="{ active: selectedCycle === option.id }" @click="selectedCycle = option.id">
            {{ option.label }}
            <small v-if="option.badge">{{ option.badge }}</small>
          </button>
        </div>
      </div>

      <div class="plan-cards">
        <article
          v-for="plan in displayPlans"
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
            <span class="plan-icon">{{ planBadge(plan.id) }}</span>
          </header>

          <div class="price-line">
            <strong>¥{{ planPrice(plan) }}</strong>
            <span>{{ cycleLabel(selectedCycle) }}</span>
          </div>

          <div class="benefit-ladder">
            <div class="ladder-head">
              <span>权益阶梯</span>
              <b>{{ selectedCycle === "monthly" ? "当期有效" : cycleLabel(selectedCycle) }}</b>
            </div>
            <div v-for="row in planRows(plan)" :key="row.label" class="ladder-row">
              <span class="row-icon">{{ row.icon }}</span>
              <strong>{{ row.label }}</strong>
              <em>{{ row.level }}</em>
              <b>{{ row.value }}</b>
            </div>
            <p class="settlement-note">未使用次数到期清零，续费后重新获得当期权益。</p>
          </div>

          <button class="plan-buy-button" @click.stop="selectAndCheckout(plan.id)">
            开通该套餐
          </button>
        </article>
      </div>
    </section>

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

    <section class="orders-section">
      <div class="section-title">
        <div>
          <span>订单信息</span>
          <h2>套餐订单与售后</h2>
        </div>
        <button class="ghost-button" :disabled="ordersLoading" @click="loadOrders">{{ ordersLoading ? "刷新中" : "刷新订单" }}</button>
      </div>

      <div v-if="orders.length" class="orders-table">
        <div class="order-head"><span>订单</span><span>套餐</span><span>金额</span><span>状态</span><span>操作</span></div>
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
import { computed, onMounted, ref } from "vue";
import { useUsageStore } from "../stores/usage";
import { paperpilotApi } from "../services/paperpilotApi";

const usageStore = useUsageStore();
const loading = ref(false);
const paying = ref(false);
const ordersLoading = ref(false);
const provider = ref("alipay");
const selectedCycle = ref("monthly");
const selectedPlan = ref("study");
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

const plans = computed(() => usageStore.state.plans || []);
const displayPlans = computed(() => plans.value.filter((item) => item.id !== "free"));
const membership = computed(() => usageStore.state.membership || { id: "free", name: "未开通会员", benefits: {} });
const selectedPlanInfo = computed(() => displayPlans.value.find((item) => item.id === selectedPlan.value) || displayPlans.value[0] || { name: "研读会员", monthlyPrice: 19.9, reviewQuota: 10, pptQuota: 2, chatQuota: 80 });
const planInitial = computed(() => ({ free: "B", light: "L", study: "R", lab: "P" })[membership.value.id] || "B");
const benefitItems = computed(() => {
  const benefits = membership.value.benefits || {};
  const row = (key, label) => ({ key, label, ...(benefits[key] || { quota: 0, used: 0, remaining: 0 }) });
  return [
    { key: "translation", label: "导入/翻译", unlimited: true },
    row("review", "论文综述"),
    row("ppt", "组会 PPT"),
    row("chat", "AI 对话"),
  ];
});
const checkoutDescription = computed(() => [
  `论文综述 ${selectedPlanInfo.value.reviewQuota || 0} 次`,
  `组会 PPT ${selectedPlanInfo.value.pptQuota || 0} 次`,
  `AI 文章对话 ${selectedPlanInfo.value.chatQuota || 0} 次`,
].join(" · "));

onMounted(() => {
  load();
  loadOrders();
});

async function load() {
  loading.value = true;
  try {
    await usageStore.fetchSummary();
    if (!displayPlans.value.some((item) => item.id === selectedPlan.value)) selectedPlan.value = displayPlans.value[0]?.id || "study";
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

function cycleLabel(cycle) {
  return ({ monthly: "月度会员", quarterly: "季度会员", yearly: "年度会员" })[cycle] || "月度会员";
}

function cycleShortLabel(cycle) {
  return ({ monthly: "月", quarterly: "季", yearly: "年" })[cycle] || "月";
}

function planBadge(id) {
  return ({ light: "L", study: "R", lab: "P" })[id] || "M";
}

function planBadgeLabel(id) {
  return ({ light: "推荐", study: "热销", lab: "超值" })[id] || "套餐";
}

function planCardTitle(id) {
  return ({ light: "轻享月卡", study: "尊享月卡", lab: "课题月卡" })[id] || "会员套餐";
}

function planCopy(id) {
  return ({
    light: "适合轻量阅读和偶尔生成综述，保留低门槛入口。",
    study: "适合每周组会和课程论文，PPT 次数与综述次数更均衡。",
    lab: "适合课题组高频使用，给更多 PPT 与 AI 对话余量。",
  })[id] || "按任务次数使用。";
}

function planRows(plan) {
  return [
    { icon: "导", label: "论文导入与基础翻译", level: "免费", value: "不限次" },
    { icon: "综", label: "论文综述生成", level: plan.reviewQuota > 0 ? "包含" : "未含", value: `${plan.reviewQuota || 0} 次` },
    { icon: "P", label: "组会 PPT 生成", level: plan.pptQuota > 0 ? "重任务" : "未含", value: `${plan.pptQuota || 0} 次` },
    { icon: "问", label: "AI 文章对话", level: plan.chatQuota > 80 ? "高频" : "常规", value: `${plan.chatQuota || 0} 次` },
  ];
}

async function selectAndCheckout(planId) {
  selectedPlan.value = planId;
  await checkout();
}

function formatDate(value) {
  if (!value) return "-";
  if (Array.isArray(value)) return `${value[0]}-${String(value[1]).padStart(2, "0")}-${String(value[2]).padStart(2, "0")}`;
  return String(value).replace("T", " ").slice(0, 16);
}

function orderPlanName(order) {
  return plans.value.find((item) => item.id === order.planId)?.name || (order.planId === "custom-recharge" ? "历史余额订单" : "会员套餐");
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
.membership-page {
  min-height: 100vh;
  padding: 32px clamp(20px, 4vw, 64px) 64px;
  color: #172033;
  background: #f5f7fb;
  font-family: Inter, "Microsoft YaHei", system-ui, sans-serif;
}

.membership-topbar,
.current-strip,
.plan-workbench,
.checkout-bar,
.orders-section {
  width: min(1280px, 100%);
  margin: 0 auto;
}

button,
input,
select,
textarea {
  font: inherit;
}

button {
  cursor: pointer;
}

.membership-topbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.page-chip,
.section-title span,
.checkout-bar > div > span {
  color: #235dd8;
  font-size: 12px;
  font-weight: 850;
}

.membership-topbar h1 {
  max-width: 760px;
  margin: 8px 0 8px;
  font-size: 28px;
  line-height: 1.28;
  letter-spacing: 0;
  text-wrap: balance;
}

.membership-topbar p,
.plan-heading p,
.checkout-bar p {
  margin: 0;
  color: #5e6c83;
  font-size: 14px;
  line-height: 1.7;
}

.ghost-button {
  height: 38px;
  padding: 0 14px;
  border: 1px solid #d8e1ed;
  border-radius: 8px;
  color: #27334a;
  background: #fff;
  font-weight: 850;
}

.refresh-button {
  min-width: 86px;
}

.refresh-button span {
  display: inline-block;
  margin-right: 6px;
}

.spinning {
  animation: spin .7s linear infinite;
}

.current-strip {
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
}

.member-rank,
.entitlement-line,
.plan-workbench,
.checkout-bar,
.orders-section {
  border: 1px solid #dfe6f0;
  border-radius: 12px;
  background: #fff;
}

.member-rank {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 18px;
}

.member-rank > span {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  color: #fff;
  background: #245ce0;
  font-size: 22px;
  font-weight: 900;
}

.member-rank small {
  color: #6c7890;
  font-size: 12px;
  font-weight: 800;
}

.member-rank strong {
  display: block;
  margin: 2px 0 4px;
  font-size: 20px;
}

.member-rank p {
  margin: 0;
  color: #6a758a;
  font-size: 12px;
}

.entitlement-line {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
}

.entitlement-item {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
  border-left: 1px solid #edf1f6;
}

.entitlement-item:first-child {
  border-left: 0;
}

.entitlement-item span {
  color: #68758b;
  font-size: 12px;
  font-weight: 800;
}

.entitlement-item strong {
  font-size: 20px;
}

.entitlement-item i {
  height: 5px;
  overflow: hidden;
  border-radius: 99px;
  background: #e9eef7;
}

.entitlement-item b {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #245ce0;
}

.plan-workbench {
  margin-top: 22px;
  padding: 22px;
}

.plan-heading,
.section-title,
.checkout-bar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.plan-heading h2,
.section-title h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.cycle-switch {
  display: flex;
  padding: 4px;
  border-radius: 9px;
  background: #edf2f8;
}

.cycle-switch button {
  height: 32px;
  min-width: 62px;
  border: 0;
  border-radius: 6px;
  color: #58677d;
  background: transparent;
  font-size: 12px;
  font-weight: 850;
}

.cycle-switch button.active {
  color: #1f58d8;
  background: #fff;
  box-shadow: 0 1px 3px rgba(36, 56, 90, .12);
}

.cycle-switch small {
  margin-left: 4px;
  color: #168158;
}

.plan-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.plan-card {
  --tier: #35725f;
  --tier-soft: #f7fbf9;
  --tier-line: #cfe1db;
  min-width: 0;
  padding: 20px;
  border: 1px solid var(--tier-line);
  border-radius: 12px;
  background: linear-gradient(180deg, var(--tier-soft), #fff 52%);
  transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease;
}

.plan-card.study {
  --tier: #3f64ae;
  --tier-soft: #f6f8fd;
  --tier-line: #cbd7f2;
}

.plan-card.lab {
  --tier: #6c5c9c;
  --tier-soft: #f8f7fc;
  --tier-line: #d8d2ea;
}

.plan-card:hover,
.plan-card.active {
  transform: translateY(-2px);
  box-shadow: 0 12px 26px rgba(31, 48, 84, .09);
}

.plan-card.active {
  border-color: var(--tier);
}

.plan-card header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.plan-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.plan-title-row h3 {
  margin: 0;
  font-size: 20px;
}

.plan-title-row em {
  padding: 4px 9px;
  border-radius: 999px;
  color: var(--tier);
  background: #fff;
  font-size: 11px;
  font-style: normal;
  font-weight: 850;
}

.plan-card header p {
  margin: 6px 0 0;
  color: #5f6e84;
  font-size: 12px;
  line-height: 1.65;
}

.plan-icon {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 10px;
  color: var(--tier);
  background: rgba(255, 255, 255, .78);
  font-weight: 900;
}

.price-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 18px 0 16px;
}

.price-line strong {
  color: var(--tier);
  font-size: 30px;
  line-height: 1;
}

.price-line span {
  padding: 5px 9px;
  border-radius: 999px;
  color: var(--tier);
  background: rgba(255, 255, 255, .8);
  font-size: 12px;
  font-weight: 800;
}

.benefit-ladder {
  overflow: hidden;
  border: 1px solid var(--tier-line);
  border-radius: 10px;
  background: rgba(255, 255, 255, .72);
}

.ladder-head,
.ladder-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) 56px 66px;
  gap: 8px;
  align-items: center;
  min-height: 42px;
  padding: 0 12px;
  border-bottom: 1px solid rgba(125, 145, 176, .16);
}

.ladder-head {
  grid-template-columns: minmax(0, 1fr) auto;
  color: #243048;
  background: rgba(255, 255, 255, .68);
  font-weight: 900;
}

.ladder-head b {
  color: var(--tier);
  font-size: 12px;
}

.ladder-row strong {
  overflow: hidden;
  color: #243048;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ladder-row em {
  color: #f36a22;
  font-size: 12px;
  font-style: normal;
  font-weight: 850;
}

.ladder-row b {
  color: var(--tier);
  text-align: right;
  font-size: 13px;
}

.row-icon {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  color: #fff;
  background: var(--tier);
  font-size: 11px;
  font-weight: 900;
}

.settlement-note {
  margin: 0;
  padding: 11px 12px;
  color: #6c7890;
  text-align: center;
  font-size: 12px;
}

.plan-buy-button,
.primary-button,
.ticket-button {
  height: 38px;
  border: 0;
  border-radius: 8px;
  font-weight: 850;
}

.plan-buy-button {
  width: 100%;
  margin-top: 18px;
  color: #fff;
  background: var(--tier);
}

.checkout-bar {
  position: relative;
  margin-top: 14px;
  padding: 20px 22px;
}

.checkout-bar > div:first-child strong {
  display: block;
  margin: 5px 0;
  font-size: 18px;
}

.checkout-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pay-methods {
  display: flex;
  gap: 8px;
}

.pay-methods button {
  height: 38px;
  min-width: 104px;
  border: 1px solid #dfe6ee;
  border-radius: 8px;
  color: #29364c;
  background: #fff;
  font-weight: 800;
}

.pay-methods button.active {
  border-color: #245de0;
  background: #f4f7ff;
}

.pay-methods i {
  display: inline-grid;
  place-items: center;
  width: 20px;
  height: 20px;
  margin-right: 7px;
  border-radius: 5px;
  color: #fff;
  background: #2074ef;
  font-style: normal;
  font-size: 12px;
}

.pay-methods button:last-child i {
  background: #1ca950;
}

.primary-button {
  padding: 0 18px;
  color: #fff;
  background: #245ce0;
}

.primary-button:disabled,
.ghost-button:disabled {
  opacity: .58;
  cursor: wait;
}

.payment-message {
  position: absolute;
  right: 22px;
  bottom: -26px;
  margin: 0;
  color: #536179;
  font-size: 12px;
}

.orders-section {
  margin-top: 42px;
  padding: 22px;
}

.orders-table {
  margin-top: 16px;
  border-top: 1px solid #e8edf4;
}

.order-head,
.order-item {
  display: grid;
  grid-template-columns: 1.8fr 1fr .7fr .8fr .8fr;
  gap: 16px;
  align-items: center;
  padding: 13px 4px;
}

.order-head {
  color: #718099;
  font-size: 12px;
  font-weight: 800;
}

.order-item {
  border-top: 1px solid #eef2f6;
  font-size: 13px;
}

.order-item > div {
  display: grid;
  gap: 4px;
}

.order-item small {
  color: #8590a2;
  font-size: 11px;
}

.status {
  width: max-content;
  padding: 4px 7px;
  border-radius: 99px;
  color: #58657a;
  background: #eff3f8;
  font-size: 11px;
  font-weight: 800;
}

.status.paid {
  color: #0a8055;
  background: #e8f8ef;
}

.status.failed {
  color: #c73838;
  background: #fff0f0;
}

.ticket-button {
  width: max-content;
  padding: 0 10px;
  border: 1px solid #d8e1ee;
  color: #2855a9;
  background: #fff;
  font-size: 12px;
}

.orders-empty {
  padding: 34px 0;
  color: #7a879a;
  text-align: center;
  font-size: 13px;
}

.ticket-dialog {
  width: min(520px, calc(100vw - 32px));
  padding: 0;
  border: 0;
  border-radius: 12px;
  box-shadow: 0 16px 40px rgba(13, 27, 57, .24);
}

.ticket-dialog::backdrop {
  background: rgba(22, 31, 47, .38);
}

.ticket-dialog form {
  display: grid;
  gap: 14px;
  padding: 24px;
}

.dialog-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
}

.dialog-heading span {
  color: #2860dd;
  font-size: 12px;
  font-weight: 850;
}

.dialog-heading h2 {
  margin: 5px 0 0;
  font-size: 16px;
}

.close-button {
  border: 0;
  color: #6e7b91;
  background: transparent;
  font-size: 26px;
  line-height: 1;
}

.ticket-dialog label {
  display: grid;
  gap: 7px;
  color: #47556c;
  font-size: 12px;
  font-weight: 800;
}

.ticket-dialog input,
.ticket-dialog select,
.ticket-dialog textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 10px;
  border: 1px solid #d9e1ed;
  border-radius: 7px;
  color: #1d293c;
  background: #fff;
  resize: vertical;
}

.ticket-error {
  margin: 0;
  color: #c73636;
  font-size: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 980px) {
  .membership-topbar,
  .plan-heading,
  .checkout-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .current-strip {
    grid-template-columns: 1fr;
  }

  .entitlement-line,
  .plan-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .checkout-actions {
    width: 100%;
    align-items: stretch;
    flex-direction: column;
  }

  .pay-methods {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .payment-message {
    position: static;
  }

  .order-head {
    display: none;
  }

  .order-item {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 560px) {
  .membership-page {
    padding: 22px 14px 42px;
  }

  .membership-topbar h1 {
    font-size: 23px;
  }

  .entitlement-line,
  .plan-cards,
  .order-item {
    grid-template-columns: 1fr;
  }

  .cycle-switch {
    width: 100%;
    overflow-x: auto;
  }

  .cycle-switch button {
    flex: 1;
  }
}
</style>
