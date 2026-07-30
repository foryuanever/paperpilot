<template>
  <div class="admin-monitoring-panel">
    <header class="monitoring-header spatial-glass-panel">
      <div>
        <h4>系统监控与风险把控</h4>
        <p>所选日期按 24 小时统计；实时风险表继续监测近 10 分钟异常账号、异常 IP 和接口压力。</p>
      </div>
      <div class="header-actions">
        <label class="date-filter">
          <span>查看日期</span>
          <input v-model="selectedDate" type="date" @change="fetchAnalytics" />
        </label>
        <button class="quick-date-btn" type="button" @click="setDate(-1)">昨天</button>
        <button class="quick-date-btn" type="button" @click="setDate(0)">今天</button>
        <span class="live-pulse"><span class="pulse-dot"></span>{{ loading ? "同步中" : "实时监控中" }}</span>
        <button class="spatial-btn spatial-btn-accent compact-btn" :disabled="loading" @click="fetchAnalytics">
          {{ loading ? "刷新中..." : "刷新数据" }}
        </button>
      </div>
    </header>

    <section class="live-metrics-grid">
      <article class="metric-card spatial-glass-panel">
        <span class="metric-label">实时在线</span>
        <strong class="metric-value">{{ currentOnline }}<small>人</small></strong>
        <p>近 2 分钟有业务接口心跳的账号数。</p>
      </article>
      <article class="metric-card spatial-glass-panel">
        <span class="metric-label">近 1 分钟请求</span>
        <strong class="metric-value blue">{{ trafficSummary.requestsLastMinute || 0 }}<small>次</small></strong>
        <p>排除监控接口后的真实业务请求。</p>
      </article>
      <article class="metric-card spatial-glass-panel">
        <span class="metric-label">近 10 分钟错误率</span>
        <strong class="metric-value amber">{{ formatPercent(trafficSummary.errorRateLastTenMinutes) }}</strong>
        <p>HTTP 4xx/5xx 占近 10 分钟请求比例。</p>
      </article>
      <article class="metric-card spatial-glass-panel risk-card">
        <span class="metric-label">风险拦截</span>
        <strong class="metric-value red">{{ trafficSummary.blockedLastTenMinutes || 0 }}<small>次</small></strong>
        <p>近 10 分钟被封禁规则拒绝的请求。</p>
      </article>
    </section>

    <section class="charts-layout">
      <article class="chart-container spatial-glass-panel wide">
        <ChartHeader :title="`${selectedDateLabel} 24 小时业务趋势`" accent="blue" subtitle="请求数、错误数、阻断数、独立 IP" />
        <div class="traffic-legend">
          <span><i class="legend-dot req"></i>请求</span>
          <span><i class="legend-dot err"></i>错误</span>
          <span><i class="legend-dot block"></i>拦截</span>
          <span><i class="legend-dot ip"></i>独立 IP</span>
        </div>
        <div class="svg-wrapper traffic-chart-shell">
          <svg viewBox="0 0 920 300" class="monitoring-svg traffic-svg" @mousemove="handleTrafficMouseMove" @mouseleave="hoveredTrafficIndex = null">
            <defs>
              <linearGradient id="trafficAreaGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#3b82f6" stop-opacity="0.20" />
                <stop offset="100%" stop-color="#3b82f6" stop-opacity="0" />
              </linearGradient>
            </defs>
            <rect x="56" y="24" width="834" height="210" rx="10" class="chart-plot-bg" />
            <line v-for="g in 6" :key="'tg-' + g" x1="56" :y1="24 + (g - 1) * 42" x2="890" :y2="24 + (g - 1) * 42" class="grid-line" />
            <line v-for="x in 7" :key="'tx-' + x" :x1="trafficX(Math.min((x - 1) * 4, 23))" y1="24" :x2="trafficX(Math.min((x - 1) * 4, 23))" y2="234" class="grid-line vertical" />
            <path :d="trafficAreaPath" fill="url(#trafficAreaGrad)" />
            <path :d="trafficIpPath" fill="none" stroke="#14b8a6" stroke-width="2" stroke-linecap="round" stroke-dasharray="2 7" />
            <path :d="trafficErrorPath" fill="none" stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-dasharray="5 5" />
            <path :d="trafficBlockedPath" fill="none" stroke="#ef4444" stroke-width="2" stroke-linecap="round" />
            <path :d="trafficLinePath" fill="none" stroke="#3b82f6" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round" />
            <line v-if="hoveredTrafficPoint" :x1="hoveredTrafficPoint.x" y1="24" :x2="hoveredTrafficPoint.x" y2="234" class="hover-guide" />
            <circle v-for="(pt, idx) in trafficPoints" :key="'tc-' + idx" :cx="pt.x" :cy="pt.y" :r="hoveredTrafficIndex === idx ? 5.5 : 3" class="data-circle blue" />
            <text v-for="g in 6" :key="'ty-' + g" x="16" :y="29 + (g - 1) * 42" class="axis-label">{{ trafficYLabel(g - 1) }}</text>
            <text v-for="x in 7" :key="'tl-' + x" :x="trafficX(Math.min((x - 1) * 4, 23))" y="268" class="axis-label center">{{ realtimeTraffic[Math.min((x - 1) * 4, realtimeTraffic.length - 1)]?.time }}</text>
          </svg>
          <div v-if="hoveredTraffic" class="chart-tooltip" :style="tooltipStyle(hoveredTrafficPoint, 920, 300)">
            <strong>{{ hoveredTraffic.time }}</strong>
            <span>请求数：{{ hoveredTraffic.requests }} 次</span>
            <span>错误数：{{ hoveredTraffic.errors }} 次</span>
            <span>阻断数：{{ hoveredTraffic.blocked }} 次</span>
            <span>平均耗时：{{ hoveredTraffic.avgLatencyMs || 0 }} ms</span>
            <span>独立 IP：{{ hoveredTraffic.uniqueIps || 0 }}</span>
          </div>
        </div>
      </article>

      <article class="chart-container spatial-glass-panel">
        <ChartHeader title="今日活跃用户" accent="green" subtitle="按小时去重" />
        <LineChart
          :rows="onlineUsersData"
          value-key="count"
          color="#10b981"
          unit="人"
          label="活跃用户"
        />
      </article>

      <article class="chart-container spatial-glass-panel">
        <ChartHeader title="今日 AI 调用" accent="purple" subtitle="问答 / 综述 / PPT / 其他" />
        <div class="svg-wrapper">
          <svg viewBox="0 0 430 220" class="monitoring-svg" @mousemove="handleAiMouseMove" @mouseleave="hoveredAiIndex = null">
            <line v-for="g in 5" :key="'ag-' + g" x1="42" :y1="18 + (g - 1) * 40" x2="410" :y2="18 + (g - 1) * 40" class="grid-line" />
            <g v-for="(bar, idx) in aiBars" :key="'ai-' + idx">
              <rect :x="hourX(idx) - 5" :y="178 - bar.totalH" width="10" :height="bar.otherH" fill="#64748b" rx="1" />
              <rect :x="hourX(idx) - 5" :y="178 - bar.pptBase" width="10" :height="bar.pptH" fill="#f472b6" rx="1" />
              <rect :x="hourX(idx) - 5" :y="178 - bar.reviewBase" width="10" :height="bar.reviewH" fill="#a855f7" />
              <rect :x="hourX(idx) - 5" :y="178 - bar.chatH" width="10" :height="bar.chatH" fill="#60a5fa" />
            </g>
            <text v-for="g in 5" :key="'ay-' + g" x="8" :y="23 + (g - 1) * 40" class="axis-label">{{ aiYLabel(g - 1) }}</text>
            <text v-for="x in 4" :key="'ax-' + x" :x="hourX((x - 1) * 7)" y="205" class="axis-label center">{{ (x - 1) * 7 }}:00</text>
          </svg>
          <div v-if="hoveredAi" class="chart-tooltip" :style="tooltipStyle({ x: hourX(hoveredAiIndex), y: 70 }, 430, 220)">
            <strong>{{ hoveredAi.time }}</strong>
            <span>总调用：{{ hoveredAi.total }} 次</span>
            <span>问答：{{ hoveredAi.chat }} 次</span>
            <span>综述：{{ hoveredAi.review }} 次</span>
            <span>PPT：{{ hoveredAi.ppt }} 次</span>
            <span>其他：{{ hoveredAi.other || 0 }} 次</span>
          </div>
        </div>
      </article>

      <article class="chart-container spatial-glass-panel">
        <ChartHeader title="今日翻译负载" accent="cyan" subtitle="请求量与字符量" />
        <LineChart
          :rows="translationsData"
          value-key="requests"
          secondary-key="charCount"
          color="#06b6d4"
          unit="次"
          label="翻译请求"
          secondary-label="字符量"
        />
      </article>
    </section>

    <section class="security-grid">
      <RiskTable
        title="高频 IP 风险"
        subtitle="近 10 分钟"
        :rows="topIps"
        row-key="ip"
        type="ip"
        @ban-ip="executeBanIp"
        @unban-ip="executeUnbanIp"
      />
      <RiskTable
        title="高频账号风险"
        subtitle="近 10 分钟"
        :rows="topUsers"
        row-key="userId"
        type="user"
        @ban-user="executeBanUser"
        @unban-user="executeUnbanUser"
      />
    </section>

    <section class="ops-grid">
      <article class="security-card spatial-glass-panel">
        <header class="sec-card-header">
          <div>
            <h5>接口热点</h5>
            <span>近 10 分钟调用最多的业务接口</span>
          </div>
        </header>
        <table class="monitoring-sec-table">
          <thead>
            <tr>
              <th>接口</th>
              <th>次数</th>
              <th>错误</th>
              <th>耗时</th>
              <th>IP</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in endpointHotspots" :key="row.endpoint">
              <td><code class="endpoint-code">{{ row.endpoint }}</code></td>
              <td class="num">{{ row.count }}</td>
              <td class="num">{{ row.errors }}</td>
              <td class="num">{{ row.avgLatencyMs }}ms</td>
              <td class="num">{{ row.uniqueIps }}</td>
            </tr>
            <tr v-if="!endpointHotspots.length">
              <td colspan="5" class="empty-sec-row">暂无业务接口访问数据</td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="security-card spatial-glass-panel">
        <header class="sec-card-header">
          <div>
            <h5>安全事件</h5>
            <span>拦截、高频、手动封禁和解封记录</span>
          </div>
        </header>
        <div class="audit-logs-wrap">
          <div v-for="log in securityLogs" :key="log.id" class="audit-log-item" :class="String(log.type || '').toLowerCase()">
            <time>{{ formatTime(log.timestamp) }}</time>
            <span class="log-type-tag">{{ getLogTypeLabel(log.type) }}</span>
            <strong>{{ log.target }}</strong>
            <p>{{ log.message }}</p>
          </div>
          <div v-if="!securityLogs.length" class="empty-sec-row">暂无安全事件</div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, onUnmounted, ref } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";
