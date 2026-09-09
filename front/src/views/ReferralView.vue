<template>
  <div class="spatial-page referral-page">
    <!-- 头部统计卡片 -->
    <section class="referral-stats-grid">
      <article v-for="card in statsCards" :key="card.label" class="referral-stat-card">
        <span class="referral-icon" v-html="icons[card.icon]"></span>
        <div class="stat-content">
          <strong>{{ card.value }}</strong>
          <span>{{ card.label }}</span>
        </div>
      </article>
    </section>

    <!-- 邀请流程图解 -->
    <section class="referral-flow">
      <h2>邀请奖励流程</h2>
      <div class="referral-rule-row">
        <article v-for="step in flowSteps" :key="step.title" class="referral-rule-item">
          <span class="referral-icon small" v-html="icons[step.icon]"></span>
          <div class="step-text">
            <strong>{{ step.index }}. {{ step.title }}</strong>
            <span>{{ step.desc }}</span>
          </div>
        </article>
      </div>
    </section>

    <!-- 邀请链接与邀请码卡片 -->
    <section class="invite-panel">
      <header class="invite-header">
        <h2>我的专属邀请</h2>
        <button type="button" class="text-action" @click="generateCode">+ 新建邀请码</button>
      </header>

      <div class="invite-code-card">
        <div class="invite-card-left">
          <div class="invite-card-head">
            <span v-html="icons.ticket"></span>
            <strong>专属激活邀请码 (一次性使用)</strong>
          </div>
          <div class="invite-code-value">
            <em v-if="referralCode">{{ referralCode }}</em>
            <em v-else class="empty-code">暂无有效邀请码，请点击右侧创建</em>
          </div>
          <div class="invite-card-foot">
            <span>被邀请人使用本激活码完成注册，邀请人获得 15 积分奖励</span>
            <span>{{ referralCodeCreatedAt ? `创建于 ${referralCodeCreatedAt}` : "点击右侧按钮生成" }}</span>
          </div>
        </div>
        <div class="invite-card-right">
          <button v-if="referralCode" type="button" class="action-btn cancel-btn" @click="deleteCode">作废当前码</button>
          <button v-else type="button" class="action-btn" @click="generateCode">创建邀请码</button>
        </div>
      </div>

      <div class="invite-link-row">
        <div class="invite-link-box">
          <span v-html="icons.link"></span>
          <code>{{ registerLink || '请先创建邀请码' }}</code>
        </div>
        <button type="button" class="copy-link-btn" :disabled="!referralCode" @click="copyReferralCode">
          <span v-html="icons.copy"></span>
          {{ copied ? "已复制" : "复制邀请码" }}
        </button>
      </div>
    </section>

    <!-- 规则说明卡片 -->
    <section class="referral-rules-details">
      <h2>邀请活动细则</h2>
      <ul>
        <li><strong>邀请奖励</strong>：被邀请人使用邀请码成功注册后，<strong>仅邀请人获得 15 积分</strong>。</li>
        <li><strong>限首次QQ注册登录</strong>：激活奖励仅限被邀请人<strong>第一次</strong>点击 QQ 登录并完成账号绑定注册时有效，后续重复登录不记为成功邀请。</li>
        <li><strong>一次性核销规则</strong>：为了防止滥用，<strong>每个邀请码仅限使用一次</strong>。被邀请人成功激活注册后，该邀请码将自动失效作废。</li>
        <li><strong>随时生成新码</strong>：旧码作废或被使用后，您可以点击右上角“创建邀请码”随时生成新的激活码分发给下一个好友。</li>
        <li><strong>反作弊说明</strong>：系统将通过设备指纹与网络IP地址智能判定关联账号。若发现虚假注册、同设备刷分等违规作弊行为，系统将收回赠予积分并封禁关联账户。</li>
      </ul>
    </section>

    <!-- 邀请明细记录 -->
    <section class="referral-ledger">
      <h2>成功邀请记录</h2>
      <div class="referral-table">
        <div class="referral-table-head">
          <span>注册时间</span>
          <span>被邀请人（QQ昵称）</span>
          <span>获得奖励</span>
          <span>状态</span>
        </div>
        <div v-for="row in inviteRows" :key="row.time + row.user" class="referral-table-row">
          <span>{{ row.time }}</span>
          <strong>{{ row.user }}</strong>
          <em>+{{ row.reward }} 积分</em>
          <span class="status-badge">{{ row.status }}</span>
        </div>
        <div v-if="!inviteRows.length" class="referral-empty-row">
          暂无成功邀请的注册记录
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from "vue";
import { useAuthStore } from "../stores/auth";
import { paperpilotApi } from "../services/paperpilotApi";

