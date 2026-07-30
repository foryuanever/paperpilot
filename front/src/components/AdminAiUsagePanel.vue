<template>
  <div class="ai-usage-ledger-panel">
    <section class="ai-usage-ledger">
      <div class="ledger-header">
        <div>
          <span class="pool-kicker">Usage Ledger</span>
          <h4>AI 调用明细</h4>
          <p>记录本站用户在每个模块中调用的模型、输入 Token、输出 Token、费用估算与失败原因。</p>
        </div>
        <div class="ledger-header-actions">
          <button class="spatial-btn spatial-btn-ghost compact-btn danger-btn" :disabled="loading || clearing" @click="clearAiUsageCalls">
            {{ clearing ? "清空中..." : "清空调用记录" }}
          </button>
          <button class="spatial-btn spatial-btn-accent compact-btn" :disabled="loading" @click="loadAiUsageCalls">
            {{ loading ? "刷新中..." : "刷新账本" }}
          </button>
        </div>
      </div>

      <div class="ledger-filters">
        <label>
          <span>用户检索</span>
          <input v-model.trim="filters.keyword" type="search" placeholder="用户名 / 邮箱 / 用户ID" @keyup.enter="resetPageAndLoad" />
        </label>
        <label>
          <span>模块</span>
          <select v-model="filters.scene" @change="resetPageAndLoad">
            <option value="">全部模块</option>
            <option v-for="scene in sceneOptions" :key="scene.value" :value="scene.value">{{ scene.label }}</option>
          </select>
        </label>
        <label>
          <span>模型</span>
          <input v-model.trim="filters.model" type="search" placeholder="例如 gpt / qwen / deepseek" @keyup.enter="resetPageAndLoad" />
        </label>
        <label>
          <span>状态</span>
          <select v-model="filters.status" @change="resetPageAndLoad">
            <option value="">全部状态</option>
            <option value="success">成功</option>
            <option value="failed">失败</option>
          </select>
        </label>
        <label>
          <span>开始日期</span>
          <input v-model="filters.startDate" type="date" @change="resetPageAndLoad" />
        </label>
        <label>
          <span>结束日期</span>
          <input v-model="filters.endDate" type="date" @change="resetPageAndLoad" />
        </label>
        <button class="spatial-btn spatial-btn-ghost compact-btn" @click="resetPageAndLoad">检索</button>
      </div>

      <div class="ledger-summary">
        <article>
          <span>筛选调用</span>
          <strong>{{ formatNumber(summary.matchedCalls || total) }} 次</strong>
        </article>
        <article>
          <span>输入 Token</span>
          <strong>{{ formatNumber(summary.inputTokens) }}</strong>
        </article>
        <article>
          <span>输出 Token</span>
          <strong>{{ formatNumber(summary.outputTokens) }}</strong>
        </article>
        <article>
          <span>失败调用</span>
          <strong>{{ formatNumber(summary.failed) }} 次</strong>
        </article>
        <article class="cost-card">
          <span>筛选总花费</span>
          <strong>¥{{ formatMoney(summary.cost) }}</strong>
        </article>
      </div>

      <div class="ledger-table-wrap">
        <table class="ledger-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>用户</th>
              <th class="ledger-module-col">模块</th>
              <th>模型</th>
              <th>输入</th>
              <th>输出</th>
              <th>总量</th>
              <th>费用</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" :class="{ failed: row.status === 'failed' }">
              <td class="ledger-time">{{ row.time }}</td>
              <td>
                <strong>{{ row.username || "未知用户" }}</strong>
                <small>{{ row.userEmail || `ID ${row.userId || "—"}` }}</small>
              </td>
              <td class="ledger-module-col">
                <strong>{{ row.sceneLabel }}</strong>
                <small>{{ row.action }}</small>
              </td>
              <td class="ledger-model">
                <strong>{{ row.model }}</strong>
                <small>{{ row.paper }}</small>
              </td>
              <td>{{ formatNumber(row.promptTokens) }}</td>
              <td>{{ formatNumber(row.completionTokens) }}</td>
              <td>{{ formatNumber(row.totalTokens) }}</td>
              <td>¥{{ formatMoney(row.chargeAmount) }}</td>
              <td>
                <span class="ledger-status" :class="row.status">{{ row.status === "failed" ? "失败" : "成功" }}</span>
                <small v-if="row.latencyMs">{{ row.latencyMs }} ms</small>
                <small v-if="row.status === 'failed' && row.fallbackResolved" class="ledger-fallback">
                  已切换 {{ row.fallbackModel }} 成功
                </small>
                <small v-if="row.status === 'failed'" class="ledger-error">{{ row.errorMessage || "调用失败" }}</small>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="9" class="ledger-empty">{{ loading ? "正在读取调用记录..." : "暂无调用记录，真实模型调用后会自动出现在这里。" }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-controls ledger-pagination">
        <span>{{ paginationText(total, page, pageSize) }}</span>
        <div>
          <button class="action-btn text-btn" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
          <strong>{{ page }} / {{ pageCount }}</strong>
          <button class="action-btn text-btn" :disabled="page >= pageCount || loading" @click="changePage(page + 1)">下一页</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";
import { useDialogStore } from "../stores/dialog";

const dialogStore = useDialogStore();

const page = ref(1);
const pageSize = ref(20);
const rows = ref([]);
const total = ref(0);
const totalPages = ref(1);
const loading = ref(false);
const clearing = ref(false);
const summary = ref({ inputTokens: 0, outputTokens: 0, totalTokens: 0, failed: 0, matchedCalls: 0, cost: 0 });

const filters = ref({
  keyword: "",
  scene: "",
  model: "",
  status: "",
  startDate: "",
  endDate: ""
});

const sceneOptions = [
  { value: "paper_review", label: "论文综述" },
  { value: "paper_qa", label: "AI论文问答" },
  { value: "meeting_deck", label: "PPT生成" },
  { value: "forum_moderation", label: "AI发帖审核" },
  { value: "topic_research", label: "选题研究" },
  { value: "translate", label: "全文翻译" },
  { value: "summary", label: "论文综述旧记录" },
  { value: "qa", label: "问答旧记录" },
  { value: "analyze", label: "解析旧记录" },
  { value: "report", label: "汇报旧记录" }
];

const pageCount = computed(() => Math.max(1, Number(totalPages.value) || Math.ceil(total.value / pageSize.value)));

async function loadAiUsageCalls() {
  loading.value = true;
  try {
    const data = await paperpilotApi.getAdminAiUsageCalls({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.value.keyword || undefined,
      scene: filters.value.scene || undefined,
      model: filters.value.model || undefined,
      status: filters.value.status || undefined,
      startDate: filters.value.startDate || undefined,
      endDate: filters.value.endDate || undefined
    });
    rows.value = data.rows || [];
    total.value = Number(data.total) || 0;
    totalPages.value = Number(data.totalPages) || Math.ceil(total.value / pageSize.value);
    summary.value = {
      inputTokens: 0,
      outputTokens: 0,
      totalTokens: 0,
      failed: 0,
      matchedCalls: 0,
      cost: 0,
      ...(data.summary || {})
    };
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "AI 调用明细加载失败");
  } finally {
    loading.value = false;
  }
}

