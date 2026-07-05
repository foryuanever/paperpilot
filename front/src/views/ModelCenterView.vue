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

    <nav class="usage-subnav" aria-label="用量页面导航">
      <button type="button" :class="{ active: activeSubTab === 'usage' }" @click="activeSubTab = 'usage'">用量明细</button>
      <button type="button" :class="{ active: activeSubTab === 'recharge' }" @click="activeSubTab = 'recharge'">余额充值</button>
    </nav>

    <template v-if="activeSubTab === 'usage'">
    <section class="metric-strip" aria-label="用量概览">
      <article class="metric-card pink">
        <span class="metric-icon">₮</span>
        <div>
          <small>现金余额</small>
          <strong>{{ formatCny(usageStore.state.balanceAmount || 0) }}</strong>
          <em>充值后按调用扣费</em>
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
        <button type="button" class="recharge-entry" @click="activeSubTab = 'recharge'">充值</button>
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
        <div
          v-for="item in chartRows"
          :key="item.label"
          class="bar-column"
          tabindex="0"
          :aria-label="chartTooltipText(item)"
        >
          <div class="bar-rail">
            <i :style="{ height: `${barHeight(item.value)}%` }">
              <span class="bar-tooltip" role="tooltip">
                <strong>{{ item.label }}</strong>
                <em>{{ chartMetricLabel }}：{{ formatChartValue(item.value) }}</em>
                <small>费用 {{ formatMoney(item.cost) }}</small>
                <small>调用 {{ item.calls }} 次 · Token {{ formatTokens(item.tokens) }}</small>
              </span>
            </i>
          </div>
          <span>{{ item.label }}</span>
        </div>
      </div>
      <p v-else class="empty-text">暂无可统计的调用记录。</p>
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
    </template>

    <template v-else>
    <section class="recharge-page-card" aria-label="余额充值">
      <aside class="recharge-account-panel">
        <div class="account-balance-line">
          <span>当前余额</span>
          <strong>{{ formatCny(usageStore.state.balanceAmount || 0) }}</strong>
        </div>
        <div class="account-mini-grid">
          <div>
            <small>待支付订单</small>
            <b>{{ pendingOrderCount }}</b>
          </div>
          <div>
            <small>处理中工单</small>
            <b>{{ openTicketCount }}</b>
          </div>
          <div>
            <small>已处理</small>
            <b>{{ processedTicketCount }}</b>
          </div>
        </div>
        <p>充值订单会逐条保存在下方。退款和工单必须绑定具体说明，管理员处理后这里会同步显示状态。</p>
      </aside>
      <div class="amount-panel">
        <label>
          充值金额
          <div class="amount-input">
            <span>¥</span>
            <input v-model.number="rechargeAmount" type="number" min="1" step="1" placeholder="输入金额" />
          </div>
        </label>
        <div class="quick-amounts">
          <button v-for="amount in quickAmounts" :key="amount" type="button" :class="{ active: rechargeAmount === amount }" @click="rechargeAmount = amount">
            ¥{{ amount }}
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
          <strong>{{ formatCny(rechargeAmount || 0) }}</strong>
        </div>

        <p v-if="paymentMessage" class="payment-message" :class="{ warning: paymentStatus === 'config_required' }">
          {{ paymentMessage }}
        </p>

        <button type="button" class="primary-action pay-submit" :disabled="paying || !validRechargeAmount" @click="createRechargeOrder">
          {{ paying ? "创建订单中" : `去${selectedProvider === "alipay" ? "支付宝" : "微信"}支付` }}
        </button>
      </div>
    </section>

    <section class="order-panel" aria-label="订单与售后">
      <div class="order-panel-head">
        <div>
          <p>订单信息</p>
          <h3>充值订单与售后工单</h3>
        </div>
        <div class="order-head-actions">
          <button type="button" class="secondary-action" @click="openTicketDialog('support')">新建工单</button>
          <button type="button" class="secondary-action" :disabled="ordersLoading" @click="loadPaymentOrders">
            {{ ordersLoading ? "刷新中" : "刷新订单" }}
          </button>
        </div>
      </div>

      <div class="order-grid">
        <article v-for="order in paymentOrders" :key="order.orderNo" class="order-card">
          <div>
            <span>{{ providerLabel(order.provider) }}</span>
            <strong>{{ formatCny(order.amount || 0) }}</strong>
          </div>
          <p>{{ order.orderNo }}</p>
          <em>{{ statusLabel(order.status) }} · {{ formatDateTime(order.createdAt) }}</em>
          <div class="order-actions">
            <button type="button" @click="openTicketDialog('support', order.orderNo)">新建工单</button>
            <button type="button" @click="openTicketDialog('refund', order.orderNo)">申请退款</button>
          </div>
        </article>
        <div v-if="!paymentOrders.length" class="order-empty">
          暂无充值订单。创建订单后会在这里显示订单号、金额、支付方式和处理状态。
        </div>
      </div>

      <div class="ticket-list">
        <div class="ticket-list-head">
          <strong>最近售后</strong>
          <span>{{ paymentTickets.length }} 条</span>
        </div>
        <article v-for="ticket in paymentTickets" :key="ticket.id" class="ticket-card" :class="`ticket-${ticket.status || 'open'}`">
          <div>
            <span>{{ ticket.type === "refund" ? "退款申请" : "支付工单" }} #{{ ticket.id }}</span>
            <strong>{{ ticket.subject }}</strong>
            <p>{{ ticket.detail }}</p>
            <small>关联订单 {{ ticket.orderNo }}</small>
          </div>
          <aside>
            <b>{{ statusLabel(ticket.status) }}</b>
            <em>{{ ticket.processedAt ? formatDateTime(ticket.processedAt) : formatDateTime(ticket.createdAt) }}</em>
            <p v-if="ticket.adminNote">{{ ticket.adminNote }}</p>
          </aside>
        </article>
        <div v-if="!paymentTickets.length" class="order-empty compact">
          暂无售后记录。需要退款或支付协助时，请在具体订单上提交说明。
        </div>
      </div>
    </section>

    <div v-if="showTicketDialog" class="ticket-dialog-backdrop" @click="closeTicketDialog">
      <form class="ticket-dialog" @submit.prevent="submitTicket" @click.stop>
        <header>
          <div>
            <span>{{ ticketForm.type === "refund" ? "退款申请" : "支付工单" }}</span>
            <h3>{{ ticketForm.type === "refund" ? "说明退款原因" : "描述支付问题" }}</h3>
          </div>
          <button type="button" aria-label="关闭" @click="closeTicketDialog">×</button>
        </header>
        <label>
          关联订单
          <select v-model="ticketForm.orderNo" :disabled="Boolean(ticketForm.lockedOrderNo)">
            <option value="">请选择一笔订单</option>
            <option v-for="order in paymentOrders" :key="order.orderNo" :value="order.orderNo">
              {{ providerLabel(order.provider) }} · {{ formatCny(order.amount || 0) }} · {{ order.orderNo }}
            </option>
          </select>
        </label>
        <label>
          标题
          <input v-model.trim="ticketForm.subject" maxlength="80" placeholder="例如：微信支付后未到账" />
        </label>
        <label>
          具体说明
          <textarea v-model.trim="ticketForm.detail" maxlength="600" placeholder="请写清楚时间、订单、支付方式、希望如何处理。"></textarea>
        </label>
        <p v-if="ticketError" class="ticket-error">{{ ticketError }}</p>
        <footer>
          <button type="button" class="secondary-action" @click="closeTicketDialog">取消</button>
          <button type="submit" class="primary-action" :disabled="ticketSubmitting">
            {{ ticketSubmitting ? "提交中" : "提交" }}
          </button>
        </footer>
      </form>
    </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useAuthStore } from "../stores/auth";
