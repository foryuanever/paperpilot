<template>
  <div class="team-page spatial-page reveal-ready">
    <div class="spatial-orb spatial-orb-blue" style="width: 400px; height: 400px; top: -100px; right: -50px;"></div>
    <div class="spatial-orb spatial-orb-warm" style="width: 300px; height: 300px; bottom: 10%; left: -100px;"></div>

    <section class="team-shell" data-reveal>
      <!-- Horizontal Seats Header -->
      <header class="team-seats-header">
        <div class="seats-left-container">
          <div class="seats-label-row">
            <h2>实验室席位</h2>
            <span class="seats-count-badge">{{ teamStore.usedSeats }} / {{ teamStore.totalSeats }}</span>
          </div>
          <div class="seats-avatar-row">
            <div
              v-for="member in teamStore.members"
              :key="member.id"
              class="seat-circle-avatar"
              :class="{ 'current-user-seat': member.isCurrentUser }"
              :data-user-email="member.email"
              title="查看个人卡片"
            >
              <div class="avatar-inner-wrapper">
                <img
                  v-if="member.isCurrentUser && authStore.profile.avatarUrl"
                  :src="authStore.profile.avatarUrl"
                  :alt="member.name"
                  class="seat-img"
                />
                <span
                  v-else
                  class="seat-fallback"
                  :style="{ backgroundColor: getAvatarColor(member.role) }"
                >
                  {{ getMemberInitial(member.name) }}
                </span>
                <span class="status-indicator-dot" :class="member.status"></span>
              </div>
              <span class="seat-member-name">{{ member.name.replace("导师", "").trim() }}</span>
            </div>
            <!-- Empty seats -->
            <button
              v-if="hasWriteAccess && teamStore.usedSeats < teamStore.totalSeats"
              class="seat-circle-avatar empty-seat-btn"
              @click="showInviteModal = true"
              :title="`添加成员，当前可用 ${teamStore.totalSeats - teamStore.usedSeats} 个席位`"
            >
              <span class="avatar-inner-wrapper"><span class="plus-symbol">+</span></span>
              <span class="seat-member-name">添加</span>
            </button>
            <button
              v-else-if="hasWriteAccess"
              class="seat-circle-avatar empty-seat-btn locked-seat-btn"
              @click="showToast('默认团队含 8 个席位，继续加人需开通导师车队会员')"
              title="开通导师车队会员后可扩展团队席位"
            >
              <span class="avatar-inner-wrapper"><span class="plus-symbol">+</span></span>
              <span class="seat-member-name">升级</span>
            </button>
          </div>
        </div>

        <div class="seats-right-container">
          <div class="team-plan-flag" :class="{ active: hasTeamFleetPlan }">
            <span>{{ hasTeamFleetPlan ? "导师车队会员" : "基础团队" }}</span>
            <strong>{{ hasTeamFleetPlan ? "全队共享权益" : "默认 8 席位" }}</strong>
          </div>
          <div class="team-identity-plate">
            <span class="plate-label">团队标示号</span>
            <strong class="plate-code">{{ teamStore.teamIdentifier }}</strong>
          </div>
          <button
            v-if="hasWriteAccess"
            class="invite-main-btn"
            @click="showInviteModal = true"
          >
            邀请成员
          </button>
        </div>
      </header>

      <!-- Split Layout based on Roles -->
      <div v-if="hasWriteAccess" class="dashboard-split-layout tutor-view">
        <!-- Tutor: Member Management Column (Left) -->
        <div class="dashboard-col left-col">
          <div class="col-header-row">
            <h3>团队成员管理</h3>
            <span class="col-meta-pill">管理权限</span>
          </div>

          <div class="members-cards-container">
            <article
              v-for="member in teamStore.members"
              :key="member.id"
              class="member-management-card"
            >
              <div class="card-identity-row">
                <div class="avatar-large-shell" :data-user-email="member.email" title="查看个人卡片">
                  <img
                    v-if="member.isCurrentUser && authStore.profile.avatarUrl"
                    :src="authStore.profile.avatarUrl"
                    :alt="member.name"
                    class="avatar-large-img"
                  />
                  <span
                    v-else
                    class="avatar-large-fallback"
                    :style="{ backgroundColor: getAvatarColor(member.role) }"
                  >
                    {{ getMemberInitial(member.name) }}
                  </span>
                  <span class="status-indicator-dot" :class="member.status"></span>
                </div>
                <div class="identity-info">
                  <div class="name-role-line">
                    <strong>{{ member.name }}</strong>
                    <span class="role-badge" :class="getRoleClass(member.role)">{{ member.role }}</span>
                  </div>
                  <span class="email-subtext">{{ member.email }}</span>
                </div>
              </div>

              <div class="card-details-grid">
                <div class="card-detail-item">
                  <span>加入时间</span>
                  <strong>{{ member.registerTime }}</strong>
                </div>
                <div class="card-detail-item">
                  <span>活跃时长</span>
                  <strong>{{ formatActiveTime(member.activeTime) }}</strong>
                </div>
                <div class="card-detail-item">
                  <span>今日打卡</span>
                  <strong :class="{ 'text-checked': getCheckinStatus(member.id) === '已打卡' }">
                    {{ getCheckinStatus(member.id) }}
                  </strong>
                </div>
              </div>

              <!-- Admin action row for quota limits & roles -->
              <div class="card-actions-wrapper">
                <div class="quota-management-bar">
                  <div class="quota-label-line">
                    <span>Token 限制</span>
                    <strong>{{ formatTokens(member.tokenUsed) }} / {{ formatTokens(member.tokenLimit) }}</strong>
                  </div>
                  <div class="progress-bar-track">
                    <span
                      class="progress-fill-bar"
                      :class="getQuotaColorClass(member.tokenUsed / member.tokenLimit)"
                      :style="{ width: `${Math.min(100, (member.tokenUsed / member.tokenLimit) * 100)}%` }"
                    ></span>
                  </div>

                  <!-- Inline Quota adjustment tool -->
                  <div v-if="editingMemberId === member.id" class="quota-edit-inputs">
                    <input v-model.number="tempLimit" type="number" min="0" step="100000" />
                    <button class="action-btn-mini confirm" @click="saveQuota(member.id)">保存</button>
                    <button class="action-btn-mini cancel" @click="editingMemberId = null">取消</button>
                  </div>
                  <div v-else class="quota-trigger-actions">
                    <button class="action-btn-link" @click="startEditQuota(member)">修改额度</button>
                    <button
                      v-if="member.id !== currentMemberId"
                      class="action-btn-link"
                      @click="toggleRole(member)"
                    >
                      变更角色
                    </button>
                    <button
                      v-if="member.id !== currentMemberId"
                      class="action-btn-link danger"
                      @click="deleteMember(member)"
                    >
                      移出团队
                    </button>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </div>

        <!-- Tutor: Work rhythm control (Right) -->
        <div class="dashboard-col right-col">
          <!-- Quick summary widget -->
          <div class="summary-metric-cards">
            <div class="tutor-metric-card">
              <span>Token 资产池</span>
              <strong>{{ formatTokens(teamStore.groupTokenPool) }}</strong>
            </div>
            <div class="tutor-metric-card">
              <span>今日签到率</span>
              <strong>{{ checkedInCount }} / {{ teamStore.members.length }}</strong>
            </div>
            <div class="tutor-metric-card">
              <span>任务完成度</span>
              <strong>{{ completionRate }}%</strong>
            </div>
          </div>

          <!-- Tasks control panel -->
          <div class="admin-action-section">
            <div class="section-title-bar">
              <h4>科研任务日程</h4>
              <button class="btn-toggle-form" @click="toggleTaskForm">
                {{ showAddTaskForm ? "收起表单" : "+ 添加任务" }}
              </button>
            </div>

            <form v-if="showAddTaskForm" class="tutor-inline-form" @submit.prevent="submitTask">
              <div class="form-row-grid">
                <input v-model="newTaskTitle" type="text" placeholder="任务标题" required />
                <input v-model="newTaskDeadline" type="datetime-local" required />
              </div>
              <textarea v-model="newTaskDesc" placeholder="任务说明与要求"></textarea>
              
              <!-- File Attachment Uploader -->
              <div class="attachment-uploader">
                <label class="uploader-dropzone">
                  <input type="file" multiple @change="handleTaskFilesUpload" style="display: none;" />
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="uploader-icon" style="width: 14px; height: 14px; color: #64748b;">
                    <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
                  </svg>
                  <span>点击上传图片与附件</span>
                </label>
                <div v-if="taskAttachments.length" class="uploader-preview-list">
                  <div v-for="(file, idx) in taskAttachments" :key="idx" class="preview-item">
                    <span class="file-name">{{ file.name }} ({{ file.size }})</span>
                    <button type="button" class="remove-file-btn" @click="removeTaskFile(idx)">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 12px; height: 12px;"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                    </button>
                  </div>
                </div>
              </div>

              <div class="form-submit-row">
                <button v-if="editingTaskId" type="button" class="cancel-edit-btn" @click="resetTaskForm">取消修改</button>
                <button type="submit" class="submit-form-btn">{{ editingTaskId ? "保存修改" : "发布任务" }}</button>
              </div>
            </form>

            <div class="admin-items-list">
              <div
                v-for="task in paginatedTasks"
                :key="task.id"
                class="admin-item-card task-card"
                :class="{ completed: task.status === '已完成' }"
              >
                <div class="item-main-content">
                  <div class="item-title-row">
                    <h5 @click="openDetailModal('task', task)" style="cursor: pointer; text-decoration: underline; text-underline-offset: 4px;">{{ task.title }}</h5>
                    <div class="published-actions">
                      <span class="status-tag" :class="{ done: task.status === '已完成' }">{{ task.status }}</span>
                      <button type="button" class="published-action-btn" @click="startEditTask(task)">修改</button>
                      <button type="button" class="published-action-btn danger" @click="retractTask(task)">撤回</button>
                    </div>
                  </div>
                  <p class="task-desc-truncated" @click="openDetailModal('task', task)">{{ task.description || "暂无说明" }}</p>
                  
                  <!-- Tutor Task Attachments -->
                  <div v-if="task.attachments && task.attachments.length" class="task-attachments-list">
                    <div v-for="(file, idx) in task.attachments" :key="idx" class="attachment-wrapper">
                      <!-- Image Preview -->
                      <div v-if="isImageAttachment(file)" class="image-attachment-card" @click="previewImage(file)">
                        <img :src="file.data" class="task-attachment-img-preview" title="点击下载图片" />
                        <span class="img-name">{{ file.name }}</span>
                      </div>
                      <!-- General File -->
                      <button v-else type="button" class="document-file-row" @click="downloadAttachment(file)">
                        <span class="document-file-icon" :class="getDocumentIconClass(file.name)">
                          {{ getDocumentIconLabel(file.name) }}
                        </span>
                        <span class="document-file-meta">
                          <strong :title="file.name">{{ file.name }}</strong>
                          <small>{{ file.size || "未知大小" }}</small>
                        </span>
                      </button>
                    </div>
                  </div>

                  <small>截止: {{ formatDeadlineString(task.deadline) }}</small>
                </div>
              </div>
            </div>
            <!-- Pagination Bar for Mentor Tasks -->
            <div class="pagination-bar" v-if="taskTotalPages > 1">
              <button type="button" class="pager-btn" :disabled="taskPage === 1" @click="taskPage--">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="15 18 9 12 15 6"></polyline></svg>
              </button>
              <span class="pager-info">{{ taskPage }} / {{ taskTotalPages }}</span>
              <button type="button" class="pager-btn" :disabled="taskPage === taskTotalPages" @click="taskPage++">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="9 18 15 12 9 6"></polyline></svg>
              </button>
            </div>
          </div>

          <!-- Announcements publisher -->
          <div class="admin-action-section">
            <div class="section-title-bar">
              <h4>重要通知公告</h4>
              <button class="btn-toggle-form" @click="toggleAnnouncementForm">
                {{ showAddAnnForm ? "收起表单" : "+ 发布公告" }}
              </button>
            </div>

            <form v-if="showAddAnnForm" class="tutor-inline-form" @submit.prevent="submitAnnouncement">
              <div class="form-row-grid">
                <input v-model="newAnnTitle" type="text" placeholder="公告标题" required />
                <input v-model="newAnnLink" type="url" placeholder="跳转链接（可选）" />
              </div>
              <textarea v-model="newAnnContent" placeholder="通知详细内容" required></textarea>
              <div class="announcement-image-uploader" style="margin-top: 10px; margin-bottom: 10px;">
                <label class="uploader-dropzone" style="padding: 10px; font-size: 11px; display: flex; align-items: center; justify-content: center; gap: 8px; cursor: pointer; border: 1px dashed rgba(0, 0, 0, 0.15); border-radius: 8px; background: rgba(0, 0, 0, 0.012);">
                  <input type="file" accept="image/*" @change="handleAnnImageUpload" style="display: none;" />
                  <span v-if="newAnnImage">已选配图: {{ newAnnImageName }}</span>
                  <span v-else>+ 上传通知配图 (可选)</span>
                </label>
              </div>
              <div class="announcement-attachment-uploader" style="margin-top: 10px; margin-bottom: 10px;">
                <label class="uploader-dropzone" style="padding: 10px; font-size: 11px; display: flex; align-items: center; justify-content: center; gap: 8px; cursor: pointer; border: 1px dashed rgba(0, 0, 0, 0.15); border-radius: 8px; background: rgba(0, 0, 0, 0.012);">
                  <input type="file" @change="handleAnnAttachmentUpload" style="display: none;" />
                  <span v-if="newAnnAttachmentName">已选附件: {{ newAnnAttachmentName }} ({{ newAnnAttachmentSize }})</span>
                  <span v-else>+ 上传附件 (可选)</span>
                </label>
              </div>
              <div class="form-submit-row">
                <button v-if="editingAnnouncementId" type="button" class="cancel-edit-btn" @click="resetAnnouncementForm">取消修改</button>
                <button type="submit" class="submit-form-btn">{{ editingAnnouncementId ? "保存修改" : "发布通知" }}</button>
              </div>
            </form>

            <div class="admin-items-list">
              <div
                v-for="ann in paginatedAnnouncements"
                :key="ann.id"
                class="admin-item-card task-card announcement-card"
              >
                <div class="item-main-content">
                  <div class="item-title-row">
                    <h5 @click="openDetailModal('announcement', ann)" style="cursor: pointer; text-decoration: underline; text-underline-offset: 4px;">{{ ann.title }}</h5>
                    <div class="published-actions">
                      <span class="status-tag published">已发布</span>
                      <button type="button" class="published-action-btn" @click="startEditAnnouncement(ann)">修改</button>
                      <button type="button" class="published-action-btn danger" @click="retractAnnouncement(ann)">撤回</button>
                    </div>
                  </div>
                  <p class="task-desc-truncated" @click="openDetailModal('announcement', ann)">{{ ann.content || "暂无内容" }}</p>
                  <div v-if="ann.image || ann.link || ann.attachmentName" class="task-attachments-list">
                    <div v-if="ann.image" class="attachment-wrapper">
                      <div class="image-attachment-card" @click="previewImage({ data: ann.image, name: ann.title })">
                        <img :src="ann.image" class="task-attachment-img-preview" title="点击预览图片" />
                        <span class="img-name">{{ ann.title }}配图</span>
                      </div>
                    </div>
                    <a v-if="ann.link" :href="ann.link" target="_blank" class="document-link-row" @click.stop>
                      <span class="file-format-badge">LINK</span>
                      <span class="attachment-name">查看通知链接</span>
                    </a>
                    <button v-if="ann.attachmentName" type="button" class="document-file-row" @click="downloadAnnouncementAttachment(ann)">
                      <span class="document-file-icon" :class="getDocumentIconClass(ann.attachmentName)">
                        {{ getDocumentIconLabel(ann.attachmentName) }}
                      </span>
                      <span class="document-file-meta">
                        <strong :title="ann.attachmentName">{{ ann.attachmentName }}</strong>
                        <small>{{ getAnnouncementAttachmentSize(ann) }}</small>
                      </span>
                    </button>
                  </div>
                  <small>发布: {{ ann.publishTime }}</small>
                </div>
              </div>
            </div>
            <!-- Pagination for announcements -->
            <div class="pagination-bar" v-if="annTotalPages > 1">
              <button type="button" class="pager-btn" :disabled="annPage === 1" @click="annPage--">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="15 18 9 12 15 6"></polyline></svg>
              </button>
              <span class="pager-info">{{ annPage }} / {{ annTotalPages }}</span>
              <button type="button" class="pager-btn" :disabled="annPage === annTotalPages" @click="annPage++">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="9 18 15 12 9 6"></polyline></svg>
              </button>
            </div>
          </div>

          <!-- Leaderboard in Mentor view -->
          <div class="admin-action-section">
            <h4>实验室活跃排行</h4>
            <div class="leaderboard-container">
              <div
                v-for="(member, idx) in paginatedLeaderboard"
                :key="member.id"
                class="leader-row-item"
              >
                <span class="leader-rank-no">{{ (leaderPage - 1) * leaderPageSize + idx + 1 }}</span>
                <div class="leader-info-box">
                  <div class="leader-meta-line">
                    <strong>{{ member.name }}</strong>
                    <span>{{ formatActiveTime(member.activeTime) }}</span>
                  </div>
                  <div class="leader-progress-track">
                    <div
                      class="leader-progress-bar"
                      :style="{ width: `${Math.min(100, ((member.activeTime || 0) / (maxActiveTime || 1)) * 100)}%` }"
                    ></div>
                  </div>
                </div>
              </div>
            </div>
            <!-- Pagination for leaderboard -->
            <div class="pagination-bar" v-if="leaderTotalPages > 1">
              <button type="button" class="pager-btn" :disabled="leaderPage === 1" @click="leaderPage--">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="15 18 9 12 15 6"></polyline></svg>
              </button>
              <span class="pager-info">{{ leaderPage }} / {{ leaderTotalPages }}</span>
              <button type="button" class="pager-btn" :disabled="leaderPage === leaderTotalPages" @click="leaderPage++">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="9 18 15 12 9 6"></polyline></svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Student View -->
      <div v-else class="dashboard-split-layout student-view">
        <!-- Student: Left Column (My Workbench) -->
        <div class="dashboard-col left-col">
          <!-- Sign in Widget -->
          <div class="student-glass-card workbench-sign-in">
            <div class="sign-in-header">
              <h3>实验室学术签到</h3>
              <div class="checkin-clock">
                <span>{{ currentResearchDate }}</span>
                <strong>{{ currentResearchClock }}</strong>
              </div>
            </div>
            <div class="sign-in-body">
              <div class="sign-in-text">
                <p v-if="currentCheckinItem?.status === '已打卡'">
                  您今日已于 <strong>{{ currentCheckinItem.time }}</strong> 完成签到，已连续 <strong>{{ currentCheckinItem.streak || checkinStreak }}</strong> 天。
                </p>
                <p v-else>
                  今天还没签到，快点击按钮记录今天的科研时长吧。
                </p>
              </div>
              <button
                class="student-primary-action-btn"
                :class="{ 'already-checked': currentCheckinItem?.status === '已打卡' }"
                @click="doCheckin"
              >
                {{ currentCheckinItem?.status === '已打卡' ? '查看打卡记录' : '立即签到打卡' }}
              </button>
            </div>
          </div>

          <!-- My Token & Level Info -->
          <div class="student-glass-card my-stats-panel">
            <h3>我的科研状态</h3>
            <div class="stats-pills-row">
              <div class="stat-pill-box">
                <span>科研等级</span>
                <strong>Lv.{{ getMemberLevelInfo(currentUserMember?.activeTime).level }}</strong>
                <small>{{ getMemberLevelInfo(currentUserMember?.activeTime).title }}</small>
              </div>
              <div class="stat-pill-box">
                <span>当前在线</span>
                <strong>{{ formatActiveTime(currentUserMember?.activeTime) }}</strong>
                <small>有效学术时长</small>
              </div>
            </div>

            <div class="student-quota-block">
              <div class="student-quota-header">
                <span>Token 使用量</span>
                <strong>{{ formatTokens(currentUserMember?.tokenUsed || 0) }} / {{ formatTokens(currentUserMember?.tokenLimit || 0) }}</strong>
              </div>
              <div class="progress-bar-track">
                <span
                  class="progress-fill-bar"
                  :class="getQuotaColorClass((currentUserMember?.tokenUsed || 0) / (currentUserMember?.tokenLimit || 1))"
                  :style="{ width: `${Math.min(100, ((currentUserMember?.tokenUsed || 0) / (currentUserMember?.tokenLimit || 1)) * 100)}%` }"
                ></span>
              </div>
            </div>
          </div>

          <!-- Team Tasks Assigned -->
          <div class="student-glass-card tasks-checklist-panel">
            <div class="checklist-header">
              <h3>待办科研任务</h3>
              <span class="task-count-indicator">共 {{ teamStore.tasks.length }} 项</span>
            </div>

            <div class="student-task-list">
              <article
                v-for="task in paginatedTasks"
                :key="task.id"
                class="student-task-item"
                :class="{ completed: task.status === '已完成' }"
              >
                <div class="task-info">
                  <h4 @click="openDetailModal('task', task)" style="cursor: pointer; text-decoration: underline; text-underline-offset: 4px;">{{ task.title }}</h4>
                  <p v-if="task.description" class="task-desc-truncated" @click="openDetailModal('task', task)">{{ task.description }}</p>
                  
                  <!-- Student Task Attachments -->
                  <div v-if="task.attachments && task.attachments.length" class="task-attachments-list">
                    <div v-for="(file, idx) in task.attachments" :key="idx" class="attachment-wrapper">
                      <!-- Image Preview -->
                      <div v-if="isImageAttachment(file)" class="image-attachment-card" @click="previewImage(file)">
                        <img :src="file.data" class="task-attachment-img-preview" title="点击预览图片" />
                        <span class="img-name">{{ file.name }}</span>
                      </div>
                      <!-- General File -->
                      <button v-else type="button" class="document-file-row" @click="downloadAttachment(file)">
                        <span class="document-file-icon" :class="getDocumentIconClass(file.name)">
                          {{ getDocumentIconLabel(file.name) }}
                        </span>
                        <span class="document-file-meta">
                          <strong :title="file.name">{{ file.name }}</strong>
                          <small>{{ file.size || "未知大小" }}</small>
                        </span>
                      </button>
                    </div>
                  </div>

                  <small>截止时间: {{ formatDeadlineString(task.deadline) }}</small>
                </div>

                <!-- Deadline Countdown with Hourglass Icon (Moved to right side) -->
                <div v-if="getTaskDeadlineCountdown(task.deadline)" 
                     class="task-countdown-tag" 
                     :class="getTaskDeadlineCountdown(task.deadline).colorClass">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="funnel-icon">
                    <path d="M5 2h14v3.5a1 1 0 0 1-.3.7L13 12l5.7 5.8a1 1 0 0 1 .3.7V22H5v-3.5a1 1 0 0 1 .3-.7L11 12 5.3 6.2a1 1 0 0 1-.3-.7V2z" />
                    <path d="M12 12v6M9 19h6M9 5h6" />
                  </svg>
                  <span>{{ getTaskDeadlineCountdown(task.deadline).text }}</span>
                </div>
              </article>
              <div v-if="!teamStore.tasks.length" class="empty-state-text">暂无分配的任务。</div>
            </div>
            <!-- Pagination Bar for Student Tasks -->
            <div class="pagination-bar" v-if="taskTotalPages > 1">
              <button type="button" class="pager-btn" :disabled="taskPage === 1" @click="taskPage--">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="15 18 9 12 15 6"></polyline></svg>
              </button>
              <span class="pager-info">{{ taskPage }} / {{ taskTotalPages }}</span>
              <button type="button" class="pager-btn" :disabled="taskPage === taskTotalPages" @click="taskPage++">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="9 18 15 12 9 6"></polyline></svg>
              </button>
            </div>
          </div>
        </div>

        <!-- Student: Right Column (Briefing) -->
        <div class="dashboard-col right-col">
          <!-- Announcements carousel style -->
          <div class="student-glass-card announcements-briefing">
            <h3>重要公告通知</h3>
            <div class="announcements-briefing-stack">
              <article
                v-for="ann in paginatedAnnouncements"
                :key="ann.id"
                class="briefing-ann-card"
              >
                <div class="ann-header">
                  <h4 @click="openDetailModal('announcement', ann)" style="cursor: pointer; text-decoration: underline; text-underline-offset: 4px;">{{ ann.title }}</h4>
                  <small>{{ ann.publishTime }}</small>
                </div>
                <p class="ann-content-truncated" @click="openDetailModal('announcement', ann)">{{ ann.content }}</p>
                <div v-if="ann.image || ann.link || ann.attachmentName" class="task-attachments-list">
                  <div v-if="ann.image" class="image-attachment-card" @click="previewImage({ data: ann.image, name: ann.title })">
                    <img :src="ann.image" class="task-attachment-img-preview" title="点击预览图片" />
                    <span class="img-name">{{ ann.title }}配图</span>
                  </div>
                  <a v-if="ann.link" :href="ann.link" target="_blank" class="document-link-row" @click.stop>
                    <span class="file-format-badge">LINK</span>
                    <span class="attachment-name">查看通知链接</span>
                  </a>
                  <button v-if="ann.attachmentName" type="button" class="document-file-row" @click="downloadAnnouncementAttachment(ann)">
                    <span class="document-file-icon" :class="getDocumentIconClass(ann.attachmentName)">
                      {{ getDocumentIconLabel(ann.attachmentName) }}
                    </span>
                    <span class="document-file-meta">
                      <strong :title="ann.attachmentName">{{ ann.attachmentName }}</strong>
                      <small>{{ getAnnouncementAttachmentSize(ann) }}</small>
                    </span>
                  </button>
                </div>
              </article>
              <div v-if="!teamStore.announcements.length" class="empty-state-text">目前没有公告。</div>
            </div>
            <!-- Pagination Bar for Student Announcements -->
            <div class="pagination-bar" v-if="annTotalPages > 1">
              <button type="button" class="pager-btn" :disabled="annPage === 1" @click="annPage--">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="15 18 9 12 15 6"></polyline></svg>
              </button>
              <span class="pager-info">{{ annPage }} / {{ annTotalPages }}</span>
              <button type="button" class="pager-btn" :disabled="annPage === annTotalPages" @click="annPage++">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="9 18 15 12 9 6"></polyline></svg>
              </button>
            </div>
          </div>

          <!-- Active Leaderboard -->
          <div class="student-glass-card active-leaderboard-card">
            <h3>实验室今日排行</h3>
            <div class="student-leaderboard-stack">
              <div
                v-for="(member, index) in paginatedLeaderboard"
                :key="member.id"
                class="student-leader-row"
                :class="{ 'is-current-user': member.isCurrentUser }"
              >
                <div class="rank-container">
                  <span class="rank-number" :class="'rank-' + ((leaderPage - 1) * leaderPageSize + index + 1)">{{ (leaderPage - 1) * leaderPageSize + index + 1 }}</span>
                </div>
                <div class="leader-details">
                  <div class="name-time-row">
                    <strong>{{ member.name }}</strong>
                    <span>{{ formatActiveTime(member.activeTime) }}</span>
                  </div>
                  <div class="progress-bar-track">
                    <span
                      class="progress-fill-bar"
                      :style="{ width: `${Math.min(100, ((member.activeTime || 0) / (maxActiveTime || 1)) * 100)}%` }"
                    ></span>
                  </div>
                </div>
              </div>
            </div>
            <!-- Pagination Bar for Student Leaderboard -->
            <div class="pagination-bar" v-if="leaderTotalPages > 1">
              <button type="button" class="pager-btn" :disabled="leaderPage === 1" @click="leaderPage--">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="15 18 9 12 15 6"></polyline></svg>
              </button>
              <span class="pager-info">{{ leaderPage }} / {{ leaderTotalPages }}</span>
              <button type="button" class="pager-btn" :disabled="leaderPage === leaderTotalPages" @click="leaderPage++">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="9 18 15 12 9 6"></polyline></svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Unified Team Resource Library Section (Full Width, Visible to All) -->
      <div class="team-resources-section spatial-glass-panel" data-reveal>
        <div class="resources-header">
          <div class="header-left">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="res-sec-icon" style="width:20px; height:20px; color:var(--spatial-accent, #0066ff);">
              <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
            </svg>
            <h3>团队共享资源库</h3>
          </div>
          
          <!-- Upload Button -->
          <label class="upload-resource-btn apple-btn apple-btn-primary" style="margin: 0;">
            <input type="file" @change="handleResourceUpload" style="display: none;" />
            <span>+ 上传共享材料</span>
          </label>
        </div>

        <div v-if="teamStore.resources && teamStore.resources.length" class="resources-grid">
          <div v-for="res in paginatedResources" :key="res.id" class="resource-card">
            <div class="res-card-left">
              <div class="res-icon-capsule">
                <span class="file-format-badge">{{ getFileExtension(res.name) }}</span>
              </div>
              <div class="res-meta-info">
                <h5 class="res-name" :title="res.name">{{ res.name }}</h5>
                <div class="res-sub-meta">
                  <span class="res-uploader">上传人: {{ res.uploader }}</span>
                  <span class="res-time">{{ res.uploadTime }}</span>
                  <span class="res-size">大小: {{ res.size }}</span>
                </div>
              </div>
            </div>
            
            <div class="res-actions">
              <button class="res-action-btn download" @click="downloadResource(res)">下载</button>
              <button class="res-action-btn delete" @click="deleteResource(res.id)">删除</button>
            </div>
          </div>
        </div>
        <!-- Pagination for resources -->
        <div class="pagination-bar" v-if="resTotalPages > 1" style="margin-top: 20px;">
          <button type="button" class="pager-btn" :disabled="resPage === 1" @click="resPage--">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="15 18 9 12 15 6"></polyline></svg>
          </button>
          <span class="pager-info">{{ resPage }} / {{ resTotalPages }}</span>
          <button type="button" class="pager-btn" :disabled="resPage === resTotalPages" @click="resPage++">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon"><polyline points="9 18 15 12 9 6"></polyline></svg>
          </button>
        </div>
        <div v-else class="resources-empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon" style="width: 48px; height: 48px; color: #aeaeb2; margin-bottom: 12px;">
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
          </svg>
          <p>暂无团队共享资源，点击右上角上传学术材料、数据集或压缩包。</p>
        </div>
      </div>
    </section>

    <!-- Invitation Modal -->
    <Transition name="fade">
      <div v-if="showInviteModal" class="overlay" @click="showInviteModal = false">
        <div class="modal-card" @click.stop>
          <div class="modal-head">
            <div>
              <span class="panel-eyebrow">Invite</span>
              <h3>邀请新成员</h3>
            </div>
            <button class="close-btn" @click="showInviteModal = false">关闭</button>
          </div>

          <form class="modal-form" @submit.prevent="submitInvite">
            <label>
              <span>姓名</span>
              <input v-model="newMemberName" type="text" placeholder="输入成员姓名" required />
            </label>
            <label>
              <span>邮箱</span>
              <input v-model="newMemberEmail" type="email" placeholder="输入邮箱地址" required />
            </label>
            <div class="form-grid">
              <label>
                <span>角色</span>
                <select v-model="newMemberRole">
                  <option value="学生">学生</option>
                  <option value="特权用户">特权用户</option>
                  <option value="管理员">管理员</option>
                </select>
              </label>
              <label>
                <span>初始额度</span>
                <input v-model.number="newMemberLimit" type="number" min="0" step="100000" />
              </label>
            </div>
            <div class="form-actions">
              <button type="button" class="apple-btn" @click="showInviteModal = false">取消</button>
              <button type="submit" class="apple-btn apple-btn-primary">发送邀请</button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- Profile Detail Modal -->
    <Transition name="fade">
      <div v-if="showProfileModal && selectedMember" class="overlay" @click="showProfileModal = false">
        <div class="modal-card profile-modal" @click.stop>
          <div class="profile-showcase">
            <div class="avatar-shell hero-avatar" :data-user-email="selectedMember.email" title="查看个人卡片">
              <img
                v-if="selectedMember.isCurrentUser && authStore.profile.avatarUrl"
                :src="authStore.profile.avatarUrl"
                :alt="selectedMember.name"
                class="avatar-image"
              />
              <span
                v-else
                class="avatar-fallback"
                :style="{ backgroundColor: getAvatarColor(selectedMember.role) }"
              >
                {{ getMemberInitial(selectedMember.name) }}
              </span>
              <span class="status-dot" :class="selectedMember.status"></span>
            </div>
            <div class="profile-showcase-copy">
              <span class="role-pill" :class="getRoleClass(selectedMember.role)">{{ selectedMember.role }}</span>
              <h3>{{ selectedMember.name }}</h3>
              <p>{{ selectedMember.email }}</p>
            </div>
          </div>

          <div class="detail-grid">
            <div class="detail-item">
              <span>注册时间</span>
              <strong>{{ selectedMember.registerTime }}</strong>
            </div>
            <div class="detail-item">
              <span>科研等级</span>
              <strong>{{ getMemberLevelInfo(selectedMember.activeTime).title }}</strong>
            </div>
            <div class="detail-item">
              <span>在线时长</span>
              <strong>{{ formatActiveTime(selectedMember.activeTime) }}</strong>
            </div>
            <div class="detail-item">
              <span>签到状态</span>
              <strong>{{ getCheckinStatus(selectedMember.id) }}</strong>
            </div>
          </div>

          <div v-if="canViewQuota(selectedMember)" class="quota-block roomy">
            <div class="quota-head">
              <span>Token 使用情况</span>
              <strong>{{ formatTokens(selectedMember.tokenUsed) }} / {{ formatTokens(selectedMember.tokenLimit) }}</strong>
            </div>
            <div class="progress-track">
              <span
                class="progress-fill"
                :class="getQuotaColorClass(selectedMember.tokenUsed / selectedMember.tokenLimit)"
                :style="{ width: `${Math.min(100, (selectedMember.tokenUsed / selectedMember.tokenLimit) * 100)}%` }"
              ></span>
            </div>
          </div>

          <div class="form-actions">
            <button class="apple-btn apple-btn-primary" @click="showProfileModal = false">知道了</button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="slide-up">
      <div v-if="toastMessage" class="toast">{{ toastMessage }}</div>
    </Transition>

    <Transition name="fade">
      <div v-if="showCheckinModal" class="overlay" @click="showCheckinModal = false">
        <div class="modal-card checkin-success-modal" @click.stop>
          <button class="close-btn modal-close-float" @click="showCheckinModal = false">关闭</button>
          <div class="checkin-medal">✓</div>
          <span class="panel-eyebrow">Daily Research Check-in</span>
          <h3>{{ checkinDialogTitle }}</h3>
          <p>{{ checkinMotivation }}</p>
          <div class="checkin-streak-panel">
            <span>连续打卡</span>
            <strong>{{ checkinStreak }} 天</strong>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Image Preview Lightbox Overlay -->
    <Transition name="fade">
      <div v-if="showImagePreview" class="modal-overlay image-preview-overlay" @click="showImagePreview = false">
        <div class="image-preview-container" @click.stop>
          <button class="close-btn preview-close-btn" @click="showImagePreview = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 20px; height: 20px;">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
          <div class="preview-toolbar">
            <button type="button" @click.stop.prevent="changeImageZoom(-0.25)" :disabled="previewScale <= 0.5">−</button>
            <span>{{ Math.round(previewScale * 100) }}%</span>
            <button type="button" @click.stop.prevent="changeImageZoom(0.25)" :disabled="previewScale >= 4">+</button>
            <button type="button" @click.stop.prevent="resetImageZoom">还原</button>
          </div>
          <div class="preview-image-stage">
            <img
              :src="previewImageUrl"
              class="full-preview-image"
              :style="{ transform: `scale(${previewScale})` }"
            />
          </div>
          <div class="preview-caption">{{ previewImageName }}</div>
        </div>
      </div>
    </Transition>

    <!-- Detail Modal for Tasks & Announcements -->
    <Transition name="fade">
      <div v-if="showDetailModal" class="modal-overlay image-preview-overlay" @click="showDetailModal = false">
        <div class="spatial-glass-panel detail-modal-card" @click.stop>
          <div class="detail-modal-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid rgba(255, 255, 255, 0.1); padding-bottom: 12px;">
            <span class="detail-type-badge" style="background: var(--spatial-accent, #0066ff); color: white; padding: 4px 12px; border-radius: 20px; font-size: 0.75rem; font-weight: 600;">
              {{ detailModalType === 'task' ? '科研任务详情' : '重要公告通知' }}
            </span>
            <button class="close-btn" @click="showDetailModal = false" style="background: none; border: none; color: inherit; cursor: pointer;">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 20px; height: 20px;">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>
          <div class="detail-modal-body" v-if="detailModalContent">
            <h3 class="detail-title" style="font-size: 1.25rem; font-weight: 700; margin: 0 0 16px; line-height: 1.4;">{{ detailModalContent.title }}</h3>
            
            <div class="detail-meta" style="display: flex; gap: 20px; margin-bottom: 20px; font-size: 0.8rem; color: #8e8e93;">
              <span class="detail-meta-item" v-if="detailModalType === 'task'">
                <strong>截止时间:</strong> {{ formatDeadlineString(detailModalContent.deadline) }}
              </span>
              <span class="detail-meta-item" v-if="detailModalType === 'task'">
                <strong>状态:</strong> 
                <span class="status-tag" :class="{ done: detailModalContent.status === '已完成' }" style="margin-left: 4px;">
                  {{ detailModalContent.status }}
                </span>
              </span>
              <span class="detail-meta-item" v-if="detailModalType === 'announcement'">
                <strong>发布时间:</strong> {{ detailModalContent.publishTime }}
              </span>
            </div>

            <div class="detail-description-section" style="margin-bottom: 24px;">
              <h4 style="font-size: 0.9rem; font-weight: 600; margin-bottom: 8px; color: #8e8e93;">详细内容</h4>
              <p class="detail-content-text" style="font-size: 0.95rem; line-height: 1.6; white-space: pre-wrap; margin: 0; color: inherit; margin-bottom: 16px;">
                {{ detailModalContent.description || detailModalContent.content || '暂无说明' }}
              </p>
              
              <!-- Announcement Link & Image inside details -->
              <div v-if="detailModalType === 'announcement' && (detailModalContent.image || detailModalContent.link || detailModalContent.attachmentName)" class="detail-announcement-attachments" style="margin-top: 16px; display: flex; flex-direction: column; gap: 12px;">
                <div v-if="detailModalContent.image" style="max-width: 100%; text-align: center;">
                  <img :src="detailModalContent.image" class="detail-preview-image" @click="previewImage({ data: detailModalContent.image, name: detailModalContent.title })" />
                </div>
                <div v-if="detailModalContent.link">
                  <span style="font-size: 0.9rem; font-weight: 600; color: #8e8e93; display: block; margin-bottom: 4px;">相关链接:</span>
                  <a :href="detailModalContent.link" target="_blank" style="color: var(--spatial-accent, #0066ff); text-decoration: none; word-break: break-all; font-size: 0.95rem;">
                    {{ detailModalContent.link }}
                  </a>
                </div>
                <button v-if="detailModalContent.attachmentName" type="button" class="document-file-row" @click="downloadAnnouncementAttachment(detailModalContent)">
                  <span class="document-file-icon" :class="getDocumentIconClass(detailModalContent.attachmentName)">
                    {{ getDocumentIconLabel(detailModalContent.attachmentName) }}
                  </span>
                  <span class="document-file-meta">
                    <strong>{{ detailModalContent.attachmentName }}</strong>
                    <small>{{ getAnnouncementAttachmentSize(detailModalContent) }}</small>
                  </span>
                </button>
              </div>
            </div>

            <!-- Attachments if any -->
            <div v-if="detailModalType === 'task' && detailModalContent.attachments && detailModalContent.attachments.length" class="detail-attachments-section">
              <h4 style="font-size: 0.9rem; font-weight: 600; margin-bottom: 12px; color: #8e8e93;">任务附件 ({{ detailModalContent.attachments.length }})</h4>
              <div class="task-attachments-list">
                <div v-for="(file, idx) in detailModalContent.attachments" :key="idx" class="attachment-wrapper">
                  <!-- Image Preview -->
                  <div v-if="isImageAttachment(file)" class="image-attachment-card" @click="previewImage(file)">
                    <img :src="file.data" class="task-attachment-img-preview" title="点击预览图片" />
                    <span class="img-name">{{ file.name }}</span>
                  </div>
                  <!-- General File -->
                  <button v-else type="button" class="document-file-row" @click="downloadAttachment(file)">
                    <span class="document-file-icon" :class="getDocumentIconClass(file.name)">
                      {{ getDocumentIconLabel(file.name) }}
                    </span>
                    <span class="document-file-meta">
                      <strong :title="file.name">{{ file.name }}</strong>
                      <small>{{ file.size || "未知大小" }}</small>
                    </span>
                  </button>
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
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useTeamStore } from "../stores/team";
import { useAuthStore } from "../stores/auth";
import { useScrollReveal } from "../composables/useScrollReveal";
import { paperpilotApi } from "../services/paperpilotApi";
import { useDialogStore } from "../stores/dialog";

