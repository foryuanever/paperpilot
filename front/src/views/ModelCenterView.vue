<template>
  <div class="usage-page">
    <header class="usage-header">
      <div>
        <p class="page-kicker">用量与额度</p>
        <h1>用量与费用看板</h1>
        <p>这里统计已入账的功能调用：输入 Token、输出 Token、费用和账号额度都来自后端记录。</p>
      </div>
      <div class="header-actions">
        <span class="scope-pill">{{ usageStore.state.usageScope === "all" ? "全站最近记录" : "当前账号" }}</span>
        <button type="button" class="refresh-button" :disabled="loading" @click="refreshUsage">
          <span>{{ loading ? "刷新中" : "刷新" }}</span>
        </button>
      </div>
    </header>

    <section class="metric-strip" aria-label="用量概览">
      <article class="metric-card pink">
        <span class="metric-icon">₮</span>
        <div>
          <small>当前 Token 余额</small>
          <strong>{{ formatTokens(usageStore.tokenRemaining) }}</strong>
          <em>已用 {{ usageStore.usagePercent }}%</em>
        </div>
      </article>
      <article class="metric-card teal">
        <span class="metric-icon">↻</span>
        <div>
          <small>当前 RPM</small>
          <strong>{{ usageStore.state.rpm || 0 }}</strong>
          <em>最近一分钟请求数</em>
        </div>
      </article>
      <article class="metric-card indigo">
        <span class="metric-icon">∑</span>
        <div>
          <small>当前 TPM</small>
          <strong>{{ formatTokens(usageStore.state.tpm || 0) }}</strong>
          <em>最近一分钟 Token</em>
        </div>
      </article>
      <article class="metric-card violet">
        <span class="metric-icon">$</span>
        <div>
          <small>每分钟费用</small>
          <strong>{{ formatMoney(usageStore.state.mpm || 0) }}</strong>
          <em>按已入账 Token 单价计算</em>
        </div>
      </article>
    </section>

    <section class="summary-grid">
      <article class="balance-card">
        <span class="balance-icon">◔</span>
        <div>
          <strong>{{ formatTokens(usageStore.state.tokenUsed) }}</strong>
          <p>累计 Token 用量</p>
        </div>
        <div>
          <strong>{{ formatMoney(usageStore.state.estimatedCost || 0) }}</strong>
          <p>累计费用</p>
        </div>
        <div>
          <strong>{{ totalRequestsDisplay }}</strong>
          <p>总请求数</p>
        </div>
        <button type="button" class="recharge-entry" @click="openRecharge">充值</button>
      </article>

      <article class="day-card cyan">
        <span class="balance-icon">⟳</span>
        <div>
          <strong>{{ usageStore.state.todayRequests || 0 }}</strong>
          <p>今日请求数</p>
        </div>
      </article>

      <article class="day-card amber">
        <span class="balance-icon">∑</span>
        <div>
          <strong>{{ formatTokens(usageStore.state.todayTokens || 0) }}</strong>
          <p>今日 Token 数</p>
        </div>
      </article>
    </section>

    <section class="chart-panel">
      <div class="panel-toolbar">
        <nav class="soft-tabs" aria-label="统计维度">
          <button type="button" :class="{ active: activeChart === 'cost' }" @click="activeChart = 'cost'">费用</button>
          <button type="button" :class="{ active: activeChart === 'calls' }" @click="activeChart = 'calls'">调用分布</button>
          <button type="button" :class="{ active: activeChart === 'tokens' }" @click="activeChart = 'tokens'">Token分布</button>
        </nav>
        <div class="chart-meta">
          <span>点击刷新后实时读取</span>
          <strong>周期 7 天</strong>
        </div>
      </div>

      <div class="chart-note">
        7天内总计 <strong>{{ chartTotalLabel }}</strong>，平均每天 <strong>{{ chartAverageLabel }}</strong>
      </div>

      <div v-if="usageStore.state.dailyUsage.length" class="bar-chart" :style="{ '--max': maxDailyValue }">
        <div v-for="item in chartRows" :key="item.label" class="bar-column">
          <div class="bar-rail">
            <i :style="{ height: `${barHeight(item.value)}%` }"></i>
          </div>
          <span>{{ item.label }}</span>
        </div>
      </div>
      <p v-else class="empty-text">暂无可统计的调用记录。</p>
    </section>

    <section class="cost-formula-card" aria-label="费用计算">
      <div>
        <p>费用计算</p>
        <strong>费用 = Token 用量 × {{ formatMoney(TOKEN_UNIT_PRICE) }} / 1K</strong>
      </div>
      <span>每条记录按输入 Token 与输出 Token 合计计算；页面只展示费用结果，不暴露底层调用配置。</span>
    </section>

    <section class="ledger-panel">
      <div class="panel-toolbar ledger-toolbar">
        <nav class="soft-tabs" aria-label="调用记录筛选">
          <button type="button" :class="{ active: activeLogTab === 'all' }" @click="activeLogTab = 'all'">全部</button>
          <button type="button" :class="{ active: activeLogTab === 'report' }" @click="activeLogTab = 'report'">组会</button>
          <button type="button" :class="{ active: activeLogTab === 'translate' }" @click="activeLogTab = 'translate'">翻译</button>
          <button type="button" :class="{ active: activeLogTab === 'qa' }" @click="activeLogTab = 'qa'">问答</button>
        </nav>
        <button type="button" class="icon-button" :disabled="loading" title="刷新" @click="refreshUsage">↻</button>
      </div>

      <div class="ledger-filters">
        <label>
          开始
          <input :value="dateRange.start" type="text" readonly>
        </label>
        <label>
          结束
          <input :value="dateRange.end" type="text" readonly>
        </label>
        <label>
          调用类型
          <select v-model="selectedScene">
            <option value="">全部类型</option>
            <option v-for="scene in sceneOptions" :key="scene" :value="scene">{{ translateScene(scene) }}</option>
          </select>
        </label>
      </div>

      <div class="usage-table" role="table" aria-label="最近调用记录">
        <div class="usage-row usage-head" role="row">
          <span>创建时间</span>
          <span>类型</span>
          <span>论文 / 任务</span>
          <span>输入</span>
          <span>输出</span>
          <span>Token 用量</span>
          <span>费用</span>
        </div>
        <div v-for="row in filteredCalls" :key="`${row.time}-${row.paper}-${row.tokens}`" class="usage-row" role="row">
          <span>{{ row.time || "-" }}</span>
          <span>{{ translateAction(row.action) }}</span>
          <span class="paper-cell">{{ row.paper || "当前论文" }}</span>
          <span>{{ formatTokens(row.promptTokens || 0) }}</span>
          <span>{{ formatTokens(row.completionTokens || 0) }}</span>
          <strong>{{ formatTokens(row.tokens || 0) }}</strong>
          <strong>{{ formatMoney(rowCost(row)) }}</strong>
        </div>
      </div>
      <p v-if="!filteredCalls.length" class="empty-text">
        当前筛选下没有调用记录。
      </p>
      <footer class="table-footer">
        <span>显示最近 {{ filteredCalls.length }} 条已入账记录，Token 用量 = 输入 + 输出，费用按已入账 Token 统一计算。</span>
      </footer>
      <aside class="accounting-note">
        <strong>关于组会 PPT 用量</strong>
        <span>组会 PPT 完成后会写入本页账单。由于执行器只返回材料、提示词和日志规模，系统按同一 Token 口径入账，用户侧不展示底层调用配置。</span>
      </aside>
    </section>

    <div v-if="showRecharge" class="recharge-backdrop" @click.self="closeRecharge">
      <section class="recharge-dialog" role="dialog" aria-modal="true" aria-label="额度充值">
        <header>
          <div>
            <p>额度充值</p>
            <h2>选择套餐与支付方式</h2>
          </div>
          <button type="button" class="dialog-close" aria-label="关闭" @click="closeRecharge">×</button>
        </header>

        <div class="recharge-body">
          <div class="plan-picker">
            <button
              v-for="plan in rechargePlans"
              :key="plan.id"
              type="button"
              class="plan-option"
              :class="{ active: selectedPlanId === plan.id, recommended: plan.highlight }"
              @click="selectedPlanId = plan.id"
            >
              <span>{{ plan.name }}</span>
              <strong>{{ plan.price }}<small>{{ plan.period }}</small></strong>
              <em>{{ formatTokens(plan.tokenQuota) }} Token</em>
            </button>
          </div>

          <div class="payment-picker">
            <button
              type="button"
              class="pay-option alipay"
              :class="{ active: selectedProvider === 'alipay' }"
              @click="selectedProvider = 'alipay'"
            >
              <i>支</i>
              <span>支付宝</span>
            </button>
            <button
              type="button"
              class="pay-option wechat"
              :class="{ active: selectedProvider === 'wechat' }"
              @click="selectedProvider = 'wechat'"
            >
              <i>微</i>
              <span>微信支付</span>
            </button>
          </div>

          <div class="payment-summary">
            <span>应付金额</span>
            <strong>{{ selectedPlan?.price || "-" }}</strong>
          </div>

          <p v-if="paymentMessage" class="payment-message" :class="{ warning: paymentStatus === 'config_required' }">
            {{ paymentMessage }}
          </p>
        </div>

        <footer>
          <button type="button" class="ghost-action" @click="closeRecharge">取消</button>
          <button type="button" class="primary-action" :disabled="paying || !selectedPlan" @click="createRechargeOrder">
            {{ paying ? "创建订单中" : `去${selectedProvider === "alipay" ? "支付宝" : "微信"}支付` }}
          </button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useAuthStore } from "../stores/auth";
