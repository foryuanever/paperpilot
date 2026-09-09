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
              <span class="stat-sub">普通用户 {{ globalStats.ordinaryCount }} / 管理员 {{ globalStats.adminCount }}</span>
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
            <div style="display: flex; gap: 8px;">
              <button class="spatial-btn spatial-btn-ghost compact-btn" style="color: #4f46e5; border-color: rgba(79, 70, 229, 0.2);" @click="triggerRestoreAllQuotas">
                恢复全体当前会员额度
              </button>
              <button class="spatial-btn spatial-btn-ghost compact-btn danger-lite" @click="triggerResetAllUsers">
                重置全体额度
              </button>
              <button class="spatial-btn spatial-btn-accent compact-btn" @click="showAddUserModal = true">
                添加系统用户
              </button>
            </div>
          </div>

          <!-- Search & filter toolbar -->
          <div class="search-filter-toolbar spatial-glass-panel animate-hover-up" style="margin-bottom: 20px; display: flex; gap: 16px; padding: 16px; align-items: center; border-radius: 12px;">
            <div style="flex: 1; position: relative; display: flex; align-items: center;">
            <input id="admin-search" name="adminSearch" v-model="searchQuery" placeholder="输入用户名、邮箱、IP 搜索..." style="width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--spatial-line); background: var(--spatial-surface); color: var(--spatial-graphite);" />
            </div>
            <div style="width: 160px;">
              <select v-model="roleFilter" class="admin-select" style="margin-top: 0; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--spatial-line); background: var(--spatial-surface); color: var(--spatial-graphite);">
                <option value="全部">所有角色</option>
                <option value="普通用户">普通用户</option>
                <option value="管理员">管理员</option>
              </select>
            </div>
            <button class="spatial-btn spatial-btn-accent" :disabled="selectedUserIds.size === 0" @click="openBatchQuotaModal">
              批量调整额度<span v-if="selectedUserIds.size">（{{ selectedUserIds.size }}）</span>
            </button>
          </div>
          <div class="table-container spatial-glass-panel">
            <table class="admin-table">
              <thead>
                <tr>
                  <th style="width: 38px;"><input type="checkbox" :checked="allCurrentPageUsersSelected" @change="toggleCurrentPageUsers($event.target.checked)" aria-label="选择当前页用户" /></th>
                  <th>用户名称</th>
                  <th>唯一 ID</th>
                  <th>IP 地址</th>
                  <th>当前角色</th>
                  <th>会员套餐</th>
                  <th>周期 / 到期</th>
                  <th>权益使用</th>
                  <th>注册时间</th>
                  <th>是否在线</th>
                  <th style="text-align: right;">管理操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in paginatedUsers" :key="user.id">
                  <td><input type="checkbox" :checked="selectedUserIds.has(user.id)" @change="toggleUserSelection(user.id, $event.target.checked)" :aria-label="`选择 ${user.username}`" /></td>
                  <td>
                    <div class="user-name-cell">
                      <span class="user-avatar" :data-user-id="user.id" :data-user-email="user.email" title="查看个人卡片" :style="{ backgroundColor: getAvatarColor(user.role) }">
                        <img v-if="user.avatarUrl" :src="user.avatarUrl" alt="头像" style="width: 100%; height: 100%; object-fit: cover; border-radius: 50%;" />
                        <template v-else>{{ user.username.slice(0, 1).toUpperCase() }}</template>
                      </span>
                      <strong style="display: inline-flex; align-items: center; gap: 6px;">
                        {{ user.username }}
                        <span v-if="user.qqOpenid || String(user.email || '').startsWith('qq_user_')" class="qq-user-badge" title="QQ快捷登录用户" style="display: inline-flex; align-items: center; background: #e6f7ff; padding: 2px 6px; border-radius: 4px; border: 1px solid #bae7ff; font-size: 10px; color: #0050b3; font-weight: 700; gap: 3px; scale: 0.9;">
                          <svg viewBox="0 0 64 64" width="12" height="12" style="display: block;">
                            <ellipse cx="22" cy="54" rx="8" ry="4" fill="#FFAE00" />
                            <ellipse cx="42" cy="54" rx="8" ry="4" fill="#FFAE00" />
                            <ellipse cx="32" cy="31" rx="20" ry="22" fill="#1C1F21" />
                            <ellipse cx="32" cy="19" rx="14" ry="12" fill="#1C1F21" />
                            <ellipse cx="32" cy="35" rx="15" ry="16" fill="#FFFFFF" />
                            <ellipse cx="32" cy="19" rx="12" ry="10" fill="#FFFFFF" />
                            <circle cx="27" cy="18" r="2.5" fill="#1C1F21" />
                            <circle cx="37" cy="18" r="2.5" fill="#1C1F21" />
                            <path d="M26,22 C26,22 32,27 38,22 C38,20 26,20 26,22 Z" fill="#FFAE00" />
                            <path d="M16,30 Q32,36 48,30 C50,35 46,35 44,35 L48,46 L42,46 L40,35 Q32,37 24,35 L20,46 L16,46 Z" fill="#FF3B30" />
                          </svg>
                          QQ
                        </span>
                      </strong>
                    </div>
                  </td>
                  <td>
                    <code class="model-code-id" style="font-weight: 700; color: var(--c-accent);">{{ user.numericId || '—' }}</code>
                  </td>
                  <td>{{ user.ip || '—' }}</td>
                  <td>
                    <span class="role-badge" :class="getRoleClass(user.role)">
                      {{ user.role }}
                    </span>
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
                    <div style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start; min-width: 140px;">
                      <div style="display: flex; flex-wrap: wrap; gap: 4px; max-width: 180px;">
                        <span class="usage-badge review-tag" title="对照翻译" style="font-size: 0.68rem; padding: 2px 5px;">
                          译: <strong>{{ user.translateUsed || 0 }}</strong>/{{ user.translateQuota || 0 }}
                        </span>
                        <span class="usage-badge ppt-tag" title="沉浸全篇" style="font-size: 0.68rem; padding: 2px 5px;">
                          沉: <strong>{{ user.immersiveUsed || 0 }}</strong>/{{ user.immersiveQuota || 0 }}
                        </span>
                        <span class="usage-badge chat-tag" title="PPT次数" style="font-size: 0.68rem; padding: 2px 5px;">
                          PPT: <strong>{{ user.pptUsed || 0 }}</strong>/{{ user.pptQuota || 0 }}
                        </span>
                        <span class="usage-badge token-tag" title="AI积分" style="font-size: 0.68rem; padding: 2px 5px;">
                          积分: <strong>{{ user.fruitScore || 0 }}</strong>
                        </span>
                      </div>
                      <button class="spatial-btn spatial-btn-ghost compact-btn" style="min-height: 22px; margin-top: 2px; padding: 2px 6px; font-size: 0.68rem; border-color: rgba(99,102,241,0.35); color: var(--c-accent); background: rgba(99,102,241,0.03);" @click="openQuotaDetailModal(user)">
                        🔍 详情与补给
                      </button>
                    </div>
                  </td>
                  <td>{{ user.createdTime }}</td>
                  <td>
                    <span class="user-online-status" :class="user.isOnline ? 'online' : 'offline'">
                      <i aria-hidden="true"></i>{{ user.isOnline ? '在线' : '离线' }}
                    </span>
                  </td>
                  <td style="text-align: right;">
                    <div class="table-actions">
                      <button class="spatial-btn spatial-btn-ghost compact-btn" style="min-height: 28px; padding: 0 10px; font-size: 0.75rem; border-color: rgba(16,185,129,0.35); color: #059669; background: rgba(16,185,129,0.03);" @click="sendUserMessage(user)">发送消息</button>
                      <button class="spatial-btn spatial-btn-accent compact-btn" style="min-height: 28px; padding: 0 10px; font-size: 0.75rem;" @click="editUserMembership(user)">分配会员</button>
                      <button class="spatial-btn spatial-btn-ghost compact-btn" style="min-height: 28px; padding: 0 10px; font-size: 0.75rem;" @click="toggleUserRole(user)">切角色</button>
                      <button class="spatial-btn spatial-btn-ghost compact-btn" style="min-height: 28px; padding: 0 10px; font-size: 0.75rem; border-color: rgba(14,165,233,0.3); color: #0ea5e9; background: rgba(14,165,233,0.02);" @click="loginAsUser(user)">登入</button>
                      <button
                        v-if="!user.banned"
                        class="spatial-btn spatial-btn-ghost compact-btn"
                        style="min-height: 28px; padding: 0 10px; font-size: 0.75rem; border-color: rgba(239,68,68,0.3); color: #ef4444; background: rgba(239,68,68,0.02);"
                        @click="promptBanUser(user)"
                      >
                        封禁
                      </button>
                      <button
                        v-else
                        class="spatial-btn spatial-btn-ghost compact-btn"
                        style="min-height: 28px; padding: 0 10px; font-size: 0.75rem; border-color: rgba(34,197,94,0.3); color: #22c55e; background: rgba(34,197,94,0.02);"
                        @click="executeUnbanUser(user)"
                      >
                        解封
                      </button>
                      <button class="spatial-btn spatial-btn-ghost compact-btn" style="min-height: 28px; padding: 0 10px; font-size: 0.75rem; border-color: rgba(239,68,68,0.2); color: #ef4444; background: rgba(239,68,68,0.02);" @click="deleteUser(user)">移除</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="filteredUsers.length === 0">
                  <td colspan="11" style="text-align: center; color: #64748b; padding: 32px 0;">未搜索到符合条件的用户</td>
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

        <div v-if="activeTab === 'membershipPlans'" class="tab-pane membership-plan-pane">
          <div class="pane-header-row">
            <div>
              <h3>会员套餐管理</h3>
              <p class="pane-description">后台实时调整套餐价格、权益额度与限时秒杀。用户购买页会自动展示原价删除线、优惠价和倒计时。</p>
            </div>
            <div class="membership-admin-actions">
              <button class="spatial-btn spatial-btn-ghost compact-btn" :disabled="membershipPlansLoading" @click="loadMembershipPlans">
                {{ membershipPlansLoading ? "同步中..." : "刷新套餐" }}
              </button>
              <button class="spatial-btn spatial-btn-accent compact-btn" :disabled="creatingMembershipPlan" @click="createMembershipPlan">
                {{ creatingMembershipPlan ? "创建中..." : "+ 上架新套餐" }}
              </button>
            </div>
          </div>

          <div class="membership-admin-summary spatial-glass-panel">
            <article>
              <span>当前套餐</span>
              <strong>{{ subscriptionPlans.length }}</strong>
              <small>数据库可编辑配置</small>
            </article>
            <article>
              <span>秒杀活动</span>
              <strong>{{ activeSeckillCount }}</strong>
              <small>正在展示倒计时</small>
            </article>
            <article>
              <span>计费周期</span>
              <strong>月付</strong>
              <small>当前只开放单月订阅</small>
            </article>
          </div>

          <!-- Subscription Membership Plans -->
          <h4 style="margin: 24px 0 12px 0; font-size: 1.1rem; color: var(--c-accent); font-weight: 800; border-left: 3px solid var(--c-accent); padding-left: 8px;">订阅套餐与权益加油包</h4>
          <div class="membership-plan-admin-grid">
            <article v-for="plan in subscriptionPlans" :key="plan.id" class="membership-plan-admin-card spatial-glass-panel" :class="{ inactive: plan.activeFlag === false, sale: plan.seckillEnabled }">
              <header>
                <div>
                  <span class="plan-admin-id">{{ plan.id }}{{ plan.topUpPack ? " · 加油包" : "" }}</span>
                  <input v-model.trim="plan.name" class="plan-admin-name" placeholder="套餐名称" />
                  <input v-model.trim="plan.subtitle" class="plan-admin-subtitle" placeholder="套餐副标题" />
                </div>
                <button
                  type="button"
                  class="plan-admin-publish-button"
                  :class="{ offline: plan.activeFlag === false }"
                  :disabled="savingMembershipPlanIds.has(plan.id)"
                  @click="toggleMembershipPlanActive(plan)"
                >
                  {{ savingMembershipPlanIds.has(plan.id) ? "同步中..." : (plan.activeFlag === false ? "上架" : "下架") }}
                </button>
              </header>

              <div class="plan-admin-section">
                <strong>价格策略</strong>
                <div class="plan-admin-fields three">
                  <label>{{ plan.topUpPack ? "一次性售价" : "月价" }}<input v-model.number="plan.monthlyPrice" type="number" min="0" step="0.01" /></label>
                  <label>排序<input v-model.number="plan.sortOrder" type="number" min="0" step="1" /></label>
                </div>
                <label class="plan-feature-toggle" style="margin-top: 12px;">
                  <input v-model="plan.topUpPack" type="checkbox" />
                  <span>作为加油包</span>
                </label>
                <small class="plan-type-hint">{{ plan.topUpPack ? "购买后立即叠加到用户当前额度，不改变当前套餐、到期时间或订阅队列。" : "作为订阅套餐，购买后按会员周期生效并进入续费队列。" }}</small>
              </div>

              <div class="plan-admin-section plan-feature-section">
                <strong>套餐权益</strong>
                <div class="plan-feature-editor">
                  <div class="plan-feature-row">
                    <label class="plan-feature-toggle">
                      <input v-model="plan.pluginImportEnabled" type="checkbox" />
                      <span>论文插件导入</span>
                    </label>
                    <em>{{ plan.pluginImportEnabled !== false ? "不限次" : "-" }}</em>
                  </div>
                  <div class="plan-feature-row">
                    <label class="plan-feature-toggle">
                      <input type="checkbox" :checked="isPlanQuotaEnabled(plan, plan.topUpPack ? 'translateQuota' : 'translateQuotaDaily')" @change="togglePlanQuotaFeature(plan, plan.topUpPack ? 'translateQuota' : 'translateQuotaDaily', $event.target.checked)" />
                      <span>对照翻译</span>
                    </label>
                    <input :value="plan.topUpPack ? plan.translateQuota : plan.translateQuotaDaily" @input="plan[plan.topUpPack ? 'translateQuota' : 'translateQuotaDaily'] = Number($event.target.value || 0)" type="number" min="0" />
                    <em>{{ plan.topUpPack ? "篇" : "篇/天" }}</em>
                  </div>
                  <div class="plan-feature-row">
                    <label class="plan-feature-toggle">
                      <input type="checkbox" :checked="isPlanQuotaEnabled(plan, plan.topUpPack ? 'immersiveQuota' : 'immersiveQuotaDaily')" @change="togglePlanQuotaFeature(plan, plan.topUpPack ? 'immersiveQuota' : 'immersiveQuotaDaily', $event.target.checked)" />
                      <span>沉浸全篇翻译</span>
                    </label>
                    <input :value="plan.topUpPack ? plan.immersiveQuota : plan.immersiveQuotaDaily" @input="plan[plan.topUpPack ? 'immersiveQuota' : 'immersiveQuotaDaily'] = Number($event.target.value || 0)" type="number" min="0" />
                    <em>{{ plan.topUpPack ? "篇" : "篇/天" }}</em>
                  </div>
                  <div class="plan-feature-row">
                    <label class="plan-feature-toggle">
                      <input type="checkbox" :checked="isPlanQuotaEnabled(plan, 'pptQuota')" @change="togglePlanQuotaFeature(plan, 'pptQuota', $event.target.checked)" />
                      <span>PPT 生成</span>
                    </label>
                    <input v-model.number="plan.pptQuota" type="number" min="0" />
                    <em>{{ plan.topUpPack ? "次" : "次/月" }}</em>
                  </div>
                  <div class="plan-feature-row">
                    <label class="plan-feature-toggle">
                      <input v-model="plan.agentEnabled" type="checkbox" />
                      <span>AI 积分（共用）</span>
                    </label>
                    <input v-model.number="plan.agentTokenQuota" type="number" min="0" step="1" />
                    <em>{{ plan.topUpPack ? "积分" : "积分/月" }}</em>
                  </div>
                  <div class="plan-feature-row">
                    <label class="plan-feature-toggle">
                      <input v-model="plan.reviewEnabled" type="checkbox" />
                      <span>AI 论文综述</span>
                    </label>
                    <em>{{ plan.reviewEnabled !== false ? "包含" : "-" }}</em>
                  </div>
                  <div class="plan-feature-row">
                    <label class="plan-feature-toggle">
                      <input v-model="plan.chatEnabled" type="checkbox" />
                      <span>研读解析与对话</span>
                    </label>
                    <em>{{ plan.chatEnabled !== false ? "包含" : "-" }}</em>
                  </div>
                  <div class="plan-feature-row">
                    <label class="plan-feature-toggle"><input v-model="plan.forumSpecial" type="checkbox" /><span>论坛标识</span></label>
                    <em>{{ plan.forumSpecial ? "包含" : "-" }}</em>
                  </div>
                  <div class="plan-feature-row">
                    <label class="plan-feature-toggle"><input v-model="plan.peakPriority" type="checkbox" /><span>高峰优先</span></label>
                    <em>{{ plan.peakPriority ? "优先通道" : "-" }}</em>
                  </div>
                  <div class="plan-feature-row">
                    <label class="plan-feature-toggle"><input :checked="isPlanQuotaEnabled(plan, 'forumTopDaily')" type="checkbox" @change="togglePlanQuotaFeature(plan, 'forumTopDaily', $event.target.checked)" /><span>发帖置顶</span></label>
                    <em>{{ isPlanQuotaEnabled(plan, 'forumTopDaily') ? "包含" : "-" }}</em>
                  </div>

                  <!-- Dynamic custom feature rows -->
                  <div v-for="(feat, fi) in (plan._extraFeatures || [])" :key="fi" class="plan-feature-row" style="gap: 6px;">
                    <label class="plan-feature-toggle">
                      <input type="checkbox" v-model="feat.included" />
                      <input v-model="feat.label" placeholder="功能名称" style="font-size: 0.78rem; background: transparent; border: none; border-bottom: 1px solid rgba(255,255,255,0.15); color: inherit; padding: 0 2px; width: 90px;" />
                    </label>
                    <input v-model="feat.value" placeholder="备注（如不限次）" style="flex: 1; font-size: 0.78rem; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 4px; padding: 3px 6px; color: inherit;" />
                    <button type="button" style="font-size: 0.7rem; color: #ef4444; background: none; border: none; cursor: pointer; padding: 2px 4px;" @click="removePlanExtraFeature(plan, fi)">×</button>
                  </div>

                  <button type="button" class="spatial-btn spatial-btn-ghost compact-btn" style="margin-top: 6px; font-size: 0.72rem; padding: 4px 10px;" @click="addPlanExtraFeature(plan)">+ 添加自定义功能</button>
                </div>
                <small class="plan-feature-note">对照翻译、沉浸全文翻译、PPT 按次数配置；AI 积分填写实际点数；其他权益仅通过勾选决定是否包含。</small>
              </div>

              <div class="plan-admin-seckill">
                <div class="seckill-head">
                  <label><input v-model="plan.seckillEnabled" type="checkbox" /> 开启限时秒杀</label>
                  <span v-if="isPlanSeckillActive(plan)" class="seckill-live">进行中 · {{ getPlanSeckillCountdownText(plan) }}</span>
                </div>
                <div class="plan-admin-fields" style="grid-template-columns: repeat(2, 1fr);">
                  <label>秒杀标签<input v-model.trim="plan.seckillLabel" placeholder="限时秒杀" /></label>
                  <label>秒杀价格<input v-model.number="plan.seckillPrice" type="number" min="0" step="0.01" /></label>
                  <label>开始时间<input v-model="plan.seckillStartsAt" type="datetime-local" /></label>
                  <label>结束时间<input v-model="plan.seckillEndsAt" type="datetime-local" /></label>
                </div>
              </div>

              <!-- 兑换码生成区域 -->
              <div class="plan-admin-promo-code" style="padding: 12px 16px; margin: 12px -16px; background: rgba(255,255,255,0.02); border-top: 1px solid rgba(255,255,255,0.05); border-bottom: 1px solid rgba(255,255,255,0.05);">
                <div style="display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 4px;">
                  <span style="font-size: 0.8rem; color: #94a3b8; font-weight: 600;">兑换码管理</span>
                  <button class="spatial-btn spatial-btn-ghost compact-btn" style="padding: 4px 10px; font-size: 0.75rem;" :disabled="generatingPromo[plan.id]" @click="generatePromoCodeForPlan(plan.id)">
                    {{ generatingPromo[plan.id] ? "生成中..." : "+ 新建兑换码" }}
                  </button>
                </div>
                <div v-if="getPromoCodesForPlan(plan.id).length" style="display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px;">
                  <div
                    v-for="codeObj in getPromoCodesForPlan(plan.id)"
                    :key="typeof codeObj === 'string' ? codeObj : codeObj.code"
                    :style="{ display: 'flex', alignItems: 'center', gap: '4px', background: (typeof codeObj === 'object' && codeObj.invalidated) ? 'rgba(148,163,184,0.08)' : 'rgba(16,185,129,0.08)', border: `1px solid ${(typeof codeObj === 'object' && codeObj.invalidated) ? 'rgba(148,163,184,0.25)' : 'rgba(16,185,129,0.2)'}`, borderRadius: '6px', padding: '3px 8px', cursor: (typeof codeObj === 'object' && codeObj.invalidated) ? 'default' : 'pointer', opacity: (typeof codeObj === 'object' && codeObj.invalidated) ? 0.65 : 1 }"
                    @click="copyPromoCode(typeof codeObj === 'string' ? codeObj : codeObj.code)"
                    :title="'点击复制：' + (typeof codeObj === 'string' ? codeObj : codeObj.code)"
                  >
                    <strong :style="{ color: (typeof codeObj === 'object' && codeObj.invalidated) ? '#94a3b8' : '#10b981', fontFamily: 'monospace', fontSize: '0.9rem', letterSpacing: '1px' }">{{ typeof codeObj === 'string' ? codeObj : codeObj.code }}</strong>
                    <span v-if="typeof codeObj === 'object'" style="font-size: 0.7rem; color: #64748b; margin-left: 2px;">已兑 {{ codeObj.usedCount || 0 }} 次 / 无限</span>
                    <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="#10b981" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                    <button v-if="typeof codeObj === 'object' && !codeObj.invalidated" type="button" class="promo-invalidate-btn" title="作废兑换码" @click.stop="invalidatePromoCode(codeObj)">×</button>
                    <span v-else-if="typeof codeObj === 'object' && codeObj.invalidated" style="font-size: 0.68rem; color: #94a3b8;">已作废</span>
                  </div>
                </div>
                <div v-else style="font-size: 0.75rem; color: #64748b; margin-top: 4px;">暂无兑换码，点击上方按钮生成（每个账号限兑一次，可手动作废）</div>
              </div>

              <footer>
                <p>{{ planPreview(plan) }}</p>
                <div class="plan-admin-footer-actions">
                  <button
                    v-if="canDeleteMembershipPlan(plan)"
                    class="spatial-btn spatial-btn-ghost compact-btn danger-lite"
                    :disabled="deletingMembershipPlanIds.has(plan.id)"
                    @click="deleteMembershipPlan(plan)"
                  >
                    {{ deletingMembershipPlanIds.has(plan.id) ? "删除中..." : "删除" }}
                  </button>
                  <button class="spatial-btn spatial-btn-accent compact-btn" :disabled="savingMembershipPlanIds.has(plan.id)" @click="saveMembershipPlan(plan)">
                    {{ savingMembershipPlanIds.has(plan.id) ? "保存中..." : "保存套餐" }}
                  </button>
                </div>
              </footer>
            </article>
          </div>

        </div>

        <!-- Tab Content: Recharges -->
        <div v-if="activeTab === 'recharges'" class="tab-pane">
          <div class="pane-header-row">
            <div>
              <h3>充值订单与售后处理</h3>
              <p class="pane-description">集中处理用户支付工单、退款申请、订单记录与手动充值入账。</p>
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
                <span>{{ paginationText(paymentTicketsTotal || paymentTickets.length, ticketPage, ticketPageSize) }}</span>
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
                  <strong>{{ filteredPaymentOrders.length }} 笔</strong>
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
              <div v-if="!filteredPaymentOrders.length" class="payment-empty">暂无已完成的支付订单。</div>
              <div v-else class="admin-pagination compact-pagination">
                <span>{{ paginationText(filteredPaymentOrders.length, orderPage, orderPageSize) }}</span>
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
            <input id="admin-recharge-search" name="rechargeSearch" v-model="rechargeQuery" placeholder="按用户名、ID 过滤充值记录..." style="width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--spatial-line); background: var(--spatial-surface); color: var(--spatial-graphite);" />
          </div>

          <div class="table-container spatial-glass-panel">
            <table class="admin-table">
              <thead><tr><th>唯一 ID</th><th>用户名称</th><th>入账金额</th><th>入账方式</th><th>时间</th></tr></thead>
              <tbody>
                <tr v-for="r in paginatedRecharges" :key="r.id">
                  <td><code class="model-code-id" style="font-weight: 700; color: var(--c-accent);">{{ r.numericId || '—' }}</code></td>
                  <td>{{ r.username }}</td>
                  <td>¥{{ formatMoney(r.amount) }}</td>
                  <td>余额充值</td>
                  <td>{{ r.time }}</td>
                </tr>
                <tr v-if="filteredRecharges.length === 0">
                  <td colspan="5" style="text-align: center; color: #64748b; padding: 32px 0;">暂无充值发放记录</td>
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


        <!-- Tab Content: Models (Redesigned 2-Column Layout) -->
        <div v-if="activeTab === 'models'" class="tab-pane models-redesign-pane">

          <!-- Header Area -->
          <div class="models-pane-header">
            <div class="models-pane-header-left">
              <h3>AI 中转模型配置中心</h3>
              <p class="pane-description">集中管理 API 中转服务商、模型连接与业务号池路由。</p>
            </div>
            <!-- Scene pool status cards -->
            <div class="scene-pool-status-bar">
              <button
                v-for="scene in modelSceneOptions"
                :key="scene.value"
                class="scene-status-card"
                @click="openScenePoolModal(scene.value)"
                :title="scene.hint"
              >
                <div class="scene-status-header">
                  <span class="scene-status-dot" :class="getPoolCount(scene.value) > 0 ? 'active' : 'inactive'"></span>
                  <span class="scene-status-name">{{ scene.label }}</span>
                </div>
                <div class="scene-status-count">
                  <strong>{{ getPoolCount(scene.value) }}</strong>
                  <span>可用</span>
                </div>
              </button>
            </div>
          </div>
          <div class="models-two-col-layout" :class="{ 'providers-collapsed': isProvidersCollapsed }">

            <!-- Column 1: 中转站服务商 -->
            <div class="providers-column spatial-glass-panel">
              <header class="column-header">
                <strong v-if="!isProvidersCollapsed">中转服务商</strong>
                <button v-if="!isProvidersCollapsed" class="add-provider-btn" @click="showAddRelayModal = true">
                  + 添加
                </button>
                <button class="collapse-providers-btn" @click="isProvidersCollapsed = !isProvidersCollapsed" :title="isProvidersCollapsed ? '展开中转商列表' : '折叠中转商列表'">
                  <svg v-if="isProvidersCollapsed" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 14px; height: 14px;"><polyline points="13 17 18 12 13 7"/><polyline points="6 17 11 12 6 7"/></svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 14px; height: 14px;"><polyline points="11 17 6 12 11 7"/><polyline points="18 17 13 12 18 7"/></svg>
                </button>
              </header>
              <div v-if="loadingRelays" class="loading-state">
                <span class="loading-spinner"></span> 加载中...
              </div>
              <div v-else-if="relays.length === 0" class="empty-state">
                暂无
              </div>
              <div v-else class="providers-list">
                <div
                  v-for="relay in relays"
                  :key="relay.id"
                  class="provider-item-card"
                  :class="{ active: activeRelay?.id === relay.id, 'collapsed-item': isProvidersCollapsed }"
                  @click="activeRelay = relay"
                  :title="isProvidersCollapsed ? (relay.providerName + ' (' + relay.baseUrl + ')') : ''"
                >
                  <template v-if="isProvidersCollapsed">
                    <div class="provider-avatar-circle">
                      {{ relay.providerName.substring(0, 2).toUpperCase() }}
                    </div>
                  </template>
                  <template v-else>
                    <div class="provider-card-info">
                      <span class="provider-name">{{ relay.providerName }}</span>
                      <span class="provider-url">{{ relay.baseUrl }}</span>
                    </div>
                    <div class="provider-card-actions">
                      <button class="provider-action-btn edit-btn" type="button" @click.stop="openEditRelayModal(relay)" title="编辑">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 13px; height: 13px;"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 1 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                      </button>
                      <button class="provider-action-btn delete-btn" type="button" @click.stop="deleteRelay(relay)" title="删除">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 13px; height: 13px;"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                      </button>
                    </div>
                  </template>
                </div>
              </div>
            </div>

            <!-- Column 2: Models Config Grid with Pagination -->
            <div class="models-right-stack">
              <!-- Models Panel -->
              <div class="models-grid-panel spatial-glass-panel">
                <div v-if="!activeRelay" class="empty-state" style="padding: 60px 0;">
                  请先在左侧选择或添加中转服务商
                </div>
                <div v-else-if="loadingModels" class="loading-state" style="padding: 60px 0;">
                  <span class="loading-spinner"></span> 正在读取可用模型列表...
                </div>
                <div v-else-if="relayModels.length === 0" class="empty-state" style="padding: 60px 0;">
                  <strong>未读取到模型列表</strong>
                  <p v-if="relayModelsError" class="model-load-error">{{ relayModelsError }}</p>
                  <p v-else>请先配置连接并验证 API Key。</p>
                  <div class="model-empty-actions">
                    <button class="spatial-btn spatial-btn-ghost compact-btn" @click="openEditRelayModal(activeRelay)">
                      配置连接
                    </button>
                    <button class="spatial-btn spatial-btn-accent compact-btn" @click="loadRelayModels(activeRelay)">
                      重新读取模型
                    </button>
                  </div>
                </div>
                <div v-else class="models-section">
                  <!-- Provider Info Banner -->
                  <div class="relay-info-banner">
                    <div class="relay-info-left">
                      <div class="relay-info-name">{{ activeRelay.providerName }}<span class="relay-info-dash"> - </span><span class="relay-info-subtitle">连接配置</span></div>
                      <div class="relay-info-url">Base URL &nbsp;<code>{{ activeRelay.baseUrl }}</code></div>
                    </div>
                    <div class="relay-info-actions">
                      <button class="spatial-btn spatial-btn-ghost compact-btn" @click="openEditRelayModal(activeRelay)">
                        配置连接
                      </button>
                      <button
                        class="spatial-btn spatial-btn-accent compact-btn"
                        :disabled="testingRelay"
                        :style="testingRelay ? 'opacity:0.7;cursor:not-allowed;' : ''"
                        title="向该中转站 Base URL 发送端点连通性测试，不消耗任何大模型 Token 费用"
                        @click="testRelayConnectionSpeed"
                      >
                        <span v-if="testingRelay">探测中…</span>
                        <span v-else-if="relayPingResult && relayPingResult.success">⚡ {{ relayPingResult.latencyMs }}ms (连通)</span>
                        <span v-else-if="relayPingResult && !relayPingResult.success">⚠️ 连接失败</span>
                        <span v-else>测速节点 (0消耗)</span>
                      </button>
                    </div>
                  </div>

                  <!-- Models filtering and search sub-header -->
                  <div class="models-subheader">
                    <div class="models-filters-left">
                      <!-- Search Input -->
                      <div class="model-search-box">
                        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <circle cx="11" cy="11" r="8"></circle>
                          <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                        </svg>
                        <input
                          v-model="modelSearchQuery"
                          type="text"
                          placeholder="搜索模型标识..."
                          class="spatial-search-input"
                        />
                        <button v-if="modelSearchQuery" class="clear-search-btn" @click="modelSearchQuery = ''">×</button>
                      </div>

                      <!-- Filter Dropdown -->
                      <div class="model-filter-box">
                        <span class="filter-label">路由号池:</span>
                        <select v-model="modelSceneFilter" class="spatial-filter-select">
                          <option value="all">所有模型</option>
                          <option value="available">当前可用 (测速成功)</option>
                          <option value="none">未分配号池</option>
                          <option value="any">已分配任意号池</option>
                          <option v-for="scene in modelSceneOptions" :key="scene.value" :value="scene.value">
                            {{ scene.label }}号池
                          </option>
                        </select>
                      </div>
                    </div>

                    <div class="models-filters-right">
                      <span class="models-count-label">
                        已选 <em>{{ filteredModels.length }}</em> / 共 {{ relayModels.length }}
                      </span>
                    </div>
                  </div>

                  <div class="models-cards-grid">
                    <article v-for="model in paginatedModels" :key="model.id" class="model-dashboard-card">
                      <!-- Card Top: name + speed pill inline -->
                      <div class="model-card-top-row">
                        <strong class="model-name-id">{{ model.id }}</strong>
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

                      <!-- Provider badge -->
                      <div style="display: flex; align-items: center; gap: 8px; margin-top: 4px;">
                        <span class="model-badge-provider">{{ activeRelay.providerName }}</span>
                        <span class="model-type-tag" :class="getModelMetadata(model.id).typeClass">
                          {{ getModelMetadata(model.id).type }}
                        </span>
                      </div>

                      <!-- Billing info - visual cost tier -->
                      <div class="model-cost-tier-wrapper">
                        <span
                          class="model-cost-tier-badge"
                          :class="getModelMetadata(model.id).costTierClass"
                          :title="getModelMetadata(model.id).billing"
                        >
                          <span class="cost-tier-dots">
                            <i v-for="n in 5" :key="n" :class="{ active: n <= getModelMetadata(model.id).costDots }"></i>
                          </span>
                          {{ getModelMetadata(model.id).costLabel }}
                        </span>
                        <span class="model-billing-hint" :title="getModelMetadata(model.id).billing">
                          {{ getModelMetadata(model.id).billingShort }}
                        </span>
                      </div>

                      <!-- Model Description -->
                      <p class="model-desc-info-text">
                        {{ getModelMetadata(model.id).desc }}
                      </p>

                      <!-- Speed error message -->
                      <div
                        v-if="modelTestResults[model.id] && !modelTestResults[model.id].success && modelTestResults[model.id].message"
                        class="model-speed-error-container"
                      >
                        <div class="model-speed-error-summary" @click="toggleErrorDetail(model.id)">
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 12px; height: 12px; margin-right: 4px; color: #ff6b6b;"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                          <span>接口返回错误 (点击查看)</span>
                        </div>
                        <div
                          v-if="showErrorDetail[model.id]"
                          class="model-speed-error-detail"
                        >
                          {{ modelTestResults[model.id].message }}
                        </div>
                      </div>

                      <!-- Scene checkboxes -->
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

                  <!-- Pagination -->
                  <div v-if="filteredModels.length > 0" class="models-pagination">
                    <div v-if="totalPages > 1" class="models-pagination-nav" style="display: flex; align-items: center; gap: 8px;">
                      <button
                        :disabled="currentPage === 1"
                        @click="currentPage--"
                        class="pagination-arrow-btn"
                      >
                        &lt; 上一页
                      </button>
                      <div class="pagination-pages">
                        <button
                          v-for="page in visiblePages"
                          :key="page"
                          :class="{ active: currentPage === page, separator: page === '...' }"
                          :disabled="page === '...'"
                          @click="currentPage = page"
                          class="pagination-page-btn"
                        >
                          {{ page }}
                        </button>
                      </div>
                      <button
                        :disabled="currentPage === totalPages"
                        @click="currentPage++"
                        class="pagination-arrow-btn"
                      >
                        下一页 &gt;
                      </button>
                    </div>
                    <div v-else></div> <!-- Placeholder to push selector to the right -->

                    <!-- Page Size Selector -->
                    <div class="models-page-size-wrap">
                      <select v-model.number="pageSize" class="spatial-page-size-select">
                        <option :value="12">12 个模型/页</option>
                        <option :value="24">24 个模型/页</option>
                        <option :value="48">48 个模型/页</option>
                        <option :value="96">96 个模型/页</option>
                      </select>
                    </div>
                  </div>
                </div>
              </div>

            </div>

          </div>
        </div>

        <!-- Tab Content: AI Usage Calls -->
        <div v-if="activeTab === 'aiUsage'" class="tab-pane">
          <AdminAiUsagePanel />
        </div>

        <!-- Tab Content: Monitoring Dashboard -->
        <div v-if="activeTab === 'monitoring'" class="tab-pane">
          <AdminMonitoringPanel />
        </div>

        <!-- Tab Content: Translation Issues -->
        <div v-if="activeTab === 'translationIssues'" class="tab-pane translation-issues-pane">
          <div class="pane-header-row">
            <div>
              <h3>翻译问题日志</h3>
              <p class="pane-description">显示每次翻译请求的成功/失败、耗时、路由和网络环境，便于区分执行中与故障。</p>
            </div>
            <button class="spatial-btn spatial-btn-ghost compact-btn" :disabled="translationIssuesLoading" @click="loadTranslationIssues">
              {{ translationIssuesLoading ? "刷新中..." : "刷新" }}
            </button>
          </div>

          <div class="translation-issue-summary">
            <article class="translation-issue-stat spatial-glass-panel">
              <span>问题记录</span>
              <strong>{{ translationIssueSummary.problemCount || 0 }}</strong>
              <small>当前列表</small>
            </article>
            <article class="translation-issue-stat spatial-glass-panel">
              <span>失败请求</span>
              <strong>{{ translationIssueSummary.failedCount || 0 }}</strong>
              <small>累计失败</small>
            </article>
            <article class="translation-issue-stat spatial-glass-panel">
              <span>慢请求</span>
              <strong>{{ translationIssueSummary.slowCount || 0 }}</strong>
              <small>≥ {{ translationIssueSummary.slowThresholdMs || 1800 }}ms</small>
            </article>
            <article class="translation-issue-stat spatial-glass-panel">
              <span>Google 问题</span>
              <strong>{{ translationIssueSummary.googleIssues || 0 }}</strong>
              <small>当前列表</small>
            </article>
            <article class="translation-issue-stat spatial-glass-panel">
              <span>P95 延迟</span>
              <strong>{{ translationIssueSummary.p95LatencyMs || 0 }}ms</strong>
              <small>问题样本</small>
            </article>
          </div>

          <div class="search-filter-toolbar spatial-glass-panel translation-issue-toolbar">
            <input v-model.trim="translationIssueQuery" type="text" placeholder="搜索引擎、路由、用户、错误、IP" />
            <select v-model="translationIssueProviderFilter">
              <option value="全部">全部引擎</option>
              <option v-for="provider in translationIssueProviders" :key="provider" :value="provider">{{ provider }}</option>
            </select>
            <select v-model="translationIssueStatusFilter">
              <option value="全部">全部状态</option>
              <option value="failed">失败</option>
              <option value="slow">慢请求</option>
            </select>
          </div>

          <div class="table-container spatial-glass-panel" style="margin-top: 16px;">
            <table class="admin-table translation-issue-table">
              <thead>
                <tr>
                  <th style="white-space: nowrap;">时间</th>
                  <th>用户</th>
                  <th>文章 / 翻译类型</th>
                  <th style="white-space: nowrap;">引擎</th>
                  <th>路由</th>
                  <th style="white-space: nowrap;">语言</th>
                  <th style="white-space: nowrap;">耗时</th>
                  <th style="white-space: nowrap;">状态</th>
                  <th>错误与网络</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="issue in paginatedTranslationIssues" :key="issue.id" :class="{ 'status-open': !issue.success }">
                  <td class="ledger-time" style="white-space: nowrap;">{{ formatDateTime(issue.createdAt) }}</td>
                  <td>
                    <strong>{{ issue.username || issue.email || "—" }}</strong>
                    <small>{{ issue.numericId || issue.userId || "—" }} · {{ issue.ipAddress || "无 IP" }}</small>
                  </td>
                  <td><strong>{{ issue.paperTitle || "未命名论文" }}</strong><small>{{ issue.translationMode || (String(issue.route || '').startsWith('immersive') ? '沉浸式翻译' : String(issue.route || '').startsWith('translate') || String(issue.route || '').startsWith('dual') ? '对照翻译' : '未记录') }}</small></td>
                  <td>
                    <span class="translation-provider-pill">{{ issue.provider || "unknown" }}</span>
                    <small>{{ issue.clientType || "desktop" }}</small>
                  </td>
                  <td>
                    <span class="translation-route-text" :title="issue.route || '无路由信息'">{{ issue.route || "—" }}</span>
                  </td>
                  <td style="white-space: nowrap;">{{ issue.sourceLang || "auto" }} → {{ issue.targetLang || "zh-CN" }}</td>
                  <td style="white-space: nowrap;">
                    <strong :class="{ 'latency-danger': Number(issue.latencyMs || 0) >= (translationIssueSummary.slowThresholdMs || 1800) }">
                      {{ issue.quotaRecord ? '—' : `${issue.latencyMs || 0}ms` }}
                    </strong>
                    <small>{{ issue.charCount || 0 }} 字符</small>
                  </td>
                  <td>
                    <span class="translation-status-badge" :class="issue.quotaRecord ? 'pending' : issue.success
                      ? (Number(issue.latencyMs || 0) >= (translationIssueSummary.slowThresholdMs || 1800) ? 'slow' : 'success')
                      : 'failed'">
                      {{ issue.quotaRecord ? '额度已扣减（非完成）' : issue.success ? (Number(issue.latencyMs || 0) >= (translationIssueSummary.slowThresholdMs || 1800) ? "成功·较慢" : "成功" ) : "失败" }}
                    </span>
                  </td>
                  <td>
                    <div class="translation-error-text" :title="issue.errorMessage || issue.networkProfile || issue.userAgent || ''">
                      {{ issue.errorMessage || issue.networkProfile || issue.userAgent || "—" }}
                    </div>
                  </td>
                </tr>
                <tr v-if="!filteredTranslationIssues.length">
                  <td colspan="9" class="payment-empty" style="text-align: center; padding: 32px;">暂无翻译问题记录。</td>
                </tr>
              </tbody>
            </table>
          </div>

          <section v-if="translationJobs.length" class="translation-jobs-panel spatial-glass-panel">
            <div class="translation-jobs-heading">
              <div>
                <strong>异步任务状态</strong>
                <small>双栏翻译与论文结构解析会在后台持续执行，切换页面不会中断。</small>
              </div>
            </div>
            <div class="table-container translation-jobs-table">
              <table class="admin-table">
                <thead><tr><th>任务</th><th>文献</th><th>进度</th><th>状态</th><th>更新时间</th><th>说明</th></tr></thead>
                <tbody>
                  <tr v-for="job in translationJobs" :key="`${job.type}-${job.id}`">
                    <td>{{ job.type === 'PDF_MATH_TRANSLATE_V2' ? '对照翻译' : '论文结构解析' }}</td>
                    <td class="translation-route-text" :title="job.resourceId">{{ job.resourceId || '—' }}</td>
                    <td>{{ Number(job.progress || 0) }}%</td>
                    <td><span class="translation-status-badge" :class="jobStatusClass(job.status)">{{ jobStatusLabel(job.status) }}</span></td>
                    <td style="white-space: nowrap;">{{ formatDateTime(job.updatedAt) }}</td>
                    <td class="translation-error-text">{{ job.message || job.detail || '—' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <div v-if="filteredTranslationIssues.length" class="admin-pagination compact-pagination" style="margin-top: 16px;">
            <span>{{ paginationText(filteredTranslationIssues.length, translationIssuePage, translationIssuePageSize) }}</span>
            <div>
              <select v-model.number="translationIssuePageSize" class="pagination-size-select">
                <option :value="10">10 条/页</option>
                <option :value="20">20 条/页</option>
                <option :value="40">40 条/页</option>
              </select>
              <button :disabled="translationIssuePage <= 1" @click="translationIssuePage -= 1">上一页</button>
              <strong>{{ translationIssuePage }} / {{ translationIssuePageCount }}</strong>
              <button :disabled="translationIssuePage >= translationIssuePageCount" @click="translationIssuePage += 1">下一页</button>
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
                  管理员公告
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
                <div
                  ref="siteMessageEditor"
                  class="site-message-editor"
                  contenteditable="true"
                  role="textbox"
                  aria-multiline="true"
                  :data-placeholder="newSiteMessage.messageType === 'timeline' ? '输入本次版本新增、修复、优化内容' : '输入需要全站展示的通知内容'"
                  @input="syncSiteMessageEditor"
                ></div>
              </label>
              <div class="site-message-color-picker">
                <span>文字颜色</span>
                <button
                  v-for="color in siteMessageColors"
                  :key="color"
                  type="button"
                  class="site-message-color-swatch"
                  :class="{ active: newSiteMessage.textColor === color }"
                  :style="{ backgroundColor: color }"
                  :title="`选择 ${color}`"
                  @click="applySiteMessageColor(color)"
                ></button>
              </div>

              <!-- Image Upload Section -->
              <div class="message-image-upload-section">
                <span>配图上传 (可选)</span>
                <div v-if="newSiteMessage.imageUrl" class="message-image-preview-card">
                  <img :src="newSiteMessage.imageUrl" alt="配图预览" />
                  <button type="button" class="remove-preview-btn" @click="clearMessageImage">移除图片</button>
                </div>
                <div v-else class="message-image-upload-trigger">
                  <label class="upload-trigger-label">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 15px; height: 15px; margin-right: 6px;"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                    选择配图
                    <input type="file" accept="image/*" @change="handleMessageImageUpload" style="display: none;" />
                  </label>
                </div>
              </div>

              <div class="site-message-form-footer">
                <span>{{ newSiteMessage.content.length }}/1000</span>
                <button class="spatial-btn spatial-btn-accent compact-btn" :disabled="siteMessagePublishing">
                  {{ siteMessagePublishing ? (editingSiteMessageId ? "保存中..." : "发布中...") : (editingSiteMessageId ? "保存修改" : "立即发布") }}
                </button>
                <button v-if="editingSiteMessageId" type="button" class="spatial-btn compact-btn" :disabled="siteMessagePublishing" @click="cancelSiteMessageEdit">
                  取消编辑
                </button>
              </div>
            </form>

            <section class="site-message-list spatial-glass-panel">
              <div class="site-message-list-header">
                <h4>历史消息</h4>
                <span>{{ siteMessages.length }} 条</span>
              </div>
              <article v-for="message in paginatedSiteMessages" :key="message.id" class="site-message-row">
                <div style="flex: 1; min-width: 0;">
                  <div class="site-message-title-row">
                    <strong>{{ message.title }}</strong>
                    <span class="message-type-badge" :class="message.messageType === 'timeline' ? 'timeline' : 'notice'">
                      {{ message.messageType === "timeline" ? "版本时间线" : "管理员公告" }}
                    </span>
                    <span :class="message.activeFlag ? 'message-active' : 'message-inactive'">
                      {{ message.activeFlag ? "展示中" : "已撤下" }}
                    </span>
                  </div>
                  <p :style="{ color: message.textColor || '#000000', wordBreak: 'break-all' }" v-html="sanitizeSiteMessageHtml(message.content)"></p>

                  <!-- Render message image if it exists -->
                  <div v-if="message.imageUrl" class="site-message-image-thumb">
                    <button type="button" class="image-thumb-btn" @click="openCampusImage(message.imageUrl, '消息配图')">
                      <img :src="message.imageUrl" alt="消息配图" />
                      <span>点击放大</span>
                    </button>
                  </div>

                  <small>{{ formatDateTime(message.createdAt) }}</small>
                </div>
                <div class="site-message-actions">
                  <button class="action-btn text-btn" @click="editSiteMessage(message)">编辑</button>
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
                      <button
                        class="spatial-btn spatial-btn-ghost compact-btn danger-lite"
                        @click="deleteForumPostDirectly(report)"
                      >
                        删除
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

          <div class="table-container spatial-glass-panel" style="margin-top: 16px;">
            <table class="admin-table">
              <thead>
                <tr>
                  <th style="white-space: nowrap;">时间</th>
                  <th style="white-space: nowrap;">申请人</th>
                  <th>学校与学号</th>
                  <th>学生证件</th>
                  <th>审核备注</th>
                  <th style="white-space: nowrap;">状态</th>
                  <th style="text-align: right; min-width: 200px; padding-right: 18px;">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="request in paginatedCampusVerifications"
                  :key="request.id"
                  :class="`status-${request.status}`"
                >
                  <td class="ledger-time" style="white-space: nowrap;">
                    {{ formatDateTime(request.createdAt) }}
                  </td>
                  <td>
                    <div class="campus-user-info">
                      <strong>{{ request.userName || "—" }}</strong>
                      <small style="display: block; font-size: 0.72rem; color: var(--spatial-silver); margin-top: 2px;">
                        {{ request.email || "—" }}
                      </small>
                    </div>
                  </td>
                  <td>
                    <div class="campus-school-info">
                      <strong>{{ request.schoolName }}</strong>
                      <small style="display: block; font-size: 0.72rem; color: var(--spatial-silver); margin-top: 2px;">
                        学号: {{ request.studentNo }}
                      </small>
                    </div>
                  </td>
                  <td>
                    <!-- Thumbnail preview section -->
                    <div class="campus-thumbs-row">
                      <button
                        v-if="request.studentCardFront"
                        type="button"
                        class="campus-thumb-btn"
                        @click="openCampusImage(request.studentCardFront, '学生证正面')"
                        title="查看学生证正面大图"
                      >
                        <img :src="request.studentCardFront" alt="正面" />
                        <span>正面</span>
                      </button>
                      <button
                        v-if="request.studentCardBack"
                        type="button"
                        class="campus-thumb-btn"
                        @click="openCampusImage(request.studentCardBack, '学生证反面')"
                        title="查看学生证反面大图"
                      >
                        <img :src="request.studentCardBack" alt="反面" />
                        <span>反面</span>
                      </button>
                    </div>
                  </td>
                  <td>
                    <span v-if="request.adminNote" class="campus-note-text" :title="request.adminNote">
                      {{ request.adminNote }}
                    </span>
                    <span v-else style="color: var(--spatial-silver); font-style: italic; font-size: 0.8rem;">无</span>
                  </td>
                  <td>
                    <span class="campus-status-badge" :class="request.status">
                      {{ request.statusLabel || campusVerificationStatusLabel(request.status) }}
                    </span>
                  </td>
                  <td style="text-align: right; padding-right: 18px;">
                    <div class="forum-report-actions" style="justify-content: flex-end; display: flex; gap: 8px;">
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
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="!campusVerifications.length" class="payment-empty" style="border-top: none;">暂无校园认证申请。</div>
          </div>

          <div v-if="campusVerifications.length" class="admin-pagination compact-pagination">
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
              <option v-for="plan in assignableMembershipPlans" :key="plan.id" :value="plan.id">
                {{ plan.name }}
              </option>
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

    <Transition name="fade">
      <div v-if="showBatchMembershipModal" class="admin-modal-overlay" @click="showBatchMembershipModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop>
          <h4>批量调整额度</h4>
          <p>只修改勾选用户的额度，不改变会员套餐。留空的项目保持原值；对照翻译和沉浸阅读按今日额度处理，次日恢复套餐日额度。</p>
          <div class="admin-batch-quota-grid">
            <label>AI积分<input v-model="batchQuota.fruitScore" type="number" min="0" placeholder="不修改" /></label>
            <label>对照翻译（今日）<input v-model="batchQuota.translate" type="number" min="0" placeholder="不修改" /></label>
            <label>沉浸阅读（今日）<input v-model="batchQuota.immersive" type="number" min="0" placeholder="不修改" /></label>
            <label>PPT次数<input v-model="batchQuota.ppt" type="number" min="0" placeholder="不修改" /></label>
            <label>AI对话次数<input v-model="batchQuota.chat" type="number" min="0" placeholder="不修改" /></label>
            <label>研读解析次数<input v-model="batchQuota.research" type="number" min="0" placeholder="不修改" /></label>
            <label>文献综述次数<input v-model="batchQuota.review" type="number" min="0" placeholder="不修改" /></label>
            <label>Agent Token<input v-model="batchQuota.agent" type="number" min="0" placeholder="不修改" /></label>
          </div>
          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showBatchMembershipModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" :disabled="batchMembershipSaving" @click="saveBatchQuota">{{ batchMembershipSaving ? "正在保存…" : "确认修改" }}</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Quota Detail & Replenish Modal -->
    <Transition name="fade">
      <div v-if="showQuotaDetailModal" class="admin-modal-overlay" @click="showQuotaDetailModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop style="width: 620px; max-width: 96vw;">
          <div class="quota-modal-heading">
            <div>
          <h4>权益使用详情与每日额度调整</h4>
              <p>查看 {{ selectedUserForQuota?.username }} 的权益消耗；对照翻译和沉浸全文翻译的每日额度调整会持续生效，签到等单次补给仍单独计入。</p>
            </div>
            <button
              type="button"
              class="spatial-btn spatial-btn-accent quota-refill-button"
              :disabled="refillingPlanQuota"
              @click="refillPlanQuota"
            >
              {{ refillingPlanQuota ? "补满中" : "补满套餐权益" }}
            </button>
          </div>
          <div class="quota-modal-snapshot" style="margin-bottom: 20px;">
            <div>
              <span>用户名</span>
              <strong>{{ selectedUserForQuota?.username }}</strong>
            </div>
            <div>
              <span>当前套餐</span>
              <strong>{{ membershipPlanName(selectedUserForQuota?.membershipPlan) }}</strong>
            </div>
          </div>

          <div style="display: flex; flex-direction: column; gap: 12px; max-height: 400px; overflow-y: auto; padding-right: 4px;">
            <div v-for="item in [
              { key: 'translate', name: '对照翻译（单次补给）', used: 0, quota: selectedUserForQuota?.translateOneOffQuota, unit: '篇', isDaily: false, isOneOffTranslation: true },
              { key: 'immersive', name: '沉浸翻译（单次补给）', used: 0, quota: selectedUserForQuota?.immersiveOneOffQuota, unit: '篇', isDaily: false, isOneOffTranslation: true },
              { key: 'translateDaily', name: '对照翻译（每日额度）', used: selectedUserForQuota?.translateUsed, quota: selectedUserForQuota?.translateQuota, unit: '篇/天', isDaily: true },
              { key: 'immersiveDaily', name: '沉浸翻译（每日额度）', used: selectedUserForQuota?.immersiveUsed, quota: selectedUserForQuota?.immersiveQuota, unit: '篇/天', isDaily: true },
              { key: 'ppt', name: 'PPT生成次数', used: selectedUserForQuota?.pptUsed, quota: selectedUserForQuota?.pptQuota, unit: '次/月', isDaily: false },
              { key: 'fruitScore', name: 'AI通用积分', used: null, quota: selectedUserForQuota?.fruitScore, unit: '积分', isPoints: true }
            ]" :key="item.key" class="quota-item-row">
              <div style="display: flex; flex-direction: column; gap: 3px;">
                <div style="display: flex; align-items: center; gap: 6px;">
                  <span style="font-weight: 700; font-size: 0.85rem;">{{ item.name }}</span>
                  <span v-if="item.isPoints" style="font-size: 0.72rem; color: #10b981; font-weight: 700; background: rgba(16,185,129,0.1); padding: 1px 5px; border-radius: 4px;">
                    余额: {{ item.quota || 0 }} 积分
                  </span>
                  <span v-else style="font-size: 0.72rem; color: #6366f1; font-weight: 700; background: rgba(99,102,241,0.1); padding: 1px 5px; border-radius: 4px;">
                    剩余可用: {{ Math.max(0, (item.quota || 0) - (item.used || 0)) }} {{ item.isDaily ? '篇' : '次' }}
                  </span>
                </div>
                <span style="font-size: 0.75rem; color: #64748b;">
                  <template v-if="item.isDaily">
                    今日已用: <strong style="color: #2563eb;">{{ item.used || 0 }}</strong> 篇 / 每日额度: <strong>{{ item.quota || 0 }}</strong> 篇
                  </template>
                  <template v-else-if="item.isOneOffTranslation">
                    当前剩余单次补给: <strong style="color: #2563eb;">{{ item.quota || 0 }}</strong> 篇（用完即止，不随每日额度重置）
                  </template>
                  <template v-else-if="item.isPoints">
                    当前账户积分余额: <strong style="color: #10b981;">{{ item.quota || 0 }}</strong> 积分 (研读/问答单次扣减 1 积分)
                  </template>
                  <template v-else>
                    当期已用: <strong style="color: #2563eb;">{{ item.used || 0 }}</strong> 次 / 当期总额度: <strong>{{ item.quota || 0 }}</strong> 次 (PPT专属次数)
                  </template>
                </span>
              </div>
              <div style="display: flex; align-items: center; gap: 8px;">
                <input
                  type="number"
                  v-model.number="quotaReplenishAmounts[item.key]"
                  :placeholder="item.isDaily ? '调整每日额度' : '+ 补给额度'"
                  class="quota-replenish-input"
                  style="width: 100px;"
                />
                <button
                  class="spatial-btn spatial-btn-accent compact-btn"
                  style="min-height: 28px; font-size: 0.75rem; padding: 0 10px;"
                  :disabled="replenishingKeys.has(`${selectedUserForQuota?.id}-${item.key}`)"
                  @click="replenishQuota(item.key)"
                >
                  {{ replenishingKeys.has(`${selectedUserForQuota?.id}-${item.key}`) ? "处理中..." : (item.isDaily ? "调整" : "补给") }}
                </button>
              </div>
            </div>
          </div>

          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showQuotaDetailModal = false" style="width: 100%;">关闭</button>
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
            <input id="new-email" name="email" v-model="newUser.email" placeholder="you@papersolver.app" />
          </div>
          <div class="form-group" style="margin-top: 12px;">
            <label>角色</label>
            <select id="new-role" name="role" v-model="newUser.role" class="admin-select">
              <option value="普通用户">普通用户</option>
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

    <!-- Reset All Users Quota Modal -->
    <Transition name="fade">
      <div v-if="showResetModal" class="admin-modal-overlay" @click="showResetModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop>
          <h4 style="color: #ff3b30;">🚨 重置全体用户额度（安全校验）</h4>
          <p style="font-size: 13px; color: #60646c; margin-top: 8px; line-height: 1.6;">
            【警告】此操作将把所有用户的会员状态恢复至空闲免费版（重置全部使用频次，清空购买队列，清除过期时间），此操作不可逆！
          </p>
          <div class="form-group" style="margin-top: 16px;">
            <label>请输入管理员密钥</label>
            <input
              id="reset-secret-key"
              v-model="resetSecretKey"
              type="password"
              placeholder="管理员安全密钥"
              style="margin-top: 6px;"
              @keyup.enter="executeResetAllUsers"
            />
          </div>
          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showResetModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent danger-btn" style="background: #ff3b30; color: white;" @click="executeResetAllUsers">确定重置</button>
          </div>
        </div>
      </div>
    </Transition>
    <!-- Restore All Users Quota Modal -->
    <Transition name="fade">
      <div v-if="showRestoreModal" class="admin-modal-overlay" @click="showRestoreModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop>
          <h4 style="color: #4f46e5;">🛡️ 恢复全体会员额度（安全校验）</h4>
          <p style="font-size: 13px; color: #60646c; margin-top: 8px; line-height: 1.6;">
            【确认】此操作将清零所有用户的已用度数（把已用次数恢复为 0），以恢复用户各自当前所持有的所有开通会员额度。
          </p>
          <div class="form-group" style="margin-top: 16px;">
            <label>请输入管理员密钥</label>
            <input
              id="restore-secret-key"
              v-model="restoreSecretKey"
              type="password"
              placeholder="管理员安全密钥"
              style="margin-top: 6px;"
              @keyup.enter="executeRestoreAllQuotas"
            />
          </div>
          <div class="modal-actions" style="margin-top: 24px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showRestoreModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" style="background: #4f46e5; color: white;" @click="executeRestoreAllQuotas">确定恢复</button>
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
            <input id="recharge-email" name="recharge-email" v-model="newRecharge.email" placeholder="e.g. student@papersolver.app" />
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
          <header class="modal-header">
            <h5>团队详情</h5>
            <button class="modal-close" @click="showViewTeamModal = false">×</button>
          </header>

          <div class="modal-body">
            <!-- Team Info Cards Grid -->
            <div class="team-detail-summary">
              <div class="summary-card">
                <span>团队名称</span>
                <strong>{{ selectedTeam?.name }}</strong>
              </div>
              <div class="summary-card">
                <span>团队标识</span>
                <code>{{ selectedTeam?.identifier }}</code>
              </div>
              <div class="summary-card">
                <span>成员数量</span>
                <strong>{{ selectedTeamMembers.length }} 人</strong>
              </div>
            </div>

            <!-- Loading State -->
            <div v-if="teamMembersLoading" class="team-members-empty">
              <span class="loading-spinner"></span> 正在加载团队成员...
            </div>

            <!-- Table Section -->
            <div v-else class="team-members-table-wrap">
              <table class="admin-table">
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
                    <td>
                      <span class="role-badge" :class="getRoleClass(member.role)">{{ member.role }}</span>
                    </td>
                    <td>
                      <strong>Lv.{{ member.level }}</strong>
                      <small>{{ member.levelTitle }}</small>
                    </td>
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
            <article v-if="selectedForumReportDetail?.screenshot">
              <span>截图证据 (点击放大)</span>
              <div class="report-screenshot-preview" @click="openCampusImage(selectedForumReportDetail.screenshot, '举报截图证据')">
                <img :src="selectedForumReportDetail.screenshot" style="max-width: 100%; max-height: 150px; border-radius: 8px; cursor: pointer; border: 1px solid var(--spatial-line); object-fit: contain; margin-top: 4px; display: block;" />
              </div>
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
              <label>OpenAI 协议类型</label>
              <select v-model="newRelay.apiFormat" class="spatial-select">
                <option value="openai_chat">Chat Completions</option>
                <option value="openai_responses">Responses</option>
              </select>
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

    <!-- Ban User Modal -->
    <Transition name="fade">
      <div v-if="showBanUserModal" class="admin-modal-overlay" @click="showBanUserModal = false">
        <div class="admin-modal-card spatial-glass-panel" @click.stop style="max-width: 400px;">
          <h4>封禁用户账号</h4>
          <p class="form-hint" style="margin-top: 8px;">被封禁用户：<strong>{{ selectedBanUser?.username }}</strong></p>

          <div class="form-group" style="margin-top: 16px;">
            <label style="display: block; margin-bottom: 6px; font-weight: 700; color: var(--spatial-silver);">封禁时长</label>
            <select v-model="banUserDays" class="spatial-select" style="width: 100%;">
              <option :value="1">1 天 (24 小时)</option>
              <option :value="3">3 天 (72 小时)</option>
              <option :value="-1">永久封禁</option>
            </select>
          </div>

          <div class="form-group" style="margin-top: 16px;">
            <label style="display: block; margin-bottom: 6px; font-weight: 700; color: var(--spatial-silver);">封禁原因</label>
            <textarea v-model.trim="banUserReason" class="spatial-input" rows="3" style="width: 100%; min-height: 80px; resize: vertical; box-sizing: border-box; padding: 8px 10px;" placeholder="请输入封禁违规详情，例如：违规发布他人隐私身份信息"></textarea>
          </div>

          <div class="modal-footer" style="margin-top: 24px; display: flex; justify-content: flex-end; gap: 10px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showBanUserModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" style="background: #ef4444; border-color: #ef4444;" :disabled="!banUserReason" @click="submitBanUser">
              确定封禁
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Edit Relay Modal -->
    <Transition name="fade">
      <div v-if="showEditRelayModal" class="admin-modal-overlay" @click="showEditRelayModal = false">
        <div class="admin-modal-card spatial-glass-panel edit-relay-modal-card" @click.stop>
          <header class="modal-header">
            <div>
              <h5>配置中转代理连接</h5>
              <p class="subtitle">编辑中转服务商及其路由协议参数</p>
            </div>
            <button class="modal-close" @click="showEditRelayModal = false">×</button>
          </header>
          <div class="modal-body">
            <div class="form-group" style="margin-bottom: 12px;">
              <label style="display: block; margin-bottom: 6px; font-size: 0.8rem; font-weight: 700; color: var(--spatial-gray);">中转商户名称</label>
              <input v-model="editRelay.providerName" placeholder="例如: QiHang, Anthropic, SiliconFlow" class="spatial-input" style="width: 100%; box-sizing: border-box;" />
            </div>
            <div class="form-group" style="margin-bottom: 12px;">
              <label style="display: block; margin-bottom: 6px; font-size: 0.8rem; font-weight: 700; color: var(--spatial-gray);">接口代理地址 (Base URL)</label>
              <input v-model="editRelay.baseUrl" placeholder="例如: https://api.qihang.ai/v1" class="spatial-input" style="width: 100%; box-sizing: border-box;" />
            </div>
            <div class="form-group" style="margin-bottom: 12px;">
              <label style="display: block; margin-bottom: 6px; font-size: 0.8rem; font-weight: 700; color: var(--spatial-gray);">API Key / 凭证密钥</label>
              <input v-model="editRelay.apiKey" type="password" placeholder="留空则沿用原密钥，新输入则覆盖" class="spatial-input" autocomplete="new-password" style="width: 100%; box-sizing: border-box;" />
            </div>
            <div class="form-group" style="margin-bottom: 12px;">
              <label style="display: block; margin-bottom: 6px; font-size: 0.8rem; font-weight: 700; color: var(--spatial-gray);">OpenAI 协议类型</label>
              <select v-model="editRelay.apiFormat" class="spatial-select" style="width: 100%; box-sizing: border-box;">
                <option value="openai_chat">Chat Completions</option>
                <option value="openai_responses">Responses</option>
              </select>
            </div>
            <div class="form-group checkbox-group" style="margin-top: 14px;">
              <label class="scene-checkbox-label">
                <input type="checkbox" v-model="editRelay.fullUrl" />
                <span>使用完整自定义URL路径</span>
              </label>
            </div>
          </div>
          <footer class="modal-footer" style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px; border-top: 1px solid var(--spatial-line); padding-top: 16px;">
            <button class="spatial-btn spatial-btn-ghost" @click="showEditRelayModal = false">取消</button>
            <button class="spatial-btn spatial-btn-accent" :disabled="updatingRelay" @click="submitEditRelayConfig">
              {{ updatingRelay ? "保存中..." : "保存更改" }}
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

            <div v-if="loadingScenePool && scenePoolData.length === 0" class="loading-state">
              <span class="loading-spinner"></span> 数据更新中...
            </div>
            <div v-else-if="scenePoolData.length === 0" class="empty-state">
              号池为空，请在右侧模型列表中勾选对应模型加入
            </div>
            <div v-else class="pool-table-wrap" :class="{ 'pool-table-refreshing': loadingScenePool }">
              <table class="spatial-pool-table">
                <thead>
                  <tr>
                    <th>所属中转站</th>
                    <th>模型标识</th>
                    <th>延迟测速</th>
                    <th>状态</th>
                    <th style="width: 100px; text-align: center;">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(route, index) in scenePoolData"
                    :key="route.id"
                    draggable="true"
                    @dragstart="handleRouteDragStart($event, index)"
                    @dragover.prevent="handleRouteDragOver($event, index)"
                    @dragend="handleRouteDragEnd"
                    @drop="handleRouteDrop($event, index)"
                    :class="{ 'dragging-row': draggingIdx === index, 'drag-over-row': dragOverIdx === index }"
                  >
                    <td>
                      <div class="provider-info-cell">
                        <strong>{{ route.providerName }}</strong>
                        <span class="url-hint">{{ route.baseUrl }}</span>
                        <!-- Display error message if the route is failed/unhealthy -->
                        <span v-if="route.status !== 'available' && route.message" class="node-error-message">
                          {{ route.message }}
                        </span>
                      </div>
                    </td>
                    <td>
                      <code>{{ route.modelName }}</code>
                    </td>
                    <td>
                      <span class="node-latency" :class="{ error: route.status !== 'available' }">
                        {{ route.status === 'available' ? `${route.latencyMs}ms` : '故障' }}
                      </span>
                    </td>
                    <td>
                      <span class="status-badge" :class="route.status === 'available' ? 'available' : 'unavailable'">
                        {{ route.status === 'available' ? '可用' : '不可用' }}
                      </span>
                    </td>
                    <td style="text-align: center;">
                      <button
                        class="pool-cancel-btn"
                        @click="removeScenePoolRoute(route.id)"
                        title="将该节点移出当前号池"
                      >
                        取消号池
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
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
import { ref, onMounted, onBeforeUnmount, computed, watch, reactive, nextTick } from "vue";
import { sanitizeSiteMessageHtml } from "../utils/siteMessageHtml";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { useDialogStore } from "../stores/dialog";
import { paperpilotApi } from "../services/paperpilotApi";
import { useScrollReveal } from "../composables/useScrollReveal";
import ModelConfigPanel from "../components/ModelConfigPanel.vue";
import { useWorkspaceStore } from "../stores/workspace";
import AdminAiUsagePanel from "../components/AdminAiUsagePanel.vue";
import AdminMonitoringPanel from "../components/AdminMonitoringPanel.vue";

defineOptions({ name: "AdminView" });

const authStore = useAuthStore();
const dialogStore = useDialogStore();
const workspaceStore = useWorkspaceStore();
const router = useRouter();
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
  { value: "membershipPlans", label: "套餐管理", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><path d="M20 12v7a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-7"/><path d="M2 7h20v5H2z"/><path d="M12 22V7"/><path d="M12 7H7.5a2.5 2.5 0 1 1 0-5C11 2 12 7 12 7z"/><path d="M12 7h4.5a2.5 2.5 0 1 0 0-5C13 2 12 7 12 7z"/></svg>` },
  { value: "recharges", label: "充值入账记录", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>` },

  { value: "models", label: "AI 路由与模型", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><rect x="4" y="4" width="16" height="16" rx="2"/><rect x="9" y="9" width="6" height="6"/><path d="M9 1v3M15 1v3M9 20v3M15 20v3M20 9h3M20 15h3M1 9h3M1 15h3"/></svg>` },
  { value: "aiUsage", label: "AI 调用记录", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>` },
  { value: "monitoring", label: "管理员监控页面", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>` },
  { value: "translationIssues", label: "翻译问题日志", icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 15px; height: 15px;"><path d="M3 5h12"/><path d="M9 3v2c0 4-2 7-6 9"/><path d="M5 9c1 2 3 4 6 5"/><path d="M14 19l4-9 4 9"/><path d="M15.5 16h5"/></svg>` },
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
const translationIssuePage = ref(1);
const translationIssuePageSize = ref(10);
const translationIssueQuery = ref("");
const translationIssueProviderFilter = ref("全部");
const translationIssueStatusFilter = ref("全部");
const translationIssuesLoading = ref(false);
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
const membershipPlans = ref([]);
const membershipPlansLoading = ref(false);
const seckillTick = ref(Date.now());
let seckillTimer = null;
let userPresenceTimer = null;
const creatingMembershipPlan = ref(false);
const savingMembershipPlanIds = ref(new Set());
const deletingMembershipPlanIds = ref(new Set());
const promoCodes = reactive({});
const generatingPromo = reactive({});

function getPromoCodesForPlan(planId) {
  return Array.isArray(promoCodes[planId]) ? promoCodes[planId] : [];
}

function copyPromoCode(code) {
  navigator.clipboard?.writeText(code).then(() => {
    logAction(`已复制兑换码到剪贴板: ${code}`, "info");
  }).catch(() => {});
}

async function generatePromoCodeForPlan(planId) {
  if (generatingPromo[planId]) return;
  generatingPromo[planId] = true;
  try {
    const res = await paperpilotApi.generateAdminPromoCode(planId);
    if (res.success && res.code) {
      if (!Array.isArray(promoCodes[planId])) promoCodes[planId] = [];
      promoCodes[planId].unshift({ code: res.code, usedCount: 0, maxUses: 0, invalidated: false });
    } else {
      dialogStore.alert("生成兑换码失败，请重试");
    }
  } catch (error) {
    console.error("Failed to generate promo code:", error);
    dialogStore.alert(error.response?.data?.message || "生成兑换码失败");
  } finally {
    generatingPromo[planId] = false;
  }
}

async function invalidatePromoCode(codeObj) {
  if (!codeObj?.id || codeObj.invalidated) return;
  const ok = await dialogStore.confirm(`确定作废兑换码 ${codeObj.code} 吗？作废后不可恢复。`, { title: "作废兑换码", confirmText: "作废", danger: true });
  if (!ok) return;
  try {
    await paperpilotApi.invalidateAdminPromoCode(codeObj.id);
    codeObj.invalidated = true;
    dialogStore.toast("兑换码已作废");
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "作废兑换码失败");
  }
}



