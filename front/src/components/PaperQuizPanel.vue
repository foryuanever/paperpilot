<template>
  <section class="quiz-overlay" role="dialog" aria-modal="true" aria-labelledby="quiz-title">
    <!-- Top Header -->
    <header class="quiz-header">
      <div class="quiz-header-left">
        <div class="quiz-brand-pill">
          <svg class="quiz-sparkle-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M12 2L14.5 9.5L22 12L14.5 14.5L12 22L9.5 14.5L2 12L9.5 9.5L12 2Z" fill="url(#quizAiGlowGrad)" stroke="none"/>
            <defs>
              <linearGradient id="quizAiGlowGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#818cf8"/>
                <stop offset="100%" stop-color="#3b82f6"/>
              </linearGradient>
            </defs>
          </svg>
          <span>学术自测 · AI 智能考卷</span>
        </div>
        <div class="quiz-title-box">
          <h2 id="quiz-title" class="quiz-main-title">学术自测</h2>
          <span class="quiz-paper-subtitle" :title="title">{{ title || '当前研读文献' }}</span>
        </div>
      </div>

      <div class="quiz-header-right">
        <!-- Graded Score Badge -->
        <div v-if="quiz?.result" class="quiz-score-pill">
          <span class="quiz-score-badge" :class="scoreGradeClass(quiz.result.score)">
            {{ scoreGradeLabel(quiz.result.score) }}
          </span>
          <div class="quiz-score-numbers">
            <strong class="quiz-score-main">{{ quiz.result.score }}</strong>
            <span class="quiz-score-total">/ 100 分</span>
          </div>
        </div>

        <!-- Answering Progress Bar -->
        <div v-else-if="quiz" class="quiz-progress-pill">
          <div class="quiz-progress-info">
            <span>作答进度</span>
            <strong>{{ answeredCount }} / {{ totalQuestionCount }}</strong>
          </div>
          <div class="quiz-progress-bar-bg">
            <div class="quiz-progress-bar-fill" :style="{ width: `${(answeredCount / totalQuestionCount) * 100}%` }"></div>
          </div>
        </div>

        <!-- Retake Button when graded -->
        <button
          v-if="quiz?.result"
          type="button"
          class="quiz-btn-retake"
          title="重新生成试题并再次测试"
          :disabled="busy"
          @click="retakeQuiz"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/>
          </svg>
          <span>再次测试</span>
        </button>

        <!-- Back to Reading Button -->
        <button type="button" class="quiz-btn-close" title="退出学术自测并返回阅读 (Esc)" @click="$emit('close')">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
          <span>返回阅读</span>
        </button>
      </div>
    </header>

    <!-- Main Workspace Split Container -->
    <div class="quiz-split-layout">
      <!-- Left: Exam & Question Area -->
      <section class="quiz-exam-pane">
        <!-- Banner Intro & Jump Strip -->
        <div class="quiz-banner-area">
          <div class="quiz-summary-strip">
            <div class="quiz-summary-tags">
              <span class="summary-tag">10 道单选 · 50分</span>
              <span class="summary-tag">5 道判断 · 20分</span>
              <span class="summary-tag">2 道简答 · 30分</span>
              <span class="summary-tag total">满分 100 分</span>
            </div>
            <div class="quiz-tip-text">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
              <span>{{ quiz?.result ? '点击“查看本题原文依据与出处”可在右侧即时比对解析正文与段落证据' : '题目严格基于论文提取；本地自动暂存作答内容' }}</span>
            </div>
          </div>

          <!-- Question Jump Navigator (1-17) -->
          <nav v-if="quiz" class="quiz-jump-nav" aria-label="题目快捷跳转">
            <div class="jump-section-group">
              <span class="jump-group-label">单选 (1-10)</span>
              <div class="jump-pills">
                <button
                  v-for="idx in 10"
                  :key="idx"
                  type="button"
                  class="jump-pill"
                  :class="getQuestionJumpClass(idx)"
                  @click="scrollToQuestion(idx)"
                >
                  {{ idx }}
                </button>
              </div>
            </div>

            <div class="jump-section-group">
              <span class="jump-group-label">判断 (11-15)</span>
              <div class="jump-pills">
                <button
                  v-for="idx in [11, 12, 13, 14, 15]"
                  :key="idx"
                  type="button"
                  class="jump-pill"
                  :class="getQuestionJumpClass(idx)"
                  @click="scrollToQuestion(idx)"
                >
                  {{ idx }}
                </button>
              </div>
            </div>

            <div class="jump-section-group">
              <span class="jump-group-label">简答 (16-17)</span>
              <div class="jump-pills">
                <button
                  v-for="idx in [16, 17]"
                  :key="idx"
                  type="button"
                  class="jump-pill"
                  :class="getQuestionJumpClass(idx)"
                  @click="scrollToQuestion(idx)"
                >
                  {{ idx }}
                </button>
              </div>
            </div>
          </nav>
        </div>

        <!-- Initial Start / Loading / Error State -->
        <div v-if="loading" class="quiz-state-box loading">
          <div class="quiz-loader-ring"></div>
          <p>正在读取学术自测信息…</p>
        </div>

        <div v-else-if="error" class="quiz-state-box error" role="alert">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <div class="error-msg-wrap">
            <strong>学术自测加载失败</strong>
            <p>{{ error }}</p>
          </div>
          <button type="button" class="quiz-btn-primary" @click="generate">重试生成</button>
        </div>

        <div v-else-if="!quiz" class="quiz-state-box start">
          <div class="start-icon-shield">
            <svg width="42" height="42" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><polyline points="9 12 11 14 15 10"/></svg>
          </div>
          <h3>检验你对这篇文献的深度理解</h3>
          <p class="start-desc">
            AI 教学助手将依据已解析的论文正文（研究目标、实验方法、核心证据与结论），智能生成 17 道考查题目，并在提交后提供逐题得分、命题依据与原文对应段落。
          </p>
          <div class="start-meta-list">
            <div class="meta-item"><span class="meta-dot"></span> 消耗 2 积分（含生成与完整智能评阅）</div>
            <div class="meta-item"><span class="meta-dot"></span> 生成失败不扣除任何积分</div>
            <div class="meta-item"><span class="meta-dot"></span> 答题进度实时保存在本地，支持多端对照</div>
          </div>
          <button type="button" class="quiz-btn-primary generate-btn" :disabled="busy" @click="generate">
            <span v-if="busy" class="btn-spinner"></span>
            <span>{{ busy ? '正在生成学术自测试题并核验题目出处…' : '开始学术自测 · 消耗 2 积分' }}</span>
          </button>
        </div>

        <!-- Question Form Area -->
        <form v-else class="quiz-form" @submit.prevent="submit">
          <article
            v-for="(q, index) in displayedQuestions"
            :key="q.id"
            :id="`quiz-question-${index + 1}`"
            class="quiz-card"
            :class="{ 'card-graded': !!quiz.result, 'is-active-source': selectedSource?.question?.id === q.id }"
          >
            <!-- Card Header -->
            <div class="quiz-card-head">
              <div class="quiz-card-tags">
                <span class="q-number-badge">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="q-type-badge">{{ typeLabel(q.type) }}</span>
                <span class="q-score-badge">{{ q.type === 'choice' ? 5 : q.type === 'boolean' ? 4 : 15 }} 分</span>
              </div>
              <div v-if="quiz.result" class="q-graded-badge" :class="getQuestionScoreClass(q)">
                <template v-if="q.type === 'short'">
                  得分 {{ q.score }} / {{ q.maxScore || 15 }} 分
                </template>
                <template v-else-if="q.score > 0">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
                  回答正确 (+{{ q.score }}分)
                </template>
                <template v-else>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  回答错误 (0分)
                </template>
              </div>
              <span v-else-if="answers[q.id]" class="q-answered-tag">已答</span>
            </div>

            <!-- Question Prompt Title -->
            <h3 class="quiz-card-prompt">{{ q.prompt }}</h3>

            <!-- Question Options / Inputs -->
            <!-- 1. Choice Options (ABCD) -->
            <div v-if="q.type === 'choice'" class="quiz-options-group choice">
              <label
                v-for="(option, i) in q.options"
                :key="i"
                class="quiz-option-item"
                :class="getOptionClass(q, 'ABCD'[i])"
              >
                <input
                  type="radio"
                  :name="`q-${q.id}`"
                  :value="'ABCD'[i]"
                  v-model="answers[q.id]"
                  :disabled="busy || !!quiz.result"
                  class="quiz-hidden-input"
                />
                <span class="quiz-option-badge">{{ 'ABCD'[i] }}</span>
                <span class="quiz-option-text">{{ cleanOptionText(option, i) }}</span>
                <span v-if="quiz.result && q.answer === 'ABCD'[i]" class="quiz-ans-tag correct">正确答案</span>
                <span v-else-if="quiz.result && answers[q.id] === 'ABCD'[i] && q.answer !== 'ABCD'[i]" class="quiz-ans-tag wrong">你的选择</span>
              </label>
            </div>

            <!-- 2. True / False Options (判断题) -->
            <div v-else-if="q.type === 'boolean'" class="quiz-options-group boolean">
              <label
                class="quiz-option-item boolean-item"
                :class="getBooleanOptionClass(q, 'true')"
              >
                <input
                  type="radio"
                  :name="`q-${q.id}`"
                  value="true"
                  v-model="answers[q.id]"
                  :disabled="busy || !!quiz.result"
                  class="quiz-hidden-input"
                />
                <span class="quiz-option-badge">T</span>
                <span class="quiz-option-text bold-label">正确 (True)</span>
                <span v-if="quiz.result && q.answer === 'true'" class="quiz-ans-tag correct">正确答案</span>
                <span v-else-if="quiz.result && answers[q.id] === 'true' && q.answer !== 'true'" class="quiz-ans-tag wrong">你的选择</span>
              </label>

              <label
                class="quiz-option-item boolean-item"
                :class="getBooleanOptionClass(q, 'false')"
              >
                <input
                  type="radio"
                  :name="`q-${q.id}`"
                  value="false"
                  v-model="answers[q.id]"
                  :disabled="busy || !!quiz.result"
                  class="quiz-hidden-input"
                />
                <span class="quiz-option-badge">F</span>
                <span class="quiz-option-text bold-label">错误 (False)</span>
                <span v-if="quiz.result && q.answer === 'false'" class="quiz-ans-tag correct">正确答案</span>
                <span v-else-if="quiz.result && answers[q.id] === 'false' && q.answer !== 'false'" class="quiz-ans-tag wrong">你的选择</span>
              </label>
            </div>

            <!-- 3. Short Answer Textarea (简答题) -->
            <div v-else class="quiz-short-group">
              <textarea
                v-model="answers[q.id]"
                :disabled="busy || !!quiz.result"
                rows="5"
                maxlength="6000"
                class="quiz-textarea"
                placeholder="请结合文献的研究方法、实验数据或讨论结论，用自己的语言进行总结回答…"
                :aria-label="`第 ${index + 1} 题作答内容`"
              ></textarea>
              <div class="quiz-textarea-footer">
                <span class="char-counter">已输入 {{ (answers[q.id] || '').length }} / 6000 字</span>
              </div>
            </div>

            <!-- Graded Feedback & Reference Answer -->
            <div v-if="quiz.result" class="quiz-feedback-box">
              <div class="feedback-row ans-row">
                <span class="feedback-lbl">参考答案：</span>
                <strong class="ans-value">{{ answerLabel(q.answer) }}</strong>
              </div>
              <div v-if="q.feedback" class="feedback-row rubric-row">
                <span class="feedback-lbl">阅卷评语：</span>
                <p class="feedback-text">{{ q.feedback }}</p>
              </div>
              <div v-if="q.explanation" class="feedback-row expl-row">
                <span class="feedback-lbl">解析与评分要点：</span>
                <p class="feedback-text">{{ q.explanation }}</p>
              </div>
              <div v-if="q.quote" class="feedback-quote-row">
                <span class="feedback-lbl">论文原文证据：</span>
                <blockquote class="cited-quote">“{{ q.quote }}”</blockquote>
              </div>
            </div>

            <!-- Source Inspection Action Button -->
            <div v-if="quiz.result" class="quiz-card-footer">
              <button
                type="button"
                class="quiz-btn-source-inspect"
                :class="{ active: selectedSource?.question?.id === q.id }"
                @click="inspectSource(q)"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                <span>查看本题原文依据与出处</span>
              </button>
            </div>
          </article>

          <!-- Bottom Sticky Submit Bar -->
          <footer v-if="!quiz.result" class="quiz-submit-bar">
            <div class="submit-bar-info">
              <span>已作答 <strong>{{ answeredCount }}</strong> / {{ totalQuestionCount }} 题</span>
              <small v-if="answeredCount < totalQuestionCount">全部作答完毕后即可提交智能评阅</small>
              <small v-else class="ready-text">✓ 全部题目已作答完毕，准备提交</small>
            </div>
            <button
              type="submit"
              class="quiz-btn-primary submit-btn"
              :disabled="busy || answeredCount !== totalQuestionCount"
            >
              <span v-if="busy" class="btn-spinner"></span>
              <span>{{ busy ? '正在智能评阅中，请稍候…' : '提交自测并评阅' }}</span>
            </button>
          </footer>
        </form>
      </section>

      <!-- Right: Source Evidence & Parsed Document Viewer -->
      <aside class="quiz-source-pane" aria-label="原文出处与文献视图">
        <header class="source-pane-head">
          <div class="source-pane-title">
            <span class="source-kicker">SOURCE & EVIDENCE</span>
            <h3>原文出处与文献对照</h3>
          </div>
          <!-- Segmented Tab Bar (2 Tabs) -->
          <div class="source-tab-segmented">
            <button
              type="button"
              class="source-tab-btn"
              :class="{ active: rightTab === 'evidence' }"
              @click="rightTab = 'evidence'"
            >
              出处依据
            </button>
            <button
              type="button"
              class="source-tab-btn"
              :class="{ active: rightTab === 'parsed' }"
              @click="rightTab = 'parsed'"
            >
              解析正文
            </button>
          </div>
        </header>

        <!-- Right Content Body -->
        <div class="source-pane-body">
          <!-- Tab 1: Evidence & Analysis View -->
          <div v-if="rightTab === 'evidence'" class="source-view-evidence">
            <div v-if="selectedSource" class="evidence-content-wrap">
              <div class="evidence-top-card">
                <div class="evidence-meta-pill">
                  <span class="evidence-num">第 {{ selectedSource.question?.id || '—' }} 题 出处依据</span>
                  <span v-if="matchedPageNumber" class="evidence-page">位于原论文 第 {{ matchedPageNumber }} 页</span>
                </div>
                <div class="evidence-prompt-summary">
                  <strong>题目：</strong> {{ selectedSource.question?.prompt }}
                </div>
              </div>

              <!-- Quoted Evidence Box -->
              <div class="evidence-quote-section">
                <div class="section-title-bar">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>
                  <span>论文原文关键依据</span>
                  <button type="button" class="btn-copy-quote" title="复制引用句" @click="copyQuote(selectedSource.quote)">
                    {{ quoteCopied ? '已复制' : '复制' }}
                  </button>
                </div>
                <blockquote class="evidence-direct-quote">
                  “{{ selectedSource.quote }}”
                </blockquote>
              </div>

              <!-- Surrounding Full Paragraph Box with Quote Highlighted -->
              <div class="evidence-paragraph-section">
                <div class="section-title-bar">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
                  <span>所属上下文段落</span>
                </div>
                <div
                  class="evidence-full-paragraph"
                  v-html="highlightQuoteInText(sourceParagraphText(selectedSource), selectedSource.quote)"
                ></div>
              </div>

              <!-- Jump to Parsed View Action Button -->
              <div class="evidence-actions">
                <button
                  type="button"
                  class="quiz-btn-primary locate-in-parsed-btn"
                  @click="locateAndScrollInParsedView"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                  <span>在完整解析正文中高亮定位此段</span>
                </button>
              </div>
            </div>

            <!-- Empty State -->
            <div v-else class="source-empty-state">
              <div class="empty-icon-compass">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="10"/><polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/></svg>
              </div>
              <h4>选择题目以查看原文出处依据</h4>
              <p>点击左侧任一题目下方的<strong>“查看本题原文依据与出处”</strong>，此处将实时展示该题对应的论文关键证据句、段落上下文及解析。</p>
            </div>
          </div>

          <!-- Tab 2: Structured Parsed Document View -->
          <div v-else ref="parsedDocContainer" class="source-view-parsed">
            <div class="parsed-doc-topbar">
              <span>全文解析结构视图</span>
              <small v-if="selectedSource">正在追踪 第 {{ selectedSource.question?.id }} 题依据</small>
            </div>
            <div class="parsed-pages-stream">
              <!-- Render pages and blocks -->
              <template v-if="pages && pages.length">
                <section
                  v-for="page in pages"
                  :key="page.pageNumber"
                  class="parsed-page-box"
                >
                  <div class="parsed-page-divider">Page {{ page.pageNumber }}</div>
                  <div
                    v-for="block in page.blocks"
                    :key="block.id"
                    :id="`quiz-parsed-block-${block.id}`"
                    class="parsed-block"
                    :class="{
                      'is-target-highlight': isBlockTargetSource(block),
                      'is-heading': block.kind === 'heading',
                      'is-equation': block.kind === 'equation',
                      'is-figure': block.kind === 'figure' || block.kind === 'table'
                    }"
                  >
                    <h4 v-if="block.kind === 'heading'" class="parsed-heading-text">{{ block.text }}</h4>
                    <!-- Figure / Table Card -->
                    <figure
                      v-else-if="block.kind === 'figure' || block.kind === 'table'"
                      class="parsed-figure-card"
                    >
                      <div v-if="block.imageUrl" class="parsed-figure-img-container">
                        <img :src="block.imageUrl" :alt="block.text || '论文图表'" class="parsed-figure-img" loading="lazy" />
                      </div>
                      <div v-else-if="block.html" class="parsed-table-html" v-html="block.html"></div>
                      <figcaption class="parsed-figure-caption">
                        <span class="figure-tag">{{ block.kind === 'table' ? '表格' : '图表' }}</span>
                        <span class="figure-caption-text">{{ block.text || (block.kind === 'table' ? '未命名表格' : '未命名图表') }}</span>
                      </figcaption>
                    </figure>
                    <!-- Equation Card -->
                    <div
                      v-else-if="block.kind === 'equation'"
                      class="parsed-equation-card"
                    >
                      <div v-if="block.imageUrl" class="parsed-equation-img-container">
                        <img :src="block.imageUrl" :alt="block.text || '公式'" class="parsed-equation-img" loading="lazy" />
                      </div>
                      <pre v-else class="parsed-equation-text">{{ block.text }}</pre>
                      <span v-if="block.equationNumber" class="equation-num">({{ block.equationNumber }})</span>
                    </div>
                    <!-- Regular Paragraph -->
                    <p
                      v-else
                      class="parsed-paragraph-text"
                      v-html="highlightQuoteInText(block.text, selectedSource?.quote)"
                    ></p>
                  </div>
                </section>
              </template>
              <!-- Fallback to flat paragraphs -->
              <template v-else-if="paragraphs && paragraphs.length">
                <div
                  v-for="(p, pIdx) in paragraphs"
                  :key="p.id || pIdx"
                  :id="`quiz-parsed-block-${p.id}`"
                  class="parsed-block"
                  :class="{ 'is-target-highlight': selectedSource && (String(p.id) === String(selectedSource.id) || (p.text && p.text.includes(selectedSource.quote))) }"
                >
                  <div class="parsed-block-meta">#{{ pIdx + 1 }} {{ p.pageNumber ? `(P.${p.pageNumber})` : '' }}</div>
                  <p class="parsed-paragraph-text" v-html="highlightQuoteInText(p.text, selectedSource?.quote)"></p>
                </div>
              </template>
              <div v-else class="source-empty-state">
                <p>暂无解析段落数据</p>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <div v-if="confirmRetakeOpen" class="quiz-confirm-backdrop" role="presentation" @click.self="confirmRetakeOpen = false">
      <section class="quiz-confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="retake-confirm-title">
        <div class="quiz-confirm-icon">↻</div>
        <h3 id="retake-confirm-title">确认重新测评？</h3>
        <p>重新测评会生成一份新问卷，当前答题进度将被清空，并消耗 2 积分。</p>
        <div class="quiz-confirm-actions">
          <button type="button" class="quiz-confirm-cancel" @click="confirmRetakeOpen = false">取消</button>
          <button type="button" class="quiz-confirm-ok" @click="confirmRetake">确认重新测评</button>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { apiClient } from '../services/apiClient';

