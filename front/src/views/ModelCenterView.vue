<template>
  <div class="spatial-page models-spatial">
    <section class="spatial-chapter" style="padding-top:24px">
      <div class="spatial-chapter-inner">
        <span class="spatial-chapter-eyebrow">额度与计费</span>
        <h1 class="spatial-chapter-title">AI 额度与计费中心</h1>
        <p class="spatial-chapter-lead">查看平台 AI 服务消耗、计费记录，并购买适合科研工作的 Token 套餐。</p>

        <!-- Premium Tab Switcher -->
        <div class="spatial-nav-tabs">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'usage' }"
            @click="activeTab = 'usage'"
          >
            消耗与计费
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'billing' }"
            @click="activeTab = 'billing'"
          >
            套餐购买
          </button>
        </div>
      </div>
    </section>

    <!-- Tab 1: Models and Usage Dashboard -->
    <section v-if="activeTab === 'usage'" class="spatial-chapter-inner model-dashboard">
      <div class="usage-overview-strip">
        <div class="usage-quota-main">
          <span>本周期额度</span>
          <strong>{{ formatTokens(usageStore.tokenRemaining) }}</strong>
          <p>剩余 Token · 已使用 {{ usageStore.usagePercent }}%</p>
          <div class="usage-quota-meter">
            <div :style="{ width: usageStore.usagePercent + '%' }"></div>
          </div>
        </div>
        <div class="usage-kpi">
          <span>已用 / 总额</span>
          <strong>{{ formatTokens(usageStore.state.tokenUsed) }} / {{ formatTokens(usageStore.state.tokenQuota) }}</strong>
        </div>
        <div class="usage-kpi">
          <span>近 7 日</span>
          <strong>{{ formatTokens(usageStore.state.weekTokens) }}</strong>
        </div>
        <div class="usage-kpi">
          <span>今日调用</span>
          <strong>{{ todayCalls }}</strong>
        </div>
        <button class="usage-plan-button" @click="activeTab = 'billing'">
          <span>当前套餐</span>
          <strong>{{ usageStore.state.planName }}</strong>
        </button>
      </div>

      <div class="usage-workbench">
        <section class="usage-panel usage-panel-wide">
          <div class="model-panel-head">
            <div>
              <h2>近 7 日 Token 用量</h2>
              <p>按调用日期聚合，展示近期消耗峰值和低谷。</p>
            </div>
            <span class="toolbar-chip">{{ formatTokens(usageStore.state.weekTokens) }}</span>
          </div>
          <div class="usage-chart">
            <div v-for="item in usageStore.state.dailyUsage" :key="item.label" class="usage-chart-col">
              <div class="usage-chart-bar-wrap">
                <div class="usage-chart-bar" :style="{ height: barHeight(item.tokens) + '%' }"></div>
              </div>
              <span class="usage-chart-label">{{ item.label }}</span>
              <span class="usage-chart-value">{{ formatTokens(item.tokens) }}</span>
            </div>
          </div>
        </section>

        <section class="usage-panel">
          <div class="model-panel-head">
            <div>
              <h2>消耗构成</h2>
              <p>模型通道与场景拆分。</p>
            </div>
          </div>
          <div v-if="usageStore.state.modelBreakdown.length || usageStore.state.sceneBreakdown.length" class="usage-split-list">
            <div v-for="item in usageStore.state.modelBreakdown" :key="item.model" class="usage-split-row">
              <span>{{ displayPlanModelName }}</span>
              <strong>{{ item.share }}%</strong>
              <div><i :style="{ width: item.share + '%' }"></i></div>
            </div>
            <div v-for="item in usageStore.state.sceneBreakdown" :key="item.scene" class="usage-split-row muted">
              <span>{{ translateScene(item.scene) }}</span>
              <strong>{{ formatTokens(item.tokens) }}</strong>
              <div><i :style="{ width: item.share + '%' }"></i></div>
            </div>
          </div>
          <p v-else class="usage-empty-state">暂无 AI 调用记录。完成一次论文分析后，这里会显示模型、场景和 Token 构成。</p>
        </section>

        <section class="usage-panel">
          <div class="model-panel-head">
            <div>
              <h2>输入 / 输出</h2>
              <p>Prompt 与生成内容占比。</p>
            </div>
            <span class="toolbar-chip">估算</span>
          </div>
          <div class="token-composition compact">
            <div class="token-composition-ring" :style="{ '--prompt-deg': `${promptShare * 3.6}deg` }">
              <div class="token-composition-core">
                <strong>{{ formatTokens(usageStore.state.promptTokens + usageStore.state.completionTokens) }}</strong>
                <span>总计</span>
              </div>
            </div>
            <div class="token-composition-legend">
              <div class="composition-row">
                <span class="composition-dot prompt"></span>
                <strong>输入</strong>
                <span>{{ formatTokens(usageStore.state.promptTokens) }}</span>
              </div>
              <div class="composition-row">
                <span class="composition-dot completion"></span>
                <strong>输出</strong>
                <span>{{ formatTokens(usageStore.state.completionTokens) }}</span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <section class="usage-panel">
        <div class="model-panel-head">
          <h2>最近调用记录</h2>
          <span class="toolbar-chip">{{ usageStore.state.usageScope === "all" ? "全站最近" : "当前账号" }}</span>
        </div>
        <div v-if="usageStore.state.recentCalls.length" class="model-call-table">
          <div v-for="row in usageStore.state.recentCalls" :key="row.time + row.paper" class="model-call-row">
            <span>{{ row.time }}</span>
            <strong>{{ translateAction(row.action) }}</strong>
            <span class="model-call-paper">{{ row.paper }}</span>
            <span class="model-call-tokens">-{{ formatTokens(row.tokens) }}</span>
          </div>
        </div>
        <p v-else class="usage-empty-state">还没有可展示的调用记录。运行一次 AI 分析后会自动写入这里。</p>
      </section>

    </section>

    <!-- Tab 2: Billing packages and Pricing Center -->
    <section v-else-if="activeTab === 'billing'" class="spatial-chapter-inner billing-dashboard">
      <div class="billing-header-card spatial-glass-panel">
        <div class="quota-progress-section">
          <h3>账户额度分配</h3>
          <p class="quota-desc">
            当前套餐：<strong>{{ usageStore.state.planName }}</strong> ·
            剩余 Token <strong>{{ formatTokens(usageStore.tokenRemaining) }}</strong>
          </p>
          <div class="token-meter">
            <div class="token-meter-bar">
              <div class="token-meter-fill" :style="{ width: usageStore.usagePercent + '%' }"></div>
            </div>
            <span class="token-meter-label">已用 {{ formatTokens(usageStore.state.tokenUsed) }} / {{ formatTokens(usageStore.state.tokenQuota) }}</span>
          </div>
        </div>
        <div class="quota-summary-box">
          <span class="spatial-drift-label">账户 Token 剩余</span>
          <strong class="usage-big-number">{{ formatTokens(usageStore.tokenRemaining) }}</strong>
          <p class="spatial-drift-detail text-muted">额度将于 {{ usageStore.state.resetAt }} 重置</p>
        </div>
      </div>

      <h2 class="section-heading">科研订阅套餐</h2>
      <div class="pricing-rows">
        <section v-for="row in planRows" :key="row.cycle" class="pricing-row">
          <div class="pricing-row-title">
            <strong>{{ row.cycle }}</strong>
            <span>{{ row.summary }}</span>
          </div>
          <div class="pricing-grid">
            <article
              v-for="plan in row.plans"
              :key="plan.id"
              class="pricing-card"
              :class="{ featured: plan.highlight, active: usageStore.state.planId === plan.id }"
            >
              <span class="spatial-drift-label">{{ plan.tier }}</span>
              <h3>{{ plan.name }}</h3>
              <div class="pricing-amount">
                <strong>{{ plan.price }}</strong>
                <span>{{ plan.period }}</span>
              </div>
              <p class="pricing-quota">{{ formatTokens(plan.tokenQuota) }} Token</p>
              <ul class="pricing-features">
                <li v-for="item in plan.features" :key="item">{{ item }}</li>
              </ul>
              <div class="payment-button-row">
                <button class="payment-btn alipay" @click="selectPlan(plan, 'alipay')">
                  <span class="pay-logo alipay-logo">支</span>
                  <span>支付宝</span>
                </button>
                <button class="payment-btn wechat" @click="selectPlan(plan, 'wechat')">
                  <span class="pay-logo wechat-logo" aria-hidden="true">
                    <i></i><i></i>
                  </span>
                  <span>微信支付</span>
                </button>
              </div>
            </article>
          </div>
        </section>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { useScrollReveal } from "../composables/useScrollReveal";
