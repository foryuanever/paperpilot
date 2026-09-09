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

      <!-- Stacked subscriptions list -->
      <div v-if="membership.stacked && membership.stacked.length > 1" class="membership-stack-list animate-hover-up" style="margin-top: 16px; padding: 16px 24px; border-radius: var(--r); background: var(--c-surface); border: 1px solid var(--c-border); display: flex; flex-direction: column; gap: 10px; width: 100%; box-sizing: border-box; box-shadow: var(--sh-sm); position: relative; z-index: 2;">
        <span style="font-size: 0.78rem; font-weight: 850; color: var(--c-muted); text-transform: uppercase; letter-spacing: 0.05em; display: flex; align-items: center; gap: 6px;">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
          已购套餐队列
        </span>
        <div style="display: flex; flex-direction: column; gap: 8px;">
          <div v-for="(sub, idx) in membership.stacked" :key="idx" style="display: flex; justify-content: space-between; align-items: center; font-size: 0.85rem; padding: 6px 0; border-bottom: 1px dashed var(--c-border); margin-bottom: 4px;">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-weight: 800; color: var(--c-text);">{{ sub.planName }}</span>
              <span v-if="sub.status === 'active'" style="background: rgba(16, 185, 129, 0.12); color: #10b981; font-size: 10px; font-weight: 850; padding: 2px 6px; border-radius: var(--r-sm);">当前生效</span>
              <span v-else style="background: rgba(245, 158, 11, 0.12); color: #f59e0b; font-size: 10px; font-weight: 850; padding: 2px 6px; border-radius: var(--r-sm);">排队中 (暂挂)</span>
            </div>
            <span style="color: var(--c-muted); font-size: 0.8rem;">有效期至 {{ formatFullDate(sub.expiresAt) }}</span>
          </div>
        </div>
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
          <div v-for="item in benefitItems" :key="item.key" class="entitlement-row-group">
          <button type="button" class="linear-row" :class="{ expanded: expandedBenefit === item.key }" :disabled="item.isFeature" @click="!item.isFeature && toggleBenefit(item.key)">
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
                <small v-else-if="item.key === 'agent'">当前余额 {{ item.quota }} 积分</small>
                <small v-else>{{ item.isDaily ? '每日额度' : '当期额度' }} {{ item.quota }} {{ item.unit }}</small>
              </div>
            </div>

            <div class="row-center-meter">
              <div v-if="!item.unlimited && !item.isFeature && item.quota > 0" class="linear-meter-track">
                <b :style="{ width: `${quotaPercent(item)}%`, background: getMeterColor(item) }"></b>
              </div>
              <span v-else-if="item.unlimited" class="linear-unlimited-label">✓ 不限次数</span>
              <span v-else-if="item.isFeature" class="linear-feature-badge" :class="{ active: item.included }">
                {{ item.included ? '✓ ' + (item.value || '包含') : '× 未包含' }}
              </span>
              <span v-else-if="item.isTeam && !item.quota" class="linear-disabled-label">未开放</span>
            </div>

            <div class="row-right">
              <span class="row-stat-text">{{ benefitUsageLabel(item) }}</span>
              <span class="row-expand-icon" aria-hidden="true">{{ expandedBenefit === item.key ? '⌃' : '⌄' }}</span>
            </div>
          </button>
          <section v-if="expandedBenefit === item.key" class="benefit-detail-panel">
            <header>
              <strong>{{ item.label }}使用明细</strong>
              <span>{{ membershipStartLabel }} 至 {{ membershipEndLabel }}</span>
            </header>
            <div class="benefit-detail-filters">
              <label>开始日期<input v-model="detailStartDate" type="date" @change="loadBenefitDetails" /></label>
              <label>结束日期<input v-model="detailEndDate" type="date" @change="loadBenefitDetails" /></label>
            </div>
            <div v-if="detailsLoading" class="benefit-detail-empty">正在加载使用记录...</div>
            <div v-else-if="benefitDetails.length === 0" class="benefit-detail-empty">所选日期内暂无使用记录。</div>
            <div v-else class="benefit-detail-table-wrap">
              <table class="benefit-detail-table"><thead><tr><th>时间</th><th>场景</th><th>论文</th><th v-if="isCountBenefit(expandedBenefit)">扣减</th><th>结果</th></tr></thead>
                <tbody>
                  <tr v-for="row in benefitDetails" :key="row.id">
                    <td>{{ formatDetailTime(row.time) }}</td>
                    <td>
                      <span v-if="isRestoreBenefit(row)" class="restore-scene-tag">{{ row.sceneLabel || '重置福利' }}</span>
                      <span v-else>{{ row.sceneLabel || 'AI 调用' }}</span>
                    </td>
                    <td>{{ isPaperlessScene(row.scene) ? '-' : (row.paper || '-') }}</td>
                    <td v-if="isCountBenefit(expandedBenefit)">
                      <span v-if="isRestoreBenefit(row)" class="detail-full-blood">满血</span>
                      <span v-else :class="row.delta > 0 ? 'detail-add' : row.effective ? 'detail-deduct' : 'detail-muted'">{{ row.delta > 0 ? '+' + row.delta : row.delta }}</span>
                    </td>
                    <td>
                      <span v-if="isRestoreBenefit(row)" class="detail-full-blood">已恢复满血</span>
                      <span v-else :class="row.effective ? 'detail-success' : 'detail-failed'">{{ row.status }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div class="benefit-detail-pagination">
                <span>{{ benefitDetailTotal ? `第 ${benefitDetailPage}/${benefitDetailTotalPages} 页，共 ${benefitDetailTotal} 条` : '暂无记录' }}</span>
                <button type="button" :disabled="benefitDetailPage <= 1 || detailsLoading" @click="changeBenefitDetailPage(benefitDetailPage - 1)">上一页</button>
                <button type="button" :disabled="benefitDetailPage >= benefitDetailTotalPages || detailsLoading" @click="changeBenefitDetailPage(benefitDetailPage + 1)">下一页</button>
              </div>
            </div>
          </section>
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
          @click="selectPlanForCheckout(plan.id)"
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
              <p style="margin-top: 8px; min-height: 36px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">{{ plan.topUpPack ? "一次性购买后立即叠加当前额度，不影响正在生效的会员套餐。" : planCopy(plan.id) }}</p>
            </div>

            <!-- Price -->
            <div class="plan-price-row" style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap; min-height: 32px;">
              <template v-if="Number(planPrice(plan)) <= 0 && !isSeckillActive(plan)">
                <span class="price-main" style="font-size: 26px;">免费</span>
                <span class="price-sub">永久开放</span>
              </template>
              <template v-else>
                <span class="price-main">¥<em>{{ planPrice(plan) }}</em></span>
                <span class="price-sub">{{ plan.topUpPack ? "一次性" : `/ ${cycleShortLabel(selectedCycle)}` }}</span>
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
              <span v-if="row.included" class="feat-status-icon status-ok" style="background: rgba(16, 185, 129, 0.15) !important; color: #10b981 !important; border: 1px solid rgba(16, 185, 129, 0.25);">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round" class="feat-svg-icon" style="width: 10px; height: 10px;"><polyline points="20 6 9 17 4 12"/></svg>
              </span>
              <span v-else class="feat-status-icon status-no" style="background: rgba(239, 68, 68, 0.15) !important; color: #ef4444 !important; border: 1px solid rgba(239, 68, 68, 0.25); display: flex; align-items: center; justify-content: center;">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" style="width: 9px; height: 9px;"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
              </span>
              <span class="feat-label">{{ row.label }}</span>
              <span class="feat-val" :style="{ color: row.included ? 'var(--c-text)' : 'rgba(239,68,68,0.75)', textDecoration: row.included ? 'none' : 'none' }">{{ row.value }}</span>
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

    <!-- ── Floating Checkout Bar ───────────────────────────────── -->
    <section v-if="showCheckoutBar" class="checkout-bar">
      <div>
        <span>本次开通</span>
        <strong>{{ selectedPlanInfo.name }}{{ selectedPlanInfo.topUpPack ? ' (即时叠加)' : ' · ' + cycleLabel(selectedCycle) }}</strong>
        <p>{{ checkoutDescription }}</p>
      </div>
      <div class="checkout-actions">
        <div class="pay-methods" v-if="selectedPlanInfo.id && selectedPlanInfo.id !== 'free'">
          <button type="button" :class="{ active: payMethod === 'redeem' }" @click="payMethod = 'redeem'" style="display: flex; align-items: center; justify-content: center; gap: 6px;">
            <span style="font-size: 1.1rem; line-height: 1;">🎟️</span>
            兑换通道
          </button>
          <button v-if="!selectedPlanRequiresRedeem" type="button" :class="{ active: payMethod === 'wechat' }" @click="payMethod = 'wechat'" style="display: flex; align-items: center; justify-content: center; gap: 6px;">
            <svg class="wechat-pay-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="width: 18px; height: 18px;">
              <rect width="24" height="24" rx="5" fill="#07C160"/>
              <path d="M12 17.5c2.485 0 4.5-1.79 4.5-4s-2.015-4-4.5-4-4.5 1.79-4.5 4c0 .878.318 1.69.856 2.348l-.348 1.152 1.348-.674c.642.176 1.314.274 2.144.274z" fill="#FFF"/>
              <path d="M10.2 13.5l1.3 1.3 2.8-3" stroke="#07C160" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            微信支付
          </button>
        </div>
        <button v-if="payMethod === 'wechat' && !selectedPlanRequiresRedeem" class="primary-button" :disabled="paying" @click="checkout">
          {{ paying ? "正在创建订单..." : `¥${planPrice(selectedPlanInfo)} 去支付` }}
        </button>
        <div v-else style="display: flex; gap: 8px; align-items: center;">
          <input v-model.trim="redeemCode" placeholder="输入 5 位兑换码" maxlength="5" style="padding: 0 12px; height: 38px; border-radius: var(--r-pill); border: 1px solid var(--c-border); background: var(--c-bg); color: var(--c-text); text-align: center; text-transform: uppercase; font-family: monospace; font-size: 14px; letter-spacing: 2px; outline: none; width: 170px;" />
          <button class="primary-button" :disabled="redeeming || !redeemCode" @click="submitRedeemCode" style="margin-top: 0; height: 38px; line-height: 38px; padding: 0 18px; display: flex; align-items: center; justify-content: center;">
            {{ redeeming ? "兑换中..." : "立即兑换" }}
          </button>
        </div>
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
        <label v-if="ticket.type === 'refund'">退款支付宝账号<input v-model.trim="ticket.alipayAccount" placeholder="请输入用于接收退款的支付宝账号" required /></label>
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
            <span>{{ selectedPlanInfo.name }} · {{ selectedPlanInfo.topUpPack ? "加油包" : cycleLabel(selectedCycle) }}</span>
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
import { useAuthStore } from "../stores/auth";
import { paperpilotApi } from "../services/paperpilotApi";
import { getCurrentApiBaseUrl } from "../services/apiClient";
import goldCardReference from "../assets/membership/gold-card-cropped.jpg";
import luckinLogo from "../assets/luckin-logo.png";

const usageStore = useUsageStore();
const authStore = useAuthStore();
const loading = ref(false);
const expandedBenefit = ref("");
const benefitDetails = ref([]);
const benefitDetailPage = ref(1);
const benefitDetailPageSize = ref(10);
const benefitDetailTotal = ref(0);
const benefitDetailTotalPages = ref(1);
const detailsLoading = ref(false);
const detailStartDate = ref("");
const detailEndDate = ref("");
const paying = ref(false);
const payMethod = ref("wechat");
const redeemCode = ref("");
const redeeming = ref(false);
const ordersLoading = ref(false);
const orders = ref([]);
const filteredOrders = computed(() => {
  return orders.value.filter(order => order.status === "paid" || order.status === "refunded");
});
const provider = ref("wechat");
const selectedCycle = ref("monthly");
const selectedPlan = ref("plus");
const showCheckoutBar = ref(false);
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

const defaultPlans = [];

const fetchedPlans = ref([]);
const publicPlansLoaded = ref(false);
const planOrder = ["free", "lite", "plus", "pro", "team_plus", "team_pro"];
const displayPlans = computed(() => {
  // The public API is authoritative; before it answers, render no plan values.
  const source = publicPlansLoaded.value ? (fetchedPlans.value || []) : defaultPlans;
  return source
    .map((plan) => {
      const id = plan.id && plan.id.startsWith("pack_") ? plan.id : normalizePlanId(plan.id);
      return { ...plan, id };
    })
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
const personalPlans = computed(() => displayPlans.value.filter((item) => !item.teamShared));
const membership = computed(() => usageStore.state.membership || { id: "free", name: "未开通会员", benefits: {} });
const membershipStartLabel = computed(() => formatFullDate(membership.value.startedAt || membership.value.startAt) || "开通日");
const membershipEndLabel = computed(() => formatFullDate(membership.value.expiresAt) || "到期日");
const selectedPlanInfo = computed(() => {
  const id = selectedPlan.value || "";
  return displayPlans.value.find((item) => item.id === id) || displayPlans.value[0] || { name: "暂未上架套餐", monthlyPrice: 0, reviewQuota: 0, pptQuota: 0, chatQuota: 0 };
});
const selectedPlanRequiresRedeem = computed(() => {
  const plan = selectedPlanInfo.value;
  return Boolean(plan?.seckillActive && Number(planPrice(plan)) <= 0);
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
  }[key] || "box-purple";
}

const benefitItems = computed(() => {
  const benefits = membership.value.benefits || {};
  const currentPlanId = normalizePlanId(membership.value.id || "free");
  const planInfo = displayPlans.value.find((p) => p.id === currentPlanId) || displayPlans.value[0] || {};

  const items = [
    { key: "import", label: "论文插件导入", unlimited: true },
    { key: "translation", label: "对照翻译", unlimited: false, quota: benefits.translation?.quota !== undefined ? benefits.translation.quota : (planInfo?.translateQuotaDaily || 0), used: benefits.translation?.used || 0, unit: "篇/天" },
    { key: "immersive", label: "沉浸全篇翻译", unlimited: false, quota: benefits.immersive?.quota !== undefined ? benefits.immersive.quota : (planInfo?.immersiveQuotaDaily || 0), used: benefits.immersive?.used || 0, unit: "篇/天" },
    { key: "ppt", label: "PPT 生成", unlimited: false, quota: benefits.ppt?.quota !== undefined ? benefits.ppt.quota : (planInfo.pptQuotaMonthly || 0), used: benefits.ppt?.used || 0, unit: "次/月" },
    { key: "agent", label: "积分", unlimited: false, quota: authStore.profile.fruitScore || 0, used: 0, unit: "积分" },
    { key: "forumBadge", label: "论坛会员标识", isFeature: true, included: planInfo.forumSpecial },
    { key: "forumTop", label: "发帖置顶", isFeature: true, included: Number(planInfo.forumTopDaily || 0) > 0, value: planInfo.forumTopDaily ? "包含" : "未包含" },
    { key: "peakPriority", label: "高峰期优先通道", isFeature: true, included: planInfo.peakPriority, value: planInfo.peakPriority ? "优先通道" : "标准通道" },
  ];

  return items.map(item => {
    const isDaily = item.unit && item.unit.endsWith("/天");
    if (isDaily) {
      const dailyQuota = Number(item.quota || 0);
      const dailyUsed = Number(item.used || 0);
      const dailyRemaining = Math.max(0, dailyQuota - dailyUsed);

      return {
        ...item,
        isDaily: true,
        dailyRemaining,
        quota: dailyQuota,
        used: dailyUsed,
        unit: item.unit
      };
    } else {
      const quota = Number(item.quota || 0);
      const used = Number(item.used || 0);
      const remaining = Math.max(0, quota - used);
      return {
        ...item,
        isDaily: false,
        remaining,
        quota,
        used,
        unit: item.unit || "次"
      };
    }
  });
});

const checkoutDescription = computed(() => {
  const plan = selectedPlanInfo.value;
  if (plan.topUpPack) {
    return "购买后立即叠加下列额度，不改变当前套餐、有效期和已用次数。";
  }
  const count = plan.teamShared ? teamMemberCount.value : 0;
  return [
    `对照 ${plan.translateQuotaDaily || 0} 篇/天`,
    `沉浸 ${plan.immersiveQuotaDaily || 0} 篇/天`,
    `PPT ${plan.pptQuotaMonthly || 0} 次/月`,
    plan.teamShared ? `团队 ${count} 人席位` : "",
  ].filter(Boolean).join(" · ");
});

onMounted(() => {
  saleTimer = window.setInterval(() => {
    nowTick.value = Date.now();
  }, 40);
  window.addEventListener("paperpilot:membership-plans-changed", refreshPublicMembershipPlans);
  load();
  loadOrders();
});

onBeforeUnmount(() => {
  if (saleTimer) {
    clearInterval(saleTimer);
    saleTimer = null;
  }
  if (paymentPollTimer) {
    clearInterval(paymentPollTimer);
    paymentPollTimer = null;
  }
  window.removeEventListener("paperpilot:membership-plans-changed", refreshPublicMembershipPlans);
});

function toggleBenefit(key) {
  expandedBenefit.value = expandedBenefit.value === key ? "" : key;
  benefitDetailPage.value = 1;
  if (expandedBenefit.value) loadBenefitDetails();
}

async function loadBenefitDetails() {
  if (!expandedBenefit.value) return;
  detailsLoading.value = true;
  try {
    const data = await usageStore.fetchDetails({
      benefit: expandedBenefit.value,
      startDate: detailStartDate.value || undefined,
      endDate: detailEndDate.value || undefined,
      page: benefitDetailPage.value,
      pageSize: benefitDetailPageSize.value,
    });
    benefitDetails.value = Array.isArray(data) ? data : (data.rows || []);
    benefitDetailTotal.value = Array.isArray(data) ? data.length : Number(data.total || 0);
    benefitDetailTotalPages.value = Array.isArray(data) ? 1 : Math.max(1, Number(data.totalPages || 1));
  } catch (error) {
    benefitDetails.value = [];
    benefitDetailTotal.value = 0;
    benefitDetailTotalPages.value = 1;
    console.warn("load usage details failed", error);
  } finally {
    detailsLoading.value = false;
  }
}

function changeBenefitDetailPage(target) {
  benefitDetailPage.value = Math.min(Math.max(1, target), benefitDetailTotalPages.value);
  loadBenefitDetails();
}

function formatDetailTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("zh-CN", { hour12: false });
}

function isCountBenefit(key) {
  return ["translation", "immersive", "ppt", "agent"].includes(key);
}

function isRestoreBenefit(row) {
  if (!row) return false;
  return row.deltaLabel === "满血"
    || row.scene === "restore-benefit"
    || row.scene === "gift-restore"
    || row.action === "restore-benefit"
    || row.action === "restore-full"
    || String(row.sceneLabel || "").includes("重置福利")
    || String(row.sceneLabel || "").includes("恢复满血");
}

function isPaperlessScene(scene) {
  const s = String(scene || "").toLowerCase();
  return s === "checkin" || s === "recharge" || s === "admin-gift" || s === "restore-benefit" || s.startsWith("gift");
}

onBeforeUnmount(() => {
  if (saleTimer) window.clearInterval(saleTimer);
  window.removeEventListener("paperpilot:membership-plans-changed", refreshPublicMembershipPlans);
  stopPaymentPolling();
});

async function refreshPublicMembershipPlans() {
  try {
    const response = await paperpilotApi.getPublicMembershipPlans();
    fetchedPlans.value = Array.isArray(response) ? response : [];
    publicPlansLoaded.value = true;
    selectedPlan.value = normalizePlanId(selectedPlan.value);
    if (!displayPlans.value.some((item) => item.id === selectedPlan.value)) {
      selectedPlan.value = displayPlans.value[0]?.id || "free";
    }
  } catch (error) {
    // Keep the local bootstrap cards only when the public catalog is genuinely unreachable.
    publicPlansLoaded.value = false;
    console.warn("fetch public membership plans failed", error);
  }
}

async function load() {
  loading.value = true;
  try {
    await usageStore.fetchSummary();
    await refreshPublicMembershipPlans();
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
  const monthly = isSeckillActive(plan) ? Number(plan.seckillPrice ?? plan.effectiveMonthlyPrice ?? plan.monthlyPrice ?? 0) : Number(plan.effectiveMonthlyPrice ?? plan.monthlyPrice ?? 0);
  return totalPlanPrice(monthly, plan);
}

function totalPlanPrice(monthly, plan) {
  if (plan?.topUpPack) return Number(monthly || 0).toFixed(2);
  const factor = selectedCycle.value === "quarterly" ? 2.7 : selectedCycle.value === "yearly" ? 9 : 1;
  const isTeam = plan.teamShared;
  const count = isTeam ? Math.max(1, teamMemberCount.value) : 1;
  return (Number(monthly || 0) * factor * count).toFixed(2);
}

function isSeckillActive(plan) {
  nowTick.value;
  if (!plan || !plan.seckillEnabled) return false;
  if (plan.seckillActive === true) return true;
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
  const days = Math.floor(ms / 86400000);
  const hours = Math.floor((ms % 86400000) / 3600000);
  const minutes = Math.floor((ms % 3600000) / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);
  const milliseconds = ms % 1000;
  const dd = String(days).padStart(2, "0");
  const hh = String(hours).padStart(2, "0");
  const mm = String(minutes).padStart(2, "0");
  const ss = String(seconds).padStart(2, "0");
  const msStr = String(milliseconds).padStart(3, "0");
  return `${dd}天 ${hh}时 ${mm}分 ${ss}秒 ${msStr}毫秒`;
}

function quotaPercent(item) {
  const quota = Number(item.quota || 0);
  const used = Number(item.used || 0);
  if (!quota) return 0;
  const remaining = Math.max(0, quota - used);
  return Math.max(0, Math.min(100, (remaining / quota) * 100));
}

function getMeterColor(item) {
  const pct = quotaPercent(item);
  if (pct < 10) {
    return "linear-gradient(90deg, #ef4444, #f87171)";
  } else if (pct < 30) {
    return "linear-gradient(90deg, #f59e0b, #fbbf24)";
  } else {
    return "linear-gradient(90deg, #10b981, #34d399)";
  }
}

function benefitUsageLabel(item) {
  if (item.unlimited) return "不限次";
  if (item.isFeature) return item.included ? (item.value || "包含") : "未包含";
  if (item.key === "agent") {
    return `余 ${item.remaining} 积分`;
  }
  const unitChar = item.key === "translation" || item.key === "immersive" ? "篇" : "次";

  if (item.isDaily) {
    return `日剩 ${item.dailyRemaining} ${unitChar}`;
  } else {
    return `月剩 ${item.remaining} ${unitChar}`;
  }
}

function quotaTone(item) {
  if (item.unlimited) return "quota-unlimited";
  const percent = quotaPercent(item);
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
  if (plan.topUpPack) {
    const amount = (value, unit) => Number(value || 0) > 0 ? `+${Number(value).toLocaleString("zh-CN")} ${unit}` : "未包含";
    return [
      { label: "对照翻译", description: "直接叠加当前可用篇数", value: amount(plan.translateQuota, "篇"), included: Number(plan.translateQuota || 0) > 0 },
      { label: "沉浸全篇翻译", description: "直接叠加当前可用篇数", value: amount(plan.immersiveQuota, "篇"), included: Number(plan.immersiveQuota || 0) > 0 },
      { label: "PPT 生成", description: "直接叠加当前可用次数", value: amount(plan.pptQuota, "次"), included: Number(plan.pptQuota || 0) > 0 },
      { label: "AI 积分（共用）", description: "直接叠加当前可用点数", value: amount(plan.agentTokenQuota, "积分"), included: Number(plan.agentTokenQuota || 0) > 0 },
    ];
  }
  const isTeam = Boolean(plan.teamShared);
  const prefix = isTeam ? "每人" : "";
  const renderedTranslateVal = plan.translateQuotaDaily ? `${prefix}每天 ${plan.translateQuotaDaily} 篇` : "未包含";
  const renderedImmersiveVal = plan.immersiveQuotaDaily ? `${prefix}每天 ${plan.immersiveQuotaDaily} 篇` : "未包含";
  const renderedPptVal = plan.pptQuotaMonthly ? `${prefix}每月 ${plan.pptQuotaMonthly} 次` : "未包含";
  const renderedAgentVal = plan.agentEnabled !== false ? `${prefix}${Number(plan.agentTokenQuota || 0).toLocaleString("zh-CN")} 积分/月` : "未包含";
  const included = (label, description, enabled) => ({ label, description, value: enabled ? "-" : "未包含", included: Boolean(enabled) });
  return [
    included("论文插件导入", "文献一键入库与管理", plan.pluginImportEnabled !== false),
    { label: "对照翻译", description: "双栏对照翻译阅读", value: renderedTranslateVal, included: Number(plan.translateQuotaDaily || 0) > 0 },
    { label: "沉浸全篇翻译", description: "全页版式保留沉浸翻译", value: renderedImmersiveVal, included: Number(plan.immersiveQuotaDaily || 0) > 0 },
    { label: "PPT 生成", description: "PPT 自动生成组会汇报", value: renderedPptVal, included: Number(plan.pptQuotaMonthly || 0) > 0 },
    { label: "AI 积分（共用）", description: "论文解析、对话与综述的通用点数", value: renderedAgentVal, included: plan.agentEnabled !== false },
    included("AI 论文综述", "结构化文献综述生成", plan.reviewEnabled !== false),
    included("研读解析与对话", "针对论文深度问答与推演", plan.chatEnabled !== false),
    included("论坛会员特效与标识", "彩色姓名与专属标识", plan.forumSpecial),
    included("高峰期优先响应", "高并发时段优先通道", plan.peakPriority),
  ];
}

async function selectAndCheckout(planId) {
  selectedPlan.value = normalizePlanId(planId);
  showCheckoutBar.value = true;
  await checkout();
}

function selectPlanForCheckout(planId) {
  selectedPlan.value = normalizePlanId(planId);
  showCheckoutBar.value = true;
  paymentMessage.value = "";
  if (selectedPlanRequiresRedeem.value) payMethod.value = "redeem";
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
  const plan = displayPlans.value.find((item) => item.id === normalizePlanId(order.planId));
  if (plan) return `${plan.name}${plan.topUpPack ? "（加油包）" : ""}`;
  return order.planId === "custom-recharge" ? "历史余额订单" : "会员套餐";
}

function statusLabel(status) {
  return ({ config_required: "待支付配置", pending_payment: "待支付", paid: "已生效", created: "已创建", failed: "支付失败" })[status] || "处理中";
}

async function checkout() {
  if (selectedPlanRequiresRedeem.value) {
    payMethod.value = "redeem";
    paymentMessage.value = "限时 0 元套餐必须使用兑换码兑换，不能创建支付订单。";
    return;
  }
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

async function submitRedeemCode() {
  if (!redeemCode.value) {
    paymentMessage.value = "请输入兑换码。";
    return;
  }
  const codeStr = String(redeemCode.value).trim();
  if (codeStr.length !== 5) {
    paymentMessage.value = "请输入 5 位有效的兑换码。";
    return;
  }
  redeeming.value = true;
  paymentMessage.value = "";
  try {
    const res = await paperpilotApi.redeemPromoCode(codeStr, selectedPlan.value);
    if (res.success) {
      paymentMessage.value = "🎉 兑换成功，会员权益已生效！";
      redeemCode.value = "";
      await usageStore.fetchSummary(); // 刷新本地额度权益
      await loadOrders(); // 刷新本地订单列表
    } else {
      paymentMessage.value = res.message || "兑换失败，请检查兑换码是否正确。";
    }
  } catch (error) {
    console.error("Redeem failed:", error);
    paymentMessage.value = error?.response?.data?.message || "兑换失败，请检查兑换码是否正确。";
  } finally {
    redeeming.value = false;
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
      await usageStore.fetchSummary();
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
  ticket.value = { orderNo: order.orderNo, type: "support", subject: "", detail: "", alipayAccount: "" };
  ticketError.value = "";
  ticketDialog.value?.showModal();
}

async function submitTicket() {
  if (ticket.value.type === "refund" && !ticket.value.alipayAccount) {
    ticketError.value = "请输入您的退款收款支付宝账号。";
    return;
  }
  if (ticket.value.detail.length < 6) {
    ticketError.value = "请把遇到的情况写具体一些。";
    return;
  }
  ticketSubmitting.value = true;
  try {
    const payload = { ...ticket.value };
    if (payload.type === "refund") {
      payload.detail = `【退款收款支付宝账号：${payload.alipayAccount}】\n\n` + payload.detail;
    }
    await paperpilotApi.createPaymentTicket(payload);
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
   QUOTA MANAGEMENT & MEMBERSHIP CENTER — Apple Glassmorphism Aesthetic
   ════════════════════════════════════════════════════════════ */

.membership-page {
  --c-bg: #f8fafc;
  --c-surface: #ffffff;
  --c-surface-glass: rgba(255, 255, 255, 0.85);
  --c-border: #e2e8f0;
  --c-border-subtle: rgba(148, 163, 184, 0.2);
  --c-text: #0f172a;
  --c-muted: #475569;
  --c-subtle: #94a3b8;
  --c-accent: #6366f1;
  --c-accent2: #a855f7;
  --c-accent-glow: rgba(99, 102, 241, 0.2);
  --r: 20px;
  --r-sm: 12px;
  --r-pill: 999px;
  --sh-sm: 0 4px 14px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(15, 23, 42, 0.02);
  --sh-md: 0 12px 32px rgba(15, 23, 42, 0.06), 0 2px 6px rgba(15, 23, 42, 0.03);
  --sh-lg: 0 24px 64px rgba(15, 23, 42, 0.12), 0 4px 16px rgba(15, 23, 42, 0.06);

  position: relative;
  min-height: 100vh;
  background: var(--c-bg);
  color: var(--c-text);
  padding: 32px clamp(20px, 4vw, 56px) 140px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  transition: background 0.3s ease, color 0.3s ease;
  width: 100%;
  box-sizing: border-box;
  margin: 0 auto;
}

:root[data-theme="dark"] .membership-page {
  --c-bg: #09090e;
  --c-surface: #111827;
  --c-surface-glass: rgba(17, 24, 39, 0.85);
  --c-border: rgba(255, 255, 255, 0.08);
  --c-border-subtle: rgba(255, 255, 255, 0.05);
  --c-text: #f8fafc;
  --c-muted: #94a3b8;
  --c-subtle: #64748b;
  --c-accent-glow: rgba(99, 102, 241, 0.35);
  --sh-sm: 0 4px 14px rgba(0, 0, 0, 0.3);
  --sh-md: 0 12px 36px rgba(0, 0, 0, 0.45);
  --sh-lg: 0 24px 64px rgba(0, 0, 0, 0.65);
}

/* ── Ambient Background Atmosphere ─────────────────────────── */
.ambient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
  opacity: 0.28;
  z-index: 1;
}
.orb-1 {
  top: -60px;
  left: 12%;
  width: 440px;
  height: 440px;
  background: radial-gradient(circle, #818cf8, #c084fc);
}
.orb-2 {
  top: 240px;
  right: 6%;
  width: 520px;
  height: 520px;
  background: radial-gradient(circle, #38bdf8, #818cf8);
}
:root[data-theme="dark"] .ambient-orb {
  opacity: 0.18;
}

/* ── Topbar ────────────────────────────────────────────────── */
.membership-topbar {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 32px;
}
@media (max-width: 840px) {
  .membership-topbar { flex-direction: column; align-items: flex-start; }
}

.topbar-badge-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.page-chip {
  padding: 5px 14px;
  border-radius: var(--r-pill);
  background: rgba(99, 102, 241, 0.1);
  color: var(--c-accent);
  font-size: 12px;
  font-weight: 850;
  letter-spacing: 0.4px;
  border: 1px solid rgba(99, 102, 241, 0.22);
  box-shadow: 0 2px 6px rgba(99, 102, 241, 0.08);
}
:root[data-theme="dark"] .page-chip {
  background: rgba(99, 102, 241, 0.18);
  color: #a5b4fc;
  border-color: rgba(99, 102, 241, 0.38);
}

.reset-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 13px;
  border-radius: var(--r-pill);
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  font-size: 12px;
  font-weight: 800;
  border: 1px solid rgba(16, 185, 129, 0.28);
}
:root[data-theme="dark"] .reset-tag {
  background: rgba(16, 185, 129, 0.16);
  border-color: rgba(16, 185, 129, 0.38);
  color: #6ee7b7;
}

.membership-topbar h1 {
  margin: 0 0 8px;
  font-size: clamp(23px, 2.6vw, 32px);
  font-weight: 900;
  color: var(--c-text);
  line-height: 1.25;
  letter-spacing: -0.6px;
}

.membership-topbar p {
  margin: 0;
  font-size: 14.5px;
  color: var(--c-muted);
  max-width: 780px;
  line-height: 1.65;
}

.ghost-button {
  height: 42px;
  padding: 0 20px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-surface-glass);
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: var(--sh-sm);
  backdrop-filter: blur(12px);
  white-space: nowrap;
}
.ghost-button:hover:not(:disabled) {
  border-color: var(--c-accent);
  color: var(--c-accent);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.15);
}
.spinning { display: inline-block; animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }

/* ── Current Strip (Horizontal VIP Banner + Linear Rows) ────── */
.current-strip-linear {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 48px;
}

/* 💳 Horizontal Membership Status Banner (Subtle Glass Bar) */
.horizontal-membership-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  padding: 24px 34px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.9));
  border: 1.5px solid rgba(99, 102, 241, 0.2);
  box-shadow: 0 12px 36px -8px rgba(99, 102, 241, 0.12), 0 4px 12px rgba(0, 0, 0, 0.03);
  backdrop-filter: blur(16px);
  position: relative;
  overflow: hidden;
}
:root[data-theme="dark"] .horizontal-membership-bar {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.88), rgba(15, 23, 42, 0.95));
  border-color: rgba(99, 102, 241, 0.35);
  box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.5);
}
@media (max-width: 960px) {
  .horizontal-membership-bar { flex-direction: column; align-items: flex-start; gap: 20px; padding: 22px; }
  .vip-metrics { width: 100%; justify-content: space-between; }
  .upgrade-vip-btn { width: 100%; text-align: center; }
}