import { useDialogStore } from "../stores/dialog";

const ChartHeader = defineComponent({
  props: {
    title: String,
    subtitle: String,
    accent: { type: String, default: "blue" },
  },
  setup(props) {
    return () => h("header", { class: "chart-header" }, [
      h("div", [
        h("h5", props.title),
        h("span", props.subtitle),
      ]),
      h("i", { class: ["chart-accent", props.accent] }),
    ]);
  },
});

const LineChart = defineComponent({
  props: {
    rows: { type: Array, default: () => [] },
    valueKey: { type: String, required: true },
    secondaryKey: { type: String, default: "" },
    color: { type: String, default: "#3b82f6" },
    label: { type: String, default: "数值" },
    secondaryLabel: { type: String, default: "辅助值" },
    unit: { type: String, default: "" },
  },
  setup(props) {
    const hovered = ref(null);
    const width = 430;
    const height = 220;
    const left = 42;
    const right = 410;
    const top = 18;
    const bottom = 178;
    const xFor = (index) => left + index * ((right - left) / Math.max(1, props.rows.length - 1));
    const maxValue = computed(() => Math.max(1, ...props.rows.map(row => Number(row?.[props.valueKey] || 0))));
    const yFor = (value) => bottom - (Number(value || 0) / maxValue.value) * (bottom - top);
    const points = computed(() => props.rows.map((row, index) => ({ x: xFor(index), y: yFor(row?.[props.valueKey]), row, index })));
    const path = computed(() => points.value.map((pt, index) => `${index === 0 ? "M" : "L"} ${pt.x} ${pt.y}`).join(" "));
    const area = computed(() => points.value.length ? `${path.value} L ${points.value.at(-1).x} ${bottom} L ${points.value[0].x} ${bottom} Z` : "");
    const handleMove = (event) => {
      const rect = event.currentTarget.getBoundingClientRect();
      const ratio = (event.clientX - rect.left) / rect.width;
      const index = Math.round(ratio * (props.rows.length - 1));
      hovered.value = index >= 0 && index < props.rows.length ? index : null;
    };
    return () => h("div", { class: "svg-wrapper" }, [
      h("svg", {
        viewBox: `0 0 ${width} ${height}`,
        class: "monitoring-svg",
        onMousemove: handleMove,
        onMouseleave: () => { hovered.value = null; },
      }, [
        h("line", { x1: left, y1: top, x2: left, y2: bottom, class: "axis-line" }),
        ...Array.from({ length: 5 }, (_, index) => h("line", { x1: left, y1: top + index * 40, x2: right, y2: top + index * 40, class: "grid-line" })),
        h("path", { d: area.value, fill: props.color, opacity: "0.12" }),
        h("path", { d: path.value, fill: "none", stroke: props.color, "stroke-width": "2.5", "stroke-linecap": "round" }),
        ...points.value.map((pt, index) => h("circle", { cx: pt.x, cy: pt.y, r: hovered.value === index ? 5 : 2.5, fill: props.color, class: "data-circle" })),
        ...Array.from({ length: 5 }, (_, index) => h("text", { x: 8, y: 23 + index * 40, class: "axis-label" }, String(Math.round(maxValue.value - index * maxValue.value / 4)))),
        ...[0, 7, 14, 21].map(index => h("text", { x: xFor(index), y: 205, class: "axis-label center" }, `${index}:00`)),
      ]),
      hovered.value !== null && points.value[hovered.value] ? h("div", {
        class: "chart-tooltip",
        style: {
          left: `${Math.min(86, Math.max(14, points.value[hovered.value].x / width * 100))}%`,
          top: `${Math.max(12, points.value[hovered.value].y / height * 100 - 8)}%`,
        },
      }, [
        h("strong", points.value[hovered.value].row.time),
        h("span", `${props.label}：${points.value[hovered.value].row[props.valueKey] || 0}${props.unit}`),
        props.secondaryKey ? h("span", `${props.secondaryLabel}：${formatNumber(points.value[hovered.value].row[props.secondaryKey] || 0)}`) : null,
        points.value[hovered.value].row.avgLatencyMs ? h("span", `平均耗时：${points.value[hovered.value].row.avgLatencyMs}ms`) : null,
      ]) : null,
    ]);
  },
});