const authStore = useAuthStore();
const copied = ref(false);

const referralCode = ref("");
const referralCodeActive = ref(true);
const referralCodeCreatedAt = ref("");

const registerLink = computed(() => {
  if (!referralCode.value) return "";
  return String(referralCode.value).replace(/^INV[-_]?/i, "").toUpperCase();
});

const inviteStats = ref({
  registered: 0,
  returned: 0,
  totalPointsReward: 0
});

const statsCards = computed(() => [
  { icon: "users", value: inviteStats.value.registered, label: "已激活注册好友" },
  { icon: "coin", value: inviteStats.value.returned, label: "奖励发放次数" },
  { icon: "wallet", value: `${inviteStats.value.totalPointsReward} 积分`, label: "累计获得积分奖励" },
  { icon: "chart", value: "15 积分", label: "单次邀请奖励" },
]);

const flowSteps = [
  { index: 1, icon: "share", title: "分享好友", desc: "发送专属链接或激活码给好友" },
  { index: 2, icon: "userPlus", title: "登录激活", desc: "好友完成首次 QQ 登录并填写邀请码" },
  { index: 3, icon: "cash", title: "核销单次码", desc: "注册成功后邀请码自动核销作废" },
  { index: 4, icon: "coin", title: "发放积分", desc: "邀请人账户获得 15 积分奖励" },
];

const inviteRows = ref([]);

const icons = {
  users: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
  coin: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`,
  wallet: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 7H5a3 3 0 0 0 0 6h15v6H5a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3h13z"/><path d="M16 13h6v-3h-6a1.5 1.5 0 0 0 0 3z"/></svg>`,
  chart: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>`,
  share: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line></svg>`,
  userPlus: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="19" y1="8" x2="19" y2="14"></line><line x1="22" y1="11" x2="16" y2="11"></line></svg>`,
  cash: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><line x1="12" y1="10" x2="12" y2="14"></line><line x1="10" y1="12" x2="14" y2="12"></line></svg>`,
  ticket: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9a3 3 0 0 0 0 6v3a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-3a3 3 0 0 0 0-6V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2z"/></svg>`,
  link: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.1 0l2-2a5 5 0 0 0-7.1-7.1l-1.1 1.1"/><path d="M14 11a5 5 0 0 0-7.1 0l-2 2a5 5 0 0 0 1.1 7.1l1.1-1.1"/></svg>`,
  copy: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>`,
  exchange: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 3 20 7 16 11"></polyline><line x1="20" y1="7" x2="4" y2="7"></line><polyline points="8 21 4 17 8 13"></polyline><line x1="4" y1="17" x2="20" y2="17"></line></svg>`,
};

async function loadData() {
  try {
    const codeData = await paperpilotApi.getReferralCode();
    referralCode.value = codeData.code || "";
    referralCodeActive.value = codeData.active ?? false;
    if (codeData.createdAt) {
      referralCodeCreatedAt.value = new Date(codeData.createdAt).toLocaleString("zh-CN", { hour12: false }).replace(/\//g, "-");
    } else {
      referralCodeCreatedAt.value = "";
    }

    const statsData = await paperpilotApi.getReferralStats();
    inviteStats.value = statsData;

    const recordsData = await paperpilotApi.getReferralRecords();
    inviteRows.value = recordsData;
  } catch (error) {
    console.error("加载邀请奖励数据失败", error);
  }
}

onMounted(() => {
  loadData();
});

async function copyReferralCode() {
  if (!referralCode.value) return;
  try {
    await navigator.clipboard.writeText(registerLink.value);
    copied.value = true;
    setTimeout(() => { copied.value = false; }, 1600);
  } catch {
    authStore.addNotification({ title: "复制失败", desc: referralCode.value });
  }
}

async function generateCode() {
  try {
    const codeData = await paperpilotApi.createReferralCode();
    referralCode.value = codeData.code;
    referralCodeActive.value = codeData.active;
    if (codeData.createdAt) {
      referralCodeCreatedAt.value = new Date(codeData.createdAt).toLocaleString("zh-CN", { hour12: false }).replace(/\//g, "-");
    }
    authStore.addNotification({ title: "已创建新邀请码", desc: codeData.code });
    loadData();
  } catch (error) {
    authStore.addNotification({ title: "生成失败", desc: error?.message || "请稍后重试" });
  }
}

async function deleteCode() {
  try {
    await paperpilotApi.deleteReferralCode();
    referralCode.value = "";
    referralCodeActive.value = false;
    referralCodeCreatedAt.value = "";
    authStore.addNotification({ title: "已作废邀请码", desc: "作废后其他人将无法使用它进行注册。" });
    loadData();
  } catch (error) {
    authStore.addNotification({ title: "作废失败", desc: error?.message || "请稍后重试" });
  }
}
</script>

<style scoped>
/* ═══ ReferralView — Premium Dual-Theme ═══ */
.referral-page {
  --c-bg: #f8fafc;
  --c-surface: #ffffff;
  --c-border: #e2e8f0;
  --c-text-primary: #0f172a;
  --c-text-secondary: #475569;
  --c-text-muted: #94a3b8;
  --c-accent: #2563eb;
  --c-accent-light: #eff6ff;
  --c-accent-hover: #1d4ed8;
  --c-success: #10b981;
  --c-success-light: #ecfdf5;
  --c-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05), 0 1px 2px -1px rgba(0, 0, 0, 0.05), 0 4px 6px -1px rgba(0, 0, 0, 0.02);
  --c-card-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05), 0 4px 6px -4px rgba(0, 0, 0, 0.05);

  min-height: 100vh;
  background-color: var(--c-bg);
  padding: 32px;
  font-family: Inter, "PingFang SC", system-ui, sans-serif;
  transition: background-color .3s, color .3s;
}