.vip-status-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.vip-badge-glow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border-radius: var(--r-pill);
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.18), rgba(217, 119, 6, 0.14));
  border: 1.5px solid rgba(245, 158, 11, 0.38);
  color: #d97706;
  font-size: 14px;
  font-weight: 900;
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.16);
  letter-spacing: 0.3px;
}
:root[data-theme="dark"] .vip-badge-glow {
  color: #fbbf24;
  border-color: rgba(251, 191, 36, 0.45);
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.22), rgba(245, 158, 11, 0.15));
}

.vip-details {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.vip-name {
  font-size: 18px;
  font-weight: 900;
  color: var(--c-text);
  letter-spacing: -0.3px;
}
.vip-expire {
  font-size: 13px;
  color: var(--c-muted);
  font-weight: 600;
}

.vip-metrics {
  display: flex;
  align-items: center;
  gap: 28px;
}
.metric-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.metric-item small {
  font-size: 11.5px;
  color: var(--c-subtle);
  font-weight: 750;
  letter-spacing: 0.3px;
}
.metric-item strong {
  font-size: 18px;
  font-weight: 900;
  color: var(--c-text);
  font-variant-numeric: tabular-nums;
}
.metric-item strong .unit {
  font-size: 13px;
  font-weight: 750;
  color: var(--c-muted);
}
.status-active { color: #10b981 !important; }

.metric-divider {
  width: 1px;
  height: 32px;
  background: var(--c-border);
}

.upgrade-vip-btn {
  height: 42px;
  padding: 0 24px;
  border-radius: var(--r-pill);
  border: none;
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #ffffff;
  font-size: 14px;
  font-weight: 850;
  cursor: pointer;
  box-shadow: 0 4px 18px rgba(99, 102, 241, 0.35);
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  white-space: nowrap;
}
.upgrade-vip-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.45);
}

