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
        <div class="member-card-bg" aria-hidden="true" :style="{ '--gold-card-image': `url(${goldCardReference})` }">
          <span></span>
        </div>
        <div class="member-card-top">
          <strong>{{ membership.active ? memberPeriodLabel : "体验会员" }}</strong>
          <button type="button" @click="scrollToPlans">更多 <i aria-hidden="true"></i></button>
        </div>
        <div class="member-card-main">
          <div>
            <small>{{ membership.active ? "剩余" : "当前状态" }}</small>
            <strong v-if="membership.active" class="remaining-days">
              <b>{{ remainingDays }}</b>
              <span>天</span>
            </strong>
            <strong v-else class="remaining-days inactive">
              <b>0</b>
              <span>天</span>
            </strong>
          </div>
        </div>
        <div class="member-card-bottom">
          <span>{{ membership.name }}</span>
          <p>{{ membership.active ? `有效期至 ${formatFullDate(membership.expiresAt)}` : "当前未购买套餐" }}</p>
        </div>
      </div>
      <div class="entitlement-line">
        <div v-for="item in benefitItems" :key="item.key" class="entitlement-item" :class="quotaTone(item)">
          <span>{{ item.label }}</span>
          <strong>{{ benefitUsageLabel(item) }}</strong>
          <i v-if="!item.unlimited" class="quota-meter"><b :style="{ width: `${quotaPercent(item)}%` }"></b></i>
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
                <span class="plan-icon">{{ planBadge(plan.id) }}</span>
              </header>

              <div class="price-line">
                <strong>¥{{ planPrice(plan) }}</strong>
                <span>{{ cycleLabel(selectedCycle) }}</span>
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
              <p class="settlement-note">未使用次数到期清零，续费后重新获得当期权益。</p>

              <button class="plan-buy-button" @click.stop="selectAndCheckout(plan.id)">
                开通该套餐
              </button>
            </article>
          </div>
        </section>
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
  grid-template-columns: minmax(620px, 1.15fr) minmax(360px, .85fr);
  gap: 24px;
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
  position: relative;
  overflow: hidden;
  aspect-ratio: 1245 / 556;
  min-height: 0;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 0;
  padding: clamp(24px, 2.2vw, 34px) clamp(24px, 2.6vw, 34px) clamp(22px, 2vw, 30px) clamp(32px, 3.2vw, 46px);
  border-color: #f0bf54;
  border-radius: 18px;
  background:
    linear-gradient(110deg, #fff9ed 0%, #f9e7bd 50%, #e8bf61 100%);
  box-shadow: 0 22px 44px rgba(174, 117, 34, .14);
}

.member-card-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

.member-card-bg::before {
  position: absolute;
  inset: 0;
  z-index: 2;
  background:
    radial-gradient(ellipse 46% 54% at 100% 0%, rgba(82, 76, 72, .76) 0%, rgba(105, 96, 88, .68) 34%, rgba(184, 154, 106, .3) 56%, rgba(226, 190, 128, 0) 78%),
    linear-gradient(90deg, rgba(255, 249, 235, .99) 0%, rgba(255, 244, 220, .97) 40%, rgba(247, 221, 174, .74) 58%, rgba(226, 190, 128, .28) 76%, rgba(226, 190, 128, 0) 94%);
  content: "";
}

.member-card-bg::after {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 68%;
  z-index: 1;
  background-image: var(--gold-card-image);
  background-size: auto 100%;
  background-position: right center;
  background-repeat: no-repeat;
  opacity: .98;
  content: "";
}

.member-card-bg span {
  position: absolute;
  inset: 0;
  z-index: 3;
  background:
    radial-gradient(ellipse 44% 52% at 100% 0%, rgba(72, 67, 64, .58) 0%, rgba(96, 88, 82, .5) 34%, rgba(174, 146, 100, .18) 58%, rgba(226, 190, 128, 0) 80%),
    linear-gradient(90deg, rgba(255, 248, 232, .99) 0%, rgba(255, 244, 219, .98) 38%, rgba(247, 224, 181, .66) 58%, rgba(226, 190, 128, .18) 78%, rgba(226, 190, 128, 0) 96%);
}

.member-card-top,
.member-card-main,
.member-card-bottom {
  position: relative;
  z-index: 2;
}

.member-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.member-card-top > strong {
  color: #050505;
  font-size: clamp(22px, 2.05vw, 30px);
  line-height: 1.1;
  font-weight: 400;
  letter-spacing: 0;
}

.member-card-top button {
  height: clamp(34px, 3vw, 42px);
  min-width: clamp(88px, 8.4vw, 112px);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 999px;
  padding: 0 clamp(14px, 1.5vw, 20px);
  color: #111;
  background: rgba(255, 255, 255, .96);
  font-size: clamp(16px, 1.45vw, 20px);
  font-weight: 500;
  box-shadow: 0 8px 16px rgba(87, 63, 42, .1);
}

.member-card-top button i {
  width: 0;
  height: 0;
  border-top: clamp(5px, .55vw, 7px) solid transparent;
  border-bottom: clamp(5px, .55vw, 7px) solid transparent;
  border-left: clamp(7px, .75vw, 10px) solid #111;
}

.member-card-main {
  display: flex;
  align-items: center;
  gap: 0;
  align-self: center;
}

.member-card-main small {
  color: #050505;
  font-size: clamp(20px, 1.9vw, 26px);
  font-weight: 400;
}