import { useUsageStore } from "../stores/usage";
import { paperpilotApi } from "../services/paperpilotApi";

const usageStore = useUsageStore();
const authStore = useAuthStore();

const loading = ref(false);
const activeSubTab = ref("usage");
const activeChart = ref("cost");
const activeLogTab = ref("all");
const selectedScene = ref("");
const TOKEN_UNIT_PRICE = 0.02;
const selectedProvider = ref("alipay");
const paying = ref(false);
const paymentMessage = ref("");
const paymentStatus = ref("");
const rechargeAmount = ref(50);
const quickAmounts = [20, 50, 100, 200, 500];
const paymentOrders = ref([]);
const paymentTickets = ref([]);
const ordersLoading = ref(false);
const showTicketDialog = ref(false);
const ticketSubmitting = ref(false);
const ticketError = ref("");
const ticketForm = ref({
  type: "support",
  orderNo: "",
  lockedOrderNo: "",
  subject: "",
  detail: "",
});
const validRechargeAmount = computed(() => Number(rechargeAmount.value || 0) > 0);
const pendingOrderCount = computed(() => paymentOrders.value.filter((order) => ["pending_payment", "config_required"].includes(order.status)).length);
const openTicketCount = computed(() => paymentTickets.value.filter((ticket) => ticket.status === "open").length);
const processedTicketCount = computed(() => paymentTickets.value.filter((ticket) => ["processed", "rejected", "closed"].includes(ticket.status)).length);

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

