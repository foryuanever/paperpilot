<template>
  <div class="admin-page spatial-page reveal-ready" :class="{ 'admin-sidebar-collapsed': adminSidebarCollapsed }">

    <section class="admin-shell" data-reveal>
      <!-- Title & Header -->
      <header class="admin-header">
        <div class="header-left">
          <span class="admin-eyebrow">ADMINISTRATOR CORE</span>
          <h2>系统全局统计与管理</h2>
        </div>
        <div class="header-right" style="display: flex; align-items: center; gap: 12px;">
          <!-- Compact stats summary when cards are collapsed -->
          <div v-if="!showStatsPanel" class="compact-stats-bar">
            <span class="compact-stat-item">
              <span class="compact-icon" v-html="adminIcons.users"></span>
              用户: <strong>{{ globalStats.totalUsers || 0 }}</strong>
            </span>
            <span class="compact-stat-item">
              <span class="compact-icon" v-html="adminIcons.papers"></span>
              文献: <strong>{{ globalStats.totalPapers || 0 }}</strong>
            </span>
            <span class="compact-stat-item">
              <span class="compact-icon" v-html="adminIcons.tokens"></span>
              会员: <strong>{{ activeMemberCount || 0 }}</strong>
            </span>
            <span class="compact-stat-item">
              <span class="compact-icon" v-html="adminIcons.status"></span>
              订单: <strong>¥{{ formatMoney(globalStats.totalRechargeAmount) }}</strong>
            </span>
          </div>
          <button class="spatial-btn spatial-btn-ghost compact-btn toggle-stats-btn" @click="showStatsPanel = !showStatsPanel">
            {{ showStatsPanel ? "隐藏数据卡" : "查看数据卡" }}
          </button>
          <div class="admin-badge">
            <span class="badge-dot"></span>
            管理员控制台
          </div>
        </div>
      </header>

      <!-- Global Stats Cards -->
      <Transition name="slide-fade">
        <div v-if="showStatsPanel" class="admin-stats-grid">
          <div class="admin-stat-card spatial-glass-panel animate-hover-up">
            <div class="stat-icon" v-html="adminIcons.users"></div>
            <div class="stat-info">
              <span class="stat-label">全局注册用户</span>
              <strong class="stat-value">{{ globalStats.totalUsers }} 位</strong>
              <span class="stat-sub">学生 {{ globalStats.studentCount }} / 导师 {{ globalStats.tutorCount }} / 管理员 {{ globalStats.adminCount }}</span>
            </div>
          </div>
          <div class="admin-stat-card spatial-glass-panel animate-hover-up">
            <div class="stat-icon" v-html="adminIcons.papers"></div>
            <div class="stat-info">
              <span class="stat-label">平台文献总量</span>
              <strong class="stat-value">{{ globalStats.totalPapers }} 篇</strong>
              <span class="stat-sub">PDF 文献库沉淀数</span>
            </div>
          </div>
          <div class="admin-stat-card spatial-glass-panel animate-hover-up">
            <div class="stat-icon" v-html="adminIcons.tokens"></div>
            <div class="stat-info">
              <span class="stat-label">开通会员人数</span>
              <strong class="stat-value">{{ activeMemberCount }} 位</strong>
              <span class="stat-sub">当前仍在有效期内的会员</span>
            </div>
          </div>
          <div class="admin-stat-card spatial-glass-panel animate-hover-up">
            <div class="stat-icon" v-html="adminIcons.status"></div>
            <div class="stat-info">
              <span class="stat-label">会员订单总计</span>
              <strong class="stat-value">¥{{ formatMoney(globalStats.totalRechargeAmount) }}</strong>
              <span class="stat-sub">累计 {{ globalStats.rechargeCount || 0 }} 笔 · 用于套餐开通与续费</span>
            </div>
          </div>
        </div>
      </Transition>

      <!-- Main Layout Panels -->
      <div class="admin-main-layout">
        <!-- Sidebar Navigation -->
        <aside
          class="admin-side-nav"
          :class="{ collapsed: adminSidebarCollapsed }"
          aria-label="管理员导航"
        >
          <!-- Floating collapse button -->
          <button
            class="admin-side-toggle"
            type="button"
            :title="adminSidebarCollapsed ? '展开菜单' : '收起菜单'"
            @click="adminSidebarCollapsed = !adminSidebarCollapsed"
          >
            {{ adminSidebarCollapsed ? "›" : "‹" }}
          </button>
          <nav class="admin-side-tabs">
            <button
              v-for="tab in adminTabOptions"
              :key="tab.value"
              class="admin-side-tab"
              :class="{ active: activeTab === tab.value }"
              type="button"
              :title="tab.label"
              @click="activeTab = tab.value"
            >
              <span class="admin-side-icon" v-html="tab.icon"></span>
              <span v-if="!adminSidebarCollapsed" class="admin-side-label">{{ tab.label }}</span>
            </button>
          </nav>
        </aside>
        <!-- Tab Content: Users -->
        <div v-if="activeTab === 'users'" class="tab-pane users-pane">
          <div class="pane-header-row">
            <h3>全局用户目录</h3>
            <button class="spatial-btn spatial-btn-accent compact-btn" @click="showAddUserModal = true">
              添加系统用户
            </button>
          </div>

          <!-- Search & filter toolbar -->
          <div class="search-filter-toolbar spatial-glass-panel animate-hover-up" style="margin-bottom: 20px; display: flex; gap: 16px; padding: 16px; align-items: center; border-radius: 12px;">
            <div style="flex: 1; position: relative; display: flex; align-items: center;">
            <input id="admin-search" name="adminSearch" v-model="searchQuery" placeholder="输入用户名、邮箱、IP 搜索..." style="width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--spatial-line); background: var(--spatial-surface); color: var(--spatial-graphite);" />
            </div>
            <div style="width: 160px;">
              <select v-model="roleFilter" class="admin-select" style="margin-top: 0; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--spatial-line); background: var(--spatial-surface); color: var(--spatial-graphite);">
                <option value="全部">所有角色</option>
                <option value="学生">学生</option>
                <option value="导师">导师</option>
                <option value="管理员">管理员</option>
              </select>
            </div>
          </div>
          <div class="table-container spatial-glass-panel">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>用户名称</th>
                  <th>电子邮箱</th>
                  <th>IP 地址</th>
                  <th>当前角色</th>
                  <th>明文密码</th>
                  <th>会员套餐</th>
                  <th>周期 / 到期</th>
                  <th>权益使用</th>
                  <th>注册时间</th>
                  <th style="text-align: right;">管理操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in paginatedUsers" :key="user.email">
                  <td>
                    <div class="user-name-cell">
                      <span class="user-avatar" :data-user-id="user.id" :data-user-email="user.email" title="查看个人卡片" :style="{ backgroundColor: getAvatarColor(user.role) }">
                        {{ user.username.slice(0, 1).toUpperCase() }}
                      </span>
                      <strong>{{ user.username }}</strong>
                    </div>
                  </td>
                  <td>{{ user.email }}</td>
                  <td>{{ user.ip || '—' }}</td>
                  <td>
                    <span class="role-badge" :class="getRoleClass(user.role)">
                      {{ user.role }}
                    </span>
                  </td>
                  <td>
                    <code class="user-password-code">
                      {{ user.password }}
                    </code>
                  </td>
                  <td style="min-width: 100px;">
                    <span class="membership-plan-pill" :class="membershipPlanClass(user.membershipPlan)">
                      {{ membershipPlanName(user.membershipPlan) }}
                    </span>
                  </td>
                  <td style="min-width: 115px;">
                    <div class="membership-cycle-cell">
                      <strong>{{ membershipCycleName(user.membershipCycle) }}</strong>
                      <small>{{ user.membershipExpiresAt ? `至 ${formatDate(user.membershipExpiresAt)}` : '未开通' }}</small>
                    </div>
                  </td>
                  <td>
                    <div class="membership-usage-cell">
                      <span class="usage-badge review-tag">综述 <strong>{{ user.reviewUsed || 0 }}</strong>/{{ user.reviewQuota || 0 }}</span>
                      <span class="usage-badge ppt-tag">PPT <strong>{{ user.pptUsed || 0 }}</strong>/{{ user.pptQuota || 0 }}</span>
                      <span class="usage-badge chat-tag">对话 <strong>{{ user.chatUsed || 0 }}</strong>/{{ user.chatQuota || 0 }}</span>
                    </div>
                  </td>
                  <td>{{ user.createdTime }}</td>
                  <td style="text-align: right;">
                    <div class="table-actions">
                      <button class="spatial-btn spatial-btn-accent compact-btn" style="min-height: 28px; padding: 0 10px; font-size: 0.75rem;" @click="editUserMembership(user)">分配会员</button>
                      <button class="spatial-btn spatial-btn-ghost compact-btn" style="min-height: 28px; padding: 0 10px; font-size: 0.75rem;" @click="toggleUserRole(user)">切角色</button>
                      <button class="spatial-btn spatial-btn-ghost compact-btn" style="min-height: 28px; padding: 0 10px; font-size: 0.75rem; border-color: rgba(239,68,68,0.2); color: #ef4444; background: rgba(239,68,68,0.02);" @click="deleteUser(user)">移除</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="filteredUsers.length === 0">
                  <td colspan="10" style="text-align: center; color: #64748b; padding: 32px 0;">未搜索到符合条件的用户</td>
                </tr>
              </tbody>
            </table>
          </div>
          <!-- Pagination placed OUTSIDE table container -->
          <div v-if="filteredUsers.length" class="admin-pagination spatial-glass-panel" style="margin-top: 16px; border-top: none; border-radius: 16px;">
            <span>{{ paginationText(filteredUsers.length, userPage, userPageSize) }}</span>
            <div>
              <select v-model.number="userPageSize" class="pagination-size-select">
                <option :value="8">8 条/页</option>
                <option :value="12">12 条/页</option>
                <option :value="20">20 条/页</option>
              </select>
              <button :disabled="userPage <= 1" @click="userPage -= 1">上一页</button>
              <strong>{{ userPage }} / {{ userPageCount }}</strong>
              <button :disabled="userPage >= userPageCount" @click="userPage += 1">下一页</button>
            </div>
          </div>
        </div>

        <!-- Tab Content: Recharges -->
        <div v-if="activeTab === 'recharges'" class="tab-pane">
          <div class="pane-header-row">
            <div>
              <h3>充值订单与售后处理</h3>
              <p class="pane-description">这里处理用户提交的支付工单和退款申请；处理结果会同步回用户充值页。</p>
            </div>
            <button class="spatial-btn spatial-btn-accent compact-btn" @click="showAddRechargeModal = true">手动入账</button>
          </div>

          <div class="payment-admin-grid">
            <section class="payment-work-card spatial-glass-panel">
              <div class="payment-work-head">
                <div>
                  <span>售后工单</span>
                  <strong>{{ paymentTickets.length }} 条</strong>
                </div>
                <button class="action-btn text-btn" @click="fetchAllData">刷新</button>
              </div>
              <article v-for="ticket in paginatedPaymentTickets" :key="ticket.id" class="payment-ticket-admin" :class="`status-${ticket.status}`">
                <header>
                  <div>
                    <span>{{ ticket.type === "refund" ? "退款申请" : "支付工单" }} #{{ ticket.id }}</span>
                    <strong>{{ ticket.subject }}</strong>
                    <small>{{ ticket.email }} · {{ formatDateTime(ticket.createdAt) }}</small>
                  </div>
                  <b>{{ paymentStatusLabel(ticket.status) }}</b>
                </header>
                <p>{{ ticket.detail }}</p>
                <code v-if="ticket.orderNo">{{ ticket.orderNo }}</code>
                <em v-if="ticket.adminNote">处理备注：{{ ticket.adminNote }}</em>
                <div class="payment-ticket-actions">
                  <button class="spatial-btn spatial-btn-ghost compact-btn" @click="openPaymentTicketModal(ticket, 'processed')">标记已处理</button>
                  <button class="spatial-btn spatial-btn-ghost compact-btn danger-lite" @click="openPaymentTicketModal(ticket, 'rejected')">驳回</button>
                </div>
              </article>
              <div v-if="!paymentTickets.length" class="payment-empty">暂无支付工单或退款申请。</div>
              <div v-else class="admin-pagination compact-pagination">
                <span>{{ paginationText(paymentTickets.length, ticketPage, ticketPageSize) }}</span>
                <div>
                  <button :disabled="ticketPage <= 1" @click="ticketPage -= 1">上一页</button>
                  <strong>{{ ticketPage }} / {{ ticketPageCount }}</strong>
                  <button :disabled="ticketPage >= ticketPageCount" @click="ticketPage += 1">下一页</button>
                </div>
              </div>
            </section>

            <section class="payment-work-card spatial-glass-panel">
              <div class="payment-work-head">
                <div>
                  <span>支付订单</span>
                  <strong>{{ paymentOrders.length }} 笔</strong>
                </div>
              </div>
              <article v-for="order in paginatedPaymentOrders" :key="order.orderNo" class="payment-order-admin">
                <div>
                  <strong>¥{{ formatMoney(order.amount) }}</strong>
                  <span>{{ providerLabel(order.provider) }} · {{ paymentStatusLabel(order.status) }}</span>
                  <small>{{ order.email }} · {{ formatDateTime(order.createdAt) }}</small>
                </div>
                <code>{{ order.orderNo }}</code>
              </article>
              <div v-if="!paymentOrders.length" class="payment-empty">暂无用户创建的支付订单。</div>
              <div v-else class="admin-pagination compact-pagination">
                <span>{{ paginationText(paymentOrders.length, orderPage, orderPageSize) }}</span>
                <div>
                  <button :disabled="orderPage <= 1" @click="orderPage -= 1">上一页</button>
                  <strong>{{ orderPage }} / {{ orderPageCount }}</strong>
                  <button :disabled="orderPage >= orderPageCount" @click="orderPage += 1">下一页</button>
                </div>
              </div>
            </section>
          </div>

          <!-- Search toolbar -->
          <div class="search-filter-toolbar spatial-glass-panel animate-hover-up" style="margin-bottom: 20px; display: flex; align-items: center; padding: 16px; border-radius: 12px; position: relative;">
            <input id="admin-recharge-search" name="rechargeSearch" v-model="rechargeQuery" placeholder="按用户邮箱过滤充值记录..." style="width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--spatial-line); background: var(--spatial-surface); color: var(--spatial-graphite);" />
          </div>

          <div class="table-container spatial-glass-panel">
            <table class="admin-table">
              <thead><tr><th>邮箱</th><th>入账金额</th><th>入账方式</th><th>时间</th></tr></thead>
              <tbody>
                <tr v-for="r in paginatedRecharges" :key="r.id"><td>{{ r.email }}</td><td>¥{{ formatMoney(r.amount) }}</td><td>余额充值</td><td>{{ r.time }}</td></tr>
                <tr v-if="filteredRecharges.length === 0">
                  <td colspan="4" style="text-align: center; color: #64748b; padding: 32px 0;">暂无充值发放记录</td>
                </tr>
              </tbody>
            </table>
            <div v-if="filteredRecharges.length" class="admin-pagination">
              <span>{{ paginationText(filteredRecharges.length, rechargePage, rechargePageSize) }}</span>
              <div>
                <select v-model.number="rechargePageSize" class="pagination-size-select">
                  <option :value="8">8 条/页</option>
                  <option :value="12">12 条/页</option>
                  <option :value="20">20 条/页</option>
                </select>
                <button :disabled="rechargePage <= 1" @click="rechargePage -= 1">上一页</button>
                <strong>{{ rechargePage }} / {{ rechargePageCount }}</strong>
                <button :disabled="rechargePage >= rechargePageCount" @click="rechargePage += 1">下一页</button>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab Content: Teams -->
        <div v-if="activeTab === 'teams'" class="tab-pane">
          <div class="pane-header-row">
            <h3>科研团队管理</h3>
            <button class="spatial-btn spatial-btn-accent compact-btn" @click="showAddTeamModal = true">创建团队</button>
          </div>

          <!-- Search toolbar -->
          <div class="search-filter-toolbar spatial-glass-panel animate-hover-up" style="margin-bottom: 20px; display: flex; align-items: center; padding: 16px; border-radius: 12px; position: relative;">
            <input id="admin-team-search" name="teamSearch" v-model="teamQuery" placeholder="输入团队名称或团队标识过滤..." style="width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--spatial-line); background: var(--spatial-surface); color: var(--spatial-graphite);" />
          </div>

          <div class="admin-stats-grid">
            <div v-for="t in filteredTeams" :key="t.id" class="admin-stat-card spatial-glass-panel animate-hover-up" style="flex-direction: column; gap: 12px; align-items: stretch; border-radius: 16px;">
              <div style="display: flex; align-items: center; gap: 12px;">
                <div>
                  <h4 style="margin: 0; font-size: 1.1rem; font-weight: 600; color: var(--spatial-graphite);">{{ t.name }}</h4>
                  <code class="team-identifier">{{ t.identifier }}</code>
                </div>
              </div>
              <div style="display: flex; justify-content: space-between; align-items: center; font-size: 0.85rem; color: var(--spatial-gray); border-top: 1px solid var(--spatial-line); padding-top: 12px; margin-top: 4px;">
                <span>包含成员数: <strong>{{ t.memberCount }} 人</strong></span>
                <div class="table-actions" style="gap: 8px;">
                  <button class="spatial-btn spatial-btn-ghost compact-btn" style="min-height: 28px; padding: 0 10px; font-size: 0.75rem;" @click="viewTeam(t)">详情</button>
                  <button class="spatial-btn spatial-btn-ghost compact-btn" style="min-height: 28px; padding: 0 10px; font-size: 0.75rem; border-color: rgba(239,68,68,0.2); color: #ef4444; background: rgba(239,68,68,0.02);" @click="deleteTeam(t)">删除</button>
                </div>
              </div>
            </div>
            <div v-if="filteredTeams.length === 0" style="grid-column: 1 / -1; text-align: center; color: #64748b; padding: 48px 0;" class="spatial-glass-panel">暂无科研团队</div>
          </div>
        </div>

        <!-- Tab Content: Models (Redesigned 2-Column Layout) -->
        <div v-if="activeTab === 'models'" class="tab-pane models-redesign-pane">
          
          <!-- Header Area -->
          <div class="models-pane-header">
            <div>
              <h3>AI 中转模型配置中心</h3>
              <p class="pane-description">集中管理 API 中转服务商连接参数。可对接下属各模型进行实时测速并勾选分配至对应业务场景。</p>
            </div>
            <button class="spatial-btn spatial-btn-accent" @click="openAllScenesPoolModal">
              全站各模块号池一览
            </button>
          </div>

          <!-- Pool Quick Access Badges -->
          <div class="pools-quick-access spatial-glass-panel">
            <span class="quick-access-title">业务号池状态：</span>
            <div class="quick-access-badges">
              <button
                v-for="scene in modelSceneOptions"
                :key="scene.value"
                class="pool-quick-badge"
                @click="openAllScenesPoolModal"
                title="点击查看号池状态详情"
              >
                <span class="badge-dot"></span>
                <strong>{{ scene.label }}号池</strong>
                <span class="badge-count">{{ getPoolCount(scene.value) }}</span>
              </button>
            </div>
          </div>

          <!-- 2-Column Dashboard Layout -->
          <div class="models-two-col-layout">
            
            <!-- Column 1: 中转站服务商 -->
            <div class="providers-column spatial-glass-panel">
              <header class="column-header">
                <strong>中转站服务商</strong>
                <button class="add-provider-btn" @click="showAddRelayModal = true">
                  + 添加中转站
                </button>
              </header>
              <div v-if="loadingRelays" class="loading-state">
                <span class="loading-spinner"></span> 加载中...
              </div>
              <div v-else-if="relays.length === 0" class="empty-state">
                暂无服务商
              </div>
              <div v-else class="providers-list">
                <div
                  v-for="relay in relays"
                  :key="relay.id"
                  class="provider-item-card"
                  :class="{ active: activeRelay?.id === relay.id }"
                  @click="activeRelay = relay"
                >
                  <div class="provider-card-info">
                    <span class="provider-name">{{ relay.providerName }}</span>
                    <span class="provider-url">{{ relay.baseUrl }}</span>
                  </div>
                  <button class="provider-delete-btn" type="button" @click.stop="deleteRelay(relay)" title="删除">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 13px; height: 13px;"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                  </button>
                </div>
              </div>
            </div>

            <!-- Column 2: Stack of Config and Models -->
            <div class="models-right-stack">
              <!-- Connect Configuration -->
              <div class="config-card-panel spatial-glass-panel">
                <div v-if="!activeRelay" class="empty-state" style="padding: 24px;">
                  请先选择或添加左侧中转站
                </div>
                <div v-else class="provider-config-form">
                  <header class="column-header">
                    <strong>{{ activeRelay.providerName }} - 连接配置</strong>
                  </header>
                  
                  <div class="form-body-horizontal">
                    <div class="form-group-item">
                      <label>Base URL 接口地址</label>
                      <input v-model="activeRelay.baseUrl" placeholder="https://api..." class="spatial-input" />
                    </div>
                    <div class="form-group-item">
                      <label>Key / 凭证密钥</label>
                      <input v-model="activeRelay.apiKey" type="password" placeholder="填写新密钥进行覆盖更新" class="spatial-input" />
                    </div>
                    <button class="spatial-btn spatial-btn-accent save-config-btn" :disabled="updatingRelay" @click="saveRelayConfig">
                      {{ updatingRelay ? "保存中..." : "保存配置" }}
                    </button>
                  </div>
                </div>
              </div>

              <!-- Models List Grid -->
              <div v-if="activeRelay" class="models-grid-panel spatial-glass-panel">
                <div v-if="loadingModels" class="loading-state" style="padding: 40px 0;">
                  <span class="loading-spinner"></span> 正在读取可用模型...
                </div>
                <div v-else-if="relayModels.length === 0" class="empty-state" style="padding: 40px 0;">
                  未读取到模型列表，请确认 Key 是否配置正确且网络通畅。
                </div>
                <div v-else class="models-section">
                  <header class="column-header" style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                    <strong>包含模型 ({{ relayModels.length }})</strong>
                    <button class="spatial-btn spatial-btn-accent compact-btn" @click="testAllModelsSpeed">
                      一键测速
                    </button>
                  </header>
                  
                  <div class="models-cards-grid">
                    <article v-for="model in relayModels" :key="model.id" class="model-dashboard-card">
                      <div class="model-card-header">
                        <strong class="model-name-id">{{ model.id }}</strong>
                        <span class="model-badge-provider">{{ activeRelay.providerName }}</span>
                      </div>
                      
                      <div class="model-speed-row">
                        <button
                          class="model-speed-pill-new"
                          :class="{
                            testing: modelTestResults[model.id]?.testing,
                            success: modelTestResults[model.id] && !modelTestResults[model.id].testing && modelTestResults[model.id].success,
                            error: modelTestResults[model.id] && !modelTestResults[model.id].testing && !modelTestResults[model.id].success
                          }"
                          :disabled="modelTestResults[model.id]?.testing"
                          @click="testModelSpeed(model)"
                        >
                          <span v-if="modelTestResults[model.id]?.testing">测速中...</span>
                          <span v-else-if="modelTestResults[model.id] && !modelTestResults[model.id].testing && modelTestResults[model.id].success">
                            {{ modelTestResults[model.id].latencyMs }}ms
                          </span>
                          <span v-else-if="modelTestResults[model.id] && !modelTestResults[model.id].testing && !modelTestResults[model.id].success">
                            失败
                          </span>
                          <span v-else>点击测速</span>
                        </button>
                      </div>

                      <!-- Speed error message -->
                      <div
                        v-if="modelTestResults[model.id] && !modelTestResults[model.id].success && modelTestResults[model.id].message"
                        class="model-speed-error-msg"
                      >
                        {{ modelTestResults[model.id].message }}
                      </div>

                      <div class="scene-assignments-grid">
                        <span class="assign-label-tag">分配业务号池</span>
                        <div class="checkbox-columns">
                          <label
                            v-for="scene in modelSceneOptions"
                            :key="scene.value"
                            class="scene-checkbox-label"
                            :class="{
                              checked: assignedScenesMap[`${activeRelay.providerName.toLowerCase()}|${activeRelay.baseUrl.toLowerCase()}|${model.id}`]?.[scene.value],
                              disabled: modelActionStates[`${model.id}|${scene.value}`]
                            }"
                          >
                            <input
                              type="checkbox"
                              :checked="assignedScenesMap[`${activeRelay.providerName.toLowerCase()}|${activeRelay.baseUrl.toLowerCase()}|${model.id}`]?.[scene.value]"
                              :disabled="modelActionStates[`${model.id}|${scene.value}`]"
                              @change="toggleModelScene(model, scene.value, assignedScenesMap[`${activeRelay.providerName.toLowerCase()}|${activeRelay.baseUrl.toLowerCase()}|${model.id}`]?.[scene.value])"
                            />
                            <span>{{ scene.label }}</span>
                          </label>
                        </div>
                      </div>
                    </article>
                  </div>
                </div>
              </div>
            </div>

          </div>

          <section class="module-route-table spatial-glass-panel">
            <div class="module-route-head">
              <h4>业务模块模型路由</h4>
              <span>{{ flatScenePoolRows.length }} 条模型连接</span>
            </div>
            <div class="module-route-table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>业务号池</th>
                    <th>所属中转站</th>
                    <th>模型标识</th>
                    <th>延迟测速</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in flatScenePoolRows.slice(0, 8)" :key="`${row.scene}-${row.id}-${row.modelName}`">
                    <td>{{ row.sceneLabel }}</td>
                    <td>{{ row.providerName }}</td>
                    <td><code>{{ row.modelName }}</code></td>
                    <td><strong :class="{ error: row.status !== 'available' }">{{ row.status === "available" ? `${row.latencyMs || 0}ms` : "故障" }}</strong></td>
                    <td><span class="status-tag" :class="row.status">{{ row.status === "available" ? "可用" : "不可用" }}</span></td>
                  </tr>
                  <tr v-if="flatScenePoolRows.length === 0">
                    <td colspan="5" class="empty-row">暂无号池模型，请先在上方模型卡片中勾选业务号池。</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <!-- Tab Content: AI Usage Calls -->
        <div v-if="activeTab === 'aiUsage'" class="tab-pane">
          <AdminAiUsagePanel />
        </div>

        <!-- Tab Content: Logs -->
        <div v-if="activeTab === 'logs'" class="tab-pane logs-pane">
          <div class="pane-header-row">
            <h3>系统运行日志</h3>
            <button class="action-btn text-btn" @click="clearLogs">清空日志</button>
          </div>
          <div class="log-console-container spatial-glass-panel">
            <div class="console-body">
              <div v-for="log in paginatedSystemLogs" :key="log.time" class="log-line">
                <span class="log-time">[{{ log.time }}]</span>
                <span class="log-tag" :class="log.level">{{ log.level.toUpperCase() }}</span>
                <span class="log-msg">{{ log.message }}</span>
              </div>
            </div>
            <div v-if="systemLogs.length" class="admin-pagination log-pagination">
              <span>{{ paginationText(systemLogs.length, logPage, logPageSize) }}</span>
              <div>
                <select v-model.number="logPageSize" class="pagination-size-select">
                  <option :value="20">20 条/页</option>
                  <option :value="40">40 条/页</option>
                  <option :value="80">80 条/页</option>
                </select>
                <button :disabled="logPage <= 1" @click="logPage -= 1">上一页</button>
                <strong>{{ logPage }} / {{ logPageCount }}</strong>
                <button :disabled="logPage >= logPageCount" @click="logPage += 1">下一页</button>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab Content: Site Messages -->
        <div v-if="activeTab === 'messages'" class="tab-pane site-messages-pane">
          <div class="pane-header-row">
            <div>
              <h3>站内消息发布</h3>
              <p class="pane-description">在这里发布系统公告或版本时间线；版本时间线会进入用户端“系统公告”的时间线栏目。</p>
            </div>
          </div>

          <div class="site-message-admin-grid">
            <form class="site-message-form spatial-glass-panel" @submit.prevent="publishSiteMessage">
              <h4>发布新消息</h4>
              <div class="site-message-type-switch">
                <button
                  type="button"
                  :class="{ active: newSiteMessage.messageType === 'notice' }"
                  @click="newSiteMessage.messageType = 'notice'"
                >
                  系统公告
                </button>
                <button
                  type="button"
                  :class="{ active: newSiteMessage.messageType === 'timeline' }"
                  @click="newSiteMessage.messageType = 'timeline'"
                >
                  版本时间线
                </button>
              </div>
              <label>
                <span>消息标题</span>
                <input v-model.trim="newSiteMessage.title" maxlength="120" :placeholder="newSiteMessage.messageType === 'timeline' ? '例如：v1.8.0 模型配置中心更新' : '例如：系统维护通知'" />
              </label>
              <label>
                <span>消息内容</span>
                <textarea v-model.trim="newSiteMessage.content" maxlength="1000" :placeholder="newSiteMessage.messageType === 'timeline' ? '输入本次版本新增、修复、优化内容' : '输入需要全站展示的通知内容'"></textarea>
              </label>
              <div class="site-message-form-footer">
                <span>{{ newSiteMessage.content.length }}/1000</span>
                <button class="spatial-btn spatial-btn-accent compact-btn" :disabled="siteMessagePublishing">
                  {{ siteMessagePublishing ? "发布中..." : "立即发布" }}
                </button>
              </div>
            </form>

            <section class="site-message-list spatial-glass-panel">
              <div class="site-message-list-header">
                <h4>历史消息</h4>
                <span>{{ siteMessages.length }} 条</span>
              </div>
              <article v-for="message in paginatedSiteMessages" :key="message.id" class="site-message-row">
                <div>
                  <div class="site-message-title-row">
                    <strong>{{ message.title }}</strong>
                    <span class="message-type-badge" :class="message.messageType === 'timeline' ? 'timeline' : 'notice'">
                      {{ message.messageType === "timeline" ? "版本时间线" : "系统公告" }}
                    </span>
                    <span :class="message.activeFlag ? 'message-active' : 'message-inactive'">
                      {{ message.activeFlag ? "展示中" : "已撤下" }}
                    </span>
                  </div>
                  <p>{{ message.content }}</p>
                  <small>{{ formatDateTime(message.createdAt) }}</small>
                </div>
                <div class="site-message-actions">
                  <button class="action-btn text-btn" @click="toggleSiteMessage(message)">
                    {{ message.activeFlag ? "撤下" : "重新发布" }}
                  </button>
                  <button class="action-btn text-danger-btn" @click="removeSiteMessage(message)">删除</button>
                </div>
              </article>
              <div v-if="siteMessages.length === 0" class="site-message-empty">暂无站内消息，发布后顶部滚动条才会出现。</div>
              <div v-else class="admin-pagination compact-pagination">
                <span>{{ paginationText(siteMessages.length, siteMessagePage, siteMessagePageSize) }}</span>
                <div>
                  <button :disabled="siteMessagePage <= 1" @click="siteMessagePage -= 1">上一页</button>
                  <strong>{{ siteMessagePage }} / {{ siteMessagePageCount }}</strong>
                  <button :disabled="siteMessagePage >= siteMessagePageCount" @click="siteMessagePage += 1">下一页</button>
                </div>
              </div>
            </section>
          </div>
        </div>

        <!-- Tab Content: Forum Reports -->
        <div v-if="activeTab === 'forumReports'" class="tab-pane forum-reports-pane">
          <div class="pane-header-row">
            <div>
              <h3>论坛举报处理</h3>
              <p class="pane-description">用户在帖子详情页提交的举报会集中到这里。处理后会通知举报人；选择封禁时也会通知帖子作者。</p>
            </div>
            <button class="spatial-btn spatial-btn-ghost compact-btn" @click="fetchAllData">刷新</button>
          </div>

          <div class="table-container spatial-glass-panel" style="margin-top: 16px;">
            <table class="admin-table">
              <thead>
                <tr>
                  <th style="white-space: nowrap;">时间</th>
                  <th style="min-width: 160px;">原帖</th>
                  <th style="white-space: nowrap;">作者</th>
                  <th style="white-space: nowrap;">举报人</th>
                  <th style="min-width: 180px;">举报说明</th>
                  <th style="white-space: nowrap;">状态</th>
                  <th style="text-align: right; min-width: 260px; padding-right: 18px;">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="report in paginatedForumReports" :key="report.id" :class="[`status-${report.status}`, { banned: report.postBanned }]">
                  <td class="ledger-time" style="white-space: nowrap;">{{ formatDateTime(report.createdAt) }}</td>
                  <td>
                    <strong>
                      <a :href="'/forum/post/' + String(report.postId).replace('post-', '')" target="_blank" class="report-post-link" title="在独立标签页中打开原帖">
                        {{ report.postTitle }}
                      </a>
                    </strong>
                    <small style="display: block; font-size: 0.72rem; color: var(--spatial-silver); margin-top: 2px;">类型: {{ report.postType || "论坛帖子" }}</small>
                  </td>
                  <td>
                    <span>{{ report.author || "—" }}</span>
                  </td>
                  <td>
                    <span>{{ report.reporterName || "—" }}</span>
                  </td>
                  <td>
                    <div class="report-detail-text" :title="report.detail" style="max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">{{ report.detail }}</div>
                    <small v-if="report.adminNote" style="display: block; font-size: 0.72rem; color: var(--spatial-silver); margin-top: 2px; font-style: italic;">处理备注: {{ report.adminNote }}</small>
                  </td>
                  <td>
                    <span class="forum-report-status-badge" :class="report.status">{{ forumReportStatusLabel(report.status) }}</span>
                  </td>
                  <td style="text-align: right; padding-right: 18px;">
                    <div class="forum-report-actions" style="justify-content: flex-end; display: flex; gap: 8px;">
                      <button
                        class="spatial-btn spatial-btn-ghost compact-btn"
                        @click="openForumReportDetail(report)"
                      >
                        详情
                      </button>
                      <button
                        class="spatial-btn spatial-btn-ghost compact-btn"
                        :disabled="report.status !== 'open'"
                        @click="openForumReportModal(report, 'processed', false)"
                      >
                        已处理
                      </button>
                      <button
                        class="spatial-btn spatial-btn-ghost compact-btn danger-lite"
                        :disabled="report.postBanned"
                        @click="openForumReportModal(report, 'processed', true)"
                      >
                        封禁
                      </button>
                      <button
                        class="spatial-btn spatial-btn-ghost compact-btn"
                        :disabled="report.status !== 'open'"
                        @click="openForumReportModal(report, 'rejected', false)"
                      >
                        不采纳
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="!forumReports.length">
                  <td colspan="7" class="payment-empty" style="text-align: center; padding: 32px;">暂无帖子举报记录。</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="forumReports.length" class="admin-pagination compact-pagination" style="margin-top: 16px;">
            <span>{{ paginationText(forumReports.length, forumReportPage, forumReportPageSize) }}</span>
            <div>
              <select v-model.number="forumReportPageSize" class="pagination-size-select">
                <option :value="6">6 条/页</option>
                <option :value="10">10 条/页</option>
                <option :value="16">16 条/页</option>
              </select>
              <button :disabled="forumReportPage <= 1" @click="forumReportPage -= 1">上一页</button>
              <strong>{{ forumReportPage }} / {{ forumReportPageCount }}</strong>
              <button :disabled="forumReportPage >= forumReportPageCount" @click="forumReportPage += 1">下一页</button>
            </div>
          </div>
        </div>

        <!-- Tab Content: Campus Verification -->
        <div v-if="activeTab === 'campusVerifications'" class="tab-pane campus-verifications-pane">
          <div class="pane-header-row">
            <div>
              <h3>校园认证审核</h3>
              <p class="pane-description">审核用户提交的学校、学号与学生证照片。通过后用户个人主页显示学校，并收到站内通知。</p>
            </div>
            <button class="spatial-btn spatial-btn-ghost compact-btn" @click="fetchAllData">刷新</button>
          </div>

          <section class="campus-review-list spatial-glass-panel">
            <article
              v-for="request in paginatedCampusVerifications"
              :key="request.id"
              class="campus-review-row"
              :class="`status-${request.status}`"
            >
              <header>
                <div>
                  <span>认证 #{{ request.id }} · {{ formatDateTime(request.createdAt) }}</span>
                  <strong>{{ request.schoolName }}</strong>
                  <small>{{ request.userName || "—" }} · {{ request.email || "—" }} · 学号 {{ request.studentNo }}</small>
                </div>
                <b>{{ request.statusLabel || campusVerificationStatusLabel(request.status) }}</b>
              </header>
              <div class="campus-card-preview-grid">
                <figure>
                  <button type="button" class="campus-image-button" @click="openCampusImage(request.studentCardFront, '学生证正面')">
                    <img :src="request.studentCardFront" alt="学生证正面" />
                  </button>
                  <figcaption>学生证正面</figcaption>
                </figure>
                <figure>
                  <button type="button" class="campus-image-button" @click="openCampusImage(request.studentCardBack, '学生证反面')">
                    <img :src="request.studentCardBack" alt="学生证反面" />
                  </button>
                  <figcaption>学生证反面</figcaption>
                </figure>
              </div>
              <em v-if="request.adminNote">审核备注：{{ request.adminNote }}</em>
              <div class="forum-report-actions">
                <button
                  class="spatial-btn spatial-btn-ghost compact-btn"
                  :disabled="request.status === 'approved'"
                  @click="reviewCampusVerification(request, 'approved')"
                >
                  通过认证
                </button>
                <button
                  class="spatial-btn spatial-btn-ghost compact-btn danger-lite"
                  :disabled="request.status === 'rejected'"
                  @click="reviewCampusVerification(request, 'rejected')"
                >
                  驳回
                </button>
              </div>
            </article>
            <div v-if="!campusVerifications.length" class="payment-empty">暂无校园认证申请。</div>
            <div v-else class="admin-pagination compact-pagination">
              <span>{{ paginationText(campusVerifications.length, campusVerificationPage, campusVerificationPageSize) }}</span>
              <div>
                <select v-model.number="campusVerificationPageSize" class="pagination-size-select">
                  <option :value="4">4 条/页</option>
                  <option :value="8">8 条/页</option>
                  <option :value="12">12 条/页</option>
                </select>
                <button :disabled="campusVerificationPage <= 1" @click="campusVerificationPage -= 1">上一页</button>
                <strong>{{ campusVerificationPage }} / {{ campusVerificationPageCount }}</strong>
                <button :disabled="campusVerificationPage >= campusVerificationPageCount" @click="campusVerificationPage += 1">下一页</button>
              </div>
            </div>
          </section>
        </div>
      </div>
    </section>

    <!-- Membership Modal -->
    <Transition name="fade">
      <div v-if="showMembershipModal" class="admin-modal-overlay" @click="showMembershipModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop>
          <h4>分配会员套餐</h4>
          <p>为 {{ selectedUser?.username }} 开通、续期或取消会员权益。</p>
          <div class="quota-modal-snapshot">
            <div>
              <span>当前套餐</span>
              <strong>{{ membershipPlanName(selectedUser?.membershipPlan) }}</strong>
            </div>
            <div>
              <span>当前权益</span>
              <strong>综述 {{ selectedUser?.reviewUsed || 0 }}/{{ selectedUser?.reviewQuota || 0 }} · PPT {{ selectedUser?.pptUsed || 0 }}/{{ selectedUser?.pptQuota || 0 }}</strong>
            </div>
          </div>
          <div class="form-group" style="margin-top: 16px;">
            <label>会员套餐</label>
            <select v-model="selectedMembershipPlan" class="admin-select">
              <option value="free">未开通</option>
              <option value="light">轻享月卡</option>
              <option value="study">研读会员</option>
              <option value="lab">课题会员</option>
              <option value="team">导师车队会员</option>
              <option value="team_plus">团队 Plus 会员</option>
            </select>
          </div>
          <div v-if="selectedMembershipPlan !== 'free'" class="form-group" style="margin-top: 12px;">
            <label>开通周期</label>
            <select v-model="selectedMembershipCycle" class="admin-select">
              <option value="monthly">月付</option>
              <option value="quarterly">季度</option>
              <option value="yearly">年度</option>
            </select>
          </div>
          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showMembershipModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" @click="saveUserMembership">保存会员</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Add User Modal -->
    <Transition name="fade">
      <div v-if="showAddUserModal" class="admin-modal-overlay" @click="showAddUserModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop>
          <h4>添加系统用户 (Mock)</h4>
          <div class="form-group" style="margin-top: 12px;">
            <label>用户名</label>
            <input id="new-username" name="username" v-model="newUser.username" placeholder="e.g. 李四" />
          </div>
          <div class="form-group" style="margin-top: 12px;">
            <label>邮箱</label>
            <input id="new-email" name="email" v-model="newUser.email" placeholder="you@paperslover.app" />
          </div>
          <div class="form-group" style="margin-top: 12px;">
            <label>角色</label>
            <select id="new-role" name="role" v-model="newUser.role" class="admin-select">
              <option value="学生">学生</option>
              <option value="导师">导师</option>
              <option value="管理员">管理员</option>
            </select>
          </div>
          <div class="form-group" style="margin-top: 12px;">
            <label>初始密码</label>
            <input id="new-password" name="password" v-model="newUser.password" type="text" placeholder="设置初始密码 (e.g. Student2026!)" />
          </div>
          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showAddUserModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" @click="addUser">添加</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Add Recharge Modal -->
    <Transition name="fade">
      <div v-if="showAddRechargeModal" class="admin-modal-overlay" @click="showAddRechargeModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop>
          <h4>手动充值入账</h4>
          <div class="form-group" style="margin-top: 12px;">
            <label>用户邮箱</label>
            <input id="recharge-email" name="recharge-email" v-model="newRecharge.email" placeholder="e.g. student@paperslover.app" />
          </div>
          <div class="form-group" style="margin-top: 12px;">
            <label>充值金额 (¥)</label>
            <input id="recharge-amount" name="recharge-amount" v-model.number="newRecharge.amount" type="number" placeholder="100" />
          </div>
          <p class="form-hint" style="margin-top: 12px;">历史余额入口仅用于兼容旧订单；新用户请通过会员套餐开通权益。</p>
          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showAddRechargeModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" @click="addRecharge">确认入账</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Add Team Modal -->
    <Transition name="fade">
      <div v-if="showAddTeamModal" class="admin-modal-overlay" @click="showAddTeamModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop>
          <h4>新建科研团队</h4>
          <div class="form-group" style="margin-top: 12px;">
            <label>团队名称</label>
            <input id="team-name" name="team-name" v-model="newTeam.name" placeholder="e.g. AI 翻译科研组" />
          </div>
          <div class="form-group" style="margin-top: 12px;">
            <label>团队标识</label>
            <input id="team-identifier" name="team-identifier" v-model="newTeam.identifier" placeholder="e.g. LAB-2026-AI" />
          </div>
          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showAddTeamModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" @click="addTeam">确认创建</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- View Team Details Modal -->
    <Transition name="fade">
      <div v-if="showViewTeamModal" class="admin-modal-overlay" @click="showViewTeamModal = false">
        <div class="admin-modal-card team-detail-modal spatial-glass-panel" @click.stop>
          <h4>团队详情</h4>
          <div class="team-detail-summary">
            <div><span>团队名称</span><strong>{{ selectedTeam?.name }}</strong></div>
            <div><span>团队标识</span><strong>{{ selectedTeam?.identifier }}</strong></div>
            <div><span>成员数量</span><strong>{{ selectedTeamMembers.length }} 人</strong></div>
          </div>
          <div v-if="teamMembersLoading" class="team-members-empty">正在加载团队成员...</div>
          <div v-else class="team-members-table-wrap">
            <table class="team-members-table">
              <thead>
                <tr>
                  <th>成员</th>
                  <th>角色</th>
                  <th>科研等级</th>
                  <th>会员权益</th>
                  <th>活跃时长</th>
                  <th>注册时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="member in selectedTeamMembers" :key="member.id">
                  <td>
                    <strong>{{ member.name }}</strong>
                    <small>{{ member.email }}</small>
                  </td>
                  <td><span class="role-badge" :class="getRoleClass(member.role)">{{ member.role }}</span></td>
                  <td><strong>Lv.{{ member.level }}</strong><small>{{ member.levelTitle }}</small></td>
                  <td>
                    <strong>{{ membershipPlanName(member.membershipPlan) }}</strong>
                    <small>综述 {{ member.reviewUsed || 0 }}/{{ member.reviewQuota || 0 }} · PPT {{ member.pptUsed || 0 }}/{{ member.pptQuota || 0 }}</small>
                  </td>
                  <td>{{ formatActiveTime(member.activeTime) }}</td>
                  <td>{{ formatDate(member.createdAt) }}</td>
                </tr>
                <tr v-if="selectedTeamMembers.length === 0">
                  <td colspan="6" class="team-members-empty">该团队暂无成员</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-accent" @click="showViewTeamModal = false">关闭</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Payment Ticket Modal -->
    <Transition name="fade">
      <div v-if="showPaymentTicketModal" class="admin-modal-overlay" @click="showPaymentTicketModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop>
          <h4>{{ paymentTicketDecision === "processed" ? "处理完成" : "驳回工单" }}</h4>
          <p class="form-hint" style="margin-top: 8px;">{{ selectedPaymentTicket?.subject }} · #{{ selectedPaymentTicket?.id }}</p>
          <div class="form-group" style="margin-top: 14px;">
            <label>处理备注</label>
            <textarea v-model.trim="paymentTicketNote" placeholder="写给用户看的处理结果，例如：已核对订单，稍后人工入账。"></textarea>
          </div>
          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showPaymentTicketModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" :disabled="paymentTicketSaving" @click="submitPaymentTicketDecision">
              {{ paymentTicketSaving ? "保存中..." : "确认处理" }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Forum Report Modal -->
    <Transition name="fade">
      <div v-if="showForumReportModal" class="admin-modal-overlay" @click="showForumReportModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop>
          <h4>{{ forumReportBanPost ? "处理举报并封禁帖子" : forumReportDecision === "rejected" ? "不采纳举报" : "标记举报已处理" }}</h4>
          <p class="form-hint" style="margin-top: 8px;">{{ selectedForumReport?.postTitle }} · 举报 #{{ selectedForumReport?.id }}</p>
          <div class="form-group" style="margin-top: 14px;">
            <label>处理备注</label>
            <textarea v-model.trim="forumReportNote" placeholder="写给用户看的处理说明，例如：已核实违规并处理，或信息不足暂不采纳。"></textarea>
          </div>
          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showForumReportModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" :disabled="forumReportSaving" @click="submitForumReportDecision">
              {{ forumReportSaving ? "保存中..." : "确认处理" }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="fade">
      <div v-if="showForumReportDetailModal" class="admin-modal-overlay" @click="showForumReportDetailModal = false">
        <div class="admin-modal-card forum-report-detail-card spatial-glass-panel" @click.stop>
          <h4>举报详情</h4>
          <p class="form-hint" style="margin-top: 8px;">
            <a :href="'/forum/post/' + String(selectedForumReportDetail?.postId).replace('post-', '')" target="_blank" class="report-post-link" style="font-weight: 700; color: var(--spatial-accent);" title="在独立标签页中打开原帖">
              {{ selectedForumReportDetail?.postTitle }}
            </a>
            · 举报 #{{ selectedForumReportDetail?.id }}
          </p>
          <div class="forum-report-detail-grid">
            <article>
              <span>举报人说明</span>
              <p>{{ selectedForumReportDetail?.detail || "—" }}</p>
            </article>
            <article>
              <span>原帖内容</span>
              <p>{{ selectedForumReportDetail?.postContent || "帖子已删除或无正文" }}</p>
            </article>
          </div>
          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showForumReportDetailModal = false">关闭</button>
            <button
              v-if="selectedForumReportDetail?.status === 'open'"
              class="spatial-btn spatial-btn-accent"
              @click="showForumReportDetailModal = false; openForumReportModal(selectedForumReportDetail, 'processed', true)"
            >
              处理并封禁
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="fade">
      <div v-if="selectedCampusImage" class="admin-modal-overlay campus-image-overlay" @click="selectedCampusImage = null">
        <div class="campus-image-preview" @click.stop>
          <header>
            <strong>{{ selectedCampusImage.title }}</strong>
            <button class="modal-close" @click="selectedCampusImage = null">×</button>
          </header>
          <img :src="selectedCampusImage.src" :alt="selectedCampusImage.title" />
        </div>
      </div>
    </Transition>

    <!-- Add Relay Modal -->
    <Transition name="fade">
      <div v-if="showAddRelayModal" class="admin-modal-overlay" @click="showAddRelayModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop>
          <header class="modal-header">
            <h5>添加中转配置站</h5>
            <button class="modal-close" @click="showAddRelayModal = false">×</button>
          </header>
          <div class="modal-body">
            <div class="form-group">
              <label>中转商户名称</label>
              <input v-model="newRelay.providerName" placeholder="例如: QiHang, Anthropic, SiliconFlow" class="spatial-input" />
            </div>
            <div class="form-group">
              <label>接口代理地址 (Base URL)</label>
              <input v-model="newRelay.baseUrl" placeholder="例如: https://api.qihang.ai/v1" class="spatial-input" />
            </div>
            <div class="form-group">
              <label>API Key / 凭证密钥</label>
              <input v-model="newRelay.apiKey" type="password" placeholder="密钥敏感信息不会泄露，可为空代表使用默认" class="spatial-input" />
            </div>
            <div class="form-group">
              <label>默认主控模型</label>
              <input v-model="newRelay.modelName" placeholder="例如: gpt-4o, claude-3-5-sonnet" class="spatial-input" />
            </div>
          </div>
          <footer class="modal-footer">
            <button class="spatial-btn spatial-btn-ghost" @click="showAddRelayModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" :disabled="submittingNewRelay" @click="submitNewRelay">
              {{ submittingNewRelay ? "保存中..." : "确认添加" }}
            </button>
          </footer>
        </div>
      </div>
    </Transition>

    <!-- Scene Pool Modal -->
    <Transition name="fade">
      <div v-if="activePoolScene" class="admin-modal-overlay" @click="activePoolScene = null">
        <div class="admin-modal-card scene-pool-modal-card spatial-glass-panel" @click.stop>
          <header class="modal-header">
            <div>
              <h5>{{ modelSceneOptions.find(s => s.value === activePoolScene)?.label || activePoolScene }} 号池状态</h5>
              <p class="subtitle">当前在用负载均衡轮询队列</p>
            </div>
            <button class="modal-close" @click="activePoolScene = null">×</button>
          </header>
          <div class="modal-body">
            <div class="pool-modal-actions">
              <button class="spatial-btn spatial-btn-ghost compact-btn" :disabled="loadingScenePool" @click="refreshScenePool">
                {{ loadingScenePool ? "测速检测中..." : "一键测试并刷新" }}
              </button>
              <button class="spatial-btn spatial-btn-ghost compact-btn" :disabled="loadingScenePool" @click="cleanupScenePool">
                清理不可用节点
              </button>
            </div>

            <div v-if="loadingScenePool" class="loading-state">
              <span class="loading-spinner"></span> 数据更新中...
            </div>
            <div v-else-if="scenePoolData.length === 0" class="empty-state">
              号池为空，请在右侧模型列表中勾选对应模型加入
            </div>
            <div v-else class="pool-nodes-list">
              <div
                v-for="route in scenePoolData"
                :key="route.id"
                class="pool-node-item"
                :class="route.status"
              >
                <div class="node-main-info">
                  <div class="node-title">
                    <strong>{{ route.providerName }}</strong>
                    <span class="model-badge">{{ route.modelName }}</span>
                    <span v-if="route.active" class="node-badge active-tag">主路由</span>
                  </div>
                  <span class="node-url">{{ route.baseUrl }}</span>
                  <span v-if="route.message" class="node-message">{{ route.message }}</span>
                </div>
                <div class="node-metrics">
                  <span class="node-latency" :class="{ error: route.status !== 'available' }">
                    {{ route.status === 'available' ? `${route.latencyMs}ms` : '故障' }}
                  </span>
                  <button v-if="!route.active" class="node-remove-btn" type="button" @click="removeScenePoolRoute(route.id)" title="从号池移除">
                    ×
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- All Scene Pools Overview Modal -->
    <Transition name="fade">
      <div v-if="showAllScenesPoolModal" class="admin-modal-overlay" @click="showAllScenesPoolModal = false">
        <div class="admin-modal-card all-pools-modal-card spatial-glass-panel" @click.stop>
          <header class="modal-header">
            <h5>全站各模块号池一览</h5>
            <button class="modal-close" @click="showAllScenesPoolModal = false">×</button>
          </header>
          <div class="modal-body">
            <div class="all-pools-container">
              
              <div v-for="scene in modelSceneOptions" :key="scene.value" class="scene-pool-section">
                <header class="section-title">
                  <span class="bullet-dot"></span>
                  <strong>{{ scene.label }} 号池</strong>
                  <span class="count-tag">已接管 {{ allScenesPoolData[scene.value]?.length || 0 }} 个模型</span>
                </header>
                
                <div class="table-wrapper">
                  <table class="scene-pool-table">
                    <thead>
                      <tr>
                        <th>所属中转站</th>
                        <th>模型标识</th>
                        <th>延迟测速</th>
                        <th>状态</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="!allScenesPoolData[scene.value] || allScenesPoolData[scene.value].length === 0">
                        <td colspan="4" class="empty-row">暂未添加模型到该号池</td>
                      </tr>
                      <tr v-else-if="loadingAllScenesPool" class="empty-row">
                        <td colspan="4"><span class="loading-spinner"></span> 加载数据中...</td>
                      </tr>
                      <tr v-else v-for="route in allScenesPoolData[scene.value]" :key="route.id">
                        <td>{{ route.providerName }}</td>
                        <td><code class="model-code-id">{{ route.modelName }}</code></td>
                        <td>
                          <span class="latency-text" :class="{ error: route.status !== 'available' }">
                            {{ route.status === 'available' ? `${route.latencyMs}ms` : '故障' }}
                          </span>
                        </td>
                        <td>
                          <span class="status-tag" :class="route.status">
                            {{ route.status === 'available' ? '可用' : '不可用' }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>


<script setup>
import { ref, onMounted, onBeforeUnmount, computed, watch } from "vue";
import { useAuthStore } from "../stores/auth";
import { useDialogStore } from "../stores/dialog";
import { paperpilotApi } from "../services/paperpilotApi";
import { useScrollReveal } from "../composables/useScrollReveal";
import ModelConfigPanel from "../components/ModelConfigPanel.vue";
import { useWorkspaceStore } from "../stores/workspace";
import AdminAiUsagePanel from "../components/AdminAiUsagePanel.vue";

const authStore = useAuthStore();
const dialogStore = useDialogStore();
const workspaceStore = useWorkspaceStore();
const activeTab = ref("models");
const adminSidebarCollapsed = ref(false);
const showStatsPanel = ref(false);

// Model Config Redesign (3-Column Layout) States
const showAllScenesPoolModal = ref(false);
const allScenesPoolData = ref({});
const loadingAllScenesPool = ref(false);
const updatingRelay = ref(false);


const adminTabOptions = [
  { value: "users", label: "用户目录与授权", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>` },
  { value: "recharges", label: "充值入账记录", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>` },
  { value: "teams", label: "科研团队管理", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><polyline points="17 11 19 13 23 9"/></svg>` },
  { value: "models", label: "AI 路由与模型", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><rect x="4" y="4" width="16" height="16" rx="2"/><rect x="9" y="9" width="6" height="6"/><path d="M9 1v3M15 1v3M9 20v3M15 20v3M20 9h3M20 15h3M1 9h3M1 15h3"/></svg>` },
  { value: "aiUsage", label: "AI 调用记录", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>` },
  { value: "logs", label: "系统操作日志", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>` },
  { value: "forumReports", label: "论坛举报处理", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>` },
  { value: "campusVerifications", label: "校园认证审核", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c0 2 2 3 6 3s6-1 6-3v-5"/></svg>` },
  { value: "messages", label: "站内消息发布", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>` }
];

// Search and filters
const searchQuery = ref("");
const roleFilter = ref("全部");
const rechargeQuery = ref("");
const teamQuery = ref("");
const userPage = ref(1);
const userPageSize = ref(8);
const ticketPage = ref(1);
const ticketPageSize = ref(4);
const orderPage = ref(1);
const orderPageSize = ref(5);
const rechargePage = ref(1);
const rechargePageSize = ref(8);
const logPage = ref(1);
const logPageSize = ref(20);
const forumReportPage = ref(1);
const forumReportPageSize = ref(6);
const campusVerificationPage = ref(1);
const campusVerificationPageSize = ref(4);
const siteMessagePage = ref(1);
const siteMessagePageSize = ref(5);
const tutorialPage = ref(1);
const tutorialPageSize = ref(5);
const topicAdminQuery = ref("");
const topicAdminLoading = ref(false);
const topicAdminGenerating = ref(false);


// Initialize scroll reveal animations
useScrollReveal(".admin-page");

// Modals
const showMembershipModal = ref(false);
const showAddUserModal = ref(false);
const showAddRechargeModal = ref(false);
const showAddTeamModal = ref(false);
const showViewTeamModal = ref(false);
const showPaymentTicketModal = ref(false);
const showForumReportModal = ref(false);
const showForumReportDetailModal = ref(false);
const siteMessagePublishing = ref(false);
const tutorialSaving = ref(false);
const editingTutorialId = ref(null);

const selectedUser = ref(null);
const selectedMembershipPlan = ref("free");
const selectedMembershipCycle = ref("monthly");
const selectedTeam = ref(null);
const selectedTeamMembers = ref([]);
const teamMembersLoading = ref(false);
const selectedPaymentTicket = ref(null);
const paymentTicketDecision = ref("processed");
const paymentTicketNote = ref("");
const paymentTicketSaving = ref(false);
const selectedForumReport = ref(null);
const selectedForumReportDetail = ref(null);
const selectedCampusImage = ref(null);
const forumReportDecision = ref("processed");
const forumReportBanPost = ref(false);
const forumReportNote = ref("");
const forumReportSaving = ref(false);

const newUser = ref({
  username: "",
  email: "",
  role: "学生",
  password: "",
});

const newRecharge = ref({
  email: "",
  amount: 100,
});

const newTeam = ref({
  name: "",
  identifier: "",
});
const newSiteMessage = ref({ title: "", content: "", messageType: "notice" });
const tutorialForm = ref({
  title: "",
  category: "使用教程",
  sortOrder: 0,
  content: "",
  activeFlag: true,
});

// Reactive Data
const systemUsers = ref([]);
const rechargeRecords = ref([]);
const paymentOrders = ref([]);
const paymentTickets = ref([]);
const teams = ref([]);
const systemLogs = ref([]);
const siteMessages = ref([]);
const tutorials = ref([]);
const forumReports = ref([]);
const campusVerifications = ref([]);
const adminTopics = ref([]);
// Model Config Redesign States
const relays = ref([]);
const loadingRelays = ref(false);
const activeRelay = ref(null);

const relayModels = ref([]);
const loadingModels = ref(false);

const modelTestResults = ref({}); // key: modelId, value: { testing: boolean, latencyMs: number, success: boolean, message: string }
const modelActionStates = ref({}); // key: modelId + '|' + scene, value: boolean
const assignedScenesMap = ref({}); // key: providerName|baseUrl|modelName, value: { [scene]: boolean }

const showAddRelayModal = ref(false);
const submittingNewRelay = ref(false);
const newRelay = ref({
  providerName: "",
  baseUrl: "",
  apiKey: "",
  modelName: "gpt-4o",
});

const activePoolScene = ref(null);
const scenePoolData = ref([]);
const loadingScenePool = ref(false);

const modelScene = ref("paper_review");
const modelSceneOptions = [
  {
    value: "paper_review",
    label: "论文综述",
    hint: "长文本总结、文献综述、报告初稿",
  },
  {
    value: "paper_qa",
    label: "AI论文问答",
    hint: "读者对话、追问、解释公式和方法",
  },
  {
    value: "topic_research",
    label: "调研广场",
    hint: "deep-research、选题卡、代表论文和研究空白",
  },
  {
    value: "meeting_deck",
    label: "PPT生成",
    hint: "必须配置 gpt-5.4 级别强模型",
  },
  {
    value: "forum_moderation",
    label: "AI发帖审核",
    hint: "低价快模型，稳定输出 JSON",
  },
];
const aiUsageSceneOptions = [
  { value: "paper_review", label: "论文综述" },
  { value: "paper_qa", label: "AI论文问答" },
  { value: "meeting_deck", label: "PPT生成" },
  { value: "forum_moderation", label: "AI发帖审核" },
  { value: "topic_research", label: "选题研究" },
  { value: "translate", label: "全文翻译" },
  { value: "summary", label: "论文综述旧记录" },
  { value: "qa", label: "问答旧记录" },
  { value: "analyze", label: "解析旧记录" },
  { value: "report", label: "汇报旧记录" },
];
const globalStats = ref({
  totalUsers: 0,
  studentCount: 0,
  tutorCount: 0,
  adminCount: 0,
  activeMemberCount: 0,
  totalPapers: 0,
  totalTokensUsed: 0,
  totalTokensLimit: 10000000,
  usagePercentage: 0.0,
  totalRechargeAmount: 0,
  totalBalanceAmount: 0,
  totalRechargeTokens: 0,
  rechargeCount: 0,
  averageLatencyMs: 0,
  successRate: 100.0,
  engineStats: {}
});

const adminIcons = {
  users: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 20px; height: 20px; color: #0066ff;"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
  papers: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 20px; height: 20px; color: #0284c7;"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>`,
  tokens: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 20px; height: 20px; color: #a855f7;"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>`,
  status: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 20px; height: 20px; color: #10b981;"><polygon points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>`
};

// Filtered data computeds
const filteredUsers = computed(() => {
  return systemUsers.value.filter(u => {
    const query = searchQuery.value.toLowerCase();
    const matchesSearch = 
      String(u.username || "").toLowerCase().includes(query) ||
      String(u.email || "").toLowerCase().includes(query) ||
      String(u.ip || "").toLowerCase().includes(query);
    const matchesRole = roleFilter.value === "全部" || u.role === roleFilter.value;
    return matchesSearch && matchesRole;
  });
});

const filteredRecharges = computed(() => {
  return rechargeRecords.value.filter(r => {
    return r.email.toLowerCase().includes(rechargeQuery.value.toLowerCase());
  });
});

const filteredTeams = computed(() => {
  const query = teamQuery.value.toLowerCase();
  return teams.value.filter(t => {
    return t.name.toLowerCase().includes(query) || t.identifier.toLowerCase().includes(query);
  });
});

function paginateRows(rows, page, pageSize) {
  const safePage = Math.max(1, Number(page) || 1);
  const safeSize = Math.max(1, Number(pageSize) || 10);
  const start = (safePage - 1) * safeSize;
  return rows.slice(start, start + safeSize);
}

function getPageCount(total, pageSize) {
  return Math.max(1, Math.ceil((Number(total) || 0) / Math.max(1, Number(pageSize) || 10)));
}

function paginationText(total, page, pageSize) {
  if (!total) return "暂无记录";
  const start = (page - 1) * pageSize + 1;
  const end = Math.min(total, page * pageSize);
  return `显示 ${start}-${end} 条，共 ${total} 条`;
}

function keepPageInRange(pageRef, countRef) {
  if (pageRef.value > countRef.value) pageRef.value = countRef.value;
  if (pageRef.value < 1) pageRef.value = 1;
}

const membershipUserCount = computed(() => systemUsers.value.filter(user => (user.membershipPlan || "free") !== "free").length);
const activeMemberCount = computed(() => Number(globalStats.value.activeMemberCount) || membershipUserCount.value);
const totalReviewQuota = computed(() => systemUsers.value.reduce((sum, user) => sum + (Number(user.reviewQuota) || 0), 0));
const totalReviewUsed = computed(() => systemUsers.value.reduce((sum, user) => sum + (Number(user.reviewUsed) || 0), 0));
const totalPptQuota = computed(() => systemUsers.value.reduce((sum, user) => sum + (Number(user.pptQuota) || 0), 0));
const totalPptUsed = computed(() => systemUsers.value.reduce((sum, user) => sum + (Number(user.pptUsed) || 0), 0));
const totalChatQuota = computed(() => systemUsers.value.reduce((sum, user) => sum + (Number(user.chatQuota) || 0), 0));
const totalChatUsed = computed(() => systemUsers.value.reduce((sum, user) => sum + (Number(user.chatUsed) || 0), 0));
const totalBenefitQuota = computed(() => totalReviewQuota.value + totalPptQuota.value + totalChatQuota.value);
const totalBenefitUsed = computed(() => totalReviewUsed.value + totalPptUsed.value + totalChatUsed.value);
const userPageCount = computed(() => getPageCount(filteredUsers.value.length, userPageSize.value));
const ticketPageCount = computed(() => getPageCount(paymentTickets.value.length, ticketPageSize.value));
const orderPageCount = computed(() => getPageCount(paymentOrders.value.length, orderPageSize.value));
const rechargePageCount = computed(() => getPageCount(filteredRecharges.value.length, rechargePageSize.value));
const logPageCount = computed(() => getPageCount(systemLogs.value.length, logPageSize.value));
const forumReportPageCount = computed(() => getPageCount(forumReports.value.length, forumReportPageSize.value));
const campusVerificationPageCount = computed(() => getPageCount(campusVerifications.value.length, campusVerificationPageSize.value));
const siteMessagePageCount = computed(() => getPageCount(siteMessages.value.length, siteMessagePageSize.value));
const tutorialPageCount = computed(() => getPageCount(tutorials.value.length, tutorialPageSize.value));
const paginatedUsers = computed(() => paginateRows(filteredUsers.value, userPage.value, userPageSize.value));
const paginatedPaymentTickets = computed(() => paginateRows(paymentTickets.value, ticketPage.value, ticketPageSize.value));
const paginatedPaymentOrders = computed(() => paginateRows(paymentOrders.value, orderPage.value, orderPageSize.value));
const paginatedRecharges = computed(() => paginateRows(filteredRecharges.value, rechargePage.value, rechargePageSize.value));
const paginatedSystemLogs = computed(() => paginateRows(systemLogs.value, logPage.value, logPageSize.value));
const paginatedForumReports = computed(() => paginateRows(forumReports.value, forumReportPage.value, forumReportPageSize.value));
const paginatedCampusVerifications = computed(() => paginateRows(campusVerifications.value, campusVerificationPage.value, campusVerificationPageSize.value));
const paginatedSiteMessages = computed(() => paginateRows(siteMessages.value, siteMessagePage.value, siteMessagePageSize.value));
const paginatedTutorials = computed(() => paginateRows(tutorials.value, tutorialPage.value, tutorialPageSize.value));
const filteredAdminTopics = computed(() => {
  const query = topicAdminQuery.value.toLowerCase();
  if (!query) return adminTopics.value;
  return adminTopics.value.filter(topic => {
    return [
      topic.title,
      topic.summary,
      topic.source,
      topic.providerLabel,
      ...(topic.tags || []),
      ...(topic.themeClusters || []),
    ].some(value => String(value || "").toLowerCase().includes(query));
  });
});

const flatScenePoolRows = computed(() => {
  return modelSceneOptions.flatMap((scene) => {
    const rows = allScenesPoolData.value[scene.value] || [];
    return rows
      .filter((route) => !route.template)
      .map((route) => ({
        ...route,
        scene: scene.value,
        sceneLabel: scene.label,
      }));
  });
});

watch([searchQuery, roleFilter, userPageSize], () => {
  userPage.value = 1;
});
watch([rechargeQuery, rechargePageSize], () => {
  rechargePage.value = 1;
});
watch([ticketPageSize, orderPageSize, logPageSize, forumReportPageSize, siteMessagePageSize, tutorialPageSize], () => {
  ticketPage.value = 1;
  orderPage.value = 1;
  logPage.value = 1;
  forumReportPage.value = 1;
  siteMessagePage.value = 1;
  tutorialPage.value = 1;
});
watch(userPageCount, () => keepPageInRange(userPage, userPageCount));
watch(ticketPageCount, () => keepPageInRange(ticketPage, ticketPageCount));
watch(orderPageCount, () => keepPageInRange(orderPage, orderPageCount));
watch(rechargePageCount, () => keepPageInRange(rechargePage, rechargePageCount));
watch(logPageCount, () => keepPageInRange(logPage, logPageCount));
watch(forumReportPageCount, () => keepPageInRange(forumReportPage, forumReportPageCount));
watch(siteMessagePageCount, () => keepPageInRange(siteMessagePage, siteMessagePageCount));
watch(tutorialPageCount, () => keepPageInRange(tutorialPage, tutorialPageCount));

const configuredModelRoutes = computed(() => modelPool.value.filter(route => route.keyConfigured).length);
const availableModelRoutes = computed(() => modelPool.value.filter(route => route.status === "available").length);
const unconfiguredModelRoutes = computed(() => modelPool.value.filter(route => !route.keyConfigured || route.status === "unconfigured" || route.template).length);
const modelSceneLabel = computed(() => modelSceneOptions.find(scene => scene.value === modelScene.value)?.label || "通用功能");
const modelSceneDescription = computed(() => ({
  paper_review: "论文综述独立配置，适合长上下文、结构化综述、引用线索整理；建议用性价比强模型。",
  paper_qa: "AI 论文问答独立配置，优先低延迟和低成本，保障用户愿意高频使用。",
  topic_research: "选题调研独立配置，适合 deep-research、主题聚类、代表论文和研究空白整理，可用便宜长上下文模型。",
  meeting_deck: "PPT 生成使用独立强模型配置，必须配置 gpt-5.4 或更强模型；不会影响问答和审核。",
  forum_moderation: "AI 发帖审核独立配置，适合低价快模型，重点是稳定 JSON 输出和审核延迟。",
})[modelScene.value] || "模型入口独立配置，避免高成本任务和轻量任务混用。");
const modelPoolDescription = computed(() => ({
  paper_review: "这里只管理论文综述模型池。建议主路由选稳定强模型，备用路由选便宜模型。",
  paper_qa: "这里只管理 AI 论文问答模型池。遇到限流、超时或上游失败时，会在问答池内尝试备用路由。",
  topic_research: "这里只管理选题调研模型池。生成主题簇、研究空白、代表论文和可行路线时会优先读取这个池子。",
  meeting_deck: "这里只管理 PPT 生成专用池。PPT 多轮 Agent 只会读取这个池子，主模型建议 gpt-5.4。",
  forum_moderation: "这里只管理发帖审核模型池。可以配置低价快模型，不必占用 PPT 强模型额度。",
})[modelScene.value] || "这里只管理当前入口的模型池。");
const visibleModelPool = computed(() => {
  const rows = sortedModelPool.value;
  if (showUnconfiguredPool.value) return rows;
  const configured = rows.filter(route =>
    (route.keyConfigured || route.active || route.status === "available")
    && !["failed", "auth_error", "timeout", "needs_adapter", "unconfigured"].includes(route.status)
  );
  const recommended = rows.filter(route => route.template && !["needs_adapter", "unconfigured"].includes(route.status)).slice(0, 2);
  const merged = new Map();
  [...configured, ...recommended].forEach(route => merged.set(String(route.id), route));
  return Array.from(merged.values()).slice(0, 12);
});
const sortedModelPool = computed(() => [...modelPool.value].sort(compareModelRoutes));

function compareModelRoutes(a, b) {
  const statusRank = {
    available: 0,
    limited: 2,
    unknown: 3,
    timeout: 4,
    unconfigured: 5,
    auth_error: 6,
    failed: 7,
    needs_adapter: 8,
  };
  const scoreA = [
    a.active ? -3 : 0,
    a.keyConfigured ? 0 : 2,
    statusRank[a.status] ?? 9,
    Number.isFinite(Number(a.latencyMs)) ? Number(a.latencyMs) / 1000 : 99,
    a.template ? 3 : 0,
    Number(a.priority) || 99,
  ].reduce((sum, value) => sum + value, 0);
  const scoreB = [
    b.active ? -3 : 0,
    b.keyConfigured ? 0 : 2,
    statusRank[b.status] ?? 9,
    Number.isFinite(Number(b.latencyMs)) ? Number(b.latencyMs) / 1000 : 99,
    b.template ? 3 : 0,
    Number(b.priority) || 99,
  ].reduce((sum, value) => sum + value, 0);
  return scoreA - scoreB;
}

function formatNumber(value) {
  const number = Number(value) || 0;
  return number.toLocaleString("zh-CN");
}

function poolStatusLabel(status) {
  return {
    available: "可用",
    limited: "限流",
    unconfigured: "未配置",
    auth_error: "鉴权失败",
    timeout: "超时",
    failed: "失败",
    needs_adapter: "需适配",
    unknown: "未检测",
  }[status] || "未检测";
}

function poolMessageKey(route) {
  return String(route.id || `${route.providerName}-${route.modelName}`);
}

function isLongPoolMessage(message) {
  return String(message || "").length > 48;
}

function isPoolMessageExpanded(route) {
  return expandedPoolMessages.value.has(poolMessageKey(route));
}

function togglePoolMessage(route) {
  const next = new Set(expandedPoolMessages.value);
  const key = poolMessageKey(route);
  if (next.has(key)) next.delete(key);
  else next.add(key);
  expandedPoolMessages.value = next;
}

// Safe date helpers
function formatDate(val) {
  if (!val) return "—";
  if (typeof val === "string") {
    return val.split("T")[0];
  }
  if (Array.isArray(val)) {
    const y = val[0];
    const m = String(val[1] || 1).padStart(2, '0');
    const d = String(val[2] || 1).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
  return String(val);
}

function formatDateTime(val) {
  if (!val) return "—";
  if (typeof val === "string") {
    return val.replace("T", " ").substring(0, 19);
  }
  if (Array.isArray(val)) {
    const y = val[0];
    const m = String(val[1] || 1).padStart(2, '0');
    const d = String(val[2] || 1).padStart(2, '0');
    const h = String(val[3] || 0).padStart(2, '0');
    const min = String(val[4] || 0).padStart(2, '0');
    const s = String(val[5] || 0).padStart(2, '0');
    return `${y}-${m}-${d} ${h}:${min}:${s}`;
  }
  return String(val);
}

function formatTime(val) {
  if (!val) return "";
  const date = new Date(val);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString("zh-CN", { hour12: false, hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

function routeLatencyLabel(route) {
  const latency = Number(route?.latencyMs);
  if (!Number.isFinite(latency) || latency <= 0) {
    if (!route?.keyConfigured) return "待配置";
    if (route?.status === "timeout") return "超时";
    return "未检测";
  }
  return `${Math.round(latency)} ms`;
}

function routePriorityLabel(route, index) {
  if (route?.active) return "主路由";
  if (route?.status === "available" && index < 3) return "优先";
  if (route?.status === "available") return "备用";
  if (!route?.keyConfigured || route?.template) return "候选";
  return "观察";
}

// Fetch all admin data
async function fetchAllData() {
  try {
    await workspaceStore.hydrateFromBackend(modelScene.value);
    // 1. Fetch Users
    const usersData = await paperpilotApi.getAdminUsers();
    systemUsers.value = usersData.map(u => ({
      id: u.id,
      username: u.username || "—",
      email: u.email,
      ip: u.lastIp || "—",
      role: u.role || "学生",
      password: u.plainPassword || "—",
      tokenLimit: u.tokenLimit || 5000000,
      tokenUsed: u.tokenUsed || 0,
      balanceAmount: u.balanceAmount || 0,
      membershipPlan: u.membershipPlan || "free",
      membershipCycle: u.membershipCycle || "monthly",
      membershipExpiresAt: u.membershipExpiresAt || null,
      reviewQuota: u.reviewQuota || 0,
      reviewUsed: u.reviewUsed || 0,
      pptQuota: u.pptQuota || 0,
      pptUsed: u.pptUsed || 0,
      chatQuota: u.chatQuota || 0,
      chatUsed: u.chatUsed || 0,
      fruitScore: u.fruitScore || 0,
      createdTime: formatDate(u.createdAt),
    }));

    // 2. Fetch Recharge Records
    const rechargesData = await paperpilotApi.getRechargeRecords();
    rechargeRecords.value = rechargesData.map(r => ({
      id: r.id,
      email: r.email,
      amount: r.amount || 0,
      tokens: r.tokens || 0,
      time: formatDateTime(r.createdAt),
    }));

    const paymentsData = await paperpilotApi.getAdminPayments();
    paymentOrders.value = paymentsData.orders || [];
    paymentTickets.value = paymentsData.tickets || [];

    // 3. Fetch Teams
    const teamsData = await paperpilotApi.getTeams();
    teams.value = teamsData.map(t => ({
      id: t.id,
      name: t.name,
      identifier: t.identifier,
      memberCount: t.memberCount || 1,
    }));

    // 4. Fetch Logs
    const logsData = await paperpilotApi.getSystemLogs();
    systemLogs.value = logsData.map(l => ({
      time: l.timestamp ? l.timestamp.replace("T", " ").substring(11, 19) : "—",
      level: l.level || "info",
      message: l.message || "",
    }));

    // 5. Fetch Global Stats
    const statsData = await paperpilotApi.getAdminStats();
    globalStats.value = statsData;

    // 6. Fetch site-wide messages
    siteMessages.value = await paperpilotApi.getAdminSiteMessages();

    // 7. Fetch forum reports
    forumReports.value = await paperpilotApi.getForumReports();

    // 8. Fetch campus verification requests
    campusVerifications.value = await paperpilotApi.getAdminCampusVerifications();

    // 9. Initialize Model Configuration Center
    await loadRelays();
    await loadAllScenePools();

  } catch (error) {
    console.error("Failed to fetch admin data from backend:", error);
    // Fallback message if backend is offline
    logAction("获取系统数据失败，请确认本地后端 Spring Boot 服务器已启动且 MySQL 运行正常！", "error");
  }
}

onMounted(() => {
  fetchAllData();
});

watch(activeTab, async (value) => {
  if (value === "models") {
    await loadRelays();
    await loadAllScenePools();
  }
});

async function loadAdminTopics(showLoading = true) {
  if (showLoading) topicAdminLoading.value = true;
  try {
    adminTopics.value = await paperpilotApi.getAdminTopics({ keyword: topicAdminQuery.value });
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "选题广场列表加载失败");
  } finally {
    topicAdminLoading.value = false;
  }
}

async function generateOfficialHotTopics() {
  if (topicAdminGenerating.value) return;
  topicAdminGenerating.value = true;
  try {
    const generated = await paperpilotApi.generateAdminHotTopics();
    adminTopics.value = [...generated, ...adminTopics.value.filter(topic => !generated.some(item => item.id === topic.id))];
    dialogStore.alert(`已发布 ${generated.length || 0} 个官方热门选题`);
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "官方热门选题生成失败");
  } finally {
    topicAdminGenerating.value = false;
  }
}

async function deleteAdminTopic(topic) {
  if (!topic?.id) return;
  const ok = await dialogStore.confirm(`确认删除选题「${topic.title}」吗？删除后用户端选题广场也会移除。`, {
    title: "删除选题",
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!ok) return;
  try {
    await paperpilotApi.deleteAdminTopic(topic.id);
    adminTopics.value = adminTopics.value.filter(item => item.id !== topic.id);
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "删除选题失败");
  }
}

function topicProviderLabel(topic) {
  if (topic?.providerLabel) return topic.providerLabel;
  const source = String(topic?.source || "");
  return source.includes("官方") || source.includes("daily-frontier") ? "官方" : "匿名用户提供";
}

function topicProviderClass(topic) {
  return topicProviderLabel(topic) === "官方" ? "official" : "anonymous";
}

function shortText(value, max = 80) {
  const text = String(value || "").replace(/\s+/g, " ").trim();
  return text.length > max ? `${text.slice(0, max)}...` : text;
}

// --- Model Config Center Redesign Methods ---
async function loadRelays() {
  loadingRelays.value = true;
  try {
    const pool = await paperpilotApi.getModelPool("general");
    const uniqueRelays = [];
    const seen = new Set();
    for (const item of pool) {
      if (item.template) continue;
      const key = `${item.providerName.toLowerCase()}|${item.baseUrl.toLowerCase()}`;
      if (!seen.has(key)) {
        seen.add(key);
        uniqueRelays.push(item);
      }
    }
    relays.value = uniqueRelays;
    if (uniqueRelays.length > 0 && !activeRelay.value) {
      activeRelay.value = uniqueRelays[0];
    }
  } catch (e) {
    console.error("Failed to load relays:", e);
  } finally {
    loadingRelays.value = false;
  }
}

async function loadAllScenePools() {
  const scenes = ["paper_review", "paper_qa", "topic_research", "meeting_deck", "forum_moderation"];
  const newMap = {};
  const newPoolData = {};
  for (const scene of scenes) {
    try {
      const pool = await paperpilotApi.getModelPool(scene);
      newPoolData[scene] = pool;
      for (const item of pool) {
        if (item.template) continue;
        const routeKey = `${item.providerName.toLowerCase()}|${item.baseUrl.toLowerCase()}`;
        const modelKey = `${routeKey}|${item.modelName}`;
        if (!newMap[modelKey]) {
          newMap[modelKey] = {};
        }
        newMap[modelKey][scene] = true;
      }
    } catch (e) {
      console.error(`Failed to load pool for ${scene}:`, e);
      newPoolData[scene] = [];
    }
  }
  assignedScenesMap.value = newMap;
  allScenesPoolData.value = newPoolData;
}

async function loadRelayModels(relay) {
  if (!relay) {
    relayModels.value = [];
    return;
  }
  loadingModels.value = true;
  relayModels.value = [];
  modelTestResults.value = {};
  try {
    const res = await paperpilotApi.fetchRelayRouteModels(relay.id);
    if (res && res.models) {
      relayModels.value = res.models;
    }
  } catch (e) {
    console.error("Failed to load relay models:", e);
  } finally {
    loadingModels.value = false;
  }
}

async function testModelSpeed(model) {
  if (!activeRelay.value) return;
  modelTestResults.value[model.id] = { testing: true, latencyMs: null, success: false, message: "" };
  try {
    const res = await paperpilotApi.testRelayRouteModel(activeRelay.value.id, model.id);
    modelTestResults.value[model.id] = {
      testing: false,
      latencyMs: res.latencyMs || 0,
      success: res.status === "available" || res.success,
      message: res.message || ""
    };
  } catch (e) {
    modelTestResults.value[model.id] = {
      testing: false,
      latencyMs: null,
      success: false,
      message: e.response?.data?.message || e.message
    };
  }
}

async function toggleModelScene(model, scene, currentEnabled) {
  if (!activeRelay.value) return;
  const stateKey = `${model.id}|${scene}`;
  modelActionStates.value[stateKey] = true;
  const newEnabled = !currentEnabled;
  try {
    await paperpilotApi.assignRelayModelToScene(activeRelay.value.id, model.id, scene, newEnabled);
    
    const routeKey = `${activeRelay.value.providerName.toLowerCase()}|${activeRelay.value.baseUrl.toLowerCase()}`;
    const modelKey = `${routeKey}|${model.id}`;
    if (!assignedScenesMap.value[modelKey]) {
      assignedScenesMap.value[modelKey] = {};
    }
    assignedScenesMap.value[modelKey][scene] = newEnabled;
  } catch (e) {
    dialogStore.alert("更新号池失败: " + (e.response?.data?.message || e.message));
  } finally {
    modelActionStates.value[stateKey] = false;
  }
}

async function deleteRelay(relay) {
  if (!confirm(`确定删除中转站 "${relay.providerName}" 吗？此操作将清除其在所有模块的模型池记录。`)) {
    return;
  }
  try {
    await paperpilotApi.deleteRelayRoute(relay.id);
    if (activeRelay.value?.id === relay.id) {
      activeRelay.value = null;
    }
    await loadRelays();
    await loadAllScenePools();
  } catch (e) {
    dialogStore.alert("删除失败: " + (e.response?.data?.message || e.message));
  }
}

async function submitNewRelay() {
  if (!newRelay.value.providerName || !newRelay.value.baseUrl || !newRelay.value.modelName) {
    dialogStore.alert("请填写完整信息（名称、接口地址、默认模型）");
    return;
  }
  submittingNewRelay.value = true;
  try {
    const payload = {
      providerName: newRelay.value.providerName,
      baseUrl: newRelay.value.baseUrl,
      apiKey: newRelay.value.apiKey || "",
      modelName: newRelay.value.modelName,
      scene: "general",
      apiFormat: "openai_chat",
      authType: "bearer",
      fullUrl: false
    };
    await paperpilotApi.saveModelConfig(payload);
    showAddRelayModal.value = false;
    newRelay.value = { providerName: "", baseUrl: "", apiKey: "", modelName: "gpt-4o" };
    await loadRelays();
  } catch (e) {
    dialogStore.alert("保存失败: " + (e.response?.data?.message || e.message));
  } finally {
    submittingNewRelay.value = false;
  }
}

async function openScenePoolModal(sceneValue) {
  activePoolScene.value = sceneValue;
  await loadScenePoolData();
}

async function loadScenePoolData() {
  if (!activePoolScene.value) return;
  loadingScenePool.value = true;
  try {
    scenePoolData.value = await paperpilotApi.getModelPool(activePoolScene.value);
  } catch (e) {
    console.error("Failed to load scene pool:", e);
  } finally {
    loadingScenePool.value = false;
  }
}

async function refreshScenePool() {
  if (!activePoolScene.value) return;
  loadingScenePool.value = true;
  try {
    scenePoolData.value = await paperpilotApi.refreshModelPool(activePoolScene.value);
  } catch (e) {
    console.error("Failed to refresh scene pool:", e);
  } finally {
    loadingScenePool.value = false;
  }
}

async function cleanupScenePool() {
  if (!activePoolScene.value) return;
  loadingScenePool.value = true;
  try {
    await paperpilotApi.cleanupModelPool(activePoolScene.value);
    await loadScenePoolData();
    await loadAllScenePools();
  } catch (e) {
    console.error("Failed to cleanup scene pool:", e);
  } finally {
    loadingScenePool.value = false;
  }
}

async function removeScenePoolRoute(routeId) {
  if (!confirm("确定将该节点移出此场景的可用列表吗？")) return;
  try {
    await paperpilotApi.assignModelPoolRoute(routeId, activePoolScene.value, false);
    await loadScenePoolData();
    await loadAllScenePools();
  } catch (e) {
    dialogStore.alert("移出失败: " + (e.response?.data?.message || e.message));
  }
}

watch(activeRelay, (newVal) => {
  if (newVal) {
    loadRelayModels(newVal);
  } else {
    relayModels.value = [];
  }
});

async function openAllScenesPoolModal() {
  showAllScenesPoolModal.value = true;
  loadingAllScenesPool.value = true;
  const scenes = ["paper_review", "paper_qa", "topic_research", "meeting_deck", "forum_moderation"];
  const data = {};
  for (const scene of scenes) {
    try {
      data[scene] = await paperpilotApi.getModelPool(scene);
    } catch (e) {
      console.error(`Failed to load pool for ${scene}:`, e);
      data[scene] = [];
    }
  }
  allScenesPoolData.value = data;
  loadingAllScenesPool.value = false;
}

function getPoolCount(scene) {
  return allScenesPoolData.value[scene]?.length || 0;
}

async function testAllModelsSpeed() {
  if (!relayModels.value.length) return;
  // Run all model speed tests in parallel
  relayModels.value.forEach(model => testModelSpeed(model));
}

async function saveRelayConfig() {
  if (!activeRelay.value) return;
  updatingRelay.value = true;
  try {
    const payload = {
      providerName: activeRelay.value.providerName,
      baseUrl: activeRelay.value.baseUrl,
      apiKey: activeRelay.value.apiKey || "",
      modelName: activeRelay.value.modelName || "gpt-4o",
      scene: "general",
      apiFormat: activeRelay.value.apiFormat || "openai_chat",
      authType: activeRelay.value.authType || "bearer",
      fullUrl: activeRelay.value.fullUrl || false
    };
    await paperpilotApi.saveModelConfig(payload);
    dialogStore.alert("中转站配置保存成功！");
    await loadRelays();
  } catch (e) {
    dialogStore.alert("保存失败: " + (e.response?.data?.message || e.message));
  } finally {
    updatingRelay.value = false;
  }
}

function countUsersByRole(role) {
  return systemUsers.value.filter(u => u.role === role).length;
}

function getAvatarColor(role) {
  if (role === "管理员") return "#ff3b30";
  if (role === "导师") return "#a855f7";
  return "#0066ff";
}

function getRoleClass(role) {
  if (role === "管理员") return "role-admin";
  if (role === "导师") return "role-tutor";
  return "role-student";
}

function membershipPlanName(plan) {
  const mapping = {
    free: "未开通",
    lite: "轻享月卡",
    light: "轻享月卡",
    plus: "研读会员",
    study: "研读会员",
    pro: "课题会员",
    lab: "课题会员",
    max: "导师车队会员",
    team: "导师车队会员",
    team_plus: "团队 Plus 会员",
    team_pro: "团队 Pro 会员",
  };
  return mapping[plan] || mapping["free"];
}

function membershipPlanClass(plan) {
  const normalized = plan || "free";
  if (normalized === "lite" || normalized === "light") return "plan-light";
  if (normalized === "plus" || normalized === "study") return "plan-study";
  if (normalized === "pro" || normalized === "lab") return "plan-lab";
  if (normalized === "max" || normalized === "team") return "plan-team";
  if (normalized === "team_plus") return "plan-team_plus";
  if (normalized === "team_pro") return "plan-team_plus";
  return `plan-${normalized}`;
}

function membershipCycleName(cycle) {
  return {
    monthly: "月付",
    quarterly: "季度",
    yearly: "年度",
  }[cycle || "monthly"] || "月付";
}

function formatTokens(n) {
  n = Number(n || 0);
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`;
  if (n >= 1000) return `${Math.round(n / 1000)}K`;
  return String(n);
}

function formatMoney(value) {
  const amount = Number(value || 0);
  if (amount > 0 && amount < 1) return amount.toFixed(3).replace(/0+$/, "").replace(/\.$/, "");
  return amount.toLocaleString("zh-CN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function providerLabel(provider) {
  return provider === "wechat" ? "微信支付" : "支付宝";
}

function paymentStatusLabel(status) {
  return {
    config_required: "待配置",
    pending_payment: "待支付",
    paid: "已支付",
    open: "处理中",
    processed: "已处理",
    rejected: "已驳回",
    closed: "已关闭",
    refunded: "已退款",
  }[status] || "处理中";
}

function forumReportStatusLabel(status) {
  return {
    open: "待处理",
    processed: "已处理",
    rejected: "未采纳",
  }[status] || "待处理";
}

function campusVerificationStatusLabel(status) {
  return {
    pending: "待审核",
    approved: "已通过",
    rejected: "未通过",
  }[status] || "待审核";
}

function openCampusImage(src, title) {
  if (!src) return;
  selectedCampusImage.value = { src, title };
}

function formatActiveTime(seconds) {
  const total = Number(seconds || 0);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  if (hours > 0) return `${hours}小时${minutes}分钟`;
  return `${minutes}分钟`;
}

function editUserMembership(user) {
  selectedUser.value = user;
  selectedMembershipPlan.value = user.membershipPlan || "free";
  selectedMembershipCycle.value = user.membershipCycle || "monthly";
  showMembershipModal.value = true;
}

async function saveUserMembership() {
  if (selectedUser.value) {
    try {
      await paperpilotApi.updateAdminUserMembership(selectedUser.value.id, {
        planId: selectedMembershipPlan.value,
        cycle: selectedMembershipCycle.value,
      });
      showMembershipModal.value = false;
      await fetchAllData();
    } catch (error) {
      console.error("Failed to update user membership:", error);
      dialogStore.alert("更新会员套餐失败");
    }
  }
}

async function toggleUserRole(user) {
  let nextRole = "学生";
  if (user.role === "学生") nextRole = "导师";
  else if (user.role === "导师") nextRole = "管理员";
  else nextRole = "学生";

  try {
    await paperpilotApi.updateUserRole(user.id, nextRole);
    await fetchAllData();
  } catch (error) {
    console.error("Failed to toggle user role:", error);
    dialogStore.alert("更新用户角色失败");
  }
}

async function deleteUser(user) {
  if (await dialogStore.confirm(`确定要删除用户 ${user.username} 吗？`, {
    title: "删除用户",
    confirmText: "删除",
    danger: true,
  })) {
    try {
      await paperpilotApi.deleteUser(user.id);
      await fetchAllData();
    } catch (error) {
      console.error("Failed to delete user:", error);
      dialogStore.alert("删除用户失败");
    }
  }
}

async function addUser() {
  if (!newUser.value.username || !newUser.value.email) {
    dialogStore.alert("用户名和邮箱不能为空");
    return;
  }
  try {
    await paperpilotApi.addAdminUser({
      username: newUser.value.username,
      email: newUser.value.email,
      role: newUser.value.role,
      password: newUser.value.password || "Password2026!",
    });
    showAddUserModal.value = false;
    newUser.value = { username: "", email: "", role: "学生", password: "" };
    await fetchAllData();
  } catch (error) {
    console.error("Failed to add user:", error);
    dialogStore.alert("添加用户失败，可能是邮箱已存在");
  }
}

async function addRecharge() {
  if (!newRecharge.value.email) {
    dialogStore.alert("邮箱不能为空");
    return;
  }
  try {
    await paperpilotApi.addRechargeRecord({
      email: newRecharge.value.email,
      amount: newRecharge.value.amount || 0,
    });
    showAddRechargeModal.value = false;
    newRecharge.value = { email: "", amount: 100 };
    await fetchAllData();
  } catch (error) {
    console.error("Failed to distribute quota:", error);
    dialogStore.alert("充值入账失败，可能是邮箱对应的用户不存在");
  }
}

function openPaymentTicketModal(ticket, status) {
  selectedPaymentTicket.value = ticket;
  paymentTicketDecision.value = status;
  paymentTicketNote.value = ticket.adminNote || (status === "processed" ? "已处理完成，请刷新订单或会员状态。" : "申请信息不足，暂无法处理。");
  showPaymentTicketModal.value = true;
}

async function submitPaymentTicketDecision() {
  if (!selectedPaymentTicket.value) return;
  paymentTicketSaving.value = true;
  try {
    await paperpilotApi.updatePaymentTicket(selectedPaymentTicket.value.id, {
      status: paymentTicketDecision.value,
      adminNote: paymentTicketNote.value,
    });
    showPaymentTicketModal.value = false;
    await fetchAllData();
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "工单处理失败");
  } finally {
    paymentTicketSaving.value = false;
  }
}

function openForumReportModal(report, status, banPost = false) {
  selectedForumReport.value = report;
  forumReportDecision.value = status;
  forumReportBanPost.value = banPost;
  forumReportNote.value = banPost
    ? "已核实举报内容，帖子已封禁。"
    : status === "rejected"
      ? "经核查暂未达到封禁标准，举报不予采纳。"
      : "举报已处理完成。";
  showForumReportModal.value = true;
}

function openForumReportDetail(report) {
  selectedForumReportDetail.value = report;
  showForumReportDetailModal.value = true;
}

async function submitForumReportDecision() {
  if (!selectedForumReport.value) return;
  forumReportSaving.value = true;
  try {
    await paperpilotApi.updateForumReport(selectedForumReport.value.id, {
      status: forumReportDecision.value,
      adminNote: forumReportNote.value,
      banPost: forumReportBanPost.value,
    });
    showForumReportModal.value = false;
    await fetchAllData();
    window.dispatchEvent(new Event("paperpilot:forum-posts-changed"));
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "举报处理失败");
  } finally {
    forumReportSaving.value = false;
  }
}

async function reviewCampusVerification(request, status) {
  const isApprove = status === "approved";
  let adminNote = isApprove ? "校园认证信息已核验通过。" : await dialogStore.prompt("请输入驳回原因", {
    title: "驳回校园认证",
    confirmText: "继续驳回",
    cancelText: "取消",
    danger: true,
    defaultValue: request.adminNote || "学生证信息不清晰，请重新上传。",
    placeholder: "写清楚需要用户补充或重新上传的原因",
  });
  if (!isApprove && adminNote === null) return;
  if (!await dialogStore.confirm(`确定${isApprove ? "通过" : "驳回"} ${request.userName || request.email} 的校园认证吗？`, {
    title: "校园认证审核",
    confirmText: isApprove ? "通过" : "驳回",
    danger: !isApprove,
  })) return;
  try {
    await paperpilotApi.reviewCampusVerification(request.id, {
      status,
      adminNote: adminNote || "",
    });
    campusVerifications.value = await paperpilotApi.getAdminCampusVerifications();
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "校园认证审核失败");
  }
}

function cleanActionName(value) {
  const text = String(value || "-");
  if (text.includes("PPT") || text.includes("Agent")) return "组会PPT Agent执行";
  if (text.includes("综述") || text.includes("汇报") || text.includes("组会")) return "论文综述生成";
  return "AI文章对话";
}

async function addTeam() {
  if (!newTeam.value.name || !newTeam.value.identifier) {
    dialogStore.alert("团队名称和团队标识不能为空");
    return;
  }
  try {
    await paperpilotApi.createTeam({
      name: newTeam.value.name,
      identifier: newTeam.value.identifier,
    });
    showAddTeamModal.value = false;
    newTeam.value = { name: "", identifier: "" };
    await fetchAllData();
  } catch (error) {
    console.error("Failed to create team:", error);
    dialogStore.alert("创建团队失败，可能是团队名称已存在");
  }
}

async function viewTeam(team) {
  selectedTeam.value = team;
  selectedTeamMembers.value = [];
  showViewTeamModal.value = true;
  teamMembersLoading.value = true;
  try {
    selectedTeamMembers.value = await paperpilotApi.getTeamMembersById(team.id);
  } catch (error) {
    console.error("Failed to load team members:", error);
    dialogStore.alert("团队成员加载失败");
  } finally {
    teamMembersLoading.value = false;
  }
}

async function deleteTeam(team) {
  if (await dialogStore.confirm(`确定要解散团队 ${team.name} 吗？`, {
    title: "解散团队",
    confirmText: "解散",
    danger: true,
  })) {
    try {
      await paperpilotApi.deleteTeam(team.id);
      await fetchAllData();
    } catch (error) {
      console.error("Failed to delete team:", error);
      dialogStore.alert("删除团队失败");
    }
  }
}

function logAction(msg, level = "info") {
  const now = new Date();
  const timeStr = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`;
  systemLogs.value.unshift({
    time: timeStr,
    level,
    message: msg
  });
}

async function clearLogs() {
  try {
    await paperpilotApi.clearSystemLogs();
    await fetchAllData();
  } catch (error) {
    console.error("Failed to clear logs:", error);
    dialogStore.alert("清空日志失败");
  }
}

async function publishSiteMessage() {
  if (!newSiteMessage.value.title || !newSiteMessage.value.content) {
    dialogStore.alert("请填写消息标题和内容");
    return;
  }
  siteMessagePublishing.value = true;
  try {
    await paperpilotApi.publishSiteMessage(newSiteMessage.value);
    newSiteMessage.value = { title: "", content: "", messageType: "notice" };
    siteMessages.value = await paperpilotApi.getAdminSiteMessages();
    window.dispatchEvent(new Event("paperpilot:site-messages-changed"));
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "站内消息发布失败");
  } finally {
    siteMessagePublishing.value = false;
  }
}

async function toggleSiteMessage(message) {
  try {
    await paperpilotApi.updateSiteMessageStatus(message.id, !message.activeFlag);
    siteMessages.value = await paperpilotApi.getAdminSiteMessages();
    window.dispatchEvent(new Event("paperpilot:site-messages-changed"));
  } catch (error) {
    dialogStore.alert("消息状态更新失败");
  }
}

async function removeSiteMessage(message) {
  if (!await dialogStore.confirm(`确定删除站内消息“${message.title}”吗？`, {
    title: "删除站内消息",
    confirmText: "删除",
    danger: true,
  })) return;
  try {
    await paperpilotApi.deleteSiteMessage(message.id);
    siteMessages.value = await paperpilotApi.getAdminSiteMessages();
    window.dispatchEvent(new Event("paperpilot:site-messages-changed"));
  } catch (error) {
    dialogStore.alert("站内消息删除失败");
  }
}

function resetTutorialForm() {
  editingTutorialId.value = null;
  tutorialForm.value = {
    title: "",
    category: "使用教程",
    sortOrder: 0,
    content: "",
    activeFlag: true,
  };
}

function editTutorial(article) {
  editingTutorialId.value = article.id;
  tutorialForm.value = {
    title: article.title || "",
    category: article.category || "使用教程",
    sortOrder: article.sortOrder || 0,
    content: article.content || "",
    activeFlag: article.activeFlag !== false,
  };
  activeTab.value = "tutorials";
}

async function saveTutorial() {
  if (!tutorialForm.value.title || !tutorialForm.value.content.trim()) {
    dialogStore.alert("请填写教程标题和 Markdown 内容");
    return;
  }
  tutorialSaving.value = true;
  try {
    if (editingTutorialId.value) {
      await paperpilotApi.updateTutorial(editingTutorialId.value, tutorialForm.value);
    } else {
      await paperpilotApi.publishTutorial(tutorialForm.value);
    }
    tutorials.value = await paperpilotApi.getAdminTutorials();
    resetTutorialForm();
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "教程保存失败");
  } finally {
    tutorialSaving.value = false;
  }
}

async function toggleTutorial(article) {
  try {
    await paperpilotApi.updateTutorialStatus(article.id, !article.activeFlag);
    tutorials.value = await paperpilotApi.getAdminTutorials();
  } catch (error) {
    dialogStore.alert("教程状态更新失败");
  }
}

async function removeTutorial(article) {
  if (!await dialogStore.confirm(`确定删除教程“${article.title}”吗？`, {
    title: "删除教程",
    confirmText: "删除",
    danger: true,
  })) return;
  try {
    await paperpilotApi.deleteTutorial(article.id);
    tutorials.value = await paperpilotApi.getAdminTutorials();
    if (editingTutorialId.value === article.id) resetTutorialForm();
  } catch (error) {
    dialogStore.alert("教程删除失败");
  }
}

function truncateText(value, length = 100) {
  const text = String(value || "").replace(/\s+/g, " ").trim();
  return text.length > length ? `${text.slice(0, length)}...` : text;
}

function formatTokenCount(num) {
  if (!num && num !== 0) return "0";
  const n = Number(num);
  if (n >= 1000000) {
    return (n / 1000000).toFixed(1) + "M";
  }
  if (n >= 1000) {
    return (n / 1000).toFixed(0) + "K";
  }
  return n.toString();
}
</script>


<style scoped>
.admin-page {
  padding: 32px 36px 48px 36px;
  position: relative;
  min-height: 100vh;
}

.admin-main-layout {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 28px;
  align-items: start;
  margin-top: 32px;
}

.admin-side-nav {
  position: sticky;
  top: 24px;
  z-index: 30;
  width: 236px;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  overflow: visible;
  border: 1px solid var(--spatial-line);
  border-radius: 24px;
  background: var(--spatial-glass);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(22px);
  user-select: none;
  transition: width 0.25s ease, border-radius 0.25s ease, background 0.25s ease, border-color 0.25s ease;
}

.admin-side-nav.collapsed {
  width: 68px;
  border-radius: 22px;
}

.admin-side-tabs {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  padding: 14px 12px;
  overflow-y: auto;
}

.admin-side-tab {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 46px;
  width: 100%;
  border: 0;
  border-radius: 15px;
  background: transparent;
  color: var(--spatial-gray);
  cursor: pointer;
  font-weight: 700;
  text-align: left;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.admin-side-tab:hover {
  color: var(--spatial-graphite);
  background: var(--spatial-accent-soft);
  transform: translateX(2px);
}

.admin-side-tab.active {
  color: var(--spatial-accent);
  background: var(--spatial-accent-soft);
}

.admin-side-icon {
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  background: var(--spatial-warm-2);
  color: inherit;
  font-size: 0.85rem;
  font-weight: 800;
}

.admin-side-tab.active .admin-side-icon {
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
}

.admin-side-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-side-nav.collapsed .admin-side-tab {
  justify-content: center;
  padding-inline: 0;
}

.admin-side-nav.collapsed .admin-side-icon {
  width: 34px;
  height: 34px;
}

.admin-shell {
  max-width: min(1560px, 100%);
  margin: 0 auto;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;
}

.admin-eyebrow {
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #0066ff;
  text-transform: uppercase;
}

.admin-header h2 {
  font-family: 'Outfit', sans-serif;
  font-size: 2.2rem;
  font-weight: 600;
  letter-spacing: -0.03em;
  color: #0f172a;
  margin-top: 4px;
}

.admin-badge {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(0, 102, 255, 0.08);
  border: 1px solid rgba(0, 102, 255, 0.15);
  border-radius: 99px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #0066ff;
  white-space: nowrap;
}

.badge-dot {
  width: 8px;
  height: 8px;
  background: #0066ff;
  border-radius: 50%;
  box-shadow: 0 0 8px #0066ff;
}

/* Stats Grid */
.admin-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.admin-stat-card {
  padding: 24px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(10, 10, 12, 0.02);
}

.stat-icon {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 0.85rem;
  color: #64748b;
  font-weight: 500;
}

.stat-value {
  font-size: 1.6rem;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
  margin: 4px 0 2px;
}

.stat-sub {
  font-size: 0.75rem;
  color: #64748b;
}

/* Main Layout Tabs (Segmented Capsule Control) */
.admin-tabs-nav {
  display: flex;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  background: rgba(0, 0, 0, 0.04);
  padding: 6px;
  border-radius: 99px;
  border: 1px solid rgba(0,0,0,0.02);
  gap: 4px;
  margin-bottom: 32px;
  align-self: flex-start;
  box-shadow: inset 0 2px 8px rgba(0,0,0,0.02);
  backdrop-filter: blur(12px);
  width: fit-content;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.admin-tabs-nav::-webkit-scrollbar {
  display: none;
}

.tab-btn {
  flex: 0 0 auto;
  border: none;
  background: transparent;
  padding: 10px 24px;
  border-radius: 99px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
}

.tab-btn:hover {
  color: #0066ff;
}

.tab-btn.active {
  background: #ffffff;
  color: #0066ff;
  box-shadow: 0 4px 14px rgba(0, 102, 255, 0.08), 0 2px 4px rgba(0, 102, 255, 0.03);
}

/* Tab Pane Common */
.tab-pane {
  animation: fadeIn 0.35s ease-out;
}

.pane-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.pane-header-row h3 {
  font-family: 'Outfit', sans-serif;
  font-size: 1.35rem;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.billing-rule-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.billing-rule-card {
  display: grid;
  gap: 8px;
  padding: 20px;
  border-radius: 14px;
}

.billing-rule-card.wide {
  grid-column: span 2;
}

.billing-rule-card span {
  color: #64748b;
  font-size: .82rem;
  font-weight: 700;
}

.billing-rule-card strong {
  color: #0f172a;
  font-size: 1.2rem;
  line-height: 1.35;
}

.billing-rule-card input {
  width: 100%;
  height: 40px;
  border: 1px solid rgba(15, 23, 42, .1);
  border-radius: 10px;
  background: #fff;
  color: #0f172a;
  padding: 0 12px;
  font: inherit;
  font-weight: 700;
}

.billing-save-btn {
  justify-self: start;
  margin-top: 4px;
}

.billing-rule-card p,
.form-hint {
  margin: 0;
  color: #64748b;
  font-size: .82rem;
  line-height: 1.6;
}

.payment-admin-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(320px, .88fr);
  gap: 18px;
  margin-bottom: 20px;
}

.payment-work-card {
  display: grid;
  align-content: start;
  gap: 12px;
  min-height: 260px;
  padding: 18px;
  border-radius: 16px;
}

.payment-work-head,
.payment-ticket-admin header,
.payment-order-admin {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.payment-work-head span,
.payment-ticket-admin span,
.payment-order-admin span {
  color: #64748b;
  font-size: .78rem;
  font-weight: 700;
}

.payment-work-head strong {
  display: block;
  margin-top: 3px;
  color: #0f172a;
  font-size: 1.15rem;
}

.payment-ticket-admin,
.payment-order-admin {
  border: 1px solid rgba(15, 23, 42, .08);
  border-radius: 14px;
  background: rgba(255, 255, 255, .72);
  padding: 14px;
}

.payment-ticket-admin strong,
.payment-order-admin strong {
  display: block;
  color: #0f172a;
  font-size: .98rem;
  line-height: 1.35;
}

.payment-ticket-admin small,
.payment-order-admin small,
.payment-ticket-admin em {
  display: block;
  margin-top: 5px;
  color: #64748b;
  font-size: .76rem;
  font-style: normal;
  line-height: 1.5;
}

.payment-ticket-admin p {
  margin: 10px 0;
  color: #334155;
  font-size: .86rem;
  line-height: 1.65;
}

.payment-ticket-admin code,
.payment-order-admin code {
  display: inline-block;
  max-width: 100%;
  overflow-wrap: anywhere;
  border-radius: 8px;
  background: rgba(0, 102, 255, .06);
  color: #2550b8;
  padding: 5px 8px;
  font-size: .75rem;
}

.payment-ticket-admin header b {
  flex: 0 0 auto;
  border-radius: 999px;
  background: #eef4ff;
  color: #1d4ed8;
  padding: 5px 9px;
  font-size: .75rem;
}

/* Quota badges styling */
.membership-usage-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.usage-badge {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.72rem;
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid var(--spatial-line);
  background: var(--spatial-surface-2);
  color: var(--spatial-gray);
  min-width: 96px;
}

.usage-badge strong {
  color: var(--spatial-graphite);
  font-weight: 700;
}

.payment-ticket-admin.status-processed header b {
  background: #dcfce7;
  color: #15803d;
}

.payment-ticket-admin.status-rejected header b {
  background: #fee2e2;
  color: #b42318;
}

.payment-ticket-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}

.danger-lite {
  border-color: rgba(239, 68, 68, .22) !important;
  color: #dc2626 !important;
  background: rgba(254, 242, 242, .72) !important;
}

.payment-empty {
  display: grid;
  place-items: center;
  min-height: 150px;
  border: 1px dashed rgba(100, 116, 139, .28);
  border-radius: 14px;
  color: #64748b;
  font-size: .88rem;
}

/* Forum Reports table styles matching AI 调用记录 style */
.forum-report-status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  font-size: 0.72rem;
  font-weight: 700;
  border-radius: 99px;
  white-space: nowrap;
}

.forum-report-status-badge.open {
  color: #ea580c;
  background: rgba(234, 88, 12, 0.06);
  border: 1px solid rgba(234, 88, 12, 0.15);
}

.forum-report-status-badge.processed {
  color: #16a34a;
  background: rgba(22, 163, 74, 0.06);
  border: 1px solid rgba(22, 163, 74, 0.15);
}

.forum-report-status-badge.rejected {
  color: var(--spatial-gray);
  background: rgba(148, 163, 184, 0.06);
  border: 1px solid rgba(148, 163, 184, 0.15);
}

.admin-table tr.status-open td {
  background-color: rgba(234, 88, 12, 0.015);
}

.admin-table tr.status-processed td {
  background-color: rgba(22, 163, 74, 0.01);
}

.admin-table tr.banned td {
  border-left: 3px solid rgba(239, 68, 68, 0.5);
}

/* Link Styling */
.report-post-link {
  text-decoration: none;
  color: inherit;
  cursor: pointer;
  transition: color 0.2s ease;
}

.report-post-link:hover {
  color: var(--spatial-accent) !important;
  text-decoration: underline;
}

/* Dark theme adaptation for Forum Reports */
:global(html[data-theme="dark"]) .forum-report-status-badge.open {
  color: #fb923c;
  background: rgba(251, 146, 60, 0.12);
  border-color: rgba(251, 146, 60, 0.2);
}

:global(html[data-theme="dark"]) .forum-report-status-badge.processed {
  color: #4ade80;
  background: rgba(74, 222, 128, 0.12);
  border-color: rgba(74, 222, 128, 0.2);
}

:global(html[data-theme="dark"]) .forum-report-status-badge.rejected {
  color: var(--spatial-silver);
  background: rgba(148, 163, 184, 0.12);
  border-color: rgba(148, 163, 184, 0.2);
}

:global(html[data-theme="dark"]) .admin-table tr.status-open td {
  background-color: rgba(251, 146, 60, 0.03);
}

:global(html[data-theme="dark"]) .admin-table tr.status-processed td {
  background-color: rgba(74, 222, 128, 0.02);
}

.campus-review-list {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 16px;
}

.campus-review-row {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid rgba(15, 23, 42, .08);
  border-radius: 14px;
  background: #ffffff;
}

.campus-review-row.status-pending {
  border-color: rgba(20, 184, 166, .28);
  background: linear-gradient(135deg, #ecfdf8, #fff);
}

.campus-review-row.status-approved {
  border-color: rgba(34, 197, 94, .22);
  background: linear-gradient(135deg, #f0fdf4, #fff);
}

.campus-review-row.status-rejected {
  background: linear-gradient(135deg, #fff1f2, #fff);
  border-color: rgba(244, 63, 94, .2);
}

.campus-review-row header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.campus-review-row header span,
.campus-review-row small,
.campus-review-row em {
  display: block;
  color: #64748b;
  font-size: .76rem;
  line-height: 1.5;
  font-style: normal;
}

.campus-review-row strong {
  display: block;
  margin: 4px 0;
  color: #0f172a;
  font-size: 1rem;
}

.campus-review-row header b {
  flex: 0 0 auto;
  padding: 5px 10px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: .75rem;
}

.campus-review-row.status-approved header b {
  color: #15803d;
  background: #dcfce7;
}

.campus-review-row.status-rejected header b {
  color: #be123c;
  background: #ffe4e6;
}

.campus-card-preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.campus-card-preview-grid figure {
  margin: 0;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.campus-image-button {
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: zoom-in;
}

.campus-card-preview-grid img {
  width: 100%;
  height: 180px;
  display: block;
  object-fit: cover;
}

.campus-card-preview-grid figcaption {
  padding: 8px 10px;
  color: #64748b;
  font-size: .76rem;
}

.campus-image-overlay {
  background: rgba(15, 23, 42, .72);
}

.campus-image-preview {
  width: min(980px, calc(100vw - 48px));
  max-height: calc(100vh - 64px);
  overflow: hidden;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 28px 90px rgba(2, 6, 23, .34);
}

.campus-image-preview header {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
}

.campus-image-preview img {
  width: 100%;
  max-height: calc(100vh - 126px);
  object-fit: contain;
  background: #0f172a;
}

.form-group textarea {
  width: 100%;
  min-height: 120px;
  border: 1px solid rgba(15, 23, 42, .1);
  border-radius: 10px;
  background: #fff;
  color: #0f172a;
  padding: 10px 12px;
  font: inherit;
  line-height: 1.6;
  resize: vertical;
}

/* User Table */
.table-container {
  overflow-x: auto;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 48px rgba(10, 10, 12, 0.03);
}

.admin-table {
  width: 100%;
  min-width: 1180px;
  border-collapse: collapse;
  table-layout: auto;
}

.admin-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid var(--spatial-line);
  white-space: nowrap;
  word-break: keep-all;
  writing-mode: horizontal-tb;
  font-size: 0.84rem;
  color: var(--spatial-graphite);
}

.admin-table th {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--spatial-silver);
  background: rgba(0, 0, 0, 0.01);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 2px solid var(--spatial-line);
  padding: 14px 16px;
}

.admin-table tr {
  transition: background-color 0.25s ease;
}

.admin-table tr:hover {
  background-color: rgba(0, 102, 255, 0.015);
}

.user-quota-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.user-quota-summary-grid article {
  min-height: 86px;
  padding: 18px 20px;
  border: 1px solid rgba(37, 99, 235, 0.12);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(239, 246, 255, 0.78));
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.04);
}

.user-quota-summary-grid span,
.quota-modal-snapshot span {
  display: block;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 700;
}

.user-quota-summary-grid strong {
  color: #0f172a;
  font-size: 1.35rem;
  letter-spacing: 0;
}

.quota-money-cell {
  color: #0f766e;
  font-size: 0.98rem;
}

.quota-limit-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  color: #1d4ed8;
  background: #eff6ff;
  font-weight: 700;
}

.quota-used-text {
  color: #475569;
  font-weight: 700;
}

.membership-plan-pill {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 700;
  border: 1px solid transparent;
}

.membership-plan-pill.plan-free {
  color: var(--spatial-silver);
  background: rgba(148, 163, 184, 0.08);
  border-color: rgba(148, 163, 184, 0.15);
}

.membership-plan-pill.plan-light {
  color: #087f5b;
  background: #e7f8f0;
  border-color: #b7ecd4;
}

.membership-plan-pill.plan-study {
  color: #1d4ed8;
  background: #eaf2ff;
  border-color: #c7dcff;
}

.membership-plan-pill.plan-lab {
  color: #7c3aed;
  background: #f3eefe;
  border-color: #ddd0fb;
}

.membership-plan-pill.plan-team {
  color: #b45309;
  background: #fff5dd;
  border-color: #fde7ad;
}

.membership-plan-pill.plan-team_plus {
  color: #9333ea;
  background: #f5edff;
  border-color: #dfc7ff;
}

.user-password-code {
  font-family: monospace;
  font-size: 0.8rem;
  background: rgba(249, 115, 22, 0.06);
  border: 1px solid rgba(249, 115, 22, 0.15);
  padding: 3px 8px;
  border-radius: 6px;
  color: #d97706;
  font-weight: 600;
  display: inline-block;
}

:global(html[data-theme="dark"]) .user-password-code {
  background: rgba(251, 146, 60, 0.1);
  border-color: rgba(251, 146, 60, 0.2);
  color: #fb923c;
}

/* Refined Membership Column Styles */
.membership-cycle-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.3;
}

.membership-cycle-cell strong {
  color: var(--spatial-graphite);
  font-size: 0.84rem;
  font-weight: 700;
}

.membership-cycle-cell small {
  color: var(--spatial-silver);
  font-size: 0.72rem;
}

.membership-usage-cell {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.usage-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.usage-badge.review-tag {
  background: rgba(37, 99, 235, 0.08);
  border: 1px solid rgba(37, 99, 235, 0.15);
  color: #3b82f6;
}

.usage-badge.ppt-tag {
  background: rgba(168, 85, 247, 0.08);
  border: 1px solid rgba(168, 85, 247, 0.15);
  color: #a855f7;
}

.usage-badge.chat-tag {
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.15);
  color: #10b981;
}

.usage-badge.token-tag {
  background: rgba(249, 115, 22, 0.08);
  border: 1px solid rgba(249, 115, 22, 0.15);
  color: #f97316;
}

/* Ensure strong inside badge stands out with high contrast */
.usage-badge strong {
  color: var(--spatial-graphite);
  font-weight: 800;
  margin-left: 2px;
}


.admin-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
  color: #64748b;
  font-size: 0.84rem;
}

.admin-pagination > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-pagination button,
.admin-pagination select {
  height: 34px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 10px;
  background: #ffffff;
  color: #0f172a;
  font-weight: 700;
}

.admin-pagination button {
  padding: 0 12px;
  cursor: pointer;
}

.admin-pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.pagination-size-select {
  padding: 0 10px;
}

.compact-pagination {
  padding: 12px 0 0;
  border-top: none;
}

.user-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: nowrap;
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  color: #ffffff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.role-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 99px;
  font-size: 0.8rem;
  font-weight: 600;
}

.role-admin {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.role-tutor {
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
}

.role-student {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.table-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: nowrap;
}

.team-detail-modal {
  width: min(1080px, calc(100vw - 48px));
  max-width: 1080px;
  max-height: 86vh;
  overflow: auto;
}

.team-detail-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0 20px;
}

.team-detail-summary > div {
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(0, 102, 255, 0.05);
}

.team-detail-summary span,
.team-members-table small {
  display: block;
  color: #64748b;
  font-size: 0.75rem;
  margin-top: 4px;
}

.team-members-table-wrap {
  overflow-x: auto;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
}

.team-members-table {
  width: 100%;
  min-width: 880px;
  border-collapse: collapse;
}

.team-members-table th,
.team-members-table td {
  padding: 14px 16px;
  text-align: left;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.team-members-table th {
  color: #64748b;
  font-size: 0.78rem;
  background: rgba(0, 0, 0, 0.02);
}

.team-members-empty {
  padding: 28px;
  text-align: center;
  color: #64748b;
}

@media (max-width: 720px) {
  .admin-shell {
    padding-inline: 18px;
  }

  .admin-page {
    padding: 168px 14px 36px;
  }

  .admin-page.admin-sidebar-collapsed {
    padding-left: 14px;
  }

  .admin-side-nav,
  .admin-side-nav.collapsed {
    top: 82px;
    left: 14px;
    right: 14px;
    bottom: auto;
    width: auto;
    height: 68px;
    border-radius: 20px;
  }

  .admin-side-brand {
    display: none;
  }

  .admin-side-tabs {
    flex-direction: row;
    padding: 10px;
    overflow-x: auto;
  }

  .admin-side-tab,
  .admin-side-nav.collapsed .admin-side-tab {
    flex: 0 0 auto;
    width: auto;
    justify-content: flex-start;
    min-height: 46px;
    padding: 0 12px;
  }

  .admin-side-nav.collapsed .admin-side-icon,
  .admin-side-icon {
    width: 30px;
    height: 30px;
  }

  .admin-side-label {
    display: inline;
  }

  .admin-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }

  .admin-header h2 {
    max-width: 8em;
    font-size: 2rem;
  }

  .admin-tabs-nav {
    width: 100%;
    border-radius: 16px;
  }

  .tab-btn {
    min-width: 132px;
    padding-inline: 16px;
    text-align: center;
  }

  .team-detail-summary {
    grid-template-columns: 1fr;
  }
}

:global(html[data-theme="dark"]) .admin-page {
  background: var(--spatial-warm);
}

:global(html[data-theme="dark"]) .admin-side-nav {
  border-color: var(--spatial-line);
  background: var(--spatial-glass);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.35);
}