import { billingPlans } from "../constants/pages";
import { useUsageStore } from "../stores/usage";
import { useAuthStore } from "../stores/auth";
import { paperpilotApi } from "../services/paperpilotApi";

useScrollReveal(".models-spatial");

const route = useRoute();
const usageStore = useUsageStore();
const authStore = useAuthStore();

const activeTab = ref("usage");
const displayPlanModelName = computed(() => {
  const name = usageStore.state.planName || "当前套餐";
  if (/Elite|课题组|年包|旗舰/.test(name)) return "旗舰精读模型通道";
  if (/Pro|深度|季包|进阶/.test(name)) return "Pro 深度阅读模型";
  if (/Plus|冲刺|月包/.test(name)) return "Plus 论文冲刺模型";
  return "Starter 基础分析模型";
});
const planRows = computed(() => {
  const summaries = {
    月包: "短期精读、课程论文和临时组会",
    季包: "开题准备、阶段综述和连续汇报",
    年包: "毕业论文、长期课题和团队协作",
  };
  return ["月包", "季包", "年包"]
    .map((cycle) => ({
      cycle,
      summary: summaries[cycle],
      plans: billingPlans.filter((plan) => plan.billingCycle === cycle),
    }))
    .filter((row) => row.plans.length);
});

onMounted(async () => {
  try {
    await usageStore.fetchSummary();
  } catch (error) {
    console.error("Failed to fetch usage summary", error);
  }
  if (route.query.tab === "billing") {
    activeTab.value = "billing";
  }
});

