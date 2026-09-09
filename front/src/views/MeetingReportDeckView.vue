<template>
  <div class="meeting-timeline-page">
    <header class="timeline-header">
      <div>
        <span>组会汇报</span>
        <h1>按时间沉淀每一次组会材料</h1>
        <p>每张卡片是一场组会：选择汇报文献、补充重点内容、生成论文综述，并把 PPT 任务留在后台执行。</p>
      </div>
      <button type="button" class="add-meeting-button" @click="triggerNewMeetingPicker">
        <span aria-hidden="true">+</span>
        添加组会
      </button>
    </header>

    <!-- Stats Bar -->
    <div class="timeline-stats-bar">
      <div class="stat-card">
        <span class="homepage-icon-box box-blue">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
            <line x1="16" y1="2" x2="16" y2="6"/>
            <line x1="8" y1="2" x2="8" y2="6"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
            <path d="M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01M16 18h.01"/>
          </svg>
        </span>
        <div class="stat-info">
          <strong>{{ meetingStats.total }} 场</strong>
          <small>沉淀组会</small>
        </div>
      </div>

      <div class="stat-card">
        <span class="homepage-icon-box box-purple">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
            <line x1="9" y1="7" x2="15" y2="7"/>
            <line x1="9" y1="11" x2="13" y2="11"/>
          </svg>
        </span>
        <div class="stat-info">
          <strong>{{ meetingStats.totalPapers }} 篇</strong>
          <small>关联文献</small>
        </div>
      </div>

      <div class="stat-card">
        <span class="homepage-icon-box box-emerald">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="3" width="20" height="14" rx="3"/>
            <line x1="8" y1="21" x2="16" y2="21"/>
            <line x1="12" y1="17" x2="12" y2="21"/>
            <path d="M7 12l3-3 3 3 4-4"/>
          </svg>
        </span>
        <div class="stat-info">
          <strong>{{ meetingStats.pptsCompleted }} 份</strong>
          <small>PPT 就绪</small>
        </div>
      </div>

      <div class="stat-card">
        <span class="homepage-icon-box box-amber">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="3" width="20" height="14" rx="3"/>
            <line x1="8" y1="21" x2="16" y2="21"/>
            <line x1="12" y1="17" x2="12" y2="21"/>
            <path d="M9 10l2 2 4-4"/>
          </svg>
        </span>
        <div class="stat-info">
          <strong>{{ meetingStats.advisorTasks }} 项</strong>
          <small>导师意见记录</small>
        </div>
      </div>
    </div>


    <main class="timeline-shell">
      <div v-if="sortedMeetings.length === 0" class="timeline-empty-state">
        <div class="empty-icon-box">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="empty-svg">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
            <path d="M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01M16 18h.01"></path>
          </svg>
        </div>
        <h2>开启您的第一次学术组会</h2>
        <p>这里将按时间沉淀每一次汇报的文献和综述，让您的汇报准备井井有条。</p>
        <button type="button" class="btn-empty-add" @click="triggerNewMeetingPicker">
          <span>+</span> 创建新组会安排
        </button>
      </div>

      <section v-else class="timeline-list" aria-label="组会时间轴">
        <article
          v-for="(meeting, index) in sortedMeetings"
          :key="meeting.id"
          class="meeting-card"
          :class="[`tone-${index % 4}`, { active: activeMeetingId === meeting.id }]"
        >
          <div class="timeline-pin" aria-hidden="true">
            <span></span>
          </div>

          <div class="meeting-card-head">
            <div class="meeting-date-block-premium">
              <div class="date-info-group">
                <div class="calendar-icon-wrapper">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" class="calendar-svg">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                    <line x1="16" y1="2" x2="16" y2="6"></line>
                    <line x1="8" y1="2" x2="8" y2="6"></line>
                    <line x1="3" y1="10" x2="21" y2="10"></line>
                  </svg>
                </div>
                <div class="date-text-wrapper">
                  <span class="date-main-label">组会日期</span>
                  <span class="date-sub-value">{{ formatMeetingDateOnly(meeting.meetingTime) }}</span>
                </div>
              </div>

              <div class="time-divider" aria-hidden="true"></div>

              <div class="time-info-group">
                <span class="time-clock-value">{{ getMeetingHourMin(meeting.meetingTime) }}</span>
                <span class="countdown-badge" :class="meetingCountdownClass(meeting.meetingTime)">
                  {{ meetingCountdownText(meeting.meetingTime) }}
                </span>
              </div>

              <div class="picker-trigger-wrapper" @click="triggerDatePicker($event)">
                <button type="button" class="btn-time-edit" aria-label="调整组会时间">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 20h9"></path>
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
                  </svg>
                  <span>调整</span>
                </button>
                <input
                  v-model="meeting.meetingTime"
                  type="datetime-local"
                  class="hidden-datetime-picker"
                  aria-label="选择组会时间"
                  @change="persistMeetings"
                />
              </div>
            </div>
            <div class="meeting-head-actions">
              <div class="meeting-status" :data-status="meetingStatus(meeting)">
                <span></span>
                {{ statusText(meeting) }}
              </div>
              <button type="button" class="delete-meeting-button" title="删除组会" @click="removeMeeting(meeting.id)">
                删除
              </button>
            </div>
          </div>

          <div class="meeting-card-body">
            <section class="meeting-main">
              <input
                v-model="meeting.title"
                class="meeting-title-input"
                placeholder="例如：欧盟 AI 法案合规自动化组会汇报"
                @change="persistMeetings"
              />

              <label class="meeting-notes" :class="{ importing: importingReview && reviewModal.meeting?.id === meeting.id }">
                <span>组会重点内容</span>
                <textarea
                  v-model="meeting.notes"
                  placeholder="记录这次要讲清楚的核心问题、导师可能追问的点、需要讨论的实验或方法缺口。"
                  @change="persistMeetings"
                ></textarea>
              </label>

              <div class="meeting-detail-grid" aria-label="组会汇报细节">
                <label
                  v-for="field in meetingDetailFields"
                  :key="field.key"
                  class="meeting-detail-field"
                  :class="{ importing: importingReview && reviewModal.meeting?.id === meeting.id && (field.key === 'objective' || field.key === 'questions') }"
                >
                  <span>{{ field.label }}</span>
                  <textarea
                    v-model="meeting.details[field.key]"
                    :placeholder="field.placeholder"
                    @change="persistMeetings"
                  ></textarea>
                </label>
              </div>

              <label class="meeting-advisor-note">
                <span>导师建议修改</span>
                <textarea
                  v-model="meeting.advisorAdvice"
                  placeholder="记录导师提出的修改意见，例如补实验、换图表、重写研究问题、增加对照或调整汇报顺序。"
                  @change="persistMeetings"
                ></textarea>
              </label>
            </section>

            <aside class="meeting-side">
              <div class="paper-box">
                <div class="box-title">
                  <span>组会汇报文献</span>
                  <button type="button" @click="openPaperPicker(meeting)">点击添加</button>
                </div>
                <div v-if="meeting.papers.length" class="selected-papers">
                  <article
                    v-for="paperId in meeting.papers"
                    :key="paperId"
                    class="selected-paper"
                    @click="handlePaperCardClick(meeting, paperById(paperId))"
                  >
                    <div>
                      <strong>{{ paperTitle(paperId) }}</strong>
                      <small>{{ paperMeta(paperId) }}</small>
                    </div>
                    <div class="selected-paper-tags">
                      <span
                        class="paper-status-badge review-badge"
                        :class="[reviewPaperStatus(paperId)]"
                      >
                        {{ reviewPaperStatusText(paperId) }}
                      </span>
                      <span
                        class="paper-status-badge note-badge"
                        :class="[paperNoteStatus(paperId)]"
                      >
                        {{ paperNoteLabel(paperId) }}
                      </span>
                    </div>
                    <div class="paper-review-progress" aria-hidden="true">
                      <i :style="{ width: `${paperReviewPercent(paperId)}%` }"></i>
                    </div>
                  </article>
                </div>
                <p v-else>从已导入论文中选择，也可以直接上传 PDF。</p>
              </div>

              <!-- PPT Generation (full width) -->
              <div class="generation-grid single">
                <div class="generation-action" :data-status="deckActionStatus(meeting)">
                  <div class="progress-label">
                    <span>PPT 生成</span>
                    <strong>{{ deckProgressLabel(meeting) }}</strong>
                  </div>
                  <p class="generation-step" :title="deckStepText(meeting)">{{ deckStepText(meeting) }}</p>
                  <div class="generation-progress ppt-progress" aria-hidden="true">
                    <i :style="{ width: `${deckPercent(meeting)}%` }"></i>
                  </div>
                  <p class="deck-paper-scope">{{ deckPaperScope(meeting) }}</p>
                  <div class="deck-action-row">
                    <button
                      v-if="deckJobs[meeting.id]?.previewUrls?.length"
                      type="button"
                      class="preview-deck-button"
                      @click="openDeckPreview(meeting)"
                    >
                      预览 PPT
                    </button>
                    <button
                      v-if="deckJobs[meeting.id]?.confirmUrl && !deckJobs[meeting.id]?.downloadUrl && Number(deckJobs[meeting.id]?.progress || 0) <= 28"
                      type="button"
                      class="confirm-link-button"
                      @click="openDeckConfirmPage(deckJobs[meeting.id].confirmUrl)"
                    >
                      打开参数页
                    </button>
                    <a
                      v-if="deckJobs[meeting.id]?.downloadUrl"
                      class="download-button"
                      :href="absoluteApiUrl(deckJobs[meeting.id].downloadUrl)"
                      target="_blank"
                      rel="noreferrer"
                    >
                      下载 PPT
                    </a>
                    <button
                      v-if="deckJobs[meeting.id]?.downloadUrl && canSaveDeckToDesktop()"
                      type="button"
                      class="local-save-button"
                      :disabled="savingLocalDecks.has(meeting.id) || Boolean(deckJobs[meeting.id]?.localPath)"
                      @click="saveDeckToDesktop(meeting)"
                    >
                      {{ deckJobs[meeting.id]?.localPath ? "已保存本机" : savingLocalDecks.has(meeting.id) ? "保存中" : "保存到本机" }}
                    </button>
                    <button
                      v-if="deckJobs[meeting.id]?.downloadUrl"
                      type="button"
                      class="regenerate-deck-button"
                      :disabled="!canMakePpt(meeting) || isDeckBusy(meeting)"
                      @click="confirmMakePpt(meeting)"
                    >
                      重新生成
                    </button>
                    <button
                      v-if="!deckJobs[meeting.id]?.downloadUrl"
                      type="button"
                      class="primary-button"
                      :disabled="isDeckBusy(meeting)"
                      @click="confirmMakePpt(meeting)"
                    >
                      {{ deckJobs[meeting.id]?.status === "failed" ? "重新生成 PPT" : isDeckBusy(meeting) ? "执行中" : "生成汇报 PPT" }}
                    </button>
                  </div>
                </div>
              </div>
            </aside>
          </div>
        </article>
      </section>
    </main>

    <Transition name="modal-fade">
      <div v-if="paperPicker.open" class="modal-backdrop" @click.self="closePaperPicker">
        <section class="paper-picker modal-panel" aria-label="选择组会文献">
          <header>
            <div>
              <span>添加汇报文献</span>
              <h2>{{ paperPicker.meeting?.title || "选择论文" }}</h2>
              <p>可选择 1-3 篇文献共同准备组会汇报，PPT 会按所选文献合并生成。</p>
            </div>
            <div class="picker-header-actions">
              <strong>{{ selectedPickerCount }}/{{ MAX_MEETING_PAPERS }}</strong>
              <small>已选文献</small>
            </div>
            <button type="button" aria-label="关闭" @click="closePaperPicker">×</button>
          </header>

          <div class="picker-tools">
            <input v-model="keyword" type="search" placeholder="搜索标题、作者或年份" />
            <label class="upload-inline" :class="{ busy: uploading }">
              <input type="file" accept="application/pdf,.pdf" class="hidden-file-input" :disabled="uploading" @change="uploadPaper" />
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
              <span>{{ uploading ? "上传中..." : "上传 PDF" }}</span>
            </label>
          </div>

          <div v-if="loadingPapers" class="paper-loading">
            <span v-for="item in 4" :key="item"></span>
          </div>
          <div v-else class="picker-paper-list">
            <button
              v-for="paper in filteredPapers"
              :key="paper.workspaceId"
              type="button"
              class="picker-paper"
              :class="{
                selected: paperPicker.meeting?.papers.includes(paper.workspaceId),
                disabled: !paperPicker.meeting?.papers.includes(paper.workspaceId) && selectedPickerCount >= MAX_MEETING_PAPERS
              }"
              @click="toggleMeetingPaper(paperPicker.meeting, paper)"
            >
              <div class="picker-paper-top">
                <span :class="['pdf-badge', hasPdf(paper) ? 'ready' : 'missing']">
                  {{ hasPdf(paper) ? "PDF" : "待补 PDF" }}
                </span>
                <span v-if="paperPicker.meeting?.papers.includes(paper.workspaceId)" class="selected-check">已选</span>
              </div>
              <strong>{{ paper.title || "未命名论文" }}</strong>
              <small>{{ compactMeta(paper) }}</small>
            </button>
          </div>
        </section>
      </div>
    </Transition>

    <Transition name="modal-fade">
      <div v-if="reviewModal.open" class="modal-backdrop" @click.self="closeReview">
        <section class="review-modal modal-panel" aria-label="组会论文综述">
          <header>
            <div>
              <span>{{ reviewModal.generated ? "已保存综述" : "组会汇报综述" }}</span>
              <h2>{{ reviewModal.paper?.title || "论文综述" }}</h2>
            </div>
            <button type="button" aria-label="关闭" @click="closeReview">×</button>
          </header>

          <div v-if="reviewModal.loading" class="review-loading">
            <strong>{{ reviewModal.message || "正在读取综述" }}</strong>
            <div class="wide-progress">
              <i :style="{ width: `${Math.max(8, reviewModal.progress)}%` }"></i>
            </div>
          </div>

          <template v-else>
            <div class="review-modal-actions">
              <div class="modal-tab-group">
                <button
                  type="button"
                  class="btn-tab"
                  :class="{ active: reviewModal.activeTab === 'review' }"
                  @click="reviewModal.activeTab = 'review'"
                >
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
                  <span>组会综述</span>
                </button>
                <button
                  type="button"
                  class="btn-tab btn-tab-ai"
                  :class="{ active: reviewModal.activeTab === 'note' }"
                  @click="reviewModal.activeTab = 'note'"
                >
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                  <span>Ai笔记</span>
                </button>
              </div>

              <div class="modal-right-actions">
                <button
                  v-if="reviewModal.activeTab === 'review' && reviewModal.generated"
                  type="button"
                  class="btn-action btn-save"
                  :disabled="reviewModal.saving"
                  @click="saveReview"
                >
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
                  <span>{{ reviewModal.saving ? "保存中" : "保存编辑" }}</span>
                </button>
              </div>
            </div>

            <!-- TAB 1: 组会综述列表 -->
            <div v-if="reviewModal.activeTab === 'review' && reviewModal.generated" class="review-point-list">
              <section v-for="section in reviewSections" :key="section.key" class="review-point">
                <div class="review-point-meta">
                  <strong>{{ section.title }}</strong>
                  <small>{{ section.hint }}</small>
                  <div class="review-point-badge">
                    <span class="dot"></span>
                    <span>已精读整理</span>
                  </div>
                </div>
                <button
                  type="button"
                  class="copy-section-button"
                  :title="`复制${section.title}`"
                  @click="copyReviewSection(section)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <rect x="8" y="8" width="10" height="10" rx="2"></rect>
                    <path d="M6 16H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                  </svg>
                </button>
                <div
                  class="review-rich-editor"
                  contenteditable="true"
                  role="textbox"
                  spellcheck="false"
                  :aria-label="section.title"
                  :data-placeholder="section.placeholder"
                  v-html="highlightedReviewHtml(reviewModal.sections[section.key])"
                  @input="updateReviewSection(section.key, $event)"
                  @blur="formatReviewSection(section.key)"
                  @paste="pastePlainReviewText"
                ></div>
              </section>
            </div>

            <div v-else-if="reviewModal.activeTab === 'review'" class="review-empty-state">
              <div class="empty-icon-wrap">R</div>
              <strong>尚未生成论文综述</strong>
              <p>组会汇报这里只查看已保存内容。请进入文献阅读页面，在对应文献中生成综述后再回来查看。</p>
            </div>

            <!-- TAB 2: AI笔记 Markdown 渲染查看 -->
            <div v-else class="review-note-markdown-view">
              <div v-if="reviewModal.paper?.note" class="rendered-note-container">
                <div class="rendered-note-content markdown-rendered" v-html="renderMarkdown(reviewModal.paper.note)"></div>
              </div>
              <div v-else class="review-note-empty">
                <div class="empty-icon-wrap">📝</div>
                <strong>暂无 Ai 笔记</strong>
                <p>当前论文尚未保存深度精读笔记，请先在文献阅读页面完成 AI 笔记后再回来查看。</p>
              </div>
            </div>
          </template>
        </section>
      </div>
    </Transition>

    <Transition name="modal-fade">
      <div v-if="deckPreview.open" class="modal-backdrop deck-preview-backdrop" @click.self="closeDeckPreview">
        <section class="deck-preview-modal" aria-label="PPT 预览">
          <header>
            <div>
              <span>PPT MASTER 预览</span>
              <h2>{{ deckPreview.title }}</h2>
            </div>
            <div class="deck-preview-counter">{{ deckPreview.index + 1 }} / {{ deckPreview.urls.length }}</div>
            <button type="button" class="deck-preview-close" aria-label="关闭预览" @click="closeDeckPreview">×</button>
          </header>
          <div v-if="deckPreview.loading" class="deck-preview-loading">正在载入最终版页面预览...</div>
          <div v-else class="deck-preview-stage">
            <button type="button" class="deck-preview-nav previous" aria-label="上一页" :disabled="deckPreview.index <= 0" @click="deckPreview.index--">‹</button>
            <img :src="deckPreview.urls[deckPreview.index]" :alt="`第 ${deckPreview.index + 1} 页预览`" />
            <button type="button" class="deck-preview-nav next" aria-label="下一页" :disabled="deckPreview.index >= deckPreview.urls.length - 1" @click="deckPreview.index++">›</button>
          </div>
          <div v-if="!deckPreview.loading" class="deck-preview-thumbnails">
            <button v-for="(url, index) in deckPreview.urls" :key="url" type="button" :class="{ active: index === deckPreview.index }" @click="deckPreview.index = index">
              <img :src="url" :alt="`第 ${index + 1} 页`" />
              <span>{{ index + 1 }}</span>
            </button>
          </div>
        </section>
      </div>
    </Transition>

    <Transition name="modal-fade">
      <div v-if="newMeetingModal.open" class="modal-backdrop picker-modal-backdrop" @click.self="newMeetingModal.open = false">
        <div class="custom-picker-card">
          <div class="picker-card-header">
            <h3>新建组会安排</h3>
            <p>请设置组会的召开日期与具体时间</p>
          </div>
          
          <div class="picker-card-body">
            <div class="picker-field-group">
              <label for="new-meeting-date">选择日期</label>
              <div class="input-with-icon">
                <span class="icon">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                </span>
                <input
                  id="new-meeting-date"
                  v-model="newMeetingModal.date"
                  type="date"
                  required
                />
              </div>
            </div>
            
            <div class="picker-field-group">
              <label for="new-meeting-time">选择时间</label>
              <div class="input-with-icon">
                <span class="icon">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                </span>
                <input
                  id="new-meeting-time"
                  v-model="newMeetingModal.time"
                  type="time"
                  required
                />
              </div>
            </div>
          </div>
          
          <div class="picker-card-footer">
            <button type="button" class="btn-picker-cancel" @click="newMeetingModal.open = false">取消</button>
            <button type="button" class="btn-picker-confirm" @click="confirmNewMeeting">确定</button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="modal-fade">
      <div v-if="pptParamsModal.open" class="modal-mask" @click="pptParamsModal.open = false">
        <div class="modal-box ppt-params-modal-box" @click.stop>
          <div class="modal-head">
            <div class="modal-head-titles">
              <span class="modal-badge-tag">PPT 智能生成</span>
              <h3>组会 PPT 参数与风格配置</h3>
            </div>
            <button type="button" class="modal-close-btn" @click="pptParamsModal.open = false">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M18 6 6 18M6 6l12 12"/></svg>
            </button>
          </div>

          <div class="ppt-params-body">
            <div class="ppt-param-field">
              <label class="ppt-param-label">
                <span>幻灯片目标页数</span>
                <small>根据组会汇报时长选择合适容量</small>
              </label>
              <div class="ppt-radio-group">
                <button
                  v-for="opt in slideCountOptions"
                  :key="opt.value"
                  type="button"
                  :class="['ppt-radio-btn', { active: pptParamsModal.slideCount === opt.value }]"
                  @click="pptParamsModal.slideCount = opt.value"
                >
                  <strong>{{ opt.label }}</strong>
                  <span>{{ opt.desc }}</span>
                </button>
              </div>
            </div>

            <div class="ppt-param-field">
              <label class="ppt-param-label">
                <span>汇报受众与侧重点</span>
                <small>调整表达深度与核心逻辑</small>
              </label>
              <div class="ppt-radio-group">
                <button
                  v-for="opt in audienceOptions"
                  :key="opt.value"
                  type="button"
                  :class="['ppt-radio-btn', { active: pptParamsModal.audience === opt.value }]"
                  @click="pptParamsModal.audience = opt.value"
                >
                  <strong>{{ opt.label }}</strong>
                  <span>{{ opt.desc }}</span>
                </button>
              </div>
            </div>

            <div class="ppt-param-field">
              <label class="ppt-param-label">
                <span>视觉设计风格</span>
                <small>专业配色与排版版式</small>
              </label>
              <div class="ppt-radio-group">
                <button
                  v-for="opt in visualStyleOptions"
                  :key="opt.value"
                  type="button"
                  :class="['ppt-radio-btn', { active: pptParamsModal.visualStyle === opt.value }]"
                  @click="pptParamsModal.visualStyle = opt.value"
                >
                  <strong>{{ opt.label }}</strong>
                  <span>{{ opt.desc }}</span>
                </button>
              </div>
            </div>

            <div class="ppt-param-field-checkbox">
              <label class="ppt-checkbox-label">
                <input type="checkbox" v-model="pptParamsModal.includeComparisonAppendix" />
                <span>生成文献横向对比与导师追问讨论页</span>
              </label>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="pptParamsModal.open = false">取消</button>
            <button type="button" class="btn-confirm-start" @click="startPptWithParams">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="5 3 19 12 5 21 5 3"/></svg>
              <span>立即开始生成 PPT</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="toast-slide">
      <div v-if="toastMessage" class="meeting-toast">{{ toastMessage }}</div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import MarkdownIt from "markdown-it";