useScrollReveal(".team-page");

const teamStore = useTeamStore();
const authStore = useAuthStore();
const dialogStore = useDialogStore();

const tabs = [
  { key: "roster", label: "成员" },
  { key: "tasks", label: "任务" },
  { key: "announcements", label: "公告" },
];

const activeTab = ref("roster");
const showInviteModal = ref(false);
const showProfileModal = ref(false);
const selectedMember = ref(null);
const showImagePreview = ref(false);
const previewImageUrl = ref("");
const previewImageName = ref("");
const previewScale = ref(1);

function previewImage(file) {
  previewImageUrl.value = file.data;
  previewImageName.value = file.name;
  previewScale.value = 1;
  showImagePreview.value = true;
}

function changeImageZoom(delta) {
  previewScale.value = Math.min(4, Math.max(0.5, Number((previewScale.value + delta).toFixed(2))));
}

function resetImageZoom() {
  previewScale.value = 1;
}

function handleResourceUpload(event) {
  const files = event.target.files;
  if (!files || !files.length) return;
  const file = files[0];
  const uploaderName = authStore.profile.name || "团队成员";
  
  const reader = new FileReader();
  reader.onload = (e) => {
    teamStore.addResource(
      file.name,
      formatBytes(file.size),
      file.type,
      e.target.result, // Base64
      uploaderName
    );
    resPage.value = 1;
    showToast(`成功上传资源: ${file.name}`);
  };
  reader.readAsDataURL(file);
}