import { useUsageStore } from "../stores/usage";
import { billingPlans } from "../constants/pages";
import { paperpilotApi } from "../services/paperpilotApi";

const usageStore = useUsageStore();
const authStore = useAuthStore();

const loading = ref(false);
const activeChart = ref("cost");
const activeLogTab = ref("all");
const selectedScene = ref("");
const TOKEN_UNIT_PRICE = 0.02;
const showRecharge = ref(false);
const selectedPlanId = ref("monthly-pro");
const selectedProvider = ref("alipay");
const paying = ref(false);
const paymentMessage = ref("");
const paymentStatus = ref("");

const rechargePlans = computed(() => billingPlans.filter((plan) => ["monthly-basic", "monthly-pro", "quarterly-pro", "yearly-pro"].includes(plan.id)));
const selectedPlan = computed(() => rechargePlans.value.find((plan) => plan.id === selectedPlanId.value) || rechargePlans.value[0]);

const sceneOptions = computed(() => [
  ...new Set(usageStore.state.recentCalls.map((row) => row.action).filter(Boolean)),
]);

const filteredCalls = computed(() => usageStore.state.recentCalls.filter((row) => {
  if (selectedScene.value && row.action !== selectedScene.value) return false;
  if (activeLogTab.value === "report") return /组会|汇报|综述/.test(String(row.action || ""));
  if (activeLogTab.value === "translate") return /翻译/.test(String(row.action || ""));
  if (activeLogTab.value === "qa") return /问答|提问/.test(String(row.action || ""));
  return true;
}));

