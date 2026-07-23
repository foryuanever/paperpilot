<template>
  <div class="spatial-page referral-page">
    <section class="referral-stats-grid">
      <article v-for="card in statsCards" :key="card.label" class="referral-stat-card">
        <span class="referral-icon" v-html="icons[card.icon]"></span>
        <div>
          <strong>{{ card.value }}</strong>
          <span>{{ card.label }}</span>
        </div>
      </article>
    </section>

    <section class="referral-flow">
      <h2>返佣规则</h2>
      <div class="referral-rule-row">
        <article v-for="step in flowSteps" :key="step.title" class="referral-rule-item">
          <span class="referral-icon small" v-html="icons[step.icon]"></span>
          <div>
            <strong>{{ step.index }}. {{ step.title }}</strong>
            <span>{{ step.desc }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="commission-card">
      <div>
        <h2>佣金余额</h2>
        <span>可用佣金</span>
        <strong>¥{{ inviteStats.commission }}</strong>
        <p>邀请好友获得的佣金，可以直接划转到消费余额</p>
      </div>
      <div class="commission-actions">
        <button type="button" @click="transferCommission">
          <span v-html="icons.wallet"></span>
          划转到余额
        </button>
        <button type="button" @click="redeemPoints">
          <span v-html="icons.exchange"></span>
          兑换积分
        </button>
      </div>
    </section>

    <section class="invite-panel">
      <header>
        <h2>邀请链接</h2>
        <button type="button" class="text-action" @click="generateCode">+ 创建邀请码</button>
      </header>

      <div class="invite-code-card">
        <div class="invite-card-head">
          <span v-html="icons.ticket"></span>
          <strong>邀请码 1</strong>
        </div>
        <button type="button" @click="deleteCode">删除邀请码</button>
        <em>{{ referralCode }}</em>
        <div class="invite-card-foot">
          <span>扫码注册可获得额外福利</span>
          <span>{{ createdAt ? `创建于 ${createdAt}` : "账号默认邀请码" }}</span>
        </div>
      </div>

      <div class="invite-link-row">
        <div class="invite-link-box">
          <span v-html="icons.link"></span>
          <code>{{ registerLink }}</code>
        </div>
        <button type="button" class="copy-link-btn" @click="copyReferralCode">
          <span v-html="icons.copy"></span>
          {{ copied ? "已复制" : "复制链接" }}
        </button>
      </div>

    </section>

    <section class="referral-ledger">
      <h2>返佣记录</h2>
      <div class="referral-table">
        <div class="referral-table-head">
          <span>注册时间</span>
          <span>用户</span>
          <span>消费金额</span>
          <span>佣金</span>
        </div>
        <div v-for="row in inviteRows" :key="row.time + row.user" class="referral-table-row">
          <span>{{ row.time }}</span>
          <strong>{{ row.user }}</strong>
          <em>¥{{ row.amount }}</em>
          <span>¥{{ row.reward }}</span>
        </div>
        <p v-if="!inviteRows.length" class="referral-empty-row">暂无真实返佣记录</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useAuthStore } from "../stores/auth";