function downloadResource(res) {
  if (!res.data) return;
  const link = document.createElement("a");
  link.href = res.data;
  link.download = res.name;
  link.click();
  showToast(`开始下载资源: ${res.name}`);
}

async function deleteResource(id) {
  if (await dialogStore.confirm("确定要删除这个共享资源吗？", {
    title: "删除共享资源",
    confirmText: "删除",
    danger: true,
  })) {
    teamStore.deleteResource(id);
    showToast("资源已删除");
  }
}

const newMemberName = ref("");
const newMemberEmail = ref("");
const newMemberRole = ref("学生");
const newMemberLimit = ref(1000000);

const editingMemberId = ref(null);
const tempLimit = ref(0);

const showAddTaskForm = ref(false);
const editingTaskId = ref(null);
const newTaskTitle = ref("");
const newTaskDesc = ref("");
const newTaskDeadline = ref("");
const taskAttachments = ref([]);

const showAddAnnForm = ref(false);
const editingAnnouncementId = ref(null);
const newAnnTitle = ref("");
const newAnnContent = ref("");
const newAnnLink = ref("");
const newAnnImage = ref("");
const newAnnImageName = ref("");
// Attachment for announcement (single file)
const newAnnAttachmentData = ref("");
const newAnnAttachmentName = ref("");
const newAnnAttachmentSize = ref("");
const newAnnAttachmentType = ref("");