/* 📏 Full-Width Linear Rows Panel */
.entitlement-linear-panel {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 22px;
  padding: 28px 34px;
  box-shadow: var(--sh-sm);
  backdrop-filter: blur(16px);
}
@media (max-width: 768px) {
  .entitlement-linear-panel { padding: 20px 16px; }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--c-border);
}
.panel-tag {
  display: block;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--c-accent);
  margin-bottom: 3px;
}
.panel-header h3 {
  margin: 0;
  font-size: 19px;
  font-weight: 900;
  color: var(--c-text);
  letter-spacing: -0.3px;
}

.reset-cycle-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
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
  gap: 6px;
}

.entitlement-row-group {
  border-bottom: 1px solid var(--c-border-subtle);
  transition: all 0.2s ease;
}
.entitlement-row-group:last-child {
  border-bottom: none;
}

.linear-row {
  width: 100%;
  display: grid;
  grid-template-columns: 260px 1fr 160px;
  align-items: center;
  gap: 24px;
  padding: 16px 14px;
  border-radius: 14px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}
.linear-row:disabled {
  cursor: default;
}
.linear-row:hover:not(:disabled),
.linear-row.expanded {
  background: rgba(99, 102, 241, 0.05);
  transform: translateX(4px);
}
:root[data-theme="dark"] .linear-row:hover:not(:disabled),
:root[data-theme="dark"] .linear-row.expanded {
  background: rgba(99, 102, 241, 0.12);
}
@media (max-width: 800px) {
  .linear-row { grid-template-columns: 1fr; gap: 12px; }
  .row-right { text-align: left !important; }
}