:global(html[data-theme="dark"]) .admin-side-tab:hover {
  background: rgba(96, 165, 250, 0.1);
}

:global(html[data-theme="dark"]) .admin-side-tab.active {
  color: #93c5fd;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.22), rgba(79, 70, 229, 0.16));
  box-shadow: inset 0 0 0 1px rgba(96, 165, 250, 0.18);
}

:global(html[data-theme="dark"]) .admin-stat-card {
  background: rgba(18, 26, 40, 0.65);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

:global(html[data-theme="dark"]) .stat-icon {
  background: rgba(30, 41, 59, 0.9);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:global(html[data-theme="dark"]) .stat-value {
  color: #f8fafc;
}

:global(html[data-theme="dark"]) .stat-label,
:global(html[data-theme="dark"]) .stat-sub {
  color: #94a3b8;
}

:global(html[data-theme="dark"]) .user-quota-summary-grid article {
  background: linear-gradient(135deg, rgba(18, 26, 40, 0.96), rgba(30, 41, 59, 0.78));
  border-color: rgba(96, 165, 250, 0.2);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.15);
}

:global(html[data-theme="dark"]) .user-quota-summary-grid span {
  color: #94a3b8;
}

:global(html[data-theme="dark"]) .user-quota-summary-grid strong {
  color: #f8fafc;
}

:global(html[data-theme="dark"]) .admin-table th {
  color: #94a3b8;
  background: rgba(255, 255, 255, 0.02);
}

:global(html[data-theme="dark"]) .admin-table tr:hover {
  background-color: rgba(255, 255, 255, 0.02);
}

.action-btn {
  border: none;
  background: none;
  font-size: 0.85rem;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.text-btn {
  color: #0066ff;
}

.text-btn:hover {
  color: #004ecc;
  text-decoration: underline;
}

.text-danger-btn {
  color: #ef4444;
}

.text-danger-btn:hover {
  color: #b91c1c;
  text-decoration: underline;
}

/* Models pane */
.model-route-heading {
  margin-bottom: 18px;
}

.admin-route-badge {
  padding: 7px 12px;
  border-radius: 999px;
  color: #075ee5;
  background: #edf4ff;
  font-size: 0.78rem;
  font-weight: 700;
}

.model-scene-switch {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  padding: 8px;
  margin-bottom: 18px;
  border: 1px solid #dfe7f2;
  border-radius: 16px;
  background: #f8fbff;
}

.scene-switch-btn {
  display: flex;
  min-height: 68px;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 5px;
  padding: 12px 13px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  color: #334155;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
}

.scene-switch-btn strong {
  color: #0f172a;
  font-size: 0.95rem;
}

.scene-switch-btn span {
  color: #64748b;
  font-size: 0.78rem;
  line-height: 1.4;
}

.scene-switch-btn.active {
  border-color: #2563eb;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.08);
}

.models-pane :deep(.reader-panel) {
  margin-bottom: 24px;
  border: 1px solid #dfe7f2;
  border-radius: 16px;
  background: #ffffff;
}

.models-pane :deep(.reader-panel-header) {
  align-items: flex-start;
}

.model-pool-panel {
  padding: 22px;
  margin-bottom: 24px;
  border: 1px solid #dfe7f2;
  border-radius: 18px;
  background: #ffffff;
}

.model-pool-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.model-pool-header h4 {
  margin: 0;
  color: #0f172a;
  font-size: 1.12rem;
}

.pool-kicker {
  display: inline-flex;
  margin-bottom: 5px;
  color: #2563eb;
  font-size: 0.72rem;
  font-weight: 800;
}

.model-pool-header p {
  max-width: 72ch;
  margin: 5px 0 0;
  color: #536174;
  font-size: 0.82rem;
  line-height: 1.55;
}

.model-pool-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.pool-summary {
  padding: 7px 10px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #334155;
  font-size: 0.78rem;
  font-weight: 700;
}

.pool-refresh-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 0.76rem;
  font-weight: 760;
}

.pool-refresh-chip::before {
  width: 7px;
  height: 7px;
  content: "";
  border-radius: 50%;
  background: #94a3b8;
}

.pool-refresh-chip.active::before {
  background: #10b981;
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.12);
}