function handleAnnImageUpload(event) {
  const files = event.target.files;
  if (!files || !files.length) return;
  const file = files[0];
  const reader = new FileReader();
  reader.onload = (e) => {
    newAnnImage.value = e.target.result;
    newAnnImageName.value = file.name;
  };
  reader.readAsDataURL(file);
}

function handleAnnAttachmentUpload(event) {
  const files = event.target.files;
  if (!files || !files.length) return;
  const file = files[0];
  const reader = new FileReader();
  reader.onload = (e) => {
    newAnnAttachmentData.value = e.target.result;
    newAnnAttachmentName.value = file.name;
    newAnnAttachmentSize.value = `${(file.size/1024).toFixed(1)}KB`;
    newAnnAttachmentType.value = file.type;
  };
  reader.readAsDataURL(file);
}

const toastMessage = ref("");
let toastTimer = null;
let clockTimer = null;
const currentResearchDate = ref("");
const currentResearchClock = ref("");
const showCheckinModal = ref(false);
const checkinStreak = ref(0);
const checkinDialogTitle = ref("今天的科研节奏已经记录");
const checkinMotivation = ref("稳定推进比短暂冲刺更可靠。今天多走一步，下一次汇报就多一分底气。");
const checkinMessages = [
  "稳定推进比短暂冲刺更可靠。今天多走一步，下一次汇报就多一分底气。",
  "科研不是只靠灵感，更多时候靠每天把问题往前推一点。",
  "今天的记录已经归档。把复杂问题拆小，你会看到进展越来越清楚。",
  "保持节奏很好。连续打卡不是形式，是给自己留下一条可追踪的成长线。",
];
const TEAM_BASE_SEATS = 8;

const currentMemberId = computed(() => {
  const current = teamStore.members.find((member) => member.isCurrentUser);
  return current ? current.id : "m-tutor";
});

const currentUserMember = computed(() => {
  return teamStore.members.find((member) => member.isCurrentUser) || teamStore.members[0];
});

const hasWriteAccess = computed(() => currentUserMember.value?.role === "导师");

const onlineCount = computed(() => teamStore.members.filter((member) => member.status === "online").length);

const checkedInCount = computed(() => teamStore.checkins.filter((item) => item.status === "已打卡").length);

const currentCheckinItem = computed(() => {
  return teamStore.checkins.find((item) => item.memberId === currentMemberId.value);
});

const hasTeamFleetPlan = computed(() => Number(teamStore.totalSeats || TEAM_BASE_SEATS) > TEAM_BASE_SEATS);