const RiskTable = defineComponent({
  props: {
    title: String,
    subtitle: String,
    rows: { type: Array, default: () => [] },
    type: { type: String, default: "ip" },
  },
  emits: ["ban-ip", "unban-ip", "ban-user", "unban-user"],
  setup(props, { emit }) {
    const targetOf = (row) => props.type === "ip" ? row.ip : row.userId;
    const nameOf = (row) => props.type === "ip" ? row.username : row.username;
    const action = (row) => {
      const target = targetOf(row);
      if (props.type === "ip") emit(row.banned ? "unban-ip" : "ban-ip", target);
      else emit(row.banned ? "unban-user" : "ban-user", target);
    };
    return () => h("article", { class: "security-card spatial-glass-panel" }, [
      h("header", { class: "sec-card-header" }, [
        h("div", [h("h5", props.title), h("span", props.subtitle)]),
      ]),
      h("table", { class: "monitoring-sec-table" }, [
        h("thead", h("tr", [
          h("th", props.type === "ip" ? "访问来源" : "用户账号"),
          h("th", "风险"),
          h("th", "请求"),
          h("th", "耗时"),
          h("th", "最后接口"),
          h("th", "操作"),
        ])),
        h("tbody", props.rows.length ? props.rows.map(row => h("tr", { key: targetOf(row) }, [
          h("td", [h("strong", { class: "source-main" }, String(targetOf(row))), h("small", nameOf(row) || "未知")]),
          h("td", h("span", { class: ["risk-pill", row.riskLevel || "normal"] }, riskLabel(row.riskLevel))),
          h("td", [h("b", { class: "num" }, `${row.count || 0}`), h("small", `${row.rpm || 0} 次/分`) ]),
          h("td", h("span", { class: "num" }, `${row.avgLatencyMs || 0}ms`)),
          h("td", h("code", { class: "endpoint-code" }, row.lastUrl || "-")),
          h("td", h("button", { class: ["sec-action-btn", row.banned ? "unban-btn" : "ban-btn"], onClick: () => action(row) }, row.banned ? "解封" : "封禁")),
        ])) : [h("tr", h("td", { colspan: 6, class: "empty-sec-row" }, "暂无风险对象"))]),
      ]),
    ]);
  },
});

