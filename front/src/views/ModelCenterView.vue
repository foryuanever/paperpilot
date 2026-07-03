<template>
  <div class="usage-page">
    <header class="usage-header">
      <div>
        <p class="page-kicker">模型与额度</p>
        <h1>模型用量看板</h1>
        <p>这里统计已入账的模型调用：输入 Token、输出 Token 和账号额度都来自后端记录。</p>
      </div>
      <div class="header-actions">
        <span class="scope-pill">{{ usageStore.state.usageScope === "all" ? "全站最近记录" : "当前账号" }}</span>
        <button type="button" class="refresh-button" :disabled="loading" @click="refreshUsage">
          {{ loading ? "刷新中" : "刷新" }}
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
          <small>当前 MPM</small>
          <strong>{{ formatMoney(usageStore.state.mpm || 0) }}</strong>
          <em>按系统估算单价</em>
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
          <p>估算成本</p>
        </div>
        <div>
          <strong>{{ totalRequestsDisplay }}</strong>
          <p>总请求数</p>
        </div>
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

    <section class="active-model-panel" aria-label="当前计费模型范围">
      <div class="active-model-head">
        <div>
          <p>当前计费模型范围</p>
          <strong>这里显示正在被功能调用的真实模型配置，不把未发生的调用伪造成用量。</strong>
        </div>
        <span>{{ activeModels.length }} 条路由</span>
      </div>
      <div class="active-model-grid">
        <article v-for="model in activeModels" :key="model.scene" class="active-model-card">
          <div class="model-route-top">
            <span>{{ model.label }}</span>
            <em :class="{ muted: !model.configured }">{{ model.configured ? "已配置" : "未配置" }}</em>
          </div>
          <h3>{{ model.modelName || "unknown-model" }}</h3>
          <p>{{ model.providerName || "未知供应商" }}</p>
          <div class="model-route-meta">
            <span>{{ model.apiFormat || "openai_chat" }}</span>
            <strong>{{ formatTokens(model.recordedTokens || 0) }} Token</strong>
          </div>
          <small>{{ model.accountingRule }}</small>
        </article>
      </div>
    </section>

    <section class="chart-panel">
      <div class="panel-toolbar">
        <nav class="soft-tabs" aria-label="统计维度">
          <button type="button" :class="{ active: activeChart === 'cost' }" @click="activeChart = 'cost'">估算成本</button>
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
          模型名称
          <select v-model="selectedModel">
            <option value="">全部模型</option>
            <option v-for="model in modelOptions" :key="model" :value="model">{{ model }}</option>
          </select>
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
          <span>模型</span>
          <span>论文 / 任务</span>
          <span>输入</span>
          <span>输出</span>
          <span>Token 用量</span>
        </div>
        <div v-for="row in filteredCalls" :key="`${row.time}-${row.paper}-${row.tokens}`" class="usage-row" role="row">
          <span>{{ row.time || "-" }}</span>
          <span>{{ translateAction(row.action) }}</span>
          <span class="model-cell"><i></i>{{ row.model || "unknown-model" }}</span>
          <span class="paper-cell">{{ row.paper || "当前论文" }}</span>
          <span>{{ formatTokens(row.promptTokens || 0) }}</span>
          <span>{{ formatTokens(row.completionTokens || 0) }}</span>
          <strong>{{ formatTokens(row.tokens || 0) }}</strong>
        </div>
      </div>
      <p v-if="!filteredCalls.length" class="empty-text">
        {{ selectedModel && activeModelNames.includes(selectedModel) ? "该模型当前已配置，但还没有真实入账记录；组会 PPT 生成完成后会在这里显示用量。" : "当前筛选下没有调用记录。" }}
      </p>
      <footer class="table-footer">
        <span>显示最近 {{ filteredCalls.length }} 条已入账记录，Token 用量 = 输入 + 输出。</span>
      </footer>
      <aside class="accounting-note">
        <strong>关于 PPT Agent 用量</strong>
        <span>PPT Master 组会 PPT 会以“组会PPT Agent执行（估算）”入账并显示 GPT5.5；因为 Codex CLI 不返回供应商精确 token，系统按提示词、材料和日志做本地估算。</span>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useAuthStore } from "../stores/auth";
import { useUsageStore } from "../stores/usage";

const usageStore = useUsageStore();
const authStore = useAuthStore();