const totalRequestsDisplay = computed(() => {
  const total = Number(usageStore.state.totalRequests || 0);
  return total || usageStore.state.recentCalls.length;
});

const dateRange = computed(() => {
  const rows = usageStore.state.dailyUsage || [];
  return {
    start: rows[0]?.label || "-",
    end: rows[rows.length - 1]?.label || "-",
  };
});

const chartRows = computed(() => (usageStore.state.dailyUsage || []).map((item) => {
  const tokens = Number(item.tokens || 0);
  const calls = Number(item.calls || 0);
  const cost = tokens * TOKEN_UNIT_PRICE / 1000;
  return {
    label: item.label,
    tokens,
    calls,
    cost,
    value: activeChart.value === "calls" ? calls : activeChart.value === "cost" ? cost : tokens,
  };
}));

const maxDailyValue = computed(() => Math.max(...chartRows.value.map((row) => Number(row.value || 0)), 1));

const chartTotalLabel = computed(() => {
  const total = chartRows.value.reduce((sum, row) => sum + Number(row.value || 0), 0);
  if (activeChart.value === "cost") return formatMoney(total);
  if (activeChart.value === "calls") return `${total} 次`;
  return formatTokens(total);
});

const chartAverageLabel = computed(() => {
  const count = Math.max(chartRows.value.length, 1);
  const total = chartRows.value.reduce((sum, row) => sum + Number(row.value || 0), 0) / count;
  if (activeChart.value === "cost") return formatMoney(total);
  if (activeChart.value === "calls") return `${total.toFixed(1)} 次`;
  return formatTokens(Math.round(total));
});