function resetPageAndLoad() {
  page.value = 1;
  loadAiUsageCalls();
}

function changePage(targetPage) {
  page.value = Math.min(Math.max(1, targetPage), pageCount.value);
  loadAiUsageCalls();
}

async function clearAiUsageCalls() {
  const ok = await dialogStore.confirm("确认清空全部 AI 调用记录吗？清空后会保留本次清空动作的系统日志。", {
    title: "清空 AI 调用记录",
    confirmText: "清空",
    cancelText: "取消",
    danger: true,
  });
  if (!ok) return;
  clearing.value = true;
  try {
    await paperpilotApi.clearAdminAiUsageCalls();
    page.value = 1;
    await loadAiUsageCalls();
    dialogStore.alert("AI 调用记录已清空。");
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "清空 AI 调用记录失败");
  } finally {
    clearing.value = false;
  }
}

function formatNumber(value) {
  return (Number(value) || 0).toLocaleString("zh-CN");
}

function formatMoney(value) {
  const amount = Number(value || 0);
  if (amount > 0 && amount < 1) return amount.toFixed(3).replace(/0+$/, "").replace(/\.$/, "");
  return amount.toLocaleString("zh-CN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function paginationText(totalCount, currentPage, size) {
  if (!totalCount) return "暂无记录";
  const start = (currentPage - 1) * size + 1;
  const end = Math.min(totalCount, currentPage * size);
  return `显示 ${start}-${end} 条，共 ${totalCount} 条`;
}

watch(pageSize, () => {
  page.value = 1;
  loadAiUsageCalls();
});

onMounted(() => {
  loadAiUsageCalls();
});
</script>

<style scoped>
.ai-usage-ledger-panel {
  width: 100%;
}

.ai-usage-ledger {
  padding: 24px;
  border-radius: 20px;
  background: var(--spatial-surface);
  border: 1px solid var(--spatial-line);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.03);
  transition: background 0.3s ease, border-color 0.3s ease;
}

.ledger-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  margin-bottom: 20px;
}

.ledger-header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.ledger-header-actions .danger-btn {
  color: #fecaca;
  border-color: rgba(248, 113, 113, .38);
  background: rgba(127, 29, 29, .16);
}