const loading = ref(false);
const activeChart = ref("cost");
const activeLogTab = ref("all");
const selectedModel = ref("");
const selectedScene = ref("");

const activeModels = computed(() => usageStore.state.activeModels || []);

const activeModelNames = computed(() => activeModels.value.map((row) => row.modelName).filter(Boolean));

const modelOptions = computed(() => [
  ...new Set([
    ...usageStore.state.recentCalls.map((row) => row.model).filter(Boolean),
    ...activeModelNames.value,
  ]),
]);

const sceneOptions = computed(() => [
  ...new Set(usageStore.state.recentCalls.map((row) => row.action).filter(Boolean)),
]);

const filteredCalls = computed(() => usageStore.state.recentCalls.filter((row) => {
  if (selectedModel.value && row.model !== selectedModel.value) return false;
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
  const cost = tokens * 0.02 / 1000;
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

const sceneMap = {
  translate: "学术翻译",
  analyze: "论文解析",
  summary: "汇总综述",
  report: "组会",
  qa: "论文问答",
  "组会汇报": "组会论文综述生成",
  "组会论文内容生成": "组会论文内容生成",
  "组会论文综述生成": "组会论文综述生成",
  "组会PPT Agent执行（估算）": "组会PPT Agent执行（估算）",
  "双栏翻译": "PDF双栏翻译",
  "PDF双栏翻译": "PDF双栏翻译",
  "论文问答": "论文问答",
  "论文选区提问": "论文选区提问",
};

function translateScene(value) {
  return sceneMap[value] || value || "-";
}

function translateAction(value) {
  return sceneMap[value] || value || "-";
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
  min-height: 38px;
  padding: 0 14px;
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: .55;
}

.metric-strip,
.summary-grid,
.active-model-panel,
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
  grid-template-columns: 62px repeat(3, 1fr);
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

.day-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px 24px;
}

.active-model-panel {
  margin-bottom: 22px;
  border: 1px solid #e4eaf2;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 18px rgba(18, 31, 53, .04);
}

.active-model-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px 12px;
}

.active-model-head p {
  margin: 0 0 6px;
  color: #182234;
  font-size: 16px;
  font-weight: 900;
}

.active-model-head strong {
  color: #667085;
  font-size: 13px;
  font-weight: 700;
}

.active-model-head > span {
  border-radius: 999px;
  background: #edf4ff;
  color: #2357d6;
  padding: 7px 11px;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.active-model-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 0 24px 22px;
}

.active-model-card {
  display: grid;
  gap: 10px;
  border: 1px solid #dfe7f2;
  border-radius: 14px;
  background: linear-gradient(180deg, #fbfdff 0%, #f7fbff 100%);
  padding: 18px;
}

.model-route-top,
.model-route-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.model-route-top span {
  color: #526075;
  font-size: 13px;
  font-weight: 900;
}

.model-route-top em {
  border-radius: 999px;
  background: #dcfce7;
  color: #087b4a;
  padding: 5px 9px;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.model-route-top em.muted {
  background: #f1f5f9;
  color: #64748b;
}

.active-model-card h3 {
  margin: 0;
  color: #182234;
  font-size: 22px;
  line-height: 1.15;
}

.active-model-card p {
  margin: 0;
  color: #607086;
  font-size: 13px;
  font-weight: 700;
  word-break: break-word;
}

.model-route-meta span {
  color: #7a8799;
  font-size: 12px;
  font-weight: 800;
}

.model-route-meta strong {
  color: #1f2a44;
  font-size: 15px;
}

.active-model-card small {
  color: #7a8799;
  font-size: 12px;
  line-height: 1.5;
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
  grid-template-columns: repeat(4, minmax(160px, 1fr));
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
  grid-template-columns: 150px 120px 180px minmax(240px, 1fr) 110px 110px 110px;
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

.model-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #505b6f;
  font-weight: 800;
}

.model-cell i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4ade80;
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

@media (max-width: 1120px) {
  .metric-strip,
  .summary-grid,
  .active-model-grid {
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
  .active-model-grid,
  .balance-card {
    grid-template-columns: 1fr;
  }

  .active-model-head,
  .model-route-top,
  .model-route-meta {
    align-items: flex-start;
    flex-direction: column;
  }

  .balance-card div:not(:first-child) {
    border-left: 0;
    padding-left: 0;
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