import { paperpilotApi } from "../services/paperpilotApi";
import { API_BASE_URL } from "../services/apiClient";
import { useDialogStore } from "../stores/dialog";
import { useLibraryStore } from "../stores/library";

const markdownRenderer = new MarkdownIt({ html: false, linkify: true, breaks: true });
const libraryStore = useLibraryStore();
const reviewNoteImageUrls = reactive({});
const reviewNoteImageLoading = new Set();

function normalizeNotesMarkdownTables(markdown) {
  if (!markdown) return "";
  const lines = String(markdown).split(/\r?\n/);
  const result = [];
  let inTable = false;
  let tableRows = [];

  const splitRowCells = (text) => {
    const trimmed = text.trim();
    if (trimmed.includes("|")) {
      return trimmed.replace(/^\|/, "").replace(/\|$/, "").split("|").map(c => c.trim()).filter(Boolean);
    }
    return trimmed.split(/\s{2,}|\t+/).map(c => c.trim()).filter(Boolean);
  };

  const isDividerText = (text) => {
    const trimmed = text.trim();
    if (!trimmed.includes("-")) return false;
    return /^\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)+\|?$/.test(trimmed) || /^:?-{2,}:?(\s+:?-{2,}:?)+$/.test(trimmed);
  };

  const isRowCandidate = (text) => {
    const trimmed = text.trim();
    if (!trimmed || trimmed.startsWith("#") || trimmed.startsWith(">") || trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
      return false;
    }
    if (trimmed.includes("|") && trimmed.split("|").length >= 3) return true;
    const cells = splitRowCells(trimmed);
    return cells.length >= 2 && !trimmed.endsWith("。") && !trimmed.endsWith("！") && !trimmed.endsWith("；");
  };

  const flushTable = () => {
    if (!tableRows.length) return;
    const validDataRows = tableRows.filter(r => !r.isDivider);
    if (!validDataRows.length) {
      tableRows = [];
      return;
    }
    if (validDataRows.length === 1 && tableRows.length === 1) {
      result.push(tableRows[0].raw);
      tableRows = [];
      return;
    }

    const headerCells = validDataRows[0].cells;
    const colCount = Math.max(2, ...validDataRows.map(r => r.cells.length));
    
    while (headerCells.length < colCount) headerCells.push(`列 ${headerCells.length + 1}`);
    
    if (result.length && result[result.length - 1] !== "") {
      result.push("");
    }
    result.push(`| ${headerCells.map(c => c.replace(/\|/g, "/")).join(" | ")} |`);
    result.push(`| ${Array.from({ length: colCount }, () => ":---").join(" | ")} |`);

    for (let i = 1; i < validDataRows.length; i++) {
      const rowCells = [...validDataRows[i].cells];
      while (rowCells.length < colCount) rowCells.push("-");
      result.push(`| ${rowCells.slice(0, colCount).map(c => c.replace(/\|/g, "/")).join(" | ")} |`);
    }
    result.push("");
    tableRows = [];
  };

  for (let i = 0; i < lines.length; i++) {
    const raw = lines[i];
    const trimmed = raw.trim();

    if (isDividerText(trimmed)) {
      if (!inTable) {
        if (result.length && isRowCandidate(result[result.length - 1])) {
          const prevRaw = result.pop();
          inTable = true;
          tableRows.push({ raw: prevRaw, cells: splitRowCells(prevRaw), isDivider: false });
        }
      }
      if (inTable) {
        tableRows.push({ raw, cells: [], isDivider: true });
        continue;
      }
    }

    if (inTable) {
      if (isRowCandidate(raw)) {
        tableRows.push({ raw, cells: splitRowCells(raw), isDivider: false });
      } else {
        flushTable();
        inTable = false;
        result.push(raw);
      }
    } else {
      const nextLine = i + 1 < lines.length ? lines[i + 1].trim() : "";
      if (isRowCandidate(raw) && (isDividerText(nextLine) || (isRowCandidate(nextLine) && splitRowCells(raw).length >= 2 && splitRowCells(nextLine).length >= 2))) {
        inTable = true;
        tableRows.push({ raw, cells: splitRowCells(raw), isDivider: false });
      } else {
        result.push(raw);
      }
    }
  }

  if (inTable) {
    flushTable();
  }
  return result.join("\n");
}

async function hydrateReviewNoteImages(markdown, workspaceId) {
  if (!workspaceId) return;
  const matches = String(markdown || "").match(/mineru:\/\/[^)\s]+/g) || [];
  for (const token of new Set(matches)) {
    if (reviewNoteImageUrls[token] || reviewNoteImageLoading.has(token)) continue;
    reviewNoteImageLoading.add(token);
    try {
      const assetPath = decodeURIComponent(token.slice("mineru://".length));
      if (assetPath.startsWith("http://") || assetPath.startsWith("https://") || assetPath.startsWith("blob:")) {
        reviewNoteImageUrls[token] = assetPath;
      } else {
        const blob = await paperpilotApi.getMineruAsset(assetPath, workspaceId);
        const objectUrl = URL.createObjectURL(blob);
        reviewNoteImageUrls[token] = objectUrl;
      }
    } catch (err) {
      console.warn("Failed to load note figure asset:", token, err);
    } finally {
      reviewNoteImageLoading.delete(token);
    }
  }
}

function renderMarkdown(text) {
  if (!text) return "";
  const normalized = normalizeNotesMarkdownTables(String(text || "")
    .replace(/^[ \t]+(?=\|.*\|\s*$)/gm, "")
    .replace(/^[ \t]+(?=\|\s*:?-{3,}.*\|\s*$)/gm, ""));
  
  if (reviewModal.paper?.workspaceId) {
    void hydrateReviewNoteImages(normalized, reviewModal.paper.workspaceId);
  }
  const withImages = normalized.replace(/mineru:\/\/[^)\s]+/g, token => reviewNoteImageUrls[token] || token);
  return markdownRenderer.render(withImages.trim());
}

const STORAGE_KEY = "paperpilot-meeting-timeline-v1";
const DECK_STORAGE_KEY = "paperpilot-meeting-deck-jobs-v1";
const REVIEW_STORAGE_KEY = "paperpilot-meeting-review-jobs-v1";
const PPT_MASTER_CONFIRM_PREFIX = "papersolver-ppt-master-confirm:";
const DEFAULT_MEETING_TITLE = "新组会汇报";
const MAX_MEETING_PAPERS = 3;
const MAX_DECK_SLIDES = 10;
const DEFAULT_DECK_SLIDE_COUNT = "10";
const DEFAULT_MEETING_NOTES = "本次重点：先讲清研究问题，再讨论方法路线、证据质量和后续可推进方向。";
const DEFAULT_MEETING_DETAILS = {
  objective: "",
  questions: "",
  evidence: "",
  discussion: "",
};
const meetingDetailFields = [
  { key: "objective", label: "汇报目标", placeholder: "这次汇报希望导师/组员重点判断什么？例如选题是否成立、方法是否可行。" },
  { key: "questions", label: "关键问题", placeholder: "列出 2-3 个必须讲清的问题，避免 PPT 只复述论文。" },
  { key: "evidence", label: "实验与数据", placeholder: "需要展示的数据、实验设置、指标、图表页码或待补的证据。" },
  { key: "discussion", label: "待讨论决策", placeholder: "本次组会需要拍板的下一步：补实验、改方向、换数据、投稿策略等。" },
];

const reviewSections = [
  { key: "basicInfo", title: "基本信息", hint: "题录、来源与研究对象", placeholder: "作者、年份、期刊/会议、研究对象、数据来源。" },
  { key: "overview", title: "研究问题", hint: "为什么要做", placeholder: "背景痛点、研究缺口、本文要回答的问题。" },
  { key: "background", title: "理论背景", hint: "相关工作与概念框架", placeholder: "关键概念、相关理论、与既有工作的关系。" },
  { key: "method", title: "方法路线", hint: "模型、框架或实验路径", placeholder: "输入、方法模块、实验流程、变量/指标设置。" },
  { key: "results", title: "结果证据", hint: "主要发现与支撑证据", placeholder: "核心结果、对比、消融、统计或案例证据。" },
  { key: "datasets", title: "数据与评测", hint: "数据来源、设置和指标", placeholder: "样本、数据集、划分、评价指标、可复现性。" },
  { key: "conclusion", title: "贡献与局限", hint: "组会讨论点", placeholder: "贡献、边界、局限、后续研究方向。" },
];

const meetings = ref([]);
const newMeetingModal = reactive({
  open: false,
  date: "",
  time: "",
});
const papers = ref([]);
const keyword = ref("");
const loadingPapers = ref(false);
const uploading = ref(false);
const activeMeetingId = ref("");
const toastMessage = ref("");
const importingReview = ref(false);
const dialogStore = useDialogStore();
const router = useRouter();
const deckJobs = reactive({});
const reviewJobs = reactive({});
const importJobs = reactive({});
const savingLocalDecks = reactive(new Set());
const deckPreview = reactive({ open: false, title: "", urls: [], index: 0, loading: false });
const paperPicker = reactive({ open: false, meeting: null });
const reviewModal = reactive({
  open: false,
  activeTab: "review", // 'review' | 'note'
  meeting: null,
  paper: null,
  sections: emptySections(),
  generated: false,
  loading: false,
  generating: false,
  saving: false,
  progress: 0,
  message: "",
  modelName: "",
});

let toastTimer = null;
const deckTimers = new Map();
const reviewTimers = new Map();
const confirmOpened = ref("");
let pendingConfirmWindow = null;
let pendingPptMasterConfirmation = null;

const meetingStats = computed(() => {
  const total = meetings.value.length;
  const totalPapers = new Set(meetings.value.flatMap(m => m.papers || [])).size;
  const advisorTasks = meetings.value.filter(m => String(m.advisorAdvice || "").trim()).length;
  const pptsCompleted = meetings.value.filter(m => deckJobs[m.id]?.downloadUrl).length;
  return { total, totalPapers, advisorTasks, pptsCompleted };
});

const sortedMeetings = computed(() => [...meetings.value].sort((a, b) => new Date(b.meetingTime) - new Date(a.meetingTime)));
const selectedPickerCount = computed(() => paperPicker.meeting?.papers?.length || 0);
const canImportReviewToMeeting = computed(() => Boolean(
  reviewModal.meeting && reviewSections.some(section => String(reviewModal.sections?.[section.key] || "").trim()),
));

const filteredPapers = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  if (!query) return papers.value;
  return papers.value.filter((paper) => [
    paper.title,
    paper.authors,
    paper.publishYear,
    paper.source,
    paper.note,
  ].some((value) => String(value || "").toLowerCase().includes(query)));
});

watch(meetings, persistMeetings, { deep: true });
watch(() => ({ ...deckJobs }), persistDeckJobs, { deep: true });
watch(() => ({ ...reviewJobs }), persistReviewJobs, { deep: true });

onMounted(async () => {
  window.addEventListener("message", handlePptMasterConfirmation);
  loadPersistedState();
  await loadPapers();
  ensureFirstMeeting();
  cleanupAutoSeededMeetings();
  resumeDeckJobs();
  resumeReviewJobs();
});

onBeforeUnmount(() => {
  closeDeckPreview();
  window.removeEventListener("message", handlePptMasterConfirmation);
  if (toastTimer) clearTimeout(toastTimer);
  deckTimers.forEach((timer) => window.clearTimeout(timer));
  reviewTimers.forEach((timer) => window.clearTimeout(timer));
});

function emptySections() {
  return Object.fromEntries(reviewSections.map((section) => [section.key, ""]));
}

function loadPersistedState() {
  try {
    const savedMeetings = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
    if (Array.isArray(savedMeetings)) meetings.value = savedMeetings.map(normalizeMeeting);
  } catch {
    meetings.value = [];
  }
  try {
    Object.assign(deckJobs, JSON.parse(localStorage.getItem(DECK_STORAGE_KEY) || "{}"));
  } catch {
    Object.keys(deckJobs).forEach((key) => delete deckJobs[key]);
  }
  try {
    Object.assign(reviewJobs, JSON.parse(localStorage.getItem(REVIEW_STORAGE_KEY) || "{}"));
  } catch {
    Object.keys(reviewJobs).forEach((key) => delete reviewJobs[key]);
  }
}

async function loadPapers() {
  loadingPapers.value = true;
  try {
    papers.value = await paperpilotApi.getLibraryPapers();
  } catch (error) {
    showToast(error?.response?.data?.message || "论文列表加载失败");
  } finally {
    loadingPapers.value = false;
  }
}

function ensureFirstMeeting() {
  if (meetings.value.length) return;
  meetings.value = [createMeeting()];
  activeMeetingId.value = meetings.value[0].id;
}