.pool-kicker {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--spatial-accent);
  letter-spacing: 0.05em;
  margin-bottom: 4px;
}

.ledger-header h4 {
  margin: 0;
  color: var(--spatial-graphite);
  font-size: 1.15rem;
  font-weight: 700;
}

.ledger-header p {
  margin: 6px 0 0;
  color: var(--spatial-gray);
  font-size: 0.85rem;
  line-height: 1.5;
}

.ledger-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
  padding: 16px;
  border: 1px solid var(--spatial-line);
  border-radius: 14px;
  background: var(--spatial-warm-2);
  align-items: flex-end;
}

.ledger-filters label {
  display: grid;
  gap: 6px;
  flex: 1 1 160px;
}

.ledger-filters span {
  color: var(--spatial-gray);
  font-size: 0.7rem;
  font-weight: 700;
}

.ledger-filters input,
.ledger-filters select {
  width: 100%;
  min-height: 38px;
  padding: 0 12px;
  border: 1px solid var(--spatial-line);
  border-radius: 10px;
  color: var(--spatial-graphite);
  background: var(--spatial-surface);
  font: inherit;
  font-size: 0.82rem;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.ledger-filters input:focus,
.ledger-filters select:focus {
  border-color: var(--spatial-accent);
  box-shadow: 0 0 0 3px var(--spatial-accent-soft);
}

.ledger-filters > button {
  min-height: 38px;
  flex: 0 0 auto;
}

.ledger-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.ledger-summary article {
  padding: 14px 16px;
  border: 1px solid var(--spatial-line);
  border-radius: 12px;
  background: var(--spatial-surface);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.01);
  transition: transform 0.2s ease;
}

.ledger-summary article:hover {
  transform: translateY(-2px);
}

.ledger-summary span {
  display: block;
  color: var(--spatial-gray);
  font-size: 0.72rem;
  font-weight: 700;
}

.ledger-summary strong {
  display: block;
  margin-top: 6px;
  color: var(--spatial-graphite);
  font-size: 1.15rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.ledger-summary .cost-card {
  border-color: var(--spatial-accent-soft);
  background: linear-gradient(135deg, var(--spatial-accent-soft), rgba(16, 185, 129, 0.06));
}

.ledger-table-wrap {
  margin-top: 20px;
  overflow-x: auto;
  border: 1px solid var(--spatial-line);
  border-radius: 14px;
  background: var(--spatial-surface);
}

.ledger-table {
  width: 100%;
  min-width: 1100px;
  border-collapse: collapse;
  color: var(--spatial-graphite);
  font-size: 0.8rem;
}

.ledger-table th {
  padding: 12px 14px;
  color: var(--spatial-gray);
  font-size: 0.72rem;
  font-weight: 700;
  text-align: left;
  background: var(--spatial-warm-2);
  border-bottom: 1px solid var(--spatial-line);
}

.ledger-table td {
  padding: 14px;
  border-bottom: 1px solid var(--spatial-line);
  vertical-align: middle;
}

.ledger-table tr:last-child td {
  border-bottom: none;
}

.ledger-table tr.failed {
  background: rgba(239, 68, 68, 0.02);
}

.ledger-table strong,
.ledger-table small {
  display: block;
}

.ledger-table strong {
  color: var(--spatial-graphite);
  font-weight: 600;
}

.ledger-table small {
  margin-top: 4px;
  color: var(--spatial-gray);
  line-height: 1.4;
}

.ledger-time {
  color: var(--spatial-graphite);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.ledger-module-col {
  white-space: nowrap;
  min-width: 90px;
}

.ledger-module-col strong,
.ledger-module-col small {
  white-space: nowrap !important;
}

.ledger-model {
  max-width: 300px;
}

.ledger-model strong,
.ledger-model small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ledger-status {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 700;
}

.ledger-status.success {
  color: #047857;
  background: #dcfce7;
}

.ledger-status.failed {
  color: #b91c1c;
  background: #fee2e2;
}

.ledger-error {
  margin-top: 4px;
  max-width: 260px;
  color: #b91c1c;
}

.ledger-fallback {
  margin-top: 4px;
  color: #047857;
  font-weight: 700;
}

.ledger-empty {
  padding: 48px !important;
  text-align: center;
  color: var(--spatial-gray);
  font-size: 0.85rem;
}

.ledger-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  color: var(--spatial-gray);
  font-size: 0.8rem;
}

.ledger-pagination strong {
  color: var(--spatial-graphite);
  margin: 0 12px;
}

.action-btn {
  border: none;
  background: none;
  font-size: 0.82rem;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.text-btn {
  color: var(--spatial-accent);
}

.text-btn:hover:not(:disabled) {
  opacity: 0.8;
  text-decoration: underline;
}

.text-btn:disabled {
  color: var(--spatial-gray);
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