.model-pool-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(284px, 1fr));
  gap: 14px;
}

.model-pool-card {
  position: relative;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto auto;
  gap: 12px;
  min-height: 236px;
  padding: 16px;
  border: 1px solid #e4eaf2;
  border-radius: 14px;
  background: #fbfdff;
  overflow: hidden;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.model-pool-card::before {
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  content: "";
  background: #cbd5e1;
}

.model-pool-card:hover {
  transform: translateY(-2px);
  border-color: #c7d7ef;
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.07);
}

.model-pool-card.status-available::before { background: linear-gradient(90deg, #10b981, #38bdf8); }
.model-pool-card.status-limited::before,
.model-pool-card.status-timeout::before { background: linear-gradient(90deg, #f59e0b, #facc15); }
.model-pool-card.status-auth_error::before,
.model-pool-card.status-failed::before { background: linear-gradient(90deg, #ef4444, #fb7185); }
.model-pool-card.status-needs_adapter::before { background: linear-gradient(90deg, #6366f1, #8b5cf6); }

.model-pool-card.message-expanded {
  min-height: 292px;
}

.pool-card-top,
.pool-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.pool-rank-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.pool-rank {
  color: #64748b;
  font-size: 0.76rem;
  font-weight: 850;
}

.pool-card-body {
  min-width: 0;
  min-height: 0;
}

.pool-state-dot {
  width: 10px;
  height: 10px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #94a3b8;
}

.status-available .pool-state-dot { background: #10b981; }
.status-limited .pool-state-dot,
.status-timeout .pool-state-dot { background: #f59e0b; }
.status-auth_error .pool-state-dot,
.status-failed .pool-state-dot { background: #ef4444; }
.status-needs_adapter .pool-state-dot { background: #6366f1; }

.pool-title-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  min-width: 0;
}

.pool-title-line strong {
  max-width: 100%;
  color: #172033;
  font-size: 0.92rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pool-card-body p {
  margin: 9px 0 5px;
  color: #334155;
  font-size: 0.88rem;
  font-weight: 720;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pool-card-body small {
  display: block;
  color: #64748b;
  font-size: 0.76rem;
  overflow-wrap: anywhere;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pool-health-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.pool-health-metrics span {
  min-width: 0;
  padding: 9px 10px;
  border: 1px solid #e6edf6;
  border-radius: 10px;
  background: #f8fafc;
}

.pool-health-metrics small {
  display: block;
  color: #7a8798;
  font-size: 0.68rem;
  font-weight: 700;
}

.pool-health-metrics strong {
  display: block;
  margin-top: 3px;
  color: #172033;
  font-size: 0.84rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pool-chip {
  padding: 3px 7px;
  border-radius: 999px;
  background: #eef2f7;
  color: #475569;
  font-size: 0.68rem;
  font-weight: 750;
}

.pool-chip.primary,
.chip-available {
  background: #e7f8f0;
  color: #047857;
}

.chip-limited,
.chip-timeout {
  background: #fff7e6;
  color: #a15c07;
}

.chip-auth_error,
.chip-failed {
  background: #fff0f1;
  color: #b42318;
}

.chip-needs_adapter {
  background: #eef2ff;
  color: #4338ca;
}

.pool-card-footer {
  color: #536174;
  font-size: 0.78rem;
  flex-wrap: wrap;
}

.pool-card-footer .action-btn {
  padding: 0;
  white-space: nowrap;
}

.pool-message-wrap {
  min-width: 0;
}

.pool-message {
  margin: 0;
  color: #536174;
  font-size: 0.74rem;
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.pool-message.expanded {
  display: block;
  max-height: 110px;
  overflow: auto;
  padding-right: 4px;
  -webkit-line-clamp: unset;
}

.pool-message-toggle {
  margin-top: 4px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #2557d6;
  font-size: 0.72rem;
  font-weight: 800;
  cursor: pointer;
}

.pool-empty {
  padding: 28px;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  color: #64748b;
  text-align: center;
}

.models-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
  gap: 24px;
}

.models-card-col {
  padding: 28px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 16px 48px rgba(10, 10, 12, 0.03);
}

.models-card-col h4 {
  font-family: 'Outfit', sans-serif;
  font-size: 1.15rem;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 24px;
  letter-spacing: -0.01em;
}

.model-status-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0,0,0,0.03);
}

.model-status-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.status-indicator.online {
  background: #10b981;
  box-shadow: 0 0 8px #10b981;
}

.status-indicator.offline {
  background: #ef4444;
  box-shadow: 0 0 8px #ef4444;
}

.status-details {
  display: flex;
  flex-direction: column;
}

.status-details strong {
  font-size: 0.95rem;
  color: #1e293b;
  font-weight: 600;
}

.status-details span {
  font-size: 0.8rem;
  color: #64748b;
  margin-top: 2px;
}

.trend-bars {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.trend-bar-row {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 0.85rem;
  color: #475569;
}

.trend-bar-row span:first-child {
  width: 85px;
  font-weight: 600;
}

.trend-bar-row span:last-child {
  width: auto;
  min-width: 140px;
  text-align: right;
  font-weight: 500;
  font-family: monospace;
}

.bar-outer {
  flex: 1;
  height: 10px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 99px;
  overflow: hidden;
}

.bar-inner {
  height: 100%;
  border-radius: 99px;
  transition: width 0.8s cubic-bezier(0.25, 1, 0.5, 1);
}

/* Logs console (Developer Interface) */
.log-console-container {
  border-radius: 16px;
  padding: 24px;
  background: rgba(10, 10, 12, 0.92) !important;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 8px 32px rgba(0,0,0,0.5), 0 16px 48px rgba(0,0,0,0.15);
}

.console-body {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.82rem;
  color: #a7f3d0;
  max-height: 450px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.log-line {
  line-height: 1.5;
  display: flex;
  align-items: flex-start;
}

.log-time {
  color: #64748b;
  margin-right: 12px;
  flex-shrink: 0;
}

.log-tag {
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.7rem;
  margin-right: 12px;
  flex-shrink: 0;
  letter-spacing: 0.03em;
}

.log-tag.info {
  background: rgba(59, 130, 246, 0.15);
  color: #93c5fd;
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.log-tag.warn {
  background: rgba(245, 158, 11, 0.15);
  color: #fde047;
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.log-tag.error {
  background: rgba(239, 68, 68, 0.15);
  color: #fca5a5;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.log-msg {
  color: #e2e8f0;
}

.log-pagination {
  margin-top: 18px;
  border-top-color: rgba(255, 255, 255, 0.08);
  color: #cbd5e1;
}

.log-pagination button,
.log-pagination select {
  background: rgba(15, 23, 42, 0.95);
  color: #e2e8f0;
  border-color: rgba(148, 163, 184, 0.18);
}

/* Modals */
.admin-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(10, 10, 12, 0.18);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.admin-modal-card {
  width: 100%;
  max-width: 400px;
  background: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 24px 64px rgba(10, 10, 12, 0.15);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.admin-modal-card.team-detail-modal {
  width: min(1240px, calc(100vw - 64px));
  max-width: 1240px;
  min-height: 560px;
  max-height: 88vh;
  overflow: auto;
  padding: 36px 40px;
  background: #ffffff;
}

.admin-modal-card.team-detail-modal.spatial-glass-panel {
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

@media (max-width: 720px) {
  .admin-modal-card.team-detail-modal {
    width: calc(100vw - 24px);
    min-height: auto;
    padding: 24px 18px;
  }
}

.admin-modal-card h4 {
  font-size: 1.15rem;
  font-weight: 600;
  color: #0f172a;
}

.admin-modal-card p {
  font-size: 0.85rem;
  color: #64748b;
  margin-top: 4px;
}

.admin-select {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #ffffff;
  color: #1e293b;
  font-size: 0.95rem;
  margin-top: 6px;
  outline: none;
}

.admin-select:focus {
  border-color: #0066ff;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.compact-btn {
  padding: 8px 16px;
  font-size: 0.85rem;
}

.pane-description {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 0.82rem;
}

.forum-report-detail-card {
  width: min(760px, calc(100vw - 48px));
}

.forum-report-detail-grid {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.forum-report-detail-grid article {
  padding: 14px;
  border: 1px solid rgba(148, 163, 184, .25);
  border-radius: 14px;
  background: rgba(248, 250, 252, .74);
}

.forum-report-detail-grid span {
  display: block;
  margin-bottom: 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 850;
}

.forum-report-detail-grid p {
  max-height: 240px;
  overflow: auto;
  margin: 0;
  color: #243044;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* Dark theme adaptation for detail modal grid */
:global(html[data-theme="dark"]) .forum-report-detail-grid article {
  background: rgba(15, 23, 42, 0.4);
  border-color: rgba(148, 163, 184, 0.15);
}

:global(html[data-theme="dark"]) .forum-report-detail-grid span {
  color: #60a5fa;
}

:global(html[data-theme="dark"]) .forum-report-detail-grid p {
  color: var(--spatial-silver);
}

.site-message-admin-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.8fr) minmax(0, 1.2fr);
  gap: 24px;
}

.site-message-form,
.site-message-list {
  padding: 24px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
}

.site-message-form h4,
.site-message-list h4 {
  margin: 0;
  color: #0f172a;
  font-size: 1rem;
}

.site-message-type-switch {
  margin-top: 16px;
  padding: 4px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  border-radius: 12px;
  background: rgba(226, 232, 240, .72);
}

.site-message-type-switch button {
  height: 36px;
  border: 0;
  border-radius: 9px;
  color: #64748b;
  background: transparent;
  font-size: .78rem;
  font-weight: 850;
  cursor: pointer;
}

.site-message-type-switch button.active {
  color: #0f172a;
  background: #fff;
  box-shadow: 0 6px 16px rgba(15, 23, 42, .08);
}

.site-message-form label {
  display: grid;
  gap: 7px;
  margin-top: 18px;
  color: #475569;
  font-size: 0.82rem;
  font-weight: 600;
}

.site-message-form input,
.site-message-form textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 11px 12px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 9px;
  color: #1e293b;
  background: #fff;
  outline: none;
}

.site-message-form textarea {
  min-height: 150px;
  resize: vertical;
  font: inherit;
}

.site-message-form input:focus,
.site-message-form textarea:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.07);
}

.site-message-form-footer,
.site-message-list-header,
.site-message-title-row,
.site-message-actions {
  display: flex;
  align-items: center;
}

.site-message-form-footer {
  justify-content: space-between;
  margin-top: 14px;
  color: #94a3b8;
  font-size: 0.75rem;
}

.site-message-list-header {
  justify-content: space-between;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.site-message-list-header span {
  color: #64748b;
  font-size: 0.78rem;
}

.site-message-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  padding: 18px 0;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.site-message-title-row { gap: 9px; }
.site-message-title-row strong { color: #1e293b; font-size: 0.92rem; }
.site-message-title-row span {
  padding: 3px 8px;
  border-radius: 99px;
  font-size: 0.68rem;
  font-weight: 700;
}
.message-active { color: #047857; background: rgba(16, 185, 129, 0.1); }
.message-inactive { color: #64748b; background: rgba(100, 116, 139, 0.1); }
.message-type-badge.notice { color: #2563eb; background: rgba(37, 99, 235, .1); }
.message-type-badge.timeline { color: #7c3aed; background: rgba(124, 58, 237, .1); }
.site-message-row p { margin: 7px 0; color: #64748b; font-size: 0.8rem; line-height: 1.55; }
.site-message-row small { color: #94a3b8; font-size: 0.7rem; }
.site-message-actions { align-self: center; gap: 12px; }
.site-message-empty { padding: 46px 20px; color: #94a3b8; font-size: 0.84rem; text-align: center; }
.tutorial-admin-grid {
  grid-template-columns: minmax(360px, 0.9fr) minmax(520px, 1.1fr);
}
.tutorial-form-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(140px, .5fr) 96px;
  gap: 12px;
}
.tutorial-markdown-input {
  min-height: 360px !important;
  font-family: "SFMono-Regular", ui-monospace, Menlo, Consolas, monospace !important;
  line-height: 1.72;
}
.tutorial-form .site-message-form-footer > div {
  display: flex;
  gap: 10px;
  align-items: center;
}
.tutorial-admin-row p {
  max-height: 3.2em;
  overflow: hidden;
}

.quota-modal-snapshot {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.quota-modal-snapshot > div {
  padding: 14px 16px;
  border: 1px solid rgba(37, 99, 235, 0.12);
  border-radius: 14px;
  background: #f8fbff;
}

.quota-modal-snapshot strong {
  color: #0f172a;
  font-size: 1.05rem;
}
/* AI usage ledger styles are now isolated in AdminAiUsagePanel.vue */

.relay-research-panel {
  margin-top: 24px;
  padding: 24px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
}

.relay-research-header,
.relay-research-actions {
  display: flex;
  align-items: center;
}

.relay-research-header {
  justify-content: space-between;
  gap: 18px;
}

.relay-research-header h4 {
  margin: 0;
  color: #0f172a;
  font-size: 1rem;
}

.relay-research-header p {
  max-width: 820px;
  margin: 6px 0 0;
  color: #64748b;
  font-size: 0.82rem;
  line-height: 1.6;
}

.relay-research-actions {
  justify-content: flex-end;
  gap: 12px;
  color: #64748b;
  font-size: 0.76rem;
  white-space: nowrap;
}

.relay-summary-grid,
.relay-purchase-grid,
.economy-routing-grid,
.membership-recommendation-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.relay-purchase-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.economy-routing-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.relay-summary-grid article,
.relay-purchase-grid article,
.economy-routing-grid article,
.membership-recommendation-grid article {
  padding: 14px;
  border: 1px solid rgba(15, 23, 42, 0.07);
  border-radius: 12px;
  background: #f8fbff;
}

.relay-summary-grid span,
.relay-purchase-grid span,
.economy-routing-grid span,
.membership-recommendation-grid span {
  display: block;
  color: #2563eb;
  font-size: 0.7rem;
  font-weight: 800;
}

.relay-summary-grid strong,
.relay-purchase-grid strong,
.economy-routing-grid strong,
.membership-recommendation-grid strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 0.92rem;
}

.relay-summary-grid p,
.relay-purchase-grid p,
.relay-purchase-grid small,
.economy-routing-grid p,
.economy-routing-grid small,
.membership-recommendation-grid p,
.membership-recommendation-grid small {
  display: block;
  margin: 6px 0 0;
  color: #64748b;
  font-size: 0.76rem;
  line-height: 1.5;
}

.economy-model-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.economy-model-heading h5,
.economy-model-heading p {
  margin: 0;
}

.economy-model-heading h5 {
  color: #0f172a;
  font-size: 0.92rem;
}

.economy-model-heading p {
  margin-top: 5px;
  color: #64748b;
  font-size: 0.76rem;
}

.economy-model-heading > strong {
  color: #2563eb;
  font-size: 0.78rem;
  white-space: nowrap;
}

.economy-table-wrap {
  margin-top: 12px;
  overflow-x: auto;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8px;
  background: #fff;
}

.economy-table {
  width: 100%;
  min-width: 1040px;
  border-collapse: collapse;
  color: #334155;
  font-size: 0.76rem;
}

.economy-table th,
.economy-table td {
  padding: 10px 12px;
  text-align: left;
  vertical-align: top;
}

.economy-table th {
  color: #64748b;
  font-size: 0.68rem;
  background: #f8fafc;
}

.economy-table td {
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

.economy-table b,
.economy-table small {
  display: block;
}

.economy-table b {
  color: #0f172a;
}

.economy-table small {
  margin-top: 3px;
  color: #94a3b8;
}

.relay-table-wrap {
  margin-top: 18px;
  overflow-x: auto;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  background: #fff;
}

.relay-table {
  width: 100%;
  min-width: 1540px;
  border-collapse: collapse;
  color: #334155;
  font-size: 0.78rem;
}

.relay-table th {
  padding: 11px 12px;
  color: #64748b;
  font-size: 0.7rem;
  text-align: left;
  background: #f8fafc;
}

.relay-table td {
  padding: 13px 12px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
  vertical-align: top;
}

.relay-table td:first-child {
  color: #94a3b8;
  font-weight: 800;
}

.relay-table strong,
.relay-table b {
  display: block;
  color: #0f172a;
}

.relay-table a,
.relay-table td span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  text-decoration: none;
}

.relay-table small {
  display: block;
  margin-top: 3px;
  color: #94a3b8;
}

.relay-status {
  display: inline-flex !important;
  margin-top: 0 !important;
  padding: 4px 7px;
  border: 1px solid rgba(220, 38, 38, 0.18);
  border-radius: 6px;
  color: #b91c1c !important;
  background: #fff7f7;
  font-size: 0.7rem;
  font-weight: 800;
  white-space: nowrap;
}

.relay-status.is-open {
  border-color: rgba(5, 150, 105, 0.2);
  color: #047857 !important;
  background: #f0fdf8;
}

.relay-cost-cell {
  min-width: 180px;
}

.relay-economy-cell {
  min-width: 240px;
}

.relay-economy-cell > span {
  margin-top: 0 !important;
  padding: 0 0 8px;
}

.relay-economy-cell > span + span {
  padding-top: 8px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

.relay-suggestion {
  display: inline-flex !important;
  width: max-content;
  margin-top: 0 !important;
  padding: 4px 8px;
  border-radius: 999px;
  color: #075985 !important;
  background: rgba(14, 165, 233, 0.1);
  font-weight: 800;
}

.relay-empty {
  padding: 28px !important;
  color: #94a3b8;
  text-align: center;
}

.topic-admin-header {
  align-items: flex-start;
  gap: 20px;
}

.topic-admin-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.topic-admin-hot-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 16px;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: linear-gradient(135deg, #0f766e 0%, #2563eb 100%);
  font-size: 0.86rem;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.22);
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}

.topic-admin-hot-btn::before {
  content: "+";
  display: inline-grid;
  place-items: center;
  width: 20px;
  height: 20px;
  margin-right: 8px;
  border-radius: 999px;
  color: #0f766e;
  background: #ffffff;
  font-size: 0.92rem;
  line-height: 1;
}

.topic-admin-hot-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 18px 34px rgba(37, 99, 235, 0.28);
}

.topic-admin-hot-btn:disabled {
  cursor: wait;
  opacity: 0.68;
}

.topic-admin-search {
  width: 240px;
  height: 38px;
  padding: 0 13px;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 10px;
  color: #0f172a;
  background: #fff;
  outline: none;
}

.topic-admin-search:focus {
  border-color: rgba(37, 99, 235, 0.55);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
}

.topic-admin-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.topic-admin-card {
  position: relative;
  display: grid;
  gap: 13px;
  min-height: 360px;
  padding: 20px 20px 18px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.05);
}

.topic-admin-delete {
  position: absolute;
  top: 12px;
  right: 12px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 32px;
  padding: 0 10px 0 7px;
  border: 1px solid rgba(220, 38, 38, 0.24);
  border-radius: 999px;
  color: #b91c1c;
  font-size: 0.74rem;
  font-weight: 900;
  background: #fff1f2;
  cursor: pointer;
  z-index: 2;
}

.topic-admin-delete span {
  display: inline-grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  color: #ffffff;
  background: #ef4444;
  font-size: 0.95rem;
  line-height: 1;
}

.topic-admin-delete:hover {
  transform: translateY(-1px);
  border-color: rgba(220, 38, 38, 0.42);
  box-shadow: 0 8px 18px rgba(220, 38, 38, 0.14);
}

.topic-admin-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-right: 58px;
}

.topic-admin-meta small {
  color: #94a3b8;
  font-size: 0.75rem;
  font-weight: 700;
}

.topic-provider-badge {
  display: inline-flex;
  align-items: center;
  width: max-content;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 900;
}

.topic-provider-badge.official {
  color: #075985;
  background: rgba(14, 165, 233, 0.12);
  border: 1px solid rgba(14, 165, 233, 0.16);
}

.topic-provider-badge.anonymous {
  color: #475569;
  background: #f1f5f9;
  border: 1px solid rgba(148, 163, 184, 0.22);
}

.topic-admin-card h4 {
  margin: 0;
  padding-right: 36px;
  color: #0f172a;
  font-size: 1.08rem;
  line-height: 1.35;
}

.topic-admin-card > p {
  margin: 0;
  color: #64748b;
  font-size: 0.86rem;
  line-height: 1.65;
}

.topic-admin-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.topic-admin-tags span {
  padding: 5px 9px;
  border-radius: 999px;
  color: #0f766e;
  background: rgba(20, 184, 166, 0.1);
  font-size: 0.72rem;
  font-weight: 800;
}

.topic-admin-subtopics {
  display: grid;
  gap: 9px;
}

.topic-admin-subtopic {
  display: grid;
  gap: 5px;
  padding: 12px;
  border: 1px solid rgba(20, 184, 166, 0.14);
  border-radius: 12px;
  background: rgba(240, 253, 250, 0.66);
}

.topic-admin-subtopic strong {
  color: #0f172a;
  font-size: 0.86rem;
}

.topic-admin-subtopic small,
.topic-admin-subtopic em {
  color: #64748b;
  font-size: 0.75rem;
  line-height: 1.55;
  font-style: normal;
}

.topic-admin-subtopic em {
  color: #0f766e;
  font-weight: 700;
}

.topic-admin-card footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
  color: #64748b;
  font-size: 0.75rem;
  font-weight: 800;
}

.topic-admin-empty {
  grid-column: 1 / -1;
  padding: 34px;
  border: 1px dashed rgba(148, 163, 184, 0.35);
  border-radius: 16px;
  color: #64748b;
  background: #fff;
  text-align: center;
}

@media (max-width: 900px) {
  .model-scene-switch {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .model-pool-header,
  .model-pool-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .model-pool-actions {
    width: 100%;
  }

  .route-health-grid {
    grid-template-columns: 1fr;
  }

  .ledger-header,
  .ledger-pagination {
    align-items: flex-start;
    flex-direction: column;
  }

  .ledger-filters,
  .ledger-summary {
    grid-template-columns: 1fr;
  }

  .site-message-admin-grid,
  .tutorial-admin-grid,
  .payment-admin-grid,
  .relay-summary-grid,
  .relay-purchase-grid,
  .economy-routing-grid,
  .membership-recommendation-grid,
  .topic-admin-grid {
    grid-template-columns: 1fr;
  }
  .relay-research-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .tutorial-form-row {
    grid-template-columns: 1fr;
  }
  .campus-card-preview-grid {
    grid-template-columns: 1fr;
  }
  .topic-admin-header,
  .topic-admin-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
  .topic-admin-hot-btn {
    width: 100%;
  }
  .topic-admin-search {
    width: 100%;
  }
}

@media (max-width: 860px) {
  .user-quota-summary-grid,
  .quota-modal-snapshot {
    grid-template-columns: 1fr;
  }

  .admin-pagination {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 560px) {
  .model-scene-switch {
    grid-template-columns: 1fr;
  }

  .pool-health-metrics {
    grid-template-columns: 1fr;
  }
}

/* --- Redesigned Models Dashboard Styles --- */
.models-redesign-pane {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.scene-pools-entrance-panel {
  padding: 24px;
  border-radius: 20px;
}

.scene-pools-entrance-panel .entrance-header {
  margin-bottom: 20px;
}

.scene-pools-entrance-panel .entrance-header h4 {
  font-size: 1.25rem;
  margin: 6px 0;
  color: var(--spatial-graphite);
}

.scene-pools-entrance-panel .entrance-header p {
  color: var(--spatial-silver);
  font-size: 0.85rem;
  margin: 0;
}

.scene-buttons-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.scene-pool-btn {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--spatial-line);
  background: var(--spatial-surface-2);
  border-radius: 16px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.scene-pool-btn:hover {
  transform: translateY(-2px);
  border-color: var(--spatial-accent-line);
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.08);
}

.scene-btn-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--spatial-accent-soft);
  color: var(--spatial-accent);
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.scene-btn-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.scene-btn-info strong {
  font-size: 0.9rem;
  color: var(--spatial-graphite);
}

.scene-btn-info span {
  font-size: 0.72rem;
  color: var(--spatial-silver);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.models-dashboard-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
  align-items: start;
}

.dashboard-left-col {
  padding: 20px;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.col-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.col-header h5 {
  font-size: 1rem;
  margin: 0;
  color: var(--spatial-graphite);
}

.relays-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 500px;
  overflow-y: auto;
}

.relay-item-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--spatial-line);
  background: var(--spatial-surface-2);
  cursor: pointer;
  transition: all 0.2s ease;
}

.relay-item-card:hover {
  border-color: var(--spatial-accent-line);
  background: var(--spatial-accent-soft);
}

.relay-item-card.active {
  border-color: var(--spatial-accent);
  background: var(--spatial-accent-soft);
  box-shadow: inset 0 0 0 1px var(--spatial-accent-line);
}

.relay-card-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.relay-card-main strong {
  font-size: 0.88rem;
  color: var(--spatial-graphite);
}

.relay-url {
  font-size: 0.72rem;
  color: var(--spatial-silver);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.relay-delete-btn {
  background: transparent;
  border: 0;
  color: var(--spatial-silver);
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  display: grid;
  place-items: center;
  transition: all 0.2s ease;
}

.relay-delete-btn:hover {
  color: #ff3b30;
  background: rgba(255, 59, 48, 0.1);
}

.dashboard-right-col {
  padding: 24px;
  border-radius: 20px;
  min-height: 400px;
}

.relay-models-header {
  margin-bottom: 20px;
  border-bottom: 1px solid var(--spatial-line);
  padding-bottom: 16px;
}

.relay-models-header h5 {
  font-size: 1.1rem;
  margin: 0;
  color: var(--spatial-graphite);
}

.relay-models-header .subtitle {
  font-size: 0.8rem;
  color: var(--spatial-silver);
  margin: 4px 0 0 0;
}

.models-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.model-item-card {
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--spatial-line);
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: var(--spatial-surface-2);
}

.model-card-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.model-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--spatial-accent-soft);
  color: var(--spatial-accent);
  font-weight: 800;
  font-size: 0.85rem;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.model-name-wrapper {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.model-id-label {
  font-size: 0.9rem;
  color: var(--spatial-graphite);
  word-break: break-all;
}

.model-owner-tag {
  font-size: 0.72rem;
  color: var(--spatial-silver);
  text-transform: uppercase;
  margin-top: 1px;
}

.model-card-metrics {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid var(--spatial-line);
  border-bottom: 1px solid var(--spatial-line);
  padding: 10px 0;
}

.speed-test-btn {
  background: var(--spatial-warm-2);
  border: 1px solid var(--spatial-line);
  color: var(--spatial-graphite);
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.speed-test-btn:hover:not(:disabled) {
  background: var(--spatial-accent-soft);
  border-color: var(--spatial-accent-line);
  color: var(--spatial-accent);
}

.speed-test-btn.testing {
  color: var(--spatial-silver);
  cursor: not-allowed;
}

.latency-badge {
  font-size: 0.8rem;
  font-weight: 700;
  color: #34c759;
  background: rgba(52, 199, 89, 0.12);
  padding: 4px 8px;
  border-radius: 6px;
}

.latency-badge.error {
  color: #ff3b30;
  background: rgba(255, 59, 48, 0.12);
}

.speed-error-banner {
  background: rgba(255, 59, 48, 0.08);
  border: 1px solid rgba(255, 59, 48, 0.15);
  color: #ff3b30;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 0.72rem;
  word-break: break-all;
  max-height: 60px;
  overflow-y: auto;
}

.model-scenes-assign {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.assign-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--spatial-silver);
  text-transform: uppercase;
}

.scene-checkboxes-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.scene-checkbox-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  color: var(--spatial-gray);
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 6px;
  transition: background 0.2s ease;
}

.scene-checkbox-item:hover {
  background: var(--spatial-warm-2);
}

.scene-checkbox-item.checked {
  color: var(--spatial-accent);
  font-weight: 600;
}

.scene-checkbox-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.scene-checkbox-item input {
  cursor: pointer;
}

/* Modals layout customization */
.scene-pool-modal-card {
  max-width: 580px;
  width: 90%;
}

.pool-modal-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--spatial-line);
  padding-bottom: 14px;
}

.pool-nodes-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 4px;
}

.pool-node-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--spatial-line);
  background: var(--spatial-surface-2);
  transition: all 0.2s ease;
}

.pool-node-item.available {
  border-left: 4px solid #34c759;
}

.pool-node-item.failed,
.pool-node-item.auth_error,
.pool-node-item.timeout {
  border-left: 4px solid #ff3b30;
}

.node-main-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 2px;
}

.node-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.node-title strong {
  font-size: 0.88rem;
  color: var(--spatial-graphite);
}

.model-badge {
  font-size: 0.72rem;
  background: var(--spatial-warm-2);
  color: var(--spatial-silver);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

.node-badge {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}

.node-badge.active-tag {
  background: rgba(37, 99, 235, 0.12);
  color: var(--spatial-accent);
}

.node-url {
  font-size: 0.72rem;
  color: var(--spatial-silver);
  word-break: break-all;
}

.node-message {
  font-size: 0.7rem;
  color: #ff3b30;
  margin-top: 2px;
}

.node-metrics {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.node-latency {
  font-size: 0.8rem;
  font-weight: 700;
  color: #34c759;
}

.node-latency.error {
  color: #ff3b30;
}

.node-remove-btn {
  background: transparent;
  border: 0;
  color: var(--spatial-silver);
  font-size: 1.2rem;
  cursor: pointer;
  padding: 0 4px;
  display: grid;
  place-items: center;
  transition: color 0.2s ease;
}

.node-remove-btn:hover {
  color: #ff3b30;
}

/* Sidebar Toggle Overlay */
.admin-side-toggle {
  position: absolute;
  right: -10px;
  top: 24px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1px solid var(--spatial-line);
  background: var(--spatial-surface);
  color: var(--spatial-graphite);
  display: grid;
  place-items: center;
  cursor: pointer;
  z-index: 40;
  box-shadow: 0 4px 10px rgba(0,0,0,0.1);
  transition: transform 0.2s ease, background 0.2s ease;
}

.admin-side-toggle:hover {
  background: var(--spatial-warm-2);
}

/* Form Styles for Modal */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}

.form-group label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--spatial-gray);
}


/* --- Redesigned 3-Column Models Dashboard & Pools Overview CSS --- */
.models-pane-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.models-pane-header h3 {
  font-family: 'Outfit', sans-serif;
  font-size: 2.2rem;
  font-weight: 600;
  letter-spacing: -0.03em;
  color: var(--spatial-graphite);
  margin: 0;
}

.models-pane-header .pane-description {
  font-size: 0.9rem;
  color: var(--spatial-silver);
  margin: 4px 0 0 0;
}

.models-two-col-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  align-items: start;
}

@media (max-width: 990px) {
  .models-two-col-layout {
    grid-template-columns: 1fr;
  }
}

.models-right-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.providers-column,
.config-card-panel,
.models-grid-panel {
  padding: 20px;
  border-radius: 20px;
}

.providers-column {
  min-height: 480px;
  display: flex;
  flex-direction: column;
}

.config-card-panel {
  display: flex;
  flex-direction: column;
}

.models-grid-panel {
  display: flex;
  flex-direction: column;
  min-height: 380px;
}

.providers-column .column-header,
.config-card-panel .column-header,
.models-grid-panel .column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--spatial-line);
  padding-bottom: 12px;
}

.column-header strong {
  font-size: 0.95rem;
  color: var(--spatial-graphite);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.add-provider-btn {
  background: var(--spatial-accent-soft);
  border: 1px solid var(--spatial-accent-line);
  color: var(--spatial-accent);
  padding: 4px 8px;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.add-provider-btn:hover {
  background: var(--spatial-accent);
  color: #fff;
}

.providers-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 450px;
  overflow-y: auto;
}

.provider-item-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--spatial-line);
  background: var(--spatial-surface-2);
  cursor: pointer;
  transition: all 0.2s ease;
}

.provider-item-card:hover {
  border-color: var(--spatial-accent-line);
  background: var(--spatial-accent-soft);
}

.provider-item-card.active {
  border-color: var(--spatial-accent);
  background: var(--spatial-accent-soft);
  box-shadow: inset 0 0 0 1px var(--spatial-accent-line);
}

.provider-card-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.provider-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--spatial-graphite);
}

.provider-url {
  font-size: 0.7rem;
  color: var(--spatial-silver);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.provider-delete-btn {
  background: transparent;
  border: 0;
  color: var(--spatial-silver);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  display: grid;
  place-items: center;
  transition: all 0.2s ease;
}

.provider-delete-btn:hover {
  color: #ff3b30;
  background: rgba(255, 59, 48, 0.08);
}

.form-body-horizontal {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  width: 100%;
}

@media (max-width: 768px) {
  .form-body-horizontal {
    flex-direction: column;
    align-items: stretch;
  }
}

.form-group-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group-item label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--spatial-silver);
}

.save-config-btn {
  min-height: 40px;
  padding: 0 20px;
  flex-shrink: 0;
}

.models-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.model-dashboard-card {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--spatial-line);
  background: var(--spatial-surface-2);
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.model-dashboard-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(10, 10, 12, 0.05);
}

.model-card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.model-name-id {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--spatial-graphite);
  word-break: break-all;
}

.model-badge-provider {
  font-size: 0.72rem;
  font-weight: 600;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.15);
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
  align-self: flex-start;
  margin-top: 2px;
}

.model-speed-row {
  display: flex;
  align-items: center;
}

.model-speed-pill-new {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  font-size: 0.72rem;
  font-weight: 700;
  border-radius: 99px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(148, 163, 184, 0.05);
  color: var(--spatial-silver);
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 24px;
}

.model-speed-pill-new.testing {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  border-color: rgba(59, 130, 246, 0.15);
}

.model-speed-pill-new.success {
  color: #10b981;
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.model-speed-pill-new.error {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.model-speed-error-msg {
  font-size: 0.7rem;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.04);
  padding: 6px;
  border-radius: 6px;
  border: 1px solid rgba(239, 68, 68, 0.1);
  word-break: break-all;
}

.scene-assignments-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-top: 1px solid var(--spatial-line);
  padding-top: 12px;
  margin-top: 4px;
}

.assign-label-tag {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--spatial-gray);
  margin-bottom: 6px;
}