const props = defineProps({
  workspaceId: String,
  title: String,
  paragraphs: Array,
  pages: Array,
  pdfSource: String
});

const emit = defineEmits(['close', 'locate', 'charged']);

const quiz = ref(null);
const loading = ref(true);
const busy = ref(false);
const error = ref('');
const answers = reactive({});
const selectedSource = ref(null);
const rightTab = ref('evidence'); // 'evidence' | 'parsed'
const quoteCopied = ref(false);
const parsedDocContainer = ref(null);
const confirmRetakeOpen = ref(false);

const requestId = ref(crypto.randomUUID());
const totalQuestionCount = 17;

const displayedQuestions = computed(() => {
  return quiz.value?.result?.questions || quiz.value?.questions || [];
});

const answeredCount = computed(() => {
  const list = quiz.value?.questions || [];
  return list.filter(q => String(answers[q.id] || '').trim().length > 0).length;
});

const matchedPageNumber = computed(() => {
  if (!selectedSource.value) return null;
  const pId = String(selectedSource.value.id || '');
  const quote = String(selectedSource.value.quote || '');
  
  if (props.pages && props.pages.length) {
    for (const page of props.pages) {
      for (const block of (page.blocks || [])) {
        if (String(block.id) === pId || (quote && block.text && block.text.includes(quote))) {
          return page.pageNumber;
        }
      }
    }
  }
  
  const p = props.paragraphs?.find(item => String(item.id) === pId || (quote && item.text && item.text.includes(quote)));
  return p?.pageNumber || null;
});

