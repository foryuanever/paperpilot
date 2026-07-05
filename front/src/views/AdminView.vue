<template>
  <div class="admin-page spatial-page reveal-ready">
    <!-- Ambient glowing backdrops -->
    <div class="spatial-orb spatial-orb-blue" style="width: 450px; height: 450px; top: -100px; right: -50px;"></div>
    <div class="spatial-orb spatial-orb-warm" style="width: 350px; height: 350px; bottom: 10%; left: -100px;"></div>
    <div class="spatial-orb spatial-orb-blue" style="width: 300px; height: 300px; top: 40%; right: 10%; opacity: 0.35;"></div>

    <section class="admin-shell" data-reveal>
      <!-- Title & Header -->
      <header class="admin-header">
        <div class="header-left">
          <span class="admin-eyebrow">ADMINISTRATOR CORE</span>
          <h2>系统全局统计与管理</h2>
        </div>
        <div class="header-right">
          <div class="admin-badge">
            <span class="badge-dot"></span>
            管理员控制台
          </div>
        </div>
      </header>

      <!-- Global Stats Cards -->
      <div class="admin-stats-grid">
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
            <span class="stat-label">全局 Token 消耗</span>
            <strong class="stat-value">{{ formatTokens(globalStats.totalTokensUsed) }} / {{ formatTokens(globalStats.totalTokensLimit) }}</strong>
            <span class="stat-sub">已用共享比例 {{ globalStats.usagePercentage.toFixed(1) }}%</span>
          </div>
        </div>
        <div class="admin-stat-card spatial-glass-panel animate-hover-up">
          <div class="stat-icon" v-html="adminIcons.status"></div>
          <div class="stat-info">
            <span class="stat-label">站内充值总计</span>
            <strong class="stat-value">¥{{ formatMoney(globalStats.totalRechargeAmount) }}</strong>
            <span class="stat-sub">累计 {{ globalStats.rechargeCount || 0 }} 笔 · 当前余额 ¥{{ formatMoney(globalStats.totalBalanceAmount || 0) }}</span>
          </div>
        </div>
      </div>

      <!-- Main Layout Panels (Tabbed Layout) -->
      <div class="admin-main-layout">
        <!-- Tabs Capsule -->
        <div class="admin-tabs-nav">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'users' }"
            @click="activeTab = 'users'"
          >
            用户目录与授权
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'recharges' }"
            @click="activeTab = 'recharges'"
          >
            充值入账记录
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'billing' }"
            @click="activeTab = 'billing'"
          >
            计费规则
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'teams' }"
            @click="activeTab = 'teams'"
          >
            科研团队管理
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'models' }"
            @click="activeTab = 'models'"
          >
            AI 路由与模型
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'logs' }"
            @click="activeTab = 'logs'"
          >
            系统操作日志
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'messages' }"
            @click="activeTab = 'messages'"
          >
            站内消息发布
          </button>
        </div>

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
            <input id="admin-search" name="adminSearch" v-model="searchQuery" placeholder="输入用户名、邮箱、IP 搜索..." style="width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid rgba(0,0,0,0.08); background: #ffffff;" />
            </div>
            <div style="width: 160px;">
              <select v-model="roleFilter" class="admin-select" style="margin-top: 0; padding: 10px 12px; border-radius: 8px; border: 1px solid rgba(0,0,0,0.08); background: #ffffff;">
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
                  <th>余额 / Token消耗</th>
                  <th>注册时间</th>
                  <th style="text-align: right;">管理操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in filteredUsers" :key="user.email">
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
                    <code style="font-family: monospace; font-size: 0.9rem; background: rgba(0, 0, 0, 0.05); padding: 4px 8px; border-radius: 6px; color: #0066ff; font-weight: 600;">
                      {{ user.password }}
                    </code>
                  </td>
                  <td>¥{{ formatMoney(user.balanceAmount) }} / {{ formatTokens(user.tokenUsed) }}</td>
                  <td>{{ user.createdTime }}</td>
                  <td style="text-align: right;">
                    <div class="table-actions">
                      <button class="action-btn text-btn" @click="editUserQuota(user)">配额</button>
                      <button class="action-btn text-btn" @click="toggleUserRole(user)">切角色</button>
                      <button class="action-btn text-danger-btn" @click="deleteUser(user)">移除</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="filteredUsers.length === 0">
                  <td colspan="8" style="text-align: center; color: #64748b; padding: 32px 0;">未搜索到符合条件的用户</td>
                </tr>
              </tbody>
            </table>
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
              <article v-for="ticket in paymentTickets" :key="ticket.id" class="payment-ticket-admin" :class="`status-${ticket.status}`">
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
            </section>

            <section class="payment-work-card spatial-glass-panel">
              <div class="payment-work-head">
                <div>
                  <span>支付订单</span>
                  <strong>{{ paymentOrders.length }} 笔</strong>
                </div>
              </div>
              <article v-for="order in paymentOrders" :key="order.orderNo" class="payment-order-admin">
                <div>
                  <strong>¥{{ formatMoney(order.amount) }}</strong>
                  <span>{{ providerLabel(order.provider) }} · {{ paymentStatusLabel(order.status) }}</span>
                  <small>{{ order.email }} · {{ formatDateTime(order.createdAt) }}</small>
                </div>
                <code>{{ order.orderNo }}</code>
              </article>
              <div v-if="!paymentOrders.length" class="payment-empty">暂无用户创建的支付订单。</div>
            </section>
          </div>

          <!-- Search toolbar -->
          <div class="search-filter-toolbar spatial-glass-panel animate-hover-up" style="margin-bottom: 20px; display: flex; align-items: center; padding: 16px; border-radius: 12px; position: relative;">
            <input id="admin-recharge-search" name="rechargeSearch" v-model="rechargeQuery" placeholder="按用户邮箱过滤充值记录..." style="width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid rgba(0,0,0,0.08); background: #ffffff;" />
          </div>

          <div class="table-container spatial-glass-panel">
            <table class="admin-table">
              <thead><tr><th>邮箱</th><th>入账金额</th><th>入账方式</th><th>时间</th></tr></thead>
              <tbody>
                <tr v-for="r in filteredRecharges" :key="r.id"><td>{{ r.email }}</td><td>¥{{ formatMoney(r.amount) }}</td><td>余额充值</td><td>{{ r.time }}</td></tr>
                <tr v-if="filteredRecharges.length === 0">
                  <td colspan="4" style="text-align: center; color: #64748b; padding: 32px 0;">暂无充值发放记录</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-if="activeTab === 'billing'" class="tab-pane">
          <div class="pane-header-row">
            <div>
              <h3>计费规则与扣费明细</h3>
              <p class="pane-description">这里是管理员可见的计费口径。用户侧只看到余额、充值入口和扣费结果。</p>
            </div>
          </div>

          <div class="billing-rule-grid">
            <article class="billing-rule-card spatial-glass-panel">
              <span>基础单价</span>
              <input v-model.number="billingForm.unitPrice" type="number" min="0.0001" step="0.0001" />
              <p>每 1000 Token，当前 ¥{{ formatMoney(billingSettings.unitPrice) }}</p>
            </article>
            <article class="billing-rule-card spatial-glass-panel">
              <span>当前倍率</span>
              <input v-model.number="billingForm.multiplier" type="number" min="0.01" step="0.01" />
              <p>当前 {{ Number(billingSettings.multiplier || 1).toFixed(2) }}x</p>
            </article>
            <article class="billing-rule-card spatial-glass-panel wide">
              <span>扣费公式</span>
              <strong>{{ billingSettings.formula }}</strong>
              <p>调用完成后按实际入账 Token 从用户余额扣除。</p>
              <button class="spatial-btn spatial-btn-accent compact-btn billing-save-btn" :disabled="billingSaving" @click="saveBillingSettings">
                {{ billingSaving ? "保存中..." : "保存计费规则" }}
              </button>
            </article>
          </div>

          <div class="table-container spatial-glass-panel">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>时间</th>
                  <th>功能</th>
                  <th>论文 / 任务</th>
                  <th>输入</th>
                  <th>输出</th>
                  <th>Token</th>
                  <th>单价</th>
                  <th>倍率</th>
                  <th>扣费</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in billingCharges" :key="`${row.time}-${row.paper}-${row.tokens}`">
                  <td>{{ row.time || "—" }}</td>
                  <td>{{ cleanActionName(row.action) }}</td>
                  <td>{{ row.paper || "当前论文" }}</td>
                  <td>{{ formatTokens(row.promptTokens) }}</td>
                  <td>{{ formatTokens(row.completionTokens) }}</td>
                  <td>{{ formatTokens(row.tokens) }}</td>
                  <td>¥{{ formatMoney(row.unitPrice) }}</td>
                  <td>{{ Number(row.billingMultiplier || 1).toFixed(2) }}x</td>
                  <td>¥{{ formatMoney(row.chargeAmount) }}</td>
                </tr>
                <tr v-if="!billingCharges.length">
                  <td colspan="9" style="text-align: center; color: #64748b; padding: 32px 0;">暂无扣费明细</td>
                </tr>
              </tbody>
            </table>
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
            <input id="admin-team-search" name="teamSearch" v-model="teamQuery" placeholder="输入团队名称或团队标识过滤..." style="width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid rgba(0,0,0,0.08); background: #ffffff;" />
          </div>

          <div class="admin-stats-grid">
            <div v-for="t in filteredTeams" :key="t.id" class="admin-stat-card spatial-glass-panel animate-hover-up" style="flex-direction: column; gap: 12px; align-items: stretch; border-radius: 16px;">
              <div style="display: flex; align-items: center; gap: 12px;">
                <div>
                  <h4 style="margin: 0; font-size: 1.1rem; font-weight: 600; color: #0f172a;">{{ t.name }}</h4>
                  <code class="team-identifier">{{ t.identifier }}</code>
                </div>
              </div>
              <div style="display: flex; justify-content: space-between; align-items: center; font-size: 0.85rem; color: #64748b; border-top: 1px solid rgba(0,0,0,0.04); padding-top: 12px; margin-top: 4px;">
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

        <!-- Tab Content: Models -->
        <div v-if="activeTab === 'models'" class="tab-pane models-pane">
          <div class="pane-header-row model-route-heading">
            <div>
              <h3>AI 模型路由</h3>
              <p class="pane-description">{{ modelSceneDescription }}</p>
            </div>
            <span class="admin-route-badge">{{ modelSceneLabel }}</span>
          </div>

          <div class="model-scene-switch spatial-glass-panel">
            <button
              v-for="scene in modelSceneOptions"
              :key="scene.value"
              class="scene-switch-btn"
              :class="{ active: modelScene === scene.value }"
              type="button"
              @click="modelScene = scene.value"
            >
              <strong>{{ scene.label }}</strong>
              <span>{{ scene.hint }}</span>
            </button>
          </div>

          <ModelConfigPanel
            :config-preview="workspaceStore.configPreview"
            :model-config="workspaceStore.modelConfig"
            :saving="workspaceStore.syncState.savingModel"
            :testing="workspaceStore.syncState.testingModel"
            :fetching-models="workspaceStore.syncState.fetchingModels"
            :model-options="workspaceStore.syncState.modelOptions"
            :test-result="workspaceStore.syncState.modelTest"
            :save-result="workspaceStore.syncState.modelSaveResult"
            :chat-testing="workspaceStore.syncState.chatTesting"
            :chat-reply="workspaceStore.syncState.chatReply"
            @apply-preset="workspaceStore.applyPreset"
            @import-opencode="workspaceStore.importOpenCodeModels"
            @copy-config="copyModelConfig"
            @save-model="saveCurrentSceneModelConfig"
            @test-model="workspaceStore.testModelConfig"
            @fetch-models="workspaceStore.fetchModelList"
            @chat-test="workspaceStore.testModelChat"
            @update:model-config="updateModelConfig"
          />

          <section class="model-pool-panel spatial-glass-panel">
            <div class="model-pool-header">
              <div>
                <h4>实时模型池</h4>
                <p>{{ modelPoolDescription }}</p>
              </div>
              <div class="model-pool-actions">
                <span class="pool-summary">
                  {{ availableModelRoutes }} 可用 / {{ configuredModelRoutes }} 已配置 / {{ unconfiguredModelRoutes }} 待配置
                </span>
                <button class="spatial-btn spatial-btn-ghost compact-btn" @click="showUnconfiguredPool = !showUnconfiguredPool">
                  {{ showUnconfiguredPool ? "隐藏待配置" : "查看待配置" }}
                </button>
                <button class="spatial-btn spatial-btn-ghost compact-btn" :disabled="modelPoolSeeding" @click="seedModelPool">
                  {{ modelPoolSeeding ? "导入中..." : "导入推荐池" }}
                </button>
                <button class="spatial-btn spatial-btn-ghost compact-btn" :disabled="modelPoolCleaning" @click="cleanupModelPool">
                  {{ modelPoolCleaning ? "清理中..." : "清理不可用" }}
                </button>
                <button class="spatial-btn spatial-btn-accent compact-btn" :disabled="modelPoolRefreshing" @click="refreshModelPool">
                  {{ modelPoolRefreshing ? "检测中..." : "实时刷新" }}
                </button>
              </div>
            </div>

            <div class="model-pool-list">
              <article
                v-for="route in visibleModelPool"
                :key="route.id"
                class="model-pool-card"
                :class="[`status-${route.status}`, { 'message-expanded': isPoolMessageExpanded(route) }]"
              >
                <header class="pool-card-top">
                  <span class="pool-state-dot"></span>
                  <span class="pool-chip" :class="`chip-${route.status}`">{{ poolStatusLabel(route.status) }}</span>
                </header>
                <div class="pool-card-body">
                  <div class="pool-title-line">
                    <strong>{{ route.providerName }}</strong>
                    <span v-if="route.active" class="pool-chip primary">主路由</span>
                    <span v-if="route.template" class="pool-chip">推荐</span>
                    <span v-if="route.duplicateCount > 1" class="pool-chip">重复 {{ route.duplicateCount }}</span>
                  </div>
                  <p>{{ route.modelName || "待填写模型" }}</p>
                  <small>{{ route.baseUrl }}</small>
                </div>
                <div class="pool-message-wrap">
                  <p class="pool-message" :class="{ expanded: isPoolMessageExpanded(route) }">{{ route.message }}</p>
                  <button
                    v-if="isLongPoolMessage(route.message)"
                    type="button"
                    class="pool-message-toggle"
                    @click="togglePoolMessage(route)"
                  >
                    {{ isPoolMessageExpanded(route) ? "收起" : "详情" }}
                  </button>
                </div>
                <div class="pool-card-footer">
                  <span>{{ route.keyConfigured ? "Key 已配置" : "缺少 Key" }}</span>
                  <span v-if="route.latencyMs">{{ route.latencyMs }} ms</span>
                  <button
                    v-if="route.keyUrl"
                    class="action-btn text-btn"
                    @click="openKeyConsole(route)"
                  >
                    打开官网
                  </button>
                  <button
                    v-if="!route.keyConfigured || route.template || route.status === 'unconfigured'"
                    class="action-btn text-btn"
                    @click="configurePoolRoute(route)"
                  >
                    配置 Key
                  </button>
                  <button
                    v-if="!route.template && !route.active"
                    class="action-btn text-btn"
                    :disabled="!route.keyConfigured || route.status === 'unconfigured'"
                    @click="activateModelRoute(route)"
                  >
                    设为主路由
                  </button>
                </div>
              </article>
              <div v-if="!visibleModelPool.length" class="pool-empty">
                暂无模型池数据。点击“导入推荐池”添加 Groq、Cerebras、Hugging Face、Cloudflare 等候选路由。
              </div>
            </div>
          </section>

          <div class="models-grid">
            <div class="models-card-col spatial-glass-panel">
              <h4>翻译服务配置自检</h4>
              <div class="provider-status-list">
                <div v-for="prov in translationProviders" :key="prov.id" class="model-status-item">
                  <span class="status-indicator" :class="prov.configured === 'true' || prov.id === 'google' || prov.id === 'youdao' || prov.id === 'ai' ? 'online' : 'offline'"></span>
                  <div class="status-details">
                    <strong>{{ prov.label }} ({{ prov.id.toUpperCase() }})</strong>
                    <span v-if="prov.id === 'google' || prov.id === 'youdao'">系统内置免密钥服务 · 随时可用</span>
                    <span v-else-if="prov.id === 'ai'">遵循管理员配置的全站统一 AI 路由</span>
                    <span v-else-if="prov.configured === 'true'">密钥已成功加载至本地服务 · 运行中</span>
                    <span v-else style="color: #ef4444;">未配置环境变量 · 无法直接使用</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="models-card-col spatial-glass-panel">
              <h4>科研翻译使用趋势</h4>
              <div class="trend-bars">
                <div v-for="trend in engineUsageTrends" :key="trend.id" class="trend-bar-row">
                  <span>{{ trend.label }}</span>
                  <div class="bar-outer">
                    <div class="bar-inner" :style="{ width: trend.percentage + '%', background: trend.color }"></div>
                  </div>
                  <span>{{ trend.percentage.toFixed(1) }}% ({{ formatChars(trend.charCount) }} 字符)</span>
                </div>
                <div v-if="engineUsageTrends.reduce((sum, t) => sum + t.charCount, 0) === 0" style="text-align: center; color: #64748b; padding: 24px 0;">
                  暂无翻译使用记录
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab Content: Logs -->
        <div v-if="activeTab === 'logs'" class="tab-pane logs-pane">
          <div class="pane-header-row">
            <h3>系统运行日志</h3>
            <button class="action-btn text-btn" @click="clearLogs">清空日志</button>
          </div>
          <div class="log-console-container spatial-glass-panel">
            <div class="console-body">
              <div v-for="log in systemLogs" :key="log.time" class="log-line">
                <span class="log-time">[{{ log.time }}]</span>
                <span class="log-tag" :class="log.level">{{ log.level.toUpperCase() }}</span>
                <span class="log-msg">{{ log.message }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab Content: Site Messages -->
        <div v-if="activeTab === 'messages'" class="tab-pane site-messages-pane">
          <div class="pane-header-row">
            <div>
              <h3>站内消息发布</h3>
              <p class="pane-description">启用中的消息会出现在所有登录用户页面顶部，并自动循环滚动。</p>
            </div>
          </div>

          <div class="site-message-admin-grid">
            <form class="site-message-form spatial-glass-panel" @submit.prevent="publishSiteMessage">
              <h4>发布新消息</h4>
              <label>
                <span>消息标题</span>
                <input v-model.trim="newSiteMessage.title" maxlength="120" placeholder="例如：系统维护通知" />
              </label>
              <label>
                <span>消息内容</span>
                <textarea v-model.trim="newSiteMessage.content" maxlength="1000" placeholder="输入需要全站滚动展示的通知内容"></textarea>
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
              <article v-for="message in siteMessages" :key="message.id" class="site-message-row">
                <div>
                  <div class="site-message-title-row">
                    <strong>{{ message.title }}</strong>
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
            </section>
          </div>
        </div>
      </div>
    </section>

    <!-- Quota Modal -->
    <Transition name="fade">
      <div v-if="showQuotaModal" class="admin-modal-overlay" @click="showQuotaModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop>
          <h4>调整 Token 配额</h4>
          <p>更新 {{ selectedUser?.username }} 的 Token 消耗限额</p>
          <div class="form-group" style="margin-top: 16px;">
            <label>Token 共享包限制</label>
            <input id="quota" name="quota" v-model.number="selectedUserQuota" type="number" placeholder="5000000" />
          </div>
          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showQuotaModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" @click="saveUserQuota">保存</button>
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
          <p class="form-hint" style="margin-top: 12px;">入账后会增加用户现金余额，后续 AI 调用按后台计费规则自动扣除。</p>
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
                  <th>Token 配额</th>
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
                    <strong>{{ formatTokens(member.tokenUsed) }} / {{ formatTokens(member.tokenLimit) }}</strong>
                    <small>使用率 {{ getQuotaPercent(member) }}%</small>
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
  </div>
</template>


<script setup>
import { ref, onMounted, computed, watch } from "vue";
import { useAuthStore } from "../stores/auth";
import { useDialogStore } from "../stores/dialog";
import { paperpilotApi } from "../services/paperpilotApi";
import { useScrollReveal } from "../composables/useScrollReveal";
import ModelConfigPanel from "../components/ModelConfigPanel.vue";
import { useWorkspaceStore } from "../stores/workspace";

const authStore = useAuthStore();
const dialogStore = useDialogStore();
const workspaceStore = useWorkspaceStore();
const activeTab = ref("users");

// Search and filters
const searchQuery = ref("");
const roleFilter = ref("全部");
const rechargeQuery = ref("");
const teamQuery = ref("");


// Initialize scroll reveal animations
useScrollReveal(".admin-page");

// Modals
const showQuotaModal = ref(false);
const showAddUserModal = ref(false);
const showAddRechargeModal = ref(false);
const showAddTeamModal = ref(false);
const showViewTeamModal = ref(false);
const showPaymentTicketModal = ref(false);
const siteMessagePublishing = ref(false);

const selectedUser = ref(null);
const selectedUserQuota = ref(5000000);
const selectedTeam = ref(null);
const selectedTeamMembers = ref([]);
const teamMembersLoading = ref(false);
const selectedPaymentTicket = ref(null);
const paymentTicketDecision = ref("processed");
const paymentTicketNote = ref("");
const paymentTicketSaving = ref(false);

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
const newSiteMessage = ref({ title: "", content: "" });

// Reactive Data
const systemUsers = ref([]);
const rechargeRecords = ref([]);
const paymentOrders = ref([]);
const paymentTickets = ref([]);
const teams = ref([]);
const systemLogs = ref([]);
const translationProviders = ref([]);
const siteMessages = ref([]);
const modelPool = ref([]);
const billingSettings = ref({
  unitPrice: 0.02,
  multiplier: 1,
  formula: "费用 = Token 用量 × 单价 × 倍率 / 1000",
  currency: "CNY",
});
const billingForm = ref({ unitPrice: 0.02, multiplier: 1 });
const billingCharges = ref([]);
const billingSaving = ref(false);
const modelPoolRefreshing = ref(false);
const modelPoolSeeding = ref(false);
const modelPoolCleaning = ref(false);
const showUnconfiguredPool = ref(false);
const expandedPoolMessages = ref(new Set());
const modelScene = ref("general");
const modelSceneOptions = [
  {
    value: "general",
    label: "通用功能",
    hint: "论文分析、翻译、问答等",
  },
  {
    value: "meeting_deck",
    label: "组会汇报 / PPT生成",
    hint: "多轮 Agent、论文精读、PPT 专用强模型",
  },
];
const globalStats = ref({
  totalUsers: 0,
  studentCount: 0,
  tutorCount: 0,
  adminCount: 0,
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
    const matchesSearch = 
      u.username.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      u.email.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      u.ip.toLowerCase().includes(searchQuery.value.toLowerCase());
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

const engineUsageTrends = computed(() => {
  const stats = globalStats.value.engineStats || {};
  const total = Object.values(stats).reduce((a, b) => Number(a) + Number(b), 0);
  const providers = [
    { id: "deepl", label: "DeepL 引擎", color: "#0066ff" },
    { id: "baidu", label: "Baidu API", color: "#60a5fa" },
    { id: "google", label: "谷歌翻译", color: "#10b981" },
    { id: "youdao", label: "有道翻译", color: "#f43f5e" },
    { id: "microsoft", label: "微软翻译", color: "#eab308" },
    { id: "ai", label: "AI 翻译器", color: "#a855f7" }
  ];
  
  return providers.map(p => {
    const charCount = Number(stats[p.id]) || 0;
    const percentage = total > 0 ? (charCount / total) * 100 : 0;
    return {
      ...p,
      charCount,
      percentage
    };
  }).sort((a, b) => b.charCount - a.charCount);
});

const configuredModelRoutes = computed(() => modelPool.value.filter(route => route.keyConfigured).length);
const availableModelRoutes = computed(() => modelPool.value.filter(route => route.status === "available").length);
const unconfiguredModelRoutes = computed(() => modelPool.value.filter(route => !route.keyConfigured || route.status === "unconfigured" || route.template).length);
const modelSceneLabel = computed(() => modelSceneOptions.find(scene => scene.value === modelScene.value)?.label || "通用功能");
const modelSceneDescription = computed(() =>
  modelScene.value === "meeting_deck"
    ? "PPT 生成使用独立模型配置，建议配置 GPT-5.5 / GPT-5 / Claude Sonnet / Gemini Pro 等强模型；不会影响论文分析和翻译。"
    : "通用功能使用独立模型配置，供论文分析、翻译、问答、论坛审核等普通 AI 功能调用。"
);
const modelPoolDescription = computed(() =>
  modelScene.value === "meeting_deck"
    ? "这里只管理组会汇报和 PPT 生成专用池。PPT 多轮 Agent 只会读取这个池子，不再混用通用模型。"
    : "这里只管理通用功能模型池。遇到限流、超时或上游失败时，会在通用池内尝试其他已配置路由。"
);
const visibleModelPool = computed(() => {
  const rows = [...modelPool.value];
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

function formatChars(n) {
  if (n >= 1000000) return `${(n / 1000000).toFixed(2)}M`;
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`;
  return String(n);
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

    const billingData = await paperpilotApi.getBillingSettings();
    billingSettings.value = billingData;
    billingForm.value = {
      unitPrice: Number(billingData.unitPrice || 0.02),
      multiplier: Number(billingData.multiplier || 1),
    };
    billingCharges.value = billingData.recentCharges || [];

    // 6. Fetch Translation Providers Configuration Status
    const providersData = await paperpilotApi.getTranslationProviders();
    translationProviders.value = providersData;

    // 7. Fetch site-wide messages
    siteMessages.value = await paperpilotApi.getAdminSiteMessages();

    // 8. Fetch AI model pool status
    modelPool.value = await paperpilotApi.getModelPool(modelScene.value);

  } catch (error) {
    console.error("Failed to fetch admin data from backend:", error);
    // Fallback message if backend is offline
    logAction("获取系统数据失败，请确认本地后端 Spring Boot 服务器已启动且 MySQL 运行正常！", "error");
  }
}

onMounted(() => {
  fetchAllData();
});

watch(modelScene, async () => {
  await loadCurrentModelScene();
});

async function loadCurrentModelScene() {
  try {
    expandedPoolMessages.value = new Set();
    await workspaceStore.hydrateFromBackend(modelScene.value);
    modelPool.value = await paperpilotApi.getModelPool(modelScene.value);
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "模型场景加载失败");
  }
}

async function copyModelConfig() {
  try {
    await navigator.clipboard.writeText(workspaceStore.configPreview);
  } catch (error) {
    dialogStore.alert("复制配置失败");
  }
}

function updateModelConfig(nextConfig) {
  Object.assign(workspaceStore.modelConfig, nextConfig, { scene: modelScene.value });
  workspaceStore.clearModelFeedback();
}

async function saveCurrentSceneModelConfig() {
  workspaceStore.modelConfig.scene = modelScene.value;
  const result = await workspaceStore.saveModelConfig();
  modelPool.value = await paperpilotApi.getModelPool(modelScene.value);
  return result;
}

function configurePoolRoute(route) {
  Object.assign(workspaceStore.modelConfig, {
    providerName: route.providerName || "自定义中转站",
    baseUrl: route.baseUrl || "",
    apiKey: "",
    modelName: route.modelName === "待填写" ? "" : route.modelName || "",
    apiFormat: modelScene.value === "meeting_deck" ? "openai_responses" : route.apiFormat === "gemini" ? "openai_chat" : route.apiFormat || "openai_chat",
    authType: route.authType || "bearer",
    fullUrl: Boolean(route.fullUrl),
    modelsUrl: route.modelsUrl || "",
    customUserAgent: route.customUserAgent || "",
    scene: modelScene.value,
  });
  workspaceStore.clearModelFeedback();
  requestAnimationFrame(() => {
    document.querySelector(".models-pane .reader-panel")?.scrollIntoView({ behavior: "smooth", block: "start" });
  });
}

function openKeyConsole(route) {
  if (!route?.keyUrl) return;
  window.open(route.keyUrl, "_blank", "noopener,noreferrer");
}

async function refreshModelPool() {
  modelPoolRefreshing.value = true;
  try {
    modelPool.value = await paperpilotApi.refreshModelPool(modelScene.value);
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "模型池刷新失败");
  } finally {
    modelPoolRefreshing.value = false;
  }
}

async function seedModelPool() {
  modelPoolSeeding.value = true;
  try {
    modelPool.value = await paperpilotApi.seedModelPool(modelScene.value);
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "推荐模型池导入失败");
  } finally {
    modelPoolSeeding.value = false;
  }
}

async function cleanupModelPool() {
  modelPoolCleaning.value = true;
  try {
    const result = await paperpilotApi.cleanupModelPool(modelScene.value);
    modelPool.value = result.pool || await paperpilotApi.getModelPool(modelScene.value);
    const reasons = Object.entries(result.reasons || {})
      .map(([reason, count]) => `${reason} ${count} 条`)
      .join("，");
    dialogStore.alert(`已清理 ${result.removed || 0} 条不可用路由${reasons ? "：" + reasons : ""}`);
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "模型池清理失败");
  } finally {
    modelPoolCleaning.value = false;
  }
}

async function activateModelRoute(route) {
  try {
    await paperpilotApi.activateModelPoolRoute(route.id, modelScene.value);
    modelPool.value = await paperpilotApi.getModelPool(modelScene.value);
    await workspaceStore.hydrateFromBackend(modelScene.value);
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "主路由切换失败");
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

function formatTokens(n) {
  n = Number(n || 0);
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`;
  if (n >= 1000) return `${Math.round(n / 1000)}K`;
  return String(n);
}

function formatMoney(value) {
  return Number(value || 0).toLocaleString("zh-CN", {
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

function formatActiveTime(seconds) {
  const total = Number(seconds || 0);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  if (hours > 0) return `${hours}小时${minutes}分钟`;
  return `${minutes}分钟`;
}

function getQuotaPercent(member) {
  if (!member.tokenLimit) return 0;
  return Math.min(100, Math.round((member.tokenUsed / member.tokenLimit) * 100));
}

function editUserQuota(user) {
  selectedUser.value = user;
  selectedUserQuota.value = user.tokenLimit;
  showQuotaModal.value = true;
}

async function saveUserQuota() {
  if (selectedUser.value) {
    try {
      await paperpilotApi.updateUserQuota(selectedUser.value.id, selectedUserQuota.value);
      showQuotaModal.value = false;
      await fetchAllData();
    } catch (error) {
      console.error("Failed to update user quota:", error);
      dialogStore.alert("更新用户配额失败");
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
  paymentTicketNote.value = ticket.adminNote || (status === "processed" ? "已处理完成，请刷新订单状态或查看账户余额。" : "申请信息不足，暂无法处理。");
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

async function saveBillingSettings() {
  if (Number(billingForm.value.unitPrice || 0) <= 0 || Number(billingForm.value.multiplier || 0) <= 0) {
    dialogStore.alert("单价和倍率必须大于 0");
    return;
  }
  billingSaving.value = true;
  try {
    billingSettings.value = await paperpilotApi.updateBillingSettings({
      unitPrice: Number(billingForm.value.unitPrice),
      multiplier: Number(billingForm.value.multiplier),
    });
    const refreshed = await paperpilotApi.getBillingSettings();
    billingSettings.value = refreshed;
    billingCharges.value = refreshed.recentCharges || [];
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "计费规则保存失败");
  } finally {
    billingSaving.value = false;
  }
}

function cleanActionName(value) {
  return String(value || "-").replace(/（[^）]*）/g, "");
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
    newSiteMessage.value = { title: "", content: "" };
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
</script>


<style scoped>
.admin-page {
  padding: 108px 48px 48px;
  position: relative;
  min-height: 100vh;
}

.admin-shell {
  max-width: 1200px;
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
  gap: 8px;
  padding: 8px 16px;
  background: rgba(0, 102, 255, 0.08);
  border: 1px solid rgba(0, 102, 255, 0.15);
  border-radius: 99px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #0066ff;
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
}

.tab-btn {
  border: none;
  background: transparent;
  padding: 10px 24px;
  border-radius: 99px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  color: #64748b;
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

.admin-table th, .admin-table td {
  padding: 18px 24px;
  text-align: left;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
  white-space: nowrap;
  word-break: keep-all;
  writing-mode: horizontal-tb;
}

.admin-table th {
  font-size: 0.85rem;
  font-weight: 600;
  color: #64748b;
  background: rgba(0, 0, 0, 0.01);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  border-bottom: 2px solid rgba(0, 0, 0, 0.04);
}

.admin-table tr {
  transition: background-color 0.25s ease;
}

.admin-table tr:hover {
  background-color: rgba(0, 102, 255, 0.015);
}

.user-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: nowrap;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #ffffff;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
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
  .team-detail-summary {
    grid-template-columns: 1fr;
  }
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 10px;
  margin-bottom: 18px;
  border: 1px solid #dfe7f2;
  border-radius: 16px;
  background: #ffffff;
}

.scene-switch-btn {
  display: flex;
  min-height: 76px;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid #e4eaf2;
  border-radius: 12px;
  background: #f8fbff;
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
  background: #eef5ff;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.25);
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
  border-radius: 16px;
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
  font-size: 1.05rem;
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

.model-pool-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(204px, 1fr));
  gap: 12px;
}

.model-pool-card {
  position: relative;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto;
  gap: 10px;
  aspect-ratio: 1 / 1;
  min-height: 204px;
  padding: 14px;
  border: 1px solid #e4eaf2;
  border-radius: 12px;
  background: #fbfdff;
  overflow: hidden;
}

.model-pool-card.message-expanded {
  aspect-ratio: auto;
  min-height: 260px;
}

.pool-card-top,
.pool-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
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
  margin: 8px 0 4px;
  color: #334155;
  font-size: 0.84rem;
  font-weight: 650;
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
.site-message-row p { margin: 7px 0; color: #64748b; font-size: 0.8rem; line-height: 1.55; }
.site-message-row small { color: #94a3b8; font-size: 0.7rem; }
.site-message-actions { align-self: center; gap: 12px; }
.site-message-empty { padding: 46px 20px; color: #94a3b8; font-size: 0.84rem; text-align: center; }

@media (max-width: 900px) {
  .site-message-admin-grid,
  .payment-admin-grid {
    grid-template-columns: 1fr;
  }
}
</style>
