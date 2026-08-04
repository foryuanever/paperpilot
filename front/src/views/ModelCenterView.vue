<template>
  <main class="membership-page">
    <!-- Ambient atmosphere orbs -->
    <div class="ambient-orb orb-1"></div>
    <div class="ambient-orb orb-2"></div>

    <!-- ── Page Header ──────────────────────────────────────── -->
    <header class="membership-topbar">
      <div>
        <div class="topbar-badge-row">
          <span class="page-chip">用量与重置中心</span>
          <span class="reset-tag">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/><path d="M16 16h5v5"/></svg>
            订阅周期到期自动复位
          </span>
        </div>
        <h1>科研用量按任务智能结算，周期自动重置透明可控。</h1>
        <p>基础文献导入与翻译全量开放；论文综述、组会 PPT、AI 对话按套餐用量扣减，按订阅周期自动全额重置。</p>
      </div>
      <button class="ghost-button refresh-button" :disabled="loading" @click="load">
        <span aria-hidden="true" :class="{ spinning: loading }">↻</span>
        {{ loading ? "更新中" : "刷新用量" }}
      </button>
    </header>

    <!-- ── Current Usage & Member Card Banner ─────────────────── -->
    <section class="current-strip-linear">
      <!-- 💳 Horizontal Membership Status Banner -->
      <div class="horizontal-membership-bar">
        <div class="vip-status-main">
          <div class="vip-badge-glow">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M5 16L3 5l5.5 5L12 4l3.5 6L21 5l-2 11H5zm14 3c0 .6-.4 1-1 1H6c-.6 0-1-.4-1-1v-1h14v1z"/></svg>
            <strong>{{ membership.active ? memberPeriodLabel : "体验会员" }}</strong>
          </div>
          <div class="vip-details">
            <span class="vip-name">{{ membership.name }}</span>
            <span class="vip-expire">{{ membership.active ? `有效期至 ${formatFullDate(membership.expiresAt)}` : "当前未开通高级套餐" }}</span>
          </div>
        </div>

        <div class="vip-metrics">
          <div class="metric-item">
            <small>剩余可用</small>
            <strong>{{ remainingDays }} <span class="unit">天</span></strong>
          </div>
          <div class="metric-divider"></div>
          <div class="metric-item">
            <small>订阅状态</small>
            <strong class="status-active">{{ membership.active ? "🟢 正常使用中" : "⚪ 未订阅" }}</strong>
          </div>
          <div class="metric-divider"></div>
          <div class="metric-item">
            <small>自动重置</small>
            <strong>到期自动复位</strong>
          </div>
        </div>

        <button type="button" class="upgrade-vip-btn" @click="scrollToPlans">
          套餐方案与开通 →
        </button>
      </div>

      <!-- 📏 Full-Width Linear Rows Dashboard -->
      <div class="entitlement-linear-panel">
        <div class="panel-header">
          <div>
            <span class="panel-tag">当期用量仪表盘</span>
            <h3>当期权益指标与重置进度</h3>
          </div>
          <span class="reset-cycle-pill">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/><path d="M16 16h5v5"/></svg>
            到期自动复位
          </span>
        </div>

        <div class="entitlement-linear-rows">
          <div v-for="item in benefitItems" :key="item.key" class="linear-row">
            <div class="row-left">
              <span class="homepage-icon-box" :class="benefitBoxClass(item.key)">
                <svg v-if="item.key === 'import'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                <svg v-else-if="item.key === 'translation'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
                <svg v-else-if="item.key === 'immersive'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>
                <svg v-else-if="item.key === 'review'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/><line x1="9" y1="7" x2="15" y2="7"/><line x1="9" y1="11" x2="13" y2="11"/></svg>
                <svg v-else-if="item.key === 'chat'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                <svg v-else-if="item.key === 'ppt'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="3"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/><path d="M7 12l3-3 3 3 4-4"/></svg>
                <svg v-else-if="item.key === 'forumBadge'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                <svg v-else-if="item.key === 'forumTop'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/></svg>
                <svg v-else-if="item.key === 'peakPriority'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
              </span>
              <div class="row-title-block">
                <strong>{{ item.label }}</strong>
                <small v-if="item.unlimited">全量开放免扣减</small>
                <small v-else-if="item.isFeature">{{ item.included ? '已开通特权' : '当期未包含' }}</small>
                <small v-else-if="item.isTeam && !item.quota">个人套餐未开通</small>
                <small v-else>当期额度 {{ item.quota }} {{ item.unit }}</small>
              </div>
            </div>

            <div class="row-center-meter">
              <div v-if="!item.unlimited && !item.isFeature && item.quota > 0" class="linear-meter-track">
                <b :style="{ width: `${quotaPercent(item)}%` }"></b>
              </div>
              <span v-else-if="item.unlimited" class="linear-unlimited-label">✓ 不限次数</span>
              <span v-else-if="item.isFeature" class="linear-feature-badge" :class="{ active: item.included }">
                {{ item.included ? '✓ ' + (item.value || '包含') : '× 未包含' }}
              </span>
              <span v-else-if="item.isTeam && !item.quota" class="linear-disabled-label">未开放</span>
            </div>

            <div class="row-right">
              <span class="row-stat-text">{{ benefitUsageLabel(item) }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ── Plan Workbench ──────────────────────────────────────── -->
    <section class="plan-workbench" id="plans-section">
      <div class="plan-heading">
        <div>
          <span class="section-chip">会员套餐</span>
          <h2>选择适合你的科研方案</h2>
          <p>全量开放文献导入与基础翻译；综述、PPT、问答按套餐用量结算，订阅周期自动重置。</p>
        </div>
        <div class="cycle-toggle-pill">
          <button
            v-for="cycle in cycles"
            :key="cycle.id"
            :class="{ active: selectedCycle === cycle.id }"
            @click="selectedCycle = cycle.id"
          >{{ cycle.label }}</button>
        </div>
      </div>

      <div class="plan-cards-v2">
        <article
          v-for="plan in personalPlans"
          :key="plan.id"
          class="plan-card-v2"
          :class="[normalizePlanId(plan.id), { active: selectedPlan === plan.id, current: plan.id === usageStore.state.membership?.id, featured: plan.id === 'plus' }]"
          @click="selectedPlan = plan.id"
        >
          <!-- Top accent line for featured -->
          <div v-if="plan.id === 'plus'" class="card-accent-line"></div>

          <!-- Badge -->
          <div class="card-top-row" style="justify-content: flex-end; min-height: 20px;">
            <span v-if="plan.id === usageStore.state.membership?.id" class="tier-badge badge-current">当前使用中</span>
            <span v-else-if="plan.id === 'plus'" class="tier-badge badge-hot">推荐</span>
          </div>

          <!-- Header Content Wrapper with min-height to ensure perfect alignment -->
          <div class="plan-card-header-wrap" style="display: flex; flex-direction: column; gap: 12px; min-height: 165px; justify-content: flex-start;">
            <!-- Name & desc -->
            <div class="plan-name-block">
              <div style="display: flex; align-items: baseline; justify-content: space-between; gap: 8px; flex-wrap: wrap;">
                <h3>{{ plan.name }}</h3>
                <span v-if="plan.subtitle" style="font-size: 11px; font-weight: 800; color: var(--c-accent); padding: 2px 6px; background: rgba(99,102,241,0.08); border-radius: 4px; border: 1px solid rgba(99,102,241,0.15);">{{ plan.subtitle }}</span>
              </div>
              <p style="margin-top: 8px; min-height: 36px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">{{ planCopy(plan.id) }}</p>
            </div>

            <!-- Price -->
            <div class="plan-price-row" style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap; min-height: 32px;">
              <template v-if="!plan.monthlyPrice">
                <span class="price-main" style="font-size: 26px;">免费</span>
                <span class="price-sub">永久开放</span>
              </template>
              <template v-else>
                <span class="price-main">¥<em>{{ planPrice(plan) }}</em></span>
                <span class="price-sub">/ {{ cycleShortLabel(selectedCycle) }}</span>
                <span v-if="isSeckillActive(plan)" class="price-original" style="text-decoration: line-through; font-size: 13px; color: var(--c-muted); font-weight: 500;">
                  ¥{{ originalPlanPrice(plan) }}
                </span>
                <!-- Inline Seckill Countdown Badge next to price -->
                <span v-if="isSeckillActive(plan)" class="seckill-inline-countdown" style="display: inline-flex; align-items: center; gap: 4px; padding: 4px 8px; border-radius: 6px; background: linear-gradient(90deg, #ef4444, #f97316); color: #fff; font-size: 10px; font-weight: 800; box-shadow: 0 2px 6px rgba(239, 68, 68, 0.25); border: 1px solid rgba(255,255,255,0.15); font-variant-numeric: tabular-nums; line-height: 1;">
                  <span>⚡️秒杀</span>
                  <span>{{ formatSeckillCountdown(plan) }}</span>
                </span>
              </template>
            </div>

            <!-- Luckin tag for Lite -->
            <div v-if="plan.id === 'lite'" class="luckin-hint">
              <img :src="luckinLogo" class="luckin-icon-sm" alt="luckin" />
              <span>瑞幸咖啡价～</span>
            </div>
          </div>

          <div class="card-hr"></div>

          <!-- Features -->
          <ul class="plan-feat-list">
            <li v-for="row in planRows(plan)" :key="row.label" :class="{ 'feat-off': !row.included }">
              <span class="feat-status-icon" :class="row.included ? 'status-ok' : 'status-no'">
                <svg v-if="row.included" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" class="feat-svg-icon" style="width: 10px; height: 10px;"><polyline points="20 6 9 17 4 12"/></svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" class="feat-svg-icon" style="width: 10px; height: 10px;"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </span>
              <span class="feat-label">{{ row.label }}</span>
              <span class="feat-val">{{ row.value }}</span>
            </li>
          </ul>

          <button class="plan-cta-btn" :class="{ 'cta-active': selectedPlan === plan.id, 'cta-current': plan.id === usageStore.state.membership?.id }">
            <template v-if="plan.id === usageStore.state.membership?.id">✓ 当前使用中</template>
            <template v-else-if="selectedPlan === plan.id">已选中 · 下方确认支付</template>
            <template v-else>选择此方案</template>
          </button>
        </article>
      </div>
    </section>

    <!-- ── Power Packs ──────────────────────────────────────── -->
    <section class="plan-workbench">
      <div class="plan-heading">
        <div>
          <span class="section-chip chip-amber">额度加油包</span>
          <h2>按需补充，永不过期</h2>
          <p>购买后即时生效，直接叠加至当前可用额度，无需等待周期重置，适合临时大额用量场景。</p>
        </div>
      </div>

      <div class="packs-grid" style="grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px;">
        <article
          v-for="pack in powerPacks"
          :key="pack.id"
          class="pack-card-v2"
          :class="{ 'pack-v2-active': selectedPlan === pack.id }"
          @click="selectedPlan = pack.id"
          style="display: flex; flex-direction: column; height: 100%; min-height: 380px;"
        >
          <!-- Left side colored strip -->
          <div class="pack-accent-bar" :class="pack.icon"></div>

          <div class="pack-v2-top">
            <div class="pack-v2-icon" :class="pack.icon">
              <svg v-if="pack.icon === 'review'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
              <svg v-else-if="pack.icon === 'ppt'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/></svg>
              <svg v-else-if="pack.icon === 'chat'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              <svg v-else-if="pack.icon === 'report'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            </div>
            <div class="pack-v2-info">
              <strong>{{ pack.name }}</strong>
              <span>{{ pack.desc }}</span>
            </div>
          </div>

          <!-- Seckill Banner for Pack -->
          <div v-if="isSeckillActive(pack)" class="seckill-countdown-banner" style="display: flex; align-items: center; justify-content: space-between; margin-top: 6px; margin-bottom: 12px; padding: 6px 10px; border-radius: 8px; background: linear-gradient(90deg, #ef4444, #f97316); color: #fff; font-size: 11px; font-weight: 800; box-shadow: 0 0 10px rgba(239, 68, 68, 0.3); border: 1px solid rgba(255,255,255,0.1);">
            <span>⚡️ 限时秒杀中</span>
            <span style="font-variant-numeric: tabular-nums;">{{ formatSeckillCountdown(pack) }}</span>
          </div>

          <div class="card-hr" style="margin: 10px 0;"></div>

          <!-- Pack Features -->
          <ul class="plan-feat-list" style="margin-top: 4px; flex: 1;">
            <li v-for="row in pack.benefits" :key="row.name" :class="{ 'feat-off': !row.included }">
              <span class="feat-status-icon" :class="row.included ? 'status-ok' : 'status-no'">
                <svg v-if="row.included" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" class="feat-svg-icon" style="width: 10px; height: 10px;"><polyline points="20 6 9 17 4 12"/></svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" class="feat-svg-icon" style="width: 10px; height: 10px;"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </span>
              <span class="feat-label">{{ row.name }}</span>
              <span class="feat-val">{{ row.value }}</span>
            </li>
          </ul>

          <div class="pack-v2-bottom" style="margin-top: auto; padding-top: 12px; border-top: 1px solid rgba(255,255,255,0.04);">
            <div class="pack-v2-price">
              ¥<em>{{ planPrice(pack) }}</em><span>/ 包</span>
              <span v-if="isSeckillActive(pack)" class="price-original" style="text-decoration: line-through; margin-left: 8px; font-size: 13px; color: var(--c-muted); font-weight: 500;">
                ¥{{ originalPlanPrice(pack) }}
              </span>
            </div>
          </div>
          <button class="pack-v2-btn" :class="{ active: selectedPlan === pack.id }">
            {{ selectedPlan === pack.id ? '✓ 已选中' : '加入选择' }}
          </button>
        </article>
      </div>
    </section>

    <!-- ── Floating Checkout Bar ───────────────────────────────── -->
    <section class="checkout-bar">
      <div>
        <span>本次开通</span>
        <strong>{{ selectedPlanInfo.name }}{{ selectedPlanInfo.id && selectedPlanInfo.id.startsWith('pack_') ? ' (即时加量)' : ' · ' + cycleLabel(selectedCycle) }}</strong>
        <p>{{ checkoutDescription }}</p>
      </div>
      <div class="checkout-actions">
        <div class="pay-methods">
          <button class="active" disabled style="display: flex; align-items: center; justify-content: center; gap: 6px;">
            <svg class="wechat-pay-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="width: 18px; height: 18px;">
              <rect width="24" height="24" rx="5" fill="#07C160"/>
              <path d="M12 17.5c2.485 0 4.5-1.79 4.5-4s-2.015-4-4.5-4-4.5 1.79-4.5 4c0 .878.318 1.69.856 2.348l-.348 1.152 1.348-.674c.642.176 1.314.274 2.144.274z" fill="#FFF"/>
              <path d="M10.2 13.5l1.3 1.3 2.8-3" stroke="#07C160" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            微信支付
          </button>
        </div>
        <button class="primary-button" :disabled="paying" @click="checkout">
          {{ paying ? "正在创建订单..." : `¥${planPrice(selectedPlanInfo)} 去支付` }}
        </button>
      </div>
      <p v-if="paymentMessage" class="payment-message" :class="{ success: paymentMessage.includes('成功') }">
        <span v-if="paymentMessage.includes('成功')" class="success-check-icon">✓</span>
        {{ paymentMessage }}
      </p>
    </section>

    <!-- ── Orders Section ──────────────────────────────────────── -->
    <section class="orders-section">
      <div class="section-title">
        <div>
          <span>订单信息</span>
          <h2>套餐订单与售后</h2>
        </div>
        <button class="ghost-button" :disabled="ordersLoading" @click="loadOrders">{{ ordersLoading ? "刷新中" : "刷新订单" }}</button>
      </div>

      <div v-if="filteredOrders.length" class="orders-table">
        <div class="order-head"><span>订单号</span><span>开通套餐</span><span>订单金额</span><span>状态</span><span>售后操作</span></div>
        <div v-for="order in filteredOrders" :key="order.orderNo" class="order-item">
          <div><strong>{{ order.orderNo }}</strong><small>{{ formatDate(order.createdAt) }}</small></div>
          <span>{{ orderPlanName(order) }}</span>
          <strong>¥{{ Number(order.amount || 0).toFixed(2) }}</strong>
          <span class="status" :class="order.status">{{ statusLabel(order.status) }}</span>
          <button class="ticket-button" @click="openTicket(order)">申请售后</button>
        </div>
      </div>
      <div v-else class="orders-empty">还没有已生效套餐订单。开通后这里会显示已支付和已退款的订单状态。</div>
    </section>

    <!-- Ticket Dialog -->
    <dialog ref="ticketDialog" class="ticket-dialog">
      <form method="dialog" @submit.prevent="submitTicket">
        <div class="dialog-heading">
          <div><span>售后工单</span><h2>{{ ticket.orderNo }}</h2></div>
          <button type="button" class="close-button" aria-label="关闭" @click="ticketDialog?.close()">×</button>
        </div>
        <label>工单类型<select v-model="ticket.type"><option value="support">支付与开通问题</option><option value="refund">退款申请</option></select></label>
        <label>问题标题<input v-model.trim="ticket.subject" placeholder="例如：支付后会员未生效" /></label>
        <label>具体说明<textarea v-model.trim="ticket.detail" rows="5" placeholder="请写明订单、发生时间、问题现象和希望处理方式。"></textarea></label>
        <p v-if="ticketError" class="ticket-error">{{ ticketError }}</p>
        <button class="primary-button" :disabled="ticketSubmitting">{{ ticketSubmitting ? "提交中..." : "提交工单" }}</button>
      </form>
    </dialog>

    <dialog ref="qrDialog" class="wechat-pay-dialog">
      <form method="dialog" @submit.prevent>
        <div class="dialog-heading">
          <div><span>WECHAT PAY</span><h2>微信扫码支付</h2></div>
          <button class="close-button" value="cancel" aria-label="关闭" @click="closeWechatPayDialog">×</button>
        </div>
        <div class="wechat-pay-body">
          <div class="wechat-qr-shell">
            <div v-if="currentPayOrder?.status === 'paid'" class="wechat-pay-success-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" class="success-checkmark-svg">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
            </div>
            <img v-else-if="wechatQrCode" :src="wechatQrCode" alt="微信支付二维码" />
            <div v-else class="wechat-qr-loading">生成二维码中...</div>
          </div>
          <div class="wechat-pay-meta">
            <strong>{{ currentPayOrder?.orderNo || "待创建订单" }}</strong>
            <span>{{ selectedPlanInfo.name }} · {{ cycleLabel(selectedCycle) }}</span>
            <b v-if="currentPayOrder?.status !== 'paid'">¥{{ Number(currentPayOrder?.amount || planPrice(selectedPlanInfo) || 0).toFixed(2) }}</b>
            <b v-else style="color: #10b981;">支付成功！</b>
            <p>{{ paymentMessage || "请使用微信扫一扫完成支付，支付成功后会员权益会自动生效。" }}</p>
          </div>
        </div>
        <div class="wechat-pay-actions">
          <button type="button" class="ghost-button" @click="loadOrders">刷新订单</button>
          <button type="button" class="primary-button" @click="closeWechatPayDialog">我知道了</button>
        </div>
      </form>
    </dialog>
  </main>
</template>

<script setup>
import { useScrollReveal } from "../composables/useScrollReveal";
useScrollReveal(".model-center-page");
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import QRCode from "qrcode";
import { useUsageStore } from "../stores/usage";
import { paperpilotApi } from "../services/paperpilotApi";
import { getCurrentApiBaseUrl } from "../services/apiClient";
import goldCardReference from "../assets/membership/gold-card-cropped.jpg";
import luckinLogo from "../assets/luckin-logo.png";

const usageStore = useUsageStore();
const loading = ref(false);
const paying = ref(false);
const ordersLoading = ref(false);
const orders = ref([]);
const filteredOrders = computed(() => {
  return orders.value.filter(order => order.status === "paid" || order.status === "refunded");
});
const provider = ref("wechat");
const selectedCycle = ref("monthly");
const selectedPlan = ref("pack_tier_lite");
const powerPacks = computed(() => {
  const packs = displayPlans.value.filter((item) => item.id.startsWith("pack_"));
  return packs.map((item) => {
    let icon = "review";
    if (item.id.includes("standard")) icon = "chat";
    else if (item.id.includes("plus")) icon = "ppt";
    else if (item.id.includes("pro")) icon = "report";

    return {
      ...item,
      icon,
      desc: item.subtitle || "补充科研额度",
      price: item.effectiveMonthlyPrice || item.monthlyPrice || 0,
      benefits: packRows(item)
    };
  });
});
const paymentMessage = ref("");
const ticketDialog = ref(null);
const qrDialog = ref(null);
const ticketSubmitting = ref(false);
const ticketError = ref("");
const nowTick = ref(Date.now());
const wechatQrCode = ref("");
const currentPayOrder = ref(null);
let saleTimer = null;
let paymentPollTimer = null;
const ticket = ref({ orderNo: "", type: "support", subject: "", detail: "" });
const teamMemberCount = ref(5);

function onTeamCountInput(e) {
  const raw = String(e.target.value).replace(/\D/g, "");
  if (!raw) return;
  const val = parseInt(raw, 10);
  if (!isNaN(val)) {
    teamMemberCount.value = Math.min(100, Math.max(2, val));
  }
}

const cycles = [
  { id: "monthly", label: "月付" },
];

const defaultPlans = [
  { id: "free", name: "个人 Free", subtitle: "永久免费版", monthlyPrice: 0, reviewQuotaDaily: 3, pptQuotaMonthly: 0, chatQuotaDaily: 5, translateQuotaDaily: 5, immersiveQuotaDaily: 3, forumSpecial: false, forumTopDaily: 0, peakPriority: false, teamShared: false },
  { id: "lite", name: "个人 Lite", subtitle: "一杯瑞幸咖啡价", monthlyPrice: 9.9, reviewQuotaDaily: 15, pptQuotaMonthly: 2, chatQuotaDaily: 30, translateQuotaDaily: 10, immersiveQuotaDaily: 10, forumSpecial: false, forumTopDaily: 0, peakPriority: false, teamShared: false },
  { id: "plus", name: "个人 Plus", subtitle: "热销推荐", monthlyPrice: 19.9, reviewQuotaDaily: 30, pptQuotaMonthly: 4, chatQuotaDaily: 60, translateQuotaDaily: 20, immersiveQuotaDaily: 20, forumSpecial: true, forumTopDaily: 0, peakPriority: false, teamShared: false },
  { id: "pro", name: "个人 Pro", subtitle: "极速进阶", monthlyPrice: 29.9, reviewQuotaDaily: 60, pptQuotaMonthly: 6, chatQuotaDaily: 120, translateQuotaDaily: 50, immersiveQuotaDaily: 50, forumSpecial: true, forumTopDaily: 1, peakPriority: true, teamShared: false },
  { id: "team_plus", name: "课题组团队 Plus", subtitle: "导师购买分配 (9折)", monthlyPrice: 17.91, perUserPrice: 19.9, reviewQuotaDaily: 30, pptQuotaMonthly: 4, chatQuotaDaily: 60, translateQuotaDaily: 20, immersiveQuotaDaily: 20, forumSpecial: true, forumTopDaily: 0, peakPriority: true, teamShared: true, teamSeats: 10 },
  { id: "team_pro", name: "课题组团队 Pro", subtitle: "实验室旗舰 (9折)", monthlyPrice: 26.91, perUserPrice: 29.9, reviewQuotaDaily: 60, pptQuotaMonthly: 6, chatQuotaDaily: 120, translateQuotaDaily: 50, immersiveQuotaDaily: 50, forumSpecial: true, forumTopDaily: 1, peakPriority: true, teamShared: true, teamSeats: 20 },
  { id: "pack_tier_lite", name: "学术启航加油包", subtitle: "适合日常轻量文献阅读", monthlyPrice: 19.9, reviewQuota: 5, pptQuota: 0, chatQuota: 50, translateQuota: 3, immersiveQuota: 3, researchQuota: 0, reportQuota: 0, teamShared: false },
  { id: "pack_tier_standard", name: "学术精进加油包", subtitle: "适合高频文献精研", monthlyPrice: 39.9, reviewQuota: 15, pptQuota: 0, chatQuota: 120, translateQuota: 8, immersiveQuota: 8, researchQuota: 10, reportQuota: 0, teamShared: false },
  { id: "pack_tier_plus", name: "学术大师加油包", subtitle: "中度学术汇报制作", monthlyPrice: 69.9, reviewQuota: 35, pptQuota: 2, chatQuota: 250, translateQuota: 18, immersiveQuota: 18, researchQuota: 25, reportQuota: 0, teamShared: false },
  { id: "pack_tier_pro", name: "学术至尊加油包", subtitle: "终极文献分析汇报", monthlyPrice: 99.9, reviewQuota: 80, pptQuota: 5, chatQuota: 600, translateQuota: 40, immersiveQuota: 40, researchQuota: 60, reportQuota: 5, teamShared: false },
];

const plans = computed(() => usageStore.state.plans || []);
const planOrder = ["free", "lite", "plus", "pro", "team_plus", "team_pro", "pack_tier_lite", "pack_tier_standard", "pack_tier_plus", "pack_tier_pro"];
const displayPlans = computed(() => {
  const byId = new Map(defaultPlans.map((plan) => [plan.id, plan]));
  (plans.value || []).forEach((plan) => {
    const id = normalizePlanId(plan.id);
    byId.set(id, { ...(byId.get(id) || {}), ...plan, id });
  });
  return Array.from(byId.values())
    .filter((plan) => plan && plan.activeFlag !== false)
    .sort((a, b) => {
      const ia = planOrder.includes(a.id) ? planOrder.indexOf(a.id) : Number(a.sortOrder ?? 99);
      const ib = planOrder.includes(b.id) ? planOrder.indexOf(b.id) : Number(b.sortOrder ?? 99);
      return ia === ib ? String(a.name || "").localeCompare(String(b.name || ""), "zh-CN") : ia - ib;
    });
});
const planGroups = computed(() => [
  {
    key: "personal",
    label: "个人套餐",
    description: "包含永久免费版、基础版、热销版与进阶版，满足不同阶段科研需求。",
    plans: personalPlans.value,
  },
]);
const personalPlans = computed(() => displayPlans.value.filter((item) => !item.teamShared && !item.id.startsWith("pack_")));
const membership = computed(() => usageStore.state.membership || { id: "free", name: "未开通会员", benefits: {} });
const selectedPlanInfo = computed(() => {
  const id = selectedPlan.value || "";
  if (id.startsWith("pack_")) {
    const pack = powerPacks.value.find(p => p.id === id);
    if (pack) {
      return {
        id: pack.id,
        name: pack.name,
        monthlyPrice: pack.price,
        originalMonthlyPrice: pack.originalMonthlyPrice || pack.price,
        description: pack.desc,
        seckillEnabled: pack.seckillEnabled,
        seckillPrice: pack.seckillPrice,
        seckillStartsAt: pack.seckillStartsAt,
        seckillEndsAt: pack.seckillEndsAt,
        seckillLabel: pack.seckillLabel,
        seckillActive: pack.seckillActive,
        teamShared: false
      };
    }
  }
  return displayPlans.value.find((item) => item.id === id) || displayPlans.value[0] || { name: "研读会员", monthlyPrice: 19.9, reviewQuota: 10, pptQuota: 2, chatQuota: 80 };
});
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
function benefitIcon(key) {
  return {
    translation: "🌐",
    review: "📄",
    ppt: "📊",
    chat: "💬",
    team_seats: "👥"
  }[key] || "✨";
}

function benefitBoxClass(key) {
  return {
    import: "box-blue",
    translation: "box-blue",
    immersive: "box-cyan",
    review: "box-purple",
    chat: "box-amber",
    ppt: "box-emerald",
    forumBadge: "box-purple",
    forumTop: "box-amber",
    peakPriority: "box-emerald",
    teamSeats: "box-blue",
  }[key] || "box-purple";
}

const benefitItems = computed(() => {
  const benefits = membership.value.benefits || {};
  const currentPlanId = normalizePlanId(membership.value.id || "free");
  const planInfo = displayPlans.value.find((p) => p.id === currentPlanId) || defaultPlans[0];

  return [
    { key: "import", label: "论文插件导入", unlimited: true },
    { key: "translation", label: "对照翻译", unlimited: false, quota: planInfo.translateQuotaDaily || 5, used: benefits.translation?.used || 0, unit: "篇/天" },
    { key: "immersive", label: "沉浸翻译", unlimited: false, quota: planInfo.immersiveQuotaDaily || 3, used: benefits.immersive?.used || 0, unit: "篇/天" },
    { key: "review", label: "AI 论文综述", unlimited: false, quota: planInfo.reviewQuotaDaily || 3, used: benefits.review?.used || 0, unit: "次/天" },
    { key: "chat", label: "研读对话", unlimited: false, quota: planInfo.chatQuotaDaily || 5, used: benefits.chat?.used || 0, unit: "次/天" },
    { key: "ppt", label: "组会 PPT", unlimited: false, quota: planInfo.pptQuotaMonthly || 0, used: benefits.ppt?.used || 0, unit: "次/月" },
    { key: "research", label: "调研广场", unlimited: false, quota: planInfo.researchQuotaDaily || 0, used: benefits.research?.used || 0, unit: "次/天" },
    { key: "report", label: "组会一键汇报", unlimited: false, quota: planInfo.reportQuotaMonthly || 0, used: benefits.report?.used || 0, unit: "次/月" },
    { key: "forumBadge", label: "论坛会员标识", isFeature: true, included: planInfo.forumSpecial },
    { key: "forumTop", label: "发帖置顶", isFeature: true, included: Number(planInfo.forumTopDaily || 0) > 0, value: planInfo.forumTopDaily ? `${planInfo.forumTopDaily} 次/天` : "未包含" },
    { key: "peakPriority", label: "高峰期优先通道", isFeature: true, included: planInfo.peakPriority, value: planInfo.peakPriority ? "优先通道" : "标准通道" },
    { key: "teamSeats", label: "团队席位", unlimited: false, quota: planInfo.teamSeats || (benefits.teamSeats?.quota || 0), used: benefits.teamSeats?.used || 0, unit: "席", isTeam: true },
  ];
});

const checkoutDescription = computed(() => {
  const plan = selectedPlanInfo.value;
  if (plan.id && plan.id.startsWith("pack_")) {
    return `${plan.description} (单次购买即时生效，无月度自动重置)`;
  }
  const count = plan.teamShared ? teamMemberCount.value : 0;
  return [
    `对照 ${plan.translateQuotaDaily || 10} 篇/天`,
    `沉浸 ${plan.immersiveQuotaDaily || 10} 篇/天`,
    `综述 ${plan.reviewQuotaDaily || 15} 次/天`,
    `PPT ${plan.pptQuotaMonthly || 2} 次/月`,
    plan.teamShared ? `团队 ${count} 人席位` : "",
  ].filter(Boolean).join(" · ");
});

onMounted(() => {
  saleTimer = window.setInterval(() => {
    nowTick.value = Date.now();
  }, 40);
  load();
  loadOrders();
});

onBeforeUnmount(() => {
  if (saleTimer) window.clearInterval(saleTimer);
  stopPaymentPolling();
});

async function load() {
  loading.value = true;
  try {
    await usageStore.fetchSummary();
    if (!selectedPlan.value.startsWith("pack_")) {
      selectedPlan.value = normalizePlanId(selectedPlan.value);
      if (!displayPlans.value.some((item) => item.id === selectedPlan.value)) {
        selectedPlan.value = displayPlans.value[1]?.id || displayPlans.value[0]?.id || "plus";
      }
    }
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
  if (plan.id && plan.id.startsWith("pack_")) {
    return Number(plan.monthlyPrice || 0).toFixed(2);
  }
  const monthly = isSeckillActive(plan) ? Number(plan.seckillPrice ?? plan.effectiveMonthlyPrice ?? plan.monthlyPrice ?? 0) : Number(plan.effectiveMonthlyPrice ?? plan.monthlyPrice ?? 0);
  return totalPlanPrice(monthly, plan);
}

function originalPlanPrice(plan) {
  const monthly = Math.max(Number(plan.originalMonthlyPrice || 0), Number(plan.monthlyPrice || 0), Number(plan.effectiveMonthlyPrice || 0));
  return totalPlanPrice(monthly, plan);
}

function totalPlanPrice(monthly, plan) {
  const factor = selectedCycle.value === "quarterly" ? 2.7 : selectedCycle.value === "yearly" ? 9 : 1;
  const isTeam = plan.teamShared;
  const count = isTeam ? Math.max(1, teamMemberCount.value) : 1;
  return (Number(monthly || 0) * factor * count).toFixed(2);
}

function isSeckillActive(plan) {
  nowTick.value;
  if (!plan || !plan.seckillEnabled) return false;
  const start = parseDateValue(plan.seckillStartsAt);
  const end = parseDateValue(plan.seckillEndsAt);
  const now = Date.now();
  return Number(plan.seckillPrice) >= 0 && (!start || start.getTime() <= now) && (!end || end.getTime() > now);
}

function seckillRemainingMs(plan) {
  nowTick.value;
  const end = parseDateValue(plan?.seckillEndsAt);
  return end ? Math.max(0, end.getTime() - Date.now()) : 0;
}

function formatSeckillCountdown(plan) {
  const ms = seckillRemainingMs(plan);
  if (!ms) return "进行中";
  const hours = Math.floor(ms / 3600000);
  const minutes = Math.floor((ms % 3600000) / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);
  const cc = Math.floor((ms % 1000) / 10);
  const hh = String(hours).padStart(2, "0");
  const mm = String(minutes).padStart(2, "0");
  const ss = String(seconds).padStart(2, "0");
  const ccStr = String(cc).padStart(2, "0");
  return `${hh}:${mm}:${ss}.${ccStr}`;
}

function quotaPercent(item) {
  const quota = Number(item.quota || 0);
  const used = Number(item.used || 0);
  return quota ? Math.max(0, Math.min(100, (used / quota) * 100)) : 0;
}

function benefitUsageLabel(item) {
  if (item.unlimited) return "不限次";
  if (item.isFeature) return item.included ? (item.value || "包含") : "未包含";
  if (item.isTeam && !item.quota) return "未开放";
  const unit = item.unit || "次";
  const quota = Number(item.quota || 0);
  const used = Number(item.used || 0);
  return `${used} / ${quota} ${unit}`;
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
  return ({ free: "F", lite: "L", plus: "P+", pro: "P", team_plus: "T+", team_pro: "TP" })[normalizePlanId(id)] || "P";
}

function planBadgeLabel(id) {
  return ({ free: "永久免费", lite: "瑞幸咖啡价", plus: "热销推荐", pro: "极速进阶", team_plus: "团队 Plus (9折)", team_pro: "团队 Pro (9折)" })[normalizePlanId(id)] || "套餐";
}

function planCardTitle(id) {
  return ({ free: "个人 Free", lite: "个人 Lite", plus: "个人 Plus", pro: "个人 Pro", team_plus: "课题组团队 Plus", team_pro: "课题组团队 Pro" })[normalizePlanId(id)] || "会员套餐";
}

function planCopy(id) {
  id = normalizePlanId(id);
  return ({
    free: "基础科研体验，满足日常小量文献阅读与对话。",
    lite: "一杯瑞幸咖啡的价格，轻松开启智能论文阅读与基础综述。",
    plus: "适合课程论文、周会准备和日常科研高频问答。",
    pro: "面向课题高压推进，高额度并享发帖置顶与高峰优先。",
    team_plus: "导师按人数购买分配 (19.9元/人×9折)，全员特权与席位。",
    team_pro: "实验室全员极速旗舰 (29.9元/人×9折)，顶配额度与全特权。",
  })[id] || "按套餐次数与功能权益使用。";
}

function planRows(plan) {
  const id = normalizePlanId(plan.id);
  const isTeam = Boolean(plan.teamShared);
  const prefix = isTeam ? "每人" : "";

  const translateVal = plan.translateQuotaDaily ? `${prefix}每天 ${plan.translateQuotaDaily} 篇` : "未包含";
  const immersiveVal = plan.immersiveQuotaDaily ? `${prefix}每天 ${plan.immersiveQuotaDaily} 篇` : "未包含";
  const reviewVal = plan.reviewQuotaDaily ? `${prefix}每天 ${plan.reviewQuotaDaily} 次` : "未包含";
  const chatVal = plan.chatQuotaDaily ? `${prefix}每天 ${plan.chatQuotaDaily} 次` : "未包含";
  const pptVal = plan.pptQuotaMonthly ? `${prefix}每月 ${plan.pptQuotaMonthly} 次` : "未包含";
  const researchVal = plan.researchQuotaDaily ? `${prefix}每天 ${plan.researchQuotaDaily} 次` : "未包含";
  const reportVal = plan.reportQuotaMonthly ? `${prefix}每月 ${plan.reportQuotaMonthly} 次` : "未包含";

  const forumVal = plan.forumSpecial ? (id === "pro" || id === "team_pro" ? "包含 (每日1次置顶)" : "包含") : "未包含";
  const peakVal = plan.peakPriority ? "优先通道" : "标准通道";

  if (id === "free") {
    return [
      { label: "论文插件导入", description: "文献一键入库与管理", value: "不限次", included: true },
      { label: "对照翻译", description: "双栏对照翻译阅读", value: translateVal, included: Number(plan.translateQuotaDaily || 0) > 0 },
      { label: "沉浸翻译", description: "全页版式保留沉浸翻译", value: immersiveVal, included: Number(plan.immersiveQuotaDaily || 0) > 0 },
      { label: "AI 论文综述", description: "结构化文献综述生成", value: reviewVal, included: Number(plan.reviewQuotaDaily || 0) > 0 },
      { label: "论文解析与研读对话", description: "针对论文深度问答与推演", value: chatVal, included: Number(plan.chatQuotaDaily || 0) > 0 },
      { label: "组会 PPT 汇报制作", description: "PPT Agent 自动生成组会汇报", value: pptVal, included: Number(plan.pptQuotaMonthly || 0) > 0 },
      { label: "调研广场", description: "社会与学术热点文献分析调研", value: researchVal, included: Number(plan.researchQuotaDaily || 0) > 0 },
      { label: "组会一键汇报", description: "自动整合组会大纲与一键汇报", value: reportVal, included: Number(plan.reportQuotaMonthly || 0) > 0 },
      { label: "论坛会员特效与标识", description: "彩色姓名与专属标识", value: forumVal, included: Boolean(plan.forumSpecial) },
      { label: "高峰期优先响应", description: "高并发时段优先通道", value: "标准通道", included: false },
    ];
  }
  if (id === "lite") {
    return [
      { label: "论文插件导入", description: "文献一键入库与管理", value: "不限次", included: true },
      { label: "对照翻译", description: "双栏对照翻译阅读", value: translateVal, included: Number(plan.translateQuotaDaily || 0) > 0 },
      { label: "沉浸翻译", description: "全页版式保留沉浸翻译", value: immersiveVal, included: Number(plan.immersiveQuotaDaily || 0) > 0 },
      { label: "AI 论文综述", description: "结构化文献综述生成", value: reviewVal, included: Number(plan.reviewQuotaDaily || 0) > 0 },
      { label: "论文解析与研读对话", description: "针对论文深度问答与推演", value: chatVal, included: Number(plan.chatQuotaDaily || 0) > 0 },
      { label: "组会 PPT 汇报制作", description: "PPT Agent 自动生成组会汇报", value: pptVal, included: Number(plan.pptQuotaMonthly || 0) > 0 },
      { label: "调研广场", description: "社会与学术热点文献分析调研", value: researchVal, included: Number(plan.researchQuotaDaily || 0) > 0 },
      { label: "组会一键汇报", description: "自动整合组会大纲与一键汇报", value: reportVal, included: Number(plan.reportQuotaMonthly || 0) > 0 },
      { label: "论坛会员特效与标识", description: "彩色姓名与专属标识", value: forumVal, included: Boolean(plan.forumSpecial) },
      { label: "高峰期优先响应", description: "高并发时段优先通道", value: peakVal, included: Boolean(plan.peakPriority) },
    ];
  }
  if (id === "plus") {
    return [
      { label: "论文插件导入", description: "文献一键入库与管理", value: "不限次", included: true },
      { label: "对照翻译", description: "双栏对照翻译阅读", value: translateVal, included: Number(plan.translateQuotaDaily || 0) > 0 },
      { label: "沉浸翻译", description: "全页版式保留沉浸翻译", value: immersiveVal, included: Number(plan.immersiveQuotaDaily || 0) > 0 },
      { label: "AI 论文综述", description: "结构化文献综述生成", value: reviewVal, included: Number(plan.reviewQuotaDaily || 0) > 0 },
      { label: "论文解析与研读对话", description: "针对论文深度问答与推演", value: chatVal, included: Number(plan.chatQuotaDaily || 0) > 0 },
      { label: "组会 PPT 汇报制作", description: "PPT Agent 自动生成组会汇报", value: pptVal, included: Number(plan.pptQuotaMonthly || 0) > 0 },
      { label: "调研广场", description: "社会与学术热点文献分析调研", value: researchVal, included: Number(plan.researchQuotaDaily || 0) > 0 },
      { label: "组会一键汇报", description: "自动整合组会大纲与一键汇报", value: reportVal, included: Number(plan.reportQuotaMonthly || 0) > 0 },
      { label: "论坛会员特效与标识", description: "彩色姓名与专属标识", value: forumVal, included: Boolean(plan.forumSpecial) },
      { label: "高峰期优先响应", description: "高并发时段优先通道", value: peakVal, included: Boolean(plan.peakPriority) },
    ];
  }
  if (id === "pro") {
    return [
      { label: "论文插件导入", description: "文献一键入库与管理", value: "不限次", included: true },
      { label: "对照翻译", description: "双栏对照翻译阅读", value: translateVal, included: Number(plan.translateQuotaDaily || 0) > 0 },
      { label: "沉浸翻译", description: "全页版式保留沉浸翻译", value: immersiveVal, included: Number(plan.immersiveQuotaDaily || 0) > 0 },
      { label: "AI 论文综述", description: "结构化文献综述生成", value: reviewVal, included: Number(plan.reviewQuotaDaily || 0) > 0 },
      { label: "论文解析与研读对话", description: "针对论文深度问答与推演", value: chatVal, included: Number(plan.chatQuotaDaily || 0) > 0 },
      { label: "组会 PPT 汇报制作", description: "PPT Agent 自动生成组会汇报", value: pptVal, included: Number(plan.pptQuotaMonthly || 0) > 0 },
      { label: "调研广场", description: "社会与学术热点文献分析调研", value: researchVal, included: Number(plan.researchQuotaDaily || 0) > 0 },
      { label: "组会一键汇报", description: "自动整合组会大纲与一键汇报", value: reportVal, included: Number(plan.reportQuotaMonthly || 0) > 0 },
      { label: "论坛会员特效与标识", description: "会员特效 + 每日1次发帖置顶", value: forumVal, included: Boolean(plan.forumSpecial) },
      { label: "高峰期优先响应", description: "高并发时段优先通道", value: peakVal, included: Boolean(plan.peakPriority) },
    ];
  }
  // Team plan
  return [
    { label: "论文插件导入", description: "全员文献入库与 PDF 管理", value: "不限次", included: true },
    { label: "对照翻译", description: "全员双栏对照翻译阅读", value: translateVal, included: Number(plan.translateQuotaDaily || 0) > 0 },
    { label: "沉浸翻译", description: "全员全页版式沉浸翻译", value: immersiveVal, included: Number(plan.immersiveQuotaDaily || 0) > 0 },
    { label: "AI 论文综述", description: "全员结构化综述生成", value: reviewVal, included: Number(plan.reviewQuotaDaily || 0) > 0 },
    { label: "论文解析与研读对话", description: "全员学术问答与推演", value: chatVal, included: Number(plan.chatQuotaDaily || 0) > 0 },
    { label: "组会 PPT 汇报制作", description: "全员 PPT Agent 自动汇报", value: pptVal, included: Number(plan.pptQuotaMonthly || 0) > 0 },
    { label: "调研广场", description: "全员社会与学术热点文献分析调研", value: researchVal, included: Number(plan.researchQuotaDaily || 0) > 0 },
    { label: "组会一键汇报", description: "全员自动大纲与一键汇报", value: reportVal, included: Number(plan.reportQuotaMonthly || 0) > 0 },
    { label: "论坛会员特效与标识", description: "全员尊享会员标识", value: forumVal, included: Boolean(plan.forumSpecial) },
    { label: "导师购买统一分配", description: "按人数结算享优惠", value: `¥${plan.monthlyPrice} / 人 / 月`, included: true },
    { label: "高峰期优先响应", description: "全员享受极速优先通道", value: peakVal, included: Boolean(plan.peakPriority) },
  ];
}

function packRows(pack) {
  const translateVal = pack.translateQuota ? `+${pack.translateQuota} 篇` : "未包含";
  const immersiveVal = pack.immersiveQuota ? `+${pack.immersiveQuota} 篇` : "未包含";
  const reviewVal = pack.reviewQuota ? `+${pack.reviewQuota} 次` : "未包含";
  const chatVal = pack.chatQuota ? `+${pack.chatQuota} 次` : "未包含";
  const pptVal = pack.pptQuota ? `+${pack.pptQuota} 次` : "未包含";
  const researchVal = pack.researchQuota ? `+${pack.researchQuota} 次` : "未包含";
  const reportVal = pack.reportQuota ? `+${pack.reportQuota} 次` : "未包含";

  return [
    { name: "对照翻译", value: translateVal, included: Number(pack.translateQuota || 0) > 0 },
    { name: "沉浸翻译", value: immersiveVal, included: Number(pack.immersiveQuota || 0) > 0 },
    { name: "AI论文综述", value: reviewVal, included: Number(pack.reviewQuota || 0) > 0 },
    { name: "研读对话", value: chatVal, included: Number(pack.chatQuota || 0) > 0 },
    { name: "调研广场", value: researchVal, included: Number(pack.researchQuota || 0) > 0 },
    { name: "组会PPT", value: pptVal, included: Number(pack.pptQuota || 0) > 0 },
    { name: "组会一键汇报", value: reportVal, included: Number(pack.reportQuota || 0) > 0 },
  ];
}

async function selectAndCheckout(planId) {
  selectedPlan.value = normalizePlanId(planId);
  await checkout();
}

function normalizePlanId(id) {
  return ({ light: "lite", study: "plus", lab: "pro", team: "team_plus", max: "pro" })[id] || id || "free";
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
  
  const currentBaseUrl = getCurrentApiBaseUrl() || "";
  if (/^https?:\/\/(127\.0\.0\.1|localhost)(:\d+)?$/i.test(currentBaseUrl)) {
    paymentMessage.value = `当前客户端连接的服务地址是：${currentBaseUrl}。本地测试服务器无法直接进行微信支付回调，因此无法生成订单。请回到客户端的登录页面，点击下方的【设置】按钮，将“云端服务地址”修改为您的公网服务器地址（例如 https://papersolver.cn/api），保存并重启客户端后再试。`;
    paying.value = false;
    return;
  }

  try {
    const order = await paperpilotApi.createPaymentOrder({
      planId: selectedPlan.value,
      planCycle: selectedCycle.value,
      provider: provider.value,
      quantity: teamMemberCount.value,
      teamMemberCount: teamMemberCount.value
    });
    currentPayOrder.value = order;
    paymentMessage.value = order.message || "订单已创建。";
    if (order.paymentUrl) {
      wechatQrCode.value = await QRCode.toDataURL(order.paymentUrl, {
        margin: 1,
        width: 260,
        color: { dark: "#111827", light: "#ffffff" },
      });
      qrDialog.value?.showModal();
      startPaymentPolling(order.orderNo);
    }
    await loadOrders();
  } catch (error) {
    paymentMessage.value = error?.response?.data?.message || "创建订单失败，请稍后重试。";
  } finally {
    paying.value = false;
  }
}

function startPaymentPolling(orderNo) {
  stopPaymentPolling();
  paymentPollTimer = window.setInterval(async () => {
    await loadOrders();
    const latest = orders.value.find((item) => item.orderNo === orderNo);
    if (latest) currentPayOrder.value = latest;
    if (latest?.status === "paid") {
      paymentMessage.value = "微信支付成功，会员权益已生效。";
      stopPaymentPolling();
      await usageStore.load();
      setTimeout(() => qrDialog.value?.close(), 900);
    }
  }, 3000);
}

function stopPaymentPolling() {
  if (paymentPollTimer) {
    window.clearInterval(paymentPollTimer);
    paymentPollTimer = null;
  }
}

function closeWechatPayDialog() {
  stopPaymentPolling();
  qrDialog.value?.close();
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
/* ════════════════════════════════════════════════════════════
   USAGE & RESET CENTER — Clean Horizontal & Linear Design
   ════════════════════════════════════════════════════════════ */

.membership-page {
  --c-bg:       #f8fafc;
  --c-surface:  #ffffff;
  --c-border:   #e2e8f0;
  --c-text:     #0f172a;
  --c-muted:    #475569;
  --c-subtle:   #94a3b8;
  --c-accent:   #6366f1;
  --c-accent2:  #a855f7;
  --r: 20px; --r-sm: 12px; --r-pill: 999px;
  --sh-sm: 0 2px 10px rgba(15,23,42,.04), 0 8px 24px rgba(15,23,42,.03);
  --sh-md: 0 10px 32px rgba(15,23,42,.08);
  --sh-lg: 0 20px 60px rgba(15,23,42,.14);

  position: relative;
  min-height: 100vh;
  background: var(--c-bg);
  color: var(--c-text);
  padding: 36px clamp(16px, 4vw, 56px) 130px;
  font-family: Inter, "PingFang SC", system-ui, sans-serif;
  transition: background 0.3s ease, color 0.3s ease;
  width: 100%;
  box-sizing: border-box;
  margin: 0 auto;
}

:root[data-theme="dark"] .membership-page {
  --c-bg:       #09090e;
  --c-surface:  #111827;
  --c-border:   rgba(255, 255, 255, 0.08);
  --c-text:     #f1f5f9;
  --c-muted:    #94a3b8;
  --c-subtle:   #64748b;
  --sh-sm: 0 2px 10px rgba(0,0,0,.3), 0 8px 24px rgba(0,0,0,.25);
  --sh-md: 0 10px 32px rgba(0,0,0,.45);
  --sh-lg: 0 20px 60px rgba(0,0,0,.65);
}

.ticket-dialog {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 16px;
  color: var(--c-text);
  width: 90%;
  max-width: 480px;
  padding: 24px;
}
.wechat-pay-success-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 200px;
  height: 200px;
  background: rgba(16, 185, 129, 0.1);
  border-radius: 12px;
  color: #10b981;
}
.success-checkmark-svg {
  width: 80px;
  height: 80px;
}
.payment-message.success {
  color: #10b981 !important;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-weight: bold;
}
.success-check-icon {
  font-size: 1.2rem;
}

/* Ambient Orbs */
.ambient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
  opacity: 0.22;
}
.orb-1 { top: -100px; left: 10%; width: 400px; height: 400px; background: radial-gradient(circle, #818cf8, #c084fc); }
.orb-2 { top: 200px; right: 5%; width: 500px; height: 500px; background: radial-gradient(circle, #38bdf8, #818cf8); }

/* ── Topbar ─────────────────────────────────────────────── */
.membership-topbar {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}

.topbar-badge-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.page-chip {
  padding: 4px 14px;
  border-radius: var(--r-pill);
  background: rgba(99, 102, 241, 0.1);
  color: var(--c-accent);
  font-size: 12px;
  font-weight: 800;
  border: 1px solid rgba(99, 102, 241, 0.2);
}
:root[data-theme="dark"] .page-chip {
  background: rgba(99, 102, 241, 0.18);
  color: #818cf8;
}

.reset-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: var(--r-pill);
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  font-size: 12px;
  font-weight: 800;
  border: 1px solid rgba(16, 185, 129, 0.25);
}

.membership-topbar h1 {
  margin: 0 0 8px;
  font-size: clamp(22px, 2.5vw, 30px);
  font-weight: 900;
  color: var(--c-text);
  line-height: 1.25;
  letter-spacing: -0.5px;
}

.membership-topbar p {
  margin: 0;
  font-size: 14px;
  color: var(--c-muted);
  max-width: 760px;
  line-height: 1.6;
}

.ghost-button {
  height: 42px;
  padding: 0 20px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-surface);
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 750;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: var(--sh-sm);
  white-space: nowrap;
}
.ghost-button:hover {
  border-color: var(--c-accent);
  color: var(--c-accent);
  transform: translateY(-1px);
}
.spinning { display: inline-block; animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }

/* ── Current Strip (Horizontal Banner + Linear Rows) ────── */
.current-strip-linear {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 44px;
}

/* 💳 Horizontal Membership Status Banner (Subtle Glass Bar) */
.horizontal-membership-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 18px 28px;
  border-radius: var(--r);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: var(--sh-sm);
}
@media (max-width: 900px) {
  .horizontal-membership-bar { flex-direction: column; align-items: flex-start; }
}

.vip-status-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.vip-badge-glow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--r-pill);
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.18), rgba(245, 158, 11, 0.12));
  border: 1px solid rgba(251, 191, 36, 0.35);
  color: #d97706;
  font-size: 13.5px;
  font-weight: 850;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.15);
}
:root[data-theme="dark"] .vip-badge-glow {
  color: #fbbf24;
}