function typeLabel(t) {
  return { choice: '单选题', boolean: '判断题', short: '简答题' }[t] || '问答题';
}

function answerLabel(a) {
  if (a === 'true') return '正确 (True)';
  if (a === 'false') return '错误 (False)';
  return a || '暂无';
}

function cleanOptionText(text, index) {
  if (!text) return '';
  const letter = 'ABCD'[index];
  // Strip leading 'A. ', 'A、', 'A: ', '(A) ', '[A] ', etc.
  return String(text)
    .replace(new RegExp(`^\\s*[(（\\[]?${letter}[)）\\]]?[.、:：\\s]+\\s*`, 'i'), '')
    .replace(/^([A-D][.、\s])+/i, '')
    .trim();
}

function scoreGradeLabel(score) {
  if (score >= 90) return '优秀';
  if (score >= 80) return '良好';
  if (score >= 60) return '合格';
  return '需加强';
}

function scoreGradeClass(score) {
  if (score >= 90) return 'grade-excellent';
  if (score >= 80) return 'grade-good';
  if (score >= 60) return 'grade-pass';
  return 'grade-fail';
}

function getQuestionScoreClass(q) {
  if (q.type === 'short') {
    return q.score >= 10 ? 'score-high' : q.score > 0 ? 'score-mid' : 'score-zero';
  }
  return q.score > 0 ? 'score-full' : 'score-zero';
}