.checkbox-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 10px;
}

.scene-checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.78rem;
  color: var(--spatial-silver);
  cursor: pointer;
  user-select: none;
}

.scene-checkbox-label.checked {
  color: var(--spatial-graphite);
  font-weight: 600;
}

.scene-checkbox-label.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Custom Checkbox Design matching screenshot */
.scene-checkbox-label input[type="checkbox"] {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 2px solid rgba(148, 163, 184, 0.4);
  background: rgba(15, 23, 42, 0.2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  cursor: pointer;
  transition: all 0.2s ease;
  outline: none;
  margin: 0;
  flex-shrink: 0;
}

.scene-checkbox-label input[type="checkbox"]:checked {
  background: #3b82f6;
  border-color: #3b82f6;
}

.scene-checkbox-label input[type="checkbox"]:checked::after {
  content: "";
  position: absolute;
  width: 4px;
  height: 8px;
  border: 2px solid #ffffff;
  border-left: 0;
  border-top: 0;
  transform: rotate(45deg);
  top: 1px;
  left: 4px;
}

/* Pool Quick Access Styles */
.pools-quick-access {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.quick-access-title {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--spatial-silver);
  white-space: nowrap;
}

.quick-access-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.pool-quick-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid var(--spatial-line);
  background: var(--spatial-surface);
  color: var(--spatial-gray);
  cursor: pointer;
  transition: all 0.2s ease;
}

