<template>
  <main class="membership-page">
    <header class="membership-header">
      <div>
        <p>会员中心</p>
        <h1>把研究节奏交给套餐，而不是零碎计费。</h1>
        <span>翻译和文献导入始终免费；综述、PPT 与学术问答按当期权益扣减。</span>
      </div>
      <button class="refresh-button" :disabled="loading" @click="load">
        <span aria-hidden="true">↻</span>{{ loading ? '更新中' : '刷新权益' }}
      </button>
    </header>

    <section class="membership-overview" :class="`plan-${membership.id}`">
      <div class="membership-mark" aria-hidden="true">{{ planInitial }}</div>
      <div class="membership-identity">
        <span class="membership-label">{{ membership.active ? '当前会员' : '当前方案' }}</span>
        <h2>{{ membership.name }}</h2>
        <p v-if="membership.active">有效至 {{ formatDate(membership.expiresAt) }} · {{ cycleLabel(membership.cycle) }}</p>
        <p v-else>先从免费翻译与导入开始，需要深度能力时再升级。</p>
      </div>
      <div class="membership-free-note">
        <strong>基础能力不限次</strong>
        <span>论文导入 · 文献管理 · 基础翻译</span>
      </div>
    </section>

    <section class="benefit-grid" aria-label="本期权益">
      <article v-for="item in benefitItems" :key="item.key" class="benefit-card" :class="item.key">
        <div class="benefit-card-top">
          <span class="benefit-icon" aria-hidden="true">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </div>
        <template v-if="item.unlimited">
          <strong>不限次</strong>
          <p>基础会员也可使用</p>
        </template>
        <template v-else>
          <strong>{{ item.remaining }} <em>/ {{ item.quota }} 次</em></strong>
          <div class="quota-track"><i :style="{ width: `${quotaPercent(item)}%` }"></i></div>
          <p>本期已使用 {{ item.used }} 次</p>
        </template>
      </article>
    </section>

    <section class="plans-section">
      <div class="section-heading">
        <div><h2>选择适合你的研究节奏</h2><p>按周期购买，开通后立即获得当期次数额度。</p></div>
        <div class="cycle-switch" role="tablist" aria-label="套餐周期">
          <button v-for="option in cycles" :key="option.id" :class="{ active: selectedCycle === option.id }" @click="selectedCycle = option.id">
            {{ option.label }}<small v-if="option.badge">{{ option.badge }}</small>
          </button>
        </div>
      </div>

      <div class="plan-list">
        <article v-for="plan in plans" :key="plan.id" class="plan-row" :class="{ selected: selectedPlan === plan.id, featured: plan.id === 'lab' }">
          <div class="plan-title"><span class="plan-dot"></span><div><h3>{{ plan.name }}</h3><p>{{ planDescription(plan.id) }}</p></div></div>
          <div class="plan-perks"><span>综述 {{ plan.reviewQuota ? `${plan.reviewQuota} 次` : '不含' }}</span><span>PPT {{ plan.pptQuota ? `${plan.pptQuota} 次` : '不含' }}</span><span>问答 {{ plan.chatQuota ? `${plan.chatQuota} 次` : '不含' }}</span></div>
          <div class="plan-price"><strong>¥{{ planPrice(plan) }}</strong><span>/ {{ cycleShortLabel(selectedCycle) }}</span></div>
          <button class="choose-plan" :class="{ active: selectedPlan === plan.id }" @click="selectedPlan = plan.id">{{ selectedPlan === plan.id ? '已选择' : '选择套餐' }}</button>
        </article>
      </div>
    </section>

    <section class="checkout-section">
      <div class="checkout-copy">
        <span>本次开通</span>
        <h2>{{ selectedPlanInfo.name }} · {{ cycleLabel(selectedCycle) }}</h2>
        <p>{{ checkoutDescription }}</p>
      </div>
      <div class="payment-actions">
        <div class="pay-methods"><button :class="{ active: provider === 'alipay' }" @click="provider = 'alipay'"><i>支</i>支付宝</button><button :class="{ active: provider === 'wechat' }" @click="provider = 'wechat'"><i>微</i>微信支付</button></div>
        <button class="pay-button" :disabled="paying" @click="checkout">{{ paying ? '正在创建订单…' : `¥${planPrice(selectedPlanInfo)} 立即开通` }}</button>
        <p v-if="paymentMessage" class="payment-message">{{ paymentMessage }}</p>
      </div>
    </section>

    <section class="orders-section">
      <div class="section-heading compact"><div><h2>套餐订单与售后</h2><p>每次开通都会留存记录；退款和问题工单必须关联具体订单。</p></div><button class="refresh-button" :disabled="ordersLoading" @click="loadOrders">刷新订单</button></div>
      <div v-if="orders.length" class="orders-table">
        <div class="order-head"><span>订单</span><span>套餐</span><span>金额</span><span>状态</span><span>操作</span></div>
        <div v-for="order in orders" :key="order.orderNo" class="order-item"><div><strong>{{ order.orderNo }}</strong><small>{{ formatDate(order.createdAt) }}</small></div><span>{{ orderPlanName(order) }}</span><strong>¥{{ Number(order.amount || 0).toFixed(2) }}</strong><span class="status" :class="order.status">{{ statusLabel(order.status) }}</span><button class="ticket-button" @click="openTicket(order)">申请售后</button></div>
      </div>
      <div v-else class="orders-empty">尚无套餐订单。选择一个套餐后，订单会出现在这里。</div>
    </section>

    <dialog ref="ticketDialog" class="ticket-dialog">
      <form method="dialog" @submit.prevent="submitTicket">
        <div class="dialog-heading"><div><span>售后工单</span><h2>{{ ticket.orderNo }}</h2></div><button class="close-button" value="cancel" aria-label="关闭">×</button></div>
        <label>工单类型<select v-model="ticket.type"><option value="support">支付与开通问题</option><option value="refund">退款申请</option></select></label>
        <label>问题标题<input v-model.trim="ticket.subject" placeholder="例如：支付后会员未生效" /></label>
        <label>具体说明<textarea v-model.trim="ticket.detail" rows="5" placeholder="请写明遇到的情况、发生时间和你的期望处理方式。"></textarea></label>
        <p v-if="ticketError" class="ticket-error">{{ ticketError }}</p>
        <button class="pay-button" :disabled="ticketSubmitting">{{ ticketSubmitting ? '提交中…' : '提交工单' }}</button>
      </form>
    </dialog>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useUsageStore } from '../stores/usage';