const completedTaskCount = computed(() => {
  return teamStore.tasks.filter((task) => task.status === "已完成").length;
});

const completionRate = computed(() => {
  if (!teamStore.tasks.length) return 0;
  return Math.round((completedTaskCount.value / teamStore.tasks.length) * 100);
});

const maxActiveTime = computed(() => Math.max(1, ...teamStore.members.map((member) => member.activeTime || 0)));

const sortedLeaderboard = computed(() => {
  return [...teamStore.members].sort((a, b) => (b.activeTime || 0) - (a.activeTime || 0));
});

const taskPage = ref(1);
const taskPageSize = 3;

const annPage = ref(1);
const annPageSize = 3;

const leaderPage = ref(1);
const leaderPageSize = 5;

const resPage = ref(1);
const resPageSize = 6;

const paginatedTasks = computed(() => {
  const start = (taskPage.value - 1) * taskPageSize;
  return teamStore.tasks.slice(start, start + taskPageSize);
});
const taskTotalPages = computed(() => Math.ceil(teamStore.tasks.length / taskPageSize) || 1);

const paginatedAnnouncements = computed(() => {
  const start = (annPage.value - 1) * annPageSize;
  return teamStore.announcements.slice(start, start + annPageSize);
});
const annTotalPages = computed(() => Math.ceil(teamStore.announcements.length / annPageSize) || 1);

const paginatedLeaderboard = computed(() => {
  const start = (leaderPage.value - 1) * leaderPageSize;
  return sortedLeaderboard.value.slice(start, start + leaderPageSize);
});
const leaderTotalPages = computed(() => Math.ceil(sortedLeaderboard.value.length / leaderPageSize) || 1);

const paginatedResources = computed(() => {
  const start = (resPage.value - 1) * resPageSize;
  return (teamStore.resources || []).slice(start, start + resPageSize);
});
const resTotalPages = computed(() => Math.ceil((teamStore.resources || []).length / resPageSize) || 1);

// Detail Modal
const showDetailModal = ref(false);
const detailModalType = ref(""); // "task" or "announcement"
const detailModalContent = ref(null);

function openDetailModal(type, item) {
  detailModalType.value = type;
  detailModalContent.value = item;
  showDetailModal.value = true;
}

function showToast(message) {
  toastMessage.value = message;
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toastMessage.value = "";
  }, 2600);
}

function showMemberProfile(member) {
  selectedMember.value = member;
  showProfileModal.value = true;
}

function getMemberInitial(name) {
  return String(name || "U").replace("导师", "").trim().slice(0, 1).toUpperCase();
}

function getCheckinStatus(memberId) {
  const item = teamStore.checkins.find((checkin) => checkin.memberId === memberId);
  return item?.status || "未打卡";
}

function canViewQuota(member) {
  return currentUserMember.value?.role === "导师" || member.id === currentMemberId.value;
}

function getAvatarColor(role) {
  if (role === "导师") return "#0a84ff";
  if (role === "管理员") return "#8e8e93";
  if (role === "特权用户") return "#bf5af2";
  return "#34c759";
}

function getRoleClass(role) {
  if (role === "导师") return "role-tutor";
  if (role === "管理员") return "role-admin";
  if (role === "特权用户") return "role-vip";
  return "role-student";
}

function getQuotaColorClass(ratio) {
  if (ratio > 0.85) return "fill-danger";
  if (ratio > 0.6) return "fill-warning";
  return "fill-safe";
}

function getMemberLevelInfo(activeTime) {
  const level = Math.floor((activeTime || 0) / 300) + 1;
  let title = "科研萌新";
  if (level >= 15) title = "科研主宰";
  else if (level >= 10) title = "科研宗师";
  else if (level >= 6) title = "学术专家";
  else if (level >= 3) title = "科研骨干";
  return { level, title };
}

function formatTokens(value) {
  const n = Number(value || 0);
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`;
  if (n >= 1000) return `${Math.round(n / 1000)}K`;
  return String(n);
}

function formatActiveTime(seconds) {
  if (!seconds) return "0分钟";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}小时${m}分钟`;
  if (m > 0) return `${m}分钟${s}秒`;
  return `${s}秒`;
}

function startEditQuota(member) {
  editingMemberId.value = member.id;
  tempLimit.value = member.tokenLimit;
}

function saveQuota(memberId) {
  if (tempLimit.value < 0) {
    showToast("额度不能小于 0");
    return;
  }
  teamStore.updateQuota(memberId, tempLimit.value);
  editingMemberId.value = null;
  showToast("额度已更新");
}

function toggleRole(member) {
  const roles = ["学生", "特权用户", "管理员"];
  const currentIndex = roles.indexOf(member.role);
  const nextRole = roles[(currentIndex + 1) % roles.length];
  teamStore.updateRole(member.id, nextRole);
  showToast(`${member.name} 已调整为 ${nextRole}`);
}

async function deleteMember(member) {
  if (!await dialogStore.confirm(`确定将 ${member.name} 移出团队吗？`, {
    title: "移出团队",
    confirmText: "移出",
    danger: true,
  })) return;
  teamStore.removeMember(member.id);
  showToast(`${member.name} 已移出团队`);
}

function submitInvite() {
  if (!newMemberName.value.trim() || !newMemberEmail.value.trim()) {
    showToast("请填写姓名和邮箱");
    return;
  }
  if (!newMemberEmail.value.includes("@")) {
    showToast("请输入有效邮箱");
    return;
  }
  try {
    teamStore.addMember({
      name: newMemberName.value.trim(),
      email: newMemberEmail.value.trim(),
      role: newMemberRole.value,
      tokenLimit: newMemberLimit.value,
    });
    newMemberName.value = "";
    newMemberEmail.value = "";
    newMemberRole.value = "学生";
    newMemberLimit.value = 1000000;
    showInviteModal.value = false;
    showToast("邀请已创建");
  } catch (error) {
    showToast(error.message || "邀请失败");
  }
}

async function submitTask() {
  if (!newTaskTitle.value.trim() || !newTaskDeadline.value) {
    showToast("请填写任务标题和截止日期");
    return;
  }
  try {
    const payload = [
      newTaskTitle.value.trim(),
      newTaskDesc.value.trim(),
      newTaskDeadline.value,
      JSON.parse(JSON.stringify(taskAttachments.value)),
    ];
    if (editingTaskId.value) {
      await teamStore.updateTask(editingTaskId.value, ...payload);
    } else {
      await teamStore.addTask(...payload);
    }
  } catch (error) {
    showToast(error.response?.data?.message || error.message || "任务保存失败");
    return;
  }
  const wasEditing = Boolean(editingTaskId.value);
  resetTaskForm();
  taskPage.value = 1;
  showToast(wasEditing ? "任务已修改" : "任务已发布");
}

function formatBytes(bytes, decimals = 2) {
  if (!+bytes) return '0 Bytes';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

function handleTaskFilesUpload(event) {
  const files = event.target.files;
  if (!files || !files.length) return;
  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    const reader = new FileReader();
    reader.onload = (e) => {
      taskAttachments.value.push({
        name: file.name,
        size: formatBytes(file.size),
        type: file.type,
        data: e.target.result,
      });
    };
    reader.readAsDataURL(file);
  }
}

function removeTaskFile(index) {
  taskAttachments.value.splice(index, 1);
}

function toggleTaskForm() {
  if (showAddTaskForm.value) resetTaskForm();
  else showAddTaskForm.value = true;
}

function startEditTask(task) {
  editingTaskId.value = task.id;
  newTaskTitle.value = task.title || "";
  newTaskDesc.value = task.description || "";
  newTaskDeadline.value = task.deadline || "";
  taskAttachments.value = JSON.parse(JSON.stringify(task.attachments || []));
  showAddTaskForm.value = true;
}

function resetTaskForm() {
  editingTaskId.value = null;
  newTaskTitle.value = "";
  newTaskDesc.value = "";
  newTaskDeadline.value = "";
  taskAttachments.value = [];
  showAddTaskForm.value = false;
}

async function retractTask(task) {
  if (!await dialogStore.confirm(`确定撤回任务“${task.title}”吗？撤回后学生端将不再显示。`, {
    title: "撤回任务",
    confirmText: "撤回",
    danger: true,
  })) return;
  try {
    await teamStore.deleteTask(task.id);
    if (editingTaskId.value === task.id) resetTaskForm();
    showToast("任务已撤回");
  } catch (error) {
    showToast(error.response?.data?.message || "任务撤回失败");
  }
}

function downloadAttachment(file) {
  if (!file.data) return;
  const link = document.createElement("a");
  link.href = file.data;
  link.download = file.name;
  link.click();
  showToast(`开始下载: ${file.name}`);
}

async function downloadAnnouncementAttachment(announcement) {
  if (!announcement?.id || !announcement.attachmentName) return;
  try {
    const response = await paperpilotApi.downloadTeamAnnouncementAttachment(announcement.id);
    const objectUrl = URL.createObjectURL(response.data);
    const link = document.createElement("a");
    link.href = objectUrl;
    link.download = announcement.attachmentName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(objectUrl);
    showToast(`开始下载: ${announcement.attachmentName}`);
  } catch (error) {
    showToast(error.response?.data?.message || "附件下载失败");
  }
}

function getDocumentIconLabel(filename) {
  const extension = getFileExtension(filename);
  if (["DOC", "DOCX"].includes(extension)) return "W";
  if (["XLS", "XLSX", "CSV"].includes(extension)) return "X";
  if (["PPT", "PPTX"].includes(extension)) return "P";
  if (extension === "PDF") return "PDF";
  return extension.slice(0, 4);
}

function getDocumentIconClass(filename) {
  const extension = getFileExtension(filename);
  if (["DOC", "DOCX"].includes(extension)) return "is-word";
  if (["XLS", "XLSX", "CSV"].includes(extension)) return "is-excel";
  if (["PPT", "PPTX"].includes(extension)) return "is-powerpoint";
  if (extension === "PDF") return "is-pdf";
  return "is-generic";
}

function getAnnouncementAttachmentSize(announcement) {
  if (announcement?.attachmentSize) return announcement.attachmentSize;
  const data = announcement?.attachmentData || "";
  const encoded = data.includes(",") ? data.slice(data.indexOf(",") + 1) : data;
  if (!encoded) return "点击下载";
  const padding = (encoded.match(/=*$/)?.[0].length || 0);
  return formatBytes(Math.max(0, Math.floor((encoded.length * 3) / 4) - padding));
}

function getAttachmentIcon(type) {
  if (!type) return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="att-svg"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline></svg>`;
  if (type.startsWith("image/")) {
    return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="att-svg" style="color:#10b981"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>`;
  }
  if (type.includes("pdf")) {
    return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="att-svg" style="color:#ef4444"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>`;
  }
  return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="att-svg" style="color:#0066ff"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline></svg>`;
}

function isImageAttachment(file) {
  if (!file) return false;
  return (file.type && file.type.startsWith("image/")) || 
         /\.(jpg|jpeg|png|gif|webp)$/i.test(file.name);
}

function getFileExtension(filename) {
  if (!filename) return "FILE";
  const parts = filename.split(".");
  return parts.length > 1 ? parts[parts.length - 1].toUpperCase() : "FILE";
}

function formatDeadlineString(deadline) {
  if (!deadline) return "";
  return deadline.replace("T", " ");
}