.pool-quick-badge:hover {
  border-color: var(--spatial-accent);
  color: var(--spatial-graphite);
  transform: translateY(-1px);
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
}

.pool-quick-badge strong {
  font-size: 0.78rem;
  font-weight: 700;
}

.badge-count {
  font-size: 0.72rem;
  background: var(--spatial-line);
  color: var(--spatial-graphite);
  padding: 1px 6px;
  border-radius: 10px;
  font-weight: 700;
}

/* All Scene Pools Modal Styles */
.all-pools-modal-card {
  max-width: 800px;
  width: 95%;
}

.all-pools-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 6px;
}

.scene-pool-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.scene-pool-section .section-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.scene-pool-section .bullet-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--spatial-accent);
}

.scene-pool-section .section-title strong {
  font-size: 0.95rem;
  color: var(--spatial-graphite);
}

.scene-pool-section .count-tag {
  font-size: 0.75rem;
  color: var(--spatial-silver);
}

.table-wrapper {
  border-radius: 12px;
  border: 1px solid var(--spatial-line);
  overflow: hidden;
  background: var(--spatial-surface-2);
}

.scene-pool-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.scene-pool-table th,
.scene-pool-table td {
  padding: 10px 14px;
  font-size: 0.8rem;
}

.scene-pool-table th {
  background: var(--spatial-warm-2);
  color: var(--spatial-silver);
  font-weight: 700;
  border-bottom: 1px solid var(--spatial-line);
}