const dialogStore = useDialogStore();

const loading = ref(false);
const onlineUsersData = ref([]);
const aiCallsData = ref([]);
const translationsData = ref([]);
const currentOnline = ref(0);
const securityLogs = ref([]);
const topIps = ref([]);
const topUsers = ref([]);
const realtimeTraffic = ref([]);
const endpointHotspots = ref([]);
const trafficSummary = ref({});
const todayAiTokens = ref(0);
const todayAiFailures = ref(0);
const todayTranslationFailures = ref(0);
const selectedDate = ref(toDateInputValue(new Date()));

const hoveredTrafficIndex = ref(null);
const hoveredAiIndex = ref(null);
let refreshIntervalId = null;

const todayAiCalls = computed(() => aiCallsData.value.reduce((sum, row) => sum + Number(row.total || 0), 0));
const todayTranslationChars = computed(() => translationsData.value.reduce((sum, row) => sum + Number(row.charCount || 0), 0));
const securityEventCount = computed(() => securityLogs.value.length);
const selectedDateLabel = computed(() => {
  const today = toDateInputValue(new Date());
  if (selectedDate.value === today) return "今天";
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  if (selectedDate.value === toDateInputValue(yesterday)) return "昨天";
  return selectedDate.value || "所选日期";
});

const maxTraffic = computed(() => Math.max(
  1,
  ...realtimeTraffic.value.map(row => Number(row.requests || 0)),
  ...realtimeTraffic.value.map(row => Number(row.errors || 0)),
  ...realtimeTraffic.value.map(row => Number(row.blocked || 0)),
  ...realtimeTraffic.value.map(row => Number(row.uniqueIps || 0)),
));
const trafficX = (index) => 56 + index * (834 / Math.max(1, realtimeTraffic.value.length - 1));
const trafficY = (value) => 234 - (Number(value || 0) / maxTraffic.value) * 210;
const trafficPoints = computed(() => realtimeTraffic.value.map((row, index) => ({ x: trafficX(index), y: trafficY(row.requests), row })));
const trafficErrorPoints = computed(() => realtimeTraffic.value.map((row, index) => ({ x: trafficX(index), y: trafficY(row.errors), row })));
const trafficBlockedPoints = computed(() => realtimeTraffic.value.map((row, index) => ({ x: trafficX(index), y: trafficY(row.blocked), row })));
const trafficIpPoints = computed(() => realtimeTraffic.value.map((row, index) => ({ x: trafficX(index), y: trafficY(row.uniqueIps), row })));
const pathFrom = (points) => points.map((pt, index) => `${index === 0 ? "M" : "L"} ${pt.x} ${pt.y}`).join(" ");
const trafficLinePath = computed(() => pathFrom(trafficPoints.value));
const trafficErrorPath = computed(() => pathFrom(trafficErrorPoints.value));
const trafficBlockedPath = computed(() => pathFrom(trafficBlockedPoints.value));
const trafficIpPath = computed(() => pathFrom(trafficIpPoints.value));
const trafficAreaPath = computed(() => trafficPoints.value.length ? `${trafficLinePath.value} L ${trafficPoints.value.at(-1).x} 234 L ${trafficPoints.value[0].x} 234 Z` : "");
const trafficYLabel = (index) => Math.round(maxTraffic.value - index * maxTraffic.value / 5);
const hoveredTraffic = computed(() => hoveredTrafficIndex.value === null ? null : realtimeTraffic.value[hoveredTrafficIndex.value]);
const hoveredTrafficPoint = computed(() => hoveredTrafficIndex.value === null ? null : trafficPoints.value[hoveredTrafficIndex.value]);