onMounted(refreshUsage);

async function refreshUsage() {
  loading.value = true;
  try {
    await usageStore.fetchSummary();
  } catch (error) {
    authStore.addNotification({
      title: "用量加载失败",
      desc: error?.response?.data?.message || "请确认后端服务正在运行。",
    });
  } finally {
    loading.value = false;
  }
}

function openRecharge() {
  paymentMessage.value = "";
  paymentStatus.value = "";
  if (!selectedPlan.value && rechargePlans.value.length) {
    selectedPlanId.value = rechargePlans.value[0].id;
  }
  showRecharge.value = true;
}

function closeRecharge() {
  if (paying.value) return;
  showRecharge.value = false;
}

async function createRechargeOrder() {
  if (!selectedPlan.value) return;
  paying.value = true;
  paymentMessage.value = "";
  paymentStatus.value = "";
  try {
    const order = await paperpilotApi.createPaymentOrder({
      planId: selectedPlan.value.id,
      provider: selectedProvider.value,
      amount: selectedPlan.value.price,
    });
    paymentStatus.value = order.status || "";
    paymentMessage.value = order.message || "订单已创建。";
    if (order.paymentUrl) {
      window.open(order.paymentUrl, "_blank", "noopener,noreferrer");
    }
  } catch (error) {
    paymentStatus.value = "failed";
    paymentMessage.value = error?.response?.data?.message || "订单创建失败，请稍后重试。";
  } finally {
    paying.value = false;
  }
}

function barHeight(value) {
  return Math.max(Number(value || 0) > 0 ? 4 : 0, Math.round((Number(value || 0) / maxDailyValue.value) * 100));
}

function formatTokens(value) {
  const number = Number(value || 0);
  if (number >= 1_000_000) return `${(number / 1_000_000).toFixed(2)}M`;
  if (number >= 10_000) return `${(number / 1000).toFixed(1)}K`;
  return new Intl.NumberFormat("zh-CN").format(Math.round(number));
}

function formatMoney(value) {
  return `$${Number(value || 0).toFixed(4)}`;
}

function rowCost(row) {
  if (row?.cost !== undefined && row?.cost !== null) return Number(row.cost || 0);
  return Number(row?.tokens || 0) * TOKEN_UNIT_PRICE / 1000;
}

const sceneMap = {
  translate: "学术翻译",
  analyze: "论文解析",
  summary: "汇总综述",
  report: "组会",
  qa: "论文问答",
  "组会汇报": "组会论文综述生成",
  "组会论文内容生成": "组会论文内容生成",
  "组会论文综述生成": "组会论文综述生成",
  "双栏翻译": "PDF双栏翻译",
  "PDF双栏翻译": "PDF双栏翻译",
  "论文问答": "论文问答",
  "论文选区提问": "论文选区提问",
};

function translateScene(value) {
  return sceneMap[value] || cleanActionName(value);
}

function translateAction(value) {
  return sceneMap[value] || cleanActionName(value);
}

function cleanActionName(value) {
  return String(value || "-").replace(/（[^）]*）/g, "");
}
</script>

<style scoped>
.usage-page {
  min-height: 100vh;
  padding: 28px 32px 48px;
  background: #f7f9fc;
  color: #171d2a;
}