const authStore = useAuthStore();
const copied = ref(false);
const localCode = ref(localStorage.getItem("papersolver-referral-code") || "");
const createdAt = ref(localStorage.getItem("papersolver-referral-created-at") || "");
const referralCode = computed(() => localCode.value || authStore.profile.inviteCode || "PAPERSLOVER2026");
const registerLink = computed(() => `${window.location.origin}/#/register?code=${encodeURIComponent(referralCode.value)}`);
const inviteStats = computed(() => ({
  registered: Number(localStorage.getItem("papersolver-referral-invited") || 0),
  returned: Number(localStorage.getItem("papersolver-referral-paid") || 0),
  totalCommission: Number(localStorage.getItem("papersolver-referral-commission") || 0),
  commission: Number(localStorage.getItem("papersolver-referral-commission") || 0).toFixed(3),
}));
const statsCards = computed(() => [
  { icon: "users", value: inviteStats.value.registered, label: "已注册用户数" },
  { icon: "coin", value: inviteStats.value.returned, label: "返利人数" },
  { icon: "wallet", value: `¥${inviteStats.value.commission}`, label: "累计获得佣金" },
  { icon: "chart", value: "10%", label: "佣金比例" },
]);
const flowSteps = [
  { index: 1, icon: "share", title: "分享", desc: "分享邀请链接" },
  { index: 2, icon: "userPlus", title: "注册", desc: "好友完成注册" },
  { index: 3, icon: "cart", title: "购买", desc: "好友购买套餐" },
  { index: 4, icon: "cash", title: "返佣", desc: "获得10%返佣" },
];
const inviteRows = computed(() => {
  const time = localStorage.getItem("papersolver-referral-paid-at");
  const user = localStorage.getItem("papersolver-referral-user");
  const amount = localStorage.getItem("papersolver-referral-amount");
  const commission = localStorage.getItem("papersolver-referral-commission");
  if (!time || !user || !amount || !commission) return [];
  return [{
    time,
    user,
    amount: Number(amount).toFixed(2),
    reward: Number(commission).toFixed(3),
  }];
});
const icons = {
  users: `<svg viewBox="0 0 24 24"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
  coin: `<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M14.8 8.8c-.7-.5-1.7-.8-2.8-.8-1.7 0-3 .8-3 2s1.3 1.8 3 2 3 .8 3 2-1.3 2-3 2c-1.1 0-2.1-.3-2.8-.8"/><path d="M12 6v12"/></svg>`,
  wallet: `<svg viewBox="0 0 24 24"><path d="M20 7H5a3 3 0 0 0 0 6h15v6H5a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3h13z"/><path d="M16 13h6v-3h-6a1.5 1.5 0 0 0 0 3z"/></svg>`,
  chart: `<svg viewBox="0 0 24 24"><path d="M4 20V10h4v10"/><path d="M10 20V4h4v16"/><path d="M16 20v-7h4v7"/></svg>`,
  share: `<svg viewBox="0 0 24 24"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.6 13.5 6.8 4"/><path d="m15.4 6.5-6.8 4"/></svg>`,
  userPlus: `<svg viewBox="0 0 24 24"><path d="M15 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8" cy="7" r="4"/><path d="M19 8v6"/><path d="M22 11h-6"/></svg>`,
  cart: `<svg viewBox="0 0 24 24"><circle cx="9" cy="20" r="1"/><circle cx="17" cy="20" r="1"/><path d="M3 4h2l2.4 11.4a2 2 0 0 0 2 1.6h7.8a2 2 0 0 0 2-1.6L21 8H6"/></svg>`,
  cash: `<svg viewBox="0 0 24 24"><rect x="3" y="7" width="18" height="12" rx="2"/><path d="M7 12h.01"/><path d="M17 14h.01"/><path d="M9 7V5h6v2"/></svg>`,
  ticket: `<svg viewBox="0 0 24 24"><path d="M3 9a3 3 0 0 0 0 6v3a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-3a3 3 0 0 0 0-6V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2z"/><path d="M13 5v14"/></svg>`,
  link: `<svg viewBox="0 0 24 24"><path d="M10 13a5 5 0 0 0 7.1 0l2-2a5 5 0 0 0-7.1-7.1l-1.1 1.1"/><path d="M14 11a5 5 0 0 0-7.1 0l-2 2A5 5 0 0 0 12 20.1l1.1-1.1"/></svg>`,
  copy: `<svg viewBox="0 0 24 24"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>`,
  exchange: `<svg viewBox="0 0 24 24"><path d="m16 3 4 4-4 4"/><path d="M20 7H4"/><path d="m8 21-4-4 4-4"/><path d="M4 17h16"/></svg>`,
};

async function copyReferralCode() {
  try {
    await navigator.clipboard.writeText(registerLink.value);
    copied.value = true;
    setTimeout(() => { copied.value = false; }, 1600);
  } catch {
    authStore.addNotification({ title: "复制失败", desc: referralCode.value });
  }
}