const hourX = (index) => 42 + index * (368 / 23);
const maxAiCalls = computed(() => Math.max(1, ...aiCallsData.value.map(row => Number(row.total || 0))));
const aiBars = computed(() => aiCallsData.value.map(row => {
  const chatH = Number(row.chat || 0) / maxAiCalls.value * 160;
  const reviewH = Number(row.review || 0) / maxAiCalls.value * 160;
  const pptH = Number(row.ppt || 0) / maxAiCalls.value * 160;
  const otherH = Number(row.other || 0) / maxAiCalls.value * 160;
  return {
    chatH,
    reviewH,
    pptH,
    otherH,
    reviewBase: chatH + reviewH,
    pptBase: chatH + reviewH + pptH,
    totalH: chatH + reviewH + pptH + otherH,
  };
}));
const aiYLabel = (index) => Math.round(maxAiCalls.value - index * maxAiCalls.value / 4);
const hoveredAi = computed(() => hoveredAiIndex.value === null ? null : aiCallsData.value[hoveredAiIndex.value]);

function handleTrafficMouseMove(event) {
  const rect = event.currentTarget.getBoundingClientRect();
  const ratio = (event.clientX - rect.left) / rect.width;
  const index = Math.round(ratio * (realtimeTraffic.value.length - 1));
  hoveredTrafficIndex.value = index >= 0 && index < realtimeTraffic.value.length ? index : null;
}