function addPlanExtraFeature(plan) {
  if (!Array.isArray(plan._extraFeatures)) plan._extraFeatures = [];
  plan._extraFeatures.push({ label: "", value: "-", included: true });
}

function removePlanExtraFeature(plan, index) {
  if (Array.isArray(plan._extraFeatures)) {
    plan._extraFeatures.splice(index, 1);
  }
}

// Initialize scroll reveal animations
useScrollReveal(".admin-page");

// Modals
const showMembershipModal = ref(false);
const showBatchMembershipModal = ref(false);
const showAddUserModal = ref(false);
const showResetModal = ref(false);
const resetSecretKey = ref("");
const showRestoreModal = ref(false);
const restoreSecretKey = ref("");
const showBanUserModal = ref(false);
const selectedBanUser = ref(null);
const banUserDays = ref(1);
const banUserReason = ref("违规发布他人隐私身份信息");
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
const selectedUserIds = ref(new Set());
const batchMembershipPlan = ref("free");
const batchMembershipCycle = ref("monthly");
const batchMembershipSaving = ref(false);
const batchQuota = reactive({ fruitScore: "", translate: "", immersive: "", ppt: "", chat: "", research: "", review: "", agent: "" });
const selectedTeam = ref(null);
const selectedTeamMembers = ref([]);
const teamMembersLoading = ref(false);
const selectedPaymentTicket = ref(null);
const paymentTicketDecision = ref("processed");
const paymentTicketNote = ref("");
const paymentTicketSaving = ref(false);
const paymentTicketsTotal = ref(0);
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
  role: "普通用户",
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
const siteMessageColors = ["#000000", "#60a5fa", "#34d399", "#fbbf24", "#fb7185", "#c084fc", "#f8fafc", "#94a3b8"];
const siteMessageEditor = ref(null);
const newSiteMessage = ref({ title: "", content: "", messageType: "notice", imageUrl: "", textColor: "#000000" });
const editingSiteMessageId = ref(null);
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
const translationIssues = ref([]);
const translationIssueSummary = ref({});
const translationJobs = ref([]);
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
const relayModelsError = ref("");
const testingRelay = ref(false);
const relayPingResult = ref(null); // { latencyMs, success, message }

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
  apiFormat: "openai_chat",
});