.vip-details {
  display: flex;
  flex-direction: column;
}
.vip-name {
  font-size: 16px;
  font-weight: 900;
  color: var(--c-text);
}
.vip-expire {
  font-size: 12px;
  color: var(--c-muted);
}

.vip-metrics {
  display: flex;
  align-items: center;
  gap: 20px;
}
.metric-item {
  display: flex;
  flex-direction: column;
}
.metric-item small {
  font-size: 11px;
  color: var(--c-subtle);
}
.metric-item strong {
  font-size: 16px;
  font-weight: 900;
  color: var(--c-text);
}
.metric-item strong .unit {
  font-size: 12px;
  font-weight: 700;
  color: var(--c-muted);
}
.status-active { color: #10b981 !important; }

.metric-divider {
  width: 1px;
  height: 28px;
  background: var(--c-border);
}

.upgrade-vip-btn {
  height: 38px;
  padding: 0 20px;
  border-radius: var(--r-pill);
  border: none;
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #ffffff;
  font-size: 13px;
  font-weight: 850;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
  transition: all 0.2s ease;
  white-space: nowrap;
}
.upgrade-vip-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

/* 📏 Full-Width Linear Rows Panel (No Box Tiles!) */
.entitlement-linear-panel {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--r);
  padding: 24px 30px;
  box-shadow: var(--sh-sm);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--c-border);
}
.panel-tag {
  display: block;
  font-size: 10.5px;
  font-weight: 900;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--c-accent);
  margin-bottom: 2px;
}
.panel-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: var(--c-text);
}