function getOptionClass(q, val) {
  const isSelected = answers[q.id] === val;
  if (!quiz.value?.result) {
    return { selected: isSelected };
  }
  const isCorrect = q.answer === val;
  return {
    selected: isSelected,
    'ans-correct': isCorrect,
    'ans-wrong': isSelected && !isCorrect,
    'ans-missed': !isSelected && isCorrect
  };
}

function getBooleanOptionClass(q, val) {
  const isSelected = answers[q.id] === val;
  if (!quiz.value?.result) {
    return { selected: isSelected };
  }
  const isCorrect = q.answer === val;
  return {
    selected: isSelected,
    'ans-correct': isCorrect,
    'ans-wrong': isSelected && !isCorrect,
    'ans-missed': !isSelected && isCorrect
  };
}

function getQuestionJumpClass(idx) {
  const qList = displayedQuestions.value;
  const q = qList[idx - 1];
  if (!q) return '';
  if (quiz.value?.result) {
    return q.score > 0 ? 'jump-correct' : 'jump-wrong';
  }
  return String(answers[q.id] || '').trim() ? 'jump-answered' : '';
}

function scrollToQuestion(idx) {
  const target = document.getElementById(`quiz-question-${idx}`);
  if (target) {
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    target.animate([
      { borderColor: '#6366f1', boxShadow: '0 0 0 2px rgba(99,102,241,0.4)' },
      { borderColor: '', boxShadow: '' }
    ], { duration: 1800 });
  }
}

function inspectSource(question) {
  selectedSource.value = {
    id: String(question.sourceId || ''),
    quote: String(question.quote || ''),
    question
  };
  if (rightTab.value === 'parsed') {
    locateAndScrollInParsedView();
  } else {
    rightTab.value = 'evidence';
  }
}

function sourceParagraphText(source) {
  if (!source) return '暂无对应段落';
  const pId = String(source.id || '');
  const quote = String(source.quote || '');
  
  if (props.pages && props.pages.length) {
    for (const page of props.pages) {
      for (const block of (page.blocks || [])) {
        if (String(block.id) === pId || (quote && block.text && block.text.includes(quote))) {
          return block.text;
        }
      }
    }
  }
  
  const paragraph = props.paragraphs?.find(item => String(item.id) === pId || (quote && item.text && item.text.includes(quote)));
  return paragraph?.text || source.quote || '暂无对应解析段落';
}

function isBlockTargetSource(block) {
  if (!selectedSource.value) return false;
  const sId = String(selectedSource.value.id || '');
  const quote = String(selectedSource.value.quote || '');
  if (String(block.id) === sId) return true;
  if (quote && block.text && block.text.includes(quote)) return true;
  return false;
}

function escapeHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function highlightQuoteInText(fullText, quote) {
  if (!fullText) return '';
  const escapedFull = escapeHtml(fullText);
  if (!quote || !quote.trim()) return escapedFull;
  
  const trimmedQuote = quote.trim();
  const escapedQuote = escapeHtml(trimmedQuote);
  
  if (escapedFull.includes(escapedQuote)) {
    return escapedFull.replace(
      escapedQuote,
      `<mark class="quiz-source-mark">${escapedQuote}</mark>`
    );
  }
  
  // Fuzzy match fallback: split words or match longest fragment
  const cleanQ = trimmedQuote.replace(/[^\w\u4e00-\u9fa5]/g, '');
  if (cleanQ.length >= 6) {
    const sub = cleanQ.slice(0, Math.min(cleanQ.length, 18));
    const subEscaped = escapeHtml(sub);
    if (escapedFull.includes(subEscaped)) {
      return escapedFull.replace(
        subEscaped,
        `<mark class="quiz-source-mark">${subEscaped}</mark>`
      );
    }
  }
  return escapedFull;
}

async function locateAndScrollInParsedView() {
  rightTab.value = 'parsed';
  await nextTick();
  
  if (!selectedSource.value) return;
  const sId = selectedSource.value.id;
  const quote = selectedSource.value.quote;
  
  let targetEl = null;
  if (sId) {
    targetEl = document.getElementById(`quiz-parsed-block-${sId}`);
  }
  if (!targetEl && quote) {
    const marks = document.querySelectorAll('.source-view-parsed .quiz-source-mark');
    if (marks.length > 0) targetEl = marks[0].closest('.parsed-block');
  }
  
  if (targetEl) {
    targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
    targetEl.animate([
      { transform: 'scale(1.02)', boxShadow: '0 0 0 3px rgba(99,102,241,0.6)' },
      { transform: 'scale(1)', boxShadow: '' }
    ], { duration: 2200 });
  }
}

function copyQuote(text) {
  if (!text) return;
  navigator.clipboard.writeText(text).then(() => {
    quoteCopied.value = true;
    setTimeout(() => { quoteCopied.value = false; }, 2000);
  }).catch(() => {});
}

function draftKey() {
  return `papersolver-quiz-draft:${props.workspaceId}`;
}

function restoreAnswers() {
  if (quiz.value?.result?.questions) {
    for (const q of quiz.value.result.questions) {
      if (q.studentAnswer !== undefined && q.studentAnswer !== null) {
        answers[q.id] = String(q.studentAnswer);
      }
    }
    return;
  }
  try {
    const saved = JSON.parse(localStorage.getItem(draftKey()) || '{}');
    Object.assign(answers, saved);
  } catch {}
}

watch(answers, () => {
  if (quiz.value && !quiz.value.result) {
    try {
      localStorage.setItem(draftKey(), JSON.stringify(answers));
    } catch {}
  }
}, { deep: true });

function handleKeyDown(e) {
  if (e.key === 'Escape') {
    emit('close');
  }
}

function message(e) {
  return e?.response?.data?.message || e?.response?.data?.detail || e?.message || '请求失败，请重试';
}

onMounted(async () => {
  window.addEventListener('keydown', handleKeyDown);
  try {
    const res = await apiClient.get('/api/paper-quizzes/latest', {
      params: { workspaceId: props.workspaceId }
    });
    quiz.value = res.data;
    if (quiz.value) {
      restoreAnswers();
      // Auto-select first question source if available
      const firstQ = (quiz.value.result?.questions || quiz.value.questions || [])[0];
      if (firstQ) {
        selectedSource.value = {
          id: String(firstQ.sourceId || ''),
          quote: String(firstQ.quote || ''),
          question: firstQ
        };
      }
    }
  } catch (e) {
    error.value = message(e);
  } finally {
    loading.value = false;
  }
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown);
});

async function generate() {
  busy.value = true;
  error.value = '';
  try {
    const res = await apiClient.post('/api/paper-quizzes', {
      // Reuse the same id for a retry so a lost response cannot charge twice.
      requestId: requestId.value,
      workspaceId: props.workspaceId,
      paragraphs: props.paragraphs
    }, { timeout: 300000 });
    quiz.value = res.data;
    restoreAnswers();
    emit('charged');
    const firstQ = (quiz.value.questions || [])[0];
    if (firstQ) {
      selectedSource.value = {
        id: String(firstQ.sourceId || ''),
        quote: String(firstQ.quote || ''),
        question: firstQ
      };
    }
  } catch (e) {
    error.value = message(e);
  } finally {
    busy.value = false;
  }
}

async function retakeQuiz() {
  if (busy.value || !quiz.value?.result) return;
  confirmRetakeOpen.value = true;
}

async function confirmRetake() {
  confirmRetakeOpen.value = false;
  if (busy.value) return;
  requestId.value = crypto.randomUUID();
  for (const k of Object.keys(answers)) {
    delete answers[k];
  }
  try {
    localStorage.removeItem(draftKey());
  } catch {}
  quiz.value = null;
  selectedSource.value = null;
  await generate();
}

async function submit() {
  busy.value = true;
  error.value = '';
  try {
    const res = await apiClient.post(`/api/paper-quizzes/${quiz.value.id}/submit`, {
      answers
    }, { timeout: 180000 });
    quiz.value = res.data;
    restoreAnswers();
    // Select first question
    const firstQ = (quiz.value.result?.questions || [])[0];
    if (firstQ) {
      inspectSource(firstQ);
    }
  } catch (e) {
    error.value = message(e);
  } finally {
    busy.value = false;
  }
}
</script>