function handleAiMouseMove(event) {
  const rect = event.currentTarget.getBoundingClientRect();
  const ratio = (event.clientX - rect.left) / rect.width;
  const index = Math.round(ratio * 23);
  hoveredAiIndex.value = index >= 0 && index < aiCallsData.value.length ? index : null;
}

function tooltipStyle(point, width, height) {
  if (!point) return {};
  return {
    left: `${Math.min(86, Math.max(14, point.x / width * 100))}%`,
    top: `${Math.max(12, point.y / height * 100 - 8)}%`,
  };
}

async function fetchAnalytics() {
  loading.value = true;
  try {
    const data = await paperpilotApi.getMonitoringAnalytics({ date: selectedDate.value });
    onlineUsersData.value = data.onlineUsers || [];
    aiCallsData.value = data.aiCalls || [];
    translationsData.value = data.translations || [];
    currentOnline.value = data.realtimeOnline || 0;
    securityLogs.value = data.securityLogs || [];
    topIps.value = data.topIps || [];
    topUsers.value = data.topUsers || [];
    realtimeTraffic.value = data.hourlyTraffic || data.realtimeTraffic || [];
    endpointHotspots.value = data.endpointHotspots || [];
    trafficSummary.value = data.trafficSummary || {};
    todayAiTokens.value = data.todayAiTokens || 0;
    todayAiFailures.value = data.todayAiFailures || 0;
    todayTranslationFailures.value = data.todayTranslationFailures || 0;
  } catch (error) {
    console.error("Failed to load monitoring analytics", error);
  } finally {
    loading.value = false;
  }
}

function toDateInputValue(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function setDate(offset) {
  const date = new Date();
  date.setDate(date.getDate() + offset);
  selectedDate.value = toDateInputValue(date);
  fetchAnalytics();
}

async function executeBanIp(ip) {
  const reason = await dialogStore.prompt("请输入封禁该 IP 的原因：", { placeholder: "高频请求 / 异常扫描 / 撞库风险" });
  if (reason === null) return;
  await paperpilotApi.banIp(ip, reason || "管理员手动封禁");
  dialogStore.toast("IP 已封禁");
  fetchAnalytics();
}

async function executeUnbanIp(ip) {
  await paperpilotApi.unbanIp(ip);
  dialogStore.toast("IP 已解封");
  fetchAnalytics();
}

async function executeBanUser(userId) {
  const reason = await dialogStore.prompt("请输入封禁该账号的原因：", { placeholder: "疯狂请求 / 异常自动化调用 / 风控处置" });
  if (reason === null) return;
  await paperpilotApi.banUser(userId, reason || "管理员手动封禁");
  dialogStore.toast("账号已封禁");
  fetchAnalytics();
}

async function executeUnbanUser(userId) {
  await paperpilotApi.unbanUser(userId);
  dialogStore.toast("账号已解封");
  fetchAnalytics();
}

function formatTime(timestamp) {
  return new Date(timestamp).toLocaleString("zh-CN", { hour12: false });
}

function formatNumber(num) {
  return Number(num || 0).toLocaleString();
}

function formatPercent(value) {
  return `${Number(value || 0).toFixed(1)}%`;
}

function riskLabel(level) {
  if (level === "critical") return "严重";
  if (level === "high") return "高危";
  if (level === "watch") return "观察";
  return "正常";
}

function getLogTypeLabel(type) {
  switch (type) {
    case "IP_ABUSE": return "IP 高频";
    case "USER_ABUSE": return "账号高频";
    case "MANUAL_BAN": return "手动封禁";
    case "MANUAL_UNBAN": return "手动解封";
    case "ATTACK_ATTEMPT": return "拒绝访问";
    default: return type || "事件";
  }
}

onMounted(() => {
  fetchAnalytics();
  refreshIntervalId = setInterval(fetchAnalytics, 10000);
});

onUnmounted(() => {
  if (refreshIntervalId) clearInterval(refreshIntervalId);
});
</script>

<style scoped>
.admin-monitoring-panel {
  display: grid;
  gap: 18px;
  font-variant-numeric: tabular-nums;
}

.monitoring-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  border: 1px solid rgba(93, 135, 255, 0.18);
  border-radius: 10px;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.92), rgba(17, 24, 39, 0.82)),
    radial-gradient(circle at 8% 0%, rgba(59, 130, 246, 0.14), transparent 32%);
}