.reset-cycle-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: var(--r-pill);
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  font-size: 12px;
  font-weight: 800;
  border: 1px solid rgba(16, 185, 129, 0.25);
}

/* Linear Row List */
.entitlement-linear-rows {
  display: flex;
  flex-direction: column;
}

.linear-row {
  display: grid;
  grid-template-columns: 240px 1fr 140px;
  align-items: center;
  gap: 24px;
  padding: 16px 12px;
  border-bottom: 1px solid var(--c-border);
  transition: background 0.18s ease;
}
.linear-row:last-child {
  border-bottom: none;
}
.linear-row:hover {
  background: rgba(99, 102, 241, 0.035);
  border-radius: var(--r-sm);
}
@media (max-width: 768px) {
  .linear-row { grid-template-columns: 1fr; gap: 10px; }
}

.row-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.row-title-block strong {
  display: block;
  font-size: 14.5px;
  font-weight: 850;
  color: var(--c-text);
}
.row-title-block small {
  font-size: 11.5px;
  color: var(--c-muted);
}

.row-center-meter {
  width: 100%;
}

.linear-meter-track {
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  overflow: hidden;
}
.linear-meter-track b {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #6366f1, #a855f7);
  transition: width 0.4s ease;
}

.linear-unlimited-label {
  font-size: 12px;
  color: #10b981;
  font-weight: 800;
}