<style scoped>
/* ================= Root Variables & Overlay ================= */
.quiz-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  flex-direction: column;
  background: #090d16;
  color: #e2e8f0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  overflow: hidden;
  animation: quizFadeIn 0.22s ease-out;
}

@keyframes quizFadeIn {
  from { opacity: 0; transform: scale(0.995); }
  to { opacity: 1; transform: scale(1); }
}

/* ================= Header ================= */
.quiz-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  height: 72px;
  padding: 0 24px;
  background: rgba(15, 23, 42, 0.95);
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
  backdrop-filter: blur(12px);
  flex-shrink: 0;
  z-index: 10;
}

.quiz-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.quiz-brand-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.12);
  border: 1px solid rgba(99, 102, 241, 0.28);
  color: #a5b4fc;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.quiz-title-box {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.quiz-main-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #f8fafc;
  line-height: 1.25;
}

.quiz-paper-subtitle {
  font-size: 12.5px;
  color: #94a3b8;
  max-width: 480px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.quiz-header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

/* Graded Score Pill */
.quiz-score-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 12px;
  border-radius: 10px;
  background: rgba(30, 41, 59, 0.85);
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.quiz-score-badge {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
}
.quiz-score-badge.grade-excellent { background: #10b981; color: #fff; }
.quiz-score-badge.grade-good { background: #3b82f6; color: #fff; }
.quiz-score-badge.grade-pass { background: #f59e0b; color: #fff; }
.quiz-score-badge.grade-fail { background: #ef4444; color: #fff; }

.quiz-score-numbers {
  display: flex;
  align-items: baseline;
  gap: 3px;
}
.quiz-score-main { font-size: 20px; font-weight: 800; color: #f8fafc; }
.quiz-score-total { font-size: 12px; color: #94a3b8; }

/* Answering Progress */
.quiz-progress-pill {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 140px;
}
.quiz-progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: #94a3b8;
}
.quiz-progress-info strong { color: #818cf8; font-weight: 700; }
.quiz-progress-bar-bg {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.2);
  overflow: hidden;
}
.quiz-progress-bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #6366f1, #3b82f6);
  transition: width 0.25s ease;
}

/* Retake Button */
.quiz-btn-retake {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid rgba(99, 102, 241, 0.4);
  background: rgba(99, 102, 241, 0.16);
  color: #a5b4fc;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
  white-space: nowrap;
}
.quiz-btn-retake:hover {
  background: rgba(99, 102, 241, 0.3);
  border-color: #818cf8;
  color: #ffffff;
  transform: translateY(-1px);
}
.quiz-btn-retake:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* Close Button */
.quiz-btn-close {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(30, 41, 59, 0.7);
  color: #cbd5e1;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.18s ease;
}
.quiz-btn-close:hover {
  background: rgba(51, 65, 85, 0.9);
  border-color: rgba(148, 163, 184, 0.45);
  color: #f8fafc;
}

/* ================= Split Layout ================= */
.quiz-split-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(380px, 0.95fr);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ================= Left: Exam Pane ================= */
.quiz-exam-pane {
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100%;
  overflow-y: auto;
  border-right: 1px solid rgba(148, 163, 184, 0.14);
  background: #090d16;
  scroll-behavior: smooth;
}

/* Banner Area & Jump Strip */
.quiz-banner-area {
  padding: 18px 28px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  background: rgba(15, 23, 42, 0.4);
}

.quiz-summary-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.quiz-summary-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-tag {
  padding: 3px 9px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.16);
}
.summary-tag.total {
  color: #a5b4fc;
  background: rgba(99, 102, 241, 0.14);
  border-color: rgba(99, 102, 241, 0.3);
  font-weight: 600;
}

.quiz-tip-text {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
}

/* Question Jump Strip */
.quiz-jump-nav {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.12);
  flex-wrap: wrap;
}

.jump-section-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.jump-group-label {
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
}

.jump-pills {
  display: flex;
  align-items: center;
  gap: 5px;
}

.jump-pill {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(30, 41, 59, 0.6);
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.15s ease;
  padding: 0;
}
.jump-pill:hover {
  background: rgba(99, 102, 241, 0.25);
  border-color: #818cf8;
  color: #fff;
}
.jump-pill.jump-answered {
  background: rgba(16, 185, 129, 0.16);
  border-color: rgba(16, 185, 129, 0.7);
  color: #047857;
}
.jump-pill.jump-correct {
  background: rgba(16, 185, 129, 0.2);
  border-color: rgba(16, 185, 129, 0.6);
  color: #6ee7b7;
}
.jump-pill.jump-wrong {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.6);
  color: #fca5a5;
}

/* ================= States: Start / Loading / Error ================= */
.quiz-state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 60px 32px;
  margin: auto;
  max-width: 580px;
}

.start-icon-shield {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(59, 130, 246, 0.15));
  border: 1px solid rgba(99, 102, 241, 0.35);
  color: #818cf8;
  margin-bottom: 20px;
}

.quiz-state-box h3 {
  font-size: 22px;
  font-weight: 700;
  color: #f8fafc;
  margin: 0 0 12px;
}

.start-desc {
  font-size: 14.5px;
  line-height: 1.7;
  color: #94a3b8;
  margin: 0 0 24px;
}

.start-meta-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 10px;
  padding: 14px 18px;
  margin-bottom: 28px;
  width: 100%;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13.5px;
  color: #cbd5e1;
}
.meta-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #6366f1;
}

.quiz-loader-ring {
  width: 44px;
  height: 44px;
  border: 3px solid rgba(99, 102, 241, 0.2);
  border-top-color: #818cf8;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ================= Form & Question Cards ================= */
.quiz-form {
  padding: 24px 28px 120px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.quiz-card {
  border-radius: 14px;
  background: #0e1526;
  border: 1px solid rgba(148, 163, 184, 0.16);
  padding: 22px 24px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.quiz-card:hover {
  border-color: rgba(99, 102, 241, 0.4);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25);
}

.quiz-card.is-active-source {
  border-color: #6366f1;
  box-shadow: 0 0 0 1.5px #6366f1, 0 8px 24px rgba(99, 102, 241, 0.15);
}

/* Card Header */
.quiz-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.quiz-card-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.q-number-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(99, 102, 241, 0.18);
  border: 1px solid rgba(99, 102, 241, 0.35);
  color: #c7d2fe;
  font-size: 13px;
  font-weight: 700;
}

.q-type-badge {
  font-size: 12.5px;
  font-weight: 600;
  color: #cbd5e1;
}

.q-score-badge {
  font-size: 12px;
  color: #cbd5e1;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(148, 163, 184, 0.1);
}

.q-graded-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  font-weight: 700;
  padding: 3px 9px;
  border-radius: 6px;
}
.q-graded-badge.score-full, .q-graded-badge.score-high {
  background: rgba(16, 185, 129, 0.16);
  border: 1px solid rgba(16, 185, 129, 0.4);
  color: #34d399;
}
.q-graded-badge.score-mid {
  background: rgba(245, 158, 11, 0.16);
  border: 1px solid rgba(245, 158, 11, 0.4);
  color: #fbbf24;
}
.q-graded-badge.score-zero {
  background: rgba(239, 68, 68, 0.16);
  border: 1px solid rgba(239, 68, 68, 0.4);
  color: #f87171;
}

.q-answered-tag {
  font-size: 11.5px;
  color: #34d399;
  background: rgba(16, 185, 129, 0.14);
  border: 1px solid rgba(16, 185, 129, 0.38);
  padding: 2px 7px;
  border-radius: 4px;
}

/* Prompt Title */
.quiz-card-prompt {
  margin: 0 0 18px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.65;
  color: #f1f5f9;
  word-break: break-word;
  overflow-wrap: anywhere;
}

/* ================= Options & Items ================= */
.quiz-options-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quiz-options-group.boolean {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.quiz-option-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  width: 100%;
  box-sizing: border-box;
  padding: 13px 16px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(15, 23, 42, 0.6);
  color: #cbd5e1;
  cursor: pointer;
  position: relative;
  transition: all 0.18s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
}