function getTaskDeadlineCountdown(deadline) {
  if (!deadline) return null;
  const target = new Date(deadline);
  const now = new Date();
  const diffMs = target - now;

  if (diffMs <= 0) {
    return { text: "已截止", colorClass: "overdue" };
  }

  const diffMins = Math.floor(diffMs / 60000);
  const diffHrs = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHrs / 24);

  const mins = diffMins % 60;
  const hrs = diffHrs % 24;

  if (diffDays >= 1) {
    return { text: `剩余 ${diffDays}天 ${hrs}小时`, colorClass: "green" };
  } else if (diffHrs >= 12) {
    return { text: `剩余 ${hrs}小时 ${mins}分钟`, colorClass: "yellow" };
  } else {
    return { text: `仅剩 ${hrs}小时 ${mins}分钟`, colorClass: "red" };
  }
}


async function submitAnnouncement() {
  if (!newAnnTitle.value.trim() || !newAnnContent.value.trim()) {
    showToast("请填写公告标题和内容");
    return;
  }
  try {
    const payload = {
      title: newAnnTitle.value.trim(),
      content: newAnnContent.value.trim(),
      image: newAnnImage.value,
      link: newAnnLink.value.trim(),
      attachmentName: newAnnAttachmentName.value,
      attachmentType: newAnnAttachmentType.value,
      attachmentData: newAnnAttachmentData.value,
      attachmentSize: newAnnAttachmentSize.value,
    };
    if (editingAnnouncementId.value) {
      await teamStore.updateAnnouncement(editingAnnouncementId.value, payload);
    } else {
      await teamStore.addAnnouncement(
        payload.title,
        payload.content,
        payload.image,
        payload.link,
        payload.attachmentName,
        payload.attachmentType,
        payload.attachmentData,
        payload.attachmentSize,
      );
    }
  } catch (error) {
    showToast(error.response?.data?.message || error.message || "公告保存失败");
    return;
  }
  const wasEditing = Boolean(editingAnnouncementId.value);
  resetAnnouncementForm();
  annPage.value = 1;
  showToast(wasEditing ? "公告已修改" : "公告已发布");
}

function toggleAnnouncementForm() {
  if (showAddAnnForm.value) resetAnnouncementForm();
  else showAddAnnForm.value = true;
}

function startEditAnnouncement(announcement) {
  editingAnnouncementId.value = announcement.id;
  newAnnTitle.value = announcement.title || "";
  newAnnContent.value = announcement.content || "";
  newAnnImage.value = announcement.image || "";
  newAnnImageName.value = announcement.image ? "原公告配图" : "";
  newAnnLink.value = announcement.link || "";
  newAnnAttachmentData.value = announcement.attachmentData || "";
  newAnnAttachmentName.value = announcement.attachmentName || "";
  newAnnAttachmentSize.value = getAnnouncementAttachmentSize(announcement);
  newAnnAttachmentType.value = announcement.attachmentType || "";
  showAddAnnForm.value = true;
}

function resetAnnouncementForm() {
  editingAnnouncementId.value = null;
  newAnnTitle.value = "";
  newAnnContent.value = "";
  newAnnImage.value = "";
  newAnnImageName.value = "";
  newAnnLink.value = "";
  newAnnAttachmentData.value = "";
  newAnnAttachmentName.value = "";
  newAnnAttachmentSize.value = "";
  newAnnAttachmentType.value = "";
  showAddAnnForm.value = false;
}

async function retractAnnouncement(announcement) {
  if (!await dialogStore.confirm(`确定撤回公告“${announcement.title}”吗？撤回后学生端将不再显示。`, {
    title: "撤回公告",
    confirmText: "撤回",
    danger: true,
  })) return;
  try {
    await teamStore.deleteAnnouncement(announcement.id);
    if (editingAnnouncementId.value === announcement.id) resetAnnouncementForm();
    showToast("公告已撤回");
  } catch (error) {
    showToast(error.response?.data?.message || "公告撤回失败");
  }
}