.linear-feature-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 850;
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
}
.linear-feature-badge.active {
  background: rgba(16, 185, 129, 0.14);
  color: #10b981;
}

.linear-disabled-label {
  font-size: 12px;
  color: var(--c-subtle);
  font-weight: 700;
}

.homepage-icon-box.box-cyan {
  background: rgba(6, 182, 212, 0.08);
  border: 1.5px solid rgba(6, 182, 212, 0.3);
  color: #0891b2;
}
:root[data-theme="dark"] .homepage-icon-box.box-cyan {
  background: rgba(6, 182, 212, 0.12);
  border-color: rgba(6, 182, 212, 0.4);
  color: #22d3ee;
}

.row-right {
  text-align: right;
}
.row-stat-text {
  font-size: 15px;
  font-weight: 950;
  color: var(--c-accent);
  font-variant-numeric: tabular-nums;
}

/* 🎨 Homepage Micro-Glow Icon Box */
.homepage-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.homepage-icon-box.box-blue {
  background: rgba(37, 99, 235, 0.08);
  border: 1.5px solid rgba(37, 99, 235, 0.3);
  color: #2563eb;
}
:root[data-theme="dark"] .homepage-icon-box.box-blue {
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.4);
  color: #60a5fa;
}

.homepage-icon-box.box-purple {
  background: rgba(147, 51, 234, 0.08);
  border: 1.5px solid rgba(147, 51, 234, 0.3);
  color: #9333ea;
}
:root[data-theme="dark"] .homepage-icon-box.box-purple {
  background: rgba(168, 85, 247, 0.12);
  border-color: rgba(168, 85, 247, 0.4);
  color: #c084fc;
}