.quiz-option-item:hover {
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(99, 102, 241, 0.45);
  transform: translateY(-1px);
}

.quiz-option-item.selected {
  background: rgba(99, 102, 241, 0.16);
  border-color: #6366f1;
  box-shadow: 0 0 0 1px #6366f1;
}

/* Graded States for Options */
.quiz-option-item.ans-correct {
  background: rgba(16, 185, 129, 0.14) !important;
  border-color: #10b981 !important;
  box-shadow: 0 0 0 1px #10b981 !important;
}
.quiz-option-item.ans-wrong {
  background: rgba(239, 68, 68, 0.14) !important;
  border-color: #ef4444 !important;
  box-shadow: 0 0 0 1px #ef4444 !important;
}
.quiz-option-item.ans-missed {
  background: rgba(16, 185, 129, 0.06) !important;
  border: 1.5px dashed #10b981 !important;
}

.quiz-hidden-input {
  position: absolute;
  opacity: 0;
  width: 1px;
  height: 1px;
  pointer-events: none;
}

/* Badge (A/B/C/D / T/F) */
.quiz-option-badge {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 700;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(30, 41, 59, 0.8);
  color: #94a3b8;
  transition: all 0.18s ease;
}

.quiz-option-item:hover .quiz-option-badge {
  border-color: #818cf8;
  color: #fff;
}

.quiz-option-item.selected .quiz-option-badge {
  background: #6366f1;
  border-color: #6366f1;
  color: #ffffff;
}

.quiz-option-item.ans-correct .quiz-option-badge {
  background: #10b981;
  border-color: #10b981;
  color: #ffffff;
}
.quiz-option-item.ans-wrong .quiz-option-badge {
  background: #ef4444;
  border-color: #ef4444;
  color: #ffffff;
}

.quiz-option-text {
  flex: 1;
  min-width: 0;
  font-size: 14.5px;
  line-height: 1.65;
  color: #e2e8f0;
  word-break: break-word;
  overflow-wrap: anywhere;
}
.quiz-option-text.bold-label {
  font-weight: 600;
}

.quiz-ans-tag {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 4px;
  white-space: nowrap;
  align-self: center;
}
.quiz-ans-tag.correct { background: #10b981; color: #fff; }
.quiz-ans-tag.wrong { background: #ef4444; color: #fff; }

/* ================= Short Answer Textarea ================= */
.quiz-short-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quiz-textarea {
  width: 100%;
  box-sizing: border-box;
  min-height: 130px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: #080d1a;
  color: #f1f5f9;
  font-size: 14.5px;
  line-height: 1.7;
  resize: vertical;
  outline: none;
  transition: all 0.2s ease;
  font-family: inherit;
}

.quiz-textarea:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.25);
  background: #0b1222;
}

.quiz-textarea-footer {
  display: flex;
  justify-content: flex-end;
}
.char-counter {
  font-size: 12px;
  color: #64748b;
}

/* ================= Feedback Box ================= */
.quiz-feedback-box {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.75);
  border: 1px solid rgba(99, 102, 241, 0.25);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feedback-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13.5px;
  line-height: 1.6;
}

.feedback-lbl {
  font-weight: 700;
  color: #94a3b8;
  white-space: nowrap;
  flex-shrink: 0;
}

.ans-value {
  color: #34d399;
  font-weight: 700;
}

.feedback-text {
  margin: 0;
  color: #cbd5e1;
}

.feedback-quote-row {
  margin-top: 4px;
}

.cited-quote {
  margin: 6px 0 0;
  padding: 10px 14px;
  border-radius: 8px;
  background: rgba(2, 6, 23, 0.6);
  border-left: 3px solid #818cf8;
  color: #e0e7ff;
  font-size: 13px;
  line-height: 1.65;
  font-style: italic;
  word-break: break-word;
}

/* ================= Card Footer Action ================= */
.quiz-card-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.quiz-btn-source-inspect {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 13px;
  border-radius: 7px;
  border: 1px solid rgba(99, 102, 241, 0.35);
  background: rgba(99, 102, 241, 0.12);
  color: #a5b4fc;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
}
.quiz-btn-source-inspect:hover, .quiz-btn-source-inspect.active {
  background: rgba(99, 102, 241, 0.28);
  border-color: #818cf8;
  color: #ffffff;
  transform: translateY(-1px);
}

/* ================= Submit Bottom Bar ================= */
.quiz-submit-bar {
  position: sticky;
  bottom: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 24px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(12px);
  margin-top: 20px;
}

.submit-bar-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.submit-bar-info span {
  font-size: 14px;
  color: #cbd5e1;
}
.submit-bar-info strong {
  color: #818cf8;
  font-size: 16px;
}
.submit-bar-info small {
  font-size: 12px;
  color: #64748b;
}
.submit-bar-info .ready-text {
  color: #34d399;
}

.quiz-btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 22px;
  border-radius: 9px;
  background: linear-gradient(135deg, #6366f1, #3b82f6);
  border: none;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
  transition: all 0.18s ease;
}
.quiz-btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #4f46e5, #2563eb);
  box-shadow: 0 6px 18px rgba(99, 102, 241, 0.45);
  transform: translateY(-1px);
}
.quiz-btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* ================= Right: Source & PDF Pane ================= */
.quiz-source-pane {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #0b1120;
  overflow: hidden;
}

.source-pane-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(15, 23, 42, 0.85);
  flex-shrink: 0;
}

.source-pane-title {
  display: flex;
  flex-direction: column;
}
.source-kicker {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #818cf8;
}
.source-pane-title h3 {
  margin: 2px 0 0;
  font-size: 15px;
  font-weight: 700;
  color: #f8fafc;
}

.source-tab-segmented {
  display: flex;
  padding: 3px;
  border-radius: 8px;
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.source-tab-btn {
  padding: 5px 11px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.source-tab-btn:hover {
  color: #e2e8f0;
}
.source-tab-btn.active {
  background: #6366f1;
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.35);
}

.source-pane-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px;
  scroll-behavior: smooth;
}

/* Tab 1: Evidence View */
.source-view-evidence {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.evidence-content-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.evidence-top-card {
  padding: 14px 16px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(99, 102, 241, 0.25);
}

.evidence-meta-pill {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.evidence-num {
  font-size: 12.5px;
  font-weight: 700;
  color: #c7d2fe;
}
.evidence-page {
  font-size: 11.5px;
  color: #38bdf8;
  background: rgba(56, 189, 248, 0.12);
  padding: 1px 7px;
  border-radius: 4px;
}

.evidence-prompt-summary {
  font-size: 13.5px;
  line-height: 1.55;
  color: #e2e8f0;
}

.section-title-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #a5b4fc;
  margin-bottom: 8px;
}

.btn-copy-quote {
  margin-left: auto;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(30, 41, 59, 0.6);
  color: #cbd5e1;
  font-size: 11.5px;
  cursor: pointer;
}
.btn-copy-quote:hover {
  background: #334155;
  color: #fff;
}

.evidence-quote-section {
  padding: 14px 16px;
  border-radius: 10px;
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.evidence-direct-quote {
  margin: 0;
  padding: 10px 14px;
  border-radius: 8px;
  background: rgba(99, 102, 241, 0.1);
  border-left: 3px solid #818cf8;
  color: #e0e7ff;
  font-size: 14px;
  line-height: 1.7;
  font-family: Georgia, 'Times New Roman', serif;
  word-break: break-word;
}

.evidence-paragraph-section {
  padding: 14px 16px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.evidence-full-paragraph {
  font-size: 13.5px;
  line-height: 1.8;
  color: #cbd5e1;
  word-break: break-word;
}

:deep(.quiz-source-mark) {
  background: transparent !important;
  color: #c7d2fe !important;
  text-decoration: underline !important;
  text-decoration-color: #818cf8 !important;
  text-decoration-thickness: 2.5px !important;
  text-underline-offset: 4px !important;
  font-weight: 700 !important;
  padding: 0 2px;
}

.evidence-actions {
  margin-top: 4px;
}

.locate-in-parsed-btn {
  width: 100%;
}

/* Empty State */
.source-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 48px 24px;
  color: #64748b;
  margin: auto;
}
.empty-icon-compass {
  color: #475569;
  margin-bottom: 14px;
}
.source-empty-state h4 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #94a3b8;
}
.source-empty-state p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  max-width: 320px;
}