.usage-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin: 0 auto 22px;
  max-width: 1560px;
}

.page-kicker {
  margin: 0 0 6px;
  color: #5d6b82;
  font-size: 13px;
  font-weight: 800;
}

.usage-header h1 {
  margin: 0;
  color: #161b26;
  font-size: 30px;
  line-height: 1.15;
  letter-spacing: 0;
}

.usage-header p:last-child {
  margin: 9px 0 0;
  color: #667085;
  font-size: 14px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.scope-pill,
.refresh-button,
.icon-button {
  border: 1px solid #d9e1ec;
  border-radius: 10px;
  background: #fff;
  color: #293548;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
}

.scope-pill {
  padding: 9px 12px;
}

.refresh-button,
.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 14px;
  cursor: pointer;
}

.refresh-button {
  width: 74px;
}

.refresh-button span {
  display: inline-block;
  width: 42px;
  text-align: center;
}

button:disabled {
  cursor: not-allowed;
  opacity: .55;
}

.metric-strip,
.summary-grid,
.cost-formula-card,
.chart-panel,
.ledger-panel {
  max-width: 1560px;
  margin-left: auto;
  margin-right: auto;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.metric-card,
.balance-card,
.day-card,
.cost-formula-card,
.chart-panel,
.ledger-panel {
  border: 1px solid #e4eaf2;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 18px rgba(18, 31, 53, .04);
}

.metric-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 96px;
  padding: 20px 22px;
}

.metric-icon,
.balance-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  border-radius: 50%;
  color: #fff;
  font-size: 20px;
  font-weight: 900;
}