.row-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.row-title-block strong {
  display: block;
  font-size: 15px;
  font-weight: 850;
  color: var(--c-text);
  letter-spacing: -0.2px;
}
.row-title-block small {
  font-size: 12px;
  color: var(--c-muted);
  font-weight: 600;
}

.row-center-meter {
  width: 100%;
}

.linear-meter-track {
  height: 10px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.18);
  overflow: hidden;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.08);
}
.linear-meter-track b {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #6366f1, #a855f7);
  transition: width 0.45s ease;
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.45);
}

.linear-unlimited-label {
  font-size: 12.5px;
  color: #10b981;
  font-weight: 850;
}

.linear-feature-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
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
  font-weight: 750;
}

.row-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  text-align: right;
}
.row-stat-text {
  font-size: 15px;
  font-weight: 900;
  color: var(--c-accent);
  font-variant-numeric: tabular-nums;
}
.row-expand-icon {
  font-size: 16px;
  color: var(--c-muted);
}

/* 🎨 Micro-Glow Icon Box */
.homepage-icon-box {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.homepage-icon-box.box-blue {
  background: rgba(37, 99, 235, 0.09);
  border: 1.5px solid rgba(37, 99, 235, 0.3);
  color: #2563eb;
}
:root[data-theme="dark"] .homepage-icon-box.box-blue {
  background: rgba(59, 130, 246, 0.14);
  border-color: rgba(59, 130, 246, 0.45);
  color: #60a5fa;
}
.homepage-icon-box.box-cyan {
  background: rgba(6, 182, 212, 0.09);
  border: 1.5px solid rgba(6, 182, 212, 0.3);
  color: #0891b2;
}
:root[data-theme="dark"] .homepage-icon-box.box-cyan {
  background: rgba(6, 182, 212, 0.14);
  border-color: rgba(6, 182, 212, 0.45);
  color: #22d3ee;
}
.homepage-icon-box.box-purple {
  background: rgba(147, 51, 234, 0.09);
  border: 1.5px solid rgba(147, 51, 234, 0.3);
  color: #9333ea;
}
:root[data-theme="dark"] .homepage-icon-box.box-purple {
  background: rgba(168, 85, 247, 0.14);
  border-color: rgba(168, 85, 247, 0.45);
  color: #c084fc;
}
.homepage-icon-box.box-emerald {
  background: rgba(16, 185, 129, 0.09);
  border: 1.5px solid rgba(16, 185, 129, 0.3);
  color: #10b981;
}
:root[data-theme="dark"] .homepage-icon-box.box-emerald {
  background: rgba(16, 185, 129, 0.14);
  border-color: rgba(16, 185, 129, 0.45);
  color: #34d399;
}
.homepage-icon-box.box-amber {
  background: rgba(245, 158, 11, 0.09);
  border: 1.5px solid rgba(245, 158, 11, 0.3);
  color: #d97706;
}
:root[data-theme="dark"] .homepage-icon-box.box-amber {
  background: rgba(245, 158, 11, 0.14);
  border-color: rgba(245, 158, 11, 0.45);
  color: #fbbf24;
}
.linear-row:hover .homepage-icon-box {
  transform: translateY(-2px) scale(1.06);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

/* ── Usage Detail Panel ────────────────────────────────────── */
.benefit-detail-panel {
  margin: 4px 14px 20px;
  padding: 20px 24px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-sm);
  background: var(--c-bg);
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.02);
}
.benefit-detail-panel header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  color: var(--c-muted);
  font-size: 13px;
}
.benefit-detail-panel header strong {
  color: var(--c-text);
  font-size: 15px;
  font-weight: 850;
}
.benefit-detail-filters {
  display: flex;
  gap: 16px;
  margin: 16px 0;
  flex-wrap: wrap;
}
.benefit-detail-filters label {
  display: flex;
  gap: 8px;
  align-items: center;
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 700;
}
.benefit-detail-filters input {
  border: 1px solid var(--c-border);
  border-radius: 8px;
  padding: 6px 10px;
  background: var(--c-surface);
  color: var(--c-text);
  font-size: 13px;
  outline: none;
}
.benefit-detail-filters input:focus {
  border-color: var(--c-accent);
}