.scene-pool-table td {
  color: var(--spatial-graphite);
  border-bottom: 1px solid var(--spatial-line);
}

.scene-pool-table tr:last-child td {
  border-bottom: 0;
}

.empty-row {
  text-align: center;
  color: var(--spatial-silver);
  padding: 20px 0 !important;
}

.model-code-id {
  font-family: monospace;
  background: var(--spatial-warm-2);
  color: var(--spatial-gray);
  padding: 2px 6px;
  border-radius: 4px;
}

.latency-text {
  color: #34c759;
  font-weight: 700;
}

.latency-text.error {
  color: #ff3b30;
}

.status-tag {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}

.status-tag.available {
  background: rgba(52, 199, 89, 0.1);
  color: #34c759;
}

.status-tag.failed,
.status-tag.timeout,
.status-tag.auth_error {
  background: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
}

/* --- Collapsible Stats Bar Styles --- */
.compact-stats-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--spatial-warm-2);
  border: 1px solid var(--spatial-line);
  padding: 6px 14px;
  border-radius: 12px;
  font-size: 0.8rem;
  color: var(--spatial-gray);
  backdrop-filter: blur(8px);
}

.compact-stat-item {
  display: flex;
  align-items: center;
}

.compact-icon {
  margin-right: 6px;
  display: flex;
  align-items: center;
}