const showEditRelayModal = ref(false);
const editRelay = ref({
  id: null,
  providerName: "",
  baseUrl: "",
  apiKey: "",
  modelName: "gpt-4o",
  apiFormat: "openai_chat",
  authType: "bearer",
  fullUrl: false
});

const isProvidersCollapsed = ref(false);

const currentPage = ref(1);
const pageSize = ref(12);

const modelSearchQuery = ref("");
const modelSceneFilter = ref("all");

const filteredModels = computed(() => {
  if (!activeRelay.value || !relayModels.value) return [];

  let list = relayModels.value;

  // 1. Search Query Filter (Case-insensitive matches model.id)
  const query = modelSearchQuery.value.trim().toLowerCase();
  if (query) {
    list = list.filter(model => model.id.toLowerCase().includes(query));
  }

  // 2. Scene Assignment or Availability Filter
  const filterVal = modelSceneFilter.value;
  if (filterVal !== "all") {
    if (filterVal === "available") {
      list = list.filter(model => {
        const testRes = modelTestResults.value[model.id];
        return testRes && !testRes.testing && testRes.success;
      });
    } else {
      list = list.filter(model => {
        const modelKey = `${activeRelay.value.providerName.toLowerCase()}|${activeRelay.value.baseUrl.toLowerCase()}|${model.id}`;
        const assignments = assignedScenesMap.value[modelKey] || {};

        if (filterVal === "none") {
          // Models that have no scenes selected (all values false or undefined)
          return !Object.values(assignments).some(val => val === true);
        } else if (filterVal === "any") {
          // Models that have at least one scene selected
          return Object.values(assignments).some(val => val === true);
        } else {
          // Specific scene is selected
          return assignments[filterVal] === true;
        }
      });
    }
  }

  // Sort by latency from smallest to largest by default
  return [...list].sort((a, b) => {
    const resA = modelTestResults.value[a.id];
    const resB = modelTestResults.value[b.id];

    const hasLatencyA = resA && !resA.testing && resA.success && typeof resA.latencyMs === 'number';
    const hasLatencyB = resB && !resB.testing && resB.success && typeof resB.latencyMs === 'number';

    if (hasLatencyA && hasLatencyB) {
      return resA.latencyMs - resB.latencyMs;
    }
    if (hasLatencyA) return -1;
    if (hasLatencyB) return 1;

    return a.id.localeCompare(b.id);
  });
});