function updateResearchClock() {
  const now = new Date();
  currentResearchDate.value = new Intl.DateTimeFormat("zh-CN", {
    timeZone: "Asia/Shanghai",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
  }).format(now);
  currentResearchClock.value = new Intl.DateTimeFormat("zh-CN", {
    timeZone: "Asia/Shanghai",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(now);
}

function openCheckinDialog(saved = currentCheckinItem.value) {
  const streak = Number(saved?.streak || currentCheckinItem.value?.streak || 1);
  checkinStreak.value = Math.max(1, streak);
  checkinDialogTitle.value = saved?.status === "已打卡" ? "今日打卡已完成" : "今天的科研节奏已经记录";
  checkinMotivation.value = checkinMessages[(checkinStreak.value - 1) % checkinMessages.length];
  showCheckinModal.value = true;
}

async function doCheckin() {
  if (currentCheckinItem.value?.status === "已打卡") {
    openCheckinDialog(currentCheckinItem.value);
    return;
  }
  try {
    const saved = await teamStore.performCheckin(currentMemberId.value);
    openCheckinDialog(saved);
  } catch (error) {
    showToast(error.response?.data?.message || "签到失败，请稍后重试");
  }
}

onMounted(() => {
  updateResearchClock();
  clockTimer = setInterval(updateResearchClock, 1000);
  teamStore.loadFromServer().catch(error => {
    console.error("Failed to refresh team page:", error);
  });
});

onUnmounted(() => {
  if (toastTimer) clearTimeout(toastTimer);
  if (clockTimer) clearInterval(clockTimer);
});
</script>

<style scoped>
.team-page {
  position: relative;
  min-height: 100vh;
  padding: 40px 24px 100px;
  background: 
    radial-gradient(circle at 10% 10%, rgba(0, 102, 255, 0.05), transparent 40%),
    radial-gradient(circle at 90% 90%, rgba(52, 199, 89, 0.03), transparent 30%),
    linear-gradient(180deg, #faf9f7 0%, #f3f2ef 100%);
  color: var(--spatial-graphite, #1c1c1e);
  font-family: var(--spatial-font-body, "Inter", -apple-system, sans-serif);
}

.team-shell {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 32px;
  position: relative;
  z-index: 10;
}

/* Horizontal Seats Header */
.team-seats-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 24px;
  padding: 28px 32px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(10, 10, 12, 0.03);
}

.seats-left-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
  min-width: 300px;
}

.seats-label-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.seats-label-row h2 {
  font-family: var(--spatial-font-body);
  font-size: 20px;
  font-weight: 700;
  margin: 0;
  color: #1d1d1f;
}

.seats-count-badge {
  font-size: 12px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 99px;
  background: rgba(0, 102, 255, 0.08);
  color: var(--spatial-accent, #0066ff);
}

.seats-avatar-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}

.seat-circle-avatar {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
}

.avatar-inner-wrapper {
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  padding: 3px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 102, 255, 0.15);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.seat-circle-avatar:hover .avatar-inner-wrapper {
  transform: translateY(-4px) scale(1.05);
  border-color: var(--spatial-accent, #0066ff);
  box-shadow: 0 6px 18px rgba(0, 102, 255, 0.15);
}

.seat-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.seat-fallback {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
}

.status-indicator-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #ffffff;
  background: #aeaeb2;
}

.status-indicator-dot.online {
  background: #34c759;
  box-shadow: 0 0 8px rgba(52, 199, 89, 0.6);
}

.status-indicator-dot.offline {
  background: #aeaeb2;
}

.seat-member-name {
  font-size: 11px;
  font-weight: 600;
  color: #6e6e73;
}

/* Empty Seat styling */
.empty-seat-btn {
  cursor: pointer;
}

.empty-seat-btn .avatar-inner-wrapper {
  background: transparent;
  border: 1.5px dashed rgba(0, 102, 255, 0.25);
  box-shadow: none;
}

.empty-seat-btn:hover .avatar-inner-wrapper {
  border-color: var(--spatial-accent, #0066ff);
  background: rgba(0, 102, 255, 0.03);
}

.plus-symbol {
  font-size: 20px;
  font-weight: 300;
  color: rgba(0, 102, 255, 0.5);
}

.locked-seat-btn .avatar-inner-wrapper {
  border-color: rgba(224, 109, 27, 0.34);
  background: rgba(255, 245, 237, 0.72);
}

.locked-seat-btn .plus-symbol {
  color: #d35f19;
}

/* Right Header Container */
.seats-right-container {
  display: flex;
  align-items: center;
  gap: 20px;
}

.team-plan-flag {
  display: grid;
  gap: 2px;
  min-width: 132px;
  padding: 10px 12px;
  border: 1px solid rgba(40, 82, 145, 0.12);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.92), rgba(255, 255, 255, 0.72));
}

.team-plan-flag span {
  color: #64748b;
  font-size: 11px;
  font-weight: 750;
}

.team-plan-flag strong {
  color: #1e3a8a;
  font-size: 13px;
}

.team-plan-flag.active {
  border-color: rgba(224, 109, 27, 0.34);
  background: linear-gradient(180deg, #fff5ed, #ffffff);
  box-shadow: 0 8px 18px rgba(224, 109, 27, 0.08);
}

.team-plan-flag.active strong {
  color: #c85112;
}

.team-identity-plate {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.plate-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: #8e8e93;
  text-transform: uppercase;
}

.plate-code {
  font-size: 16px;
  font-weight: 700;
  color: #1c1c1e;
  letter-spacing: 0.02em;
}

.invite-main-btn {
  background: var(--spatial-accent, #0066ff);
  color: #ffffff;
  border: none;
  padding: 12px 24px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 102, 255, 0.2);
  transition: all 0.2s ease;
}

.invite-main-btn:hover {
  background: #0055d4;
  transform: translateY(-1px);
}

/* Split layouts */
.dashboard-split-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(340px, 0.9fr);
  gap: 32px;
  align-items: start;
}

.dashboard-col {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.col-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.col-header-row h3 {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  color: #1d1d1f;
}

.col-meta-pill {
  font-size: 11px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 6px;
  background: rgba(142, 142, 147, 0.1);
  color: #6e6e73;
}

/* Members Management Cards */
.members-cards-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.member-management-card {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-identity-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-large-shell {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  padding: 2px;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.avatar-large-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-large-fallback {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  font-weight: 700;
}

.identity-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.name-role-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name-role-line strong {
  font-size: 16px;
  color: #1c1c1e;
}

.role-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}

.role-badge.role-tutor {
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
}

.role-badge.role-admin {
  background: rgba(142, 142, 147, 0.1);
  color: #6e6e73;
}

.role-badge.role-vip {
  background: rgba(191, 90, 242, 0.1);
  color: #bf5af2;
}

.role-badge.role-student {
  background: rgba(52, 199, 89, 0.08);
  color: #248a3d;
}

.email-subtext {
  font-size: 12px;
  color: #8e8e93;
}

.card-details-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  padding: 12px 0;
}

.card-detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-detail-item span {
  font-size: 10px;
  color: #8e8e93;
  text-transform: uppercase;
}

.card-detail-item strong {
  font-size: 12px;
  color: #3a3a3c;
}

.text-checked {
  color: #34c759 !important;
}

/* Quota management styles */
.card-actions-wrapper {
  background: rgba(0, 0, 0, 0.015);
  border-radius: 12px;
  padding: 12px;
  border: 1px solid rgba(0, 0, 0, 0.02);
}

.quota-management-bar {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quota-label-line {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
}

.quota-label-line span {
  color: #8e8e93;
}

.quota-label-line strong {
  color: #1c1c1e;
  font-weight: 600;
}

.progress-bar-track {
  width: 100%;
  height: 6px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 9px;
  overflow: hidden;
}

.progress-fill-bar {
  display: block;
  height: 100%;
  border-radius: 9px;
  transition: width 0.3s ease;
}

.progress-fill-bar.fill-safe { background: #34c759; }
.progress-fill-bar.fill-warning { background: #ff9f0a; }
.progress-fill-bar.fill-danger { background: #ff3b30; }

.quota-edit-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.quota-edit-inputs input {
  flex: 1;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 12px;
}

.action-btn-mini {
  border: none;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}

.action-btn-mini.confirm {
  background: var(--spatial-accent, #0066ff);
  color: white;
}

.action-btn-mini.cancel {
  background: rgba(0, 0, 0, 0.05);
  color: #3a3a3c;
}

.quota-trigger-actions {
  display: flex;
  gap: 12px;
  margin-top: 6px;
}

.action-btn-link {
  background: none;
  border: none;
  padding: 0;
  font-size: 11px;
  font-weight: 600;
  color: var(--spatial-accent, #0066ff);
  cursor: pointer;
}

.action-btn-link.danger {
  color: #ff3b30;
}

/* Tutor Metric Cards */
.summary-metric-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.tutor-metric-card {
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 16px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tutor-metric-card span {
  font-size: 11px;
  color: #8e8e93;
}

.tutor-metric-card strong {
  font-size: 20px;
  font-weight: 700;
  color: #1c1c1e;
}

/* Control Panel Section Admin */
.admin-action-section {
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.01);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.admin-action-section h4 {
  font-size: 15px;
  font-weight: 700;
  margin: 0;
  color: #1c1c1e;
}

.btn-toggle-form {
  background: none;
  border: none;
  font-size: 12px;
  font-weight: 600;
  color: var(--spatial-accent, #0066ff);
  cursor: pointer;
}

.tutor-inline-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: rgba(0, 0, 0, 0.015);
  border: 1px solid rgba(0, 0, 0, 0.02);
  padding: 14px;
  border-radius: 14px;
}

.form-row-grid {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.tutor-inline-form input,
.tutor-inline-form textarea {
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 12px;
}

.tutor-inline-form textarea {
  resize: vertical;
  min-height: 60px;
}

.submit-form-btn {
  background: var(--spatial-graphite, #1c1c1e);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  align-self: flex-end;
}

.form-submit-row {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.cancel-edit-btn {
  background: rgba(0, 0, 0, 0.05);
  color: #5f6368;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.published-actions {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-shrink: 0;
}

.published-action-btn {
  background: rgba(0, 102, 255, 0.06);
  color: #0066ff;
  border: none;
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}

.published-action-btn.danger {
  color: #ff3b30;
  background: rgba(255, 59, 48, 0.07);
}

.admin-items-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.admin-item-card {
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 14px;
  padding: 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
}

.admin-item-card.completed {
  opacity: 0.65;
}

.item-main-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.item-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.item-title-row h5 {
  font-size: 13px;
  font-weight: 600;
  margin: 0;
  color: #1c1c1e;
}

.item-main-content p {
  font-size: 12px;
  color: #6e6e73;
  margin: 4px 0 0;
}

.item-main-content small {
  font-size: 11px;
  color: #8e8e93;
}

.status-tag {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 159, 10, 0.1);
  color: #ff9f0a;
}

.status-tag.done {
  background: rgba(52, 199, 89, 0.1);
  color: #34c759;
}

.status-tag.published {
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
}

.announcement-task-link {
  text-decoration: none;
}

.item-actions-row {
  display: flex;
  gap: 8px;
}

.item-action-btn {
  background: rgba(0, 0, 0, 0.03);
  border: none;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  color: #3a3a3c;
}

.item-action-btn.danger {
  color: #ff3b30;
  background: rgba(255, 59, 48, 0.06);
}

/* Leaderboard rows in mentor view */
.leaderboard-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.leader-row-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.leader-rank-no {
  font-size: 14px;
  font-weight: 700;
  color: #aeaeb2;
  width: 20px;
  text-align: center;
}

.leader-info-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.leader-meta-line {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.leader-meta-line strong {
  color: #1c1c1e;
}

.leader-meta-line span {
  color: #8e8e93;
}

.leader-progress-track {
  width: 100%;
  height: 4px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 2px;
  overflow: hidden;
}

.leader-progress-bar {
  height: 100%;
  background: var(--spatial-accent, #0066ff);
  border-radius: 2px;
}

/* Student Workbench & Cards */
.student-glass-card {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.02);
}

.student-glass-card h3 {
  font-size: 16px;
  font-weight: 700;
  margin: 0 0 16px;
  color: #1d1d1f;
}

/* Sign-in widget */
.sign-in-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  padding-bottom: 12px;
  margin-bottom: 16px;
}

.sign-in-header h3 {
  margin: 0;
}

.checkin-clock {
  display: grid;
  gap: 2px;
  text-align: right;
}

.checkin-clock span {
  color: #8e8e93;
  font-size: 11px;
}

.checkin-clock strong {
  color: #1c1c1e;
  font-size: 15px;
  font-variant-numeric: tabular-nums;
}

.pulse-glow-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ff3b30;
}

.pulse-glow-dot.active {
  background: #34c759;
  box-shadow: 0 0 8px #34c759;
  animation: pulse 1.8s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.25); opacity: 0.7; }
  100% { transform: scale(1); opacity: 1; }
}

.sign-in-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.sign-in-text p {
  margin: 0;
  font-size: 13px;
  color: #48484a;
  line-height: 1.5;
}

.student-primary-action-btn {
  background: var(--spatial-accent, #0066ff);
  color: white;
  border: none;
  padding: 12px 20px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.student-primary-action-btn:hover {
  background: #0055d4;
  transform: translateY(-1px);
}

.student-primary-action-btn.already-checked {
  background: rgba(52, 199, 89, 0.08);
  color: #248a3d;
  cursor: pointer;
  transform: none;
}

/* Stats Pill layout */
.stats-pills-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 20px;
}

.stat-pill-box {
  background: rgba(0, 0, 0, 0.015);
  border: 1px solid rgba(0, 0, 0, 0.02);
  border-radius: 16px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-pill-box span {
  font-size: 11px;
  color: #8e8e93;
}

.stat-pill-box strong {
  font-size: 22px;
  font-weight: 700;
  color: #1c1c1e;
}

.stat-pill-box small {
  font-size: 10px;
  color: #aeabaf;
}

.student-quota-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.student-quota-header {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.student-quota-header span { color: #8e8e93; }
.student-quota-header strong { color: #1c1c1e; }

/* Student Tasks Checklist */
.checklist-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  padding-bottom: 12px;
  margin-bottom: 16px;
}

.checklist-header h3 {
  margin: 0;
}

.task-count-indicator {
  font-size: 11px;
  font-weight: 700;
  color: #8e8e93;
}

.student-task-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.student-task-item {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
}

.checkbox-wrapper {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  margin-top: 2px;
}

.custom-checkbox {
  display: block;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1.5px solid rgba(0, 102, 255, 0.35);
  background: #ffffff;
  position: relative;
  transition: all 0.2s ease;
}

.student-task-item:hover .custom-checkbox {
  border-color: var(--spatial-accent, #0066ff);
  background: rgba(0, 102, 255, 0.03);
}

.student-task-item.completed .custom-checkbox {
  border-color: #34c759;
  background: #34c759;
}

.student-task-item.completed .custom-checkbox::after {
  content: "";
  position: absolute;
  top: 4px;
  left: 7px;
  width: 3px;
  height: 6px;
  border: solid white;
  border-width: 0 1.5px 1.5px 0;
  transform: rotate(45deg);
}

.task-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.task-info h4 {
  font-size: 13px;
  font-weight: 600;
  margin: 0;
  color: #1c1c1e;
  transition: all 0.2s ease;
}

.student-task-item.completed h4 {
  text-decoration: line-through;
  color: #aeabaf;
}

.task-info p {
  font-size: 12px;
  color: #6e6e73;
  margin: 2px 0 0;
  line-height: 1.4;
}

.student-task-item.completed p {
  color: #d1d1d6;
}

.task-info small {
  font-size: 10px;
  color: #aeaeb2;
}

.empty-state-text {
  padding: 16px;
  text-align: center;
  color: #aeaeb2;
  font-size: 12px;
}

/* Announcements briefing student */
.announcements-briefing-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.briefing-ann-card {
  background: rgba(0, 0, 0, 0.012);
  border: 1px solid rgba(0, 0, 0, 0.02);
  border-radius: 14px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ann-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.ann-header h4 {
  font-size: 13px;
  font-weight: 600;
  margin: 0;
  color: #1c1c1e;
}

.ann-header small {
  font-size: 10px;
  color: #aeabaf;
  white-space: nowrap;
}

.briefing-ann-card p {
  font-size: 12px;
  color: #555558;
  margin: 4px 0 0;
  line-height: 1.4;
}

/* Student active leaderboard */
.student-leaderboard-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.student-leader-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px;
  border-radius: 10px;
}

.student-leader-row.is-current-user {
  background: rgba(0, 102, 255, 0.04);
  border: 1.5px solid rgba(0, 102, 255, 0.15);
}

.rank-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
}

.rank-number {
  font-size: 12px;
  font-weight: 700;
  color: #aeaeb2;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-number.rank-1 { background: rgba(255, 215, 0, 0.15); color: #b8860b; }
.rank-number.rank-2 { background: rgba(192, 192, 192, 0.2); color: #7f7f7f; }
.rank-number.rank-3 { background: rgba(205, 127, 50, 0.15); color: #8b4513; }

.leader-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.name-time-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.name-time-row strong {
  color: #1c1c1e;
}

.name-time-row span {
  color: #8e8e93;
}

/* Modals Overlay Base */
.overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.35);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.modal-card {
  width: min(560px, 100%);
  border-radius: 28px;
  padding: 28px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 24px 64px rgba(10, 10, 12, 0.15);
}

.modal-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.modal-head h3 {
  font-size: 20px;
  font-weight: 700;
  margin: 4px 0 0;
  color: #1c1c1e;
}

.panel-eyebrow {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #8e8e93;
  text-transform: uppercase;
}

.close-btn {
  background: rgba(0, 0, 0, 0.05);
  border: none;
  font-size: 12px;
  font-weight: 600;
  color: #3a3a3c;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
}

/* Forms general inside modals */
.modal-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.modal-form label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modal-form label span {
  font-size: 11px;
  font-weight: 700;
  color: #8e8e93;
  text-transform: uppercase;
}

.modal-form input,
.modal-form select {
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 13px;
  color: #1c1c1e;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.apple-btn {
  border: none;
  padding: 12px 20px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.apple-btn-primary {
  background: var(--spatial-accent, #0066ff);
  color: white;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

/* Profile showcase inside profile details modal */
.profile-modal {
  width: min(480px, 100%);
}

.profile-showcase {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.avatar-shell {
  position: relative;
  border-radius: 50%;
  padding: 3px;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.avatar-shell.hero-avatar {
  width: 68px;
  height: 68px;
}

.avatar-image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-fallback {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 20px;
}

.status-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #ffffff;
  background: #aeaeb2;
}

.status-dot.online {
  background: #34c759;
}

.profile-showcase-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.profile-showcase-copy h3 {
  font-size: 18px;
  font-weight: 700;
  margin: 4px 0 0;
  color: #1c1c1e;
}

.profile-showcase-copy p {
  font-size: 12px;
  color: #8e8e93;
  margin: 0;
}

.role-pill {
  font-size: 9px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  align-self: flex-start;
  text-transform: uppercase;
}

.role-pill.role-tutor { background: rgba(0, 102, 255, 0.08); color: #0066ff; }
.role-pill.role-admin { background: rgba(142, 142, 147, 0.1); color: #6e6e73; }
.role-pill.role-vip { background: rgba(191, 90, 242, 0.1); color: #bf5af2; }
.role-pill.role-student { background: rgba(52, 199, 89, 0.08); color: #248a3d; }

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: rgba(0, 0, 0, 0.015);
  border: 1px solid rgba(0, 0, 0, 0.02);
  padding: 12px;
  border-radius: 12px;
}

.detail-item span {
  font-size: 10px;
  color: #8e8e93;
  text-transform: uppercase;
}

.detail-item strong {
  font-size: 13px;
  color: #1c1c1e;
}

.quota-block.roomy {
  margin-bottom: 24px;
}

.quota-block.roomy .quota-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 8px;
}

.quota-block.roomy .quota-head span { color: #8e8e93; }
.quota-block.roomy .quota-head strong { color: #1c1c1e; }

.checkin-success-modal {
  position: relative;
  width: min(480px, 100%);
  overflow: hidden;
}

.checkin-success-modal::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 10%, rgba(52, 199, 89, 0.14), transparent 36%),
    radial-gradient(circle at 92% 24%, rgba(0, 102, 255, 0.12), transparent 32%);
  pointer-events: none;
}

.checkin-success-modal > * {
  position: relative;
  z-index: 1;
}

.modal-close-float {
  position: absolute;
  top: 18px;
  right: 18px;
}

.checkin-medal {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  margin-bottom: 18px;
  border-radius: 18px;
  color: #fff;
  background: linear-gradient(135deg, #1bb978, #0a84ff);
  font-size: 26px;
  font-weight: 900;
  box-shadow: 0 12px 24px rgba(20, 148, 111, 0.22);
}

.checkin-success-modal h3 {
  margin: 8px 0 10px;
  color: #111827;
  font-size: 22px;
}

.checkin-success-modal p {
  margin: 0;
  max-width: 56ch;
  color: #475569;
  line-height: 1.7;
}

.checkin-streak-panel {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 24px;
  padding: 16px 18px;
  border: 1px solid rgba(20, 148, 111, 0.18);
  border-radius: 16px;
  background: rgba(240, 253, 246, 0.84);
}

.checkin-streak-panel span {
  color: #25725a;
  font-size: 13px;
  font-weight: 800;
}

.checkin-streak-panel strong {
  color: #0f8d66;
  font-size: 28px;
  line-height: 1;
}

/* Toast */
.toast {
  position: fixed;
  left: 50%;
  bottom: 32px;
  transform: translateX(-50%);
  z-index: 2000;
  background: rgba(28, 28, 30, 0.9);
  color: #ffffff;
  backdrop-filter: blur(20px);
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

/* Transitions animation */
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.slide-up-enter-active, .slide-up-leave-active { transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-up-enter-from { opacity: 0; transform: translate(-50%, 12px); }
.slide-up-leave-to { opacity: 0; transform: translate(-50%, -12px); }

/* Responsive break points */
@media (max-width: 1024px) {
  .dashboard-split-layout {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}

@media (max-width: 768px) {
  .team-seats-header {
    flex-direction: column;
    align-items: stretch;
    padding: 20px;
  }
  
  .seats-right-container {
    justify-content: space-between;
  }

  .stats-pills-row {
    grid-template-columns: 1fr;
  }
}

/* Attachment Uploader styling */
.attachment-uploader {
  margin: 8px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.uploader-dropzone {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px dashed rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  font-size: 11px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.uploader-dropzone:hover {
  background: rgba(0, 102, 255, 0.04);
  border-color: rgba(0, 102, 255, 0.25);
  color: #0066ff;
}

.uploader-icon {
  width: 14px;
  height: 14px;
}

.uploader-preview-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.preview-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 6px;
}

.preview-item .file-name {
  font-size: 11px;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 85%;
}

.remove-file-btn {
  background: none;
  border: none;
  padding: 2px;
  cursor: pointer;
  color: #ef4444;
  display: flex;
  align-items: center;
}

.remove-file-btn svg {
  width: 12px;
  height: 12px;
}

/* Tasks attachment list styling */
.task-attachments-list {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  margin: 10px 0;
  width: 100%;
}

.attachment-wrapper {
  display: block;
  width: 100%;
}

.document-file-row {
  appearance: none;
  display: flex;
  align-items: center;
  gap: 18px;
  width: 100%;
  min-height: 76px;
  padding: 10px 12px;
  background: transparent;
  border: 0;
  border-radius: 12px;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.document-file-row:hover {
  background: rgba(0, 102, 255, 0.05);
}

.document-file-icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 54px;
  width: 54px;
  height: 64px;
  border-radius: 4px;
  color: #fff;
  background: linear-gradient(160deg, #8e9aab, #64748b);
  font-size: 25px;
  font-weight: 500;
  box-shadow: 0 7px 14px rgba(15, 23, 42, 0.12);
}

.document-file-icon::after {
  content: "";
  position: absolute;
  top: 0;
  right: 0;
  width: 15px;
  height: 15px;
  background: rgba(255, 255, 255, 0.72);
  clip-path: polygon(0 0, 100% 100%, 100% 0);
}

.document-file-icon.is-word {
  background: linear-gradient(160deg, #8ac0ff, #2878e6);
}

.document-file-icon.is-excel {
  background: linear-gradient(160deg, #83e9ad, #36c65e);
}

.document-file-icon.is-powerpoint {
  background: linear-gradient(160deg, #ffae7d, #e9622d);
}

.document-file-icon.is-pdf {
  background: linear-gradient(160deg, #ff8888, #ed3f45);
  font-size: 13px;
  font-weight: 800;
}

.document-file-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.document-file-meta strong {
  color: #25364d;
  font-size: 16px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.document-file-meta small {
  color: #8b93a3;
  font-size: 13px;
  font-weight: 600;
}

.document-link-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  color: var(--spatial-accent, #0066ff);
  background: rgba(0, 102, 255, 0.04);
  border-radius: 10px;
  text-decoration: none;
}

/* Image Attachment styling */
.image-attachment-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  padding: 8px;
  width: 100%;
  max-width: 420px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.image-attachment-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border-color: rgba(0, 102, 255, 0.2);
}

.task-attachment-img-preview {
  width: 100%;
  height: auto;
  max-height: 240px;
  object-fit: cover;
  border-radius: 9px;
  background: #f1f5f9;
}

.img-name {
  font-size: 12px;
  color: #64748b;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* File Format Badge */
.file-format-badge {
  font-size: 9px;
  font-weight: 700;
  color: #ffffff;
  background: var(--spatial-accent, #0066ff);
  padding: 1px 4px;
  border-radius: 4px;
  text-transform: uppercase;
}

/* Task Countdown Tag styling */
.task-countdown-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 99px;
  width: fit-content;
  margin-top: 0;
  flex-shrink: 0;
}

.task-countdown-tag.green {
  color: #10b981 !important;
  background: rgba(16, 185, 129, 0.08) !important;
  border: 1px solid rgba(16, 185, 129, 0.15) !important;
}

.task-countdown-tag.yellow {
  color: #f59e0b !important;
  background: rgba(245, 158, 11, 0.08) !important;
  border: 1px solid rgba(245, 158, 11, 0.15) !important;
}

.task-countdown-tag.red {
  color: #ff3b30 !important;
  background: rgba(255, 59, 48, 0.08) !important;
  border: 1px solid rgba(255, 59, 48, 0.15) !important;
  animation: pulse-breathing 2s infinite ease-in-out;
}

.task-countdown-tag.overdue {
  color: #8e8e93 !important;
  background: rgba(0, 0, 0, 0.05) !important;
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.funnel-icon {
  width: 11px;
  height: 11px;
}

/* Image Preview Lightbox Overlay */
.image-preview-overlay {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  padding: 92px 20px 20px !important;
  box-sizing: border-box !important;
  background: rgba(15, 23, 42, 0.62) !important;
  backdrop-filter: none !important;
  z-index: 2147483000 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.image-preview-container {
  position: relative;
  width: min(94vw, 1200px);
  height: min(calc(100vh - 122px), 900px);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.preview-image-stage {
  width: 100%;
  min-height: 0;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  border-radius: 14px;
}

.full-preview-image {
  max-width: 100%;
  max-height: 68vh;
  border-radius: 12px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.1);
  object-fit: contain;
  transform-origin: center center;
  transition: transform 0.18s ease;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  color: #fff;
  background: rgba(20, 20, 24, 0.82);
  border-radius: 12px;
}

.preview-toolbar button {
  min-width: 34px;
  height: 30px;
  padding: 0 10px;
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  border: 0;
  border-radius: 8px;
  cursor: pointer;
}

.preview-toolbar button:disabled {
  opacity: 0.4;
  cursor: default;
}

.preview-toolbar span {
  min-width: 54px;
  text-align: center;
  font-size: 12px;
}

.detail-preview-image {
  max-width: 100%;
  max-height: 250px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  cursor: zoom-in;
}

.preview-caption {
  color: #f1f5f9;
  font-size: 13px;
  font-weight: 500;
  background: rgba(0, 0, 0, 0.6);
  padding: 6px 14px;
  border-radius: 99px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.preview-close-btn {
  position: absolute;
  top: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #ffffff;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.preview-close-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: rotate(90deg);
}

/* Unified Team Resource Section styling */
.team-resources-section {
  margin-top: 32px;
  padding: 24px;
  border-radius: 20px;
}

.resources-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.resources-header .header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.resources-header h3 {
  font-family: 'Outfit', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: #1c1c1e;
  margin: 0;
}

.upload-resource-btn {
  background: var(--spatial-accent, #0066ff);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.upload-resource-btn:hover {
  background: #0052cc;
}

.resources-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.resource-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 12px;
  padding: 14px;
  transition: all 0.25s ease;
}

.resource-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.02);
  border-color: rgba(0, 102, 255, 0.1);
  background: rgba(255, 255, 255, 0.85);
}

.res-card-left {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
}

.res-icon-capsule {
  flex-shrink: 0;
}

.res-meta-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
}

.res-name {
  font-size: 13px;
  font-weight: 600;
  color: #1c1c1e;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.res-sub-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 10px;
  color: #8e8e93;
}

.res-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.res-action-btn {
  border: none;
  background: none;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.res-action-btn.download {
  color: #0066ff;
  background: rgba(0, 102, 255, 0.05);
}

.res-action-btn.download:hover {
  background: rgba(0, 102, 255, 0.12);
}

.res-action-btn.delete {
  color: #ff3b30;
  background: rgba(255, 59, 48, 0.05);
}

.res-action-btn.delete:hover {
  background: rgba(255, 59, 48, 0.12);
}

.resources-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #8e8e93;
  font-size: 12px;
  text-align: center;
}

@keyframes pulse-breathing {
  0% { opacity: 0.85; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.02); }
  100% { opacity: 0.85; transform: scale(1); }
}

/* Pagination and text truncation */
.task-desc-truncated {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
  transition: color 0.2s ease;
}
.task-desc-truncated:hover {
  color: var(--spatial-accent, #0066ff);
}
.ann-content-truncated {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
  transition: color 0.2s ease;
}
.ann-content-truncated:hover {
  color: var(--spatial-accent, #0066ff);
}

/* Height-stretch alignment */
.announcements-briefing {
  min-height: 330px;
  display: flex;
  flex-direction: column;
}
.announcements-briefing-stack {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}
.active-leaderboard-card {
  min-height: 420px;
  display: flex;
  flex-direction: column;
}
.student-leaderboard-stack {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

/* Pagination bar styles */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 15px;
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 5px;
}
.pager-btn {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 50%;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: inherit;
  transition: all 0.2s ease;
}
.pager-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
  color: #0f172a;
}
.pager-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.arrow-icon {
  width: 14px;
  height: 14px;
}

/* Detail modal styles */
.detail-modal-card {
  width: 90%;
  max-width: 600px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 24px;
  padding: 30px;
  box-shadow: 0 30px 70px rgba(0, 0, 0, 0.2);
  transform: scale(0.95);
  animation: detail-modal-enter 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  color: #1c1c1e;
}
.dark .detail-modal-card {
  background: rgba(30, 30, 35, 0.75);
  border-color: rgba(255, 255, 255, 0.1);
  color: #f1f5f9;
}
@keyframes detail-modal-enter {
  to {
    transform: scale(1);
  }
}
</style>