.homepage-icon-box.box-emerald {
  background: rgba(16, 185, 129, 0.08);
  border: 1.5px solid rgba(16, 185, 129, 0.3);
  color: #10b981;
}
:root[data-theme="dark"] .homepage-icon-box.box-emerald {
  background: rgba(16, 185, 129, 0.12);
  border-color: rgba(16, 185, 129, 0.4);
  color: #34d399;
}

.homepage-icon-box.box-amber {
  background: rgba(245, 158, 11, 0.08);
  border: 1.5px solid rgba(245, 158, 11, 0.3);
  color: #d97706;
}
:root[data-theme="dark"] .homepage-icon-box.box-amber {
  background: rgba(245, 158, 11, 0.12);
  border-color: rgba(245, 158, 11, 0.4);
  color: #fbbf24;
}

.linear-row:hover .homepage-icon-box {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

/* ══════════════════════════════════════════════════
   PLAN WORKBENCH — V2 Premium Design
   ══════════════════════════════════════════════════ */
.plan-workbench {
  position: relative;
  z-index: 2;
  margin-bottom: 56px;
}

.section-chip {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.1);
  color: var(--c-accent);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  border: 1px solid rgba(99, 102, 241, 0.2);
  margin-bottom: 10px;
}
.section-chip.chip-amber {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
  border-color: rgba(245, 158, 11, 0.2);
}
:root[data-theme="dark"] .section-chip.chip-amber { color: #fbbf24; }

.plan-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 32px;
}
.plan-heading h2 {
  margin: 0 0 6px;
  font-size: clamp(20px, 2vw, 26px);
  font-weight: 900;
  color: var(--c-text);
  letter-spacing: -0.3px;
}
.plan-heading > div > p {
  margin: 0;
  font-size: 14px;
  color: var(--c-muted);
  max-width: 600px;
  line-height: 1.6;
}