/* Tab 2: Parsed Document View */
.source-view-parsed {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.parsed-doc-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #64748b;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}
.parsed-doc-topbar small {
  color: #818cf8;
  font-weight: 600;
}

.parsed-pages-stream {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.parsed-page-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.parsed-page-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.08em;
  margin: 6px 0;
}
.parsed-page-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(148, 163, 184, 0.12);
}

.parsed-block {
  padding: 10px 14px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.45);
  border: 1px solid transparent;
  transition: all 0.25s ease;
}

.parsed-block.is-target-highlight {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.15);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.4);
}

.parsed-heading-text {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #93c5fd;
}

.parsed-paragraph-text {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.75;
  color: #cbd5e1;
}

/* Figure & Table Cards */
.parsed-figure-card {
  margin: 10px 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.16);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.parsed-figure-img-container {
  display: flex;
  justify-content: center;
  align-items: center;
  background: #ffffff;
  padding: 8px;
  border-radius: 6px;
  border: 1px solid rgba(148, 163, 184, 0.15);
  max-width: 100%;
  overflow: hidden;
}

.parsed-figure-img {
  max-width: 100%;
  max-height: 380px;
  object-fit: contain;
  border-radius: 4px;
}

.parsed-table-html {
  overflow-x: auto;
  max-width: 100%;
  font-size: 12.5px;
}

.parsed-figure-caption {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12.5px;
  color: #cbd5e1;
  line-height: 1.5;
}

.figure-caption-text {
  flex: 1;
  word-break: break-word;
}

.figure-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 7px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  background: rgba(56, 189, 248, 0.15);
  color: #38bdf8;
  border: 1px solid rgba(56, 189, 248, 0.3);
  flex-shrink: 0;
}