.metric-card.pink .metric-icon { background: #ec5f9f; }
.metric-card.teal .metric-icon { background: #28c7bd; }
.metric-card.indigo .metric-icon { background: #7786e8; }
.metric-card.violet .metric-icon { background: #c75ada; }

.metric-card div {
  display: grid;
  flex: 1;
  min-width: 0;
  gap: 4px;
}

.metric-card small,
.balance-card p,
.day-card p {
  color: #717b8d;
  font-size: 13px;
  font-weight: 700;
}

.metric-card strong,
.balance-card strong,
.day-card strong {
  color: #1a2030;
  font-size: 24px;
  line-height: 1.1;
}

.metric-card em {
  color: #8a94a6;
  font-size: 12px;
  font-style: normal;
}

.summary-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px 280px;
  gap: 18px;
  margin-bottom: 22px;
}

.balance-card {
  display: grid;
  grid-template-columns: 62px repeat(3, 1fr) auto;
  align-items: center;
  gap: 20px;
  min-height: 112px;
  padding: 22px 26px;
}

.balance-card .balance-icon,
.day-card.cyan .balance-icon { background: #2ecbc0; }
.day-card.amber .balance-icon { background: #f5b51b; }

.balance-card div:not(:first-child) {
  min-width: 0;
  border-left: 1px solid #edf1f6;
  padding-left: 22px;
}

.balance-card p,
.day-card p {
  margin: 7px 0 0;
}

.recharge-entry {
  min-width: 96px;
  height: 42px;
  border: 0;
  border-radius: 11px;
  background: #1f5be3;
  color: #fff;
  font: inherit;
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
  transition: background .18s ease, transform .18s ease;
}

.recharge-entry:hover {
  background: #1748c4;
}

.recharge-entry:active {
  transform: translateY(1px);
}

.day-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px 24px;
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 26px 0;
}

.soft-tabs {
  display: flex;
  gap: 24px;
}

.soft-tabs button {
  position: relative;
  border: 0;
  background: transparent;
  color: #4f5b6f;
  font: inherit;
  font-size: 14px;
  font-weight: 850;
  cursor: pointer;
}

.soft-tabs button.active {
  color: #161b26;
}

.soft-tabs button.active::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: -14px;
  height: 2px;
  border-radius: 999px;
  background: #171d2a;
}

.chart-meta {
  display: flex;
  gap: 22px;
  color: #717b8d;
  font-size: 13px;
}

.chart-meta strong {
  color: #3b4658;
}

.chart-note {
  margin: 32px 26px 0;
  color: #7a8495;
  font-size: 14px;
}

.chart-note strong {
  color: #4a5568;
}

.bar-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(42px, 1fr));
  align-items: end;
  gap: 34px;
  height: 380px;
  margin: 24px 32px 28px;
  padding: 0 20px 28px;
  border-bottom: 2px solid #8a909a;
  background:
    linear-gradient(to bottom, transparent 24%, #eef1f5 24.2%, transparent 24.5%),
    linear-gradient(to bottom, transparent 49%, #eef1f5 49.2%, transparent 49.5%),
    linear-gradient(to bottom, transparent 74%, #eef1f5 74.2%, transparent 74.5%);
}

.bar-column {
  display: grid;
  grid-template-rows: 1fr auto;
  gap: 10px;
  min-width: 0;
  height: 100%;
  text-align: center;
}

.bar-rail {
  display: flex;
  align-items: flex-end;
  min-height: 0;
}

.bar-rail i {
  display: block;
  width: 100%;
  min-height: 0;
  background: #5868f6;
  transition: height .22s ease;
}

.bar-column span {
  color: #3f4857;
  font-size: 12px;
}

.ledger-panel {
  margin-top: 22px;
  overflow: hidden;
}

.cost-formula-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-top: 18px;
  padding: 18px 22px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.cost-formula-card p {
  margin: 0 0 5px;
  color: #526075;
  font-size: 12px;
  font-weight: 900;
}

.cost-formula-card strong {
  color: #182234;
  font-size: 18px;
}

.cost-formula-card span {
  max-width: 620px;
  color: #667085;
  font-size: 13px;
  line-height: 1.55;
}

.ledger-toolbar {
  padding-bottom: 18px;
  border-bottom: 1px solid #edf1f6;
}

.icon-button {
  width: 36px;
  padding: 0;
  border-radius: 50%;
}

.ledger-filters {
  display: grid;
  grid-template-columns: repeat(3, minmax(160px, 1fr));
  gap: 18px;
  padding: 20px 26px;
  border-bottom: 1px solid #edf1f6;
}

.ledger-filters label {
  display: grid;
  gap: 8px;
  color: #384357;
  font-size: 13px;
  font-weight: 850;
}

.ledger-filters input,
.ledger-filters select {
  height: 38px;
  border: 1px solid #dce4ef;
  border-radius: 9px;
  padding: 0 12px;
  background: #fff;
  color: #202938;
  font: inherit;
}

.usage-table {
  min-width: 940px;
}

.usage-row {
  display: grid;
  grid-template-columns: 150px 180px minmax(280px, 1fr) 110px 110px 120px 110px;
  gap: 14px;
  align-items: center;
  min-height: 52px;
  padding: 0 26px;
  border-bottom: 1px dashed #edf1f6;
  color: #606b7c;
  font-size: 13px;
}

.usage-head {
  min-height: 48px;
  color: #2e384a;
  font-weight: 900;
  border-bottom-style: solid;
}

.paper-cell {
  overflow: hidden;
  color: #596579;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.usage-row strong {
  color: #384357;
}

.table-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 26px;
  color: #717b8d;
  font-size: 13px;
}

.empty-text {
  margin: 20px 26px;
  color: #717b8d;
  line-height: 1.7;
}

.accounting-note {
  display: grid;
  gap: 6px;
  margin: 0 26px 22px;
  padding: 14px 16px;
  border: 1px solid #cfe1ff;
  border-radius: 11px;
  background: #f7fbff;
  color: #40506a;
  font-size: 13px;
  line-height: 1.65;
}

.accounting-note strong {
  color: #1b4eb6;
}

.recharge-backdrop {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, .42);
}

.recharge-dialog {
  width: min(760px, calc(100vw - 32px));
  max-height: calc(100vh - 48px);
  overflow: auto;
  border: 1px solid #dce5f2;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(15, 23, 42, .22);
}

.recharge-dialog header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 26px 18px;
  border-bottom: 1px solid #edf1f6;
}

.recharge-dialog header p {
  margin: 0 0 6px;
  color: #2357d6;
  font-size: 12px;
  font-weight: 900;
}

.recharge-dialog h2 {
  margin: 0;
  color: #172033;
  font-size: 24px;
  line-height: 1.2;
}

.dialog-close {
  width: 34px;
  height: 34px;
  border: 1px solid #dce4ef;
  border-radius: 50%;
  background: #fff;
  color: #526075;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
}

.recharge-body {
  display: grid;
  gap: 18px;
  padding: 22px 26px;
}

.plan-picker {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.plan-option {
  position: relative;
  display: grid;
  gap: 8px;
  min-height: 112px;
  border: 1px solid #dfe7f2;
  border-radius: 14px;
  background: #fbfdff;
  padding: 16px;
  color: #1f2937;
  text-align: left;
  cursor: pointer;
}

.plan-option.recommended::after {
  content: "推荐";
  position: absolute;
  top: 12px;
  right: 12px;
  border-radius: 999px;
  background: #e8f1ff;
  color: #2357d6;
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 900;
}

.plan-option.active {
  border-color: #2d63df;
  background: #f5f8ff;
}

.plan-option span {
  color: #475569;
  font-size: 13px;
  font-weight: 900;
}

.plan-option strong {
  color: #111827;
  font-size: 26px;
  line-height: 1;
}

.plan-option small {
  margin-left: 2px;
  color: #64748b;
  font-size: 13px;
}

.plan-option em {
  color: #667085;
  font-size: 13px;
  font-style: normal;
  font-weight: 800;
}

.payment-picker {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.pay-option {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 58px;
  border: 1px solid #dfe7f2;
  border-radius: 13px;
  background: #fff;
  color: #1f2937;
  font: inherit;
  font-weight: 900;
  cursor: pointer;
}

.pay-option i {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  margin-left: 14px;
  border-radius: 10px;
  color: #fff;
  font-style: normal;
}

.pay-option.alipay i { background: #1677ff; }
.pay-option.wechat i { background: #16a34a; }

.pay-option.active {
  border-color: #2d63df;
  background: #f6f9ff;
}

.payment-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #e6edf6;
  border-radius: 13px;
  background: #f8fbff;
  padding: 14px 16px;
}

.payment-summary span {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.payment-summary strong {
  color: #111827;
  font-size: 24px;
}

.payment-message {
  margin: 0;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 12px 14px;
  font-size: 13px;
  line-height: 1.6;
}

.payment-message.warning {
  border-color: #fed7aa;
  background: #fff7ed;
  color: #9a3412;
}

.recharge-dialog footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 18px 26px 24px;
  border-top: 1px solid #edf1f6;
}

.ghost-action,
.primary-action {
  min-width: 112px;
  height: 42px;
  border-radius: 11px;
  font: inherit;
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
}

.ghost-action {
  border: 1px solid #d9e1ec;
  background: #fff;
  color: #344158;
}

.primary-action {
  border: 0;
  background: #1f5be3;
  color: #fff;
}

@media (max-width: 1120px) {
  .metric-strip,
  .summary-grid,
  .cost-formula-card {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .balance-card {
    grid-column: 1 / -1;
  }

  .ledger-filters {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .usage-page {
    padding: 20px 14px 32px;
  }

  .usage-header,
  .panel-toolbar,
  .table-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .metric-strip,
  .summary-grid,
  .cost-formula-card,
  .balance-card {
    grid-template-columns: 1fr;
  }

  .cost-formula-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .balance-card div:not(:first-child) {
    border-left: 0;
    padding-left: 0;
  }

  .recharge-entry {
    width: 100%;
  }

  .plan-picker,
  .payment-picker {
    grid-template-columns: 1fr;
  }

  .bar-chart {
    gap: 12px;
    margin-left: 14px;
    margin-right: 14px;
    padding-left: 0;
    padding-right: 0;
  }

  .ledger-panel {
    overflow-x: auto;
  }
}
</style>