import { paperpilotApi } from '../services/paperpilotApi';

const usageStore = useUsageStore();
const loading = ref(false); const paying = ref(false); const ordersLoading = ref(false); const provider = ref('alipay');
const selectedCycle = ref('monthly'); const selectedPlan = ref('study'); const plans = computed(() => usageStore.state.plans || []);
const orders = ref([]); const paymentMessage = ref(''); const ticketDialog = ref(null); const ticketSubmitting = ref(false); const ticketError = ref('');
const ticket = ref({ orderNo: '', type: 'support', subject: '', detail: '' });
const cycles = [{ id: 'monthly', label: '月付' }, { id: 'quarterly', label: '季付', badge: '省 10%' }, { id: 'yearly', label: '年付', badge: '省 25%' }];
const membership = computed(() => usageStore.state.membership || { id: 'free', name: '基础版', benefits: {} });
const selectedPlanInfo = computed(() => plans.value.find(p => p.id === selectedPlan.value) || plans.value[0] || { name: '研读会员', monthlyPrice: 19.9 });
const planInitial = computed(() => ({ free: 'B', light: 'L', study: 'R', lab: 'P' })[membership.value.id] || 'B');
const benefitItems = computed(() => {
  const b = membership.value.benefits || {}; const row = (key, label, icon) => ({ key, label, icon, ...(b[key] || { quota: 0, used: 0, remaining: 0 }) });
  return [{ key: 'translation', label: '翻译与导入', icon: '↗', unlimited: true }, row('review', '论文综述', '◎'), row('ppt', '汇报 PPT', '▣'), row('chat', '学术问答', '◌')];
});
const checkoutDescription = computed(() => `${selectedPlanInfo.value.reviewQuota || 0} 次论文综述 · ${selectedPlanInfo.value.pptQuota || 0} 次 PPT 生成 · ${selectedPlanInfo.value.chatQuota || 0} 次学术问答`);
onMounted(() => { load(); loadOrders(); });
async function load() { loading.value = true; try { await usageStore.fetchSummary(); if (!plans.value.some(p => p.id === selectedPlan.value)) selectedPlan.value = plans.value[0]?.id || 'study'; } finally { loading.value = false; } }
async function loadOrders() { ordersLoading.value = true; try { orders.value = (await paperpilotApi.getPaymentOrders()).orders || []; } finally { ordersLoading.value = false; } }
function planPrice(plan) { const factor = selectedCycle.value === 'quarterly' ? 2.7 : selectedCycle.value === 'yearly' ? 9 : 1; return (Number(plan.monthlyPrice || 0) * factor).toFixed(2); }
function quotaPercent(item) { return item.quota ? Math.max(0, Math.min(100, item.remaining / item.quota * 100)) : 0; }
function cycleLabel(cycle) { return ({ monthly: '月度会员', quarterly: '季度会员', yearly: '年度会员' })[cycle] || '月度会员'; }
function cycleShortLabel(cycle) { return ({ monthly: '月', quarterly: '季', yearly: '年' })[cycle] || '月'; }
function planDescription(id) { return ({ light: '文献处理的轻量起点', study: '稳定完成每周论文汇报', lab: '为高频课题研究准备' })[id] || ''; }
function formatDate(value) { if (!value) return '-'; if (Array.isArray(value)) return `${value[0]}-${String(value[1]).padStart(2, '0')}-${String(value[2]).padStart(2, '0')}`; return String(value).replace('T', ' ').slice(0, 16); }
function orderPlanName(order) { return plans.value.find(p => p.id === order.planId)?.name || (order.planId === 'custom-recharge' ? '历史余额订单' : '会员套餐'); }
function statusLabel(status) { return ({ config_required: '待支付配置', pending_payment: '待支付', paid: '已生效', created: '已创建', failed: '支付失败' })[status] || '处理中'; }
async function checkout() { paying.value = true; paymentMessage.value = ''; try { const order = await paperpilotApi.createPaymentOrder({ planId: selectedPlan.value, planCycle: selectedCycle.value, provider: provider.value }); paymentMessage.value = order.message || '订单已创建。'; if (order.paymentUrl) window.open(order.paymentUrl, '_blank', 'noopener,noreferrer'); await loadOrders(); } catch (e) { paymentMessage.value = e?.response?.data?.message || '创建订单失败，请稍后重试。'; } finally { paying.value = false; } }
function openTicket(order) { ticket.value = { orderNo: order.orderNo, type: 'support', subject: '', detail: '' }; ticketError.value = ''; ticketDialog.value?.showModal(); }
async function submitTicket() { if (ticket.value.detail.length < 6) { ticketError.value = '请把遇到的情况写具体一些。'; return; } ticketSubmitting.value = true; try { await paperpilotApi.createPaymentTicket(ticket.value); ticketDialog.value?.close(); paymentMessage.value = '售后工单已提交，管理员处理后会同步更新。'; } catch (e) { ticketError.value = e?.response?.data?.message || '提交失败，请稍后重试。'; } finally { ticketSubmitting.value = false; } }
</script>