.benefit-detail-table-wrap {
  overflow-x: auto;
}
.benefit-detail-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.benefit-detail-table th,
.benefit-detail-table td {
  padding: 10px 12px;
  border-top: 1px solid var(--c-border);
  text-align: left;
  vertical-align: middle;
}
.benefit-detail-table th {
  color: var(--c-muted);
  font-weight: 800;
  background: rgba(0, 0, 0, 0.015);
}
:root[data-theme="dark"] .benefit-detail-table th {
  background: rgba(255, 255, 255, 0.02);
}
.benefit-detail-table td {
  color: var(--c-text);
}
.benefit-detail-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 14px;
  color: var(--c-muted);
  font-size: 13px;
}
.benefit-detail-pagination button {
  border: 1px solid var(--c-border);
  border-radius: 8px;
  background: var(--c-surface);
  color: var(--c-text);
  padding: 6px 14px;
  font: inherit;
  font-weight: 750;
  cursor: pointer;
  transition: all 0.2s ease;
}
.benefit-detail-pagination button:hover:not(:disabled) {
  border-color: var(--c-accent);
  color: var(--c-accent);
}
.benefit-detail-pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.detail-success { color: #059669 !important; font-weight: 800; }
.detail-failed { color: #dc2626 !important; font-weight: 800; }
.detail-deduct { color: #4f46e5 !important; font-weight: 850; }
.detail-add { color: #10b981 !important; font-weight: 850; }
.detail-muted { color: var(--c-muted) !important; }
.detail-full-blood {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #10b981 !important;
  font-weight: 900 !important;
  background: rgba(16, 185, 129, 0.12);
  padding: 3px 10px;
  border-radius: 6px;
  border: 1px solid rgba(16, 185, 129, 0.28);
  font-size: 11.5px;
}
.restore-scene-tag {
  display: inline-flex;
  align-items: center;
  color: #6366f1 !important;
  font-weight: 850;
  background: rgba(99, 102, 241, 0.1);
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11.5px;
}
.benefit-detail-empty {
  padding: 24px 0;
  color: var(--c-muted);
  font-size: 13px;
  text-align: center;
}

/* ══════════════════════════════════════════════════
   PLAN WORKBENCH — V2 Premium Bento Grid
   ══════════════════════════════════════════════════ */
.plan-workbench {
  position: relative;
  z-index: 2;
  margin-bottom: 64px;
}

.section-chip {
  display: inline-block;
  padding: 5px 14px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.1);
  color: var(--c-accent);
  font-size: 11.5px;
  font-weight: 850;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  border: 1px solid rgba(99, 102, 241, 0.22);
  margin-bottom: 10px;
}

.plan-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 34px;
}
@media (max-width: 800px) {
  .plan-heading { flex-direction: column; align-items: flex-start; }
}

.plan-heading h2 {
  margin: 0 0 6px;
  font-size: clamp(21px, 2.2vw, 28px);
  font-weight: 900;
  color: var(--c-text);
  letter-spacing: -0.4px;
}
.plan-heading > div > p {
  margin: 0;
  font-size: 14.5px;
  color: var(--c-muted);
  max-width: 620px;
  line-height: 1.6;
}

/* Cycle Toggle */
.cycle-toggle-pill {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border-radius: 999px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: var(--sh-sm);
  flex-shrink: 0;
}
.cycle-toggle-pill button {
  height: 36px;
  padding: 0 20px;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: var(--c-muted);
  font-size: 13.5px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
}
.cycle-toggle-pill button.active {
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #fff;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
}

/* Plan Cards Grid V2 */
.plan-cards-v2 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  align-items: stretch;
}
@media (max-width: 1200px) {
  .plan-cards-v2 { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .plan-cards-v2 { grid-template-columns: 1fr; }
}

/* Plan Card V2 Base */
.plan-card-v2 {
  position: relative;
  border-radius: 22px;
  background: var(--c-surface);
  border: 1.5px solid var(--c-border);
  padding: 26px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  cursor: pointer;
  backdrop-filter: blur(16px);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  overflow: hidden;
  box-shadow: var(--sh-sm);
}

.plan-card-v2:hover {
  border-color: rgba(99, 102, 241, 0.45);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12), 0 0 20px rgba(99, 102, 241, 0.1);
  transform: translateY(-5px);
}

.plan-card-v2.active {
  border-color: var(--c-accent) !important;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.3), 0 16px 40px rgba(99, 102, 241, 0.18) !important;
}