/* Cycle Toggle */
.cycle-toggle-pill {
  display: inline-flex;
  gap: 3px;
  padding: 4px;
  border-radius: 999px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: var(--sh-sm);
  flex-shrink: 0;
}
.cycle-toggle-pill button {
  height: 34px;
  padding: 0 18px;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}
.cycle-toggle-pill button.active {
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

/* Plan Cards Grid V2 */
.plan-cards-v2 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  align-items: stretch;
}
@media (max-width: 1200px) {
  .plan-cards-v2 { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .plan-cards-v2 { grid-template-columns: 1fr; }
}

/* Plan Card V2 — Premium Glassmorphism & Dark Minimal */
.plan-card-v2 {
  position: relative;
  border-radius: 20px;
  background: radial-gradient(circle at 10% 10%, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0.005)), var(--c-surface);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  cursor: pointer;
  backdrop-filter: blur(16px);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  overflow: hidden;
}
:root[data-theme="light"] .plan-card-v2 {
  background: radial-gradient(circle at 10% 10%, rgba(15, 23, 42, 0.02), rgba(15, 23, 42, 0.002)), var(--c-surface);
  border-color: rgba(15, 23, 42, 0.08);
}
.plan-card-v2::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  padding: 1px;
  background: linear-gradient(135deg, rgba(255,255,255,0.1), transparent 50%);
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}
:root[data-theme="light"] .plan-card-v2::after {
  background: linear-gradient(135deg, rgba(15,23,42,0.1), transparent 50%);
}

.plan-card-v2:hover {
  border-color: rgba(99, 102, 241, 0.45);
  box-shadow: 0 12px 32px rgba(0,0,0,0.18), 0 0 20px rgba(99,102,241,0.1);
  transform: translateY(-4px);
}
.plan-card-v2.active {
  border-color: var(--c-accent) !important;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.25), 0 12px 32px rgba(99, 102, 241, 0.15) !important;
}
.plan-card-v2.featured {
  border-color: rgba(99, 102, 241, 0.4);
  background: radial-gradient(circle at 10% 10%, rgba(99, 102, 241, 0.06), rgba(255, 255, 255, 0.005)), var(--c-surface);
}

/* Top accent line for featured (Plus) */
.card-accent-line {
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, #6366f1, #a855f7);
  border-radius: 20px 20px 0 0;
}

/* Top row: tier icon + badge */
.card-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.plan-tier-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.12);
  box-shadow: var(--sh-sm);
}
:root[data-theme="light"] .plan-tier-icon {
  background: rgba(15,23,42,0.04);
  border-color: rgba(15,23,42,0.08);
  color: var(--c-text);
}
.plan-card-v2.featured .plan-tier-icon,
.plan-card-v2.active .plan-tier-icon {
  background: linear-gradient(135deg, #6366f1, #a855f7);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 12px rgba(99,102,241,0.25);
}
.tier-svg {
  width: 20px;
  height: 20px;
}

/* Tier badges */
.tier-badge {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.2px;
}
.badge-current {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.25);
}
.badge-hot {
  background: linear-gradient(135deg, rgba(99,102,241,0.18), rgba(168,85,247,0.18));
  color: #a5b4fc;
  border: 1px solid rgba(99,102,241,0.25);
}
:root[data-theme="light"] .badge-hot {
  color: #4f46e5;
}

/* Name block */
.plan-name-block h3 {
  margin: 0 0 4px;
  font-size: 19px;
  font-weight: 900;
  color: var(--c-text);
  letter-spacing: -0.3px;
}
.plan-name-block p {
  margin: 0;
  font-size: 12.5px;
  color: var(--c-muted);
  line-height: 1.5;
}

/* Price */
.plan-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.price-main {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--c-muted);
  line-height: 1;
}
.price-main em {
  font-style: normal;
  font-size: 38px;
  font-weight: 950;
  color: var(--c-text);
  letter-spacing: -1.5px;
  font-family: tabular-nums, Inter, system-ui;
}
.plan-card-v2.featured .price-main em,
.plan-card-v2.active .price-main em {
  background: linear-gradient(135deg, #818cf8, #c084fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.price-sub {
  font-size: 13.5px;
  color: var(--c-muted);
  font-weight: 600;
}

/* Luckin hint */
.luckin-hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px 6px 10px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.08);
  border: 1px solid rgba(14, 165, 233, 0.25);
  font-size: 11.5px;
  color: #0284c7;
  font-weight: 800;
  width: fit-content;
}
:global(html[data-theme="dark"]) .luckin-hint {
  background: rgba(56, 189, 248, 0.12);
  border-color: rgba(56, 189, 248, 0.35);
  color: #38bdf8;
}
.luckin-icon-sm {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

/* Horizontal rule */
.card-hr {
  height: 1px;
  background: linear-gradient(90deg, var(--c-border), transparent);
}

/* Feature list */
.plan-feat-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
  flex: 1;
}
.plan-feat-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  font-size: 13px;
}
:root[data-theme="light"] .plan-feat-list li {
  border-color: rgba(15, 23, 42, 0.04);
}
.plan-feat-list li:last-child { border-bottom: none; }
.plan-feat-list li.feat-off { opacity: 0.42; }

/* status icons ✓ / ✗ */
.feat-status-icon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 11.5px;
  font-weight: 900;
  flex-shrink: 0;
}
.feat-status-icon.status-ok {
  color: #10b981;
  background: rgba(16, 185, 129, 0.12);
}
.feat-status-icon.status-no {
  color: #f43f5e;
  background: rgba(244, 63, 94, 0.12);
}

.feat-label {
  flex: 1;
  color: var(--c-text);
  font-weight: 650;
}
.feat-val {
  font-size: 12px;
  font-weight: 800;
  color: var(--c-muted);
  white-space: nowrap;
}
.plan-feat-list li:not(.feat-off) .feat-val {
  color: var(--c-accent);
}