<style scoped>
.membership-page{min-height:100vh;background:#f5f7fb;color:#172033;padding:34px clamp(20px,4vw,64px) 64px;font-family:Inter,"Microsoft YaHei",system-ui,sans-serif}.membership-header,.membership-overview,.benefit-grid,.plans-section,.checkout-section,.orders-section{max-width:1260px;margin:0 auto}.membership-header{display:flex;justify-content:space-between;align-items:end;gap:24px;margin-bottom:22px}.membership-header p{margin:0 0 8px;color:#2658d7;font-size:13px;font-weight:800}.membership-header h1{margin:0;font-size:29px;line-height:1.25;letter-spacing:0;text-wrap:balance}.membership-header span{display:block;margin-top:10px;color:#66738a;font-size:14px}.refresh-button{height:38px;border:1px solid #d7dfec;border-radius:8px;background:#fff;color:#27334a;font:inherit;font-weight:800;padding:0 13px;cursor:pointer;white-space:nowrap}.refresh-button span{display:inline;margin:0 6px 0 0}.membership-overview{display:grid;grid-template-columns:66px 1fr auto;gap:18px;align-items:center;padding:24px 28px;background:#17264a;border-radius:12px;color:#fff;box-shadow:0 8px 18px rgba(18,35,70,.12)}.membership-overview.plan-light{background:#29466c}.membership-overview.plan-study{background:#1d3d70}.membership-overview.plan-lab{background:#143b62}.membership-mark{width:52px;height:52px;border-radius:50%;display:grid;place-items:center;background:#fff;color:#1f4fc7;font-weight:900;font-size:21px}.membership-label{font-size:12px;font-weight:800;color:#b9ceff}.membership-identity h2{margin:3px 0 5px;font-size:22px}.membership-identity p,.membership-free-note span{margin:0;color:#d3def2;font-size:13px}.membership-free-note{display:grid;gap:5px;text-align:right}.membership-free-note strong{font-size:14px}.benefit-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-top:14px}.benefit-card{min-height:138px;padding:18px 19px;border-radius:10px;background:#fff;box-shadow:0 3px 8px rgba(25,42,75,.06)}.benefit-card-top{display:flex;gap:9px;align-items:center;color:#66738a;font-size:13px;font-weight:800}.benefit-icon{width:26px;height:26px;display:grid;place-items:center;border-radius:7px;background:#edf3ff;color:#1f5ae2}.benefit-card strong{display:block;margin:14px 0 6px;font-size:25px}.benefit-card strong em{font-size:13px;font-style:normal;color:#748199}.benefit-card p{margin:0;color:#78849a;font-size:12px}.quota-track{height:5px;margin:9px 0;border-radius:99px;background:#e9eef7;overflow:hidden}.quota-track i{display:block;height:100%;border-radius:inherit;background:#2b62df}.plans-section,.orders-section{margin-top:34px}.section-heading{display:flex;justify-content:space-between;gap:24px;align-items:end;margin-bottom:14px}.section-heading h2{margin:0;font-size:20px}.section-heading p{margin:7px 0 0;color:#6d798e;font-size:13px}.cycle-switch{display:flex;padding:4px;border-radius:9px;background:#e9eef6}.cycle-switch button{height:32px;border:0;border-radius:6px;background:transparent;color:#5b687e;font:inherit;font-size:12px;font-weight:800;padding:0 11px;cursor:pointer}.cycle-switch button.active{background:#fff;color:#1d55cf;box-shadow:0 1px 3px rgba(33,51,85,.12)}.cycle-switch small{margin-left:4px;color:#18835f}.plan-list{border:1px solid #dfe6f0;border-radius:11px;overflow:hidden;background:#fff}.plan-row{display:grid;grid-template-columns:1.4fr 1.3fr 145px 102px;gap:22px;align-items:center;padding:19px 21px;border-bottom:1px solid #edf1f6}.plan-row:last-child{border-bottom:0}.plan-row.selected{background:#f3f7ff}.plan-row.featured .plan-dot{background:#08a578}.plan-title{display:flex;gap:11px;align-items:center}.plan-dot{width:9px;height:9px;border-radius:50%;background:#3869e5}.plan-title h3{margin:0;font-size:16px}.plan-title p{margin:5px 0 0;color:#748097;font-size:12px}.plan-perks{display:flex;gap:7px;flex-wrap:wrap}.plan-perks span{padding:5px 7px;border-radius:5px;background:#f3f5f8;color:#566276;font-size:11px;font-weight:800}.plan-price{text-align:right}.plan-price strong{font-size:21px}.plan-price span{color:#68758b;font-size:12px}.choose-plan,.pay-button,.ticket-button{border:0;border-radius:8px;font:inherit;font-weight:850;cursor:pointer}.choose-plan{height:36px;background:#edf2fb;color:#2d4c88}.choose-plan.active{background:#1f58d8;color:#fff}.checkout-section{display:grid;grid-template-columns:1fr 430px;gap:28px;align-items:center;margin-top:22px;padding:25px 28px;border-radius:11px;background:#fff;border:1px solid #dfe6f0}.checkout-copy>span{color:#2860dd;font-size:12px;font-weight:850}.checkout-copy h2{margin:6px 0;font-size:20px}.checkout-copy p{margin:0;color:#68758b;font-size:13px}.payment-actions{display:grid;gap:10px}.pay-methods{display:grid;grid-template-columns:1fr 1fr;gap:8px}.pay-methods button{height:39px;border:1px solid #dfe6ee;border-radius:8px;background:#fff;color:#29364c;font:inherit;font-weight:800;cursor:pointer}.pay-methods button.active{border-color:#245de0;background:#f4f7ff}.pay-methods i{display:inline-grid;place-items:center;width:20px;height:20px;margin-right:7px;border-radius:5px;background:#2074ef;color:#fff;font-style:normal;font-size:12px}.pay-methods button:last-child i{background:#1ca950}.pay-button{height:44px;background:#245ce0;color:#fff}.payment-message{margin:0;color:#536179;font-size:12px}.orders-section{padding:23px 25px;border:1px solid #dfe6f0;border-radius:11px;background:#fff}.section-heading.compact{margin-bottom:17px}.orders-table{border-top:1px solid #e8edf4}.order-head,.order-item{display:grid;grid-template-columns:1.8fr 1fr .7fr .8fr .8fr;gap:16px;align-items:center;padding:13px 4px}.order-head{color:#718099;font-size:12px;font-weight:800}.order-item{border-top:1px solid #eef2f6;font-size:13px}.order-item>div{display:grid;gap:4px}.order-item small{color:#8590a2;font-size:11px}.status{width:max-content;padding:4px 7px;border-radius:99px;background:#eff3f8;color:#58657a;font-size:11px;font-weight:800}.status.paid{background:#e8f8ef;color:#0a8055}.status.failed{background:#fff0f0;color:#c73838}.ticket-button{width:max-content;padding:7px 9px;background:#fff;border:1px solid #d8e1ee;color:#2855a9;font-size:12px}.orders-empty{padding:38px 0;text-align:center;color:#7a879a;font-size:13px}.ticket-dialog{width:min(520px,calc(100vw - 32px));border:0;border-radius:12px;padding:0;box-shadow:0 16px 40px rgba(13,27,57,.24)}.ticket-dialog::backdrop{background:rgba(22,31,47,.38)}.ticket-dialog form{display:grid;gap:14px;padding:24px}.dialog-heading{display:flex;justify-content:space-between;align-items:start}.dialog-heading span{color:#2860dd;font-size:12px;font-weight:850}.dialog-heading h2{margin:5px 0 0;font-size:16px}.close-button{border:0;background:transparent;font-size:26px;line-height:1;color:#6e7b91;cursor:pointer}.ticket-dialog label{display:grid;gap:7px;color:#47556c;font-size:12px;font-weight:800}.ticket-dialog input,.ticket-dialog select,.ticket-dialog textarea{width:100%;box-sizing:border-box;border:1px solid #d9e1ed;border-radius:7px;background:#fff;color:#1d293c;font:inherit;padding:10px;resize:vertical}.ticket-error{margin:0;color:#c73636;font-size:12px}@media(max-width:850px){.membership-header,.section-heading{align-items:start;flex-direction:column}.membership-overview{grid-template-columns:52px 1fr}.membership-free-note{grid-column:1/-1;text-align:left}.benefit-grid{grid-template-columns:repeat(2,1fr)}.plan-row{grid-template-columns:1fr 1fr}.plan-price{text-align:left}.checkout-section{grid-template-columns:1fr}.order-head{display:none}.order-item{grid-template-columns:1fr 1fr}.order-item>div{grid-column:1/-1}}@media(max-width:520px){.membership-page{padding:22px 16px 44px}.membership-header h1{font-size:24px}.benefit-grid{grid-template-columns:1fr}.plan-row{grid-template-columns:1fr}.plan-perks{margin-left:20px}.checkout-section,.orders-section{padding:19px}.cycle-switch{width:100%;overflow:auto}.cycle-switch button{flex:1;white-space:nowrap}}
</style>