.plan-card-v2.featured {
  border-color: rgba(99, 102, 241, 0.45);
  background: radial-gradient(circle at top right, rgba(99, 102, 241, 0.08), transparent 70%), var(--c-surface);
}

/* Top accent line for featured (Plus) */
.card-accent-line {
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 4px;
  background: linear-gradient(90deg, #6366f1, #a855f7);
  border-radius: 22px 22px 0 0;
}

/* Badges */
.card-top-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 22px;
}
.tier-badge {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.3px;
}
.badge-current {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.3);
}
.badge-hot {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.18), rgba(168, 85, 247, 0.18));
  color: #6366f1;
  border: 1px solid rgba(99, 102, 241, 0.3);
}
:root[data-theme="dark"] .badge-hot {
  color: #a5b4fc;
}

/* Name Block */
.plan-name-block h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 900;
  color: var(--c-text);
  letter-spacing: -0.3px;
}
.plan-name-block p {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--c-muted);
  line-height: 1.5;
}

/* Price Row */
.plan-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.price-main {
  font-size: 15px;
  font-weight: 750;
  color: var(--c-muted);
  line-height: 1;
}
.price-main em {
  font-style: normal;
  font-size: 38px;
  font-weight: 950;
  color: var(--c-text);
  letter-spacing: -1.5px;
  font-variant-numeric: tabular-nums;
}
.plan-card-v2.featured .price-main em,
.plan-card-v2.active .price-main em {
  background: linear-gradient(135deg, #6366f1, #a855f7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.price-sub {
  font-size: 13.5px;
  color: var(--c-muted);
  font-weight: 650;
}

/* Luckin Coffee Tag */
.luckin-hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px 5px 8px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.08);
  border: 1px solid rgba(14, 165, 233, 0.25);
  font-size: 12px;
  color: #0284c7;
  font-weight: 850;
  width: fit-content;
}
:root[data-theme="dark"] .luckin-hint {
  background: rgba(56, 189, 248, 0.14);
  border-color: rgba(56, 189, 248, 0.38);
  color: #38bdf8;
}
.luckin-icon-sm {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
}

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
  gap: 2px;
  flex: 1;
}
.plan-feat-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 0;
  border-bottom: 1px solid var(--c-border-subtle);
  font-size: 13px;
}
.plan-feat-list li:last-child {
  border-bottom: none;
}
.plan-feat-list li.feat-off {
  opacity: 0.45;
}