/* CTA button */
.plan-cta-btn {
  width: 100%;
  height: 42px;
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.12);
  background: rgba(255,255,255,0.02);
  color: var(--c-text);
  font-size: 13.5px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.22s ease;
  margin-top: 4px;
}
:root[data-theme="light"] .plan-cta-btn {
  border-color: rgba(15,23,42,0.12);
  background: rgba(15,23,42,0.02);
}
.plan-cta-btn:hover {
  border-color: var(--c-accent);
  color: #fff;
  background: var(--c-accent);
  box-shadow: 0 4px 14px rgba(99,102,241,0.25);
}
.plan-cta-btn.cta-active {
  background: linear-gradient(135deg, #6366f1, #818cf8);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 16px rgba(99,102,241,0.35);
}
.plan-cta-btn.cta-current {
  background: rgba(16,185,129,0.08);
  border-color: rgba(16,185,129,0.3);
  color: #10b981;
}

/* ── Power Packs V2 — Premium Glass Layout ─────────── */
.packs-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
@media (max-width: 960px) {
  .packs-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 580px) {
  .packs-grid { grid-template-columns: 1fr; }
}

.pack-card-v2 {
  position: relative;
  border-radius: 16px;
  background: radial-gradient(circle at 10% 10%, rgba(255, 255, 255, 0.025), rgba(255, 255, 255, 0.005)), var(--c-surface);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 20px 20px 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  overflow: hidden;
}
:root[data-theme="light"] .pack-card-v2 {
  background: radial-gradient(circle at 10% 10%, rgba(15, 23, 42, 0.015), rgba(15, 23, 42, 0.002)), var(--c-surface);
  border-color: rgba(15, 23, 42, 0.08);
}
.pack-card-v2:hover {
  border-color: rgba(99,102,241,0.4);
  box-shadow: 0 10px 28px rgba(0,0,0,0.12), 0 0 15px rgba(99,102,241,0.06);
  transform: translateY(-3px);
}
.pack-card-v2.pack-v2-active {
  border-color: var(--c-accent) !important;
  box-shadow: 0 0 0 2px rgba(99,102,241,0.25), 0 10px 28px rgba(99,102,241,0.12) !important;
}

/* Left colored accent bar */
.pack-accent-bar {
  position: absolute;
  top: 0; left: 0; bottom: 0;
  width: 4px;
  transition: width 0.25s ease;
}
.pack-card-v2:hover .pack-accent-bar {
  width: 6px;
}
/* Theme colors for accent bar and icon */
.pack-accent-bar.review      { background: #8b5cf6; }
.pack-accent-bar.ppt         { background: #10b981; }
.pack-accent-bar.chat        { background: #f59e0b; }
.pack-accent-bar.translation { background: #3b82f6; }
.pack-accent-bar.research    { background: #ec4899; }
.pack-accent-bar.report      { background: #f43f5e; }

/* Pack top: icon + info */
.pack-v2-top {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.pack-v2-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  border: 1px solid transparent;
}
.pack-v2-icon svg { width: 18px; height: 18px; }

/* Specific icon background colors with transparent opacity */
.pack-v2-icon.review      { color: #c084fc; background: rgba(139, 92, 246, 0.12); border-color: rgba(139, 92, 246, 0.15); }
.pack-v2-icon.ppt         { color: #34d399; background: rgba(16, 185, 129, 0.12); border-color: rgba(16, 185, 129, 0.15); }
.pack-v2-icon.chat        { color: #fbbf24; background: rgba(245, 158, 11, 0.12); border-color: rgba(245, 158, 11, 0.15); }
.pack-v2-icon.translation { color: #60a5fa; background: rgba(59, 130, 246, 0.12); border-color: rgba(59, 130, 246, 0.15); }
.pack-v2-icon.research    { color: #f472b6; background: rgba(236, 72, 153, 0.12); border-color: rgba(236, 72, 153, 0.15); }
.pack-v2-icon.report      { color: #fb7185; background: rgba(244, 63, 94, 0.12); border-color: rgba(244, 63, 94, 0.15); }

:root[data-theme="light"] .pack-v2-icon.review      { color: #8b5cf6; }
:root[data-theme="light"] .pack-v2-icon.ppt         { color: #10b981; }
:root[data-theme="light"] .pack-v2-icon.chat        { color: #d97706; }
:root[data-theme="light"] .pack-v2-icon.translation { color: #2563eb; }
:root[data-theme="light"] .pack-v2-icon.research    { color: #db2777; }
:root[data-theme="light"] .pack-v2-icon.report      { color: #e11d48; }

.pack-v2-info {
  flex: 1;
}
.pack-v2-info strong {
  display: block;
  font-size: 15px;
  font-weight: 800;
  color: var(--c-text);
  margin-bottom: 2px;
}
.pack-v2-info span {
  display: block;
  font-size: 12px;
  color: var(--c-muted);
  line-height: 1.45;
}

/* Pack bottom: qty + price */
.pack-v2-bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid rgba(255,255,255,0.04);
}
:root[data-theme="light"] .pack-v2-bottom {
  border-color: rgba(15,23,42,0.04);
}
.pack-v2-qty {
  font-size: 21px;
  font-weight: 950;
  color: var(--c-accent);
  letter-spacing: -0.5px;
}
.pack-v2-price {
  font-size: 23px;
  font-weight: 950;
  color: var(--c-text);
  letter-spacing: -0.5px;
  font-family: tabular-nums, Inter, system-ui;
}
.pack-v2-price span {
  font-size: 12px;
  font-weight: 600;
  color: var(--c-muted);
  margin-left: 2px;
}

/* Pack CTA button */
.pack-v2-btn {
  width: 100%;
  height: 38px;
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.01);
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
}
:root[data-theme="light"] .pack-v2-btn {
  border-color: rgba(15,23,42,0.1);
  background: rgba(15,23,42,0.01);
}
.pack-v2-btn:hover {
  border-color: var(--c-accent);
  color: var(--c-accent);
  background: rgba(99,102,241,0.06);
}
.pack-v2-btn.active {
  background: var(--c-accent);
  border-color: var(--c-accent);
  color: #fff;
  box-shadow: 0 4px 12px rgba(99,102,241,0.3);
}


/* Color Themes per tier */
.plan-card-v2.free   { --tier-h: 215; --tier-s: 25%; --tier-l: 50%; }
.plan-card-v2.lite   { --tier-h: 158; --tier-s: 64%; --tier-l: 36%; }
.plan-card-v2.plus   { --tier-h: 225; --tier-s: 80%; --tier-l: 52%; }
.plan-card-v2.pro    { --tier-h: 270; --tier-s: 75%; --tier-l: 52%; }

.plan-card-v2 {
  --tc: hsl(var(--tier-h), var(--tier-s), var(--tier-l));
  --tc-soft: hsla(var(--tier-h), var(--tier-s), var(--tier-l), 0.08);
  --tc-border: hsla(var(--tier-h), var(--tier-s), var(--tier-l), 0.25);
  --tc-glow: hsla(var(--tier-h), var(--tier-s), var(--tier-l), 0.18);

  position: relative;
  border-radius: 20px;
  background: var(--c-surface);
  border: 1.5px solid var(--tc-border);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  overflow: hidden;
}
.plan-card-v2::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, var(--tc-soft) 0%, transparent 60%);
  pointer-events: none;
  border-radius: inherit;
  transition: opacity 0.3s ease;
  opacity: 0.7;
}
.plan-card-v2:hover {
  transform: translateY(-5px);
  box-shadow: 0 16px 40px var(--tc-glow), 0 4px 12px rgba(0,0,0,0.06);
  border-color: var(--tc);
}
.plan-card-v2.active {
  border-color: var(--tc);
  box-shadow: 0 0 0 2px var(--tc), 0 16px 40px var(--tc-glow);
}
.plan-card-v2.active::before { opacity: 1; }
.plan-card-v2.featured {
  border-color: hsl(var(--tier-h), var(--tier-s), calc(var(--tier-l) + 5%));
}

/* Featured glow strip on top */
.featured-glow {
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--tc), hsl(calc(var(--tier-h) + 30), var(--tier-s), calc(var(--tier-l) + 10%)));
  border-radius: 20px 20px 0 0;
}

/* Badges */
.v2-current-badge {
  position: absolute;
  top: 14px; right: 14px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
  font-size: 10.5px;
  font-weight: 800;
  border: 1px solid rgba(16, 185, 129, 0.3);
}
.v2-hot-badge {
  position: absolute;
  top: 14px; right: 14px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  font-size: 10.5px;
  font-weight: 800;
  border: 1px solid rgba(239, 68, 68, 0.25);
}

/* Card Header */
.v2-card-header {
  display: flex;
  align-items: center;
  gap: 13px;
}
.v2-plan-icon-wrap {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: var(--tc);
  box-shadow: 0 4px 12px var(--tc-glow);
}
.v2-plan-icon {
  font-size: 18px;
  font-weight: 900;
  color: #fff;
  line-height: 1;
}
.v2-plan-meta h3 {
  margin: 0 0 3px;
  font-size: 17px;
  font-weight: 900;
  color: var(--c-text);
}
.v2-plan-meta p {
  margin: 0;
  font-size: 12px;
  color: var(--c-muted);
  line-height: 1.45;
}

/* Price block */
.v2-price-block {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.v2-price-free {
  font-size: 32px;
  font-weight: 950;
  color: var(--c-text);
  line-height: 1;
}
.v2-price-num {
  font-size: 14px;
  font-weight: 700;
  color: var(--c-muted);
  line-height: 1;
}
.v2-price-num strong {
  font-size: 36px;
  font-weight: 950;
  color: var(--tc);
  letter-spacing: -1px;
}
.v2-price-cycle {
  font-size: 13px;
  color: var(--c-muted);
  font-weight: 600;
}

/* Divider */
.v2-divider {
  height: 1px;
  background: linear-gradient(90deg, var(--tc-border), transparent);
}

/* Feature List */
.v2-features {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 9px;
  flex: 1;
}
.v2-features li {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  font-size: 13px;
}
.v2-features li.excluded {
  opacity: 0.45;
}
.v2-feat-icon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 900;
  color: #fff;
  background: var(--tc);
  margin-top: 1px;
}
.v2-features li.excluded .v2-feat-icon {
  background: rgba(148, 163, 184, 0.3);
  color: var(--c-subtle);
}
.v2-feat-text {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.v2-feat-text strong {
  font-weight: 700;
  color: var(--c-text);
  line-height: 1.3;
}
.v2-feat-text span {
  font-size: 11.5px;
  color: var(--tc);
  font-weight: 700;
}
.v2-features li.excluded .v2-feat-text span {
  color: var(--c-subtle);
}

/* CTA Button */
.v2-cta {
  width: 100%;
  height: 42px;
  border-radius: 12px;
  border: 1.5px solid var(--tc-border);
  background: var(--tc-soft);
  color: var(--tc);
  font-size: 13.5px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.25s ease;
  margin-top: auto;
}
.v2-cta:hover {
  background: var(--tc);
  color: #fff;
  border-color: var(--tc);
  box-shadow: 0 6px 18px var(--tc-glow);
  transform: translateY(-1px);
}
.v2-cta-selected {
  background: var(--tc) !important;
  color: #fff !important;
  border-color: var(--tc) !important;
  box-shadow: 0 6px 18px var(--tc-glow) !important;
}

/* ══════════════════════════════════════════════════
   POWER PACKS — V2 Premium Grid
   ══════════════════════════════════════════════════ */
.packs-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
@media (max-width: 960px) {
  .packs-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 580px) {
  .packs-grid { grid-template-columns: 1fr; }
}

/* Color themes for each pack type */
.pack-card.review      { --pk-h: 258; --pk-s: 90%; --pk-l: 58%; }
.pack-card.ppt         { --pk-h: 160; --pk-s: 70%; --pk-l: 38%; }
.pack-card.chat        { --pk-h: 37;  --pk-s: 90%; --pk-l: 50%; }
.pack-card.translation { --pk-h: 215; --pk-s: 75%; --pk-l: 52%; }
.pack-card.research    { --pk-h: 330; --pk-s: 82%; --pk-l: 55%; }
.pack-card.report      { --pk-h: 5;   --pk-s: 88%; --pk-l: 55%; }

.pack-card {
  --pk: hsl(var(--pk-h), var(--pk-s), var(--pk-l));
  --pk-soft: hsla(var(--pk-h), var(--pk-s), var(--pk-l), 0.08);
  --pk-border: hsla(var(--pk-h), var(--pk-s), var(--pk-l), 0.22);
  --pk-glow: hsla(var(--pk-h), var(--pk-s), var(--pk-l), 0.2);

  position: relative;
  border-radius: 20px;
  background: var(--c-surface);
  border: 1.5px solid var(--pk-border);
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  overflow: hidden;
}
.pack-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(145deg, var(--pk-soft) 0%, transparent 55%);
  pointer-events: none;
  border-radius: inherit;
  opacity: 0.6;
  transition: opacity 0.3s ease;
}
.pack-card:hover {
  transform: translateY(-4px);
  border-color: var(--pk);
  box-shadow: 0 16px 36px var(--pk-glow), 0 4px 12px rgba(0,0,0,0.05);
}
.pack-card:hover::before { opacity: 1; }
.pack-active {
  border-color: var(--pk) !important;
  box-shadow: 0 0 0 2px var(--pk), 0 16px 36px var(--pk-glow) !important;
}
.pack-active::before { opacity: 1 !important; }

/* Decorative blob */
.pack-blob {
  position: absolute;
  bottom: -30px; right: -30px;
  width: 100px; height: 100px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--pk-soft), transparent 70%);
  pointer-events: none;
  transition: transform 0.4s ease;
}
.pack-card:hover .pack-blob {
  transform: scale(1.4);
}

/* Pack top row */
.pack-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pack-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: var(--pk);
  color: #fff;
  box-shadow: 0 4px 12px var(--pk-glow);
  flex-shrink: 0;
}
.pack-icon-wrap svg {
  width: 20px;
  height: 20px;
}
.pack-label-tag {
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--pk-soft);
  color: var(--pk);
  font-size: 10.5px;
  font-weight: 800;
  border: 1px solid var(--pk-border);
}

/* Pack body */
.pack-body h3 {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 850;
  color: var(--c-text);
}
.pack-body p {
  margin: 0;
  font-size: 12.5px;
  color: var(--c-muted);
  line-height: 1.45;
}

/* Pack bottom */
.pack-bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 4px;
}
.pack-quantity {
  font-size: 20px;
  font-weight: 900;
  color: var(--pk);
  letter-spacing: -0.3px;
}
.pack-pricing {
  display: flex;
  align-items: baseline;
  gap: 2px;
}
.pack-pricing strong {
  font-size: 26px;
  font-weight: 950;
  color: var(--c-text);
  letter-spacing: -0.5px;
}
.pack-pricing span {
  font-size: 12px;
  color: var(--c-muted);
  font-weight: 600;
}

/* Pack CTA */
.pack-cta {
  width: 100%;
  height: 38px;
  border-radius: 10px;
  border: 1.5px solid var(--pk-border);
  background: transparent;
  color: var(--pk);
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
  transition: all 0.25s ease;
}
.pack-cta:hover {
  background: var(--pk);
  color: #fff;
  border-color: var(--pk);
  box-shadow: 0 4px 14px var(--pk-glow);
}
.pack-cta-active {
  background: var(--pk) !important;
  color: #fff !important;
  border-color: var(--pk) !important;
  box-shadow: 0 4px 14px var(--pk-glow) !important;
}

/* Premium Card Color-Coded Themes */
.plan-card.free {
  --tier: #64748b;
  --tier-soft: rgba(100, 116, 139, 0.05);
  --tier-line: rgba(100, 116, 139, 0.2);
}
.plan-card.lite {
  --tier: #0b946f;
  --tier-soft: rgba(11, 148, 111, 0.05);
  --tier-line: rgba(11, 148, 111, 0.22);
}
.plan-card.plus {
  --tier: #2664ea;
  --tier-soft: rgba(38, 100, 234, 0.05);
  --tier-line: rgba(38, 100, 234, 0.22);
}
.plan-card.pro {
  --tier: #7a2fe3;
  --tier-soft: rgba(122, 47, 227, 0.05);
  --tier-line: rgba(122, 47, 227, 0.22);
}
.plan-card.team_plus {
  --tier: #d97706;
  --tier-soft: rgba(217, 119, 6, 0.05);
  --tier-line: rgba(217, 119, 6, 0.22);
}
.plan-card.team_pro {
  --tier: #b45309;
  --tier-soft: rgba(180, 83, 9, 0.05);
  --tier-line: rgba(180, 83, 9, 0.22);
}

.plan-card {
  position: relative;
  border-radius: 16px;
  background: linear-gradient(180deg, var(--tier-soft), var(--c-surface) 60%) !important;
  border: 1px solid var(--tier-line) !important;
  box-shadow: var(--sh-sm);
  padding: 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.plan-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.08);
}
.plan-card.active {
  border-color: var(--tier) !important;
  box-shadow: 0 0 0 2px var(--tier), 0 12px 28px rgba(0, 0, 0, 0.12) !important;
}

.plan-card header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}
.plan-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.plan-title-row h3 {
  margin: 0;
  font-size: 19px;
  font-weight: 900;
  color: var(--c-text);
}
.plan-title-row em {
  font-style: normal;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--c-surface);
  border: 1px solid var(--tier-line);
  color: var(--tier);
  font-size: 10.5px;
  font-weight: 850;
}
.plan-card .plan-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--c-muted);
  line-height: 1.55;
}
.plan-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 10px;
  color: #fff;
  background: var(--tier);
  font-weight: 900;
  font-size: 16px;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.12);
}

.price-line {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 6px 9px;
  margin: 12px 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
}

/* Premium Benefit Ladder CSS */
.benefit-ladder {
  overflow: hidden;
  border: 1px solid var(--c-border);
  border-radius: 12px;
  background: var(--c-surface);
  margin-bottom: 16px;
}

.ladder-head,
.ladder-row {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  min-height: 38px;
  padding: 0 12px;
  border-bottom: 1px solid var(--c-border);
}

.ladder-head {
  grid-template-columns: minmax(0, 1fr) auto;
  color: var(--c-text);
  background: rgba(0, 0, 0, 0.02);
  font-weight: 850;
  font-size: 12.5px;
}
:root[data-theme="dark"] .ladder-head {
  background: rgba(255, 255, 255, 0.02);
}

.ladder-head b {
  color: var(--tier);
  font-size: 11px;
}

.ladder-row {
  font-size: 12px;
}
.ladder-row.not-included {
  opacity: 0.55;
}

.ladder-info {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}
.ladder-info strong {
  color: var(--c-text);
  font-weight: 700;
}
.ladder-info small {
  color: var(--c-muted);
  font-size: 10px;
  margin-top: 1px;
}

.ladder-row b {
  color: var(--c-text);
  font-weight: 800;
  font-size: 12px;
  text-align: right;
  white-space: nowrap;
}

.row-icon {
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #10b981;
  font-size: 10px;
  font-weight: 900;
}
.ladder-row.not-included .row-icon {
  background: #ef4444;
}

.settlement-note {
  margin: 0;
  padding: 8px 10px;
  color: var(--c-muted);
  text-align: center;
  font-size: 10.5px;
  background: rgba(0, 0, 0, 0.01);
}

.plan-actions {
  margin-top: 14px;
}
.solid-buy {
  width: 100%;
  height: 40px;
  border: none;
  border-radius: 10px;
  color: #fff;
  background: var(--tier);
  font-size: 13.5px;
  font-weight: 850;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}
.solid-buy:hover {
  transform: translateY(-1.5px);
  filter: brightness(1.08);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}
.solid-buy.active {
  filter: brightness(0.95);
}

.plan-sale-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 0 0 10px;
  padding: 8px 10px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.14), rgba(245, 158, 11, 0.14));
  border: 1px solid rgba(248, 113, 113, 0.24);
  color: #b91c1c;
  font-size: 11px;
  font-weight: 900;
}