const paginatedModels = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredModels.value.slice(start, end);
});

const totalPages = computed(() => Math.ceil(filteredModels.value.length / pageSize.value));

watch([modelSearchQuery, modelSceneFilter, pageSize], () => {
  currentPage.value = 1;
});

const visiblePages = computed(() => {
  const pages = [];
  const total = totalPages.value;
  const current = currentPage.value;

  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i);
  } else {
    pages.push(1);
    if (current > 4) pages.push('...');

    const start = Math.max(2, current - 2);
    const end = Math.min(total - 1, current + 2);
    for (let i = start; i <= end; i++) pages.push(i);

    if (current < total - 3) pages.push('...');
    pages.push(total);
  }
  return pages;
});

const showErrorDetail = ref({});
function toggleErrorDetail(modelId) {
  showErrorDetail.value[modelId] = !showErrorDetail.value[modelId];
}

const activePoolScene = ref(null);
const scenePoolData = ref([]);
const loadingScenePool = ref(false);
let modelRelaysCacheAt = 0;
let modelPoolsCacheAt = 0;
const ADMIN_MODEL_CACHE_TTL_MS = 45_000;

const modelScene = ref("paper_review");
const modelSceneOptions = [
  { value: "paper_quiz", label: "学术自测", hint: "根据论文生成自测题库、评阅简答题及提供原文依据" },
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
    value: "image_analysis",
    label: "图片分析",
    hint: "论文图表、流程图和数据图像的视觉理解",
  },
  {
    value: "reading_notes",
    label: "阅读笔记",
    hint: "生成阅读笔记与多套分析模板内容",
  },
  {
    value: "meeting_deck",
    label: "PPT生成",
    hint: "必须配置 gpt-5.4 级别强模型",
  },
  {
    value: "backup",
    label: "备用号池",
    hint: "所有业务模块模型全部失效后的最终备选降级通道",
  },
  {
    value: "free_pool",
    label: "免费号池",
    hint: "用户可直接选择使用，不消耗积分；仅展示已配置且可用的模型",
  },
];
const aiUsageSceneOptions = [
  { value: "paper_quiz", label: "学术自测" },
  { value: "paper_review", label: "论文综述" },
  { value: "paper_qa", label: "AI论文问答" },
  { value: "reading_notes", label: "阅读笔记" },
  { value: "meeting_deck", label: "PPT生成" },
  { value: "image_analysis", label: "图片分析" },
  { value: "backup", label: "备用号池" },
  { value: "free_pool", label: "免费号池" },
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
  const query = rechargeQuery.value.toLowerCase();
  return rechargeRecords.value.filter(r => {
    return String(r.email || "").toLowerCase().includes(query) ||
           String(r.username || "").toLowerCase().includes(query) ||
           String(r.numericId || "").toLowerCase().includes(query);
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
const assignableMembershipPlans = computed(() => membershipPlans.value.filter((plan) => plan.id !== "free" && plan.activeFlag !== false));
const subscriptionPlans = computed(() => membershipPlans.value);
const activeSeckillCount = computed(() => subscriptionPlans.value.filter((plan) => plan.seckillActive).length);
const userPageCount = computed(() => getPageCount(filteredUsers.value.length, userPageSize.value));
const ticketPageCount = computed(() => getPageCount(paymentTicketsTotal.value || paymentTickets.value.length, ticketPageSize.value));
const orderPageCount = computed(() => getPageCount(filteredPaymentOrders.value.length, orderPageSize.value));
const rechargePageCount = computed(() => getPageCount(filteredRecharges.value.length, rechargePageSize.value));
const logPageCount = computed(() => getPageCount(systemLogs.value.length, logPageSize.value));
const translationIssueProviders = computed(() => [...new Set(translationIssues.value.map(issue => issue.provider).filter(Boolean))].sort());
const filteredTranslationIssues = computed(() => {
  const query = translationIssueQuery.value.toLowerCase();
  return translationIssues.value.filter(issue => {
    if (translationIssueProviderFilter.value !== "全部" && issue.provider !== translationIssueProviderFilter.value) {
      return false;
    }
    if (translationIssueStatusFilter.value === "failed" && issue.success) {
      return false;
    }
                    if (translationIssueStatusFilter.value === "slow"
                      && (!issue.success || Number(issue.latencyMs || 0) < (translationIssueSummary.value?.slowThresholdMs || 1800))) {
      return false;
    }
    if (!query) {
      return true;
    }
    return [
      issue.paperTitle,
      issue.translationMode,
      issue.provider,
      issue.route,
      issue.username,
      issue.email,
      issue.numericId,
      issue.ipAddress,
      issue.errorMessage,
      issue.networkProfile,
      issue.userAgent,
    ].some(value => String(value || "").toLowerCase().includes(query));
  });
});
const translationIssuePageCount = computed(() => getPageCount(filteredTranslationIssues.value.length, translationIssuePageSize.value));
const forumReportPageCount = computed(() => getPageCount(forumReports.value.length, forumReportPageSize.value));
const campusVerificationPageCount = computed(() => getPageCount(campusVerifications.value.length, campusVerificationPageSize.value));
const siteMessagePageCount = computed(() => getPageCount(siteMessages.value.length, siteMessagePageSize.value));
const tutorialPageCount = computed(() => getPageCount(tutorials.value.length, tutorialPageSize.value));
const paginatedUsers = computed(() => paginateRows(filteredUsers.value, userPage.value, userPageSize.value));
const allCurrentPageUsersSelected = computed(() => paginatedUsers.value.length > 0 && paginatedUsers.value.every(user => selectedUserIds.value.has(user.id)));
const paginatedPaymentTickets = computed(() => paymentTickets.value);
const filteredPaymentOrders = computed(() => paymentOrders.value.filter(o => o.status !== 'pending_payment'));
const paginatedPaymentOrders = computed(() => paginateRows(filteredPaymentOrders.value, orderPage.value, orderPageSize.value));
const paginatedRecharges = computed(() => paginateRows(filteredRecharges.value, rechargePage.value, rechargePageSize.value));
const paginatedSystemLogs = computed(() => paginateRows(systemLogs.value, logPage.value, logPageSize.value));
const paginatedTranslationIssues = computed(() => paginateRows(filteredTranslationIssues.value, translationIssuePage.value, translationIssuePageSize.value));
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
watch([ticketPageSize, orderPageSize, logPageSize, translationIssuePageSize, forumReportPageSize, siteMessagePageSize, tutorialPageSize], () => {
  ticketPage.value = 1;
  orderPage.value = 1;
  logPage.value = 1;
  translationIssuePage.value = 1;
  forumReportPage.value = 1;
  siteMessagePage.value = 1;
  tutorialPage.value = 1;
});
watch([translationIssueQuery, translationIssueProviderFilter, translationIssueStatusFilter], () => {
  translationIssuePage.value = 1;
});
watch(ticketPage, () => {
  loadPaymentWorkdesk();
});
watch(userPageCount, () => keepPageInRange(userPage, userPageCount));
watch(ticketPageCount, () => keepPageInRange(ticketPage, ticketPageCount));
watch(orderPageCount, () => keepPageInRange(orderPage, orderPageCount));
watch(rechargePageCount, () => keepPageInRange(rechargePage, rechargePageCount));
watch(logPageCount, () => keepPageInRange(logPage, logPageCount));
watch(translationIssuePageCount, () => keepPageInRange(translationIssuePage, translationIssuePageCount));
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
  image_analysis: "图片分析独立配置，只用于论文图表、流程图和实验图片。请配置支持视觉输入的模型。",
  meeting_deck: "PPT 生成使用独立强模型配置，必须配置 gpt-5.4 或更强模型；不会影响问答和审核。",
})[modelScene.value] || "模型入口独立配置，避免高成本任务和轻量任务混用。");
const modelPoolDescription = computed(() => ({
  paper_review: "这里只管理论文综述模型池。建议主路由选稳定强模型，备用路由选便宜模型。",
  paper_qa: "这里只管理 AI 论文问答模型池。遇到限流、超时或上游失败时，会在问答池内尝试备用路由。",
  image_analysis: "这里只管理图片分析模型池。用户分析论文图表时只会读取此处支持视觉输入的模型。",
  meeting_deck: "这里只管理 PPT 生成专用池。PPT 多轮 Agent 只会读取这个池子，主模型建议 gpt-5.4。",
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
  const orderA = Number(a.sortOrder ?? a.manualOrder ?? a.orderIndex);
  const orderB = Number(b.sortOrder ?? b.manualOrder ?? b.orderIndex);
  const priorityA = parseInt(String(a.priority ?? "99999"), 10);
  const priorityB = parseInt(String(b.priority ?? "99999"), 10);
  const manualA = Number.isFinite(orderA) ? orderA : Number.isFinite(priorityA) ? priorityA : 99999;
  const manualB = Number.isFinite(orderB) ? orderB : Number.isFinite(priorityB) ? priorityB : 99999;
  if (manualA !== manualB) return manualA - manualB;
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

async function loadPaymentWorkdesk() {
  const paymentsData = await paperpilotApi.getAdminPayments({
    ticketPage: ticketPage.value,
    ticketPageSize: ticketPageSize.value,
  });
  paymentOrders.value = (paymentsData.orders || []).slice().sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
  paymentTickets.value = paymentsData.tickets || [];
  paymentTicketsTotal.value = Number(paymentsData.ticketsTotal ?? paymentTickets.value.length) || 0;
  const totalPages = Math.max(1, Number(paymentsData.ticketsTotalPages || 1));
  if (ticketPage.value > totalPages) {
    ticketPage.value = totalPages;
  }
}

function normalizeAdminUser(u) {
  return {
    id: u.id,
    username: u.username || "—",
    numericId: u.numericId || "—",
    ip: u.lastIp || "—",
    role: u.role || "普通用户",
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
    researchQuota: u.researchQuota || 0,
    researchUsed: u.researchUsed || 0,
    reportQuota: u.reportQuota || 0,
    agentTokenQuota: u.agentTokenQuota || u.tokenLimit || 0,
    reportUsed: u.reportUsed || 0,
    translateQuota: u.translateQuota || 0,
    translateUsed: u.translateUsed || 0,
    immersiveQuota: u.immersiveQuota || 0,
    immersiveUsed: u.immersiveUsed || 0,
    fruitScore: u.fruitScore || 0,
    createdTime: formatDate(u.createdAt),
    qqOpenid: u.qqOpenid,
    avatarUrl: u.avatarUrl,
    isOnline: Boolean(u.isOnline),
  };
}

async function refreshAdminUsers() {
  const usersData = await paperpilotApi.getAdminUsers();
  systemUsers.value = usersData.map(normalizeAdminUser);
}

// Fetch all admin data
async function fetchAllData() {
  try {
    await workspaceStore.hydrateFromBackend(modelScene.value);
    // 1. Fetch Users
    await refreshAdminUsers();

    // 2. Fetch Recharge Records
    const rechargesData = await paperpilotApi.getRechargeRecords();
    rechargeRecords.value = rechargesData.map(r => ({
      id: r.id,
      email: r.email,
      username: r.username || "未知用户",
      numericId: r.numericId || "—",
      amount: r.amount || 0,
      tokens: r.tokens || 0,
      time: formatDateTime(r.createdAt),
    }));

    await loadPaymentWorkdesk();
    await loadMembershipPlans(false);

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
      time: l.timestamp ? formatDateTime(l.timestamp) : "—",
      level: l.level || "info",
      message: l.message || "",
    }));
    await loadTranslationIssues(false);

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

function normalizePlanDatetime(value) {
  if (!value) return "";
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0] = value;
    return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}T${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
  }
  const text = String(value);
  return text.length >= 16 ? text.slice(0, 16) : text;
}

function normalizeMembershipPlan(plan) {
  return {
    ...plan,
    activeFlag: plan.activeFlag !== false,
    pluginImportEnabled: plan.pluginImportEnabled !== false,
    agentEnabled: plan.agentEnabled !== false,
    reviewEnabled: plan.reviewEnabled !== false,
    chatEnabled: plan.chatEnabled !== false,
    teamShared: Boolean(plan.teamShared),
    forumSpecial: Boolean(plan.forumSpecial),
    peakPriority: Boolean(plan.peakPriority),
    topUpPack: Boolean(plan.topUpPack),
    seckillEnabled: Boolean(plan.seckillEnabled),
    seckillStartsAt: normalizePlanDatetime(plan.seckillStartsAt),
    seckillEndsAt: normalizePlanDatetime(plan.seckillEndsAt),
  };
}

function isPlanQuotaEnabled(plan, key) {
  return Number(plan?.[key] || 0) > 0;
}

function togglePlanQuotaFeature(plan, key, enabled) {
  if (!plan) return;
  if (!enabled) {
    plan[key] = 0;
    return;
  }
  const defaults = {
    agentTokenQuota: 20,
    translateQuota: 5,
    immersiveQuota: 3,
    pptQuota: 0,
    forumTopDaily: 1,
  };
  if (Number(plan[key] || 0) <= 0) {
    plan[key] = defaults[key] || 1;
  }
}

async function loadMembershipPlans(showLoading = true) {
  if (showLoading) membershipPlansLoading.value = true;
  try {
    const rows = await paperpilotApi.getAdminMembershipPlans();
    // Exclude team plans and retired top-up packages from admin management list.
    membershipPlans.value = (rows || [])
      .filter((plan) => plan.id !== "team_plus" && plan.id !== "team_pro")
      .map(normalizeMembershipPlan);

    try {
      const codes = await paperpilotApi.getAdminPromoCodes();
      for (const k in promoCodes) {
        delete promoCodes[k];
      }
      if (Array.isArray(codes)) {
        codes.forEach(c => {
          if (c.planId && c.code) {
            if (!Array.isArray(promoCodes[c.planId])) promoCodes[c.planId] = [];
            if (!promoCodes[c.planId].some(item => (typeof item === 'string' ? item : item.code) === c.code)) {
              promoCodes[c.planId].push(c);
            }
          }
        });
      }
    } catch (e) {
      console.warn("Failed to load promo codes:", e);
    }
  } catch (error) {
    console.error("Failed to load membership plans:", error);
    dialogStore.alert(error.response?.data?.message || "套餐配置加载失败");
  } finally {
    membershipPlansLoading.value = false;
  }
}

async function createMembershipPlan() {
  if (creatingMembershipPlan.value) return;
  creatingMembershipPlan.value = true;
  try {
    const stamp = Date.now().toString(36);
    const saved = await paperpilotApi.createAdminMembershipPlan({
      id: `custom_${stamp}`,
      name: "新会员套餐",
      subtitle: "自定义上架套餐",
      monthlyPrice: 19.9,
      originalMonthlyPrice: 29.9,
      sortOrder: (membershipPlans.value.length + 1) * 10,
      activeFlag: true,
      topUpPack: false,
    });
    membershipPlans.value.push(normalizeMembershipPlan(saved));
    dialogStore.alert("新套餐已上架，可继续编辑价格、权益和秒杀配置。");
  } catch (error) {
    console.error("Failed to create membership plan:", error);
    dialogStore.alert(error.response?.data?.message || "新套餐创建失败");
  } finally {
    creatingMembershipPlan.value = false;
  }
}

function canDeleteMembershipPlan(plan) {
  return plan?.id && !["free", "lite", "plus", "pro", "max", "team_plus", "team_pro"].includes(plan.id);
}

async function toggleMembershipPlanActive(plan) {
  const nextActive = plan.activeFlag === false;
  const rollbackActive = plan.activeFlag;
  const next = new Set(savingMembershipPlanIds.value);
  next.add(plan.id);
  savingMembershipPlanIds.value = next;
  try {
    const saved = await paperpilotApi.updateAdminMembershipPlan(plan.id, { activeFlag: nextActive });
    const index = membershipPlans.value.findIndex((item) => item.id === plan.id);
    if (index >= 0) membershipPlans.value[index] = normalizeMembershipPlan(saved);
    window.dispatchEvent(new CustomEvent("paperpilot:membership-plans-changed", { detail: { plan: saved } }));
    dialogStore.alert(nextActive ? "套餐已上架，购买页将立即展示。" : "套餐已下架，购买页将立即隐藏。", { title: "套餐状态已同步" });
  } catch (error) {
    plan.activeFlag = rollbackActive;
    console.error("Failed to toggle membership plan:", error);
    dialogStore.alert(error.response?.data?.message || "套餐上下架状态更新失败");
  } finally {
    const done = new Set(savingMembershipPlanIds.value);
    done.delete(plan.id);
    savingMembershipPlanIds.value = done;
  }
}

async function deleteMembershipPlan(plan) {
  if (!canDeleteMembershipPlan(plan)) {
    dialogStore.alert("系统预置套餐只能隐藏，不能彻底删除。");
    return;
  }
  const ok = await dialogStore.confirm(`确认彻底删除套餐「${plan.name || plan.id}」吗？删除后用户购买页将不再展示。`, {
    title: "删除套餐",
    confirmText: "彻底删除",
    cancelText: "取消",
    danger: true,
  });
  if (!ok) return;
  const next = new Set(deletingMembershipPlanIds.value);
  next.add(plan.id);
  deletingMembershipPlanIds.value = next;
  try {
    await paperpilotApi.deleteAdminMembershipPlan(plan.id);
    membershipPlans.value = membershipPlans.value.filter((item) => item.id !== plan.id);
    dialogStore.alert("套餐已彻底删除。");
  } catch (error) {
    console.error("Failed to delete membership plan:", error);
    dialogStore.alert(error.response?.data?.message || "套餐删除失败");
  } finally {
    const done = new Set(deletingMembershipPlanIds.value);
    done.delete(plan.id);
    deletingMembershipPlanIds.value = done;
  }
}