:root[data-theme="dark"] .referral-page {
  --c-bg: #09090b;
  --c-surface: #18181b;
  --c-border: #27272a;
  --c-text-primary: #f4f4f5;
  --c-text-secondary: #a1a1aa;
  --c-text-muted: #52525b;
  --c-accent: #3b82f6;
  --c-accent-light: rgba(59, 130, 246, 0.08);
  --c-accent-hover: #60a5fa;
  --c-success: #10b981;
  --c-success-light: rgba(16, 185, 129, 0.08);
  --c-shadow: 0 0 0 1px rgba(255, 255, 255, 0.02);
  --c-card-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.3), 0 8px 10px -6px rgba(0, 0, 0, 0.3);
}

/* Sections as modern containers */
.referral-flow, .invite-panel, .referral-rules-details, .referral-ledger {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 16px;
  padding: 28px;
  margin-bottom: 28px;
  box-shadow: var(--c-shadow);
}

.referral-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 28px;
}

@media (max-width: 1024px) {
  .referral-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .referral-stats-grid {
    grid-template-columns: 1fr;
  }
}

.referral-stat-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--c-shadow);
  transition: transform 0.2s, box-shadow 0.2s;
}

.referral-stat-card:hover {
  transform: translateY(-1px);
  box-shadow: var(--c-card-shadow);
}

.referral-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--c-accent-light);
  color: var(--c-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.referral-icon svg, :deep(.referral-icon svg) {
  width: 20px;
  height: 20px;
  stroke-width: 1.5 !important;
}

.referral-icon.small {
  width: 32px;
  height: 32px;
}

.referral-icon.small svg, :deep(.referral-icon.small svg) {
  width: 16px;
  height: 16px;
  stroke-width: 1.5 !important;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-content strong {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--c-text-primary);
  line-height: 1.2;
}

.stat-content span {
  font-size: 0.75rem;
  color: var(--c-text-secondary);
  margin-top: 2px;
}

.referral-page h2 {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--c-text-primary);
  margin-top: 0;
  margin-bottom: 24px;
}

.referral-rule-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

@media (max-width: 768px) {
  .referral-rule-row {
    grid-template-columns: 1fr;
  }
}

.referral-rule-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: var(--c-bg);
  border-radius: 12px;
  border: 1px solid var(--c-border);
}

.step-text {
  display: flex;
  flex-direction: column;
}

.step-text strong {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--c-text-primary);
}

.step-text span {
  font-size: 0.75rem;
  color: var(--c-text-secondary);
  margin-top: 2px;
  line-height: 1.4;
}

.invite-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.invite-header h2 {
  margin: 0;
}

.text-action {
  background: none;
  border: none;
  color: var(--c-accent);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s;
}

.text-action:hover {
  color: var(--c-accent-hover);
}