function createMeeting(paperIds = []) {
  const now = new Date();
  now.setMinutes(Math.ceil(now.getMinutes() / 15) * 15, 0, 0);
  return {
    id: `meeting-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    meetingTime: toDatetimeLocal(now),
    title: DEFAULT_MEETING_TITLE,
    notes: DEFAULT_MEETING_NOTES,
    details: { ...DEFAULT_MEETING_DETAILS },
    advisorAdvice: "",
    tags: ["待汇报"],
    tagDraft: "",
	    params: {
	      reportType: "paper",
	      audience: "导师与课题组",
	      slideCount: DEFAULT_DECK_SLIDE_COUNT,
	    },
    papers: paperIds,
    primaryPaperId: paperIds[0] || "",
  };
}

function normalizeMeeting(meeting = {}) {
  const paperIds = Array.isArray(meeting.papers) ? meeting.papers.filter(Boolean) : [];
  return {
    id: meeting.id || `meeting-${Date.now()}`,
    meetingTime: meeting.meetingTime || toDatetimeLocal(new Date()),
    title: meeting.title || DEFAULT_MEETING_TITLE,
    notes: meeting.notes || "",
    details: normalizeMeetingDetails(meeting.details),
    advisorAdvice: meeting.advisorAdvice || "",
    tags: Array.isArray(meeting.tags) ? meeting.tags : [],
    tagDraft: "",
	    params: {
	      reportType: meeting.params?.reportType || "paper",
	      audience: meeting.params?.audience || "导师与课题组",
	      slideCount: normalizeDeckSlideCount(meeting.params?.slideCount),
	    },
    papers: paperIds,
    primaryPaperId: meeting.primaryPaperId || paperIds[0] || "",
  };
}

function normalizeMeetingDetails(details = {}) {
  return {
    ...DEFAULT_MEETING_DETAILS,
    ...(details && typeof details === "object" ? details : {}),
  };
}

function meetingFocusText(meeting) {
  const lines = [
    ["组会重点", meeting.notes],
    ...meetingDetailFields.map(field => [field.label, meeting.details?.[field.key]]),
    ["导师建议修改", meeting.advisorAdvice],
  ]
    .map(([label, value]) => `${label}：${String(value || "").trim()}`)
    .filter(line => !/：$/.test(line));
  return lines.join("\n");
}

function compactReviewText(value = "", maxLength = 220) {
  const text = String(value || "")
    .replace(/\s+/g, " ")
    .replace(/^[·•\-\d.\s]+/g, "")
    .trim();
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}

function joinReviewParts(parts, maxLength = 360, itemLength = 150) {
  const text = parts.map(item => compactReviewText(item, itemLength)).filter(Boolean).join("\n");
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}

function formatMeetingBullets(items, maxItems = 3, itemLength = 130) {
  return items
    .map(item => compactReviewText(item, itemLength))
    .filter(Boolean)
    .slice(0, maxItems)
    .map((item, index) => `${index + 1}. ${item}`)
    .join("\n");
}

function triggerNewMeetingPicker() {
  const now = new Date();
  newMeetingModal.date = now.toISOString().slice(0, 10);
  // Round minutes to next 15-minute mark
  const rawMins = now.getMinutes();
  let roundedMins = Math.ceil(rawMins / 15) * 15;
  let hours = now.getHours();
  if (roundedMins >= 60) {
    roundedMins = 0;
    hours = (hours + 1) % 24;
  }
  newMeetingModal.time = `${String(hours).padStart(2, "0")}:${String(roundedMins).padStart(2, "0")}`;
  newMeetingModal.open = true;
}

function confirmNewMeeting() {
  if (!newMeetingModal.date || !newMeetingModal.time) {
    showToast("请先选择日期和时间");
    return;
  }
  
  const selectedTime = `${newMeetingModal.date}T${newMeetingModal.time}`;
  
  const meeting = createMeeting();
  meeting.meetingTime = selectedTime;
  meetings.value.unshift(meeting);
  activeMeetingId.value = meeting.id;
  persistMeetings();
  
  newMeetingModal.open = false;
  showToast("已成功添加组会，请选择本次汇报文献");
}

function cleanupAutoSeededMeetings() {
  let changed = false;
  meetings.value = meetings.value.map((meeting) => {
    if (!isAutoSeededMeeting(meeting)) return meeting;
    changed = true;
    return {
      ...meeting,
      papers: [],
      primaryPaperId: "",
    };
  });
  if (changed) persistMeetings();
}

function isAutoSeededMeeting(meeting) {
  if (!meeting || meeting.papers?.length !== 1) return false;
  const deck = deckJobs[meeting.id];
  if (deck?.jobId || deck?.downloadUrl || deck?.status === "generated" || deck?.status === "running") return false;
  return (meeting.title || "") === DEFAULT_MEETING_TITLE && (meeting.notes || "") === DEFAULT_MEETING_NOTES;
}

async function removeMeeting(meetingId) {
  const meeting = meetings.value.find((item) => item.id === meetingId);
  if (!meeting) return;
  const ok = await dialogStore.confirm(`删除「${meeting.title || "组会汇报"}」？`, {
    title: "删除组会",
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!ok) return;
  meetings.value = meetings.value.filter((item) => item.id !== meetingId);
  delete deckJobs[meetingId];
  if (deckTimers.has(meetingId)) {
    window.clearTimeout(deckTimers.get(meetingId));
    deckTimers.delete(meetingId);
  }
  activeMeetingId.value = meetings.value[0]?.id || "";
  persistMeetings();
  persistDeckJobs();
  showToast("已删除组会");
}

function persistMeetings() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(meetings.value.map(({ tagDraft, ...meeting }) => meeting)));
}

function persistDeckJobs() {
  localStorage.setItem(DECK_STORAGE_KEY, JSON.stringify(deckJobs));
}

function persistReviewJobs() {
  localStorage.setItem(REVIEW_STORAGE_KEY, JSON.stringify(reviewJobs));
}

function formatMeetingDate(value) {
  if (!value) return "未定时间";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "未定时间";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function formatMeetingDateOnly(value) {
  if (!value) return "未定日期";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "未定日期";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "long",
    day: "numeric",
  }).format(date);
}

function getMonthName(value) {
  if (!value) return "07";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "07";
  return `${date.getMonth() + 1}月`;
}

function getDayNum(value) {
  if (!value) return "08";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "08";
  return String(date.getDate()).padStart(2, "0");
}

function getMeetingHourMin(value) {
  if (!value) return "23:00";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "23:00";
  return `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function meetingCountdownText(value) {
  if (!value) return "待排期";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "待排期";
  const diff = date.getTime() - Date.now();
  if (diff < 0) {
    if (diff > -10800000) return "进行中";
    return "已召开";
  }
  const mins = Math.round(diff / 60000);
  if (mins < 60) return `即将开始 (${mins}m)`;
  const hours = Math.round(diff / 3600000);
  if (hours < 24) return `今天 (${hours}h后)`;
  const days = Math.ceil(diff / 86400000);
  return `${days}天后`;
}

function meetingCountdownClass(value) {
  if (!value) return "status-draft";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "status-draft";
  const diff = date.getTime() - Date.now();
  if (diff < 0) {
    if (diff > -10800000) return "status-active";
    return "status-past";
  }
  const mins = Math.round(diff / 60000);
  if (mins < 60) return "status-soon";
  return "status-future";
}

function triggerDatePicker(event) {
  const parent = event.currentTarget.closest(".picker-trigger-wrapper");
  const input = parent ? parent.querySelector(".hidden-datetime-picker") : null;
  if (input) {
    if (typeof input.showPicker === "function") {
      try {
        input.showPicker();
      } catch (err) {
        input.click();
      }
    } else {
      input.click();
    }
  }
}

function toDatetimeLocal(date) {
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 16);
}

function openPaperPicker(meeting) {
  paperPicker.open = true;
  paperPicker.meeting = meeting;
  activeMeetingId.value = meeting.id;
}

function closePaperPicker() {
  paperPicker.open = false;
  paperPicker.meeting = null;
}

function toggleMeetingPaper(meeting, paper) {
  if (!meeting || !paper?.workspaceId) return;
  const exists = meeting.papers.includes(paper.workspaceId);
  if (!exists && meeting.papers.length >= MAX_MEETING_PAPERS) {
    showToast(`一场组会最多选择 ${MAX_MEETING_PAPERS} 篇文献`);
    return;
  }
  meeting.papers = exists
    ? meeting.papers.filter((id) => id !== paper.workspaceId)
    : [paper.workspaceId, ...meeting.papers];
  if (!meeting.papers.includes(meeting.primaryPaperId)) {
    meeting.primaryPaperId = meeting.papers[0] || "";
  }
  persistMeetings();
}

async function uploadPaper(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file || uploading.value) return;
  uploading.value = true;
  try {
    const paper = await paperpilotApi.uploadLibraryPaper(file);
    papers.value = [paper, ...papers.value.filter((item) => item.workspaceId !== paper.workspaceId)];
    if (paperPicker.meeting) {
      if (paperPicker.meeting.papers.includes(paper.workspaceId) || paperPicker.meeting.papers.length < MAX_MEETING_PAPERS) {
        paperPicker.meeting.papers = [paper.workspaceId, ...paperPicker.meeting.papers.filter((id) => id !== paper.workspaceId)].slice(0, MAX_MEETING_PAPERS);
        paperPicker.meeting.primaryPaperId = paper.workspaceId;
        persistMeetings();
        showToast("论文已上传并加入组会文献");
      } else {
        showToast(`论文已上传；本场组会已选满 ${MAX_MEETING_PAPERS} 篇`);
      }
    }
  } catch (error) {
    showToast(error?.response?.data?.message || "论文上传失败");
  } finally {
    uploading.value = false;
  }
}

function setPrimaryPaper(meeting, paperId) {
  meeting.primaryPaperId = paperId;
  activeMeetingId.value = meeting.id;
  persistMeetings();
}

function primaryPaper(meeting) {
  const paperId = meeting?.primaryPaperId || meeting?.papers?.[0];
  return papers.value.find((paper) => paper.workspaceId === paperId) || null;
}

function paperById(paperId) {
  return papers.value.find((paper) => paper.workspaceId === paperId) || null;
}

function selectedMeetingPapers(meeting) {
  return (meeting?.papers || []).map(paperById).filter(Boolean);
}

function paperTitle(paperId) {
  return papers.value.find((paper) => paper.workspaceId === paperId)?.title || "未命名论文";
}

function paperMeta(paperId) {
  const paper = papers.value.find((item) => item.workspaceId === paperId);
  return paper ? compactMeta(paper) : "论文信息待同步";
}

function compactMeta(paper) {
  return [paper?.authors || "作者待补全", paper?.publishYear || "年份未知", paper?.source || "来源未记录"]
    .filter(Boolean)
    .join(" · ");
}

function hasPdf(paper) {
  if (!paper) return false;
  const candidates = [
    paper.paperUrl,
    paper.pdfUrl,
    paper.sourceUrl,
    paper.paperPath
  ].map(u => String(u || "").trim()).filter(Boolean);

  for (const url of candidates) {
    if (paperpilotApi.isLikelyPdfUrl(url)) return true;
    if (url.startsWith("blob:") || url.startsWith("data:") || url.includes("/api/papers/uploads/")) return true;
    if (url.toLowerCase().startsWith("desktop-cache://")) return true;
  }

  // Cross check with library store documents
  const doc = libraryStore.state.documents.find(d => 
    (paper.workspaceId && d.workspaceId === paper.workspaceId) ||
    (paper.id && (d.id === paper.id || d.workspaceId === paper.id)) ||
    (paper.title && d.title === paper.title)
  );
  if (doc) {
    const docCandidates = [doc.paperUrl, doc.pdfUrl, doc.sourceUrl, doc.paperPath].map(u => String(u || "").trim()).filter(Boolean);
    for (const url of docCandidates) {
      if (paperpilotApi.isLikelyPdfUrl(url)) return true;
      if (url.startsWith("blob:") || url.startsWith("data:") || url.includes("/api/papers/uploads/")) return true;
      if (url.toLowerCase().startsWith("desktop-cache://")) return true;
    }
    if (doc.hasPdf || doc.pdfUploaded || doc.paperPath) return true;
  }

  if (paper.workspaceId && (paper.hasPdf || paper.pdfUploaded || paper.paperPath)) return true;
  return false;
}

function missingPdfCount(meeting) {
  return selectedMeetingPapers(meeting).filter(paper => !hasPdf(paper)).length;
}

function canMakePpt(meeting) {
  const selected = selectedMeetingPapers(meeting);
  return Boolean(selected.length);
}

// Paper note helpers
function paperNoteStatus(paperId) {
  const paper = paperById(paperId);
  if (!paper) return "note-none";
  const combined = paper.note || "";
  const parts = combined.split("=== PAPERSOLVER_NOTE_SEPARATOR ===");
  const hasAi = parts[0] && parts[0].trim().length > 5;
  const hasManual = parts[1] && parts[1].trim().length > 0;
  if (hasAi && hasManual) return "note-both";
  if (hasAi) return "note-ai";
  if (hasManual) return "note-manual";
  return "note-none";
}

function paperNoteLabel(paperId) {
  const status = paperNoteStatus(paperId);
  if (status === "note-none") return "待生成笔记";
  return "已生成笔记";
}

function paperNoteTitle(paperId) {
  const status = paperNoteStatus(paperId);
  if (status === "note-none") return "尚未记录笔记，点击【笔记】前往文献库";
  return "点击【笔记】前往文献库查看或编辑笔记";
}

function openPaperNoteInLibrary(paperId) {
  // Navigate to library with the paper highlighted for note editing
  const paper = paperById(paperId);
  if (!paper) { showToast("找不到对应论文"); return; }
  router.push({ path: "/library", query: { noteId: paperId } });
}

const pptParamsModal = reactive({
  open: false,
  meeting: null,
  slideCount: "8-10",
  audience: "导师与课题组",
  visualStyle: "academic_blue",
  includeComparisonAppendix: true,
});

const slideCountOptions = [
  { value: "5-8", label: "5 - 8 页", desc: "精简汇报 / 约 5-8 分钟" },
  { value: "8-10", label: "8 - 10 页", desc: "标准组会 / 经典推荐 (约 10-15 分钟)" },
];

const audienceOptions = [
  { value: "导师与课题组", label: "导师与课题组", desc: "强调方法创新点、实验证据链与后续计划" },
  { value: "学术会议同行", label: "学术会议同行", desc: "突出技术贡献、横向基线对比与严谨公式" },
  { value: "跨领域通识", label: "跨领域通识", desc: "强化背景引入、研究直观动机与通俗图解" },
];

const visualStyleOptions = [
  { value: "academic_blue", label: "学术经典蓝", desc: "严谨双栏白底，商务学术蓝" },
  { value: "dark_tech", label: "沉浸暗夜科技", desc: "深蓝黑科技底，高对比度荧光" },
  { value: "journal_minimal", label: "极简期刊灰", desc: "高雅学术期刊配色，克制沉稳" },
];