.compact-icon :deep(svg) {
  width: 14px !important;
  height: 14px !important;
}

.compact-stat-item strong {
  color: var(--spatial-graphite);
  margin-left: 4px;
}

.toggle-stats-btn {
  font-size: 0.78rem !important;
  min-height: 32px;
  padding: 0 12px;
}

/* Slide-Fade transition */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}
.slide-fade-leave-active {
  transition: all 0.25s cubic-bezier(1, 0.5, 0.8, 1);
}
.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-8px);
  opacity: 0;
}

/* Admin AI model routing screen, layout and structural properties */
.admin-page {
  min-height: 100vh;
  padding: 28px 48px 48px;
}

.admin-page::before {
  display: none;
}

.admin-shell {
  max-width: 1500px;
}

.admin-header {
  margin-bottom: 28px;
}

.admin-eyebrow {
  letter-spacing: 0.14em;
}

.admin-header h2 {
  font-size: 2rem;
  font-weight: 850;
  letter-spacing: -0.02em;
}

.admin-stats-grid {
  gap: 24px;
}

.admin-stat-card,
.admin-side-nav,
.providers-column,
.config-card-panel,
.models-grid-panel,
.module-route-table,
.pools-quick-access {
  border-radius: 12px !important;
  box-shadow: none !important;
  backdrop-filter: none !important;
}