const maxDailyTokens = computed(() =>
  Math.max(...usageStore.state.dailyUsage.map((item) => item.tokens), 1),
);

const todayCalls = computed(() => {
  const today = new Date();
  const label = `${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
  return usageStore.state.recentCalls.filter((item) => String(item.time || "").startsWith(label)).length;
});
const promptShare = computed(() => {
  const prompt = Number(usageStore.state.promptTokens || 0);
  const completion = Number(usageStore.state.completionTokens || 0);
  const total = prompt + completion;
  if (!total) return 50;
  return Math.round((prompt / total) * 100);
});

function formatTokens(n) {
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`;
  if (n >= 1000) return `${Math.round(n / 1000)}K`;
  return String(n);
}

function barHeight(tokens) {
  return Math.max(8, Math.round((tokens / maxDailyTokens.value) * 100));
}

async function selectPlan(plan, provider) {
  try {
    const order = await paperpilotApi.createPaymentOrder({
      planId: plan.id,
      provider,
      amount: plan.price,
      tokenQuota: plan.tokenQuota,
      durationMonths: plan.durationMonths || 1,
    });
    if (order.paymentUrl) {
      window.open(order.paymentUrl, "_blank", "noopener,noreferrer");
      return;
    }
    authStore.addNotification({
      title: `${provider === "alipay" ? "支付宝" : "微信支付"}订单待配置`,
      desc: order.message || `订单号：${order.orderNo}`,
    });
  } catch (error) {
    authStore.addNotification({
      title: "创建支付订单失败",
      desc: error?.response?.data?.message || "请检查支付配置或稍后重试。",
    });
  }
}

const sceneMap = {
  translate: "学术翻译",
  analyze: "论文解析",
  summary: "汇总综述",
  report: "七章论文分析",
  qa: "论文问答",
};
function translateScene(s) {
  return sceneMap[s] || s;
}
function translateAction(a) {
  return sceneMap[a] || a;
}
</script>

<style scoped>
.models-spatial .spatial-chapter {
  margin: 0;
  padding-left: 0;
  padding-right: 0;
}

/* Premium Tab Switcher Styles */
.spatial-nav-tabs {
  display: inline-flex;
  gap: 8px;
  background: rgba(0, 0, 0, 0.04);
  padding: 4px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  margin-top: 24px;
  backdrop-filter: blur(8px);
}

.tab-btn {
  background: transparent;
  border: none;
  padding: 8px 20px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary, #8e8e93);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.tab-btn:hover {
  color: var(--text-main, #111);
}

.tab-btn.active {
  background: #ffffff;
  color: var(--spatial-accent, #0066ff);
  box-shadow: 0 4px 12px rgba(10, 10, 12, 0.08);
}

.model-dashboard,
.billing-dashboard {
  display: grid;
  gap: 18px;
  margin-top: 16px;
}

.usage-overview-strip {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) repeat(4, minmax(136px, auto));
  align-items: stretch;
  gap: 12px;
  overflow: hidden;
  padding: 16px;
  border: 1px solid #d8e6ff;
  border-radius: 16px;
  background: linear-gradient(135deg, #f7fbff 0%, #ffffff 62%);
  box-shadow: 0 18px 48px rgba(25, 42, 70, .055);
}

.usage-quota-main,
.usage-kpi,
.usage-plan-button {
  min-height: 94px;
}

.usage-quota-main {
  display: grid;
  align-content: center;
  padding: 12px 14px;
}

.usage-quota-main span,
.usage-kpi span {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.usage-quota-main strong {
  display: block;
  margin: 7px 0 4px;
  color: #0f172a;
  font-size: 32px;
  line-height: 1;
  letter-spacing: 0;
}

.usage-quota-main p {
  margin: 0 0 12px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.usage-quota-meter {
  height: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(15, 23, 42, .08);
}

.usage-quota-meter div {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2563eb, #14b8a6);
}

.usage-kpi {
  display: grid;
  align-content: center;
  gap: 9px;
  min-width: 0;
  padding: 16px 14px;
  border: 1px solid #edf2f8;
  border-radius: 12px;
  background: #ffffff;
}

.usage-kpi strong {
  color: #111827;
  font-size: 18px;
  line-height: 1.2;
}

.usage-plan-button {
  display: grid;
  align-content: center;
  gap: 8px;
  min-width: 148px;
  border: 1px solid #cfe0ff;
  border-radius: 12px;
  padding: 16px 18px;
  color: #1d4ed8;
  background: #ffffff;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.usage-plan-button span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.usage-plan-button strong {
  color: #1d4ed8;
  font-size: 15px;
  font-weight: 850;
}

.usage-plan-button:hover {
  background: #fff;
}

.usage-workbench {
  display: grid;
  grid-template-columns: minmax(520px, 1.45fr) minmax(340px, .95fr);
  gap: 18px;
}

.usage-panel {
  padding: 22px;
  border: 1px solid #e6edf5;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 16px 40px rgba(25, 42, 70, .045);
}

.usage-panel-wide {
  grid-row: span 2;
  min-width: 0;
  min-height: 344px;
}

.usage-empty-state {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.usage-split-list {
  display: grid;
  gap: 14px;
}

.usage-split-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px 12px;
  align-items: center;
  font-size: 13px;
  padding: 2px 0 8px;
  border-bottom: 1px solid #edf2f8;
}

.usage-split-row span {
  min-width: 0;
  overflow: hidden;
  color: #1f2937;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.usage-split-row strong {
  color: #475569;
  font-size: 12px;
}

.usage-split-row div {
  grid-column: 1 / -1;
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef2f7;
}

.usage-split-row i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #2563eb;
}

.usage-split-row.muted i {
  background: #16a34a;
}

.model-stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.model-stat-card {
  padding: 22px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 12px 32px rgba(10, 10, 12, 0.04);
}

.model-stat-primary {
  grid-column: span 1;
  background: linear-gradient(145deg, rgba(0, 102, 255, 0.06), rgba(255, 255, 255, 0.9));
}

.model-stat-value {
  display: block;
  margin-top: 10px;
  font-size: 2.2rem;
  letter-spacing: -0.04em;
}

.model-stat-sm {
  font-size: 1.3rem;
}

.model-stat-bar {
  margin-top: 12px;
  height: 6px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.model-stat-fill {
  height: 100%;
  border-radius: 999px;
  background: var(--spatial-accent, #0066ff);
}

.model-stat-meta {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: var(--spatial-gray, #8e8e93);
}

.model-stat-link {
  margin-top: 12px;
  display: inline-flex;
  font-size: 13px;
  padding: 8px 14px;
  min-height: auto;
}

.model-dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
}

.model-panel {
  padding: 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.model-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.model-panel-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 1.1rem;
  letter-spacing: 0;
}

.model-panel-head p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.usage-chart {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  min-height: 220px;
  padding: 20px 4px 2px;
  background:
    linear-gradient(to top, rgba(226, 232, 240, .8) 1px, transparent 1px) 0 20px / 100% 46px no-repeat,
    linear-gradient(to top, rgba(226, 232, 240, .54) 1px, transparent 1px) 0 66px / 100% 46px no-repeat,
    linear-gradient(to top, rgba(226, 232, 240, .36) 1px, transparent 1px) 0 112px / 100% 46px no-repeat;
}

.usage-chart-col {
  flex: 1;
  display: grid;
  gap: 6px;
  justify-items: center;
}

.usage-chart-bar-wrap {
  width: 100%;
  height: 152px;
  display: flex;
  align-items: flex-end;
  padding: 0 2px;
}

.usage-chart-bar {
  width: 100%;
  border-radius: 10px 10px 3px 3px;
  background: linear-gradient(180deg, #60a5fa, #2563eb);
  min-height: 8px;
  transition: height .24s ease;
}

.usage-chart-label,
.usage-chart-value {
  font-size: 11px;
  color: var(--spatial-gray, #8e8e93);
}

.model-breakdown-list {
  display: grid;
  gap: 16px;
}

.model-breakdown-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px 16px;
  align-items: center;
}

.model-breakdown-info {
  display: grid;
  gap: 2px;
}

.model-breakdown-info span {
  font-size: 12px;
  color: var(--spatial-gray, #8e8e93);
}

.model-breakdown-bar {
  grid-column: 1 / -1;
  height: 6px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.model-breakdown-fill {
  height: 100%;
  border-radius: 999px;
  background: rgba(0, 102, 255, 0.5);
}

.model-breakdown-fill.scene-fill {
  background: rgba(15, 157, 88, 0.55);
}

.model-breakdown-tokens {
  font-size: 13px;
  font-weight: 600;
}

.token-composition {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 24px;
  align-items: center;
}

.token-composition.compact {
  grid-template-columns: 120px 1fr;
  gap: 18px;
}

.token-composition-ring {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background:
    conic-gradient(#0066ff 0deg,
      #0066ff var(--prompt-deg, 180deg),
      #34c759 var(--prompt-deg, 180deg),
      #34c759 360deg);
  position: relative;
}

.token-composition.compact .token-composition-ring {
  width: 120px;
  height: 120px;
}

.token-composition-ring::before {
  content: "";
  position: absolute;
  inset: 18px;
  border-radius: 50%;
  background: #fff;
}

.token-composition.compact .token-composition-ring::before {
  inset: 13px;
}

.token-composition-core {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: 4px;
}

.token-composition-core strong {
  font-size: 1.4rem;
}

.token-composition-core span,
.composition-row span:last-child {
  color: var(--spatial-gray, #8e8e93);
  font-size: 12px;
}

.token-composition-legend {
  display: grid;
  gap: 14px;
}

.composition-row {
  display: grid;
  grid-template-columns: 14px auto 1fr;
  gap: 10px;
  align-items: center;
}

.composition-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.composition-dot.prompt {
  background: #0066ff;
}

.composition-dot.completion {
  background: #34c759;
}

.action-pill-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.action-pill {
  min-width: 160px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(15, 23, 42, 0.08);
  display: grid;
  gap: 5px;
}

.action-pill span {
  color: var(--spatial-gray, #8e8e93);
  font-size: 12px;
}

.model-showcase-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.model-showcase-card {
  text-align: left;
  padding: 20px;
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.model-showcase-card:hover,
.model-showcase-card.active {
  border-color: rgba(0, 102, 255, 0.35);
  box-shadow: 0 12px 28px rgba(0, 102, 255, 0.08);
}

.model-showcase-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.model-status {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 999px;
}

.model-status.online {
  background: rgba(52, 199, 89, 0.12);
  color: #2fa45e;
}

.model-showcase-card h3 {
  margin: 0 0 8px;
  font-size: 1.1rem;
}

.model-showcase-card p {
  margin: 0;
  font-size: 13px;
  color: var(--spatial-gray, #666);
  line-height: 1.6;
}

.model-showcase-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  font-size: 11px;
  color: var(--spatial-gray, #888);
}

.model-call-table {
  display: grid;
  gap: 0;
}

.model-call-row {
  display: grid;
  grid-template-columns: 100px 100px minmax(0, 1fr) auto;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  font-size: 13px;
  align-items: center;
}

.model-call-paper {
  color: var(--spatial-gray, #666);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-call-tokens {
  font-weight: 600;
  color: #c1322b;
}

@media (max-width: 720px) {
  .usage-overview-strip,
  .usage-workbench {
    grid-template-columns: 1fr;
  }

  .model-call-row {
    grid-template-columns: 1fr auto;
  }

  .model-call-paper {
    grid-column: 1 / -1;
  }

  .token-composition {
    grid-template-columns: 1fr;
    justify-items: center;
  }
}

.model-config-drawer {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.model-config-drawer summary {
  padding: 16px 20px;
  cursor: pointer;
  font-weight: 600;
  list-style: none;
}

.model-config-drawer summary::-webkit-details-marker {
  display: none;
}

.model-config-drawer :deep(.reader-panel) {
  margin: 0 20px 20px;
}

/* Tab 2: Billing Styles */
.billing-header-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 16px 48px rgba(10, 10, 12, 0.04);
  flex-wrap: wrap;
  gap: 24px;
}

.quota-progress-section {
  flex: 1;
  min-width: 280px;
}

.quota-progress-section h3 {
  margin: 0 0 12px;
  font-size: 1.4rem;
}

.quota-desc {
  font-size: 14px;
  color: var(--text-secondary, #666);
}

.token-meter {
  margin-top: 20px;
  max-width: 460px;
}

.token-meter-bar {
  height: 8px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.token-meter-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #0066ff, #4d7cff);
}

.token-meter-label {
  display: block;
  margin-top: 8px;
  font-size: 12.5px;
  color: var(--spatial-gray, #8e8e93);
}

.quota-summary-box {
  background: rgba(0, 102, 255, 0.04);
  border: 1px solid rgba(0, 102, 255, 0.08);
  padding: 24px;
  border-radius: 20px;
  text-align: right;
  min-width: 220px;
}

.usage-big-number {
  display: block;
  margin-top: 10px;
  font-size: 2.8rem;
  letter-spacing: -0.04em;
  color: var(--spatial-accent, #0066ff);
}

.spatial-drift-detail {
  font-size: 11.5px;
  margin: 8px 0 0;
  color: var(--spatial-gray, #8e8e93);
}

.section-heading {
  margin: 32px 0 16px;
  font-size: 1.4rem;
  font-weight: 700;
}

.pricing-rows {
  display: grid;
  gap: 18px;
}

.pricing-row {
  padding: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.66);
}

.pricing-row-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}

.pricing-row-title strong {
  font-size: 1.25rem;
  color: #111827;
}

.pricing-row-title span {
  font-size: 13px;
  color: var(--spatial-gray, #8e8e93);
}

.pricing-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.pricing-card {
  padding: 20px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 12px 28px rgba(10, 10, 12, 0.035);
}

.pricing-card.featured {
  border-color: rgba(0, 102, 255, 0.2);
  box-shadow: 0 14px 34px rgba(0, 102, 255, 0.08);
}

.pricing-card.active {
  outline: 2px solid rgba(0, 102, 255, 0.35);
}

.pricing-card h3 {
  margin: 10px 0 0;
  font-size: 1.15rem;
}

.pricing-amount {
  margin-top: 12px;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.pricing-amount strong {
  font-size: 1.7rem;
  letter-spacing: -0.03em;
}

.pricing-amount span {
  color: var(--spatial-gray, #8e8e93);
  font-size: 14px;
}

.pricing-quota {
  margin: 8px 0 0;
  font-size: 14px;
  color: var(--spatial-accent, #0066ff);
  font-weight: 600;
}

.pricing-features {
  margin: 14px 0;
  padding-left: 18px;
  color: var(--spatial-gray, #555);
  font-size: 12.8px;
  line-height: 1.65;
}

.payment-button-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 18px;
}

.payment-btn {
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 10px;
  background: #fff;
  color: #111827;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.payment-btn:hover {
  background: #f8fafc;
}

.payment-btn.alipay {
  border-color: rgba(22, 119, 255, 0.35);
  color: #1677ff;
}

.payment-btn.wechat {
  border-color: rgba(7, 193, 96, 0.35);
  color: #079455;
}

.pay-logo {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 20px;
  border-radius: 6px;
  color: #fff;
  font-size: 13px;
  font-weight: 900;
  line-height: 1;
}

.alipay-logo {
  background: #1677ff;
}

.wechat-logo {
  position: relative;
  background: #07c160;
}

.wechat-logo i {
  position: absolute;
  width: 10px;
  height: 7px;
  border-radius: 999px;
  background: #fff;
}

.wechat-logo i:first-child {
  left: 4px;
  top: 5px;
}

.wechat-logo i:last-child {
  right: 4px;
  bottom: 5px;
  opacity: 0.82;
}

@media (max-width: 980px) {
  .pricing-grid {
    grid-template-columns: 1fr;
  }
}
</style>