function generateCode() {
  const seed = (authStore.profile.name || "YUAN").replace(/\s+/g, "").slice(0, 6).toUpperCase();
  const code = `${seed}-${Math.random().toString(36).slice(2, 7).toUpperCase()}`;
  localCode.value = code;
  createdAt.value = new Date().toLocaleString("zh-CN", { hour12: false }).replace(/\//g, "-");
  localStorage.setItem("papersolver-referral-code", code);
  localStorage.setItem("papersolver-referral-created-at", createdAt.value);
  authStore.addNotification({ title: "邀请码已生成", desc: code });
}

function deleteCode() {
  localCode.value = "";
  localStorage.removeItem("papersolver-referral-code");
  authStore.addNotification({ title: "邀请码已删除", desc: "已恢复为账号默认邀请码。" });
}

function transferCommission() {
  authStore.addNotification({ title: "已提交划转申请", desc: `可用佣金 ¥${inviteStats.value.commission}` });
}

function redeemPoints() {
  authStore.addNotification({ title: "积分兑换待开放", desc: "当前佣金可先划转到余额。" });
}
</script>

<style scoped>

/* ═══ ReferralView — Premium Dual-Theme ═══ */
.referral-page, [class*="referral"] {
  --c-bg:      #f4f5f8;
  --c-surface: #ffffff;
  --c-border:  rgba(15,23,42,.08);
  --c-text:    #0f172a;
  --c-muted:   #64748b;
  --c-accent:  #6366f1;
  --c-accent2: #a855f7;
  --sh-sm: 0 2px 8px rgba(15,23,42,.06), 0 8px 24px rgba(15,23,42,.04);
  --r: 16px; --r-sm: 10px; --r-pill: 999px;
  min-height: 100vh;
  background: var(--c-bg);
  color: var(--c-text);
  font-family: Inter, "PingFang SC", system-ui, sans-serif;
  transition: background .3s, color .3s;
}
:root[data-theme="dark"] .referral-page,
:root[data-theme="dark"] [class*="referral"] {
  --c-bg:      #09090e;
  --c-surface: rgba(18,24,40,.88);
  --c-border:  rgba(255,255,255,.07);
  --c-text:    #f1f5f9;
  --c-muted:   #94a3b8;
}

/* All surface cards */
.referral-page section, .referral-page article,
.referral-page .card, .referral-page .panel {
  background: var(--c-surface) !important;
  border: 1px solid var(--c-border) !important;
  border-radius: var(--r) !important;
  box-shadow: var(--sh-sm) !important;
  backdrop-filter: blur(16px);
  color: var(--c-text) !important;
}
.referral-page h1, .referral-page h2, .referral-page h3 { color: var(--c-text) !important; }
.referral-page p, .referral-page span { color: var(--c-muted); }
.referral-page strong { color: var(--c-text) !important; }

/* Invite code box */
.referral-page input[readonly] {
  background: var(--c-bg) !important;
  border: 1px solid var(--c-border) !important;
  color: var(--c-text) !important;
  border-radius: var(--r-sm);
  padding: 10px 14px;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 2px;
  outline: none;
}

/* CTA buttons */
.referral-page button:not(.secondary), .referral-page .cta-btn {
  border-radius: var(--r-pill) !important;
  border: none !important;
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2)) !important;
  color: #fff !important;
  font-weight: 800 !important;
  box-shadow: 0 4px 14px rgba(99,102,241,.3) !important;
  transition: all .2s !important;
}
.referral-page button:hover { transform: translateY(-1px) !important; }
.referral-page .reward-item, .referral-page .tier-card {
  padding: 16px 20px;
  border-radius: var(--r-sm) !important;
  background: var(--c-bg) !important;
  border: 1px solid var(--c-border) !important;
}

.referral-page {
  display: grid;
  gap: 26px;
  width: min(1280px, calc(100vw - 64px));
  margin: 0 auto;
  padding: 24px 0 64px;
}

.referral-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px;
}

.referral-stat-card,
.referral-flow,
.commission-card,
.invite-panel,
.referral-ledger {
  border: 1px solid #e0e5ec;
  border-radius: 8px;
  background: #f8fafc;
  box-shadow: 0 8px 22px rgba(15, 23, 42, .045);
}

.referral-stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 108px;
  padding: 20px;
}

.referral-stat-card div {
  display: grid;
  gap: 6px;
}

.referral-stat-card strong {
  color: #20242c;
  font-size: 21px;
  line-height: 1;
}

.referral-stat-card span:not(.referral-icon) {
  color: #4b5563;
  font-size: 14px;
  font-weight: 600;
}

.referral-icon {
  display: inline-grid;
  place-items: center;
  flex: 0 0 auto;
  width: 60px;
  height: 60px;
  border-radius: 10px;
  color: #315bc9;
  background: #e3e9f8;
}

.referral-icon.small {
  width: 48px;
  height: 48px;
  border-radius: 9px;
}

.referral-icon :deep(svg),
.commission-actions span :deep(svg),
.invite-card-head span :deep(svg),
.invite-link-box span :deep(svg),
.copy-link-btn span :deep(svg) {
  width: 26px;
  height: 26px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.referral-icon.small :deep(svg) {
  width: 22px;
  height: 22px;
}

.referral-flow,
.invite-panel,
.referral-ledger {
  padding: 24px;
}

.referral-flow h2,
.commission-card h2,
.invite-panel h2,
.referral-ledger h2 {
  margin: 0;
  color: #20242c;
  font-size: 20px;
  line-height: 1.2;
}

.referral-flow h2 {
  margin-bottom: 26px;
}

.referral-rule-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 28px;
}

.referral-rule-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.referral-rule-item div {
  display: grid;
  gap: 7px;
}