.feat-status-icon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  flex-shrink: 0;
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

/* Plan CTA Button */
.plan-cta-btn {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-text);
  font-size: 14px;
  font-weight: 850;
  cursor: pointer;
  transition: all 0.22s ease;
  margin-top: 4px;
}
.plan-cta-btn:hover {
  border-color: var(--c-accent);
  color: #fff;
  background: var(--c-accent);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.28);
}
.plan-cta-btn.cta-active {
  background: linear-gradient(135deg, #6366f1, #818cf8);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.35);
}
.plan-cta-btn.cta-current {
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.35);
  color: #10b981;
}

/* ── Checkout Bar ────────────────────────────────────────── */
.checkout-bar {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  width: min(1040px, calc(100vw - 32px));
  padding: 18px 28px;
  border-radius: 20px;
  background: var(--c-surface);
  border: 1.5px solid rgba(99, 102, 241, 0.25);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(24px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}
@media (max-width: 768px) {
  .checkout-bar { flex-direction: column; align-items: stretch; }
}

.checkout-bar > div:first-child span {
  font-size: 11px;
  font-weight: 850;
  color: var(--c-subtle);
  text-transform: uppercase;
  letter-spacing: 0.6px;
}
.checkout-bar > div:first-child strong {
  display: block;
  font-size: 16.5px;
  font-weight: 900;
  color: var(--c-text);
  margin: 2px 0;
}
.checkout-bar > div:first-child p {
  margin: 0;
  font-size: 12.5px;
  color: var(--c-muted);
}

.checkout-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}
@media (max-width: 600px) {
  .checkout-actions { flex-direction: column; width: 100%; }
}