const chartMetricLabel = computed(() => {
  if (activeChart.value === "cost") return "费用";
  if (activeChart.value === "calls") return "调用次数";
  return "Token 用量";
});

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

onMounted(async () => {
  await refreshUsage();
  await loadPaymentOrders();
});

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

async function createRechargeOrder() {
  if (!validRechargeAmount.value) return;
  paying.value = true;
  paymentMessage.value = "";
  paymentStatus.value = "";
  try {
    const order = await paperpilotApi.createPaymentOrder({
      planId: "custom-recharge",
      provider: selectedProvider.value,
      amount: Number(rechargeAmount.value || 0),
    });
    paymentStatus.value = order.status || "";
    paymentMessage.value = order.message || "订单已创建。";
    if (order.paymentUrl) {
      window.open(order.paymentUrl, "_blank", "noopener,noreferrer");
    }
    await loadPaymentOrders();
  } catch (error) {
    paymentStatus.value = "failed";
    paymentMessage.value = error?.response?.data?.message || "订单创建失败，请稍后重试。";
  } finally {
    paying.value = false;
  }
}

async function loadPaymentOrders() {
  ordersLoading.value = true;
  try {
    const result = await paperpilotApi.getPaymentOrders();
    paymentOrders.value = result.orders || [];
    paymentTickets.value = result.tickets || [];
  } catch (error) {
    paymentOrders.value = [];
    paymentTickets.value = [];
  } finally {
    ordersLoading.value = false;
  }
}

function openTicketDialog(type, orderNo = "") {
  ticketError.value = "";
  ticketForm.value = {
    type,
    orderNo,
    lockedOrderNo: orderNo,
    subject: type === "refund" ? "退款申请" : "支付问题咨询",
    detail: "",
  };
  showTicketDialog.value = true;
}

function closeTicketDialog() {
  if (ticketSubmitting.value) return;
  showTicketDialog.value = false;
}

async function submitTicket() {
  ticketError.value = "";
  if (!ticketForm.value.orderNo) {
    ticketError.value = "请先选择一笔具体充值订单。";
    return;
  }
  if (!ticketForm.value.subject) {
    ticketError.value = "请填写标题。";
    return;
  }
  if (ticketForm.value.detail.length < 6) {
    ticketError.value = "请把问题说明写具体一点。";
    return;
  }
  ticketSubmitting.value = true;
  try {
    await paperpilotApi.createPaymentTicket({
      type: ticketForm.value.type,
      orderNo: ticketForm.value.orderNo,
      subject: ticketForm.value.subject,
      detail: ticketForm.value.detail,
    });
    paymentMessage.value = ticketForm.value.type === "refund" ? "退款申请已提交，管理员会在后台处理。" : "工单已提交，管理员会在后台处理。";
    paymentStatus.value = "";
    await loadPaymentOrders();
    showTicketDialog.value = false;
  } catch (error) {
    paymentStatus.value = "failed";
    ticketError.value = error?.response?.data?.message || "工单提交失败，请稍后重试。";
  } finally {
    ticketSubmitting.value = false;
  }
}

function barHeight(value) {
  return Math.max(Number(value || 0) > 0 ? 4 : 0, Math.round((Number(value || 0) / maxDailyValue.value) * 100));
}

function formatChartValue(value) {
  if (activeChart.value === "cost") return formatMoney(value);
  if (activeChart.value === "calls") return `${Number(value || 0)} 次`;
  return formatTokens(value);
}