/* Equations */
.parsed-equation-card {
  margin: 8px 0;
  padding: 10px 14px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(148, 163, 184, 0.14);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.parsed-equation-img-container {
  display: flex;
  align-items: center;
  background: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
  max-width: 85%;
  overflow-x: auto;
}

.parsed-equation-img {
  max-height: 50px;
  object-fit: contain;
}

.parsed-equation-text {
  margin: 0;
  font-size: 12.5px;
  font-family: SFMono-Regular, Consolas, Monaco, monospace;
  color: #a5b4fc;
  background: rgba(30, 41, 59, 0.7);
  padding: 8px 10px;
  border-radius: 6px;
  overflow-x: auto;
  flex: 1;
}

.equation-num {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
}

.parsed-block-meta {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}

/* ================= Light Theme Adaptations ================= */
:root[data-theme="light"] .quiz-overlay {
  background: #f8fafc;
  color: #0f172a;
}

:root[data-theme="light"] .quiz-header {
  background: rgba(255, 255, 255, 0.95);
  border-bottom-color: #e2e8f0;
}

:root[data-theme="light"] .quiz-brand-pill {
  background: #eef2ff !important;
  border-color: #c7d2fe !important;
  color: #4338ca !important;
}

:root[data-theme="light"] .quiz-main-title {
  color: #0f172a;
}

:root[data-theme="light"] .quiz-paper-subtitle {
  color: #475569;
}

:root[data-theme="light"] .quiz-score-pill {
  background: #f1f5f9;
  border-color: #cbd5e1;
}
:root[data-theme="light"] .quiz-score-main {
  color: #0f172a;
}

:root[data-theme="light"] .quiz-btn-retake {
  background: #eef2ff;
  border-color: #818cf8;
  color: #4338ca;
}
:root[data-theme="light"] .quiz-btn-retake:hover {
  background: #e0e7ff;
  border-color: #6366f1;
  color: #312e81;
}

:root[data-theme="light"] .quiz-btn-close {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: #1e293b;
}
:root[data-theme="light"] .quiz-btn-close:hover {
  background: #e2e8f0;
  color: #0f172a;
}

:root[data-theme="light"] .quiz-exam-pane {
  background: #f8fafc;
  border-right-color: #e2e8f0;
}

:root[data-theme="light"] .quiz-banner-area {
  background: rgba(241, 245, 249, 0.6);
  border-bottom-color: #e2e8f0;
}

:root[data-theme="light"] .quiz-tip-text {
  color: #475569 !important;
}

:root[data-theme="light"] .summary-tag {
  background: #ffffff;
  border-color: #e2e8f0;
  color: #334155;
}
:root[data-theme="light"] .summary-tag.total {
  background: #eef2ff;
  border-color: #c7d2fe;
  color: #4338ca;
}

:root[data-theme="light"] .quiz-jump-nav {
  background: #ffffff;
  border-color: #e2e8f0;
}
:root[data-theme="light"] .jump-pill {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: #334155;
}
:root[data-theme="light"] .jump-group-label,
:root[data-theme="light"] .quiz-tip-text,
:root[data-theme="light"] .q-type-badge,
:root[data-theme="light"] .q-score-badge {
  color: #334155 !important;
}
:root[data-theme="light"] .jump-pill.jump-answered {
  background: #dcfce7;
  border-color: #22c55e;
  color: #166534;
  font-weight: 700;
}
:root[data-theme="light"] .q-answered-tag {
  color: #166534;
  background: #dcfce7;
  border-color: #86efac;
  font-weight: 700;
}
:root[data-theme="light"] .q-number-badge {
  background: #eef2ff;
  border-color: #a5b4fc;
  color: #3730a3;
}
:root[data-theme="light"] .q-type-badge {
  font-weight: 700;
}
:root[data-theme="light"] .q-score-badge {
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  font-weight: 700;
}

/* Light Theme States: Start / Loading / Error */
:root[data-theme="light"] .quiz-state-box {
  color: #0f172a;
}
:root[data-theme="light"] .quiz-state-box h3 {
  color: #0f172a !important;
  font-weight: 700 !important;
}
:root[data-theme="light"] .start-icon-shield {
  background: #eef2ff !important;
  border: 1px solid #c7d2fe !important;
  color: #4f46e5 !important;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.12) !important;
}
:root[data-theme="light"] .start-desc {
  color: #334155 !important;
}
:root[data-theme="light"] .start-meta-list {
  background: #f1f5f9 !important;
  border-color: #cbd5e1 !important;
}
:root[data-theme="light"] .meta-item {
  color: #1e293b !important;
  font-weight: 500 !important;
}
:root[data-theme="light"] .meta-dot {
  background: #4f46e5 !important;
}

:root[data-theme="light"] .quiz-state-box.error {
  background: #fef2f2 !important;
  border: 1px solid #fecdd3 !important;
  border-radius: 12px;
  padding: 24px;
}
:root[data-theme="light"] .quiz-state-box.error svg {
  color: #e11d48 !important;
}
:root[data-theme="light"] .quiz-state-box.error .error-msg-wrap strong {
  color: #881337 !important;
  font-weight: 700 !important;
}
:root[data-theme="light"] .quiz-state-box.error .error-msg-wrap p {
  color: #9f1239 !important;
  font-weight: 500 !important;
}
:root[data-theme="light"] .quiz-loader-ring {
  border-color: rgba(99, 102, 241, 0.15) !important;
  border-top-color: #6366f1 !important;
}

:root[data-theme="light"] .quiz-card {
  background: #ffffff;
  border-color: #e2e8f0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}
:root[data-theme="light"] .quiz-card:hover {
  border-color: #818cf8;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.12);
}

:root[data-theme="light"] .quiz-card-prompt {
  color: #0f172a !important;
  font-weight: 700 !important;
}

:root[data-theme="light"] .quiz-option-item {
  background: #f8fafc;
  border-color: #e2e8f0;
  color: #0f172a;
}
:root[data-theme="light"] .quiz-option-item:hover {
  background: #f1f5f9;
  border-color: #818cf8;
}
:root[data-theme="light"] .quiz-option-item.selected {
  background: #eef2ff;
  border-color: #6366f1;
}

:root[data-theme="light"] .quiz-option-badge {
  background: #ffffff;
  border-color: #cbd5e1;
  color: #0f172a;
  font-weight: 700;
}

:root[data-theme="light"] .quiz-option-text {
  color: #0f172a;
}

:root[data-theme="light"] .quiz-textarea {
  background: #ffffff;
  border-color: #cbd5e1;
  color: #0f172a;
}
:root[data-theme="light"] .quiz-textarea:focus {
  background: #ffffff;
  border-color: #6366f1;
}

/* High-Contrast Light Theme Feedback Styles */
:root[data-theme="light"] .quiz-feedback-box {
  background: #f8fafc;
  border-color: #cbd5e1;
}
:root[data-theme="light"] .feedback-lbl {
  color: #0f172a !important;
  font-weight: 700 !important;
}
:root[data-theme="light"] .feedback-text {
  color: #0f172a !important;
  font-weight: 500 !important;
}
:root[data-theme="light"] .ans-value {
  color: #047857 !important;
  font-weight: 700 !important;
}
:root[data-theme="light"] .cited-quote {
  background: #ffffff !important;
  border: 1px solid #e2e8f0 !important;
  border-left: 4px solid #4f46e5 !important;
  color: #0f172a !important;
  font-style: normal;
  font-weight: 500;
}

/* High-Contrast Light Theme Inspect Button */
:root[data-theme="light"] .quiz-btn-source-inspect {
  background: #f1f5f9 !important;
  border: 1px solid #cbd5e1 !important;
  color: #0f172a !important;
  font-weight: 600 !important;
}
:root[data-theme="light"] .quiz-btn-source-inspect:hover,
:root[data-theme="light"] .quiz-btn-source-inspect.active {
  background: #eef2ff !important;
  border-color: #6366f1 !important;
  color: #4338ca !important;
  box-shadow: 0 1px 4px rgba(99, 102, 241, 0.15);
}

:root[data-theme="light"] .quiz-submit-bar {
  background: rgba(255, 255, 255, 0.95);
  border-color: #e2e8f0;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
}

:root[data-theme="light"] .quiz-source-pane {
  background: #ffffff;
}
:root[data-theme="light"] .source-pane-head {
  background: #f8fafc;
  border-bottom-color: #e2e8f0;
}
:root[data-theme="light"] .source-pane-title h3 {
  color: #0f172a;
}
:root[data-theme="light"] .source-tab-segmented {
  background: #f1f5f9;
  border-color: #e2e8f0;
}
:root[data-theme="light"] .source-tab-btn {
  color: #64748b;
}
:root[data-theme="light"] .source-tab-btn.active {
  background: #6366f1;
  color: #ffffff;
}

:root[data-theme="light"] .evidence-top-card {
  background: #f8fafc;
  border-color: #cbd5e1;
}
:root[data-theme="light"] .evidence-prompt-summary {
  color: #0f172a !important;
}
:root[data-theme="light"] .evidence-quote-section {
  background: #f8fafc;
  border-color: #e2e8f0;
}
:root[data-theme="light"] .evidence-direct-quote {
  background: #eef2ff !important;
  border-left: 4px solid #4f46e5 !important;
  color: #0f172a !important;
  font-weight: 500;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
}
:root[data-theme="light"] .evidence-paragraph-section {
  background: #f8fafc;
  border-color: #e2e8f0;
}
:root[data-theme="light"] .evidence-full-paragraph {
  color: #0f172a !important;
}
:root[data-theme="light"] .section-title-bar {
  color: #0f172a !important;
}

:root[data-theme="light"] .parsed-block {
  background: #f8fafc;
}
:root[data-theme="light"] .parsed-paragraph-text {
  color: #0f172a !important;
}
:root[data-theme="light"] .parsed-heading-text {
  color: #0f172a !important;
}

/* Light Theme Figures / Tables / Equations */
:root[data-theme="light"] .parsed-figure-card {
  background: #f8fafc !important;
  border: 1px solid #e2e8f0 !important;
  color: #0f172a !important;
}
:root[data-theme="light"] .parsed-figure-img-container {
  background: #ffffff !important;
  border: 1px solid #e2e8f0 !important;
}
:root[data-theme="light"] .parsed-figure-caption {
  color: #0f172a !important;
}
:root[data-theme="light"] .figure-caption-text {
  color: #0f172a !important;
  font-weight: 500 !important;
}
:root[data-theme="light"] .figure-tag {
  background: #e0f2fe !important;
  color: #0284c7 !important;
  border-color: #bae6fd !important;
  font-weight: 700 !important;
}
:root[data-theme="light"] .parsed-equation-card {
  background: #f8fafc !important;
  border: 1px solid #e2e8f0 !important;
}
:root[data-theme="light"] .parsed-equation-img-container {
  background: #ffffff !important;
  border: 1px solid #e2e8f0 !important;
}
:root[data-theme="light"] .parsed-equation-text {
  background: #f1f5f9 !important;
  color: #312e81 !important;
  border: 1px solid #e2e8f0 !important;
}
:root[data-theme="light"] .equation-num {
  color: #64748b !important;
}

:root[data-theme="light"] :deep(.quiz-source-mark) {
  background: transparent !important;
  color: #0f172a !important;
  text-decoration: underline !important;
  text-decoration-color: #4f46e5 !important;
  text-decoration-thickness: 2.5px !important;
  text-underline-offset: 4px !important;
  font-weight: 700 !important;
  padding: 0 2px;
}

/* Confirmation dialog for destructive retake action */
.quiz-confirm-backdrop {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(2, 6, 23, 0.58);
}
.quiz-confirm-dialog {
  width: min(420px, 100%);
  padding: 26px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 14px;
  background: #111827;
  color: #f8fafc;
  box-shadow: 0 12px 32px rgba(2, 6, 23, 0.35);
}
.quiz-confirm-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 24px;
  font-weight: 700;
}
.quiz-confirm-dialog h3 { margin: 16px 0 8px; font-size: 18px; }
.quiz-confirm-dialog p { margin: 0; color: #cbd5e1; font-size: 13.5px; line-height: 1.7; }
.quiz-confirm-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 22px; }
.quiz-confirm-actions button { padding: 8px 14px; border-radius: 8px; font-size: 13px; font-weight: 700; cursor: pointer; }
.quiz-confirm-cancel { border: 1px solid #475569; background: transparent; color: #cbd5e1; }
.quiz-confirm-ok { border: 1px solid #6366f1; background: #4f46e5; color: #fff; }
:root[data-theme="light"] .quiz-confirm-dialog { border-color: #cbd5e1; background: #fff; color: #0f172a; }
:root[data-theme="light"] .quiz-confirm-dialog p { color: #334155; }
:root[data-theme="light"] .quiz-confirm-cancel { border-color: #cbd5e1; color: #334155; }
:root[data-theme="light"] .submit-bar-info span,
:root[data-theme="light"] .submit-bar-info small,
:root[data-theme="light"] .submit-bar-info .ready-text {
  color: #334155 !important;
  opacity: 1 !important;
  font-weight: 600;
}
:root[data-theme="light"] .submit-bar-info strong { color: #1d4ed8 !important; }
:root[data-theme="light"] .submit-bar-info .ready-text { color: #166534 !important; }

/* ================= Responsive Queries ================= */
@media (max-width: 1024px) {
  .quiz-split-layout {
    grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  }
}

@media (max-width: 820px) {
  .quiz-split-layout {
    display: flex;
    flex-direction: column;
  }
  .quiz-exam-pane {
    height: auto;
    flex: 1;
    border-right: none;
  }
  .quiz-source-pane {
    height: 380px;
    border-top: 1px solid rgba(148, 163, 184, 0.2);
  }
}
</style>