async function saveMembershipPlan(plan) {
  const ok = await dialogStore.confirm(`确认保存套餐「${plan.name || plan.id}」的价格、额度和秒杀配置吗？`, {
    title: "保存套餐配置",
    confirmText: "保存",
    cancelText: "取消",
  });
  if (!ok) return;
  const next = new Set(savingMembershipPlanIds.value);
  next.add(plan.id);
  savingMembershipPlanIds.value = next;
  try {
    const payload = {
      name: plan.name,
      subtitle: plan.subtitle,
      monthlyPrice: Number(plan.monthlyPrice || 0),
      // The public plan has one price. Keep the legacy database field aligned
      // for older clients, but do not expose a second price in the UI.
      originalMonthlyPrice: Number(plan.monthlyPrice || 0),
      agentTokenQuota: Number(plan.agentTokenQuota || plan.tokenQuota || 0),
      pluginImportEnabled: plan.pluginImportEnabled !== false,
      agentEnabled: plan.agentEnabled !== false,
      reviewEnabled: plan.reviewEnabled !== false,
      chatEnabled: plan.chatEnabled !== false,
      reviewQuota: 0,
      pptQuota: Number(plan.pptQuota || 0),
      chatQuota: 0,
      translateQuota: Number(plan.translateQuota || 0),
      immersiveQuota: Number(plan.immersiveQuota || 0),
      translateQuotaDaily: Number(plan.translateQuotaDaily ?? plan.translateQuota ?? 0),
      immersiveQuotaDaily: Number(plan.immersiveQuotaDaily ?? plan.immersiveQuota ?? 0),
      researchQuota: 0,
      reportQuota: 0,
      teamSeats: Number(plan.teamSeats || 0),
      teamShared: Boolean(plan.teamShared),
      forumSpecial: Boolean(plan.forumSpecial),
      forumTopDaily: Number(plan.forumTopDaily || 0),
      peakPriority: Boolean(plan.peakPriority),
      topUpPack: Boolean(plan.topUpPack),
      activeFlag: plan.activeFlag !== false,
      sortOrder: Number(plan.sortOrder || 99),
      seckillEnabled: Boolean(plan.seckillEnabled),
      seckillPrice: plan.seckillPrice === "" || plan.seckillPrice == null ? null : Number(plan.seckillPrice),
      seckillStartsAt: plan.seckillStartsAt || null,
      seckillEndsAt: plan.seckillEndsAt || null,
      seckillLabel: plan.seckillLabel || "限时秒杀",
    };
    const saved = await paperpilotApi.updateAdminMembershipPlan(plan.id, payload);
    const index = membershipPlans.value.findIndex((item) => item.id === plan.id);
    if (index >= 0) membershipPlans.value[index] = normalizeMembershipPlan(saved);
    dialogStore.alert("套餐配置已保存，会员购买页会自动同步。");
  } catch (error) {
    console.error("Failed to save membership plan:", error);
    dialogStore.alert(error.response?.data?.message || "套餐保存失败");
  } finally {
    const done = new Set(savingMembershipPlanIds.value);
    done.delete(plan.id);
    savingMembershipPlanIds.value = done;
  }
}

function isPlanSeckillActive(plan) {
  if (!plan.seckillEnabled) return false;
  const startStr = plan.seckillStartsAt;
  const endStr = plan.seckillEndsAt;
  if (!startStr && !endStr) return true;
  const now = new Date();

  if (startStr) {
    const start = new Date(startStr);
    if (start > now) return false;
  }
  if (endStr) {
    const end = new Date(endStr);
    if (end <= now) return false;
  }
  return true;
}

function getPlanSeckillCountdownText(plan) {
  seckillTick.value;
  if (!plan.seckillEndsAt) return "已开启";
  const end = new Date(plan.seckillEndsAt);
  const now = new Date();
  const diffMs = Math.max(0, end.getTime() - now.getTime());
  return formatAdminCountdown(diffMs);
}

function formatAdminCountdown(milliseconds) {
  const value = Math.max(0, Math.floor(Number(milliseconds || 0)));
  const days = Math.floor(value / 86400000);
  const hours = Math.floor((value % 86400000) / 3600000);
  const minutes = Math.floor((value % 3600000) / 60000);
  const remainingSeconds = Math.floor((value % 60000) / 1000);
  const remainingMilliseconds = value % 1000;
  return `${days}天 ${hours}小时 ${minutes}分钟 ${remainingSeconds}秒 ${String(remainingMilliseconds).padStart(3, "0")}毫秒`;
}

function planPreview(plan) {
  const price = Number(plan.seckillEnabled && plan.seckillPrice != null ? plan.seckillPrice : plan.monthlyPrice || 0).toFixed(2);
  return `展示价 ¥${price}/月 · 对照 ${plan.translateQuota || 0} 篇 · 沉浸 ${plan.immersiveQuota || 0} 篇 · PPT ${plan.pptQuota || 0} 次`;
}

onMounted(() => {
  seckillTimer = window.setInterval(() => { seckillTick.value = Date.now(); }, 40);
  fetchAllData();
  userPresenceTimer = window.setInterval(() => {
    refreshAdminUsers().catch(() => {});
  }, 30000);
});

onBeforeUnmount(() => {
  if (seckillTimer) window.clearInterval(seckillTimer);
  seckillTimer = null;
  if (userPresenceTimer) window.clearInterval(userPresenceTimer);
  userPresenceTimer = null;
});