.remaining-days {
  display: flex;
  align-items: baseline;
  gap: clamp(10px, 1vw, 15px);
  margin-top: 0;
  color: #080808;
}

.remaining-days b {
  font-size: clamp(42px, 4.2vw, 58px);
  line-height: .88;
  font-weight: 800;
  letter-spacing: 0;
}

.remaining-days span {
  font-size: clamp(20px, 1.8vw, 26px);
  font-weight: 400;
}

.remaining-days.inactive {
  opacity: .7;
}

.member-card-bottom {
  display: grid;
  gap: 0;
}

.member-card-bottom span {
  display: none;
}

.member-card-bottom p {
  margin: 0;
  color: rgba(22, 16, 8, .5);
  font-size: clamp(17px, 1.55vw, 22px);
  font-weight: 400;
  letter-spacing: 0;
}

.entitlement-line {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0;
  overflow: hidden;
  padding: 10px 14px;
}

.entitlement-item {
  display: grid;
  grid-template-columns: 132px 142px minmax(180px, 1fr);
  align-items: center;
  gap: 16px;
  min-height: 48px;
  padding: 8px 4px;
  border-top: 1px solid #edf1f6;
}

.entitlement-item:first-child {
  border-top: 0;
}

.entitlement-item span {
  color: #68758b;
  font-size: 12px;
  font-weight: 800;
}

.entitlement-item strong {
  color: #172033;
  font-size: 18px;
  font-variant-numeric: tabular-nums;
}

.quota-meter {
  height: 7px;
  overflow: hidden;
  border-radius: 99px;
  background: #e9eef7;
}

.quota-meter b {
  display: block;
  height: 100%;
  border-radius: inherit;
  transition: width .18s ease;
}

.quota-good .quota-meter b { background: #16a36a; }
.quota-mid .quota-meter b { background: #f2b01e; }
.quota-low .quota-meter b { background: #ef4444; }
.quota-unlimited .quota-meter { display: none; }

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

.plan-groups {
  display: grid;
  gap: 22px;
  margin-top: 18px;
}

.plan-group {
  display: grid;
  gap: 12px;
}

.plan-group-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}

.plan-group-title span {
  color: #172033;
  font-size: 15px;
  font-weight: 950;
}

.plan-group-title p {
  margin: 0;
  color: #6a758a;
  font-size: 12px;
}

.plan-cards {
  display: grid;
  gap: 16px;
  padding-bottom: 4px;
}

.plan-cards-personal {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.plan-cards-team {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.plan-card {
  --tier: #14946f;
  --tier-soft: #ffffff;
  --tier-line: #e2e8f0;
  min-width: 0;
  padding: 24px;
  border: 1px solid var(--tier-line);
  border-radius: 16px;
  background: var(--tier-soft);
  transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease;
}

.plan-card.study {
  --tier: #2563eb;
  --tier-soft: #f8fbff;
  --tier-line: #a8c7ff;
}

.plan-card.pro {
  --tier: #7c3aed;
  --tier-soft: #fbf8ff;
  --tier-line: #cbb6ff;
}

.plan-card.max {
  --tier: #be185d;
  --tier-soft: #fff7fb;
  --tier-line: #f3a6c8;
}

.plan-card.team_plus {
  --tier: #e06d1b;
  --tier-soft: #fffaf5;
  --tier-line: #f2bc8f;
}

.plan-card.team_pro {
  --tier: #b45309;
  --tier-soft: #fff8ed;
  --tier-line: #e9b86f;
}

.plan-card:hover,
.plan-card.active {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(31, 48, 84, .08);
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
  background: rgba(255, 255, 255, .9);
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

.center-plan-features {
  display: grid;
  gap: 12px;
  min-height: 250px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.center-plan-features li {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.center-plan-features li.excluded {
  opacity: .72;
}

.feature-check {
  color: #16b981;
  font-size: 16px;
  font-weight: 900;
  line-height: 1.4;
}

.feature-check.excluded {
  color: #ef4444;
}

.center-plan-features strong {
  display: block;
  color: #23304a;
  font-size: 14px;
  line-height: 1.45;
}

.center-plan-features small {
  display: block;
  margin-top: 2px;
  color: #66758b;
  font-size: 12px;
  line-height: 1.5;
}

.settlement-note {
  margin: 14px 0 0;
  padding: 10px 12px;
  border: 1px solid #e7edf5;
  border-radius: 10px;
  color: #6c7890;
  text-align: center;
  font-size: 12px;
  background: #fff;
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

  .member-rank {
    min-height: 0;
  }

  .plan-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .entitlement-item {
    grid-template-columns: 118px 124px minmax(120px, 1fr);
    gap: 12px;
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

  .member-rank {
    min-height: 0;
    padding: 22px 20px 20px;
  }

  .member-card-top button {
    height: 36px;
    padding: 0 14px;
  }

  .member-card-main {
    align-self: center;
  }

  .member-card-bg span {
    background: linear-gradient(90deg, rgba(255, 248, 232, .99) 0%, rgba(255, 244, 219, .97) 48%, rgba(255, 235, 195, .7) 64%, rgba(255, 255, 255, 0) 82%);
  }

  .plan-cards,
  .order-item {
    grid-template-columns: 1fr;
  }

  .entitlement-item {
    grid-template-columns: 1fr auto;
    gap: 6px 12px;
    padding: 11px 2px;
  }

  .entitlement-item .quota-meter {
    grid-column: 1 / -1;
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