.referral-rule-item strong {
  color: #20242c;
  font-size: 16px;
}

.referral-rule-item span:not(.referral-icon) {
  color: #4b5563;
  font-size: 14px;
}

.commission-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  min-height: 156px;
  padding: 24px;
  border-color: #b8ccff;
  background: #f8fbff;
}

.commission-card > div:first-child {
  display: grid;
  gap: 12px;
}

.commission-card span {
  color: #4b5563;
  font-size: 14px;
  font-weight: 650;
}

.commission-card strong {
  color: #315bc9;
  font-size: 32px;
  line-height: 1;
}

.commission-card p {
  margin: 0;
  color: #4b5563;
  font-size: 14px;
}

.commission-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.commission-actions button,
.copy-link-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 46px;
  border: 0;
  border-radius: 8px;
  padding: 0 22px;
  color: #fff;
  background: #315bc9;
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(49, 91, 201, .22);
  cursor: pointer;
}

.commission-actions span,
.copy-link-btn span {
  display: inline-grid;
  place-items: center;
}

.commission-actions span :deep(svg),
.copy-link-btn span :deep(svg) {
  width: 16px;
  height: 16px;
}

.invite-panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 42px;
}

.text-action {
  border: 0;
  color: #4b5563;
  background: transparent;
  font: inherit;
  font-size: 14px;
  cursor: pointer;
}

.invite-code-card {
  position: relative;
  display: grid;
  width: min(560px, 100%);
  min-height: 150px;
  margin: 0 auto 32px;
  padding: 22px 32px 18px;
  overflow: hidden;
  border-radius: 8px;
  color: #fff;
  background:
    radial-gradient(circle at 92% 86%, rgba(255, 255, 255, .08), transparent 28%),
    #456ac8;
  box-shadow: 0 10px 22px rgba(49, 91, 201, .22);
}

.invite-card-head,
.invite-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.invite-card-head {
  justify-content: flex-start;
  color: #fff;
  font-size: 18px;
  font-weight: 850;
}

.invite-card-head span :deep(svg) {
  width: 22px;
  height: 22px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
}

.invite-code-card button {
  position: absolute;
  top: 16px;
  right: 32px;
  min-height: 28px;
  border: 1px solid rgba(255, 255, 255, .48);
  border-radius: 5px;
  padding: 0 10px;
  color: #fff;
  background: transparent;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.invite-code-card em {
  justify-self: center;
  align-self: center;
  padding: 14px 18px;
  border-radius: 7px;
  color: #fff;
  background: rgba(255, 255, 255, .12);
  font-style: normal;
  font-size: 27px;
  font-weight: 850;
  letter-spacing: .04em;
}

.invite-card-foot {
  align-self: end;
  color: rgba(255, 255, 255, .9);
  font-size: 12px;
  font-weight: 650;
}

.invite-link-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.invite-link-box {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 46px;
  padding: 0 16px;
  border: 1px solid #e0e5ec;
  border-radius: 7px;
  background: #f3f5f7;
}

.invite-link-box span {
  color: #6b7280;
}

.invite-link-box span :deep(svg) {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
}

.invite-link-box code {
  min-width: 0;
  overflow: hidden;
  color: #20242c;
  font: 14px/1.4 ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.referral-ledger h2 {
  margin-bottom: 20px;
}

.referral-table {
  overflow: hidden;
  border: 1px solid #edf0f4;
  border-radius: 12px;
  background: #f8fafc;
}

.referral-table-head,
.referral-table-row {
  display: grid;
  grid-template-columns: 1.2fr 1.4fr .65fr .55fr;
  gap: 18px;
  align-items: center;
  min-height: 56px;
  padding: 0 22px;
}

.referral-table-head {
  border-bottom: 1px solid #e0e5ec;
  color: #20242c;
  font-size: 14px;
  font-weight: 850;
}

.referral-table-row {
  color: #20242c;
  font-size: 14px;
}

.referral-table-row strong,
.referral-table-row em {
  font-style: normal;
  font-weight: 750;
}

.referral-table-row em {
  color: #315bc9;
}

.referral-empty-row {
  margin: 0;
  padding: 28px 22px;
  color: #6b7280;
  font-size: 14px;
  text-align: center;
}

@media (max-width: 900px) {
  .referral-page {
    width: min(100% - 28px, 1280px);
  }

  .referral-stats-grid,
  .referral-rule-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .commission-card,
  .invite-link-row {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 620px) {
  .referral-stats-grid,
  .referral-rule-row,
  .referral-table-head,
  .referral-table-row {
    grid-template-columns: 1fr;
  }

  .referral-table-head {
    display: none;
  }
}
</style>