.monitoring-header h4 {
  margin: 0 0 6px;
  color: var(--spatial-text);
  font-size: 22px;
  line-height: 1.2;
}

.monitoring-header p,
.metric-card p,
.sec-card-header span {
  margin: 0;
  color: var(--spatial-muted);
  font-size: 12px;
  line-height: 1.6;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.date-filter {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  height: 36px;
  padding: 0 10px 0 12px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 8px;
  color: var(--spatial-muted);
  background: rgba(15, 23, 42, 0.46);
  font-size: 12px;
  font-weight: 800;
}

.date-filter input {
  width: 138px;
  border: 0;
  outline: none;
  color: var(--spatial-text);
  background: transparent;
  font: inherit;
}

.date-filter input::-webkit-calendar-picker-indicator {
  filter: invert(0.85);
  opacity: 0.72;
}

.quick-date-btn {
  height: 36px;
  padding: 0 13px;
  border: 1px solid rgba(96, 165, 250, 0.22);
  border-radius: 8px;
  color: #bfdbfe;
  background: rgba(30, 41, 59, 0.46);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.quick-date-btn:hover {
  border-color: rgba(96, 165, 250, 0.42);
  background: rgba(37, 99, 235, 0.16);
}

.live-pulse {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--spatial-muted);
  font-size: 12px;
  white-space: nowrap;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.72);
}

.live-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  padding: 17px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.15);
}

.metric-label {
  color: var(--spatial-muted);
  font-size: 11px;
  font-weight: 750;
}

.metric-value {
  display: block;
  margin: 8px 0 4px;
  color: var(--spatial-text);
  font-size: 31px;
  line-height: 1;
}

.metric-value small {
  margin-left: 4px;
  color: var(--spatial-muted);
  font-size: 13px;
}