.admin-stat-card {
  min-height: 128px;
}

.stat-icon {
  border-radius: 12px;
  box-shadow: none;
}

:global(html[data-theme="dark"]) .stat-label,
:global(html[data-theme="dark"]) .stat-sub,
:global(html[data-theme="dark"]) .models-pane-header .pane-description,
:global(html[data-theme="dark"]) .quick-access-title,
:global(html[data-theme="dark"]) .form-group-item label,
:global(html[data-theme="dark"]) .provider-url,
:global(html[data-theme="dark"]) .model-badge-provider,
:global(html[data-theme="dark"]) .assign-label-tag,
:global(html[data-theme="dark"]) .scene-checkbox-label,
:global(html[data-theme="dark"]) .count-tag {
  color: #8c98aa !important;
}

:global(html[data-theme="dark"]) .stat-value,
:global(html[data-theme="dark"]) .models-pane-header h3,
:global(html[data-theme="dark"]) .column-header strong,
:global(html[data-theme="dark"]) .provider-name,
:global(html[data-theme="dark"]) .model-name-id,
:global(html[data-theme="dark"]) .scene-checkbox-label.checked,
:global(html[data-theme="dark"]) .module-route-head h4,
:global(html[data-theme="dark"]) .scene-pool-section .section-title strong {
  color: #dfe7f3 !important;
}

.admin-side-nav {
  padding: 14px;
}

.admin-side-tab {
  min-height: 44px;
  border-radius: 8px !important;
}

/* Collapsed active tab style - remove parent background to fix protrusion/ears bug */
.admin-side-nav.collapsed .admin-side-tab,
.admin-side-nav.collapsed .admin-side-tab.active {
  background: transparent !important;
  box-shadow: none !important;
}

:global(html[data-theme="dark"]) .admin-side-tab {
  color: #94a3b8 !important;
}

:global(html[data-theme="dark"]) .admin-side-tab.active {
  background: #668bdd !important;
  color: #edf4ff !important;
}

:global(html[data-theme="dark"]) .admin-side-toggle {
  background: #101722 !important;
  border: 1px solid #1d293a !important;
  color: #94a3b8 !important;
}

.models-pane-header {
  align-items: flex-start;
  margin-bottom: 18px;
}

.models-pane-header h3 {
  font-size: 1.35rem;
  font-weight: 850;
  letter-spacing: -0.01em;
}

.models-pane-header .pane-description {
  max-width: 860px;
  margin-top: 12px;
  line-height: 1.7;
}

:global(html[data-theme="dark"]) .models-pane-header .spatial-btn-accent,
:global(html[data-theme="dark"]) .save-config-btn,
:global(html[data-theme="dark"]) .models-grid-panel .spatial-btn-accent {
  border: none !important;
  background: linear-gradient(135deg, #2563eb, #3b82f6) !important;
  color: #ffffff !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2) !important;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

:global(html[data-theme="dark"]) .models-pane-header .spatial-btn-accent:hover,
:global(html[data-theme="dark"]) .save-config-btn:hover,
:global(html[data-theme="dark"]) .models-grid-panel .spatial-btn-accent:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3) !important;
}

.models-two-col-layout {
  grid-template-columns: minmax(280px, 0.92fr) minmax(520px, 1.6fr);
  gap: 22px;
}

.providers-column,
.config-card-panel,
.models-grid-panel {
  padding: 20px;
}

.providers-column {
  min-height: 470px;
}

:global(html[data-theme="dark"]) .providers-column .column-header,
:global(html[data-theme="dark"]) .config-card-panel .column-header,
:global(html[data-theme="dark"]) .models-grid-panel .column-header {
  border-bottom-color: #1d293a;
}

:global(html[data-theme="dark"]) .add-provider-btn {
  background: #111d2e !important;
  border: 1px solid #294062 !important;
  color: #75a7ff !important;
  border-radius: 8px !important;
}

:global(html[data-theme="dark"]) .provider-item-card {
  background: #131b29 !important;
  border: 1px solid transparent !important;
  border-radius: 8px !important;
  box-shadow: none !important;
}

:global(html[data-theme="dark"]) .model-dashboard-card {
  background: #0d1527 !important;
  border: 1px solid #1f2d47 !important;
  border-radius: 16px !important;
  box-shadow: none !important;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

:global(html[data-theme="dark"]) .provider-item-card.active {
  background: #172236 !important;
  border-color: #3b82f6 !important;
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.24) !important;
}

:global(html[data-theme="dark"]) .provider-item-card:hover {
  border-color: #334664 !important;
  background: #162235 !important;
}

:global(html[data-theme="dark"]) .model-dashboard-card:hover {
  transform: translateY(-2px);
  border-color: #2e3e5c !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25) !important;
}

:global(html[data-theme="dark"]) .spatial-input,
:global(html[data-theme="dark"]) .form-group-item input {
  background: #111927 !important;
  border: 1px solid #26364d !important;
  border-radius: 8px !important;
  color: #dfe7f3 !important;
  box-shadow: none !important;
}

.spatial-input:focus,
.form-group-item input:focus {
  border-color: #668bdd !important;
  box-shadow: 0 0 0 3px rgba(102, 139, 221, 0.14) !important;
}

.model-dashboard-card {
  gap: 14px;
}

.model-badge-provider {
  background: transparent !important;
  border: 0 !important;
  padding: 0 !important;
  color: #6ea1ff !important;
}

:global(html[data-theme="dark"]) .model-speed-pill-new {
  background: rgba(148, 163, 184, 0.05) !important;
  border: 1px solid rgba(148, 163, 184, 0.2) !important;
  border-radius: 99px !important;
  color: #8c98aa !important;
}

:global(html[data-theme="dark"]) .model-speed-pill-new.success,
:global(html[data-theme="dark"]) .latency-text {
  background: rgba(69, 224, 131, 0.06) !important;
  border: 1px solid rgba(69, 224, 131, 0.25) !important;
  color: #45e083 !important;
}

:global(html[data-theme="dark"]) .model-speed-pill-new.error,
:global(html[data-theme="dark"]) .latency-text.error {
  background: rgba(255, 107, 107, 0.06) !important;
  border: 1px solid rgba(255, 107, 107, 0.25) !important;
  color: #ff6b6b !important;
}

:global(html[data-theme="dark"]) .model-speed-pill-new.testing {
  background: rgba(59, 130, 246, 0.06) !important;
  border: 1px solid rgba(59, 130, 246, 0.25) !important;
  color: #3b82f6 !important;
}

:global(html[data-theme="dark"]) .module-route-table strong {
  color: #45e083 !important;
}

:global(html[data-theme="dark"]) .module-route-table strong.error {
  color: #ff6b6b !important;
}

:global(html[data-theme="dark"]) .scene-assignments-grid {
  border-top-color: #1f2d47 !important;
}

:global(html[data-theme="dark"]) .scene-checkbox-label input[type="checkbox"] {
  border-color: #334155 !important;
  background: #0f172a !important;
}

:global(html[data-theme="dark"]) .scene-checkbox-label input[type="checkbox"]:checked {
  background: #3b82f6 !important;
  border-color: #3b82f6 !important;
}

.module-route-table {
  margin-top: 22px;
  padding: 20px;
}

.module-route-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.module-route-head h4 {
  margin: 0;
  font-size: 1.05rem;
}

.module-route-head span {
  color: #8c98aa;
  font-size: 0.82rem;
  font-weight: 700;
}

.module-route-table-wrap {
  overflow: auto;
  border: 1px solid #273449;
  border-radius: 8px;
}

.module-route-table table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

.module-route-table th,
.module-route-table td {
  padding: 13px 14px;
  border-bottom: 1px solid #273449;
  color: #cbd5e1;
  text-align: left;
  white-space: nowrap;
}

.module-route-table th {
  background: #172236;
  color: #9daabe;
  font-size: 0.82rem;
}

.module-route-table code,
.scene-pool-table code,
.model-code-id {
  background: transparent !important;
  color: #dbe4f0 !important;
  font-weight: 800;
}

.admin-modal-overlay {
  background: rgba(2, 6, 12, 0.64) !important;
  backdrop-filter: blur(10px);
}

.all-pools-modal-card {
  max-width: 820px;
  background: #101722 !important;
  border: 1px solid #22314a !important;
  border-radius: 16px !important;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.36) !important;
}

.all-pools-modal-card .modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #273449;
  padding-bottom: 16px;
  margin-bottom: 18px;
}

.all-pools-modal-card h5 {
  color: #e5edf8 !important;
  margin: 0;
}

.all-pools-modal-card .modal-close {
  width: 34px;
  height: 34px;
  display: inline-grid;
  place-items: center;
  border: 0 !important;
  border-radius: 8px !important;
  background: transparent !important;
  color: #94a3b8 !important;
  font-size: 1.6rem !important;
  line-height: 1 !important;
}

.all-pools-container {
  max-height: 72vh;
}

.scene-pool-section .bullet-dot {
  background: #6ea1ff;
}

.scene-pool-section .section-title strong {
  color: #6ea1ff !important;
}

.table-wrapper {
  border: 1px solid #273449;
  background: #101722;
  border-radius: 8px;
}

.scene-pool-table {
  min-width: 620px;
}

.scene-pool-table th,
.scene-pool-table td {
  border-bottom-color: #273449;
  color: #cbd5e1;
}

.scene-pool-table th {
  background: #172236;
  color: #9daabe;
}

.scene-pool-table td {
  background: #101722;
  color: #cbd5e1;
}

.status-tag.available {
  background: transparent;
  color: #45e083;
}

:global(html[data-theme="dark"]) .pool-quick-badge {
  border-color: #1d293a !important;
  background: #172236 !important;
  color: #cbd5e1 !important;
}

:global(html[data-theme="dark"]) .pool-quick-badge:hover {
  border-color: #3b82f6 !important;
  background: #1e293b !important;
  color: #ffffff !important;
}

:global(html[data-theme="dark"]) .pool-quick-badge .badge-count {
  background: rgba(59, 130, 246, 0.2) !important;
  color: #75a7ff !important;
}

@media (max-width: 990px) {
  .admin-page {
    padding: 24px 18px 32px;
  }
}
</style>