function confirmMakePpt(meeting) {
  const selected = selectedMeetingPapers(meeting);
  if (!selected.length) {
    showToast("请先添加组会汇报文献");
    return;
  }
  if (missingPdfCount(meeting)) {
    showToast("请先补齐本次组会文献的 PDF");
    return;
  }
  const bridgeKey = `${meeting.id}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
  const storageKey = `${PPT_MASTER_CONFIRM_PREFIX}${bridgeKey}:recommendations`;
  try {
    const recommendations = buildPptMasterRecommendations(meeting);
    localStorage.setItem(storageKey, JSON.stringify(recommendations));
    const baseUrl = new URL(window.location.href);
    baseUrl.hash = "";
    baseUrl.search = "";
    const confirmUrl = new URL("./ppt-master-confirm/index.html", baseUrl);
    confirmUrl.searchParams.set("bridge", bridgeKey);
    confirmUrl.searchParams.set("recommendations", utf8ToBase64(JSON.stringify(recommendations)));
    const popup = window.open(confirmUrl.toString(), "_blank");
    if (!popup) throw new Error("popup-blocked");
    pendingPptMasterConfirmation = { bridgeKey, meetingId: meeting.id, popup };
    showToast("请在 PPT Master 参数页确认生成设置");
  } catch (error) {
    localStorage.removeItem(storageKey);
    pptParamsModal.meeting = meeting;
    pptParamsModal.slideCount = normalizeDeckSlideCount(meeting.params?.slideCount || DEFAULT_DECK_SLIDE_COUNT);
    pptParamsModal.audience = meeting.params?.audience || "导师与课题组";
    pptParamsModal.visualStyle = meeting.params?.visualStyle || "academic_blue";
    pptParamsModal.includeComparisonAppendix = meeting.params?.includeComparisonAppendix !== false;
    pptParamsModal.open = true;
    showToast("参数页被系统拦截，已切换到应用内确认");
  }
}

function utf8ToBase64(value) {
  const bytes = new TextEncoder().encode(String(value || ""));
  let binary = "";
  bytes.forEach(byte => { binary += String.fromCharCode(byte); });
  return window.btoa(binary);
}

function buildPptMasterRecommendations(meeting) {
  const params = meeting.params || {};
  const saved = params.pptMasterConfirm || {};
  const styleMap = {
    academic_blue: "editorial",
    dark_tech: "dark-tech",
    journal_minimal: "swiss-minimal",
  };
  return {
    lang: "zh",
    recommend: {
      canvas: saved.canvas || "ppt169",
      mode: saved.mode || "pyramid",
      visual_style: saved.visual_style || styleMap[params.visualStyle] || "editorial",
      icons: saved.icons || "tabler-outline",
      image_usage: saved.image_usage || ["provided"],
      image_ai_path: saved.image_ai_path || "auto",
      formula_policy: saved.formula_policy || "mixed",
      generation_mode: saved.generation_mode || "continuous",
      delivery_purpose: saved.delivery_purpose || "balanced",
    },
    page_count: { value: saved.page_count || normalizeDeckSlideCount(params.slideCount || DEFAULT_DECK_SLIDE_COUNT) },
    audience: { value: saved.audience || params.audience || "导师与课题组" },
    content_divergence: { value: saved.content_divergence || "忠实论文事实，按组会汇报逻辑重组问题、方法、证据与结论。" },
    image_notes: { value: saved.image_notes || "优先引用论文中的真实图表与示意图；仅在缺少必要视觉解释时补充生成图。" },
    color: {
      selected: 0,
      candidates: [
        { name: "学术经典蓝", note: "白底、深蓝主色与克制金色强调", palette: { background: "#FFFFFF", secondary_bg: "#F3F6FA", primary: "#173B67", accent: "#D39B32", secondary_accent: "#4D7FAF", body_text: "#1E293B" } },
        { name: "极简期刊灰", note: "低饱和灰阶与冷青强调，适合高密度论文内容", palette: { background: "#FFFFFF", secondary_bg: "#F4F4F2", primary: "#252A31", accent: "#2E7D78", secondary_accent: "#77818D", body_text: "#20242A" } },
        { name: "沉浸暗夜科技", note: "深色背景与高对比青紫强调", palette: { background: "#0C111B", secondary_bg: "#151D2C", primary: "#E8EDF7", accent: "#55D6BE", secondary_accent: "#8A7CFF", body_text: "#D7DFEC" } },
      ],
    },
    typography: {
      selected: 0,
      candidates: [
        { name: "现代学术", note: "清晰、稳健，适合中文组会", sample_heading: "研究问题与核心贡献", sample_heading_latin: "Research Question & Contribution", sample_body: "从方法、证据与局限展开论证", sample_body_latin: "Methods, evidence, and limitations", heading: { cjk: "思源黑体", latin: "Inter", css: "'Source Han Sans SC','PingFang SC','Inter',sans-serif" }, body: { cjk: "思源黑体", latin: "Inter", css: "'Source Han Sans SC','PingFang SC','Inter',sans-serif" }, body_size: 24 },
        { name: "期刊编辑", note: "标题更具论文质感，正文保持易读", sample_heading: "实验设计与关键发现", sample_heading_latin: "Experimental Design & Findings", sample_body: "结合原文图表解释证据链", sample_body_latin: "Explain the evidence with paper figures", heading: { cjk: "思源宋体", latin: "Source Serif 4", css: "'Source Han Serif SC','Songti SC','Source Serif 4',serif" }, body: { cjk: "思源黑体", latin: "Inter", css: "'Source Han Sans SC','PingFang SC','Inter',sans-serif" }, body_size: 24 },
        { name: "演讲展示", note: "更大的字号与更短的信息块", sample_heading: "为什么这项研究重要", sample_heading_latin: "Why This Study Matters", sample_body: "每页聚焦一个可讲清的结论", sample_body_latin: "One clear conclusion per slide", heading: { cjk: "阿里巴巴普惠体", latin: "Aptos Display", css: "'Alibaba PuHuiTi','PingFang SC','Aptos Display',sans-serif" }, body: { cjk: "阿里巴巴普惠体", latin: "Aptos", css: "'Alibaba PuHuiTi','PingFang SC','Aptos',sans-serif" }, body_size: 28 },
      ],
    },
    image_strategy: {
      selected: 0,
      candidates: [
        { name: "论文证据优先", rendering: "editorial-diagram", palette: "academic", visual: "以论文原图和数据图表为主", color: "沿用整套 PPT 配色", mood: "严谨、可信" },
        { name: "机制图解", rendering: "vector-illustration", palette: "cool-corporate", visual: "用简洁矢量图补充机制解释", color: "低饱和冷色", mood: "清晰、理性" },
        { name: "场景化叙事", rendering: "editorial-collage", palette: "restrained-vivid", visual: "在背景和讨论页加入编辑式视觉", color: "主色稳定、强调色点题", mood: "有节奏、易讲述" },
      ],
    },
    refine_spec: { value: false },
  };
}

function handlePptMasterConfirmation(event) {
  const detail = event?.data;
  if (detail?.type !== "papersolver:ppt-master-confirmed") return;
  if (!pendingPptMasterConfirmation || detail.bridgeKey !== pendingPptMasterConfirmation.bridgeKey) return;
  const meeting = meetings.value.find(item => item.id === pendingPptMasterConfirmation.meetingId);
  if (!meeting) return;
  const confirmed = detail.payload || {};
  meeting.params = {
    ...(meeting.params || {}),
    slideCount: normalizeDeckSlideCount(confirmed.page_count || meeting.params?.slideCount),
    audience: confirmed.audience || meeting.params?.audience || "导师与课题组",
    visualStyle: confirmed.visual_style || meeting.params?.visualStyle || "editorial",
    pptMasterConfirm: confirmed,
  };
  persistMeetings();
  localStorage.removeItem(`${PPT_MASTER_CONFIRM_PREFIX}${detail.bridgeKey}:recommendations`);
  localStorage.removeItem(`${PPT_MASTER_CONFIRM_PREFIX}${detail.bridgeKey}:result`);
  pendingPptMasterConfirmation = null;
  makePpt(meeting);
}

function startPptWithParams() {
  if (!pptParamsModal.meeting) return;
  const meeting = pptParamsModal.meeting;
  meeting.params = {
    ...(meeting.params || {}),
    slideCount: normalizeDeckSlideCount(pptParamsModal.slideCount),
    audience: pptParamsModal.audience,
    visualStyle: pptParamsModal.visualStyle,
    includeComparisonAppendix: pptParamsModal.includeComparisonAppendix,
  };
  persistMeetings();
  pptParamsModal.open = false;
  makePpt(meeting);
}

function meetingStatus(meeting) {
  const deck = deckJobs[meeting.id];
  const reviews = selectedMeetingPapers(meeting).map(paper => reviewJobs[paper.workspaceId]).filter(Boolean);
  if (isDeckRunning(deck) || reviews.some(job => job?.status === "running")) return "running";
  if (deck?.downloadUrl || deck?.status === "generated") return "ready";
  if (reviews.some(job => job?.status === "generated")) return "ready";
  if (deck?.status === "failed") return "failed";
  if (reviews.some(job => job?.status === "failed")) return "failed";
  return "idle";
}

function statusText(meeting) {
  const status = meetingStatus(meeting);
  if (status === "ready") return "已生成";
  if (status === "running") return "执行中";
  if (status === "failed") return "失败";
  return "未开始";
}

function reviewPercent(meeting) {
  const selected = selectedMeetingPapers(meeting);
  if (!selected.length) return 0;
  const total = selected.reduce((sum, paper) => {
    const job = reviewJobs[paper.workspaceId];
    if (job?.status === "generated") return sum + 100;
    if (job?.status === "failed") return sum + 100;
    return sum + Number(job?.progress || 0);
  }, 0);
  return Math.round(total / selected.length);
}

function paperReviewPercent(paperId) {
  const job = reviewJobs[paperId];
  if (job?.status === "generated" || job?.status === "failed") return 100;
  return Math.round(job?.progress || 0);
}

function deckPercent(meeting) {
  const job = deckJobs[meeting.id];
  if (job?.status === "failed") return 100;
  if (job?.downloadUrl || job?.status === "generated") return 100;
  return Math.round(job?.progress || 0);
}

function deckProgressLabel(meeting) {
  const job = deckJobs[meeting.id];
  if (job?.status === "failed") return "失败";
  return `${deckPercent(meeting)}%`;
}

function isReviewBusy(meeting) {
  return selectedMeetingPapers(meeting).some(paper => reviewJobs[paper.workspaceId]?.status === "running");
}

function isDeckBusy(meeting) {
  return isDeckRunning(deckJobs[meeting.id]);
}

function isImportBusy(meeting) {
  return importJobs[meeting.id]?.status === "running";
}

function isDeckRunning(job) {
  return job?.status === "running" || job?.status === "awaiting_agent";
}

function reviewActionStatus(meeting) {
  const selected = selectedMeetingPapers(meeting);
  if (!selected.length) return "idle";
  const jobs = selected.map(paper => reviewJobs[paper.workspaceId]).filter(Boolean);
  if (jobs.some(job => job.status === "running")) return "running";
  if (jobs.some(job => job.status === "failed")) return "failed";
  if (jobs.length && selected.every(paper => reviewJobs[paper.workspaceId]?.status === "generated")) return "generated";
  if (jobs.some(job => job.status === "generated")) return "generated";
  return "idle";
}

function deckActionStatus(meeting) {
  const job = deckJobs[meeting.id];
  if (job?.downloadUrl) return "generated";
  return job?.status || "idle";
}

function importActionStatus(meeting) {
  return importJobs[meeting.id]?.status || "idle";
}

function importPercent(meeting) {
  const job = importJobs[meeting.id];
  if (job?.status === "generated" || job?.status === "failed") return 100;
  return Math.round(job?.progress || 0);
}

function importProgressLabel(meeting) {
  const job = importJobs[meeting.id];
  if (job?.status === "failed") return "失败";
  return `${importPercent(meeting)}%`;
}

function reviewStepText(meeting) {
  const selected = selectedMeetingPapers(meeting);
  if (!selected.length) return "先添加汇报文献";
  const generated = selected.filter(paper => reviewJobs[paper.workspaceId]?.status === "generated").length;
  const running = selected.find(paper => reviewJobs[paper.workspaceId]?.status === "running");
  const failed = selected.filter(paper => reviewJobs[paper.workspaceId]?.status === "failed").length;
  if (running) return reviewJobs[running.workspaceId]?.message || `正在生成 ${paperTitle(running.workspaceId)} 的综述`;
  if (failed) return `${failed} 篇综述失败，可逐篇重试`;
  if (generated === selected.length) return `${generated}/${selected.length} 篇综述已保存`;
  if (generated) return `${generated}/${selected.length} 篇综述已保存，剩余可逐篇生成`;
  return `${selected.length} 篇文献，综述需逐篇生成`;
}

function deckStepText(meeting) {
  const job = deckJobs[meeting.id];
  const selected = selectedMeetingPapers(meeting);
  if (!selected.length) return "先添加汇报文献";
  const missing = missingPdfCount(meeting);
  if (missing) return `${missing} 篇文献缺少 PDF，补齐后可合并生成`;
  if (!job) return "等待启动 PPT 任务";
  if (job.status === "generated") return "已生成，可下载";
  if (job.status === "failed") return publicDeckText(job.message || "生成失败");
  if (job.confirmUrl && Number(job.progress || 0) <= 28) return "参数页已弹出，请在新窗口确认生成参数";
  return [job.stage, job.message].map(publicDeckText).filter(Boolean).join(" · ") || "后台生成中";
}

function importStepText(meeting) {
  const job = importJobs[meeting.id];
  if (job?.message) return job.message;
  const selected = selectedMeetingPapers(meeting);
  if (!selected.length) return "先添加汇报文献";
  const generated = selected.filter(paper => reviewJobs[paper.workspaceId]?.status === "generated").length;
  if (!generated) return "先逐篇生成至少一篇论文综述";
  if (selected.length === 1) return "将单篇综述整理为组会字段";
  return `将融合 ${generated}/${selected.length} 篇已保存综述`;
}

function canImportMeetingReviews(meeting) {
  if (!selectedMeetingPapers(meeting).length) return false;
  if (isImportBusy(meeting)) return false;
  return true;
}

function deckPaperScope(meeting) {
  const count = selectedMeetingPapers(meeting).length;
  if (!count) return "未选择汇报文献";
  return count === 1 ? "单篇文献生成 1 个 PPT" : `${count} 篇文献合并生成 1 个 PPT`;
}

function reviewPaperStatus(paperId) {
  return reviewJobs[paperId]?.status || "idle";
}

function reviewPaperStatusText(paperId) {
  const status = reviewPaperStatus(paperId);
  if (status === "generated") return "已综述";
  if (status === "running") return "生成中";
  if (status === "failed") return "失败";
  return "待综述";
}

function handlePaperCardClick(meeting, paper) {
  if (!paper) {
    showToast("请先添加组会汇报文献");
    return;
  }
  openReview(meeting, paper);
}

async function openReview(meeting, targetPaper = null) {
  const paper = targetPaper || primaryPaper(meeting);
  if (!paper) {
    showToast("请先添加组会汇报文献");
    return;
  }
  meeting.primaryPaperId = paper.workspaceId;
  activeMeetingId.value = meeting.id;
  persistMeetings();
  Object.assign(reviewModal, {
    open: true,
    meeting,
    paper,
    sections: emptySections(),
    generated: false,
    loading: true,
    generating: false,
    saving: false,
    progress: reviewJobs[paper.workspaceId]?.progress || 10,
    message: "正在读取已保存综述",
    modelName: "",
  });
  try {
    try {
      const libPaper = await paperpilotApi.getLibraryPaper(paper.workspaceId);
      if (libPaper?.note) {
        paper.note = libPaper.note;
        reviewModal.paper.note = libPaper.note;
      }
    } catch {
      // Fallback to existing paper object
    }
    const data = await paperpilotApi.getMeetingReport(paper.workspaceId);
    applyReviewData(data);
    if (reviewModal.generated) {
      reviewJobs[paper.workspaceId] = { status: "generated", progress: 100, message: "已读取历史综述" };
      persistReviewJobs();
    }
  } catch (error) {
    showToast(error?.response?.data?.message || "综述读取失败");
  } finally {
    reviewModal.loading = false;
  }
}

function closeReview() {
  reviewModal.open = false;
}

function updatePaperNoteText(event) {
  if (reviewModal.paper) {
    reviewModal.paper.note = event.target.innerText;
  }
}

function goToReader(paper) {
  if (!paper?.workspaceId) return;
  router.push({ path: "/reader", query: { id: paper.workspaceId } });
}

async function generateReview() {
  const paper = reviewModal.paper;
  if (!paper?.workspaceId || reviewModal.generating) return;
  reviewModal.generating = true;
  reviewJobs[paper.workspaceId] = { status: "running", progress: 1, message: "提交生成任务" };
  persistReviewJobs();
  try {
    const result = await paperpilotApi.generateMeetingReport(paper.workspaceId);
    if (result?.status === "completed" || result?.generated) {
      applyReviewData(result);
      reviewJobs[paper.workspaceId] = { status: "generated", progress: 100, message: "已生成" };
      showToast("论文综述已生成并永久保存");
    } else {
      pollReview(paper.workspaceId);
    }
  } catch (error) {
    reviewJobs[paper.workspaceId] = { status: "failed", progress: 0, message: error?.response?.data?.message || "论文综述生成失败" };
    reviewModal.generating = false;
    showToast(reviewJobs[paper.workspaceId].message);
  }
}

function pollReview(workspaceId) {
  if (reviewTimers.has(workspaceId)) window.clearTimeout(reviewTimers.get(workspaceId));
  const timer = window.setTimeout(async () => {
    try {
      const status = await paperpilotApi.getMeetingReportGenerateStatus(workspaceId);
      reviewJobs[workspaceId] = {
        status: status.done ? (status.success ? "generated" : "failed") : "running",
        progress: status.success ? 100 : (status.progress || 0),
        message: status.message || "",
      };
      if (reviewModal.paper?.workspaceId === workspaceId) {
        reviewModal.progress = reviewJobs[workspaceId].progress;
        reviewModal.message = reviewJobs[workspaceId].message;
      }
      if (status.done) {
        reviewTimers.delete(workspaceId);
        reviewModal.generating = false;
        if (status.success) {
          if (reviewModal.paper?.workspaceId === workspaceId) {
            applyReviewData(await paperpilotApi.getMeetingReport(workspaceId));
          }
          showToast("论文综述已生成并永久保存");
        } else {
          showToast(status.message || "论文综述生成失败");
        }
        return;
      }
      pollReview(workspaceId);
    } catch (error) {
      reviewJobs[workspaceId] = { status: "failed", progress: reviewJobs[workspaceId]?.progress || 0, message: error?.response?.data?.message || "综述状态刷新失败" };
      reviewModal.generating = false;
      showToast(reviewJobs[workspaceId].message);
    }
  }, 1400);
  reviewTimers.set(workspaceId, timer);
}

async function saveReview() {
  const paper = reviewModal.paper;
  if (!paper?.workspaceId || reviewModal.saving) return;
  reviewModal.saving = true;
  try {
    const data = await paperpilotApi.saveMeetingReport(paper.workspaceId, {
      sections: reviewModal.sections,
      modelName: reviewModal.modelName || "人工编辑",
    });
    // Save paper note
    if (paper.note !== undefined) {
      await paperpilotApi.updateLibraryPaper(paper.workspaceId, {
        note: paper.note
      });
      // Synchronize in local list if paperById exists
      const localPaper = paperById(paper.workspaceId);
      if (localPaper) localPaper.note = paper.note;
    }
    applyReviewData(data);
    reviewJobs[paper.workspaceId] = { status: "generated", progress: 100, message: "已保存" };
    showToast("综述与笔记已成功保存");
  } catch (error) {
    showToast("保存失败：" + (error?.response?.data?.message || error?.message || "综述保存失败"));
  } finally {
    reviewModal.saving = false;
  }
}

function applyReviewData(data = {}) {
  const sections = formatReviewSections({ ...emptySections(), ...(data.sections || {}) });
  const hasSavedReview = Boolean(data.generated)
    || reviewSections.some(section => hasUsefulSection(sections[section.key]));
  reviewModal.sections = sections;
  
  const paperInfo = data.paper || reviewModal.paper || {};
  if (reviewModal.paper) {
    if (data.paper?.note) {
      reviewModal.paper.note = data.paper.note;
    } else if (data.note) {
      reviewModal.paper.note = data.note;
    }
  }
  if (!hasUsefulSection(reviewModal.sections.basicInfo)) {
    reviewModal.sections.basicInfo = buildBasicInfoFallback(paperInfo);
  }
  if (!hasUsefulSection(reviewModal.sections.background)) {
    reviewModal.sections.background = buildBackgroundFallback(reviewModal.sections, paperInfo);
  }
  if (!hasUsefulSection(reviewModal.sections.conclusion)) {
    reviewModal.sections.conclusion = buildConclusionFallback(reviewModal.sections, paperInfo);
  }
  if (!hasUsefulSection(reviewModal.sections.datasets)) {
    reviewModal.sections.datasets = buildDatasetsFallback(reviewModal.sections, paperInfo);
  }
  
  // Older saved reports may contain sections but lack the generated flag.
  reviewModal.generated = hasSavedReview;
  reviewModal.modelName = data.modelName || "";
  reviewModal.progress = hasSavedReview ? 100 : reviewModal.progress;
  reviewModal.message = hasSavedReview ? "已读取历史保存的论文综述" : "尚未生成";
  nextTick(resizeAllReviewTextareas);
}

function hasUsefulSection(value = "") {
  const text = String(value || "").replace(/\s+/g, "");
  if (text.length < 12) return false;
  return !/^(作者、年份、期刊\/会议、研究对象、数据来源。?|作者年份期刊会议研究对象数据来源|贡献、边界、局限、后续研究方向。?|关键概念、相关理论、与既有工作的关系。?)$/.test(text);
}

function splitIntoSentences(text = "") {
  return String(text || "")
    .split(/[。！；？!\n;]/)
    .map(s => s.trim())
    .filter(s => s.length > 5 && !s.includes("生成状态") && !s.includes("未完成") && !s.includes("接口返回") && !s.includes("保存可编辑"));
}

function buildBasicInfoFallback(paper = {}) {
  return formatReviewParagraphs([
    `论文定位：${paper.title || "当前论文"} 可作为本次组会的主论文材料，用于讨论研究问题、方法路线和证据链条。`,
    `发表信息：作者为 ${paper.authors || "作者信息未补全"}；来源为 ${paper.source || "来源未记录"}；年份为 ${paper.publishYear || "年份未知"}。`,
    `汇报价值：适合从研究对象、问题动机、方法设计、结果证据和局限边界几个角度组织汇报。`,
  ].join("\n\n"));
}

function buildBackgroundFallback(sections, paper = {}) {
  const sourceText = [sections.synthesis, sections.overview, paper.abstractText].filter(Boolean).join("\n");
  const sentences = splitIntoSentences(sourceText);
  
  const currentStatus = sentences.filter(s => /现状|目前|已有|传统|通常|近年来/i.test(s)).slice(0, 2);
  const keyGap = sentences.filter(s => /挑战|限制|不足|瓶颈|然而|但是|面临/i.test(s)).slice(0, 2);
  const objective = sentences.filter(s => /旨在|提出|设计|目的|目标|研究/i.test(s)).slice(0, 2);
  
  const blocks = [];
  if (currentStatus.length) blocks.push(`研究背景：${currentStatus.join("；")}。`);
  if (keyGap.length) blocks.push(`现有不足：${keyGap.join("；")}。`);
  if (objective.length) blocks.push(`研究问题：${objective.join("；")}。`);
  
  if (!blocks.length && sentences.length) {
    blocks.push(`研究背景：${sentences.slice(0, 3).join("；")}。`);
  } else if (!blocks.length) {
    blocks.push(`研究背景：本研究围绕题目所涉核心科学问题展开。主要探讨对应背景及当前领域面临的性能/合规痛点。`);
  }
  
  return formatReviewParagraphs(blocks.join("\n\n"));
}

function buildConclusionFallback(sections, paper = {}) {
  const sourceText = [sections.synthesis, sections.results, sections.overview].filter(Boolean).join("\n");
  const sentences = splitIntoSentences(sourceText);
  
  const innovation = sentences.filter(s => /创新|贡献|首次|提出|价值|意义/i.test(s)).slice(0, 2);
  const limit = sentences.filter(s => /局限|不足|限制|未来|展望|有待/i.test(s)).slice(0, 2);
  
  const blocks = [];
  if (innovation.length) blocks.push(`主要贡献：${innovation.join("；")}。`);
  if (limit.length) blocks.push(`局限性：${limit.join("；")}。`);
  
  if (!blocks.length && sentences.length) {
    blocks.push(`主要贡献：${sentences.slice(0, 2).join("；")}。`);
  } else if (!blocks.length) {
    blocks.push(`主要贡献：提出并验证了所述模型及技术方案，为后续同类研究提供了有价值的参考和经验。`);
    blocks.push(`局限性：受限于实验样本和评估环境，后续可在更大规模场景下进行深入测试与验证。`);
  }
  
  return formatReviewParagraphs(blocks.join("\n\n"));
}

function buildDatasetsFallback(sections, paper = {}) {
  const sourceText = [sections.method, sections.results, sections.synthesis].filter(Boolean).join("\n");
  const sentences = splitIntoSentences(sourceText);
  
  const datasets = sentences.filter(s => /数据|样本|实验|评测|指标|数据集|评估/i.test(s)).slice(0, 3);
  
  const blocks = [];
  if (datasets.length) {
    blocks.push(`数据与评测：${datasets.join("；")}。`);
  } else {
    blocks.push(`数据与评测：本研究的实验设计与评价指标需参考方法与结果部分，建议重点关注模型对比及消融实验设置。`);
  }
  return formatReviewParagraphs(blocks.join("\n\n"));
}

function formatReviewSections(sections) {
  return Object.fromEntries(Object.entries(sections).map(([key, value]) => [key, formatReviewParagraphs(value)]));
}

function formatReviewParagraphs(value = "") {
  const labels = [
    "论文定位", "发表信息", "发布信息", "汇报价值", "研究背景", "研究问题", "研究方法与数据", "实验与结论",
    "创新点与启示", "局限性", "核心要点", "要点", "主要贡献", "关键问题", "本文思想", "关键贡献",
    "整体框架", "关键模块", "实现流程", "主要发现", "对比结果", "实验结论", "关键证据", "实验设置",
    "数据来源", "数据设置", "评测指标", "方法路线", "结果证据", "研究结论", "现有不足", "未来展望",
    "贡献", "局限", "讨论点", "启发"
  ];
  const labelPattern = labels.join("|");
  return String(value || "")
    .replace(/\r\n/g, "\n")
    .replace(/发布信息/g, "发表信息")
    .replace(new RegExp(`\\s*((?:${labelPattern})\\s*[：:])\\s*`, "g"), "\n\n$1\n")
    .replace(/([。；;])((?:第二|第三|第四|第五|第六|第七|其次|再次|最后)[，,])/g, "$1\n\n$2")
    .replace(/([。；;])\s*((?:\d+[.、]|[（(]\d+[）)]))/g, "$1\n$2")
    .replace(/\n{3,}/g, "\n\n")
    .replace(/^\n+/, "")
    .trim();
}

function updateReviewSection(key, event) {
  reviewModal.sections[key] = event?.currentTarget?.innerText || "";
}

function formatReviewSection(key) {
  reviewModal.sections[key] = formatReviewParagraphs(reviewModal.sections[key]);
}

function pastePlainReviewText(event) {
  event.preventDefault();
  const text = event.clipboardData?.getData("text/plain") || "";
  document.execCommand("insertText", false, text);
}

function resizeAllReviewTextareas() {
  // Rich editors grow with content; kept as a stable hook for existing async flows.
}

function highlightedReviewHtml(value = "") {
  const text = String(value || "");
  if (!text.trim()) return "";
  
  const lines = text.split("\n").map(l => l.trim());
  const processed = [];
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const isHeading = /^[\u4e00-\u9fa5A-Za-z（）()、与及\s]{2,24}\s*[：:]$/.test(line);
    
    if (isHeading) {
      while (processed.length && processed[processed.length - 1] === "") {
        processed.pop();
      }
      processed.push(`<span class="review-inline-heading">${escapeHtml(line)}</span>`);
    } else if (!line) {
      if (processed.length && processed[processed.length - 1] !== "" && !processed[processed.length - 1].startsWith("<span class=\"review-inline-heading\"")) {
        processed.push("");
      }
    } else {
      processed.push(highlightLatinAndNumbers(escapeHtml(line)));
    }
  }
  
  return processed.join("<br>");
}

function highlightLatinAndNumbers(escapedLine = "") {
  return escapedLine.replace(/([+-]?\d+(?:[.,]\d+)*(?:\.\d+)?%?)/g, '<span class="review-number">$1</span>');
}

function escapeHtml(value = "") {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

async function copyReviewSection(section) {
  const content = (reviewModal.sections[section.key] || "").trim();
  if (!content) {
    showToast("这一段还没有内容");
    return;
  }
  const text = `${section.title}\n${content}`;
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
    } else {
      const textarea = document.createElement("textarea");
      textarea.value = text;
      textarea.setAttribute("readonly", "");
      textarea.style.position = "fixed";
      textarea.style.opacity = "0";
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand("copy");
      document.body.removeChild(textarea);
    }
    showToast(`已复制${section.title}`);
  } catch {
    showToast("复制失败，请手动选择文本复制");
  }
}

async function importReviewToMeeting() {
  const meeting = reviewModal.meeting;
  if (!meeting) {
    showToast("未找到当前组会");
    return;
  }
  if (importingReview.value) return;
  importingReview.value = true;
  showToast("AI 正在整理组会字段");
  await new Promise(resolve => window.setTimeout(resolve, 700));
  const sections = reviewModal.sections || {};
  meeting.details = normalizeMeetingDetails(meeting.details);
  meeting.notes = formatMeetingBullets([
    sections.overview && `研究问题：${sections.overview}`,
    sections.method && `方法路线：${sections.method}`,
    sections.results && `结果证据：${sections.results}`,
  ], 3) || meeting.notes;
  meeting.details.objective = formatMeetingBullets([
    sections.overview && `判断选题与研究问题是否成立：${sections.overview}`,
    sections.method && `确认方法路线是否可作为后续课题推进基础：${sections.method}`,
    sections.conclusion && `讨论贡献、局限与下一步修改方向：${sections.conclusion}`,
  ], 3);
  meeting.details.questions = formatMeetingBullets([
    sections.overview && `本文真正回答的核心问题是什么？${sections.overview}`,
    sections.method && `方法设计是否足以支撑研究问题？${sections.method}`,
    sections.results && `结果证据是否充分，哪些部分需要补充？${sections.results}`,
  ], 3);
  meeting.details.evidence = "";
  meeting.details.discussion = "";
  activeMeetingId.value = meeting.id;
  persistMeetings();
  importingReview.value = false;
  showToast("已整理并导入前三个组会字段");
}

async function importMeetingReviews(meeting) {
  const selected = selectedMeetingPapers(meeting);
  if (!selected.length || isImportBusy(meeting)) return;
  activeMeetingId.value = meeting.id;
  let progressTimer = null;
  let reports = [];
  importJobs[meeting.id] = {
    status: "running",
    progress: 6,
    message: selected.length === 1 ? "正在读取单篇综述" : `正在读取 ${selected.length} 篇综述`,
  };
  try {
    reports = [];
    for (let index = 0; index < selected.length; index += 1) {
      const paper = selected[index];
      importJobs[meeting.id] = {
        status: "running",
        progress: 10 + Math.round((index / selected.length) * 45),
        message: `读取综述：${paper.title || "未命名论文"}`,
      };
      const data = await paperpilotApi.getMeetingReport(paper.workspaceId);
      const sections = formatReviewSections(data.sections || {});
      if (data.generated && hasUsefulReviewSections(sections)) {
        reports.push({ paper, sections });
      }
    }
    if (!reports.length) {
      importJobs[meeting.id] = { status: "failed", progress: 100, message: "请先生成至少一篇论文综述" };
      showToast("请先生成至少一篇论文综述");
      return;
    }
    importJobs[meeting.id] = {
      status: "running",
      progress: 56,
      message: reports.length > 1 ? `模型正在融合 ${reports.length} 篇综述` : "模型正在整理单篇综述",
    };
    progressTimer = window.setInterval(() => {
      const current = importJobs[meeting.id];
      if (!current || current.status !== "running") return;
      const next = Math.min(92, Number(current.progress || 0) + (reports.length > 1 ? 3 : 5));
      importJobs[meeting.id] = {
        ...current,
        progress: next,
        message: next < 74 ? "模型正在比较研究问题与方法差异" : next < 88 ? "模型正在压缩成组会字段" : "正在等待模型返回结果",
      };
    }, 900);
    const fused = await paperpilotApi.fuseMeetingReport({
      reports: reports.map(({ paper, sections }) => ({
        title: paper.title || "未命名论文",
        authors: paper.authors || "作者未补全",
        source: paper.source || "",
        publishYear: paper.publishYear || "",
        sections,
      })),
    });
    if (progressTimer) {
      window.clearInterval(progressTimer);
      progressTimer = null;
    }
    applyFusedMeetingFields(meeting, fused, reports);
    importJobs[meeting.id] = {
      status: "generated",
      progress: 100,
      message: reports.length > 1 ? `已用模型融合 ${reports.length} 篇综述` : "已用模型整理单篇综述",
    };
    showToast(importJobs[meeting.id].message);
  } catch (error) {
    if (progressTimer) window.clearInterval(progressTimer);
    importJobs[meeting.id] = {
      status: "failed",
      progress: 100,
      message: error?.response?.data?.message || "模型融合失败，请稍后重试",
    };
    showToast(importJobs[meeting.id].message);
  }
}

function hasUsefulReviewSections(sections = {}) {
  return reviewSections.some(section => compactReviewText(sections[section.key], 80).length > 12);
}

function applyReportsToMeeting(meeting, reports) {
  meeting.details = normalizeMeetingDetails(meeting.details);
  if (reports.length === 1) {
    const sections = reports[0].sections || {};
    meeting.notes = formatMeetingBullets([
      sections.overview && `研究问题：${sections.overview}`,
      sections.method && `方法路线：${sections.method}`,
      sections.results && `结果证据：${sections.results}`,
    ], 3) || meeting.notes;
    meeting.details.objective = formatMeetingBullets([
      sections.overview && `判断选题与研究问题是否成立：${sections.overview}`,
      sections.method && `确认方法路线是否可作为后续课题推进基础：${sections.method}`,
      sections.conclusion && `讨论贡献、局限与下一步修改方向：${sections.conclusion}`,
    ], 3);
    meeting.details.questions = formatMeetingBullets([
      sections.overview && `本文真正回答的核心问题是什么？${sections.overview}`,
      sections.method && `方法设计是否足以支撑研究问题？${sections.method}`,
      sections.results && `结果证据是否充分，哪些部分需要补充？${sections.results}`,
    ], 3);
  } else {
    meeting.notes = formatMeetingBullets(reports.map(({ paper, sections }) =>
      `${paper.title || "未命名论文"}：${joinReviewParts([sections.overview, sections.method, sections.results], 210, 72)}`
    ), 3, 190);
    meeting.details.objective = formatMeetingBullets([
      `对比 ${reports.length} 篇文献的研究问题是否指向同一类学术缺口，判断本次汇报应以共同问题还是差异比较为主线。`,
      `融合各文献的方法路线与证据强度，确定哪些方法、数据或理论框架值得后续课题继续沿用。`,
      `提炼可向导师讨论的推进方向，包括补实验、换数据、调整问题表述或形成组合式研究方案。`,
    ], 3, 180);
    meeting.details.questions = formatMeetingBullets([
      `这些文献的核心问题是否一致？差异主要来自研究对象、方法假设、数据来源还是评价指标？`,
      `哪一篇的证据链最完整，哪一篇只适合作为背景或对照材料？`,
      `如果要合并成一个 PPT，应该按问题线、方法线还是结果线组织，避免逐篇流水账？`,
    ], 3, 180);
  }
  meeting.details.evidence = "";
  meeting.details.discussion = "";
  persistMeetings();
}

function applyFusedMeetingFields(meeting, fused = {}, reports = []) {
  meeting.details = normalizeMeetingDetails(meeting.details);
  meeting.notes = cleanMeetingField(fused.notes) || meeting.notes;
  meeting.details.objective = cleanMeetingField(fused.objective) || meeting.details.objective;
  meeting.details.questions = cleanMeetingField(fused.questions) || meeting.details.questions;
  if (!meeting.notes || !meeting.details.objective || !meeting.details.questions) {
    applyReportsToMeeting(meeting, reports);
    return;
  }
  meeting.details.evidence = "";
  meeting.details.discussion = "";
  persistMeetings();
}

function cleanMeetingField(value = "") {
  return String(value || "")
    .replace(/\r\n/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function normalizeDeckSlideCount(value = DEFAULT_DECK_SLIDE_COUNT) {
  const numbers = String(value || "")
    .match(/\d+/g)
    ?.map(Number)
    .filter(Number.isFinite) || [];
  if (!numbers.length) return DEFAULT_DECK_SLIDE_COUNT;
  const first = Math.min(MAX_DECK_SLIDES, Math.max(1, numbers[0]));
  const last = Math.min(MAX_DECK_SLIDES, Math.max(first, numbers[numbers.length - 1]));
  return first === last ? String(first) : `${first}-${last}`;
}

async function makePpt(meeting) {
  const selected = selectedMeetingPapers(meeting);
  const paper = primaryPaper(meeting) || selected[0];
  if (!selected.length || !paper || isDeckBusy(meeting)) return;
  if (missingPdfCount(meeting)) {
    showToast("请先补齐本次组会文献的 PDF");
    return;
  }
  confirmOpened.value = "";
  if (deckTimers.has(meeting.id)) {
    window.clearTimeout(deckTimers.get(meeting.id));
    deckTimers.delete(meeting.id);
  }
  activeMeetingId.value = meeting.id;
  deckJobs[meeting.id] = {
    status: "running",
    progress: 1,
    stage: "提交任务",
    message: "正在提交 PPT 制作任务",
    paperWorkspaceId: paper.workspaceId,
    paperTitle: selected.length > 1 ? `${selected.length} 篇文献联合汇报` : paper.title,
    jobId: "",
    confirmUrl: "",
    downloadUrl: "",
    localPath: "",
  };
  persistDeckJobs();
  try {
    const payload = {
      engine: "ppt-master-skill",
      reportWorkspaceId: paper.workspaceId,
      paperIds: selected.map(item => item.workspaceId),
      slideCount: normalizeDeckSlideCount(meeting.params.slideCount),
      audience: meeting.params.audience,
      focus: meetingFocusText(meeting),
      templateName: meeting.params.reportType,
      pptSettings: {
        ...(meeting.params.pptMasterConfirm || {}),
        visualStyle: meeting.params.visualStyle || "editorial",
        includeComparisonAppendix: meeting.params.includeComparisonAppendix !== false,
      },
    };

    let requestBody = payload;
    // Check if desktop has local cached PDF for this paper
    if (window.paperSolverDesktop?.getCachedPdf && paper.workspaceId) {
      try {
        const sourceUrls = [paper.pdfUrl, paper.paperUrl, paper.sourceUrl].filter(Boolean);
        const cached = await window.paperSolverDesktop.getCachedPdf({
          workspaceId: paper.workspaceId,
          sourceUrls
        });
        if (cached?.found && cached.base64) {
          const binary = atob(cached.base64);
          const bytes = new Uint8Array(binary.length);
          for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
          const blob = new Blob([bytes], { type: "application/pdf" });
          const formData = new FormData();
          formData.append("payload", JSON.stringify(payload));
          formData.append("reportPaper", blob, `${paper.title || "report-paper"}.pdf`);
          requestBody = formData;
        }
      } catch (err) {
        console.warn("Failed to attach desktop cached PDF for PPT generation", err);
      }
    }

    const result = await paperpilotApi.generateMeetingDeck(requestBody);
    applyDeckJob(meeting, result, paper);
    if (result?.jobId && !result.done) {
      pollDeck(meeting.id, result.jobId, paper);
      showToast("PPT 已进入后台生成，可以离开页面");
    } else if (result?.success && result.downloadUrl) {
      showToast("PPT 已生成，可以下载");
    }
  } catch (error) {
    deckJobs[meeting.id] = {
      ...deckJobs[meeting.id],
      status: "failed",
      progress: deckJobs[meeting.id]?.progress || 0,
      stage: "生成失败",
      message: publicDeckText(error?.response?.data?.message || "PPT 制作失败"),
    };
    showToast(deckJobs[meeting.id].message);
    closePendingConfirmWindow();
  }
}

function openPendingConfirmWindow() {
  try {
    const win = window.open("about:blank", "_blank");
    if (!win) {
      showToast("参数页弹窗被浏览器拦截，请允许弹窗后重试");
      return null;
    }
    win.document.write(`
      <!doctype html>
      <html lang="zh-CN">
        <head><meta charset="utf-8"><title>PPT 参数页准备中</title></head>
        <body style="margin:0;display:grid;place-items:center;min-height:100vh;font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;color:#172033;background:#f6f9ff;">
          <main style="width:min(520px,calc(100vw - 48px));padding:28px;border:1px solid #d8e5f8;border-radius:18px;background:#fff;box-shadow:0 18px 45px rgba(31,57,95,.12)">
            <strong style="display:block;font-size:18px;margin-bottom:10px;">PPT 参数页准备中</strong>
            <p style="margin:0;color:#52637a;line-height:1.7;">正在等待后端启动参数确认页，稍后会自动跳转。</p>
          </main>
        </body>
      </html>
    `);
    win.document.close();
    return win;
  } catch {
    return null;
  }
}

function closePendingConfirmWindow() {
  try {
    if (pendingConfirmWindow && !pendingConfirmWindow.closed) pendingConfirmWindow.close();
  } catch {
    // Ignore popup cleanup failures.
  } finally {
    pendingConfirmWindow = null;
  }
}

function pollDeck(meetingId, jobId, paper) {
  if (deckTimers.has(meetingId)) window.clearTimeout(deckTimers.get(meetingId));
  const timer = window.setTimeout(async () => {
    try {
      const result = await paperpilotApi.getMeetingDeckStatus(jobId);
      const meeting = meetings.value.find((item) => item.id === meetingId);
      applyDeckJob(meeting || { id: meetingId }, result, paper);
      if (result.done) {
        deckTimers.delete(meetingId);
        showToast(result.success ? "PPT 已生成，可以下载" : (result.message || "PPT 制作失败"));
        return;
      }
      pollDeck(meetingId, jobId, paper);
    } catch (error) {
      deckJobs[meetingId] = {
        ...deckJobs[meetingId],
        status: "running",
        stage: publicDeckText(deckJobs[meetingId]?.stage || "刷新状态"),
        message: publicDeckText(error?.response?.data?.message || "PPT 状态刷新失败，稍后自动重试"),
      };
      pollDeck(meetingId, jobId, paper);
    }
  }, 1600);
  deckTimers.set(meetingId, timer);
}

function applyDeckJob(meeting, payload = {}, paper = {}) {
  if (!meeting?.id) return;
  const wasGenerated = Boolean(deckJobs[meeting.id]?.downloadUrl);
  deckJobs[meeting.id] = {
    ...(deckJobs[meeting.id] || {}),
    status: payload.status || (payload.success ? "generated" : "running"),
    progress: payload.success ? 100 : Number(payload.progress ?? deckJobs[meeting.id]?.progress ?? 0),
    stage: publicDeckText(payload.stage || deckJobs[meeting.id]?.stage || ""),
    message: publicDeckText(payload.message || deckJobs[meeting.id]?.message || ""),
    paperWorkspaceId: paper.workspaceId || deckJobs[meeting.id]?.paperWorkspaceId || "",
    paperTitle: paper.title || deckJobs[meeting.id]?.paperTitle || "",
    jobId: payload.jobId || deckJobs[meeting.id]?.jobId || "",
    confirmUrl: payload.confirmUrl || deckJobs[meeting.id]?.confirmUrl || "",
    downloadUrl: payload.downloadUrl || deckJobs[meeting.id]?.downloadUrl || "",
    previewUrls: Array.isArray(payload.previewUrls) ? payload.previewUrls : (deckJobs[meeting.id]?.previewUrls || []),
    localPath: deckJobs[meeting.id]?.localPath || "",
  };
  if (deckJobs[meeting.id].downloadUrl) deckJobs[meeting.id].status = "generated";
  persistDeckJobs();
  if (
    deckJobs[meeting.id].confirmUrl &&
    confirmOpened.value !== deckJobs[meeting.id].confirmUrl &&
    Number(deckJobs[meeting.id].progress || 0) <= 28
  ) {
    confirmOpened.value = deckJobs[meeting.id].confirmUrl;
    if (pendingConfirmWindow && !pendingConfirmWindow.closed) {
      pendingConfirmWindow.location.href = absoluteApiUrl(deckJobs[meeting.id].confirmUrl);
      pendingConfirmWindow.focus();
      pendingConfirmWindow = null;
    } else {
      openDeckConfirmPage(deckJobs[meeting.id].confirmUrl);
    }
  }
  if (deckJobs[meeting.id].downloadUrl && canSaveDeckToDesktop() && !deckJobs[meeting.id].localPath) {
    saveDeckToDesktop(meeting, true);
  }
  if (!wasGenerated && deckJobs[meeting.id].downloadUrl && deckJobs[meeting.id].previewUrls?.length) {
    openDeckPreview(meeting);
  }
  if (deckJobs[meeting.id].downloadUrl || deckJobs[meeting.id].status === "failed") {
    closePendingConfirmWindow();
  }
}

function openDeckConfirmPage(url) {
  const target = absoluteApiUrl(url);
  if (!target) return;
  const win = window.open(target, "_blank", "noopener,noreferrer");
  if (!win) showToast("浏览器拦截了参数页，请允许弹窗后重试");
}

async function openDeckPreview(meeting) {
  const sourceUrls = deckJobs[meeting.id]?.previewUrls || [];
  if (!sourceUrls.length) {
    showToast("预览页尚未就绪，请稍后刷新任务状态");
    return;
  }
  deckPreview.title = meeting.title || deckJobs[meeting.id]?.paperTitle || "组会汇报";
  deckPreview.urls.forEach(url => URL.revokeObjectURL(url));
  deckPreview.urls = [];
  deckPreview.index = 0;
  deckPreview.open = true;
  deckPreview.loading = true;
  try {
    const blobs = await Promise.all(sourceUrls.map(url => paperpilotApi.fetchMeetingDeckPreview(url)));
    deckPreview.urls = blobs.map(blob => URL.createObjectURL(blob));
  } catch (error) {
    closeDeckPreview();
    showToast(error?.response?.data?.message || "PPT 预览载入失败");
  } finally {
    deckPreview.loading = false;
  }
}

function closeDeckPreview() {
  deckPreview.open = false;
  deckPreview.urls.forEach(url => URL.revokeObjectURL(url));
  deckPreview.urls = [];
  deckPreview.index = 0;
  deckPreview.loading = false;
}

function publicDeckText(value = "") {
  return String(value || "")
    .replace(/使用管理员组会汇报模型[:：]?\s*[^，。；\s]+/g, "正在启动组会 PPT 引擎")
    .replace(/当前模型\s+[^，。；]+?\s+不适合执行多轮 PPT Master；?/g, "当前 PPT 生成引擎不稳定；")
    .replace(/模型\s+[A-Za-z0-9._:/-]+/g, "模型")
    .replace(/modelName[:：]?\s*[^，。；\s]+/gi, "")
    .trim();
}

function canSaveDeckToDesktop() {
  return Boolean(window.paperSolverDesktop?.isDesktop && window.paperSolverDesktop?.savePptDeck);
}

async function saveDeckToDesktop(meeting, silent = false) {
  const job = deckJobs[meeting.id];
  if (!job?.downloadUrl || !canSaveDeckToDesktop() || savingLocalDecks.has(meeting.id)) return;
  savingLocalDecks.add(meeting.id);
  try {
    const result = await paperpilotApi.saveMeetingDeckToDesktop({
      url: absoluteApiUrl(job.downloadUrl),
      fileName: `${meeting.title || job.paperTitle || "组会汇报"}.pptx`,
      meetingTitle: meeting.title || job.paperTitle || "组会汇报",
      jobId: job.jobId,
    });
    if (result?.ok) {
      deckJobs[meeting.id] = {
        ...deckJobs[meeting.id],
        localPath: result.path || "",
      };
      persistDeckJobs();
      if (!silent) showToast("PPT 已保存到本机目录");
    } else if (!silent) {
      showToast("PPT 本机保存失败");
    }
  } catch (error) {
    if (!silent) showToast(error?.message || "PPT 本机保存失败");
  } finally {
    savingLocalDecks.delete(meeting.id);
  }
}

function resumeDeckJobs() {
  Object.entries(deckJobs).forEach(([meetingId, job]) => {
    if (isDeckRunning(job) && job.jobId) {
      const paper = papers.value.find((item) => item.workspaceId === job.paperWorkspaceId) || {};
      pollDeck(meetingId, job.jobId, paper);
    }
  });
}

function resumeReviewJobs() {
  Object.entries(reviewJobs).forEach(([workspaceId, job]) => {
    if (job?.status === "running") pollReview(workspaceId);
  });
}

function absoluteApiUrl(url) {
  if (!url) return "";
  if (/^https?:\/\//i.test(url)) return url;
  return `${API_BASE_URL}${url.startsWith("/") ? "" : "/"}${url}`;
}

function showToast(message) {
  toastMessage.value = message;
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toastMessage.value = "";
  }, 3000);
}
</script>

<style scoped>

/* Homepage Style Icon Container Boxes (Matching Screenshot) */
.homepage-icon-box {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.homepage-icon-box.box-blue {
  background: rgba(37, 99, 235, 0.08);
  border: 1.5px solid rgba(37, 99, 235, 0.35);
  color: #2563eb;
}
:root[data-theme="dark"] .homepage-icon-box.box-blue {
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.4);
  color: #60a5fa;
}

.homepage-icon-box.box-purple {
  background: rgba(147, 51, 234, 0.08);
  border: 1.5px solid rgba(147, 51, 234, 0.35);
  color: #9333ea;
}
:root[data-theme="dark"] .homepage-icon-box.box-purple {
  background: rgba(168, 85, 247, 0.12);
  border-color: rgba(168, 85, 247, 0.4);
  color: #c084fc;
}

.homepage-icon-box.box-emerald {
  background: rgba(16, 185, 129, 0.08);
  border: 1.5px solid rgba(16, 185, 129, 0.35);
  color: #10b981;
}
:root[data-theme="dark"] .homepage-icon-box.box-emerald {
  background: rgba(16, 185, 129, 0.12);
  border-color: rgba(16, 185, 129, 0.4);
  color: #34d399;
}

.homepage-icon-box.box-amber {
  background: rgba(245, 158, 11, 0.08);
  border: 1.5px solid rgba(245, 158, 11, 0.35);
  color: #d97706;
}
:root[data-theme="dark"] .homepage-icon-box.box-amber {
  background: rgba(245, 158, 11, 0.12);
  border-color: rgba(245, 158, 11, 0.4);
  color: #fbbf24;
}

.stat-card:hover .homepage-icon-box {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}


/* Stat Icons Vector Styling */
.stat-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}
.stat-card:hover .stat-icon-wrap {
  transform: scale(1.08);
}
.stat-icon-calendar { background: rgba(99, 102, 241, 0.12); color: #6366f1; }
:root[data-theme="dark"] .stat-icon-calendar { background: rgba(99, 102, 241, 0.2); color: #818cf8; }

.stat-icon-books { background: rgba(168, 85, 247, 0.12); color: #a855f7; }
:root[data-theme="dark"] .stat-icon-books { background: rgba(168, 85, 247, 0.2); color: #c084fc; }

.stat-icon-ppt { background: rgba(16, 185, 129, 0.12); color: #10b981; }
:root[data-theme="dark"] .stat-icon-ppt { background: rgba(16, 185, 129, 0.2); color: #34d399; }

.stat-icon-bulb { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
:root[data-theme="dark"] .stat-icon-bulb { background: rgba(245, 158, 11, 0.2); color: #fbbf24; }

/* Custom Hidden File Input */
.hidden-file-input {
  position: absolute !important;
  width: 1px !important;
  height: 1px !important;
  padding: 0 !important;
  margin: -1px !important;
  overflow: hidden !important;
  clip: rect(0, 0, 0, 0) !important;
  white-space: nowrap !important;
  border: 0 !important;
  opacity: 0 !important;
  pointer-events: none !important;
}

.upload-inline {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #a855f7);
  color: #ffffff !important;
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  white-space: nowrap;
  border: none !important;
}

.upload-inline:hover {
  transform: translateY(-1.5px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.45);
}

.upload-inline.busy {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ==========================================================================
   PREMIUM TIMELINE LAYOUT (Clean Dual-Theme Light & Dark)
   ========================================================================== */

/* --- CSS Variables for Clean Theme Adaptation --- */
.meeting-timeline-page {
  --bg-page: #f8fafc;
  --text-main: #0f172a;
  --text-muted: #64748b;
  --title-gradient: linear-gradient(135deg, #0f172a 0%, #334155 100%);
  
  --card-bg: #ffffff;
  --card-border: rgba(226, 232, 240, 0.8);
  --card-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
  
  --input-bg: #f8fafc;
  --input-border: #cbd5e1;
  --input-text: #0f172a;
  --input-focus: #6366f1;
  --input-shadow: rgba(99, 102, 241, 0.15);
  
  --pin-border: #6366f1;
  --pin-bg: #ffffff;
  --pin-glow: rgba(99, 102, 241, 0.25);
  
  --btn-soft-bg: #f1f5f9;
  --btn-soft-text: #334155;
  --btn-soft-border: #cbd5e1;
  
  --modal-bg: #ffffff;
  --modal-border: #e2e8f0;
  --modal-shadow: 0 25px 50px rgba(15, 23, 42, 0.12);
  --modal-header-bg: #ffffff;
  
  --item-bg: #f8fafc;
  --item-border: #e2e8f0;
  
  /* Modal multi-color themes (Light) */
  --point-1-bg: #eff6ff; --point-1-text: #1d4ed8;
  --point-2-bg: #f0fdf4; --point-2-text: #15803d;
  --point-3-bg: #fefce8; --point-3-text: #a16207;
  --point-4-bg: #f5f3ff; --point-4-text: #6d28d9;
}

:root[data-theme="dark"] .meeting-timeline-page {
  --bg-page: #08080c;
  --text-main: #f8fafc;
  --text-muted: #94a3b8;
  --title-gradient: linear-gradient(135deg, #f8fafc 0%, #cbd5e1 100%);
  
  --card-bg: rgba(15, 23, 42, 0.6);
  --card-border: rgba(255, 255, 255, 0.08);
  --card-shadow: 0 20px 50px rgba(0, 0, 0, 0.4);
  
  --input-bg: rgba(30, 41, 59, 0.5);
  --input-border: rgba(255, 255, 255, 0.1);
  --input-text: #cbd5e1;
  --input-focus: #818cf8;
  --input-shadow: rgba(99, 102, 241, 0.2);
  
  --pin-border: #a855f7;
  --pin-bg: #0f172a;
  --pin-glow: rgba(168, 85, 247, 0.6);
  
  --btn-soft-bg: rgba(255, 255, 255, 0.1);
  --btn-soft-text: #e2e8f0;
  --btn-soft-border: transparent;
  
  --modal-bg: rgba(15, 23, 42, 0.95);
  --modal-border: rgba(255, 255, 255, 0.1);
  --modal-shadow: 0 25px 60px rgba(0, 0, 0, 0.8), inset 0 0 0 1px rgba(255, 255, 255, 0.05);
  --modal-header-bg: rgba(15, 23, 42, 0.98);
  
  --item-bg: rgba(30, 41, 59, 0.4);
  --item-border: rgba(255, 255, 255, 0.05);
  
  /* Modal multi-color themes (Dark) */
  --point-1-bg: rgba(30, 58, 138, 0.25); --point-1-text: #93c5fd;
  --point-2-bg: rgba(20, 83, 45, 0.25); --point-2-text: #6ee7b7;
  --point-3-bg: rgba(113, 63, 18, 0.25); --point-3-text: #fde047;
  --point-4-bg: rgba(76, 29, 149, 0.25); --point-4-text: #c4b5fd;
}

/* --- Base Layout --- */
.meeting-timeline-page {
  min-height: 100vh;
  padding: 30px min(44px, 4vw) 80px;
  background: var(--bg-page);
  color: var(--text-main);
  font-family: Inter, "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  overflow-x: hidden;
  transition: background 0.3s ease, color 0.3s ease;
}

.timeline-header,
.timeline-stats-bar,
.timeline-shell {
  width: 100%;
  padding-inline: 48px;
  box-sizing: border-box;
  margin-inline: auto;
}

.timeline-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
  position: relative;
  z-index: 10;
}

.timeline-header span {
  color: #818cf8;
  font-size: 13px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

.timeline-header h1 {
  margin: 8px 0 0;
  color: var(--text-main);
  font-size: 36px;
  line-height: 1.25;
  font-weight: 900;
  background: var(--title-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.timeline-header p {
  max-width: 680px;
  margin: 12px 0 0;
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.65;
}

.add-meeting-button {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 0 22px;
  height: 46px;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, #6366f1, #a855f7);
  color: #ffffff;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 8px 25px rgba(99, 102, 241, 0.35);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.add-meeting-button:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 12px 32px rgba(99, 102, 241, 0.5);
}

.add-meeting-button span {
  font-size: 22px;
  font-weight: 300;
}

/* --- Executive Stats Bar --- */
.timeline-stats-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 36px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  border-radius: 16px;
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  box-shadow: var(--card-shadow);
  transition: transform 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-icon {
  font-size: 24px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-info strong {
  font-size: 18px;
  font-weight: 900;
  color: var(--text-main);
}

.stat-info small {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

/* --- Timeline Track --- */
.timeline-list {
  position: relative;
  display: grid;
  gap: 36px;
  padding-left: 48px;
}

.timeline-list::before {
  content: "";
  position: absolute;
  left: 15px;
  top: 30px;
  bottom: 0;
  width: 3px;
  border-radius: 999px;
  background: linear-gradient(to bottom, #6366f1, #a855f7, #ec4899, rgba(236, 72, 153, 0.1));
  box-shadow: 0 0 12px var(--pin-glow);
  z-index: 1;
}

/* --- Meeting Card --- */
.meeting-card {
  position: relative;
  border-radius: 20px;
  padding: 28px;
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  box-shadow: var(--card-shadow);
  transition: all 0.3s ease;
  z-index: 2;
}

.meeting-card:hover, .meeting-card.active {
  border-color: rgba(99, 102, 241, 0.4);
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.1), 0 0 30px rgba(99, 102, 241, 0.08);
}

.timeline-pin {
  position: absolute;
  left: -48px;
  top: 36px;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--pin-bg);
  border: 2px solid var(--pin-border);
  box-shadow: 0 0 15px var(--pin-glow);
  z-index: 3;
}

.timeline-pin span {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #ec4899);
  animation: pulse-glow 2s infinite alternate;
}

@keyframes pulse-glow {
  0% { box-shadow: 0 0 5px #ec4899; opacity: 0.8; }
  100% { box-shadow: 0 0 20px #ec4899; opacity: 1; }
}

/* --- Card Header --- */
.meeting-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--card-border);
}

.meeting-date-block {
  display: flex;
  align-items: center;
  gap: 16px;
}

.date-summary {
  display: flex;
  flex-direction: column;
}

.date-summary span {
  color: var(--text-muted);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 2px;
}

.date-summary strong {
  color: var(--text-main);
  font-size: 16px;
  font-weight: 700;
}

.meeting-date-block input[type="datetime-local"] {
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  color: var(--input-text);
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
}

.meeting-date-block input[type="datetime-local"]:focus {
  border-color: var(--input-focus);
}

.meeting-head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meeting-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  border-radius: 999px;
  padding: 0 14px;
  background: var(--item-bg);
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  border: 1px solid var(--item-border);
}

.meeting-status span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-muted);
}

.meeting-status[data-status="running"] {
  background: rgba(59, 130, 246, 0.12);
  color: #2563eb;
  border-color: rgba(59, 130, 246, 0.3);
}
:root[data-theme="dark"] .meeting-status[data-status="running"] { color: #93c5fd; }

.meeting-status[data-status="ready"] {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
  border-color: rgba(16, 185, 129, 0.3);
}
:root[data-theme="dark"] .meeting-status[data-status="ready"] { color: #6ee7b7; }

.meeting-status[data-status="failed"] {
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
  border-color: rgba(239, 68, 68, 0.3);
}
:root[data-theme="dark"] .meeting-status[data-status="failed"] { color: #fca5a5; }

.delete-meeting-button {
  height: 32px;
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 999px;
  padding: 0 14px;
  background: rgba(239, 68, 68, 0.08);
  color: #dc2626;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}
:root[data-theme="dark"] .delete-meeting-button { color: #fca5a5; }

.delete-meeting-button:hover {
  background: rgba(239, 68, 68, 0.2);
}

/* --- Card Body --- */
.meeting-card-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 30px;
  align-items: stretch;
}

.meeting-main {
  display: grid;
  gap: 20px;
}

.meeting-title-input {
  width: 100%;
  height: 48px;
  box-sizing: border-box;
  border: none;
  background: transparent;
  color: var(--text-main);
  font-size: 24px;
  font-weight: 900;
  padding: 0;
  outline: none;
}

.meeting-title-input::placeholder {
  color: var(--text-muted);
}

.meeting-title-input:focus {
  border-bottom: 2px solid var(--input-focus);
}

.meeting-notes,
.meeting-detail-field,
.meeting-advisor-note {
  display: grid;
  gap: 8px;
}

.meeting-notes span,
.meeting-detail-field span,
.meeting-advisor-note span {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
}

.meeting-notes textarea,
.meeting-detail-field textarea,
.meeting-advisor-note textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--input-border);
  border-radius: 12px;
  padding: 16px;
  background: var(--input-bg);
  color: var(--input-text);
  font-size: 14px;
  line-height: 1.7;
  outline: none;
  resize: vertical;
  transition: all 0.2s;
}

.meeting-notes textarea { min-height: 120px; }
.meeting-detail-field textarea { min-height: 90px; }
.meeting-advisor-note textarea { min-height: 100px; }

.meeting-notes textarea:focus,
.meeting-detail-field textarea:focus,
.meeting-advisor-note textarea:focus {
  border-color: var(--input-focus);
  box-shadow: 0 0 0 3px var(--input-shadow);
}

.meeting-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.meeting-advisor-note {
  padding: 16px;
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: 16px;
  background: rgba(245, 158, 11, 0.05);
}

.meeting-advisor-note span {
  color: #d97706;
}
:root[data-theme="dark"] .meeting-advisor-note span { color: #fbbf24; }

.meeting-advisor-note textarea {
  border-color: rgba(245, 158, 11, 0.3);
  background: var(--input-bg);
}

.meeting-advisor-note textarea:focus {
  border-color: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.15);
}

/* --- Side Panel --- */
.meeting-side {
  display: grid;
  gap: 20px;
  align-content: start;
}

.paper-box,
.generation-action {
  border-radius: 16px;
  background: var(--item-bg);
  border: 1px solid var(--item-border);
  padding: 20px;
}

.box-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.box-title span {
  color: var(--text-main);
  font-size: 14px;
}

.box-title button {
  border: none;
  background: rgba(99, 102, 241, 0.12);
  color: #4f46e5;
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}
:root[data-theme="dark"] .box-title button { color: #818cf8; background: rgba(99, 102, 241, 0.18); }

.box-title button:hover {
  background: rgba(99, 102, 241, 0.25);
}

.paper-box p {
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
}

.selected-papers {
  display: grid;
  gap: 12px;
}

.selected-paper {
  border: 1px solid var(--item-border);
  border-radius: 12px;
  padding: 14px;
  background: var(--card-bg);
  cursor: pointer;
  transition: all 0.2s ease;
}

.selected-paper:hover {
  border-color: var(--color-primary, #6366f1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.selected-paper.primary {
  border-color: rgba(99, 102, 241, 0.4);
  background: rgba(99, 102, 241, 0.04);
}

.selected-paper strong {
  display: block;
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.selected-paper small {
  color: var(--text-muted);
  font-size: 11px;
}

.selected-paper-tags {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  gap: 10px;
}

.paper-status-badge {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  box-sizing: border-box;
  text-align: center;
}

.paper-status-badge.review-badge {
  color: #64748b;
  background: rgba(100, 116, 139, 0.06);
  border: 1.2px solid rgba(100, 116, 139, 0.25);
}
.paper-status-badge.review-badge.generated {
  color: #10b981;
  background: rgba(16, 185, 129, 0.08);
  border: 1.2px solid rgba(16, 185, 129, 0.35);
}
.paper-status-badge.review-badge.running {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  border: 1.2px solid rgba(59, 130, 246, 0.35);
}
.paper-status-badge.review-badge.failed {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.08);
  border: 1.2px solid rgba(239, 68, 68, 0.35);
}

.paper-status-badge.note-badge {
  font-weight: 700;
}
.paper-status-badge.note-badge.note-none {
  background: rgba(239, 68, 68, 0.06);
  color: #ef4444;
  border: 1.2px dashed rgba(239, 68, 68, 0.35);
}
.paper-status-badge.note-badge.note-ai {
  background: rgba(59, 130, 246, 0.08);
  color: #3b82f6;
  border: 1.2px solid rgba(59, 130, 246, 0.3);
}
.paper-status-badge.note-badge.note-manual {
  background: rgba(16, 185, 129, 0.08);
  color: #10b981;
  border: 1.2px solid rgba(16, 185, 129, 0.3);
}
.paper-status-badge.note-badge.note-both {
  background: rgba(139, 92, 246, 0.08);
  color: #8b5cf6;
  border: 1.2px solid rgba(139, 92, 246, 0.3);
}


.selected-paper.primary button {
  background: linear-gradient(135deg, #4f46e5, #ec4899);
  border: none;
  color: #fff;
}

.paper-review-progress {
  height: 4px;
  background: var(--item-border);
  border-radius: 4px;
  margin-top: 12px;
  overflow: hidden;
}

.paper-review-progress i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #a855f7);
}

.generation-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.generation-grid.single {
  grid-template-columns: 1fr;
}

.generation-action {
  display: flex;
  flex-direction: column;
  padding: 16px;
  position: relative;
  overflow: hidden;
}

.generation-action::before {
  content: "";
  position: absolute;
  top: 0; left: 0; right: 0; height: 3px;
  background: linear-gradient(90deg, #6366f1, #ec4899);
  opacity: 0.8;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-label span {
  color: var(--text-main);
  font-size: 13px;
  font-weight: 700;
}

.progress-label strong {
  color: var(--text-main);
  font-size: 18px;
}

.generation-step {
  color: var(--text-muted);
  font-size: 11px;
  line-height: 1.5;
  margin-bottom: 16px;
  flex: 1;
}

.generation-progress {
  height: 4px;
  background: var(--item-border);
  border-radius: 4px;
  margin-bottom: 16px;
  overflow: hidden;
}

.generation-progress i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #ec4899);
}

.soft-button, .primary-button, .download-button, .local-save-button, .confirm-link-button, .regenerate-deck-button, .preview-deck-button {
  width: 100%;
  padding: 10px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  border: none;
  text-align: center;
  box-sizing: border-box;
}

.soft-button {
  background: var(--btn-soft-bg);
  border: 1px solid var(--btn-soft-border);
  color: var(--btn-soft-text);
}
.soft-button:hover:not(:disabled) {
  background: rgba(99, 102, 241, 0.1);
}

.primary-button, .download-button {
  background: linear-gradient(135deg, #6366f1, #a855f7);
  color: #fff;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
}
.primary-button:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.5);
}

.local-save-button {
  background: rgba(20, 184, 166, 0.1);
  border: 1px solid rgba(20, 184, 166, 0.35);
  color: #0f766e;
}
:root[data-theme="dark"] .local-save-button {
  background: rgba(20, 184, 166, 0.14);
  color: #99f6e4;
}

.confirm-link-button {
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  border: 1px solid rgba(59, 130, 246, 0.3);
  text-decoration: none;
}
:root[data-theme="dark"] .confirm-link-button { color: #93c5fd; background: rgba(59, 130, 246, 0.2); }

.regenerate-deck-button {
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.28);
  color: #4f46e5;
}

.regenerate-deck-button:hover:not(:disabled) {
  background: rgba(99, 102, 241, 0.14);
  border-color: rgba(99, 102, 241, 0.46);
}

:root[data-theme="dark"] .regenerate-deck-button {
  background: rgba(129, 140, 248, 0.14);
  border-color: rgba(129, 140, 248, 0.34);
  color: #c4b5fd;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.deck-action-row {
  display: grid;
  gap: 8px;
}

/* --- Modals --- */
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(8px);
  display: grid;
  place-items: center;
  padding: 24px;
}

.modal-panel {
  width: min(980px, 100%);
  max-height: 85vh;
  background: var(--modal-bg);
  border: 1px solid var(--modal-border);
  border-radius: 24px;
  box-shadow: var(--modal-shadow);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-panel header {
  padding: 24px 30px;
  border-bottom: 1px solid var(--item-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--modal-header-bg);
}

.modal-panel header h2 {
  color: var(--text-main);
  font-size: 20px;
  margin: 0 0 6px;
}

.modal-panel header p {
  color: var(--text-muted);
  font-size: 13px;
  margin: 0;
}

.modal-panel header button {
  background: var(--btn-soft-bg);
  border: none;
  color: var(--text-muted);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
}
.modal-panel header button:hover { background: rgba(99, 102, 241, 0.15); color: var(--text-main); }

/* --- REDESIGNED MODAL ACTION BAR (Tabs + Actions) --- */
.review-modal-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 30px;
  border-bottom: 1px solid var(--item-border);
  background: var(--item-bg);
}

.modal-tab-group {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--btn-soft-bg);
  padding: 4px;
  border-radius: 999px;
  border: 1px solid var(--item-border);
}

.btn-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 16px;
  height: 34px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-tab:hover {
  color: var(--text-main);
}

.btn-tab.active {
  background: #ffffff;
  color: #4f46e5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.btn-tab-ai.active {
  background: linear-gradient(135deg, #6366f1, #a855f7);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
}

:root[data-theme="dark"] .btn-tab.active {
  background: rgba(255, 255, 255, 0.12);
  color: #a5b4fc;
}

:root[data-theme="dark"] .btn-tab-ai.active {
  background: linear-gradient(135deg, #6366f1, #a855f7);
  color: #ffffff;
}

.modal-right-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-action {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  height: 38px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  white-space: nowrap;
}

.btn-action svg {
  flex-shrink: 0;
}

.btn-read {
  background: var(--btn-soft-bg);
  border: 1px solid var(--btn-soft-border);
  color: var(--text-main);
}

.btn-read:hover:not(:disabled) {
  background: rgba(99, 102, 241, 0.1);
  transform: translateY(-1.5px);
}

/* --- Review Note Markdown View --- */
.review-note-markdown-view {
  padding: 24px 30px;
  overflow-y: auto;
  min-height: 320px;
  max-height: 60vh;
}

.rendered-note-container {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 16px;
  padding: 24px 28px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.rendered-note-content {
  color: var(--text-main);
  font-size: 14.5px;
  line-height: 1.8;
  word-break: break-word;
}

.rendered-note-content :deep(h1),
.rendered-note-content :deep(h2),
.rendered-note-content :deep(h3),
.rendered-note-content :deep(h4) {
  margin: 20px 0 10px;
  color: var(--text-main);
  font-weight: 800;
  line-height: 1.35;
}

.rendered-note-content :deep(h1) { font-size: 20px; border-bottom: 1px solid var(--item-border); padding-bottom: 8px; }
.rendered-note-content :deep(h2) { font-size: 17px; }
.rendered-note-content :deep(h3) { font-size: 15px; }

.rendered-note-content :deep(p) {
  margin: 10px 0;
}

.rendered-note-content :deep(ul),
.rendered-note-content :deep(ol) {
  padding-left: 24px;
  margin: 10px 0;
}

.rendered-note-content :deep(li) {
  margin: 6px 0;
}

.rendered-note-content :deep(strong) {
  color: #4338ca;
  font-weight: 700;
}
:root[data-theme="dark"] .rendered-note-content :deep(strong) {
  color: #a5b4fc;
}

.rendered-note-content :deep(blockquote) {
  margin: 14px 0;
  padding: 10px 16px;
  background: var(--btn-soft-bg);
  border-left: 4px solid #6366f1;
  border-radius: 0 8px 8px 0;
  color: var(--text-muted);
}

.rendered-note-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  font-size: 13.5px;
}

.rendered-note-content :deep(th),
.rendered-note-content :deep(td) {
  border: 1px solid var(--item-border);
  padding: 8px 12px;
  text-align: left;
}

.rendered-note-content :deep(th) {
  background: var(--btn-soft-bg);
  font-weight: 700;
}

.rendered-note-content :deep(img) {
  max-width: 100%;
  border-radius: 10px;
  margin: 14px 0;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}

.review-note-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon-wrap {
  font-size: 40px;
  margin-bottom: 12px;
}

.review-note-empty strong {
  font-size: 16px;
  color: var(--text-main);
  margin-bottom: 8px;
}

.review-note-empty p {
  font-size: 13.5px;
  color: var(--text-muted);
  max-width: 480px;
  line-height: 1.6;
  margin: 0;
}

.review-empty-state {
  display: flex;
  min-height: 260px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
  padding: 32px;
  text-align: center;
  color: var(--text-main);
}

.review-empty-state .empty-icon-wrap {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 12px;
  background: var(--btn-soft-bg);
  color: var(--accent);
  font-weight: 800;
}

.review-empty-state strong {
  font-size: 18px;
}

.review-empty-state p {
  max-width: 520px;
  margin: 0;
  color: var(--text-muted);
  line-height: 1.7;
}

.btn-action svg {
  flex-shrink: 0;
}

.btn-regenerate {
  background: linear-gradient(135deg, #6366f1, #a855f7);
  border: none;
  color: #ffffff;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.35);
}

.btn-regenerate:hover:not(:disabled) {
  transform: translateY(-1.5px);
  box-shadow: 0 8px 22px rgba(99, 102, 241, 0.5);
}

.btn-import {
  background: rgba(99, 102, 241, 0.12);
  border: 1px solid rgba(99, 102, 241, 0.3);
  color: #4f46e5;
}
:root[data-theme="dark"] .btn-import { color: #818cf8; background: rgba(99, 102, 241, 0.18); }

.btn-import:hover:not(:disabled) {
  background: rgba(99, 102, 241, 0.22);
  transform: translateY(-1.5px);
}

.btn-save {
  background: var(--btn-soft-bg);
  border: 1px solid var(--btn-soft-border);
  color: var(--text-main);
}

.btn-save:hover:not(:disabled) {
  background: rgba(99, 102, 241, 0.1);
  transform: translateY(-1.5px);
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.spin-icon {
  animation: spin 1s linear infinite;
}

.picker-tools {
  padding: 20px 30px 0;
  display: flex;
  gap: 16px;
}

.picker-tools input {
  flex: 1;
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: 10px;
  padding: 10px 16px;
  color: var(--input-text);
  outline: none;
}

.picker-tools input:focus {
  border-color: var(--input-focus);
}

.picker-paper-list, .review-point-list {
  padding: 24px 30px;
  overflow-y: auto;
  display: grid;
  gap: 16px;
}

.picker-paper-list {
  grid-template-columns: 1fr 1fr;
}

.picker-paper {
  background: var(--item-bg);
  border: 1px solid var(--item-border);
  border-radius: 14px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.picker-paper:hover {
  background: rgba(99, 102, 241, 0.05);
}

.picker-paper.selected {
  border-color: var(--input-focus);
  background: rgba(99, 102, 241, 0.08);
}

.picker-paper-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.picker-paper strong {
  color: var(--text-main);
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.picker-paper small {
  color: var(--text-muted);
  font-size: 12px;
}

.pdf-badge { padding: 3px 8px; border-radius: 6px; font-size: 10px; font-weight: 700; }
.pdf-badge.ready { background: rgba(16, 185, 129, 0.15); color: #059669; }
:root[data-theme="dark"] .pdf-badge.ready { color: #34d399; }
.pdf-badge.missing { background: rgba(245, 158, 11, 0.15); color: #d97706; }
:root[data-theme="dark"] .pdf-badge.missing { color: #fbbf24; }

.selected-check { background: #6366f1; color: #fff; padding: 2px 8px; border-radius: 6px; font-size: 11px; }

/* --- REVIEW MODAL POINTS (Multi-color Restored & Premium) --- */
.review-point {
  border-radius: 16px;
  padding: 20px;
  display: grid;
  grid-template-columns: 190px 1fr;
  gap: 20px;
  border: 1px solid var(--item-border);
  position: relative;
  transition: all 0.25s ease;
}

.review-point:nth-child(4n + 1) { background: var(--point-1-bg); }
.review-point:nth-child(4n + 1) strong { color: var(--point-1-text); }
.review-point:nth-child(4n + 2) { background: var(--point-2-bg); }
.review-point:nth-child(4n + 2) strong { color: var(--point-2-text); }
.review-point:nth-child(4n + 3) { background: var(--point-3-bg); }
.review-point:nth-child(4n + 3) strong { color: var(--point-3-text); }
.review-point:nth-child(4n + 4) { background: var(--point-4-bg); }
.review-point:nth-child(4n + 4) strong { color: var(--point-4-text); }

/* Unified Premium styling for dark mode */
:root[data-theme="dark"] .review-point {
  background: rgba(15, 23, 42, 0.4) !important;
  border: 1px solid rgba(255, 255, 255, 0.05) !important;
}
:root[data-theme="dark"] .review-point:nth-child(4n + 1) { border-left: 4px solid var(--point-1-text) !important; }
:root[data-theme="dark"] .review-point:nth-child(4n + 2) { border-left: 4px solid var(--point-2-text) !important; }
:root[data-theme="dark"] .review-point:nth-child(4n + 3) { border-left: 4px solid var(--point-3-text) !important; }
:root[data-theme="dark"] .review-point:nth-child(4n + 4) { border-left: 4px solid var(--point-4-text) !important; }

/* Status / Model pill badge on the left meta column */
.review-point-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 14px;
  padding: 4px 10px;
  border-radius: 99px;
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
  font-size: 10px;
  color: var(--text-muted);
  width: fit-content;
  font-weight: 700;
  letter-spacing: 0.02em;
}
.review-point-badge .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.6);
}
:root[data-theme="dark"] .review-point-badge {
  background: rgba(255, 255, 255, 0.04) !important;
  border-color: rgba(255, 255, 255, 0.05) !important;
  color: #94a3b8 !important;
}
:root[data-theme="dark"] .review-point-badge .dot {
  background: #34d399 !important;
  box-shadow: 0 0 10px rgba(52, 211, 153, 0.8) !important;
}

.review-point strong { font-size: 15px; display: block; font-weight: 800; }
.review-point small { color: var(--text-muted); font-size: 12px; display: block; margin-top: 6px; }

/* Review Rich Editor & English/Numbers highlighting */
.review-rich-editor {
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: 12px;
  padding: 16px;
  color: var(--input-text);
  font-size: 14px;
  line-height: 1.7;
  outline: none;
  font-family: inherit;
}
.review-rich-editor:focus { border-color: var(--input-focus); box-shadow: 0 0 0 3px var(--input-shadow); }

.review-rich-editor :deep(.review-inline-heading) {
  display: inline-block;
  margin: 12px 0 6px;
  color: #1e40af;
  font-weight: 800;
  font-family: inherit;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.2;
}
:root[data-theme="dark"] .review-rich-editor :deep(.review-inline-heading) {
  color: #38bdf8 !important;
  background: rgba(56, 189, 248, 0.12) !important;
  border: 1px solid rgba(56, 189, 248, 0.25) !important;
}

.review-rich-editor :deep(.review-inline-heading:first-child) { margin-top: 0; }

.review-rich-editor :deep(.review-number) {
  color: #0d9488;
  font-weight: 800;
  padding: 0 2px;
}
:root[data-theme="dark"] .review-rich-editor :deep(.review-number) {
  color: #34d399 !important;
}

:root[data-theme="dark"] .review-rich-editor {
  background: rgba(15, 23, 42, 0.45) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  color: #cbd5e1 !important;
}

.copy-section-button {
  position: absolute; right: 20px; top: 20px;
  background: var(--btn-soft-bg); border: 1px solid var(--item-border); border-radius: 8px;
  width: 32px; height: 32px; color: var(--text-muted); cursor: pointer;
  display: grid; place-items: center;
  transition: all 0.2s;
}
.copy-section-button:hover { background: rgba(99, 102, 241, 0.15); color: var(--text-main); }
.copy-section-button svg { width: 16px; height: 16px; fill: none; stroke: currentColor; stroke-width: 1.8; }

.review-loading { padding: 20px 30px; }
.wide-progress { height: 6px; background: var(--item-border); border-radius: 6px; overflow: hidden; }
.wide-progress i { display: block; height: 100%; background: linear-gradient(90deg, #6366f1, #ec4899); }

.meeting-toast {
  position: fixed; bottom: 30px; left: 50%; transform: translateX(-50%);
  background: rgba(15, 23, 42, 0.95); color: #f8fafc;
  padding: 12px 20px; border-radius: 12px; font-size: 14px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.1);
  z-index: 200; backdrop-filter: blur(8px);
}

/* RESPONSIVE */
@media (max-width: 1024px) {
  .timeline-stats-bar { grid-template-columns: repeat(2, 1fr); }
  .meeting-card-body { grid-template-columns: 1fr; }
  .timeline-list { padding-left: 30px; gap: 30px; }
  .timeline-list::before { left: 5px; }
  .timeline-pin { left: -40px; width: 24px; height: 24px; }
  .timeline-pin span { width: 8px; height: 8px; }
  .review-point { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .timeline-header,
  .timeline-stats-bar,
  .timeline-shell {
    padding-inline: 16px;
  }
  .timeline-stats-bar { grid-template-columns: 1fr; }
  .deck-toolbar-nav { grid-template-columns: 1fr; }
  .meeting-timeline-page { padding: 20px 16px 60px; }
  .timeline-header { flex-direction: column; }
  .meeting-card { padding: 20px; }
  .meeting-detail-grid, .generation-grid, .picker-paper-list { grid-template-columns: 1fr; }
  .review-modal-actions { flex-direction: column; align-items: stretch; }
}

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s, transform 0.2s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.98); }

/* --- PREMIUM HORIZONTAL SCHEDULE BADGE --- */
.meeting-date-block-premium {
  display: inline-flex;
  align-items: center;
  gap: 16px;
  background: rgba(30, 41, 59, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.06);
  padding: 8px 16px;
  border-radius: 12px;
  backdrop-filter: blur(16px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  transition: all 0.25s ease;
}

:root[data-theme="light"] .meeting-date-block-premium {
  background: #ffffff;
  border-color: #cbd5e1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.meeting-date-block-premium:hover {
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: 0 4px 24px rgba(99, 102, 241, 0.06);
}

/* Date Info Group */
.date-info-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.calendar-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(99, 102, 241, 0.12);
  color: #818cf8;
}

:root[data-theme="light"] .calendar-icon-wrapper {
  background: #eff6ff;
  color: #2563eb;
}

.calendar-svg {
  width: 16px;
  height: 16px;
}

.date-text-wrapper {
  display: flex;
  flex-direction: column;
}

.date-main-label {
  font-size: 10px;
  color: var(--text-muted);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  line-height: 1.2;
}

.date-sub-value {
  font-size: 13px;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.3;
}

:root[data-theme="light"] .date-sub-value {
  color: #1e293b;
}

/* Divider */
.time-divider {
  width: 1px;
  height: 24px;
  background: rgba(255, 255, 255, 0.1);
}

:root[data-theme="light"] .time-divider {
  background: #e2e8f0;
}

/* Time Info Group */
.time-info-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time-clock-value {
  font-size: 16px;
  font-weight: 800;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  color: #ffffff;
  letter-spacing: -0.02em;
}

:root[data-theme="light"] .time-clock-value {
  color: #0f172a;
}

/* Countdown Badge */
.countdown-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 99px;
  letter-spacing: 0.02em;
}

.countdown-badge.status-draft {
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-muted);
}

.countdown-badge.status-active {
  background: rgba(16, 185, 129, 0.12);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.2);
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.15);
  animation: pulse-green 2s infinite;
}

.countdown-badge.status-past {
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-muted);
}

.countdown-badge.status-soon {
  background: rgba(239, 68, 68, 0.12);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.2);
  box-shadow: 0 0 8px rgba(239, 68, 68, 0.15);
  animation: pulse-red 2s infinite;
}

.countdown-badge.status-future {
  background: rgba(99, 102, 241, 0.12);
  color: #818cf8;
  border: 1px solid rgba(99, 102, 241, 0.2);
}

@keyframes pulse-green {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}
@keyframes pulse-red {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

/* Action Edit Button */
.btn-time-edit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #cbd5e1;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

:root[data-theme="light"] .btn-time-edit {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: #475569;
}

.btn-time-edit:hover {
  background: rgba(99, 102, 241, 0.15);
  border-color: rgba(99, 102, 241, 0.3);
  color: #ffffff;
}

:root[data-theme="light"] .btn-time-edit:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-time-edit svg {
  opacity: 0.8;
}

.picker-trigger-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
}

.hidden-datetime-picker {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

/* --- PREMIUM TEXTAREA GLOWS & LABELS --- */
.meeting-notes textarea,
.meeting-detail-field textarea,
.meeting-advisor-note textarea {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.meeting-notes textarea:focus,
.meeting-detail-field textarea:focus {
  border-color: rgba(99, 102, 241, 0.6) !important;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1), 0 0 0 4px rgba(99, 102, 241, 0.12) !important;
  background: rgba(255, 255, 255, 0.02) !important;
}

.meeting-advisor-note {
  border: 1px solid rgba(245, 158, 11, 0.25) !important;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.03), rgba(217, 119, 6, 0.05)) !important;
  box-shadow: 0 4px 20px rgba(245, 158, 11, 0.03) !important;
  transition: all 0.3s ease;
}
.meeting-advisor-note:focus-within {
  border-color: rgba(245, 158, 11, 0.5) !important;
  box-shadow: 0 4px 20px rgba(245, 158, 11, 0.08) !important;
}
.meeting-advisor-note textarea:focus {
  border-color: rgba(245, 158, 11, 0.5) !important;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1), 0 0 0 4px rgba(245, 158, 11, 0.15) !important;
}

/* --- CUSTOM DATETIME PICKER MODAL --- */
.picker-modal-backdrop {
  z-index: 300;
  display: flex;
  justify-content: center;
  align-items: center;
}

.custom-picker-card {
  width: 320px;
  background: rgba(30, 41, 59, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  animation: scale-up 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

:root[data-theme="light"] .custom-picker-card {
  background: #ffffff;
  border-color: #cbd5e1;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
}

@keyframes scale-up {
  from { transform: scale(0.95); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.picker-card-header {
  margin-bottom: 20px;
  text-align: center;
}

.picker-card-header h3 {
  font-size: 18px;
  font-weight: 800;
  color: #ffffff;
  margin: 0 0 6px 0;
}
:root[data-theme="light"] .picker-card-header h3 {
  color: #0f172a;
}

.picker-card-header p {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
}

.picker-card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.picker-field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.picker-field-group label {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-icon .icon {
  position: absolute;
  left: 14px;
  display: flex;
  align-items: center;
  color: var(--text-muted);
  pointer-events: none;
}

.input-with-icon input {
  width: 100%;
  box-sizing: border-box;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #ffffff;
  padding: 10px 14px 10px 38px;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  font-family: inherit;
  transition: all 0.2s ease;
}

:root[data-theme="light"] .input-with-icon input {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #0f172a;
}

.input-with-icon input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}

.picker-card-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn-picker-cancel,
.btn-picker-confirm {
  padding: 10px 20px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  font-family: inherit;
}

.btn-picker-cancel {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  color: var(--text-muted);
}
.btn-picker-cancel:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

:root[data-theme="light"] .btn-picker-cancel {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: #475569;
}
:root[data-theme="light"] .btn-picker-cancel:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-picker-confirm {
  background: linear-gradient(135deg, #6366f1, #a855f7);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.2);
}
.btn-picker-confirm:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.3);
}

/* --- TIMELINE EMPTY STATE --- */
.timeline-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  text-align: center;
  background: rgba(30, 41, 59, 0.2);
  border: 1px dashed rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  margin: 20px 0;
  backdrop-filter: blur(12px);
}

:root[data-theme="light"] .timeline-empty-state {
  background: rgba(255, 255, 255, 0.6);
  border-color: #cbd5e1;
}

.empty-icon-box {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: rgba(99, 102, 241, 0.08);
  color: #818cf8;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.05);
}

:root[data-theme="light"] .empty-icon-box {
  background: #eff6ff;
  color: #2563eb;
}

.empty-svg {
  opacity: 0.8;
}

.timeline-empty-state h2 {
  font-size: 20px;
  font-weight: 800;
  color: #ffffff;
  margin: 0 0 10px 0;
  letter-spacing: -0.01em;
}

:root[data-theme="light"] .timeline-empty-state h2 {
  color: #0f172a;
}

.timeline-empty-state p {
  font-size: 14px;
  color: var(--text-muted);
  max-width: 380px;
  line-height: 1.6;
  margin: 0 0 28px 0;
}

.btn-empty-add {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #6366f1, #a855f7);
  color: #ffffff;
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  font-family: inherit;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.2);
}

.btn-empty-add:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 30px rgba(99, 102, 241, 0.3);
}

.btn-empty-add:active {
  transform: translateY(0);
}

.btn-empty-add span {
  font-size: 18px;
  line-height: 1;
}

/* --- PPT PARAMS MODAL --- */
.ppt-params-modal-box {
  max-width: 580px;
  width: 90vw;
  background: #111827;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  color: #f3f4f6;
}

:root[data-theme="light"] .ppt-params-modal-box {
  background: #ffffff;
  border-color: #e2e8f0;
  color: #1e293b;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
}

.modal-badge-tag {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #818cf8;
  background: rgba(99, 102, 241, 0.12);
  padding: 3px 8px;
  border-radius: 6px;
  display: inline-block;
  margin-bottom: 6px;
}

.modal-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  padding-bottom: 16px;
}

:root[data-theme="light"] .modal-head {
  border-color: #f1f5f9;
}

.modal-head-titles h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.modal-close-btn {
  background: transparent;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  padding: 4px;
  border-radius: 8px;
  transition: all 0.15s;
}

.modal-close-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.ppt-params-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-bottom: 24px;
}

.ppt-param-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ppt-param-label {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.ppt-param-label span {
  font-size: 13px;
  font-weight: 700;
  color: #e5e7eb;
}

:root[data-theme="light"] .ppt-param-label span {
  color: #334155;
}

.ppt-param-label small {
  font-size: 12px;
  color: #9ca3af;
}

.ppt-radio-group {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.ppt-radio-btn {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1.5px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  color: inherit;
  font-family: inherit;
}

:root[data-theme="light"] .ppt-radio-btn {
  background: #f8fafc;
  border-color: #e2e8f0;
}

.ppt-radio-btn strong {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 3px;
  color: #f3f4f6;
}

:root[data-theme="light"] .ppt-radio-btn strong {
  color: #0f172a;
}

.ppt-radio-btn span {
  font-size: 11px;
  color: #9ca3af;
  line-height: 1.35;
}

.ppt-radio-btn:hover {
  border-color: rgba(99, 102, 241, 0.4);
  background: rgba(99, 102, 241, 0.06);
}

.ppt-radio-btn.active {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.12);
  box-shadow: 0 0 0 1px #6366f1;
}

.ppt-radio-btn.active strong {
  color: #818cf8;
}

:root[data-theme="light"] .ppt-radio-btn.active strong {
  color: #4f46e5;
}

.ppt-param-field-checkbox {
  padding-top: 6px;
}

.ppt-checkbox-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #d1d5db;
  cursor: pointer;
}

:root[data-theme="light"] .ppt-checkbox-label {
  color: #475569;
}

.ppt-checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #6366f1;
  cursor: pointer;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding-top: 18px;
}

:root[data-theme="light"] .modal-footer {
  border-color: #f1f5f9;
}

.btn-cancel {
  padding: 10px 18px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #9ca3af;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-cancel:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

:root[data-theme="light"] .btn-cancel {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: #475569;
}

.btn-confirm-start {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-confirm-start:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

.preview-deck-button {
  border: 1px solid #8b5cf6;
  background: rgba(139, 92, 246, 0.09);
  color: #6d4ce7;
}

.deck-preview-backdrop {
  z-index: 1200;
  padding: 24px;
}

.deck-preview-modal {
  width: min(1180px, 96vw);
  max-height: 94vh;
  overflow: hidden;
  border: 1px solid var(--item-border);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.34);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.deck-preview-modal > header {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--item-border);
}

.deck-preview-modal > header > div:first-child {
  min-width: 0;
  flex: 1;
}

.deck-preview-modal > header span {
  color: #7c3aed;
  font-size: 11px;
  font-weight: 800;
}

.deck-preview-modal > header h2 {
  margin: 3px 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 18px;
}

.deck-preview-counter {
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.deck-preview-close,
.deck-preview-nav {
  border: 0;
  background: transparent;
  color: var(--text);
  cursor: pointer;
}

.deck-preview-close {
  width: 36px;
  height: 36px;
  font-size: 26px;
}

.deck-preview-stage {
  position: relative;
  min-height: 0;
  padding: 20px 64px;
  display: grid;
  place-items: center;
  background: #111827;
}

.deck-preview-loading {
  min-height: 420px;
  display: grid;
  place-items: center;
  background: #111827;
  color: #e5e7eb;
  font-size: 14px;
  font-weight: 700;
}

.deck-preview-stage > img {
  display: block;
  width: 100%;
  max-height: calc(94vh - 190px);
  aspect-ratio: 16 / 9;
  object-fit: contain;
  background: #fff;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.3);
}

.deck-preview-nav {
  position: absolute;
  top: 50%;
  width: 44px;
  height: 64px;
  transform: translateY(-50%);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 38px;
}

.deck-preview-nav.previous { left: 10px; }
.deck-preview-nav.next { right: 10px; }
.deck-preview-nav:disabled { opacity: 0.22; cursor: default; }

.deck-preview-thumbnails {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  overflow-x: auto;
  border-top: 1px solid var(--item-border);
}

.deck-preview-thumbnails button {
  position: relative;
  flex: 0 0 128px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 5px;
  overflow: hidden;
  background: #111827;
  cursor: pointer;
}

.deck-preview-thumbnails button.active { border-color: #7c3aed; }
.deck-preview-thumbnails img { display: block; width: 100%; aspect-ratio: 16 / 9; object-fit: cover; }
.deck-preview-thumbnails span { position: absolute; right: 4px; bottom: 3px; padding: 1px 5px; border-radius: 3px; background: rgba(0, 0, 0, 0.7); color: #fff; font-size: 10px; }

@media (max-width: 700px) {
  .deck-preview-backdrop { padding: 8px; }
  .deck-preview-stage { padding: 12px 42px; }
  .deck-preview-thumbnails button { flex-basis: 92px; }
}
</style>