.metric-value.blue { color: #60a5fa; }
.metric-value.amber { color: #f59e0b; }
.metric-value.red { color: #ef4444; }

.charts-layout,
.security-grid,
.ops-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-container,
.security-card {
  min-width: 0;
  padding: 18px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(15, 23, 42, 0.72);
}

.chart-container.wide {
  grid-column: 1 / -1;
  padding: 20px 22px 16px;
}

.chart-header,
.sec-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.chart-header h5,
.sec-card-header h5 {
  margin: 0 0 4px;
  color: var(--spatial-text);
  font-size: 15px;
}

.chart-header span {
  color: var(--spatial-muted);
  font-size: 12px;
}

.chart-accent {
  width: 10px;
  height: 10px;
  margin-top: 4px;
  border-radius: 50%;
  background: #3b82f6;
}

.chart-accent.green { background: #10b981; }
.chart-accent.purple { background: #a855f7; }
.chart-accent.cyan { background: #06b6d4; }

.traffic-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: -2px 0 10px;
  color: var(--spatial-muted);
  font-size: 12px;
  font-weight: 750;
}

.traffic-legend span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.legend-dot {
  width: 18px;
  height: 3px;
  border-radius: 999px;
  background: #3b82f6;
}

.legend-dot.err { background: #f59e0b; }
.legend-dot.block { background: #ef4444; }
.legend-dot.ip { background: #14b8a6; }

.svg-wrapper {
  position: relative;
}

.traffic-chart-shell {
  padding: 8px 8px 0;
  border: 1px solid rgba(96, 165, 250, 0.1);
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.68), rgba(2, 6, 23, 0.1));
}

.monitoring-svg {
  width: 100%;
  height: auto;
  overflow: visible;
}

.chart-plot-bg {
  fill: rgba(2, 6, 23, 0.28);
  stroke: rgba(148, 163, 184, 0.08);
}

.grid-line,
.axis-line {
  stroke: rgba(148, 163, 184, 0.16);
  stroke-width: 1;
}

.grid-line.vertical {
  stroke-dasharray: 4 6;
  opacity: 0.55;
}

.hover-guide {
  stroke: rgba(226, 232, 240, 0.34);
  stroke-width: 1;
  stroke-dasharray: 4 4;
}

.axis-label {
  fill: var(--spatial-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 9px;
}

.axis-label.center {
  text-anchor: middle;
}

.data-circle {
  fill: currentColor;
  transition: r 120ms ease;
}

.data-circle.blue {
  color: #3b82f6;
}

.chart-tooltip {
  position: absolute;
  z-index: 20;
  min-width: 170px;
  display: grid;
  gap: 4px;
  padding: 11px 12px;
  border: 1px solid rgba(96, 165, 250, 0.32);
  border-radius: 10px;
  color: #e5eefb;
  background: rgba(9, 14, 28, 0.98);
  box-shadow: 0 18px 45px rgba(2, 6, 23, 0.46);
  pointer-events: none;
  transform: translate(-50%, -100%);
  font-size: 12px;
}

.chart-tooltip strong {
  color: #fff;
}

.monitoring-sec-table {
  width: 100%;
  border-collapse: collapse;
}

.monitoring-sec-table th,
.monitoring-sec-table td {
  padding: 10px 8px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.13);
  color: var(--spatial-text);
  font-size: 12px;
  vertical-align: middle;
}

.monitoring-sec-table th {
  color: var(--spatial-muted);
  font-size: 11px;
  font-weight: 800;
  text-align: left;
}

.monitoring-sec-table small {
  display: block;
  margin-top: 3px;
  color: var(--spatial-muted);
  font-size: 10px;
}

.source-main,
.num,
.endpoint-code,
.audit-log-item time,
.audit-log-item strong {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.endpoint-code {
  display: inline-block;
  max-width: 280px;
  overflow: hidden;
  color: #93c5fd;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.risk-pill {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  color: #cbd5e1;
  background: rgba(100, 116, 139, 0.16);
  font-size: 11px;
  font-weight: 800;
}

.risk-pill.watch { color: #fbbf24; background: rgba(245, 158, 11, 0.12); }
.risk-pill.high { color: #fb923c; background: rgba(249, 115, 22, 0.14); }
.risk-pill.critical { color: #f87171; background: rgba(239, 68, 68, 0.14); }

.sec-action-btn {
  height: 30px;
  padding: 0 11px;
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.ban-btn {
  color: #fecaca;
  background: rgba(239, 68, 68, 0.12);
  border-color: rgba(239, 68, 68, 0.24);
}

.unban-btn {
  color: #a7f3d0;
  background: rgba(16, 185, 129, 0.12);
  border-color: rgba(16, 185, 129, 0.24);
}

.audit-logs-wrap {
  display: grid;
  gap: 8px;
  max-height: 355px;
  overflow: auto;
  padding-right: 4px;
}

.audit-log-item {
  display: grid;
  grid-template-columns: 128px 78px 120px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  padding: 9px 10px;
  border: 1px solid rgba(148, 163, 184, 0.13);
  border-left: 3px solid #64748b;
  border-radius: 8px;
  color: var(--spatial-text);
  background: rgba(15, 23, 42, 0.18);
  font-size: 12px;
}

.audit-log-item.ip_abuse,
.audit-log-item.user_abuse {
  border-left-color: #f59e0b;
}

.audit-log-item.manual_ban,
.audit-log-item.attack_attempt {
  border-left-color: #ef4444;
}

.audit-log-item.manual_unban {
  border-left-color: #10b981;
}

.audit-log-item p {
  margin: 0;
  min-width: 0;
  overflow: hidden;
  color: var(--spatial-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-type-tag {
  color: #93c5fd;
  font-weight: 800;
}

.empty-sec-row {
  padding: 24px 0 !important;
  color: var(--spatial-muted) !important;
  text-align: center;
}

@media (max-width: 1180px) {
  .live-metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .charts-layout,
  .security-grid,
  .ops-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .monitoring-header,
  .header-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .live-metrics-grid {
    grid-template-columns: 1fr;
  }

  .audit-log-item {
    grid-template-columns: 1fr;
  }
}
</style>
