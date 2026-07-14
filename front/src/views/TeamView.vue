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
          <p class="seats-helper-text">
            团队席位用于协作任务、公告同步和共享材料管理；导师车队可开放团队成员加入。
          </p>
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
              @click="showToast('席位已满，继续加人需开通或升级团队会员')"
              title="开通导师车队或团队 Plus 后可管理团队席位"
            >
              <span class="avatar-inner-wrapper"><span class="plus-symbol">+</span></span>
              <span class="seat-member-name">升级</span>
            </button>
          </div>
        </div>

        <div class="seats-right-container">
          <div class="team-plan-flag" :class="{ active: hasTeamFleetPlan }">
            <span>{{ teamFleetLabel }}</span>
            <strong>{{ hasTeamFleetPlan ? "全队共享权益" : "待开通团队权益" }}</strong>
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
            <div class="roster-table-head">
              <span>成员</span>
              <span>科研状态</span>
              <span>权益与操作</span>
            </div>
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
                    <span>成员权益</span>
                    <strong>{{ member.role === "导师" && hasTeamFleetPlan ? "车队共享中" : "基础席位" }}</strong>
                  </div>
                  <div class="member-benefit-strip">
                    <span>论文导入</span>
                    <span>翻译</span>
                    <span :class="{ muted: !hasTeamFleetPlan }">PPT 权益</span>
                  </div>

                  <div class="quota-trigger-actions">
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
              <span>团队席位</span>
              <strong>{{ teamStore.usedSeats }} / {{ teamStore.totalSeats }}</strong>
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
              <div v-if="!teamStore.tasks.length" class="empty-state-text">暂无科研任务，点击右上角添加任务。</div>
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
              <div v-if="!teamStore.announcements.length" class="empty-state-text">暂无公告，点击右上角发布公告。</div>
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

        </div>
      </div>

      <!-- Student View -->
      <div v-else class="dashboard-split-layout student-view">
        <!-- Student: Left Column (My Workbench) -->
        <div class="dashboard-col left-col">
          <!-- My membership and research rhythm -->
          <div class="student-glass-card my-stats-panel">
            <div class="student-status-heading">
              <div>
                <h3>我的团队权益</h3>
                <p>当前账号未开通套餐，仅保留基础团队协作能力。</p>
              </div>
              <span class="member-plan-pill" :class="{ active: hasTeamFleetPlan }">
                {{ hasTeamFleetPlan ? "车队会员共享" : "未开通套餐" }}
              </span>
            </div>

            <div class="benefit-lane">
              <div class="benefit-item enabled">
                <span>论文导入 / 翻译</span>
                <strong>基础可用</strong>
              </div>
              <div class="benefit-item">
                <span>论文综述</span>
                <strong>{{ hasTeamFleetPlan ? "共享可用" : "未开通" }}</strong>
              </div>
              <div class="benefit-item">
                <span>组会 PPT</span>
                <strong>{{ hasTeamFleetPlan ? "车队额度" : "未开通" }}</strong>
              </div>
            </div>

            <div class="research-rhythm-panel">
              <div>
                <span>科研等级</span>
                <strong>Lv.{{ getMemberLevelInfo(currentUserMember?.activeTime).level }}</strong>
                <small>{{ getMemberLevelInfo(currentUserMember?.activeTime).title }}</small>
              </div>
              <div>
                <span>连续打卡</span>
                <strong>{{ currentCheckinItem?.streak || checkinStreak || 0 }} 天</strong>
                <small>保持节奏</small>
              </div>
              <div>
                <span>有效学术时长</span>
                <strong>{{ formatActiveTime(currentUserMember?.activeTime) }}</strong>
                <small>今日科研记录</small>
              </div>
              <div>
                <span>任务进度</span>
                <strong>{{ completedTaskCount }} / {{ teamStore.tasks.length }}</strong>
                <small>本页任务</small>
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
              <div v-if="!teamStore.tasks.length" class="empty-state-text">暂无待办科研任务。</div>
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
            <div class="checklist-header">
              <h3>重要公告通知</h3>
              <span class="task-count-indicator">共 {{ teamStore.announcements.length }} 项</span>
            </div>
            <div class="student-task-list">
              <article
                v-for="ann in paginatedAnnouncements"
                :key="ann.id"
                class="student-task-item announcement-task-item"
              >
                <div class="task-info">
                  <h4 @click="openDetailModal('announcement', ann)" style="cursor: pointer; text-decoration: underline; text-underline-offset: 4px;">{{ ann.title }}</h4>
                  <p class="task-desc-truncated" @click="openDetailModal('announcement', ann)">{{ ann.content }}</p>
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
                  <small>发布时间: {{ ann.publishTime }}</small>
                </div>
              </article>
              <div v-if="!teamStore.announcements.length" class="empty-state-text">暂无重要公告。</div>
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
            <div class="form-grid single">
              <label>
                <span>角色</span>
                <select v-model="newMemberRole">
                  <option value="学生">学生</option>
                  <option value="特权用户">特权用户</option>
                  <option value="管理员">管理员</option>
                </select>
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
              <span>会员权益</span>
              <strong>{{ hasTeamFleetPlan ? "车队权益共享中" : "未开通套餐" }}</strong>
            </div>
            <div class="member-benefit-strip roomy">
              <span>基础导入</span>
              <span>团队任务</span>
              <span :class="{ muted: !hasTeamFleetPlan }">PPT 生成</span>
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
          <div class="fruit-wheel-wrap">
            <div
              class="fruit-wheel"
              :class="{ spinning: fruitDrawSpinning }"
              :style="{ transform: `rotate(${fruitWheelRotation}deg)` }"
            >
              <span v-for="value in fruitWheelValues" :key="value">+{{ value }}</span>
            </div>
            <i class="fruit-wheel-pointer"></i>
          </div>
          <span class="panel-eyebrow">Daily Research Check-in</span>
          <h3>{{ checkinDialogTitle }}</h3>
          <p>{{ checkinMotivation }}</p>
          <button
            v-if="!currentCheckinItem?.fruitClaimed"
            class="fruit-draw-btn"
            :disabled="fruitDrawLoading"
            @click="drawFruit"
          >
            {{ fruitDrawLoading ? "转盘抽取中..." : "抽取今日硕果" }}
          </button>
          <div v-else class="fruit-award-panel">
            <span>今日获得</span>
            <strong>+{{ currentCheckinItem?.fruitAward || fruitDrawAward || 0 }} 硕果</strong>
          </div>
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
const fruitWheelValues = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
const fruitWheelRotation = ref(0);
const fruitDrawLoading = ref(false);
const fruitDrawSpinning = ref(false);
const fruitDrawAward = ref(0);
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