:root[data-theme="dark"] .plan-sale-strip {
  color: #fecaca;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.18), rgba(245, 158, 11, 0.16));
  border-color: rgba(251, 191, 36, 0.28);
}

.plan-sale-strip strong {
  flex-shrink: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: #ef4444;
  font-size: 10.5px;
  letter-spacing: 0;
}

:root[data-theme="dark"] .plan-sale-strip strong {
  color: #fbbf24;
}

.price-line.sale {
  align-items: baseline;
}

.price-line .origin-price {
  color: var(--c-muted);
  text-decoration: line-through;
  text-decoration-thickness: 2px;
  font-size: 13px;
  min-width: 0;
}

.price-line.sale strong {
  color: #ef4444;
}

:root[data-theme="dark"] .price-line.sale strong {
  color: #fb7185;
}

.price-line strong {
  font-size: clamp(28px, 2.5vw, 32px);
  font-weight: 950;
  color: var(--c-text);
  font-family: tabular-nums;
  line-height: 1;
}
.price-line span {
  font-size: 12px;
  color: var(--c-muted);
  font-weight: 700;
}

.team-seats-selector {
  margin: 0 0 18px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 12px;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(129, 140, 248, 0.3);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.seats-label {
  font-size: 13px;
  font-weight: 850;
  color: var(--c-text);
  white-space: nowrap;
}
.seats-counter {
  display: flex;
  align-items: center;
  gap: 8px;
}
.counter-btn {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid rgba(129, 140, 248, 0.35);
  background: rgba(99, 102, 241, 0.14);
  color: var(--c-text);
  font-size: 16px;
  font-weight: 900;
  display: grid;
  place-items: center;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
}
.counter-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.counter-btn:not(:disabled):hover {
  background: rgba(99, 102, 241, 0.28);
  border-color: rgba(129, 140, 248, 0.6);
  transform: scale(1.05);
}

.counter-value-box {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.45);
  border: 1px solid rgba(129, 140, 248, 0.35);
}
:root[data-theme="light"] .counter-value-box {
  background: #ffffff;
}

.counter-input {
  width: 36px;
  height: 24px;
  border: none;
  background: transparent;
  color: var(--c-text);
  text-align: center;
  font-size: 15px;
  font-weight: 900;
  outline: none;
  padding: 0;
  margin: 0;
  font-family: inherit;
}
.counter-unit {
  font-size: 13px;
  font-weight: 850;
  color: #818cf8;
}

/* Feature list */
.center-plan-features {
  list-style: none;
  padding: 0;
  margin: 0 0 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.center-plan-features li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.feature-check {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(16, 185, 129, 0.16);
  color: #10b981;
  font-size: 12px;
  font-weight: 900;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  margin-top: 1px;
}
.feature-check.excluded {
  background: rgba(239, 68, 68, 0.16);
  color: #ef4444;
}

.luckin-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  width: fit-content;
  max-width: 100%;
  flex: 0 0 100%;
  margin-top: 6px;
  padding: 3px 11px 3px 5px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 850;
  color: #a5b4fc;
  background: rgba(49, 46, 129, 0.45);
  border: 1px solid rgba(129, 140, 248, 0.35);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.18);
  white-space: normal;
}
.luckin-icon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  object-fit: cover;
}
.center-plan-features li div strong {
  display: block;
  font-size: 13px;
  font-weight: 800;
  color: var(--c-text);
}
.center-plan-features li div small {
  display: block;
  font-size: 11.5px;
  color: var(--c-muted);
}

.settlement-note {
  font-size: 11px;
  color: var(--c-subtle);
  margin-bottom: 16px;
  line-height: 1.5;
}

.plan-buy-button {
  width: 100%;
  height: 42px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-text);
  font-size: 13.5px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
}
.plan-card:hover .plan-buy-button,
.plan-card.active .plan-buy-button {
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.38);
}

/* ── Checkout Bar ────────────────────────────────────────── */
.checkout-bar {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  width: min(1020px, calc(100vw - 32px));
  padding: 16px 26px;
  border-radius: var(--r);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: var(--sh-lg);
  backdrop-filter: blur(24px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}
@media (max-width: 700px) {
  .checkout-bar { flex-direction: column; align-items: stretch; }
}

.checkout-bar > div:first-child span {
  font-size: 11px;
  font-weight: 800;
  color: var(--c-subtle);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.checkout-bar > div:first-child strong {
  display: block;
  font-size: 16px;
  font-weight: 900;
  color: var(--c-text);
}
.checkout-bar > div:first-child p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--c-muted);
}

.checkout-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.pay-methods {
  display: flex;
  gap: 6px;
}
.pay-methods button {
  height: 38px;
  padding: 0 16px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.18s;
}
.pay-methods button:disabled {
  cursor: default;
  opacity: 1;
}
.pay-methods button.active {
  border-color: var(--c-accent);
  color: var(--c-accent);
  background: rgba(99, 102, 241, 0.08);
}
.pay-methods button i {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(99, 102, 241, 0.15);
  font-style: normal;
  font-size: 10px;
  font-weight: 900;
  display: grid;
  place-items: center;
}

.primary-button {
  height: 44px;
  padding: 0 26px;
  border-radius: var(--r-pill);
  border: none;
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #ffffff;
  font-size: 14px;
  font-weight: 850;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.35);
  transition: all 0.2s ease;
  white-space: nowrap;
}
.primary-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.45);
}
.primary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* ── Orders Section ──────────────────────────────────────── */
.orders-section {
  position: relative;
  z-index: 2;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--r);
  padding: 26px 30px;
  box-shadow: var(--sh-sm);
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.section-title span {
  font-size: 11px;
  font-weight: 800;
  color: var(--c-subtle);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.section-title h2 {
  margin: 2px 0 0;
  font-size: 18px;
  font-weight: 900;
  color: var(--c-text);
}

.orders-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.order-head {
  display: grid;
  grid-template-columns: 1.5fr 1.2fr 1fr 1fr 1fr;
  padding: 8px 16px;
  font-size: 12px;
  font-weight: 750;
  color: var(--c-subtle);
  border-bottom: 1px solid var(--c-border);
}
.order-item {
  display: grid;
  grid-template-columns: 1.5fr 1.2fr 1fr 1fr 1fr;
  align-items: center;
  padding: 12px 16px;
  border-radius: var(--r-sm);
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  font-size: 13px;
  color: var(--c-text);
}
.order-item strong { font-weight: 800; }
.order-item small { color: var(--c-muted); display: block; font-size: 11px; }

.status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: var(--r-pill);
  font-size: 11px;
  font-weight: 800;
  width: fit-content;
}
.status.paid, .status.success { background: rgba(16, 185, 129, 0.12); color: #10b981; }
.status.pending, .status.pending_payment, .status.created, .status.config_required { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
.status.failed, .status.cancelled { background: rgba(239, 68, 68, 0.12); color: #ef4444; }

.ticket-button {
  height: 30px;
  padding: 0 12px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: transparent;
  color: var(--c-muted);
  font-size: 12px;
  font-weight: 750;
  cursor: pointer;
  transition: all 0.18s;
  width: fit-content;
}
.ticket-button:hover { border-color: var(--c-accent); color: var(--c-accent); }

.orders-empty {
  padding: 36px;
  text-align: center;
  color: var(--c-subtle);
  font-size: 13px;
  background: var(--c-bg);
  border-radius: var(--r-sm);
  border: 1px dashed var(--c-border);
}

/* ── Ticket Dialog ───────────────────────────────────────── */
.ticket-dialog {
  border: 1px solid var(--c-border);
  border-radius: var(--r);
  background: var(--c-surface);
  color: var(--c-text);
  box-shadow: var(--sh-lg);
  padding: 28px 32px;
  width: min(480px, calc(100vw - 32px));
}

.wechat-pay-dialog {
  border: 1px solid var(--c-border);
  border-radius: 24px;
  background: var(--c-surface);
  color: var(--c-text);
  box-shadow: var(--sh-lg);
  padding: 28px;
  width: min(560px, calc(100vw - 32px));
}

.wechat-pay-dialog::backdrop {
  background: rgba(2, 6, 23, 0.62);
  backdrop-filter: blur(8px);
}

.wechat-pay-body {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 22px;
  align-items: center;
}

.wechat-qr-shell {
  width: 220px;
  height: 220px;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.14);
  display: grid;
  place-items: center;
  padding: 14px;
}

.wechat-qr-shell img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.wechat-qr-loading {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.wechat-pay-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.wechat-pay-meta strong {
  font-size: 12px;
  color: var(--c-muted);
  word-break: break-all;
}

.wechat-pay-meta span {
  font-size: 15px;
  font-weight: 850;
}

.wechat-pay-meta b {
  font-size: 34px;
  line-height: 1;
  color: #22c55e;
}

.wechat-pay-meta p {
  margin: 4px 0 0;
  color: var(--c-muted);
  font-size: 13px;
  line-height: 1.7;
}

.wechat-pay-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

@media (max-width: 640px) {
  .wechat-pay-body {
    grid-template-columns: 1fr;
  }
  .wechat-qr-shell {
    margin: 0 auto;
  }
}

.ticket-dialog::backdrop {
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(6px);
}
.dialog-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}
.dialog-heading span { font-size: 11px; font-weight: 800; color: var(--c-accent); text-transform: uppercase; }
.dialog-heading h2 { margin: 2px 0 0; font-size: 18px; font-weight: 900; }
.close-button { border: none; background: transparent; color: var(--c-muted); font-size: 20px; cursor: pointer; }

.ticket-dialog label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
  font-size: 12.5px;
  font-weight: 750;
  color: var(--c-muted);
}
.ticket-dialog input, .ticket-dialog select, .ticket-dialog textarea {
  padding: 10px 14px;
  border-radius: var(--r-sm);
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-text);
  font-size: 13.5px;
  outline: none;
}
.ticket-dialog input:focus, .ticket-dialog select:focus, .ticket-dialog textarea:focus {
  border-color: var(--c-accent);
}
</style>