watch(activeTab, async (value) => {
  if (value === "models") {
    await loadRelays();
    await loadAllScenePools();
  }
  if (value === "membershipPlans" && !membershipPlans.value.length) {
    await loadMembershipPlans();
  }
  if (value === "translationIssues") {
    await loadTranslationIssues();
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
async function loadRelays(force = false) {
  if (!force && modelRelaysCacheAt && Date.now() - modelRelaysCacheAt < ADMIN_MODEL_CACHE_TTL_MS && relays.value.length) {
    return relays.value;
  }
  loadingRelays.value = true;
  try {
    const relayScenes = ["general", ...modelSceneOptions.map(scene => scene.value)];
    const pools = await Promise.allSettled(relayScenes.map(scene => paperpilotApi.getModelPool(scene)));
    const uniqueRelays = [];
    const seen = new Set();
    for (const result of pools) {
      if (result.status !== "fulfilled" || !Array.isArray(result.value)) continue;
      for (const item of result.value) {
        if (item.template) continue;
        const key = `${item.providerName.toLowerCase()}|${item.baseUrl.toLowerCase()}`;
        if (!seen.has(key)) {
          seen.add(key);
          uniqueRelays.push(item);
        }
      }
    }
    relays.value = uniqueRelays;
    const activeKey = activeRelay.value
      ? `${activeRelay.value.providerName.toLowerCase()}|${activeRelay.value.baseUrl.toLowerCase()}`
      : "";
    if (uniqueRelays.length > 0 && (!activeRelay.value || !seen.has(activeKey))) {
      activeRelay.value = uniqueRelays[0];
    } else if (!uniqueRelays.length) {
      activeRelay.value = null;
    }
    modelRelaysCacheAt = Date.now();
    return relays.value;
  } catch (e) {
    console.error("Failed to load relays:", e);
  } finally {
    loadingRelays.value = false;
  }
}

async function loadAllScenePools(force = false) {
  if (!force && modelPoolsCacheAt && Date.now() - modelPoolsCacheAt < ADMIN_MODEL_CACHE_TTL_MS && Object.keys(allScenesPoolData.value).length) {
    return allScenesPoolData.value;
  }
  const scenes = modelSceneOptions.map(scene => scene.value);
  const newMap = {};
  const newPoolData = {};

  // Construct a set of active provider keys for fast lookup
  const activeRelayKeys = new Set(
    relays.value.map(r => `${r.providerName.toLowerCase()}|${r.baseUrl.toLowerCase()}`)
  );

  for (const scene of scenes) {
    try {
      const pool = await paperpilotApi.getModelPool(scene);

      // Filter out pool items that belong to relays that no longer exist
      const activePool = pool.filter(item => {
        if (item.template) return false;
        const routeKey = `${item.providerName.toLowerCase()}|${item.baseUrl.toLowerCase()}`;
        return activeRelayKeys.has(routeKey);
      });

      newPoolData[scene] = activePool;
      for (const item of activePool) {
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
  modelPoolsCacheAt = Date.now();
  return allScenesPoolData.value;
}

async function loadRelayModels(relay) {
  if (!relay) {
    relayModels.value = [];
    relayModelsError.value = "";
    return;
  }
  loadingModels.value = true;
  relayModels.value = [];
  relayModelsError.value = "";
  modelTestResults.value = {};
  try {
    const res = await paperpilotApi.fetchRelayRouteModels(relay.id);
    if (res?.success === false) {
      relayModelsError.value = res.message || "模型列表接口返回失败，请检查 Base URL、模型列表地址和 API Key。";
      relayModels.value = [];
    } else if (res && res.models) {
      relayModels.value = res.models;
      if (!res.models.length) {
        relayModelsError.value = res.message || "接口返回成功但模型列表为空。";
      }
    }
  } catch (e) {
    console.error("Failed to load relay models:", e);
    relayModelsError.value = e.response?.data?.message || e.message || "模型列表读取失败。";
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
  const ok = await dialogStore.confirm(`确定删除中转站 "${relay.providerName}" 吗？此操作将清除其在所有模块的模型池记录。`, {
    confirmText: "删除",
  });
  if (!ok) return;
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
  if (!newRelay.value.providerName || !newRelay.value.baseUrl) {
    dialogStore.alert("请填写完整信息（商户名称、接口代理地址）");
    return;
  }
  submittingNewRelay.value = true;
  try {
    const payload = {
      providerName: newRelay.value.providerName,
      baseUrl: newRelay.value.baseUrl,
      apiKey: newRelay.value.apiKey || "",
      modelName: "gpt-4o", // Send "gpt-4o" under-the-hood so backend is happy
      scene: "general",
      apiFormat: newRelay.value.apiFormat || "openai_chat",
      authType: "bearer",
      fullUrl: false
    };
    await paperpilotApi.saveModelConfig(payload);
    showAddRelayModal.value = false;
    newRelay.value = { providerName: "", baseUrl: "", apiKey: "", modelName: "gpt-4o", apiFormat: "openai_chat" };
    await loadRelays();
  } catch (e) {
    dialogStore.alert("保存失败: " + (e.response?.data?.message || e.message));
  } finally {
    submittingNewRelay.value = false;
  }
}

function openEditRelayModal(relay) {
  editRelay.value = {
    id: relay.id,
    providerName: relay.providerName,
    baseUrl: relay.baseUrl,
    apiKey: "", // placeholder for new key, empty by default
    modelName: relay.modelName || "gpt-4o",
    apiFormat: relay.apiFormat || "openai_chat",
    authType: relay.authType || "bearer",
    fullUrl: relay.fullUrl || false
  };
  showEditRelayModal.value = true;
}

async function submitEditRelayConfig() {
  if (!editRelay.value.providerName || !editRelay.value.baseUrl) {
    dialogStore.alert("请填写完整信息（名称、接口地址）");
    return;
  }
  updatingRelay.value = true;
  try {
    const payload = {
      providerName: editRelay.value.providerName,
      baseUrl: editRelay.value.baseUrl,
      apiKey: editRelay.value.apiKey || "",
      modelName: "gpt-4o", // Send "gpt-4o" under-the-hood so backend is happy
      scene: "general",
      apiFormat: editRelay.value.apiFormat,
      authType: editRelay.value.authType,
      fullUrl: editRelay.value.fullUrl
    };
    await paperpilotApi.saveModelConfig(payload);
    dialogStore.toast("配置更新成功！");
    showEditRelayModal.value = false;
    await loadRelays();
    if (activeRelay.value && activeRelay.value.providerName === editRelay.value.providerName) {
      activeRelay.value = relays.value.find(r => r.providerName === editRelay.value.providerName);
    }
  } catch (e) {
    dialogStore.alert("更新失败: " + (e.response?.data?.message || e.message));
  } finally {
    updatingRelay.value = false;
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
    const rawData = await paperpilotApi.getModelPool(activePoolScene.value);
    const activeRelayKeys = new Set(
      relays.value.map(r => `${r.providerName.toLowerCase()}|${r.baseUrl.toLowerCase()}`)
    );
    scenePoolData.value = sortPoolRows(rawData.filter(item => {
      if (item.template) return false;
      const routeKey = `${item.providerName.toLowerCase()}|${item.baseUrl.toLowerCase()}`;
      return activeRelayKeys.has(routeKey);
    }));
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
    const rawData = await paperpilotApi.refreshModelPool(activePoolScene.value);
    const activeRelayKeys = new Set(
      relays.value.map(r => `${r.providerName.toLowerCase()}|${r.baseUrl.toLowerCase()}`)
    );
    scenePoolData.value = sortPoolRows(rawData.filter(item => {
      if (item.template) return false;
      const routeKey = `${item.providerName.toLowerCase()}|${item.baseUrl.toLowerCase()}`;
      return activeRelayKeys.has(routeKey);
    }));
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

function sortPoolRows(rows = []) {
  return [...rows].sort((a, b) => {
    const orderA = Number(a.sortOrder ?? a.manualOrder ?? a.orderIndex);
    const orderB = Number(b.sortOrder ?? b.manualOrder ?? b.orderIndex);
    const priorityA = parseInt(String(a.priority ?? "99999"), 10);
    const priorityB = parseInt(String(b.priority ?? "99999"), 10);
    const resolvedA = Number.isFinite(orderA) ? orderA : Number.isFinite(priorityA) ? priorityA : 99999;
    const resolvedB = Number.isFinite(orderB) ? orderB : Number.isFinite(priorityB) ? priorityB : 99999;
    if (resolvedA !== resolvedB) return resolvedA - resolvedB;
    return (Number(a.id) || 0) - (Number(b.id) || 0);
  });
}

async function removeScenePoolRoute(routeId) {
  const ok = await dialogStore.confirm("确定将该节点移出此场景的可用列表吗？", {
    confirmText: "移出",
  });
  if (!ok) return;
  try {
    await paperpilotApi.assignModelPoolRoute(routeId, activePoolScene.value, false);
    await loadScenePoolData();
    await loadAllScenePools();
  } catch (e) {
    dialogStore.alert("移出失败: " + (e.response?.data?.message || e.message));
  }
}

const draggingIdx = ref(-1);
const dragOverIdx = ref(-1);

function handleRouteDragStart(event, index) {
  draggingIdx.value = index;
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = "move";
  }
}

function handleRouteDragOver(event, index) {
  dragOverIdx.value = index;
}

function handleRouteDragEnd() {
  draggingIdx.value = -1;
  dragOverIdx.value = -1;
}

async function handleRouteDrop(event, index) {
  const fromIdx = draggingIdx.value;
  const toIdx = index;
  if (fromIdx !== -1 && fromIdx !== toIdx) {
    const items = [...scenePoolData.value];
    const draggedItem = items.splice(fromIdx, 1)[0];
    items.splice(toIdx, 0, draggedItem);
    scenePoolData.value = items;

    const ids = Array.from(new Set(items.map(r => Number(r.id)).filter(Number.isFinite)));
    scenePoolData.value = items.map((item, idx) => ({ ...item, sortOrder: idx, manualOrder: idx }));
    try {
      await paperpilotApi.sortModelPool(ids);
      await loadScenePoolData();
      dialogStore.toast("已更新优先级排序");
    } catch (e) {
      dialogStore.alert("保存排序失败：" + (e.response?.data?.message || e.message));
    }
  }
  draggingIdx.value = -1;
  dragOverIdx.value = -1;
}

watch(activeRelay, (newVal) => {
  currentPage.value = 1;
  modelSearchQuery.value = "";
  modelSceneFilter.value = "all";
  relayPingResult.value = null;
  if (newVal) {
    loadRelayModels(newVal);
  } else {
    relayModels.value = [];
    relayModelsError.value = "";
  }
});

function getModelMetadata(modelId) {
  if (!modelId) return {
    type: "文本", billing: "按量计费", desc: "通用模型", typeClass: "type-text",
    costLabel: "通用", costDots: 2, costTierClass: "tier-low", billingShort: "按量计费"
  };
  const id = modelId.toLowerCase();

  let type = "文本";
  let billing = "输入 $0.0015 / 1K | 输出 $0.005 / 1K";
  let desc = "通用大语言模型，支持文本对话、代码编写与推理";
  let typeClass = "type-text";
  // costDots: 1=极低 2=低 3=中 4=高 5=旗舰
  let costDots = 2;
  let costLabel = "低价";
  let costTierClass = "tier-low";
  let billingShort = "约 ¥0.01/千字";

  // Helper
  function setCost(dots, label, cls, short) {
    costDots = dots; costLabel = label; costTierClass = cls; billingShort = short;
  }

  // 1. Gemini
  if (id.includes("gemini")) {
    if (id.includes("flash")) {
      if (id.includes("lite")) {
        billing = "输入 $0.000075 / 1K | 输出 $0.0003 / 1K";
        desc = "轻量级高性价比模型，极速响应，适合低成本任务";
        setCost(1, "极低价", "tier-free", "约 ¥0.001/千字");
      } else {
        billing = "输入 $0.000375 / 1K | 输出 $0.001125 / 1K";
        desc = "快速且通用的多模态模型，在速度和性能间取得极佳平衡";
        setCost(1, "极低价", "tier-free", "约 ¥0.005/千字");
      }
    } else if (id.includes("pro")) {
      billing = "输入 $0.00125 / 1K | 输出 $0.00375 / 1K";
      desc = "Google 旗舰多模态模型，支持复杂推理、代码和高精度分析";
      setCost(2, "低价", "tier-low", "约 ¥0.02/千字");
    }
    if (id.includes("image") || id.includes("vision")) {
      type = "图像"; typeClass = "type-image";
      desc = "专为图像生成、视觉理解与分析定制的模型";
    } else if (id.includes("search")) {
      type = "检索"; typeClass = "type-search";
      desc = "内置官方搜索引擎组件，支持实时联网检索答疑";
    } else {
      type = "多模态"; typeClass = "type-multimodal";
    }
  }
  // 2. GPT
  else if (id.includes("gpt")) {
    if (id.includes("gpt-4o-mini")) {
      billing = "输入 $0.00015 / 1K | 输出 $0.0006 / 1K";
      desc = "轻量级 GPT-4o 衍生版，速度极快，价格极其便宜";
      type = "多模态"; typeClass = "type-multimodal";
      setCost(1, "极低价", "tier-free", "约 ¥0.004/千字");
    } else if (id.includes("gpt-4o")) {
      billing = "输入 $0.0025 / 1K | 输出 $0.010 / 1K";
      desc = "OpenAI 旗舰智能模型，全方位顶尖表现，支持视觉分析";
      type = "多模态"; typeClass = "type-multimodal";
      setCost(3, "中等", "tier-mid", "约 ¥0.09/千字");
    } else if (id.includes("gpt-4")) {
      billing = "输入 $0.03 / 1K | 输出 $0.06 / 1K";
      desc = "经典 GPT-4 复杂推理模型，深度分析与代码逻辑专家";
      setCost(5, "旗舰级", "tier-flagship", "约 ¥0.65/千字");
    } else if (id.includes("gpt-3.5")) {
      billing = "输入 $0.0005 / 1K | 输出 $0.0015 / 1K";
      desc = "快速稳定的 GPT-3.5 经典版本，适合简单文本处理";
      setCost(2, "低价", "tier-low", "约 ¥0.014/千字");
    } else if (id.includes("gpt-5")) {
      billing = "输入 $0.005 / 1K | 输出 $0.015 / 1K";
      desc = "新一代超大规模预训练模型，多语种与逻辑分析专家";
      setCost(4, "较贵", "tier-high", "约 ¥0.14/千字");
    }
  }
  // 3. Claude
  else if (id.includes("claude")) {
    if (id.includes("sonnet")) {
      billing = "输入 $0.003 / 1K | 输出 $0.015 / 1K";
      desc = "Anthropic 旗舰模型，逻辑写作、代码生成及分析领域的行业标杆";
      type = "多模态"; typeClass = "type-multimodal";
      setCost(4, "较贵", "tier-high", "约 ¥0.13/千字");
    } else if (id.includes("haiku")) {
      billing = "输入 $0.00025 / 1K | 输出 $0.00125 / 1K";
      desc = "极速轻量级模型，适合高并发、低延迟的日常文本分类及提取";
      setCost(1, "极低价", "tier-free", "约 ¥0.009/千字");
    } else if (id.includes("opus")) {
      billing = "输入 $0.015 / 1K | 输出 $0.075 / 1K";
      desc = "Claude 系列最强大脑，专攻高难度逻辑、科研解析与算法编写";
      setCost(5, "旗舰级", "tier-flagship", "约 ¥0.65/千字");
    }
  }
  // 4. DeepSeek
  else if (id.includes("deepseek")) {
    if (id.includes("r1")) {
      billing = "输入 ¥0.004 / 1K | 输出 ¥0.016 / 1K";
      desc = "开源深度推理模型，数理逻辑与复杂推理能力媲美 o1";
      setCost(2, "低价", "tier-low", "约 ¥0.016/千字");
    } else if (id.includes("v3")) {
      billing = "输入 ¥0.001 / 1K | 输出 ¥0.002 / 1K";
      desc = "高效低成本大模型，常识问答与通用文本生成性价比极高";
      setCost(1, "极低价", "tier-free", "约 ¥0.002/千字");
    } else if (id.includes("coder")) {
      billing = "输入 ¥0.001 / 1K | 输出 ¥0.002 / 1K";
      desc = "代码大模型，针对软件工程与算法生成深度优化";
      setCost(1, "极低价", "tier-free", "约 ¥0.002/千字");
    }
    if (id.includes("chat")) {
      desc = "DeepSeek 官方对话优化版，响应迅速、中文能力极强";
    }
  }
  // 5. Kimi / Moonshot
  else if (id.includes("kimi") || id.includes("moonshot")) {
    billing = "输入 ¥0.012 / 1K | 输出 ¥0.012 / 1K";
    desc = "支持超长上下文关联的中文旗舰大模型，阅读长篇文献专家";
    setCost(3, "中等", "tier-mid", "约 ¥0.012/千字");
  }
  // 6. GLM / CogView
  else if (id.includes("glm") || id.includes("cogview")) {
    if (id.includes("cogview") || id.includes("image")) {
      type = "图像"; typeClass = "type-image";
      billing = "单次计费 ¥0.1 / 张";
      desc = "智谱 CogView 高保真图像理解与画面生成模型";
      setCost(3, "中等", "tier-mid", "¥0.1/张");
    } else {
      billing = "输入 ¥0.002 / 1K | 输出 ¥0.006 / 1K";
      desc = "智谱清言最新对话大模型，学术翻译与中文语义对齐极佳";
      setCost(2, "低价", "tier-low", "约 ¥0.006/千字");
    }
  }
  // 7. Qwen
  else if (id.includes("qwen")) {
    if (id.includes("vl") || id.includes("audio")) {
      type = "多模态"; typeClass = "type-multimodal";
      billing = "输入 ¥0.008 / 1K | 输出 ¥0.008 / 1K";
      desc = "通义千问视觉/语音多模态大模型，支持音视频及图像解析";
      setCost(2, "低价", "tier-low", "约 ¥0.008/千字");
    } else {
      billing = "输入 ¥0.001 / 1K | 输出 ¥0.002 / 1K";
      desc = "阿里开源通义千问旗舰级模型，中英文表现优异、覆盖广泛";
      setCost(1, "极低价", "tier-free", "约 ¥0.002/千字");
    }
  }

  // Image models override
  if (id.includes("dall") || id.includes("sdxl") || id.includes("flux") || id.includes("midjourney")) {
    type = "图像"; typeClass = "type-image";
    billing = "单次计费 $0.02 - $0.08 / 张";
    desc = "顶级文生图/图生图扩散模型，用于生成高精度插画与海报";
    setCost(3, "中等", "tier-mid", "$0.02–0.08/张");
  }

  return { type, billing, desc, typeClass, costLabel, costDots, costTierClass, billingShort };
}

async function openAllScenesPoolModal() {
  showAllScenesPoolModal.value = true;
  loadingAllScenesPool.value = true;
  const scenes = modelSceneOptions.map(scene => scene.value);
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

async function testRelayConnectionSpeed() {
  if (!activeRelay.value || testingRelay.value) return;
  testingRelay.value = true;
  relayPingResult.value = null;
  try {
    const res = await paperpilotApi.testRelayConnection(activeRelay.value.id);
    relayPingResult.value = {
      latencyMs: res.latencyMs || 0,
      success: res.success !== false && res.status === "available",
      message: res.message || (res.success ? "节点连通正常" : "连接异常")
    };
    if (res.success) {
      dialogStore.toast(`⚡ 节点网络畅通！延迟 ${res.latencyMs}ms（0 Token 消耗）`);
    } else {
      dialogStore.alert("中转节点测试失败：" + (res.message || "未知错误"));
    }
  } catch (e) {
    relayPingResult.value = {
      latencyMs: null,
      success: false,
      message: e.response?.data?.message || e.message
    };
    dialogStore.alert("网络探测失败：" + (e.response?.data?.message || e.message));
  } finally {
    testingRelay.value = false;
  }
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
  return "#0066ff";
}

function getRoleClass(role) {
  if (role === "管理员") return "role-admin";
  return "role-student";
}

function membershipPlanName(plan) {
  const dynamicPlan = membershipPlans.value.find((item) => item.id === plan);
  if (dynamicPlan?.name) return dynamicPlan.name;
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

async function sendUserMessage(user) {
  const content = await dialogStore.prompt(`请输入发送给「${user.username}」的站内消息内容`, {
    title: "发送私信",
    placeholder: "仅该用户可见",
    confirmText: "发送",
  });
  if (!content?.trim()) return;
  try {
    await paperpilotApi.sendAdminUserMessage(user.id, { title: "管理员消息", content: content.trim() });
    dialogStore.toast(`已向 ${user.username} 发送站内消息`);
  } catch (err) {
    dialogStore.alert(err.response?.data?.message || "发送站内消息失败");
  }
}

const showQuotaDetailModal = ref(false);
const selectedUserForQuota = ref(null);
const quotaReplenishAmounts = ref({
  translate: null,
  immersive: null,
  translateDaily: null,
  immersiveDaily: null,
  ppt: null,
  fruitScore: null
});
const replenishingKeys = ref(new Set());
const refillingPlanQuota = ref(false);

function openQuotaDetailModal(user) {
  selectedUserForQuota.value = user;
  quotaReplenishAmounts.value = {
    translate: null,
    immersive: null,
    translateDaily: null,
    immersiveDaily: null,
    ppt: null,
    fruitScore: null
  };
  showQuotaDetailModal.value = true;
}

async function replenishQuota(key) {
  if (!selectedUserForQuota.value) return;
  const amount = Number(quotaReplenishAmounts.value[key] || 0);
  if (isNaN(amount) || amount === 0) {
    dialogStore.alert("请输入有效的补充数（非零整数）");
    return;
  }
  const replenishingId = `${selectedUserForQuota.value.id}-${key}`;
  replenishingKeys.value.add(replenishingId);
  try {
    const updated = await paperpilotApi.replenishUserQuota(selectedUserForQuota.value.id, { key, amount });
    // Update in list
    const idx = systemUsers.value.findIndex(u => u.id === selectedUserForQuota.value.id);
    if (idx >= 0) {
      systemUsers.value[idx] = { ...systemUsers.value[idx], ...updated };
    }
    selectedUserForQuota.value = { ...selectedUserForQuota.value, ...updated };
    quotaReplenishAmounts.value[key] = null;
    dialogStore.toast(`成功补充额度 ${amount >= 0 ? "+" : ""}${amount}`);
  } catch (err) {
    console.error("Failed to replenish quota:", err);
    dialogStore.alert(err.response?.data?.message || "额度补给失败");
  } finally {
    replenishingKeys.value.delete(replenishingId);
  }
}

async function refillPlanQuota() {
  if (!selectedUserForQuota.value || refillingPlanQuota.value) return;
  const planName = membershipPlanName(selectedUserForQuota.value.membershipPlan);
  const confirmed = await dialogStore.confirm(
    `确定将该用户的全部周期权益恢复到“${planName}”套餐上限吗？AI 积分余额不会被改动。`,
    { title: "补满套餐权益", confirmText: "确认补满" }
  );
  if (!confirmed) return;
  refillingPlanQuota.value = true;
  try {
    const updated = await paperpilotApi.refillUserPlanQuota(selectedUserForQuota.value.id);
    const idx = systemUsers.value.findIndex(u => u.id === selectedUserForQuota.value.id);
    if (idx >= 0) systemUsers.value[idx] = { ...systemUsers.value[idx], ...updated };
    selectedUserForQuota.value = { ...selectedUserForQuota.value, ...updated };
    dialogStore.toast(`已补满 ${planName} 的全部套餐权益`);
  } catch (err) {
    console.error("Failed to refill plan quota:", err);
    dialogStore.alert(err.response?.data?.message || "套餐权益补满失败");
  } finally {
    refillingPlanQuota.value = false;
  }
}

async function loginAsUser(user) {
  const ok = await dialogStore.confirm(`确定要以用户 [${user.username}] 的身份直接登录吗？这会切换您的当前会话。`, {
    title: "一键登录"
  });
  if (!ok) return;

  try {
    const res = await paperpilotApi.impersonateUser(user.id);
    authStore.applySession(res);
    router.push("/library");
  } catch (err) {
    console.error("Impersonation failed:", err);
    dialogStore.alert(`登录失败: ${err.message || err}`);
  }
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

function toggleUserSelection(userId, checked) {
  const next = new Set(selectedUserIds.value);
  if (checked) next.add(userId);
  else next.delete(userId);
  selectedUserIds.value = next;
}

function toggleCurrentPageUsers(checked) {
  const next = new Set(selectedUserIds.value);
  paginatedUsers.value.forEach((user) => {
    if (checked) next.add(user.id);
    else next.delete(user.id);
  });
  selectedUserIds.value = next;
}

function openBatchQuotaModal() {
  if (selectedUserIds.value.size === 0) return;
  Object.keys(batchQuota).forEach((key) => { batchQuota[key] = ""; });
  showBatchMembershipModal.value = true;
}

async function saveBatchQuota() {
  if (selectedUserIds.value.size === 0 || batchMembershipSaving.value) return;
  const quotas = Object.fromEntries(Object.entries(batchQuota).filter(([, value]) => String(value).trim() !== ""));
  if (!Object.keys(quotas).length) {
    dialogStore.alert("请至少填写一项额度");
    return;
  }
  batchMembershipSaving.value = true;
  try {
    const result = await paperpilotApi.updateAdminUsersQuota([...selectedUserIds.value], quotas);
    showBatchMembershipModal.value = false;
    selectedUserIds.value = new Set();
    await fetchAllData();
    dialogStore.toast(`已为 ${result?.count || 0} 位用户更新额度`);
  } catch (error) {
    console.error("Failed to batch update quotas:", error);
    dialogStore.alert(error.response?.data?.message || "批量调整额度失败");
  } finally {
    batchMembershipSaving.value = false;
  }
}

async function toggleUserRole(user) {
  let nextRole = "普通用户";
  if (user.role !== "管理员") nextRole = "管理员";

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
      const result = await paperpilotApi.deleteUser(user.id);
      if (!result?.deleted) throw new Error("服务器未确认删除用户");
      await fetchAllData();
      dialogStore.toast(`已彻底移除用户 ${user.username}`);
    } catch (error) {
      console.error("Failed to delete user:", error);
      dialogStore.alert(error.response?.data?.message || error.message || "删除用户失败");
    }
  }
}

function promptBanUser(user) {
  selectedBanUser.value = user;
  banUserDays.value = 1;
  banUserReason.value = "违规发布他人隐私身份信息";
  showBanUserModal.value = true;
}

async function submitBanUser() {
  if (!selectedBanUser.value) return;
  try {
    await paperpilotApi.banUser(selectedBanUser.value.id, banUserReason.value, banUserDays.value);
    dialogStore.toast("用户已成功封禁");
    showBanUserModal.value = false;
    await fetchAllData();
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "封禁失败");
  }
}

async function executeUnbanUser(user) {
  if (await dialogStore.confirm(`确定要解封用户 ${user.username} 吗？`, {
    title: "解封用户",
    confirmText: "解封",
  })) {
    try {
      await paperpilotApi.unbanUser(user.id);
      dialogStore.toast("用户已解封");
      await fetchAllData();
    } catch (error) {
      dialogStore.alert(error.response?.data?.message || "解封失败");
    }
  }
}

function triggerResetAllUsers() {
  resetSecretKey.value = "";
  showResetModal.value = true;
}

async function executeResetAllUsers() {
  const secret = resetSecretKey.value;
  if (!secret || !secret.trim()) {
    dialogStore.alert("密钥不能为空");
    return;
  }
  if (secret !== "@qpalzm1105") {
    dialogStore.alert("管理员密钥错误，拒绝执行重置！");
    return;
  }

  showResetModal.value = false;
  try {
    const res = await paperpilotApi.resetAllUsers(secret);
    if (res.success) {
      dialogStore.toast(`成功重置全体用户使用额度！共影响 ${res.count} 个用户。`);
      await fetchAllData();
    } else {
      dialogStore.alert("重置失败，服务器返回异常");
    }
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "重置全体用户额度请求失败");
  }
}

function triggerRestoreAllQuotas() {
  restoreSecretKey.value = "";
  showRestoreModal.value = true;
}

async function executeRestoreAllQuotas() {
  const secret = restoreSecretKey.value;
  if (!secret || !secret.trim()) {
    dialogStore.alert("密钥不能为空");
    return;
  }
  if (secret !== "@qpalzm1105") {
    dialogStore.alert("管理员密钥错误，拒绝执行恢复！");
    return;
  }

  showRestoreModal.value = false;
  try {
    const res = await paperpilotApi.restoreAllQuotas(secret);
    if (res.success) {
      dialogStore.toast(`成功恢复全体用户的当前会员额度！共影响 ${res.count} 个用户。`);
      await fetchAllData();
    } else {
      dialogStore.alert("恢复失败，服务器返回异常");
    }
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "恢复全体用户会员额度请求失败");
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
    newUser.value = { username: "", email: "", role: "普通用户", password: "" };
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
    await loadPaymentWorkdesk();
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

async function deleteForumPostDirectly(report) {
  const postId = String(report.postId).replace('post-', '');
  const ok = await dialogStore.confirm(`确认要永久删除帖子《${report.postTitle}》吗？此操作将永久清除该帖子、所有回复及相关的举报记录！`, {
    title: "删除帖子",
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!ok) return;
  try {
    await paperpilotApi.deleteAdminForumPost(postId);
    dialogStore.alert("帖子已成功永久删除。");
    await fetchAllData();
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || "删除帖子失败");
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
  if (text.includes("融合")) return "组会一键融合";
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

async function loadTranslationIssues(showLoading = true) {
  if (showLoading) translationIssuesLoading.value = true;
  try {
    const data = await paperpilotApi.getAdminTranslationIssues({ slowMs: 1800, limit: 500 });
    translationIssueSummary.value = data.summary || {};
    translationIssues.value = Array.isArray(data.rows) ? data.rows : [];
    translationJobs.value = Array.isArray(data.jobs) ? data.jobs : [];
  } catch (error) {
    console.error("Failed to load translation issues:", error);
    if (showLoading) {
      dialogStore.alert(error.response?.data?.message || "翻译问题日志加载失败");
    }
  } finally {
    translationIssuesLoading.value = false;
  }
}

function jobStatusLabel(status) {
  const normalized = String(status || '').toUpperCase();
  if (['SUCCESS', 'COMPLETED', 'GENERATED'].includes(normalized)) return '已完成';
  if (['FAILURE', 'FAILED'].includes(normalized)) return '失败';
  return '执行中';
}

function jobStatusClass(status) {
  const normalized = String(status || '').toUpperCase();
  if (['SUCCESS', 'COMPLETED', 'GENERATED'].includes(normalized)) return 'success';
  if (['FAILURE', 'FAILED'].includes(normalized)) return 'failed';
  return 'running';
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
  const ok = await dialogStore.confirm("确认清空系统操作日志吗？清空后只会保留本次清空动作的新日志。", {
    title: "清空系统日志",
    confirmText: "清空",
    cancelText: "取消",
    danger: true,
  });
  if (!ok) return;
  try {
    await paperpilotApi.clearSystemLogs();
    await fetchAllData();
  } catch (error) {
    console.error("Failed to clear logs:", error);
    dialogStore.alert("清空日志失败");
  }
}

function syncSiteMessageEditor() {
  if (!siteMessageEditor.value) return;
  newSiteMessage.value.content = siteMessageEditor.value.innerHTML;
}

function applySiteMessageColor(color) {
  newSiteMessage.value.textColor = color;
  siteMessageEditor.value?.focus();
  document.execCommand("foreColor", false, color);
  syncSiteMessageEditor();
}

async function publishSiteMessage() {
  syncSiteMessageEditor();
  if (!newSiteMessage.value.title || !siteMessageEditor.value?.innerText.trim()) {
    dialogStore.alert("请填写消息标题和内容");
    return;
  }
  siteMessagePublishing.value = true;
  try {
    if (editingSiteMessageId.value) {
      await paperpilotApi.updateSiteMessage(editingSiteMessageId.value, newSiteMessage.value);
    } else {
      await paperpilotApi.publishSiteMessage(newSiteMessage.value);
    }
    resetSiteMessageForm();
    siteMessages.value = await paperpilotApi.getAdminSiteMessages();
    window.dispatchEvent(new Event("paperpilot:site-messages-changed"));
  } catch (error) {
    dialogStore.alert(error.response?.data?.message || (editingSiteMessageId.value ? "站内消息修改失败" : "站内消息发布失败"));
  } finally {
    siteMessagePublishing.value = false;
  }
}

function resetSiteMessageForm() {
  editingSiteMessageId.value = null;
  newSiteMessage.value = { title: "", content: "", messageType: "notice", imageUrl: "", textColor: "#000000" };
  if (siteMessageEditor.value) siteMessageEditor.value.innerHTML = "";
}

function editSiteMessage(message) {
  editingSiteMessageId.value = message.id;
  newSiteMessage.value = {
    title: message.title || "",
    content: message.content || "",
    messageType: message.messageType === "timeline" ? "timeline" : "notice",
    imageUrl: message.imageUrl || "",
    textColor: message.textColor || "#000000",
  };
  nextTick(() => {
    if (siteMessageEditor.value) siteMessageEditor.value.innerHTML = newSiteMessage.value.content;
    document.querySelector(".site-message-form")?.scrollIntoView({ behavior: "smooth", block: "start" });
  });
}

function cancelSiteMessageEdit() {
  resetSiteMessageForm();
}

async function handleMessageImageUpload(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file) return;
  if (!file.type?.startsWith("image/")) {
    dialogStore.alert("请选择有效的图片文件。");
    return;
  }
  if (file.size > 2 * 1024 * 1024) {
    dialogStore.alert("图片文件不能超过 2MB，请压缩后上传。");
    return;
  }
  try {
    const reader = new FileReader();
    reader.onload = () => {
      newSiteMessage.value.imageUrl = reader.result;
    };
    reader.readAsDataURL(file);
  } catch (err) {
    console.error("Failed to read image:", err);
    dialogStore.alert("读取图片失败");
  }
}

function clearMessageImage() {
  newSiteMessage.value.imageUrl = "";
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
.admin-batch-quota-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}
.admin-batch-quota-grid label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--spatial-graphite);
  font-size: 12px;
  font-weight: 700;
}
.admin-batch-quota-grid input {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 10px;
  border: 1px solid var(--spatial-line);
  border-radius: 8px;
  background: var(--spatial-surface);
  color: var(--spatial-graphite);
}
@media (max-width: 640px) {
  .admin-batch-quota-grid { grid-template-columns: 1fr; }
}
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
  max-width: 100%;
  width: 100%;
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

.promo-invalidate-btn {
  width: 17px;
  height: 17px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: #ef4444;
  color: #fff;
  font-size: 14px;
  line-height: 17px;
  cursor: pointer;
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

/* ── Campus Verification Table Custom Elements ── */
.campus-user-info strong,
.campus-school-info strong {
  display: block;
  font-size: 0.88rem;
  color: var(--spatial-graphite, #1e293b);
}

:global([data-theme="dark"] .campus-user-info strong),
:global([data-theme="dark"] .campus-school-info strong) {
  color: #e2e8f0 !important;
}

.campus-thumbs-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.campus-thumb-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(0, 0, 0, 0.02);
  cursor: pointer;
  transition: all 0.2s ease;
  outline: none;
}

.campus-thumb-btn img {
  width: 24px;
  height: 24px;
  object-fit: cover;
  border-radius: 4px;
}

.campus-thumb-btn span {
  font-size: 0.72rem;
  color: var(--spatial-graphite, #1e293b);
  font-weight: 500;
}

.campus-thumb-btn:hover {
  background: rgba(59, 130, 246, 0.06);
  border-color: rgba(59, 130, 246, 0.2);
}

:global([data-theme="dark"] .campus-thumb-btn) {
  border-color: rgba(255, 255, 255, 0.08) !important;
  background: rgba(255, 255, 255, 0.02) !important;
}

:global([data-theme="dark"] .campus-thumb-btn span) {
  color: #cbd5e1 !important;
}

:global([data-theme="dark"] .campus-thumb-btn:hover) {
  background: rgba(59, 130, 246, 0.1) !important;
  border-color: rgba(59, 130, 246, 0.3) !important;
}

.campus-note-text {
  max-width: 180px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.8rem;
  color: var(--spatial-graphite, #1e293b);
}

:global([data-theme="dark"] .campus-note-text) {
  color: #94a3b8 !important;
}

/* Campus status badges */
.campus-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.campus-status-badge.pending {
  background: rgba(245, 158, 11, 0.08);
  color: #d97706;
  border: 1px solid rgba(245, 158, 11, 0.15);
}

.campus-status-badge.approved {
  background: rgba(16, 185, 129, 0.08);
  color: #059669;
  border: 1px solid rgba(16, 185, 129, 0.15);
}

.campus-status-badge.rejected {
  background: rgba(239, 68, 68, 0.08);
  color: #dc2626;
  border: 1px solid rgba(239, 68, 68, 0.15);
}

/* Dark mode overrides for campus badges */
:global([data-theme="dark"] .campus-status-badge.pending) {
  background: rgba(245, 158, 11, 0.12) !important;
  color: #f59e0b !important;
  border-color: rgba(245, 158, 11, 0.2) !important;
}

:global([data-theme="dark"] .campus-status-badge.approved) {
  background: rgba(16, 185, 129, 0.12) !important;
  color: #10b981 !important;
  border-color: rgba(16, 185, 129, 0.2) !important;
}

:global([data-theme="dark"] .campus-status-badge.rejected) {
  background: rgba(239, 68, 68, 0.12) !important;
  color: #f87171 !important;
  border-color: rgba(239, 68, 68, 0.2) !important;
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
  white-space: nowrap;
}

.admin-pagination > div {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
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
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.admin-pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.admin-pagination strong {
  white-space: nowrap;
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
    padding-inline: 4px;
  }

  .admin-page {
    padding: 20px 14px 36px !important;
  }

  .admin-main-layout {
    display: flex;
    flex-direction: column;
    gap: 20px;
    margin-top: 24px;
  }

  .admin-side-nav,
  .admin-side-nav.collapsed {
    position: static !important;
    width: 100% !important;
    height: auto !important;
    border-radius: 16px !important;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05) !important;
    margin-bottom: 8px !important;
    backdrop-filter: none !important;
  }

  :global([data-theme="dark"] .admin-side-nav) {
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2) !important;
  }

  .admin-side-toggle {
    display: none !important;
  }

  .admin-side-brand {
    display: none;
  }

  .admin-side-tabs {
    flex-direction: row !important;
    padding: 8px !important;
    overflow-x: auto !important;
    gap: 8px !important;
    scrollbar-width: none;
  }

  .admin-side-tabs::-webkit-scrollbar {
    display: none;
  }

  .admin-side-tab,
  .admin-side-nav.collapsed .admin-side-tab {
    flex: 0 0 auto !important;
    width: auto !important;
    justify-content: center !important;
    min-height: 40px !important;
    padding: 8px 14px !important;
  }

  .admin-side-nav.collapsed .admin-side-icon,
  .admin-side-icon {
    width: 20px;
    height: 20px;
    margin: 0 !important;
  }

  .admin-side-label {
    display: inline !important;
    font-size: 0.8rem;
  }

  .admin-header {
    align-items: flex-start !important;
    flex-direction: column !important;
    gap: 16px !important;
  }

  .admin-header h2 {
    max-width: 100% !important;
    font-size: 1.8rem !important;
  }

  .header-right {
    display: flex !important;
    flex-wrap: wrap !important;
    align-items: center !important;
    width: 100% !important;
    gap: 10px !important;
  }

  .compact-stats-bar {
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  /* Toolbar styling for small viewports */
  .search-filter-toolbar {
    flex-direction: column !important;
    align-items: stretch !important;
    gap: 12px !important;
  }

  .search-filter-toolbar > div {
    width: 100% !important;
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
    grid-template-columns: 1fr !important;
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

.user-online-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  font-size: 0.78rem;
  font-weight: 700;
}
.user-online-status i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
}
.user-online-status.online { color: #059669; }
.user-online-status.online i { background: #10b981; box-shadow: 0 0 7px rgba(16, 185, 129, .75); }
.user-online-status.offline { color: #64748b; }

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

.translation-issue-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin: 16px 0;
}

.translation-issue-stat {
  padding: 14px 16px;
  border-radius: 12px;
}

.translation-issue-stat span,
.translation-issue-stat small,
.translation-issue-table small {
  display: block;
  color: var(--spatial-silver);
  font-size: 0.72rem;
}

.translation-issue-stat strong {
  display: block;
  margin: 6px 0 2px;
  color: var(--spatial-ink);
  font-size: 1.35rem;
}

.translation-issue-toolbar {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 160px 140px;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border-radius: 12px;
}

.translation-provider-pill,
.translation-status-badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 4px 9px;
  font-size: 0.72rem;
  font-weight: 700;
}

.translation-provider-pill {
  color: #075985;
  background: rgba(14, 165, 233, 0.12);
}

.translation-status-badge.failed {
  color: #991b1b;
  background: rgba(239, 68, 68, 0.14);
}

.translation-status-badge.success {
  color: #166534;
  background: rgba(34, 197, 94, 0.14);
}

.translation-status-badge.running {
  color: #1d4ed8;
  background: rgba(59, 130, 246, 0.14);
}

.translation-status-badge.slow {
  color: #92400e;
  background: rgba(245, 158, 11, 0.16);
}

.translation-jobs-panel {
  margin-top: 16px;
  padding: 16px;
}

.translation-jobs-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.translation-jobs-heading strong,
.translation-jobs-heading small {
  display: block;
}

.translation-jobs-heading small {
  margin-top: 4px;
  color: var(--spatial-silver);
  font-size: 0.75rem;
}

.translation-jobs-table {
  overflow-x: auto;
}

.translation-route-text,
.translation-error-text {
  display: block;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.latency-danger {
  color: #b45309;
}

@media (max-width: 720px) {
  .translation-issue-toolbar {
    grid-template-columns: 1fr;
  }
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
  width: min(1180px, calc(100vw - 48px)) !important;
  max-width: 1180px !important;
  min-height: 480px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 24px 32px;
}

.admin-modal-card.team-detail-modal .modal-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

@media (max-width: 720px) {
  .admin-modal-card.team-detail-modal {
    width: calc(100vw - 24px) !important;
    min-height: auto;
    padding: 20px 16px;
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

.site-message-editor {
  min-height: 150px;
  padding: 11px 12px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 9px;
  color: #1e293b;
  background: #fff;
  outline: none;
  white-space: pre-wrap;
  word-break: break-word;
}
.site-message-editor:empty::before {
  content: attr(data-placeholder);
  color: #94a3b8;
  pointer-events: none;
}
.site-message-editor:focus { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37, 99, 235, .12); }

.site-message-form textarea {
  min-height: 150px;
  resize: vertical;
  font: inherit;
}

.site-message-color-picker {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  color: #475569;
  font-size: 0.82rem;
  font-weight: 700;
}
.site-message-color-swatch {
  width: 22px;
  height: 22px;
  padding: 0;
  border: 2px solid rgba(15, 23, 42, .16);
  border-radius: 50%;
  cursor: pointer;
}
.site-message-color-swatch.active {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
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
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.02);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quota-modal-snapshot span {
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 500;
}

.quota-modal-snapshot strong {
  color: #0f172a;
  font-size: 1.05rem;
  font-weight: 700;
}

:root[data-theme="dark"] .quota-modal-snapshot > div {
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
}

:root[data-theme="dark"] .quota-modal-snapshot span {
  color: #94a3b8;
}

:root[data-theme="dark"] .quota-modal-snapshot strong {
  color: #e2e8f0;
}

/* Quota Item row theme adaptation */
.quota-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(15, 23, 42, 0.02);
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid rgba(15, 23, 42, 0.05);
}

.quota-item-row span {
  color: #334155;
}

.quota-item-row strong {
  color: #0f172a;
}

:root[data-theme="dark"] .quota-item-row {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.04);
}

:root[data-theme="dark"] .quota-item-row span {
  color: #cbd5e1;
}

:root[data-theme="dark"] .quota-item-row strong {
  color: #f1f5f9;
}

.quota-replenish-input {
  width: 76px;
  padding: 5px 8px;
  border-radius: 6px;
  border: 1px solid rgba(15, 23, 42, 0.15);
  background: #ffffff;
  color: #0f172a;
  font-size: 0.8rem;
  text-align: center;
  outline: none;
}

.quota-modal-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.quota-modal-heading h4,
.quota-modal-heading p {
  margin-top: 0;
}

.quota-modal-heading p {
  margin-bottom: 0;
}

.quota-refill-button {
  flex: 0 0 auto;
  min-height: 38px;
  padding: 0 16px;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .quota-modal-heading {
    flex-direction: column;
  }

  .quota-refill-button {
    width: 100%;
  }
}

.quota-replenish-input::placeholder {
  color: #94a3b8;
}

:root[data-theme="dark"] .quota-replenish-input {
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(0, 0, 0, 0.25);
  color: #ffffff;
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
  max-width: 820px;
  width: 95%;
}

.pool-modal-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--spatial-line);
  padding-bottom: 14px;
}

.pool-table-wrap {
  width: 100%;
  max-height: 400px;
  overflow-y: auto;
  margin-top: 8px;
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  transition: opacity 0.25s ease;
}

.pool-table-wrap.pool-table-refreshing {
  opacity: 0.55;
  pointer-events: none;
}

:global([data-theme="dark"] .pool-table-wrap) {
  border-color: rgba(255, 255, 255, 0.06) !important;
  background: rgba(255, 255, 255, 0.01);
}

.spatial-pool-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
  text-align: left;
}

.spatial-pool-table th {
  font-size: 0.76rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #64748b;
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.02);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

:global([data-theme="dark"] .spatial-pool-table th) {
  color: #8fa0b5 !important;
  background: rgba(255, 255, 255, 0.02) !important;
  border-bottom-color: rgba(255, 255, 255, 0.06) !important;
}

.spatial-pool-table td {
  padding: 14px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  vertical-align: middle;
  color: var(--spatial-graphite, #1e293b);
}

:global([data-theme="dark"] .spatial-pool-table td) {
  border-bottom-color: rgba(255, 255, 255, 0.04) !important;
  color: #cbd5e1 !important;
}

.spatial-pool-table tr:last-child td {
  border-bottom: none;
}

.spatial-pool-table tbody tr:hover {
  background: rgba(59, 130, 246, 0.02);
}

:global([data-theme="dark"] .spatial-pool-table tbody tr:hover) {
  background: rgba(59, 130, 246, 0.04) !important;
}

.spatial-pool-table tbody tr {
  cursor: grab;
  transition: transform 0.2s ease, background-color 0.2s ease, border 0.2s ease;
}

.spatial-pool-table tbody tr.dragging-row {
  opacity: 0.4;
  background: rgba(59, 130, 246, 0.08) !important;
  cursor: grabbing;
}

.spatial-pool-table tbody tr.drag-over-row {
  border-top: 2px dashed #3b82f6 !important;
  background: rgba(59, 130, 246, 0.05) !important;
}

.provider-info-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  max-width: 380px; /* Allow wider content to prevent overflow and wrap text */
}

.provider-info-cell strong {
  font-size: 0.88rem;
  color: var(--spatial-graphite, #1e293b);
}

:global([data-theme="dark"] .provider-info-cell strong) {
  color: #e2e8f0 !important;
}

.provider-info-cell .url-hint {
  font-size: 0.72rem;
  color: #94a3b8;
  font-family: monospace;
  word-break: break-all;
}

/* Node Error Message */
.node-error-message {
  display: block;
  font-size: 0.72rem;
  color: #ef4444;
  margin-top: 4px;
  line-height: 1.35;
  word-break: break-all;
  white-space: pre-wrap;
}

:global([data-theme="dark"] .node-error-message) {
  color: #f87171 !important;
}

.spatial-pool-table code {
  font-family: monospace;
  background: rgba(0, 0, 0, 0.04);
  padding: 3px 6px;
  border-radius: 4px;
  font-size: 0.8rem;
  color: #2563eb;
}

:global([data-theme="dark"] .spatial-pool-table code) {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #60a5fa !important;
}

.spatial-pool-table .node-latency {
  font-weight: 700;
  color: #22c55e;
  font-size: 0.85rem;
}

.spatial-pool-table .node-latency.error {
  color: #ef4444;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.78rem;
  font-weight: 600;
}

.status-badge.available {
  color: #22c55e;
}

.status-badge.unavailable {
  color: #ef4444;
}

.status-badge::before {
  content: "";
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.pool-cancel-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  font-size: 0.78rem;
  font-weight: 600;
  border-radius: 6px;
  border: 1px solid rgba(239, 68, 68, 0.2);
  background: rgba(239, 68, 68, 0.04);
  color: #ef4444;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pool-cancel-btn:hover {
  background: #ef4444;
  border-color: #ef4444;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.2);
}

:global([data-theme="dark"] .pool-cancel-btn) {
  background: rgba(239, 68, 68, 0.08) !important;
  border-color: rgba(239, 68, 68, 0.25) !important;
  color: #f87171 !important;
}

:global([data-theme="dark"] .pool-cancel-btn:hover) {
  background: #ef4444 !important;
  border-color: #ef4444 !important;
  color: #ffffff !important;
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
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.models-pane-header-left {
  flex-shrink: 0;
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

/* Scene pool status bar */
.scene-pool-status-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: flex-start;
  padding-top: 6px;
}

.scene-status-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  background: rgba(255, 255, 255, 0.02);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
  min-width: 88px;
}

.scene-status-card:hover {
  background: rgba(59, 130, 246, 0.06);
  border-color: rgba(59, 130, 246, 0.2);
  transform: translateY(-1px);
}

:global([data-theme="dark"] .scene-status-card) {
  background: rgba(16, 26, 46, 0.6) !important;
  border-color: rgba(255, 255, 255, 0.06) !important;
}

:global([data-theme="dark"] .scene-status-card:hover) {
  background: rgba(37, 60, 110, 0.5) !important;
  border-color: rgba(59, 130, 246, 0.25) !important;
}

.scene-status-header {
  display: flex;
  align-items: center;
  gap: 5px;
}

.scene-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.scene-status-dot.active {
  background: #22c55e;
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.6);
  animation: pulse-green 2s infinite;
}

.scene-status-dot.inactive {
  background: #374151;
}

@keyframes pulse-green {
  0%, 100% { box-shadow: 0 0 4px rgba(34, 197, 94, 0.5); }
  50% { box-shadow: 0 0 10px rgba(34, 197, 94, 0.9); }
}

.scene-status-name {
  font-size: 0.72rem;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
}

:global([data-theme="dark"] .scene-status-name) {
  color: #7a90aa !important;
}

.scene-status-count {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.scene-status-count strong {
  font-size: 1.2rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  color: #1e293b;
}

:global([data-theme="dark"] .scene-status-count strong) {
  color: #e2e8f0 !important;
}

.scene-status-count span {
  font-size: 0.65rem;
  color: #94a3b8;
  font-weight: 500;
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
  display: flex;
  flex-direction: column;
  align-self: start;
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
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  padding-top: 10px;
  margin-top: 6px;
}

.assign-label-tag {
  display: block;
  font-size: 0.65rem;
  font-weight: 700;
  color: #3d5068;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 8px;
}

.checkbox-columns {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* ── Premium toggleable chip (Linear / Vercel style) ── */
.scene-checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.73rem;
  font-weight: 500;
  color: #4a5a72;
  cursor: pointer;
  user-select: none;
  padding: 4px 10px 4px 8px;
  border-radius: 6px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
  white-space: nowrap;
  line-height: 1.5;
  position: relative;
}

/* Checkmark icon (hidden by default, revealed on check) */
.scene-checkbox-label::before {
  content: '';
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 3px;
  background: transparent;
  border: 1.5px solid rgba(255, 255, 255, 0.15);
  flex-shrink: 0;
  transition: background 0.18s ease, border-color 0.18s ease;
  position: relative;
  top: 0;
}

.scene-checkbox-label:hover:not(.disabled) {
  border-color: rgba(99, 130, 200, 0.25);
  color: #7a93b8;
  background: rgba(59, 130, 246, 0.04);
}

.scene-checkbox-label.checked {
  background: rgba(37, 99, 235, 0.18) !important;
  border-color: rgba(59, 130, 246, 0.45) !important;
  color: #93c5fd !important;
  font-weight: 600 !important;
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.1) !important;
}

.scene-checkbox-label.checked::before {
  background: #3b82f6;
  border-color: #3b82f6;
  /* SVG checkmark via mask */
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 10 10' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M2 5.5l2.5 2.5 3.5-5' stroke='white' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round' fill='none'/%3E%3C/svg%3E");
  mask-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 10 10' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M2 5.5l2.5 2.5 3.5-5' stroke='white' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round' fill='none'/%3E%3C/svg%3E");
  -webkit-mask-size: cover;
  mask-size: cover;
}

.scene-checkbox-label.disabled {
  opacity: 0.3;
  cursor: not-allowed;
  pointer-events: none;
}

/* Completely hide the native checkbox input */
.scene-checkbox-label input[type="checkbox"] {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  pointer-events: none;
}

/* Error message container - contained & truncated */
.model-speed-error-container {
  background: rgba(239, 68, 68, 0.05);
  border: 1px solid rgba(239, 68, 68, 0.15);
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.model-speed-error-summary {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  font-size: 0.72rem;
  color: #f87171;
  cursor: pointer;
  gap: 4px;
  transition: background 0.15s ease;
}

.model-speed-error-summary:hover {
  background: rgba(239, 68, 68, 0.06);
}

.model-speed-error-detail {
  padding: 6px 8px;
  font-size: 0.68rem;
  color: #fca5a5;
  border-top: 1px solid rgba(239, 68, 68, 0.1);
  word-break: break-all;
  overflow-wrap: anywhere;
  max-height: 80px;
  overflow-y: auto;
  line-height: 1.5;
  white-space: pre-wrap;
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
  max-width: 100%;
  width: 100%;
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

:global([data-theme="dark"] .stat-label),
:global([data-theme="dark"] .stat-sub),
:global([data-theme="dark"] .models-pane-header .pane-description),
:global([data-theme="dark"] .quick-access-title),
:global([data-theme="dark"] .form-group-item label),
:global([data-theme="dark"] .provider-url),
:global([data-theme="dark"] .model-badge-provider),
:global([data-theme="dark"] .assign-label-tag),
:global([data-theme="dark"] .scene-checkbox-label),
:global([data-theme="dark"] .count-tag) {
  color: #8c98aa !important;
}

:global([data-theme="dark"] .stat-value),
:global([data-theme="dark"] .models-pane-header h3),
:global([data-theme="dark"] .column-header strong),
:global([data-theme="dark"] .provider-name),
:global([data-theme="dark"] .model-name-id),
:global([data-theme="dark"] .scene-checkbox-label.checked),
:global([data-theme="dark"] .module-route-head h4),
:global([data-theme="dark"] .scene-pool-section .section-title strong) {
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

:global([data-theme="dark"] .admin-side-tab) {
  color: #94a3b8 !important;
}

:global([data-theme="dark"] .admin-side-tab.active) {
  background: #668bdd !important;
  color: #edf4ff !important;
}

:global([data-theme="dark"] .admin-side-toggle) {
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

:global([data-theme="dark"] .save-config-btn),
:global([data-theme="dark"] .spatial-btn-accent) {
  border: none !important;
  background: linear-gradient(135deg, #2563eb, #3b82f6) !important;
  color: #ffffff !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25) !important;
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.2s ease !important;
}

:global([data-theme="dark"] .save-config-btn:hover),
:global([data-theme="dark"] .spatial-btn-accent:hover) {
  transform: translateY(-1.5px) !important;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.4) !important;
}

:global([data-theme="dark"] .spatial-btn-ghost) {
  background: rgba(255, 255, 255, 0.03) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  color: #cbd5e1 !important;
  border-radius: 8px !important;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: none !important;
}

:global([data-theme="dark"] .spatial-btn-ghost:hover:not(:disabled)) {
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(255, 255, 255, 0.2) !important;
  color: #ffffff !important;
  transform: translateY(-1.5px) !important;
}

:global([data-theme="dark"] .spatial-btn-ghost:disabled) {
  opacity: 0.45 !important;
  cursor: not-allowed !important;
}

.models-two-col-layout {
  grid-template-columns: minmax(280px, 0.92fr) minmax(520px, 1.6fr);
  gap: 22px;
  transition: grid-template-columns 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.models-two-col-layout.providers-collapsed {
  grid-template-columns: 88px minmax(520px, 1fr);
}

.providers-column,
.config-card-panel,
.models-grid-panel {
  padding: 20px;
}

.providers-column {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  align-self: start;
}

.providers-collapsed .providers-column {
  padding: 20px 8px !important;
}

.provider-item-card.collapsed-item {
  justify-content: center !important;
  padding: 8px !important;
  width: 44px;
  height: 44px;
  border-radius: 50% !important;
  margin: 0 auto;
}

.provider-avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 800;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #fff;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

:global([data-theme="dark"] .provider-item-card.collapsed-item.active .provider-avatar-circle) {
  background: linear-gradient(135deg, #10b981, #059669) !important;
  box-shadow: 0 0 12px rgba(16, 185, 129, 0.5) !important;
}

:global([data-theme="dark"] .providers-column .column-header),
:global([data-theme="dark"] .config-card-panel .column-header),
:global([data-theme="dark"] .models-grid-panel .column-header) {
  border-bottom-color: #1d293a;
}

:global([data-theme="dark"] .add-provider-btn) {
  background: linear-gradient(135deg, #2563eb, #3b82f6) !important;
  border: none !important;
  color: #ffffff !important;
  border-radius: 6px !important;
  padding: 4px 10px !important;
  font-weight: 700 !important;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25) !important;
  transition: all 0.2s ease !important;
}

:global([data-theme="dark"] .add-provider-btn:hover) {
  transform: translateY(-1px) !important;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4) !important;
}

.provider-card-actions {
  display: flex;
  gap: 4px;
}

.provider-action-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  display: inline-grid;
  place-items: center;
  transition: all 0.2s ease;
}

:global([data-theme="dark"] .provider-action-btn.edit-btn) {
  color: #94a3b8 !important;
}
:global([data-theme="dark"] .provider-action-btn.edit-btn:hover) {
  background: rgba(255, 255, 255, 0.05) !important;
  color: #3b82f6 !important;
}

:global([data-theme="dark"] .provider-action-btn.delete-btn) {
  color: #94a3b8 !important;
}
:global([data-theme="dark"] .provider-action-btn.delete-btn:hover) {
  background: rgba(239, 68, 68, 0.08) !important;
  color: #f87171 !important;
}

:global([data-theme="dark"] .provider-item-card) {
  background: #131b29 !important;
  border: 1px solid transparent !important;
  border-radius: 8px !important;
  box-shadow: none !important;
}

:global([data-theme="dark"] .model-dashboard-card) {
  background: linear-gradient(135deg, #111a2d 0%, #09101f 100%) !important;
  border: 1px solid rgba(59, 130, 246, 0.15) !important;
  border-radius: 16px !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2) !important;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s ease, box-shadow 0.25s ease !important;
}

:global([data-theme="dark"] .provider-item-card.active) {
  background: #172236 !important;
  border-color: #3b82f6 !important;
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.24) !important;
}

:global([data-theme="dark"] .provider-item-card:hover) {
  border-color: #334664 !important;
  background: #162235 !important;
}

:global([data-theme="dark"] .model-dashboard-card:hover) {
  transform: translateY(-4px) !important;
  border-color: rgba(59, 130, 246, 0.45) !important;
  box-shadow: 0 10px 30px rgba(59, 130, 246, 0.18), 0 4px 12px rgba(0, 0, 0, 0.3) !important;
}

:global([data-theme="dark"] .model-name-id) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace !important;
  font-size: 0.92rem !important;
  font-weight: 700 !important;
  color: #f1f5f9 !important;
  letter-spacing: -0.02em !important;
}

:global([data-theme="dark"] .spatial-input),
:global([data-theme="dark"] .form-group-item input) {
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

:global([data-theme="dark"] .model-badge-provider) {
  font-size: 0.7rem !important;
  font-weight: 700 !important;
  color: #3b82f6 !important;
  background: rgba(59, 130, 246, 0.08) !important;
  border: 1px solid rgba(59, 130, 246, 0.2) !important;
  padding: 2px 8px !important;
  border-radius: 4px !important;
  display: inline-block !important;
  margin-top: 4px !important;
}

:global([data-theme="dark"] .model-speed-pill-new) {
  background: rgba(255, 255, 255, 0.02) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  border-radius: 6px !important;
  color: #94a3b8 !important;
  padding: 4px 10px !important;
  font-weight: 600 !important;
  transition: all 0.2s ease !important;
}

:global([data-theme="dark"] .model-speed-pill-new:hover:not(:disabled)) {
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(255, 255, 255, 0.2) !important;
  color: #fff !important;
}

:global([data-theme="dark"] .model-speed-pill-new.success),
:global([data-theme="dark"] .latency-text) {
  background: rgba(16, 185, 129, 0.1) !important;
  border: 1px solid rgba(16, 185, 129, 0.3) !important;
  color: #34d399 !important;
}

:global([data-theme="dark"] .model-speed-pill-new.error),
:global([data-theme="dark"] .latency-text.error) {
  background: rgba(239, 68, 68, 0.1) !important;
  border: 1px solid rgba(239, 68, 68, 0.3) !important;
  color: #f87171 !important;
}

:global([data-theme="dark"] .model-speed-pill-new.testing) {
  background: rgba(59, 130, 246, 0.1) !important;
  border: 1px solid rgba(59, 130, 246, 0.3) !important;
  color: #60a5fa !important;
}

:global([data-theme="dark"] .module-route-table strong) {
  color: #45e083 !important;
}

:global([data-theme="dark"] .module-route-table strong.error) {
  color: #ff6b6b !important;
}

:global([data-theme="dark"] .scene-assignments-grid) {
  border-top-color: #1f2d47 !important;
}

:global([data-theme="dark"] .scene-checkbox-label input[type="checkbox"]) {
  border-color: rgba(255, 255, 255, 0.15) !important;
  background: transparent !important;
}

:global([data-theme="dark"] .scene-checkbox-label input[type="checkbox"]:checked) {
  background: #3b82f6 !important;
  border-color: #3b82f6 !important;
  box-shadow: 0 0 5px rgba(59, 130, 246, 0.6) !important;
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
.scene-pool-table code {
  background: transparent !important;
  color: #dbe4f0 !important;
  font-weight: 800;
}

.model-code-id {
  font-family: Menlo, Monaco, Consolas, monospace;
  background: rgba(37, 99, 235, 0.05) !important;
  color: #1d4ed8 !important;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 700;
  border: 1px solid rgba(37, 99, 235, 0.1);
}

:root[data-theme="dark"] .model-code-id {
  background: rgba(59, 130, 246, 0.1) !important;
  color: #60a5fa !important;
  border: 1px solid rgba(59, 130, 246, 0.2);
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

:global([data-theme="dark"] .pool-quick-badge) {
  border-color: #1d293a !important;
  background: #172236 !important;
  color: #cbd5e1 !important;
}

:global([data-theme="dark"] .pool-quick-badge:hover) {
  border-color: #3b82f6 !important;
  background: #1e293b !important;
  color: #ffffff !important;
}

:global([data-theme="dark"] .pool-quick-badge .badge-count) {
  background: rgba(59, 130, 246, 0.2) !important;
  color: #75a7ff !important;
}

@media (max-width: 990px) {
  .admin-page {
    padding: 24px 18px 32px;
  }
}

/* ── Screenshot-style redesign ── */

/* Provider info banner at top of right panel */
.relay-info-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.05);
  border: 1px solid rgba(59, 130, 246, 0.12);
  margin-bottom: 16px;
}

:global([data-theme="dark"] .relay-info-banner) {
  background: rgba(20, 35, 65, 0.7) !important;
  border-color: rgba(59, 130, 246, 0.18) !important;
}

.relay-info-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.relay-info-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: #e2e8f0;
  line-height: 1.3;
}

.relay-info-dash {
  color: #4a5a7a;
  margin: 0 2px;
}

.relay-info-subtitle {
  font-size: 0.85rem;
  font-weight: 500;
  color: #7a9ac8;
}

.relay-info-url {
  font-size: 0.75rem;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
}

.relay-info-url code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: #60a5fa;
  background: rgba(59, 130, 246, 0.08);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 0.73rem;
}

.relay-info-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* Models subheader with search and filter */
.models-subheader {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

:global([data-theme="dark"] .models-subheader) {
  border-bottom-color: #1a2740 !important;
}

.models-filters-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.models-filters-right {
  flex-shrink: 0;
}

/* Search Box */
.model-search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.model-search-box .search-icon {
  position: absolute;
  left: 10px;
  width: 14px;
  height: 14px;
  color: #64748b;
  pointer-events: none;
}

.spatial-search-input {
  width: 200px;
  height: 32px;
  padding: 0 30px 0 30px;
  font-size: 0.85rem;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(0, 0, 0, 0.01);
  color: var(--spatial-graphite, #1e293b);
  outline: none;
  transition: all 0.2s ease;
}

.spatial-search-input:focus {
  background: #ffffff;
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

:global([data-theme="dark"] .spatial-search-input) {
  border-color: rgba(255, 255, 255, 0.08) !important;
  background: rgba(255, 255, 255, 0.02) !important;
  color: #f1f5f9 !important;
}

:global([data-theme="dark"] .spatial-search-input:focus) {
  background: rgba(15, 25, 45, 0.8) !important;
  border-color: rgba(59, 130, 246, 0.4) !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15) !important;
}

.clear-search-btn {
  position: absolute;
  right: 8px;
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 1.1rem;
  cursor: pointer;
  padding: 0;
  display: grid;
  place-items: center;
  width: 16px;
  height: 16px;
}

.clear-search-btn:hover {
  color: #64748b;
}

/* Filter Dropdown Box */
.model-filter-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-filter-box .filter-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

:global([data-theme="dark"] .model-filter-box .filter-label) {
  color: #8fa0b5 !important;
}

.spatial-filter-select {
  height: 32px;
  padding: 0 24px 0 10px;
  font-size: 0.85rem;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(0, 0, 0, 0.01);
  color: var(--spatial-graphite, #1e293b);
  outline: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.spatial-filter-select:focus,
.spatial-filter-select:hover {
  border-color: rgba(59, 130, 246, 0.3);
}

:global([data-theme="dark"] .spatial-filter-select) {
  border-color: rgba(255, 255, 255, 0.08) !important;
  background: rgba(15, 25, 45, 0.6) !important;
  color: #cbd5e1 !important;
}

:global([data-theme="dark"] .spatial-filter-select:focus) {
  border-color: rgba(59, 130, 246, 0.4) !important;
}

.models-count-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: #6b7c9a;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.models-count-label em {
  font-style: normal;
  color: #60a5fa;
  font-weight: 700;
  margin-left: 3px;
}


/* Model card top row: name left, speed pill right */
.model-card-top-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.model-card-top-row .model-name-id {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-card-top-row .model-speed-pill-new {
  flex-shrink: 0;
}

/* Provider badge sits just below name */
.model-dashboard-card > .model-badge-provider {
  align-self: flex-start;
  margin-top: -4px;
}

/* Tighten card gaps */
:global([data-theme="dark"] .model-dashboard-card) {
  gap: 10px !important;
}

/* Slightly adjust two-col layout widths to be closer to screenshot */
.models-two-col-layout {
  grid-template-columns: 230px 1fr !important;
  gap: 18px !important;
}

.models-two-col-layout.providers-collapsed {
  grid-template-columns: 72px 1fr !important;
}

/* Provider item cards – tighter, more like screenshot */
.provider-item-card {
  padding: 10px 12px !important;
  border-radius: 10px !important;
}

.provider-name {
  font-size: 0.82rem !important;
  font-weight: 700 !important;
}

.provider-url {
  font-size: 0.68rem !important;
  max-width: 160px;
}

/* Providers list gap tighter */
.providers-list {
  gap: 6px !important;
}

/* Remove the old column-header margin from models-grid-panel header since we use banner now */
.models-grid-panel .column-header {
  display: none !important;
}

.model-load-error {
  max-width: 560px;
  margin: 10px auto 0;
  color: #fca5a5;
  font-size: 0.9rem;
  line-height: 1.7;
}

.model-empty-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 18px;
  flex-wrap: wrap;
}

/* ── Global modal close button ── */
.modal-close {
  width: 28px;
  height: 28px;
  display: inline-grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  border-radius: 7px !important;
  background: rgba(255, 255, 255, 0.04) !important;
  color: #64748b !important;
  font-size: 1.1rem !important;
  line-height: 1 !important;
  cursor: pointer;
  transition: all 0.18s ease !important;
  flex-shrink: 0;
}

.modal-close:hover {
  background: rgba(239, 68, 68, 0.1) !important;
  border-color: rgba(239, 68, 68, 0.25) !important;
  color: #f87171 !important;
}

/* Dark mode modal-header universal style */
:global([data-theme="dark"] .modal-header) {
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  gap: 16px !important;
  border-bottom: 1px solid #1a2740 !important;
  padding-bottom: 14px !important;
  margin-bottom: 18px !important;
}

:global([data-theme="dark"] .modal-header h5),
:global([data-theme="dark"] .modal-header .modal-title) {
  color: #dde5f0 !important;
  margin: 0 !important;
  font-size: 1rem !important;
  font-weight: 700 !important;
}

/* ── Collapse providers button ── */
.collapse-providers-btn {
  width: 28px;
  height: 28px;
  display: inline-grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.03);
  color: #4a5a72;
  cursor: pointer;
  transition: all 0.18s ease;
  flex-shrink: 0;
  padding: 0;
}

.collapse-providers-btn:hover {
  background: rgba(59, 130, 246, 0.08);
  border-color: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
}

:global([data-theme="dark"] .collapse-providers-btn) {
  border-color: rgba(255, 255, 255, 0.07) !important;
  background: rgba(15, 25, 45, 0.6) !important;
  color: #4a5a72 !important;
}

:global([data-theme="dark"] .collapse-providers-btn:hover) {
  background: rgba(37, 70, 130, 0.3) !important;
  border-color: rgba(59, 130, 246, 0.25) !important;
  color: #7eb3ff !important;
}

/* ── Models Pagination Redesign ── */
.models-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 32px;
  padding: 16px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

:global([data-theme="dark"] .models-pagination) {
  border-top-color: rgba(255, 255, 255, 0.05) !important;
}

.models-page-size-wrap {
  display: flex;
  align-items: center;
}

.spatial-page-size-select {
  height: 34px;
  padding: 0 28px 0 10px;
  font-size: 0.8rem;
  font-weight: 500;
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(0, 0, 0, 0.02);
  color: var(--spatial-graphite, #1e293b);
  outline: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.spatial-page-size-select:focus,
.spatial-page-size-select:hover {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(59, 130, 246, 0.02);
}

:global([data-theme="dark"] .spatial-page-size-select) {
  border-color: rgba(255, 255, 255, 0.08) !important;
  background: rgba(255, 255, 255, 0.02) !important;
  color: #cbd5e1 !important;
}

:global([data-theme="dark"] .spatial-page-size-select:focus) {
  border-color: rgba(59, 130, 246, 0.4) !important;
  background: rgba(59, 130, 246, 0.08) !important;
}

.pagination-pages {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pagination-arrow-btn,
.pagination-page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  font-size: 0.85rem;
  font-weight: 500;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(0, 0, 0, 0.02);
  color: var(--spatial-graphite, #1e293b);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  outline: none;
  box-sizing: border-box;
}

.pagination-arrow-btn {
  padding: 0 14px;
}

.pagination-page-btn {
  width: 32px;
  padding: 0;
}

.pagination-arrow-btn:disabled,
.pagination-page-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
  border-color: rgba(0, 0, 0, 0.04) !important;
  background: transparent !important;
  color: #94a3b8 !important;
}

.pagination-arrow-btn:not(:disabled):hover,
.pagination-page-btn:not(:disabled):hover {
  background: rgba(59, 130, 246, 0.06);
  border-color: rgba(59, 130, 246, 0.2);
  color: #2563eb;
}

.pagination-page-btn.active {
  background: #2563eb !important;
  border-color: #2563eb !important;
  color: #ffffff !important;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.2);
}

.pagination-page-btn.separator {
  border-color: transparent !important;
  background: transparent !important;
  cursor: default;
  color: #94a3b8;
  width: 20px;
}

/* Dark Mode Overrides for Pagination */
:global([data-theme="dark"] .pagination-arrow-btn),
:global([data-theme="dark"] .pagination-page-btn) {
  border-color: rgba(255, 255, 255, 0.06) !important;
  background: rgba(255, 255, 255, 0.02) !important;
  color: #cbd5e1 !important;
}

:global([data-theme="dark"] .pagination-arrow-btn:disabled),
:global([data-theme="dark"] .pagination-page-btn:disabled) {
  opacity: 0.3;
  border-color: rgba(255, 255, 255, 0.03) !important;
  background: transparent !important;
  color: #475569 !important;
}

:global([data-theme="dark"] .pagination-arrow-btn:not(:disabled):hover),
:global([data-theme="dark"] .pagination-page-btn:not(:disabled):hover) {
  background: rgba(59, 130, 246, 0.1) !important;
  border-color: rgba(59, 130, 246, 0.3) !important;
  color: #60a5fa !important;
}

:global([data-theme="dark"] .pagination-page-btn.active) {
  background: #3b82f6 !important;
  border-color: #3b82f6 !important;
  color: #ffffff !important;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25) !important;
}

/* ── Team Details Modal Redesign ── */
.team-detail-modal {
  /* Dimensions are controlled by .admin-modal-card.team-detail-modal for specificity */
}

.team-detail-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.team-detail-summary .summary-card {
  padding: 16px 20px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

:global([data-theme="dark"] .team-detail-summary .summary-card) {
  background: rgba(255, 255, 255, 0.02) !important;
  border-color: rgba(255, 255, 255, 0.06) !important;
}

.team-detail-summary .summary-card span {
  font-size: 0.76rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

:global([data-theme="dark"] .team-detail-summary .summary-card span) {
  color: #8fa0b5 !important;
}

.team-detail-summary .summary-card strong {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--spatial-graphite, #1e293b);
}

:global([data-theme="dark"] .team-detail-summary .summary-card strong) {
  color: #f1f5f9 !important;
}

.team-detail-summary .summary-card code {
  font-family: monospace;
  font-size: 1.05rem;
  font-weight: 700;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.05);
  padding: 2px 6px;
  border-radius: 4px;
  align-self: flex-start;
}

:global([data-theme="dark"] .team-detail-summary .summary-card code) {
  color: #60a5fa !important;
  background: rgba(96, 165, 250, 0.08) !important;
}

/* Members Table wrapper */
.team-members-table-wrap {
  flex: 1;
  max-height: 480px;
  overflow-y: auto;
  overflow-x: hidden !important;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(0, 0, 0, 0.005);
  margin-bottom: 8px;
}

:global([data-theme="dark"] .team-members-table-wrap) {
  border-color: rgba(255, 255, 255, 0.08) !important;
  background: rgba(255, 255, 255, 0.01) !important;
}

.team-members-table-wrap .admin-table {
  min-width: 100% !important;
  width: 100% !important;
  table-layout: auto;
}

.team-members-table-wrap .admin-table td {
  padding: 12px 14px !important;
  white-space: normal !important; /* Allow small text to wrap */
  word-break: break-all;
}

.team-members-table-wrap .admin-table td strong {
  font-size: 0.86rem;
  display: block;
}

.team-members-table-wrap .admin-table td small {
  display: block;
  font-size: 0.72rem;
  color: #64748b;
  margin-top: 2px;
}

:global([data-theme="dark"] .team-members-table-wrap .admin-table td small) {
  color: #94a3b8 !important;
}

/* Dark mode overrides for modal cards */
:global([data-theme="dark"] .admin-modal-card) {
  background: var(--spatial-glass, #121a28) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.45) !important;
  backdrop-filter: blur(20px) !important;
}

:global([data-theme="dark"] .admin-modal-card h4),
:global([data-theme="dark"] .admin-modal-card h5) {
  color: #f1f5f9 !important;
}

:global([data-theme="dark"] .admin-modal-card p) {
  color: #94a3b8 !important;
}

.team-members-empty {
  padding: 40px 0;
  text-align: center;
  color: #64748b;
  font-size: 0.9rem;
  font-style: italic;
}

/* ── Model Card Metadata Enhancements ── */
.model-type-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.model-type-tag.type-text {
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
}
:global([data-theme="dark"] .model-type-tag.type-text) {
  background: rgba(59, 130, 246, 0.15) !important;
  color: #60a5fa !important;
}

.model-type-tag.type-image {
  background: rgba(236, 72, 153, 0.08);
  color: #ec4899;
}
:global([data-theme="dark"] .model-type-tag.type-image) {
  background: rgba(244, 63, 94, 0.15) !important;
  color: #fb7185 !important;
}

.model-type-tag.type-search {
  background: rgba(16, 185, 129, 0.08);
  color: #10b981;
}
:global([data-theme="dark"] .model-type-tag.type-search) {
  background: rgba(52, 211, 153, 0.15) !important;
  color: #34d399 !important;
}

.model-type-tag.type-multimodal {
  background: rgba(139, 92, 246, 0.08);
  color: #8b5cf6;
}
:global([data-theme="dark"] .model-type-tag.type-multimodal) {
  background: rgba(168, 85, 247, 0.15) !important;
  color: #c084fc !important;
}

/* Cost Tier Badge System */
.model-cost-tier-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.model-cost-tier-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px 3px 7px;
  border-radius: 20px;
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  cursor: default;
  transition: transform 0.15s;
  white-space: nowrap;
}
.model-cost-tier-badge:hover { transform: scale(1.04); }

/* Dot indicators inside badge */
.cost-tier-dots {
  display: inline-flex;
  gap: 2.5px;
  align-items: center;
}
.cost-tier-dots i {
  display: inline-block;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  opacity: 0.25;
  background: currentColor;
  transition: opacity 0.2s;
}
.cost-tier-dots i.active { opacity: 1; }

/* Tier color schemes */
.tier-free  { background: rgba(16,185,129,0.12); color: #059669; border: 1px solid rgba(5,150,105,0.25); }
.tier-low   { background: rgba(59,130,246,0.10); color: #2563eb; border: 1px solid rgba(37,99,235,0.20); }
.tier-mid   { background: rgba(245,158,11,0.12); color: #b45309; border: 1px solid rgba(180,83,9,0.25); }
.tier-high  { background: rgba(249,115,22,0.12); color: #c2410c; border: 1px solid rgba(194,65,12,0.25); }
.tier-flagship { background: rgba(239,68,68,0.10); color: #b91c1c; border: 1px solid rgba(185,28,28,0.22); }

/* Short billing hint next to the badge */
.model-billing-hint {
  font-size: 0.67rem;
  color: #94a3b8;
  font-family: monospace;
  white-space: nowrap;
  cursor: help;
}

/* Dark mode overrides */
:global([data-theme="dark"] .tier-free)     { background: rgba(16,185,129,0.15) !important; color: #34d399 !important; border-color: rgba(52,211,153,0.25) !important; }
:global([data-theme="dark"] .tier-low)      { background: rgba(96,165,250,0.12) !important; color: #60a5fa !important; border-color: rgba(96,165,250,0.22) !important; }
:global([data-theme="dark"] .tier-mid)      { background: rgba(251,191,36,0.12) !important; color: #fbbf24 !important; border-color: rgba(251,191,36,0.25) !important; }
:global([data-theme="dark"] .tier-high)     { background: rgba(251,146,60,0.12) !important; color: #fb923c !important; border-color: rgba(251,146,60,0.22) !important; }
:global([data-theme="dark"] .tier-flagship) { background: rgba(248,113,113,0.12) !important; color: #f87171 !important; border-color: rgba(248,113,113,0.22) !important; }
:global([data-theme="dark"] .model-billing-hint) { color: #64748b !important; }

.model-desc-info-text {
  font-size: 0.72rem;
  line-height: 1.45;
  color: #64748b;
  margin: 6px 0 8px 0;
}

:global([data-theme="dark"] .model-desc-info-text) {
  color: #94a3b8 !important;
}

/* ── Dark Mode Adaptation for Payment Orders & Support Tickets ── */
:global([data-theme="dark"] .payment-work-head strong) {
  color: #f1f5f9 !important;
}

:global([data-theme="dark"] .payment-work-head span) {
  color: #8fa0b5 !important;
}

:global([data-theme="dark"] .payment-ticket-admin),
:global([data-theme="dark"] .payment-order-admin) {
  background: rgba(255, 255, 255, 0.02) !important;
  border-color: rgba(255, 255, 255, 0.06) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25) !important;
}

:global([data-theme="dark"] .payment-ticket-admin strong),
:global([data-theme="dark"] .payment-order-admin strong) {
  color: #f1f5f9 !important;
}

:global([data-theme="dark"] .payment-ticket-admin span),
:global([data-theme="dark"] .payment-order-admin span) {
  color: #cbd5e1 !important;
}

:global([data-theme="dark"] .payment-ticket-admin small),
:global([data-theme="dark"] .payment-order-admin small),
:global([data-theme="dark"] .payment-ticket-admin em) {
  color: #94a3b8 !important;
}

:global([data-theme="dark"] .payment-ticket-admin p) {
  color: #cbd5e1 !important;
}

:global([data-theme="dark"] .payment-ticket-admin code),
:global([data-theme="dark"] .payment-order-admin code) {
  background: rgba(59, 130, 246, 0.1) !important;
  color: #60a5fa !important;
}

/* ── Site Message Image Upload Styling ── */
.message-image-upload-section {
  margin: 14px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-image-upload-section > span {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--spatial-graphite, #64748b);
}

.message-image-preview-card {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px dashed rgba(99, 102, 241, 0.25);
  background: rgba(99, 102, 241, 0.02);
  align-self: flex-start;
}

.message-image-preview-card img {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid var(--spatial-line);
}

.remove-preview-btn {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 700;
  background: rgba(239, 68, 68, 0.08);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.15);
  cursor: pointer;
  transition: all 0.15s ease;
}

.remove-preview-btn:hover {
  background: #ef4444;
  color: #ffffff;
}

.message-image-upload-trigger {
  display: flex;
}

.upload-trigger-label {
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px dashed var(--spatial-line);
  background: var(--spatial-surface);
  color: var(--spatial-graphite);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s ease;
}

.upload-trigger-label:hover {
  border-color: #6366f1;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.02);
}

.site-message-image-thumb {
  margin-top: 8px;
  display: inline-flex;
}

.image-thumb-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px;
  border-radius: 8px;
  border: 1px solid var(--spatial-line);
  background: var(--spatial-surface);
  color: var(--spatial-graphite);
  cursor: pointer;
  font-size: 0.72rem;
  font-weight: 600;
  transition: all 0.15s ease;
}

.image-thumb-btn img {
  width: 28px;
  height: 28px;
  object-fit: cover;
  border-radius: 4px;
}

.image-thumb-btn:hover {
  border-color: #6366f1;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.02);
}

.membership-admin-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.membership-admin-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  padding: 16px;
  margin-bottom: 18px;
}

.membership-admin-summary article {
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.12), rgba(20, 184, 166, 0.08));
  border: 1px solid rgba(96, 165, 250, 0.18);
}

.membership-admin-summary span,
.membership-admin-summary small {
  display: block;
  color: rgba(203, 213, 225, 0.74);
  font-size: 0.78rem;
  font-weight: 700;
}

.membership-admin-summary strong {
  display: block;
  margin: 6px 0 4px;
  color: #f8fafc;
  font-size: 1.35rem;
}

.membership-plan-admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 480px), 1fr));
  gap: 18px;
  align-items: start;
}

.membership-plan-admin-card {
  min-width: 0;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(96, 165, 250, 0.18);
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.15), transparent 34%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.94), rgba(2, 6, 23, 0.86));
  box-shadow: 0 22px 60px rgba(2, 6, 23, 0.28);
}

.membership-plan-admin-card.sale {
  border-color: rgba(251, 191, 36, 0.35);
  box-shadow: 0 22px 60px rgba(251, 146, 60, 0.12);
}

.membership-plan-admin-card.inactive {
  opacity: 0.62;
}

.membership-plan-admin-card header,
.membership-plan-admin-card footer,
.seckill-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.plan-admin-publish-button {
  min-width: 74px;
  height: 34px;
  padding: 0 13px;
  border: 1px solid rgba(74, 222, 128, 0.38);
  border-radius: 7px;
  background: rgba(34, 197, 94, 0.14);
  color: #86efac;
  font: inherit;
  font-size: 0.82rem;
  font-weight: 800;
  cursor: pointer;
  transition: transform 160ms ease, background 160ms ease, border-color 160ms ease;
}
.plan-admin-publish-button:hover:not(:disabled) {
  transform: translateY(-1px);
  background: rgba(34, 197, 94, 0.22);
}
.plan-admin-publish-button.offline {
  border-color: rgba(148, 163, 184, 0.36);
  background: rgba(100, 116, 139, 0.14);
  color: #cbd5e1;
}
.plan-admin-publish-button.offline:hover:not(:disabled) {
  background: rgba(100, 116, 139, 0.24);
}
.plan-admin-publish-button:disabled { cursor: wait; opacity: 0.62; }

.plan-admin-id {
  display: inline-flex;
  padding: 3px 8px;
  margin-bottom: 8px;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.12);
  color: #67e8f9;
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.plan-admin-name,
.plan-admin-subtitle,
.plan-admin-fields input {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.82);
  color: #e5eefb;
  border-radius: 10px;
  outline: none;
}

.plan-admin-name {
  display: block;
  padding: 8px 10px;
  font-size: 1.02rem;
  font-weight: 900;
}

.plan-admin-subtitle {
  margin-top: 8px;
  padding: 7px 10px;
  font-size: 0.82rem;
}

.plan-admin-switch,
.plan-admin-flags label,
.seckill-head label {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: rgba(226, 232, 240, 0.86);
  font-size: 0.78rem;
  font-weight: 800;
  white-space: nowrap;
}

.plan-admin-section,
.plan-admin-seckill {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
}

.plan-admin-section > strong {
  display: block;
  margin-bottom: 10px;
  color: #bfdbfe;
  font-size: 0.85rem;
}

.plan-admin-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.plan-admin-fields.three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.plan-admin-fields label {
  color: rgba(203, 213, 225, 0.75);
  font-size: 0.72rem;
  font-weight: 800;
}

.plan-admin-fields input {
  margin-top: 5px;
  padding: 9px 10px;
}

.plan-admin-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.plan-admin-flags input[type="number"] {
  width: 58px;
  margin-left: 4px;
  padding: 4px 6px;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.82);
  color: #e5eefb;
}

.plan-feature-section {
  padding: 16px;
  border: 1px solid rgba(96, 165, 250, 0.16);
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.42);
}

.plan-feature-editor {
  display: grid;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 16px;
  background: rgba(2, 6, 23, 0.2);
}

.plan-feature-row {
  display: grid;
  grid-template-columns: minmax(132px, 1fr) minmax(76px, 110px) 48px;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}

.plan-feature-row:last-child {
  border-bottom: 0;
}

.plan-feature-row.included {
  grid-template-columns: 24px minmax(110px, 1fr) minmax(88px, 130px) 68px;
}

.plan-feature-check {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: #bbf7d0;
  background: rgba(34, 197, 94, 0.14);
  font-weight: 900;
}

.plan-feature-row strong,
.plan-feature-toggle {
  color: #edf5ff;
  font-size: 0.84rem;
  font-weight: 900;
}

.plan-feature-toggle {
  display: inline-flex;
  align-items: center;
  gap: 9px;
}

.plan-feature-row input[type="number"] {
  width: 100%;
  min-width: 0;
  padding: 7px 9px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 10px;
  color: #e5eefb;
  background: rgba(15, 23, 42, 0.78);
}

.plan-feature-row em {
  color: rgba(203, 213, 225, 0.78);
  font-size: 0.75rem;
  font-style: normal;
  font-weight: 800;
  white-space: nowrap;
}

.plan-feature-note {
  display: block;
  margin-top: 10px;
  color: rgba(203, 213, 225, 0.6);
  font-size: 0.72rem;
  line-height: 1.55;
}

.seckill-live {
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(251, 191, 36, 0.12);
  color: #fde68a;
  font-size: 0.72rem;
  font-weight: 900;
}

.membership-plan-admin-card footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
}

.membership-plan-admin-card footer p {
  margin: 0;
  color: rgba(203, 213, 225, 0.72);
  font-size: 0.78rem;
  line-height: 1.55;
}

.plan-admin-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .membership-admin-summary,
  .membership-plan-admin-grid,
  .plan-admin-fields.three {
    grid-template-columns: 1fr;
  }
}

/* Custom premium checkbox style with green tick */
.plan-admin-flags input[type="checkbox"],
.seckill-head input[type="checkbox"],
.plan-admin-switch input[type="checkbox"] {
  appearance: none;
  -webkit-appearance: none;
  width: 17px;
  height: 17px;
  border: 1.5px solid rgba(148, 163, 184, 0.4);
  border-radius: 4px;
  background: rgba(15, 23, 42, 0.6);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  vertical-align: middle;
}

.plan-admin-flags input[type="checkbox"]:hover,
.seckill-head input[type="checkbox"]:hover,
.plan-admin-switch input[type="checkbox"]:hover {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.05);
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.2);
}

.plan-admin-flags input[type="checkbox"]:checked,
.seckill-head input[type="checkbox"]:checked,
.plan-admin-switch input[type="checkbox"]:checked {
  border-color: #10b981;
  background: #10b981;
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.4);
}

/* Green checkmark indicator */
.plan-admin-flags input[type="checkbox"]:checked::after,
.seckill-head input[type="checkbox"]:checked::after,
.plan-admin-switch input[type="checkbox"]:checked::after {
  content: "";
  position: absolute;
  left: 5px;
  top: 2px;
  width: 4px;
  height: 8px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

/* Make label look premium on hover */
.plan-admin-flags label,
.seckill-head label,
.plan-admin-switch {
  cursor: pointer;
}
.plan-admin-flags label:hover,
.seckill-head label:hover {
  color: #10b981 !important;
}

:root[data-theme="light"] .membership-plan-admin-card {
  background: linear-gradient(180deg, #ffffff, #f7faff);
  border-color: #d7e2f2;
  box-shadow: 0 14px 30px rgba(31, 52, 83, 0.08);
}

:root[data-theme="light"] .membership-plan-admin-card .plan-admin-name,
:root[data-theme="light"] .membership-plan-admin-card .plan-admin-subtitle,
:root[data-theme="light"] .membership-plan-admin-card .plan-admin-fields input,
:root[data-theme="light"] .membership-plan-admin-card .plan-feature-row input[type="number"] {
  background: #ffffff;
  border-color: #cfdced;
  color: #16243b;
}

:root[data-theme="light"] .membership-plan-admin-card .plan-admin-switch,
:root[data-theme="light"] .membership-plan-admin-card .plan-feature-toggle,
:root[data-theme="light"] .membership-plan-admin-card .plan-feature-row strong {
  color: #26364d;
}

:root[data-theme="light"] .plan-admin-publish-button {
  border-color: rgba(22, 163, 74, 0.34);
  background: rgba(34, 197, 94, 0.1);
  color: #15803d;
}
:root[data-theme="light"] .plan-admin-publish-button.offline {
  border-color: rgba(100, 116, 139, 0.3);
  background: rgba(100, 116, 139, 0.08);
  color: #475569;
}

:root[data-theme="light"] .membership-plan-admin-card .plan-admin-section > strong,
:root[data-theme="light"] .membership-plan-admin-card .plan-feature-row em,
:root[data-theme="light"] .membership-plan-admin-card .plan-feature-note,
:root[data-theme="light"] .membership-plan-admin-card footer p {
  color: #65748b;
}

:root[data-theme="light"] .membership-plan-admin-card .plan-feature-section,
:root[data-theme="light"] .membership-plan-admin-card .plan-feature-editor {
  background: #f8fbff;
  border-color: #dce6f3;
}
</style>