/* Coupon Ticket Style Card */
.invite-code-card {
  position: relative;
  background: linear-gradient(135deg, var(--c-accent), #1d4ed8) !important;
  color: #ffffff;
  border-radius: 16px;
  padding: 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  overflow: hidden;
  box-shadow: var(--c-card-shadow);
  margin-bottom: 24px;
}

.invite-code-card::before, .invite-code-card::after {
  content: "";
  position: absolute;
  width: 24px;
  height: 24px;
  background: var(--c-bg);
  border-radius: 50%;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
}

.invite-code-card::before {
  left: -12px;
}

.invite-code-card::after {
  right: -12px;
}

.invite-card-left {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
  flex: 1;
}

.invite-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.85rem;
  font-weight: 600;
}

.invite-card-head svg {
  width: 16px;
  height: 16px;
}

.invite-code-value em {
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-style: normal;
  font-size: 2.25rem;
  font-weight: 800;
  letter-spacing: 4px;
  color: #ffffff;
  margin: 12px 0;
  display: block;
}

.invite-code-value .empty-code {
  font-size: 1.15rem;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: normal;
  font-family: inherit;
  font-weight: 500;
  margin: 16px 0;
  font-style: normal;
  display: block;
}

.invite-card-foot {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.8);
}

.invite-card-right {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-left: 32px;
  border-left: 2px dashed rgba(255, 255, 255, 0.25);
  height: 100px;
  margin-left: 24px;
  flex-shrink: 0;
}

.action-btn {
  background: #ffffff;
  color: var(--c-accent);
  border: none;
  border-radius: 8px;
  padding: 12px 24px;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
}

.action-btn:hover {
  background: #f1f5f9;
  transform: translateY(-1px);
}

.action-btn.cancel-btn {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.action-btn.cancel-btn:hover {
  background: rgba(255, 255, 255, 0.25);
}

.invite-link-row {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.invite-link-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: 10px;
  padding: 0 16px;
  height: 48px;
  overflow: hidden;
}

.invite-link-box svg {
  width: 18px;
  height: 18px;
  color: var(--c-text-secondary);
  flex-shrink: 0;
}

.invite-link-box code {
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 0.875rem;
  color: var(--c-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.copy-link-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: var(--c-accent);
  color: #ffffff;
  border: none;
  border-radius: 10px;
  padding: 0 24px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.copy-link-btn:hover:not(:disabled) {
  background: var(--c-accent-hover);
}

.copy-link-btn:disabled {
  background: var(--c-border);
  color: var(--c-text-muted);
  cursor: not-allowed;
}

.copy-link-btn svg {
  width: 16px;
  height: 16px;
}

/* Rules Section */
.referral-rules-details ul {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 0;
}

.referral-rules-details li {
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--c-text-secondary);
  position: relative;
  padding-left: 24px;
}

.referral-rules-details li::before {
  content: "✓";
  position: absolute;
  left: 0;
  color: var(--c-success);
  font-weight: 900;
  font-size: 1rem;
}

.referral-rules-details li strong {
  color: var(--c-text-primary);
  font-weight: 600;
}

/* Ledger (Records Table) styling */
.referral-ledger {
  padding: 28px 0 !important;
}

.referral-ledger h2 {
  padding: 0 28px;
}

.referral-table {
  width: 100%;
}

.referral-table-head {
  display: grid;
  grid-template-columns: 1.5fr 2fr 1fr 1fr;
  background: var(--c-bg);
  border-top: 1px solid var(--c-border);
  border-bottom: 1px solid var(--c-border);
  padding: 14px 28px;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--c-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.referral-table-row {
  display: grid;
  grid-template-columns: 1.5fr 2fr 1fr 1fr;
  align-items: center;
  padding: 16px 28px;
  font-size: 0.875rem;
  border-bottom: 1px solid var(--c-border);
  transition: background-color 0.2s;
}

.referral-table-row:hover {
  background-color: var(--c-bg);
}

.referral-table-row span {
  color: var(--c-text-secondary);
}

.referral-table-row strong {
  font-weight: 500;
  color: var(--c-text-primary);
}

.referral-table-row em {
  font-style: normal;
  color: var(--c-success);
  font-weight: 600;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  background: var(--c-success-light);
  color: var(--c-success);
  font-size: 0.75rem;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 6px;
  width: fit-content;
}

.referral-empty-row {
  text-align: center;
  padding: 60px 0;
  color: var(--c-text-secondary);
  font-size: 0.875rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.referral-empty-row::before {
  content: "✉";
  font-size: 2.5rem;
  color: var(--c-text-muted);
  opacity: 0.5;
}
</style>