const teamTutor = computed(() => teamStore.members.find((member) => member.role === "导师"));
const teamFleetPlan = computed(() => normalizeMembershipPlan(teamTutor.value?.membershipPlan || "free"));
const hasTeamFleetPlan = computed(() => ["team_plus", "team_pro"].includes(teamFleetPlan.value));
const teamFleetLabel = computed(() => teamFleetPlan.value === "team_pro" ? "团队 Pro" : teamFleetPlan.value === "team_plus" ? "团队 Plus" : "未开通团队套餐");

function normalizeMembershipPlan(plan) {
  return ({ light: "lite", study: "plus", lab: "pro", team: "team_plus" })[plan] || plan || "free";
}

const completedTaskCount = computed(() => {
  return teamStore.tasks.filter((task) => task.status === "已完成").length;
});

const completionRate = computed(() => {
  if (!teamStore.tasks.length) return 0;
  return Math.round((completedTaskCount.value / teamStore.tasks.length) * 100);
});

const taskPage = ref(1);
const taskPageSize = 4;

const annPage = ref(1);
const annPageSize = 4;

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

function formatActiveTime(seconds) {
  if (!seconds) return "0分钟";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}小时${m}分钟`;
  if (m > 0) return `${m}分钟${s}秒`;
  return `${s}秒`;
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
    });
    newMemberName.value = "";
    newMemberEmail.value = "";
    newMemberRole.value = "学生";
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
  fruitDrawAward.value = Number(saved?.fruitAward || currentCheckinItem.value?.fruitAward || 0);
  const claimed = Boolean(saved?.fruitClaimed ?? currentCheckinItem.value?.fruitClaimed);
  checkinDialogTitle.value = claimed ? "今日硕果已入账" : "签到成功，抽取今日硕果";
  checkinMotivation.value = claimed
    ? `今天已经获得 ${fruitDrawAward.value || 0} 枚硕果，继续保持。`
    : "先完成签到，再亲手转动一次硕果转盘。连续签到越久，高额硕果概率越大。";
  showCheckinModal.value = true;
}

async function drawFruit() {
  if (fruitDrawLoading.value || currentCheckinItem.value?.fruitClaimed) return;
  fruitDrawLoading.value = true;
  fruitDrawSpinning.value = true;
  fruitWheelRotation.value += 720 + Math.floor(Math.random() * 240);
  try {
    const saved = await teamStore.drawCheckinFruit(currentMemberId.value);
    fruitDrawAward.value = Number(saved.fruitAward || 0);
    const prizeIndex = Math.max(0, fruitWheelValues.indexOf(fruitDrawAward.value));
    const segment = 360 / fruitWheelValues.length;
    fruitWheelRotation.value += 720 + (360 - prizeIndex * segment) - segment / 2;
    window.setTimeout(() => {
      fruitDrawSpinning.value = false;
      openCheckinDialog(saved);
    }, 650);
  } catch (error) {
    fruitDrawSpinning.value = false;
    showToast(error.response?.data?.message || "抽取硕果失败，请稍后重试");
  } finally {
    window.setTimeout(() => {
      fruitDrawLoading.value = false;
    }, 700);
  }
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
    linear-gradient(135deg, rgba(37, 99, 235, 0.08), transparent 34%),
    linear-gradient(180deg, #f4f7fb 0%, #eef3f8 100%);
  color: var(--spatial-graphite, #1c1c1e);
  font-family: var(--spatial-font-body, "Inter", -apple-system, sans-serif);
}

.team-page .spatial-orb {
  display: none;
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
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: stretch;
  gap: 24px;
  padding: 28px;
  border-radius: 26px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.95) 58%, rgba(239, 249, 244, 0.94));
  border: 1px solid #dce7f4;
  box-shadow: 0 22px 55px rgba(30, 53, 92, 0.11);
}

.seats-left-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
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

.seats-helper-text {
  margin: -2px 0 6px;
  color: #516173;
  font-size: 13px;
  line-height: 1.55;
}

.seats-avatar-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 18px;
  background: rgba(245, 248, 253, 0.86);
  border: 1px solid rgba(206, 218, 235, 0.78);
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
  width: 50px;
  height: 50px;
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
  transform: translateY(-2px);
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
  display: grid;
  grid-template-columns: 1fr;
  align-content: space-between;
  justify-items: end;
  gap: 14px;
  min-width: 250px;
  padding-left: 22px;
  border-left: 1px solid #dbe5f1;
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

.dashboard-split-layout.tutor-view {
  grid-template-columns: minmax(0, 1.6fr) minmax(360px, 0.78fr);
  gap: 22px;
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
  gap: 0;
  overflow: hidden;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid #dce6f1;
  box-shadow: 0 18px 42px rgba(31, 48, 84, 0.08);
}

.roster-table-head {
  display: grid;
  grid-template-columns: minmax(230px, 1.15fr) minmax(300px, 1.35fr) minmax(250px, 1fr);
  gap: 18px;
  align-items: center;
  padding: 13px 20px;
  background: #f5f8fc;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.member-management-card {
  background: #ffffff;
  border: 0;
  border-top: 1px solid #eef2f7;
  border-radius: 0;
  padding: 16px 20px;
  box-shadow: none;
  display: grid;
  grid-template-columns: minmax(230px, 1.15fr) minmax(300px, 1.35fr) minmax(250px, 1fr);
  align-items: center;
  gap: 18px;
  transition: background 0.18s ease;
}

.member-management-card:hover {
  background: #fbfdff;
}

.card-identity-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-large-shell {
  position: relative;
  width: 46px;
  height: 46px;
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
  gap: 10px;
  border: 0;
  padding: 12px;
  border-radius: 14px;
  background: #f7f9fc;
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
  background: transparent;
  border-radius: 0;
  padding: 0;
  border: 0;
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

.member-benefit-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.member-benefit-strip span {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #eef7f2;
  color: #137348;
  border: 1px solid #cdebdc;
  font-size: 10px;
  font-weight: 750;
}

.member-benefit-strip span.muted {
  background: #f4f6f9;
  color: #778397;
  border-color: #e1e7ef;
}

.member-benefit-strip.roomy {
  padding: 12px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e5ebf3;
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
  gap: 0;
  overflow: hidden;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid #dce6f1;
  box-shadow: 0 16px 36px rgba(31, 48, 84, 0.07);
}

.tutor-metric-card {
  background: #ffffff;
  border: 0;
  border-right: 1px solid #edf2f7;
  border-radius: 0;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tutor-metric-card:last-child {
  border-right: 0;
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
  background: #ffffff;
  border: 1px solid #dce6f1;
  border-radius: 20px;
  padding: 18px;
  box-shadow: 0 16px 36px rgba(31, 48, 84, 0.07);
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
  background: #f3f7ff;
  border: 1px solid #d8e5ff;
  border-radius: 999px;
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--spatial-accent, #0066ff);
  cursor: pointer;
}

.tutor-inline-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 14px;
  border-radius: 14px;
}

.form-row-grid {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.form-grid.single {
  grid-template-columns: 1fr;
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
  gap: 10px;
}

.admin-item-card {
  background: #fbfdff;
  border: 1px solid #e6edf5;
  border-radius: 16px;
  padding: 13px;
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

/* Student membership rhythm */
.my-stats-panel {
  display: grid;
  gap: 18px;
}

.student-status-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e8edf4;
}

.student-status-heading h3 {
  margin: 0 0 6px;
}

.student-status-heading p {
  margin: 0;
  color: #53657a;
  font-size: 13px;
  line-height: 1.5;
}

.member-plan-pill {
  flex: none;
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #fff7ed;
  color: #9a4b10;
  border: 1px solid #fed7aa;
  font-size: 12px;
  font-weight: 800;
}

.member-plan-pill.active {
  background: #ecfdf3;
  color: #067647;
  border-color: #bbf7d0;
}

.benefit-lane {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.benefit-item {
  min-height: 86px;
  display: grid;
  align-content: center;
  gap: 7px;
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.benefit-item.enabled {
  background: #eefcf5;
  border-color: #c7ead7;
}

.benefit-item span {
  color: #617087;
  font-size: 12px;
  font-weight: 750;
}

.benefit-item strong {
  color: #142033;
  font-size: 18px;
  font-weight: 850;
}

.research-rhythm-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid #dfe7f1;
  border-radius: 18px;
  background: #ffffff;
}

.research-rhythm-panel > div {
  min-height: 92px;
  display: grid;
  align-content: center;
  gap: 4px;
  padding: 16px;
  border-right: 1px solid #e8edf4;
}

.research-rhythm-panel > div:last-child {
  border-right: 0;
}

.research-rhythm-panel span {
  color: #66758a;
  font-size: 11px;
  font-weight: 760;
}

.research-rhythm-panel strong {
  color: #121a2a;
  font-size: 20px;
  font-weight: 860;
}

.research-rhythm-panel small {
  color: #8a96a8;
  font-size: 10px;
}

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

.fruit-wheel-wrap {
  position: relative;
  width: 172px;
  height: 172px;
  display: grid;
  place-items: center;
  margin: 0 auto 20px;
}

.fruit-wheel {
  position: relative;
  width: 152px;
  height: 152px;
  border: 9px solid #fff;
  border-radius: 50%;
  background:
    conic-gradient(
      from -18deg,
      #e6f7ee 0 36deg,
      #eef5ff 36deg 72deg,
      #fff4e3 72deg 108deg,
      #f2edff 108deg 144deg,
      #e8fbf6 144deg 180deg,
      #fff0f4 180deg 216deg,
      #edf7ff 216deg 252deg,
      #f7f4e9 252deg 288deg,
      #f0fdf4 288deg 324deg,
      #f3f7ff 324deg 360deg
    );
  box-shadow: 0 18px 36px rgba(28, 45, 75, .14), inset 0 0 0 1px rgba(21, 39, 70, .08);
  transition: transform 720ms cubic-bezier(.16, 1, .3, 1);
}

.fruit-wheel.spinning {
  transition-duration: 980ms;
}

.fruit-wheel::after {
  content: "";
  position: absolute;
  inset: 43px;
  border-radius: 50%;
  background: #fff;
  box-shadow: inset 0 0 0 1px rgba(21, 39, 70, .08);
}

.fruit-wheel span {
  position: absolute;
  left: 50%;
  top: 50%;
  z-index: 1;
  color: #185f48;
  font-size: 14px;
  font-weight: 900;
  transform-origin: 0 0;
}

.fruit-wheel span:nth-child(1) { transform: rotate(0deg) translate(51px) rotate(0deg); }
.fruit-wheel span:nth-child(2) { transform: rotate(36deg) translate(51px) rotate(-36deg); }
.fruit-wheel span:nth-child(3) { transform: rotate(72deg) translate(51px) rotate(-72deg); }
.fruit-wheel span:nth-child(4) { transform: rotate(108deg) translate(51px) rotate(-108deg); }
.fruit-wheel span:nth-child(5) { transform: rotate(144deg) translate(51px) rotate(-144deg); }
.fruit-wheel span:nth-child(6) { transform: rotate(180deg) translate(51px) rotate(-180deg); }
.fruit-wheel span:nth-child(7) { transform: rotate(216deg) translate(51px) rotate(-216deg); }
.fruit-wheel span:nth-child(8) { transform: rotate(252deg) translate(51px) rotate(-252deg); }
.fruit-wheel span:nth-child(9) { transform: rotate(288deg) translate(51px) rotate(-288deg); }
.fruit-wheel span:nth-child(10) { transform: rotate(324deg) translate(51px) rotate(-324deg); }

.fruit-wheel-pointer {
  position: absolute;
  top: 0;
  left: 50%;
  z-index: 3;
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-top: 18px solid #0f8d66;
  transform: translateX(-50%);
  filter: drop-shadow(0 6px 8px rgba(15, 141, 102, .22));
}

.fruit-draw-btn {
  width: 100%;
  min-height: 46px;
  margin-top: 20px;
  border: 0;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(135deg, #12805c, #1c7bf2);
  font-size: 15px;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 14px 28px rgba(24, 111, 189, .18);
}

.fruit-draw-btn:disabled {
  cursor: not-allowed;
  opacity: .72;
}

.fruit-award-panel {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 20px;
  padding: 16px 18px;
  border: 1px solid rgba(37, 99, 235, .16);
  border-radius: 16px;
  color: #1e3a8a;
  background: #f4f8ff;
}

.fruit-award-panel span {
  font-size: 13px;
  font-weight: 850;
}

.fruit-award-panel strong {
  font-size: 24px;
  line-height: 1;
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

/* Frontend-design: Swiss laboratory board
   White surface, hairline rules, asymmetric work columns. No stacked rounded cards. */
.team-page {
  --team-bg: #F7F7F8;
  --team-surface: #FFFFFF;
  --team-ink: #111111;
  --team-muted: #5F6368;
  --team-faint: #8B9098;
  --team-line: #D8DADF;
  --team-accent: #002FA7;
  --team-alert: #E4002B;
  padding: 0 28px 80px;
  background: var(--team-bg);
  color: var(--team-ink);
  font-family: "Helvetica Neue", Helvetica, Arial, system-ui, sans-serif;
}

.team-shell {
  max-width: 1640px;
  gap: 0;
  padding-top: 30px;
}

.team-seats-header,
.student-glass-card,
.admin-action-section,
.members-cards-container,
.team-resources-section,
.spatial-glass-panel,
.summary-metric-cards,
.briefing-ann-card,
.admin-item-card,
.resource-card,
.benefit-item,
.research-rhythm-panel,
.detail-item {
  border-radius: 0;
  box-shadow: none;
  backdrop-filter: none;
}

.team-seats-header {
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 0;
  padding: 0;
  background: var(--team-surface);
  border: 0;
  border-top: 2px solid var(--team-ink);
  border-bottom: 1px solid var(--team-ink);
}

.seats-left-container {
  gap: 14px;
  padding: 28px 32px 26px 0;
}

.seats-label-row {
  align-items: baseline;
  gap: 16px;
}

.seats-label-row h2 {
  color: var(--team-ink);
  font-size: 34px;
  line-height: 0.95;
  font-weight: 800;
  letter-spacing: 0;
}

.seats-count-badge,
.col-meta-pill,
.task-count-indicator,
.status-tag,
.member-plan-pill,
.role-badge,
.role-pill {
  border-radius: 0;
  background: transparent;
  border: 1px solid var(--team-line);
  color: var(--team-accent);
}

.seats-count-badge {
  padding: 4px 8px;
  font-size: 13px;
}

.seats-helper-text {
  max-width: 66ch;
  margin: 0;
  color: var(--team-muted);
  font-size: 14px;
}

.seats-avatar-row {
  gap: 12px;
  padding: 18px 0 0;
  background: transparent;
  border: 0;
  border-top: 1px solid var(--team-line);
  border-radius: 0;
}

.avatar-inner-wrapper,
.avatar-large-shell,
.avatar-shell {
  box-shadow: none;
  border-color: var(--team-line);
}

.seat-circle-avatar:hover .avatar-inner-wrapper {
  transform: none;
  border-color: var(--team-accent);
  box-shadow: none;
}

.status-indicator-dot.online,
.status-dot.online,
.pulse-glow-dot.active {
  background: var(--team-accent);
  box-shadow: none;
}

.seat-member-name,
.email-subtext,
.plate-label,
.card-detail-item span,
.quota-label-line span,
.tutor-metric-card span,
.research-rhythm-panel span,
.benefit-item span,
.item-main-content small,
.checkin-clock span {
  color: var(--team-muted);
}

.seats-right-container {
  align-content: stretch;
  justify-items: stretch;
  gap: 18px;
  min-width: 0;
  padding: 28px 0 26px 30px;
  border-left: 1px solid var(--team-ink);
}

.team-plan-flag {
  min-width: 0;
  padding: 12px;
  background: var(--team-surface);
  border: 1px solid var(--team-ink);
  border-radius: 0;
}

.team-plan-flag.active {
  background: var(--team-surface);
  border-color: var(--team-accent);
  box-shadow: none;
}

.team-plan-flag strong,
.team-plan-flag.active strong,
.plate-code {
  color: var(--team-ink);
}

.team-identity-plate {
  align-items: flex-start;
  padding-top: 16px;
  border-top: 1px solid var(--team-line);
}

.plate-code {
  font-size: 24px;
  letter-spacing: 0;
}

.invite-main-btn,
.student-primary-action-btn,
.submit-form-btn,
.btn-toggle-form,
.apple-btn.apple-btn-primary,
.res-action-btn.download {
  border-radius: 0;
  box-shadow: none;
  background: var(--team-accent);
  border: 1px solid var(--team-accent);
  color: #FFFFFF;
}

.btn-toggle-form,
.student-primary-action-btn.already-checked {
  background: var(--team-surface);
  color: var(--team-accent);
}

.invite-main-btn:hover,
.student-primary-action-btn:hover {
  transform: none;
  background: var(--team-ink);
  border-color: var(--team-ink);
}

.dashboard-split-layout {
  grid-template-columns: minmax(0, 1.2fr) 430px;
  gap: 0;
  border-bottom: 1px solid var(--team-line);
}

.dashboard-split-layout.tutor-view {
  grid-template-columns: minmax(0, 1.55fr) 430px;
  gap: 0;
}

.dashboard-col {
  gap: 28px;
}

.dashboard-col.left-col {
  padding: 34px 34px 40px 0;
}

.dashboard-col.right-col {
  padding: 34px 0 40px 34px;
  border-left: 1px solid var(--team-line);
}

.col-header-row,
.section-title-bar,
.checklist-header,
.resources-header,
.student-status-heading,
.sign-in-header {
  border-bottom: 1px solid var(--team-ink);
}

.col-header-row h3,
.admin-action-section h4,
.student-glass-card h3,
.checklist-header h3,
.resources-header h3 {
  color: var(--team-ink);
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0;
}

.student-glass-card,
.admin-action-section,
.team-resources-section {
  padding: 0;
  background: transparent;
  border: 0;
}

.student-glass-card:not(:first-child),
.admin-action-section:not(:first-child),
.team-resources-section {
  padding-top: 24px;
  border-top: 1px solid var(--team-line);
}

.workbench-sign-in {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  border-top: 2px solid var(--team-ink);
  padding-top: 20px;
}

.workbench-sign-in .sign-in-header {
  display: grid;
  gap: 18px;
  align-content: start;
  padding: 0;
  margin: 0;
  border: 0;
}

.workbench-sign-in .sign-in-body {
  align-items: end;
  justify-content: end;
  gap: 18px;
}

.checkin-clock {
  text-align: left;
}

.checkin-clock strong {
  color: var(--team-ink);
  font-size: 28px;
}

.my-stats-panel {
  gap: 20px;
}

.student-status-heading {
  align-items: end;
  padding-bottom: 14px;
}

.student-status-heading p,
.sign-in-text p {
  color: var(--team-muted);
}

.benefit-lane {
  display: block;
  border-top: 1px solid var(--team-line);
}

.benefit-item {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  padding: 14px 0;
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--team-line);
}

.benefit-item.enabled {
  background: transparent;
  border-color: var(--team-line);
}

.benefit-item strong {
  color: var(--team-ink);
  font-size: 18px;
}

.research-rhythm-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  background: transparent;
  border: 0;
  border-top: 1px solid var(--team-line);
  border-bottom: 1px solid var(--team-line);
}

.research-rhythm-panel > div {
  min-height: 0;
  flex: 1 1 190px;
  padding: 16px 18px 16px 0;
  border-right: 0;
}

.research-rhythm-panel strong,
.tutor-metric-card strong,
.card-detail-item strong {
  color: var(--team-ink);
}

.members-cards-container,
.summary-metric-cards {
  overflow: visible;
  background: transparent;
  border: 0;
}

.roster-table-head {
  padding: 12px 0;
  background: transparent;
  color: var(--team-muted);
  border-top: 1px solid var(--team-ink);
  border-bottom: 1px solid var(--team-line);
}

.member-management-card {
  padding: 16px 0;
  background: transparent;
  border-top: 0;
  border-bottom: 1px solid var(--team-line);
}

.member-management-card:hover {
  background: transparent;
}

.card-details-grid,
.member-benefit-strip.roomy,
.detail-item {
  background: transparent;
  border: 0;
  border-radius: 0;
  padding: 0;
}

.member-benefit-strip span {
  border-radius: 0;
  background: transparent;
  border-color: var(--team-line);
  color: var(--team-accent);
}

.member-benefit-strip span.muted {
  background: transparent;
  color: var(--team-muted);
}

.summary-metric-cards {
  display: block;
  border-top: 2px solid var(--team-ink);
}

.tutor-metric-card {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: baseline;
  padding: 14px 0;
  background: transparent;
  border-right: 0;
  border-bottom: 1px solid var(--team-line);
}

.tutor-inline-form,
.admin-item-card,
.briefing-ann-card,
.student-task-item,
.resource-card {
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--team-line);
  border-radius: 0;
}

.tutor-inline-form {
  padding: 16px 0;
}

.tutor-inline-form input,
.tutor-inline-form textarea,
.modal-form input,
.modal-form select {
  border-radius: 0;
  border: 1px solid var(--team-line);
  background: var(--team-surface);
}

.admin-item-card,
.briefing-ann-card {
  padding: 16px 0;
}

.item-title-row h5,
.student-task-item h4,
.briefing-ann-card h4 {
  color: var(--team-ink);
  text-decoration: none !important;
}

.student-task-list,
.announcements-briefing-stack,
.admin-items-list,
.resources-grid,
.student-leaderboard-stack {
  gap: 0;
}

.student-task-item {
  padding: 18px 0;
}

.task-countdown-tag,
.published-action-btn,
.res-action-btn,
.pager-btn,
.action-btn-mini,
.action-btn-link {
  border-radius: 0;
  box-shadow: none;
}

.action-btn-link,
.published-action-btn,
.btn-toggle-form {
  color: var(--team-accent);
}

.action-btn-link.danger,
.published-action-btn.danger,
.res-action-btn.delete {
  color: var(--team-alert);
}

.image-attachment-card,
.document-file-row,
.document-link-row,
.uploader-dropzone {
  border-radius: 0;
  border-color: var(--team-line);
  background: var(--team-surface);
}

.team-resources-section {
  margin-top: 0;
  border-top: 2px solid var(--team-ink);
}

.resources-grid {
  display: block;
}

.resource-card {
  display: flex;
  padding: 18px 0;
}

.toast {
  border-radius: 0;
  box-shadow: none;
  background: var(--team-ink);
}

/* Responsive break points */
@media (max-width: 1024px) {
  .dashboard-split-layout {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .team-seats-header {
    grid-template-columns: 1fr;
  }

  .seats-right-container {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    justify-items: stretch;
    min-width: 0;
    padding-left: 0;
    padding-top: 18px;
    border-left: 0;
    border-top: 1px solid #dbe5f1;
  }

  .roster-table-head {
    display: none;
  }

  .member-management-card {
    grid-template-columns: 1fr;
    gap: 14px;
    border-radius: 16px;
    margin: 10px;
    border: 1px solid #e6edf5;
  }

  .benefit-lane,
  .research-rhythm-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .research-rhythm-panel > div:nth-child(2n) {
    border-right: 0;
  }

  .research-rhythm-panel > div {
    border-bottom: 1px solid #e8edf4;
  }

  .research-rhythm-panel > div:nth-last-child(-n + 2) {
    border-bottom: 0;
  }
}

@media (max-width: 768px) {
  .team-seats-header {
    align-items: stretch;
    padding: 20px;
  }
  
  .seats-right-container {
    grid-template-columns: 1fr;
  }

  .stats-pills-row {
    grid-template-columns: 1fr;
  }

  .benefit-lane,
  .research-rhythm-panel {
    grid-template-columns: 1fr;
  }

  .research-rhythm-panel > div,
  .research-rhythm-panel > div:nth-child(2n),
  .research-rhythm-panel > div:nth-last-child(-n + 2) {
    border-right: 0;
    border-bottom: 1px solid #e8edf4;
  }

  .research-rhythm-panel > div:last-child {
    border-bottom: 0;
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

/* Impeccable redesign: calm research operations surface.
   Soft hierarchy, flowing bands, restrained color, no grid-of-cards rhythm. */
.team-page {
  --team-bg: #eef3f8;
  --team-surface: #ffffff;
  --team-surface-soft: #f7faff;
  --team-ink: #142033;
  --team-muted: #5b6a80;
  --team-faint: #8794a8;
  --team-line: #d9e3ee;
  --team-blue: #2d63d8;
  --team-green: #0f8f66;
  --team-red: #c73b45;
  padding: 34px 22px 88px;
  background:
    radial-gradient(circle at 8% -10%, rgba(68, 111, 210, 0.12), transparent 30%),
    linear-gradient(180deg, #f6f9fc 0%, var(--team-bg) 100%);
  color: var(--team-ink);
  font-family: Inter, "Helvetica Neue", Arial, system-ui, sans-serif;
}

.team-shell {
  max-width: 1480px;
  gap: 24px;
}

.team-seats-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 0;
  padding: 0;
  overflow: hidden;
  background: var(--team-surface);
  border: 1px solid var(--team-line);
  border-radius: 22px;
}

.seats-left-container {
  gap: 18px;
  padding: 28px 30px;
  min-width: 0;
}

.seats-label-row {
  align-items: center;
  gap: 12px;
}

.seats-label-row h2 {
  color: var(--team-ink);
  font-size: 25px;
  line-height: 1.1;
  font-weight: 820;
}

.seats-count-badge {
  padding: 5px 10px;
  border: 0;
  border-radius: 999px;
  background: #eaf1ff;
  color: var(--team-blue);
  font-size: 12px;
}

.seats-helper-text {
  max-width: 66ch;
  color: var(--team-muted);
  font-size: 13px;
  line-height: 1.65;
}

.seats-avatar-row {
  gap: 14px;
  padding: 16px;
  background: linear-gradient(180deg, #f8fbff, #f3f7fc);
  border: 1px solid #e2ebf5;
  border-radius: 18px;
}

.seat-circle-avatar {
  min-width: 58px;
}

.avatar-inner-wrapper {
  width: 52px;
  height: 52px;
  border: 3px solid #ffffff;
  background: #ffffff;
  box-shadow: 0 4px 10px rgba(43, 73, 118, 0.12);
}

.seat-circle-avatar:hover .avatar-inner-wrapper {
  transform: translateY(-2px);
  border-color: #dbe8ff;
  box-shadow: 0 6px 12px rgba(43, 73, 118, 0.14);
}

.status-indicator-dot.online,
.status-dot.online,
.pulse-glow-dot.active {
  background: var(--team-green);
  box-shadow: 0 0 0 4px rgba(15, 143, 102, 0.13);
}

.seat-member-name {
  color: #536176;
  font-size: 11px;
}

.seats-right-container {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
  min-width: 0;
  padding: 28px;
  background: #f7fbf9;
  border-left: 1px solid var(--team-line);
}

.team-plan-flag {
  min-width: 0;
  padding: 14px;
  background: #ffffff;
  border: 0;
  border-radius: 16px;
}

.team-plan-flag span,
.plate-label {
  color: var(--team-muted);
  font-size: 12px;
  letter-spacing: 0;
  text-transform: none;
}

.team-plan-flag strong,
.team-plan-flag.active strong,
.plate-code {
  color: var(--team-ink);
}

.team-identity-plate {
  align-items: flex-start;
  padding: 0;
  border: 0;
}

.plate-code {
  margin-top: 4px;
  font-size: 22px;
  letter-spacing: 0;
}

.invite-main-btn,
.student-primary-action-btn,
.submit-form-btn,
.apple-btn.apple-btn-primary,
.res-action-btn.download {
  border: 0;
  border-radius: 12px;
  background: var(--team-blue);
  color: #ffffff;
  box-shadow: none;
}

.btn-toggle-form,
.student-primary-action-btn.already-checked {
  border: 0;
  border-radius: 999px;
  background: #edf5ef;
  color: #247456;
}

.invite-main-btn:hover,
.student-primary-action-btn:hover {
  transform: translateY(-1px);
  background: #214fb5;
}

.dashboard-split-layout,
.dashboard-split-layout.tutor-view {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  border: 0;
}

.dashboard-col.left-col {
  flex: 1 1 auto;
  min-width: 0;
  padding: 0;
}

.dashboard-col.right-col {
  flex: 0 0 390px;
  padding: 0;
  border: 0;
}

.dashboard-col {
  gap: 18px;
}

.student-glass-card,
.admin-action-section,
.team-resources-section {
  position: relative;
  padding: 22px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--team-line);
  border-radius: 20px;
  box-shadow: none;
  backdrop-filter: none;
}

.student-glass-card:not(:first-child),
.admin-action-section:not(:first-child),
.team-resources-section {
  padding-top: 22px;
  border-top: 1px solid var(--team-line);
}

.student-glass-card h3,
.admin-action-section h4,
.col-header-row h3,
.checklist-header h3,
.resources-header h3 {
  margin: 0;
  color: var(--team-ink);
  font-size: 18px;
  line-height: 1.25;
  font-weight: 820;
  letter-spacing: 0;
}

.col-header-row,
.section-title-bar,
.checklist-header,
.resources-header,
.student-status-heading,
.sign-in-header {
  padding-bottom: 14px;
  border-bottom: 1px solid #e6edf5;
}

.workbench-sign-in {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 28px;
}

.workbench-sign-in .sign-in-header {
  display: grid;
  align-content: start;
  gap: 10px;
  padding: 0;
  margin: 0;
  border: 0;
}

.workbench-sign-in .sign-in-body {
  justify-content: flex-end;
  align-items: center;
  gap: 18px;
}

.checkin-clock {
  text-align: left;
}

.checkin-clock strong {
  color: var(--team-ink);
  font-size: 28px;
  font-variant-numeric: tabular-nums;
}

.sign-in-text p,
.student-status-heading p,
.item-main-content p,
.ann-content-truncated,
.task-desc-truncated {
  color: var(--team-muted);
  line-height: 1.65;
}

.my-stats-panel {
  gap: 18px;
}

.student-status-heading {
  align-items: center;
}

.member-plan-pill,
.seats-count-badge,
.col-meta-pill,
.task-count-indicator,
.status-tag,
.role-badge,
.role-pill {
  border-radius: 999px;
}

.member-plan-pill {
  border: 0;
  background: #fff4e5;
  color: #9a5a14;
}

.member-plan-pill.active {
  background: #e8f8ef;
  color: #18734f;
}

.benefit-lane {
  display: flex;
  flex-direction: column;
  gap: 0;
  border: 1px solid #e5edf6;
  border-radius: 16px;
  overflow: hidden;
}

.benefit-item {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 15px 16px;
  background: #ffffff;
  border: 0;
  border-bottom: 1px solid #e5edf6;
  border-radius: 0;
}

.benefit-item:last-child {
  border-bottom: 0;
}

.benefit-item.enabled {
  background: #f4fbf7;
}

.benefit-item span,
.research-rhythm-panel span,
.tutor-metric-card span,
.card-detail-item span,
.quota-label-line span,
.item-main-content small {
  color: var(--team-muted);
}

.benefit-item strong {
  color: var(--team-ink);
  font-size: 16px;
}

.research-rhythm-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  overflow: visible;
  padding: 0;
  background: transparent;
  border: 0;
}

.research-rhythm-panel > div {
  flex: 1 1 170px;
  min-height: 78px;
  padding: 14px 16px;
  border: 0;
  border-radius: 16px;
  background: #f7faff;
}

.research-rhythm-panel strong,
.tutor-metric-card strong,
.card-detail-item strong {
  color: var(--team-ink);
}

.members-cards-container {
  overflow: hidden;
  background: #ffffff;
  border: 1px solid var(--team-line);
  border-radius: 18px;
  box-shadow: none;
}

.roster-table-head {
  padding: 13px 18px;
  background: #f5f8fc;
  border: 0;
  border-bottom: 1px solid var(--team-line);
  color: var(--team-muted);
}

.member-management-card {
  padding: 16px 18px;
  background: #ffffff;
  border: 0;
  border-bottom: 1px solid #e7edf5;
}

.member-management-card:last-child {
  border-bottom: 0;
}

.member-management-card:hover {
  background: #f8fbff;
}

.card-details-grid {
  padding: 0;
  gap: 10px;
  background: transparent;
  border: 0;
}

.card-detail-item {
  padding: 10px 12px;
  background: #f7faff;
  border: 0;
  border-radius: 12px;
}

.member-benefit-strip {
  gap: 7px;
}

.member-benefit-strip span {
  min-height: 24px;
  border: 0;
  border-radius: 999px;
  background: #edf7f2;
  color: #18734f;
}

.member-benefit-strip span.muted {
  background: #f0f3f7;
  color: var(--team-muted);
}

.summary-metric-cards {
  display: flex;
  flex-direction: column;
  gap: 0;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid var(--team-line);
  border-radius: 18px;
  box-shadow: none;
}

.tutor-metric-card {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: baseline;
  padding: 15px 16px;
  background: #ffffff;
  border: 0;
  border-bottom: 1px solid #e7edf5;
}

.tutor-metric-card:last-child {
  border-bottom: 0;
}

.admin-action-section,
.announcements-briefing,
.active-leaderboard-card {
  overflow: hidden;
}

.admin-items-list,
.announcements-briefing-stack,
.student-task-list,
.student-leaderboard-stack {
  gap: 0;
}

.admin-item-card,
.briefing-ann-card,
.student-task-item,
.resource-card {
  padding: 16px 0;
  background: transparent;
  border: 0;
  border-bottom: 1px solid #e7edf5;
  border-radius: 0;
}

.admin-item-card:last-child,
.briefing-ann-card:last-child,
.student-task-item:last-child,
.resource-card:last-child {
  border-bottom: 0;
}

.item-title-row h5,
.student-task-item h4,
.briefing-ann-card h4 {
  color: var(--team-ink);
  text-decoration: none !important;
}

.tutor-inline-form {
  padding: 16px;
  background: #f7faff;
  border: 0;
  border-radius: 16px;
}

.tutor-inline-form input,
.tutor-inline-form textarea,
.modal-form input,
.modal-form select {
  border-radius: 12px;
  border: 1px solid #d6e0ec;
  background: #ffffff;
}

.btn-toggle-form,
.published-action-btn,
.action-btn-link,
.res-action-btn,
.pager-btn,
.task-countdown-tag {
  box-shadow: none;
}

.action-btn-link,
.published-action-btn {
  color: var(--team-blue);
}

.action-btn-link.danger,
.published-action-btn.danger,
.res-action-btn.delete {
  color: var(--team-red);
}

.image-attachment-card,
.document-file-row,
.document-link-row,
.uploader-dropzone {
  border-radius: 14px;
  border-color: #dce5f0;
  background: #ffffff;
}

.team-resources-section {
  margin-top: 0;
}

.resources-grid {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.resource-card {
  display: flex;
}

.toast {
  border-radius: 12px;
  background: #172033;
  box-shadow: none;
}

/* Header refinement: softer transition and tighter top rhythm */
.team-page {
  padding-top: 14px;
}

.team-shell {
  padding-top: 0;
}

.team-seats-header {
  grid-template-columns: minmax(0, 1fr) 330px;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.96) 0%, rgba(255, 255, 255, 0.96) 66%, rgba(248, 251, 249, 0.98) 100%);
}

.seats-left-container {
  padding-top: 26px;
  padding-bottom: 26px;
}

.seats-right-container {
  justify-content: center;
  gap: 26px;
  padding: 26px 34px 26px 28px;
  background: transparent;
  border-left: 1px solid rgba(217, 227, 238, 0.72);
}

.team-plan-flag {
  padding: 0;
  background: transparent;
  border-radius: 0;
}

.team-identity-plate {
  padding-top: 22px;
  border-top: 1px solid rgba(217, 227, 238, 0.72);
}

@media (max-width: 1120px) {
  .team-seats-header,
  .dashboard-split-layout,
  .dashboard-split-layout.tutor-view,
  .workbench-sign-in {
    display: flex;
    flex-direction: column;
  }

  .dashboard-col.right-col {
    flex: 1 1 auto;
    width: 100%;
  }

  .seats-right-container {
    border-left: 0;
    border-top: 1px solid var(--team-line);
  }
}

@media (max-width: 760px) {
  .team-page {
    padding: 18px 12px 72px;
  }

  .team-seats-header,
  .student-glass-card,
  .admin-action-section,
  .members-cards-container,
  .summary-metric-cards {
    border-radius: 16px;
  }

  .seats-left-container,
  .seats-right-container,
  .student-glass-card,
  .admin-action-section {
    padding: 18px;
  }

  .seats-label-row h2 {
    font-size: 22px;
  }

  .student-status-heading,
  .sign-in-body {
    align-items: flex-start;
    flex-direction: column;
  }
}

/* Team density pass: compact growing lists and make empty states intentional */
.dashboard-split-layout .admin-action-section,
.dashboard-split-layout .tasks-checklist-panel,
.dashboard-split-layout .announcements-briefing {
  min-height: 0;
}

.dashboard-col.right-col .admin-action-section .admin-items-list,
.announcements-briefing .student-task-list,
.tasks-checklist-panel .student-task-list {
  max-height: 560px;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 6px;
  scrollbar-width: thin;
  scrollbar-color: #c9d5e4 transparent;
}

.dashboard-col.right-col .admin-action-section .admin-items-list::-webkit-scrollbar,
.announcements-briefing .student-task-list::-webkit-scrollbar,
.tasks-checklist-panel .student-task-list::-webkit-scrollbar {
  width: 6px;
}

.dashboard-col.right-col .admin-action-section .admin-items-list::-webkit-scrollbar-thumb,
.announcements-briefing .student-task-list::-webkit-scrollbar-thumb,
.tasks-checklist-panel .student-task-list::-webkit-scrollbar-thumb {
  background: #c9d5e4;
  border-radius: 999px;
}

.student-task-item,
.announcement-task-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 18px;
  min-height: 0;
  padding: 14px 0;
}

.announcements-briefing .student-task-item,
.dashboard-col.right-col .admin-item-card {
  grid-template-columns: minmax(0, 1fr);
}

.task-info,
.item-main-content {
  min-width: 0;
}

.task-info {
  display: grid;
  gap: 7px;
}

.task-info h4,
.item-title-row h5 {
  line-height: 1.35;
}

.task-info small,
.item-main-content small {
  color: #8390a3;
  font-size: 11px;
}

.task-desc-truncated {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-height: 1.55;
}

.announcements-briefing .task-desc-truncated {
  -webkit-line-clamp: 3;
}

.student-task-item .task-attachments-list,
.announcement-task-item .task-attachments-list,
.admin-item-card .task-attachments-list {
  display: flex;
  flex-flow: row wrap;
  align-items: center;
  gap: 8px;
  width: 100%;
  margin: 4px 0 0;
}

.student-task-item .attachment-wrapper,
.announcement-task-item .attachment-wrapper,
.admin-item-card .attachment-wrapper {
  display: block;
  width: auto;
  max-width: 100%;
}

.student-task-item .image-attachment-card,
.announcement-task-item .image-attachment-card,
.admin-item-card .image-attachment-card {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  width: min(220px, 100%);
  max-width: 100%;
  min-height: 54px;
  padding: 8px;
  border-radius: 14px;
  background: #f8fbff;
  border: 1px solid #dce5f0;
  box-shadow: none;
}

.student-task-item .image-attachment-card:hover,
.announcement-task-item .image-attachment-card:hover,
.admin-item-card .image-attachment-card:hover {
  transform: none;
  background: #f2f7ff;
}

.student-task-item .task-attachment-img-preview,
.announcement-task-item .task-attachment-img-preview,
.admin-item-card .task-attachment-img-preview {
  width: 46px;
  height: 38px;
  max-height: 38px;
  border-radius: 9px;
  object-fit: cover;
}

.student-task-item .img-name,
.announcement-task-item .img-name,
.admin-item-card .img-name {
  min-width: 0;
  color: #52637a;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.student-task-item .document-file-row,
.announcement-task-item .document-file-row,
.admin-item-card .document-file-row,
.student-task-item .document-link-row,
.announcement-task-item .document-link-row,
.admin-item-card .document-link-row {
  width: auto;
  max-width: min(280px, 100%);
  min-height: 54px;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 14px;
  background: #f8fbff;
  border: 1px solid #dce5f0;
}

.student-task-item .document-file-icon,
.announcement-task-item .document-file-icon,
.admin-item-card .document-file-icon {
  flex: 0 0 38px;
  width: 38px;
  height: 44px;
  font-size: 15px;
}

.student-task-item .document-file-icon.is-pdf,
.announcement-task-item .document-file-icon.is-pdf,
.admin-item-card .document-file-icon.is-pdf {
  font-size: 10px;
}

.student-task-item .document-file-meta,
.announcement-task-item .document-file-meta,
.admin-item-card .document-file-meta {
  gap: 3px;
}

.student-task-item .document-file-meta strong,
.announcement-task-item .document-file-meta strong,
.admin-item-card .document-file-meta strong {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.student-task-item .document-file-meta small,
.announcement-task-item .document-file-meta small,
.admin-item-card .document-file-meta small {
  font-size: 11px;
}

.task-countdown-tag {
  align-self: start;
  margin-top: 2px;
  white-space: nowrap;
}

.empty-state-text {
  min-height: 158px;
  display: grid;
  place-items: center;
  padding: 22px;
  color: #718096;
  font-size: 13px;
  font-weight: 800;
  text-align: center;
  border: 1px dashed #cbd7e6;
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(247, 251, 255, 0.96)),
    radial-gradient(circle at 50% 0%, rgba(43, 104, 255, 0.08), transparent 52%);
}

.admin-items-list .empty-state-text {
  min-height: 132px;
  margin-top: 4px;
}

/* Team flow rewrite: every work module is a full-width horizontal strip */
.dashboard-split-layout,
.dashboard-split-layout.tutor-view,
.dashboard-split-layout.student-view {
  display: flex;
  flex-direction: column;
  gap: 18px;
  align-items: stretch;
  border: 0;
}

.dashboard-col,
.dashboard-col.left-col,
.dashboard-col.right-col {
  width: 100%;
  flex: none;
  min-width: 0;
  padding: 0;
  border: 0;
}

.dashboard-col.left-col,
.dashboard-col.right-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.tutor-view .right-col {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
}

.summary-metric-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  overflow: hidden;
}

.tutor-metric-card {
  border-bottom: 0;
  border-right: 1px solid #e7edf5;
}

.tutor-metric-card:last-child {
  border-right: 0;
}

.admin-action-section,
.student-glass-card.tasks-checklist-panel,
.student-glass-card.announcements-briefing {
  width: 100%;
}

.dashboard-col.right-col .admin-action-section .admin-items-list,
.announcements-briefing .student-task-list,
.tasks-checklist-panel .student-task-list {
  max-height: none;
  overflow: visible;
  padding-right: 0;
}

.announcements-briefing .student-task-item,
.tasks-checklist-panel .student-task-item,
.admin-item-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 24px;
  min-height: 92px;
}

.announcements-briefing .student-task-item .task-info,
.tasks-checklist-panel .student-task-item .task-info,
.admin-item-card .item-main-content {
  max-width: none;
}

.announcements-briefing .task-desc-truncated,
.tasks-checklist-panel .task-desc-truncated,
.admin-item-card .task-desc-truncated {
  -webkit-line-clamp: 2;
  max-width: 78ch;
}

.announcements-briefing .task-attachments-list,
.tasks-checklist-panel .task-attachments-list,
.admin-item-card .task-attachments-list {
  margin-top: 6px;
}

.announcements-briefing .pagination-bar,
.tasks-checklist-panel .pagination-bar,
.admin-action-section .pagination-bar {
  justify-content: flex-end;
  padding-top: 12px;
  margin-top: 4px;
  border-top: 1px solid #e7edf5;
}

.empty-state-text {
  min-height: 118px;
}

@media (max-width: 900px) {
  .summary-metric-cards {
    grid-template-columns: 1fr;
  }

  .tutor-metric-card {
    border-right: 0;
    border-bottom: 1px solid #e7edf5;
  }

  .tutor-metric-card:last-child {
    border-bottom: 0;
  }

  .announcements-briefing .student-task-item,
  .tasks-checklist-panel .student-task-item,
  .admin-item-card {
    grid-template-columns: 1fr;
    align-items: start;
    gap: 10px;
  }
}

</style>