.pay-methods {
  display: flex;
  gap: 6px;
}
.pay-methods button {
  height: 40px;
  padding: 0 16px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-muted);
  font-size: 13.5px;
  font-weight: 800;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
}
.pay-methods button.active {
  border-color: var(--c-accent);
  color: var(--c-accent);
  background: rgba(99, 102, 241, 0.1);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);
}

.primary-button {
  height: 44px;
  padding: 0 28px;
  border-radius: var(--r-pill);
  border: none;
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #ffffff;
  font-size: 14.5px;
  font-weight: 850;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.38);
  transition: all 0.25s ease;
  white-space: nowrap;
}
.primary-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.48);
}
.primary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.payment-message {
  margin: 10px 0 0;
  font-size: 13px;
  color: #ef4444;
  font-weight: 750;
  text-align: center;
}
.payment-message.success {
  color: #10b981 !important;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-weight: 850;
}
.success-check-icon {
  font-size: 1.2rem;
}

/* ── Orders Section ──────────────────────────────────────── */
.orders-section {
  position: relative;
  z-index: 2;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 22px;
  padding: 28px 34px;
  box-shadow: var(--sh-sm);
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
}
.section-title span {
  font-size: 11px;
  font-weight: 850;
  color: var(--c-subtle);
  text-transform: uppercase;
  letter-spacing: 0.6px;
}
.section-title h2 {
  margin: 2px 0 0;
  font-size: 19px;
  font-weight: 900;
  color: var(--c-text);
  letter-spacing: -0.3px;
}

.orders-table {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.order-head {
  display: grid;
  grid-template-columns: 1.5fr 1.2fr 1fr 1fr 1fr;
  padding: 8px 16px;
  font-size: 12.5px;
  font-weight: 800;
  color: var(--c-subtle);
  border-bottom: 1px solid var(--c-border);
}
.order-item {
  display: grid;
  grid-template-columns: 1.5fr 1.2fr 1fr 1fr 1fr;
  align-items: center;
  padding: 14px 18px;
  border-radius: 12px;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  font-size: 13.5px;
  color: var(--c-text);
}
.order-item strong { font-weight: 850; }
.order-item small { color: var(--c-muted); display: block; font-size: 11.5px; }

.status {
  display: inline-block;
  padding: 3px 12px;
  border-radius: var(--r-pill);
  font-size: 11.5px;
  font-weight: 850;
  width: fit-content;
}
.status.paid, .status.success { background: rgba(16, 185, 129, 0.12); color: #10b981; }
.status.pending, .status.pending_payment, .status.created, .status.config_required { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
.status.failed, .status.cancelled { background: rgba(239, 68, 68, 0.12); color: #ef4444; }

.ticket-button {
  height: 32px;
  padding: 0 14px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-surface);
  color: var(--c-muted);
  font-size: 12.5px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
  width: fit-content;
}
.ticket-button:hover { border-color: var(--c-accent); color: var(--c-accent); }

.orders-empty {
  padding: 40px;
  text-align: center;
  color: var(--c-subtle);
  font-size: 13.5px;
  background: var(--c-bg);
  border-radius: var(--r-sm);
  border: 1px dashed var(--c-border);
}

/* ── Ticket & Payment Dialogs ────────────────────────────── */
.ticket-dialog {
  border: 1px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-surface);
  color: var(--c-text);
  box-shadow: var(--sh-lg);
  padding: 28px 32px;
  width: min(500px, calc(100vw - 32px));
}
.ticket-dialog::backdrop {
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
}
.dialog-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 22px;
}
.dialog-heading span { font-size: 11px; font-weight: 850; color: var(--c-accent); text-transform: uppercase; }
.dialog-heading h2 { margin: 2px 0 0; font-size: 19px; font-weight: 900; }
.close-button { border: none; background: transparent; color: var(--c-muted); font-size: 22px; cursor: pointer; }

.ticket-dialog label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
  font-size: 13px;
  font-weight: 800;
  color: var(--c-muted);
}
.ticket-dialog input, .ticket-dialog select, .ticket-dialog textarea {
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-text);
  font-size: 13.5px;
  outline: none;
}
.ticket-dialog input:focus, .ticket-dialog select:focus, .ticket-dialog textarea:focus {
  border-color: var(--c-accent);
}
.ticket-error {
  color: #ef4444;
  font-size: 12.5px;
  font-weight: 750;
  margin: 8px 0;
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
  background: rgba(2, 6, 23, 0.65);
  backdrop-filter: blur(10px);
}

.wechat-pay-body {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 24px;
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
.wechat-pay-success-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 180px;
  height: 180px;
  background: rgba(16, 185, 129, 0.1);
  border-radius: 16px;
  color: #10b981;
}
.success-checkmark-svg {
  width: 72px;
  height: 72px;
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
  .wechat-pay-body { grid-template-columns: 1fr; }
  .wechat-qr-shell { margin: 0 auto; }
}
</style>