function chartTooltipText(item) {
  return `${item.label}，${chartMetricLabel.value} ${formatChartValue(item.value)}，费用 ${formatMoney(item.cost)}，调用 ${item.calls} 次，Token ${formatTokens(item.tokens)}`;
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

function formatCny(value) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function providerLabel(provider) {
  return provider === "wechat" ? "微信支付" : "支付宝";
}

function statusLabel(status) {
  return {
    config_required: "待配置",
    pending_payment: "待支付",
    paid: "已支付",
    open: "处理中",
    processed: "已处理",
    rejected: "已驳回",
    closed: "已关闭",
  }[status] || "处理中";
}

function formatDateTime(value) {
  if (!value) return "-";
  if (Array.isArray(value)) {
    const [y, m = 1, d = 1, h = 0, min = 0] = value;
    return `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")} ${String(h).padStart(2, "0")}:${String(min).padStart(2, "0")}`;
  }
  return String(value).replace("T", " ").slice(0, 16);
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

.usage-subnav {
  display: flex;
  gap: 6px;
  max-width: 1560px;
  margin: 0 auto 18px;
  padding: 6px;
  border: 1px solid #e4eaf2;
  border-radius: 13px;
  background: #fff;
  width: fit-content;
}

.usage-subnav button {
  min-width: 98px;
  height: 36px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: #526075;
  font: inherit;
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
}

.usage-subnav button.active {
  background: #1f5be3;
  color: #fff;
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
  position: relative;
  display: grid;
  grid-template-rows: 1fr auto;
  gap: 10px;
  min-width: 0;
  height: 100%;
  text-align: center;
  outline: none;
}

.bar-rail {
  display: flex;
  align-items: flex-end;
  min-height: 0;
}

.bar-rail i {
  position: relative;
  display: block;
  width: 100%;
  min-height: 0;
  background: #5868f6;
  transition: height .22s ease, filter .18s ease, transform .18s ease;
}

.bar-column:hover .bar-rail i,
.bar-column:focus-visible .bar-rail i {
  filter: drop-shadow(0 10px 18px rgba(88, 104, 246, .22));
  transform: translateY(-2px);
}

.bar-tooltip {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 12px);
  z-index: 5;
  display: grid;
  min-width: 172px;
  gap: 5px;
  padding: 11px 12px;
  border: 1px solid rgba(207, 218, 237, .92);
  border-radius: 12px;
  background: rgba(255, 255, 255, .97);
  box-shadow: 0 18px 38px rgba(20, 31, 51, .14);
  color: #1d2636;
  text-align: left;
  pointer-events: none;
  opacity: 0;
  transform: translate(-50%, 8px);
  transition: opacity .16s ease, transform .16s ease;
}

.bar-tooltip::after {
  position: absolute;
  left: 50%;
  bottom: -6px;
  width: 10px;
  height: 10px;
  border-right: 1px solid rgba(207, 218, 237, .92);
  border-bottom: 1px solid rgba(207, 218, 237, .92);
  background: rgba(255, 255, 255, .97);
  content: "";
  transform: translateX(-50%) rotate(45deg);
}

.bar-tooltip strong {
  color: #111827;
  font-size: 13px;
  font-weight: 900;
}

.bar-tooltip em {
  color: #3154df;
  font-size: 13px;
  font-style: normal;
  font-weight: 900;
}

.bar-tooltip small {
  color: #657085;
  font-size: 12px;
  font-weight: 700;
}

.bar-column:hover .bar-tooltip,
.bar-column:focus-visible .bar-tooltip {
  opacity: 1;
  transform: translate(-50%, 0);
}

.bar-column span {
  color: #3f4857;
  font-size: 12px;
}

.ledger-panel {
  margin-top: 22px;
  overflow: hidden;
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

.recharge-page-card {
  display: grid;
  grid-template-columns: minmax(320px, .78fr) minmax(420px, 1fr);
  gap: 28px;
  align-items: stretch;
  max-width: 1560px;
  margin: 0 auto 18px;
  border: 1px solid #e4eaf2;
  border-radius: 16px;
  background:
    radial-gradient(circle at 8% 12%, rgba(31, 91, 227, .08), transparent 28%),
    linear-gradient(135deg, #fff 0%, #f9fcff 100%);
  padding: 30px;
  box-shadow: 0 8px 18px rgba(18, 31, 53, .04);
}

.recharge-account-panel {
  display: grid;
  align-content: space-between;
  gap: 22px;
  min-height: 330px;
  border: 1px solid #dbe7fb;
  border-radius: 14px;
  background: linear-gradient(145deg, #f8fbff 0%, #eef6ff 100%);
  padding: 24px;
}

.account-balance-line {
  display: grid;
  gap: 8px;
}

.account-balance-line span,
.account-mini-grid small {
  color: #526075;
  font-size: 13px;
  font-weight: 850;
}

.account-balance-line strong {
  color: #101828;
  font-size: 38px;
  line-height: 1;
}

.account-mini-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.account-mini-grid div {
  display: grid;
  gap: 6px;
  border: 1px solid #dce8f8;
  border-radius: 12px;
  background: rgba(255, 255, 255, .72);
  padding: 14px;
}

.account-mini-grid b {
  color: #1f5be3;
  font-size: 22px;
}

.recharge-account-panel p {
  max-width: 62ch;
  margin: 0;
  color: #46566e;
  font-size: 14px;
  line-height: 1.7;
}

.amount-panel {
  display: grid;
  gap: 16px;
  max-width: none;
}

.amount-panel label {
  display: grid;
  gap: 8px;
  color: #344158;
  font-size: 13px;
  font-weight: 900;
}

.amount-input {
  display: flex;
  align-items: center;
  height: 58px;
  border: 1px solid #dce4ef;
  border-radius: 13px;
  background: #fff;
  overflow: hidden;
}

.amount-input span {
  padding-left: 16px;
  color: #64748b;
  font-size: 18px;
  font-weight: 900;
}

.amount-input input {
  width: 100%;
  height: 100%;
  border: 0;
  outline: 0;
  padding: 0 16px 0 8px;
  color: #111827;
  font: inherit;
  font-size: 24px;
  font-weight: 900;
}

.quick-amounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-amounts button {
  height: 36px;
  border: 1px solid #d9e1ec;
  border-radius: 999px;
  background: #fff;
  color: #40506a;
  padding: 0 14px;
  font: inherit;
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
}

.quick-amounts button.active {
  border-color: #1f5be3;
  background: #edf4ff;
  color: #1f5be3;
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

.primary-action {
  min-width: 112px;
  height: 42px;
  border-radius: 11px;
  font: inherit;
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
}

.primary-action {
  border: 0;
  background: #1f5be3;
  color: #fff;
}

.order-panel {
  max-width: 1560px;
  margin: 0 auto;
  border: 1px solid #e4eaf2;
  border-radius: 16px;
  background: #fff;
  padding: 24px;
  box-shadow: 0 10px 24px rgba(18, 31, 53, .05);
}

.order-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.order-panel-head p {
  margin: 0 0 5px;
  color: #1f5be3;
  font-size: 12px;
  font-weight: 900;
}

.order-panel-head h3 {
  margin: 0;
  color: #171d2a;
  font-size: 22px;
  line-height: 1.2;
}

.order-head-actions,
.order-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.secondary-action,
.order-actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  border: 1px solid #d9e5f7;
  border-radius: 10px;
  background: #fff;
  color: #2550b8;
  padding: 0 13px;
  font: inherit;
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
}

.secondary-action:hover,
.order-actions button:hover {
  border-color: #9fb8f7;
  background: #f5f8ff;
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(240px, 1fr));
  gap: 14px;
}

.order-card,
.order-empty {
  min-height: 146px;
  border: 1px solid #dce6f5;
  border-radius: 14px;
  background: linear-gradient(180deg, #fbfdff 0%, #f7fbff 100%);
  padding: 16px;
}

.order-card {
  display: grid;
  gap: 10px;
}

.order-card div:first-child {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.order-card span {
  color: #50627d;
  font-size: 13px;
  font-weight: 900;
}

.order-card strong {
  color: #111827;
  font-size: 22px;
}

.order-card p {
  margin: 0;
  overflow-wrap: anywhere;
  color: #40506a;
  font-size: 13px;
  line-height: 1.45;
}

.order-card em {
  color: #718096;
  font-size: 12px;
  font-style: normal;
}

.order-empty {
  display: grid;
  place-items: center;
  grid-column: 1 / -1;
  color: #718096;
  text-align: center;
  line-height: 1.7;
}

.ticket-list {
  margin-top: 18px;
  border-top: 1px solid #edf2f7;
  padding-top: 16px;
}

.ticket-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.ticket-list-head strong {
  color: #1f2937;
  font-size: 14px;
}

.ticket-list-head span {
  color: #526075;
  font-size: 12px;
  font-weight: 800;
}

.ticket-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(160px, 240px);
  gap: 16px;
  border: 1px solid #dfe7f2;
  border-radius: 13px;
  background: #fbfdff;
  padding: 14px 16px;
}

.ticket-card + .ticket-card {
  margin-top: 10px;
}

.ticket-card span,
.ticket-card small {
  color: #63728a;
  font-size: 12px;
  font-weight: 800;
}

.ticket-card strong {
  display: block;
  margin: 5px 0;
  color: #182235;
  font-size: 15px;
}

.ticket-card p {
  margin: 0 0 8px;
  color: #40506a;
  font-size: 13px;
  line-height: 1.6;
}

.ticket-card aside {
  display: grid;
  align-content: start;
  gap: 6px;
  border-radius: 10px;
  background: #f3f7fc;
  padding: 12px;
}

.ticket-card aside b {
  color: #1f5be3;
  font-size: 14px;
}

.ticket-card aside em {
  color: #718096;
  font-size: 12px;
  font-style: normal;
}

.ticket-card.ticket-processed {
  border-color: #bbf7d0;
  background: #f3fff8;
}

.ticket-card.ticket-processed aside {
  background: #e9fbf0;
}

.ticket-card.ticket-processed aside b {
  color: #15803d;
}

.ticket-card.ticket-rejected {
  border-color: #fecaca;
  background: #fff8f8;
}

.ticket-card.ticket-rejected aside {
  background: #fff1f2;
}

.ticket-card.ticket-rejected aside b {
  color: #b42318;
}

.order-empty.compact {
  min-height: 96px;
}

.ticket-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  background: rgba(15, 23, 42, .34);
  padding: 24px;
}

.ticket-dialog {
  width: min(620px, 100%);
  border: 1px solid #dce5f2;
  border-radius: 16px;
  background: #fff;
  padding: 22px;
  box-shadow: 0 22px 56px rgba(15, 23, 42, .18);
}

.ticket-dialog header,
.ticket-dialog footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.ticket-dialog header {
  margin-bottom: 18px;
}

.ticket-dialog header span {
  color: #1f5be3;
  font-size: 12px;
  font-weight: 900;
}

.ticket-dialog h3 {
  margin: 4px 0 0;
  color: #111827;
  font-size: 22px;
}

.ticket-dialog header button {
  width: 34px;
  height: 34px;
  border: 1px solid #d9e1ec;
  border-radius: 10px;
  background: #fff;
  color: #475569;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.ticket-dialog label {
  display: grid;
  gap: 8px;
  margin-bottom: 14px;
  color: #344158;
  font-size: 13px;
  font-weight: 900;
}

.ticket-dialog input,
.ticket-dialog select,
.ticket-dialog textarea {
  width: 100%;
  border: 1px solid #dce4ef;
  border-radius: 11px;
  background: #fff;
  color: #111827;
  padding: 11px 12px;
  font: inherit;
}

.ticket-dialog textarea {
  min-height: 130px;
  resize: vertical;
  line-height: 1.6;
}

.ticket-error {
  margin: 0 0 12px;
  border: 1px solid #fecaca;
  border-radius: 10px;
  background: #fff7f7;
  color: #b42318;
  padding: 10px 12px;
  font-size: 13px;
}

@media (max-width: 1120px) {
  .metric-strip,
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .recharge-page-card {
    grid-template-columns: 1fr;
  }

  .order-grid {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
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

  .usage-subnav {
    width: 100%;
  }

  .usage-subnav button {
    flex: 1;
  }

  .metric-strip,
  .summary-grid,
  .balance-card {
    grid-template-columns: 1fr;
  }

  .balance-card div:not(:first-child) {
    border-left: 0;
    padding-left: 0;
  }

  .recharge-entry {
    width: 100%;
  }

  .payment-picker {
    grid-template-columns: 1fr;
  }

  .recharge-page-card {
    padding: 16px;
  }

  .order-panel {
    padding: 16px;
  }

  .order-panel-head {
    align-items: stretch;
    flex-direction: column;
  }

  .order-grid {
    grid-template-columns: 1fr;
  }

  .account-mini-grid,
  .ticket-card {
    grid-template-columns: 1fr;
  }

  .ticket-dialog-backdrop {
    align-items: end;
    padding: 12px;
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
