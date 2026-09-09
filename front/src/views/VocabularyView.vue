<template>
  <div class="vocabulary-page spatial-page">
    <!-- 顶部概览与统计面板 -->
    <header class="vocab-header">
      <div class="vocab-title-wrap">
        <div class="vocab-brand-badge">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
            <path d="M12 6v6"></path>
            <path d="M9 9h6"></path>
          </svg>
          <span>学术词汇库</span>
        </div>
        <h1>词汇积累</h1>
        <p>文献阅读划词自动收录 · 牛津双解权威释义 · 学术常用搭配 · 艾宾浩斯记忆闪卡</p>
      </div>

      <!-- 统计指标网格 -->
      <div class="vocab-stats-grid">
        <div class="stat-card">
          <span class="stat-lbl">累计收录</span>
          <div class="stat-val-row">
            <strong class="stat-val">{{ stats.total }}</strong>
            <span class="stat-unit">词</span>
          </div>
          <small class="stat-hint">从文献中积累的专业词汇</small>
        </div>

        <div class="stat-card">
          <span class="stat-lbl">今日新增</span>
          <div class="stat-val-row">
            <strong class="stat-val highlight-blue">{{ stats.todayAdded }}</strong>
            <span class="stat-unit">词</span>
          </div>
          <small class="stat-hint">今日阅读文献划词入库</small>
        </div>

        <div class="stat-card">
          <span class="stat-lbl">待复习巩固</span>
          <div class="stat-val-row">
            <strong class="stat-val highlight-amber">{{ stats.needsReview }}</strong>
            <span class="stat-unit">词</span>
          </div>
          <small class="stat-hint">艾宾浩斯遗忘曲线提醒</small>
        </div>

        <div class="stat-card">
          <span class="stat-lbl">已牢记掌握</span>
          <div class="stat-val-row">
            <strong class="stat-val highlight-emerald">{{ stats.masteredWords }}</strong>
            <span class="stat-unit">词</span>
          </div>
          <div class="stat-progress-wrap">
            <div class="stat-progress-bar" :style="{ width: `${stats.masteryRate}%` }"></div>
          </div>
          <small class="stat-hint">掌握率 {{ stats.masteryRate }}%</small>
        </div>
      </div>
    </header>

    <!-- 工具栏与筛选区 -->
    <section class="vocab-toolbar-card">
      <div class="toolbar-left">
        <!-- 搜索框 -->
        <div class="vocab-search-box">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
          <input
            v-model="vocabStore.activeFilter.keyword"
            type="search"
            placeholder="搜索单词、中文释义、来源文献或例句…"
            @input="onFilterChange"
          />
          <button v-if="vocabStore.activeFilter.keyword" class="clear-btn" @click="clearKeyword">×</button>
        </div>

        <!-- 来源文献筛选 -->
        <div class="vocab-select-wrap">
          <select v-model="vocabStore.activeFilter.paperId" @change="onFilterChange">
            <option value="">全部文献来源 ({{ vocabStore.items.length }})</option>
            <option v-for="paper in vocabStore.paperSources" :key="paper.id" :value="paper.id">
              {{ truncateText(paper.title, 24) }} ({{ paper.count }}词)
            </option>
          </select>
        </div>

        <!-- 掌握度过滤 Tab -->
        <div class="mastery-tabs">
          <button
            class="tab-btn"
            :class="{ active: vocabStore.activeFilter.masteryLevel === 'all' }"
            @click="setMasteryFilter('all')"
          >
            全部 ({{ vocabStore.items.length }})
          </button>
          <button
            class="tab-btn"
            :class="{ active: vocabStore.activeFilter.masteryLevel === 0 }"
            @click="setMasteryFilter(0)"
          >
            待学习 ({{ stats.newWords }})
          </button>
          <button
            class="tab-btn"
            :class="{ active: vocabStore.activeFilter.masteryLevel === 1 }"
            @click="setMasteryFilter(1)"
          >
            熟悉中 ({{ stats.familiarWords }})
          </button>
          <button
            class="tab-btn"
            :class="{ active: vocabStore.activeFilter.masteryLevel === 2 }"
            @click="setMasteryFilter(2)"
          >
            已掌握 ({{ stats.masteredWords }})
          </button>
        </div>
      </div>

      <div class="toolbar-right">
        <!-- 排序选择 -->
        <div class="vocab-select-wrap">
          <select v-model="vocabStore.activeFilter.sortBy" @change="onFilterChange">
            <option value="date_desc">添加时间 (最新优先)</option>
            <option value="date_asc">添加时间 (最早优先)</option>
            <option value="alpha_asc">字母排序 (A → Z)</option>
            <option value="alpha_desc">字母排序 (Z → A)</option>
            <option value="mastery_asc">掌握度 (待学优先)</option>
          </select>
        </div>

        <!-- 视图切换 -->
        <div class="view-switch-btns">
          <button
            class="view-btn"
            :class="{ active: vocabStore.currentViewMode === 'list' }"
            title="网格卡片视图"
            @click="vocabStore.currentViewMode = 'list'"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/></svg>
            <span>卡片</span>
          </button>
          <button
            class="view-btn"
            :class="{ active: vocabStore.currentViewMode === 'split' }"
            title="词典分栏视图"
            @click="vocabStore.currentViewMode = 'split'"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="18" height="18" x="3" y="3" rx="2"/><path d="M9 3v18"/></svg>
            <span>词典</span>
          </button>
          <button
            class="view-btn flashcard-view-btn"
            :class="{ active: vocabStore.currentViewMode === 'flashcard' }"
            title="记忆闪卡自测模式"
            @click="startFlashcards"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="16" height="20" x="4" y="2" rx="2"/><path d="M12 6v4"/><path d="M10 8h4"/></svg>
            <span>闪卡复习</span>
          </button>
        </div>

        <button class="spatial-btn spatial-btn-accent add-word-btn" @click="openAddWordModal">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 5v14M5 12h14"/></svg>
          <span>手动添加</span>
        </button>
      </div>
    </section>

    <!-- 视图 1：网格卡片视图 (List / Grid View) -->
    <section v-if="vocabStore.currentViewMode === 'list'" class="vocab-grid-view">
      <div v-if="vocabStore.filteredItems.length === 0" class="vocab-empty-card">
        <div class="empty-icon">📖</div>
        <h3>暂无匹配的词汇</h3>
        <p>在「文献阅读」中选中任意专业单词，即可点击「加入词库」一键收录；也可以点击右上角「手动添加」。</p>
        <button class="spatial-btn spatial-btn-accent" @click="openAddWordModal">手动添加生词</button>
      </div>

      <div v-else>
        <div class="vocab-card-grid">
          <article
            v-for="item in vocabStore.paginatedItems"
            :key="item.id"
            class="vocab-card"
            :class="{
              'is-new': !item.masteryLevel || item.masteryLevel === 0,
              'is-familiar': item.masteryLevel === 1,
              'is-mastered': item.masteryLevel === 2
            }"
          >
            <!-- 卡片头部 -->
            <div class="card-head">
              <div class="word-title-group">
                <h2 class="word-name">{{ item.word }}</h2>
                <span v-if="item.phonetic" class="word-phonetic">{{ item.phonetic }}</span>
                <button class="audio-tts-btn" title="朗读发音" @click="playWordAudio(item.word)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/></svg>
                </button>
              </div>
              <div class="card-badges">
                <span v-if="item.oxfordLevel" class="oxford-level-badge">{{ item.oxfordLevel }}</span>
                <span class="mastery-pill" :class="`level-${item.masteryLevel || 0}`">
                  {{ item.masteryLevel === 2 ? '已掌握' : item.masteryLevel === 1 ? '熟悉' : '待学习' }}
                </span>
              </div>
            </div>

            <!-- 词性与中文释义 -->
            <div class="card-meaning-block">
              <span v-if="item.partOfSpeech" class="pos-badge">{{ item.partOfSpeech }}</span>
              <p class="meaning-cn">{{ item.meaningCn }}</p>
            </div>

            <!-- 英文释义 -->
            <p v-if="item.meaningEn" class="meaning-en">{{ item.meaningEn }}</p>

            <!-- 论文出处与原文例句 -->
            <div v-if="item.contextSentence" class="context-citation-box">
              <div class="citation-header">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
                <span class="citation-source" :title="item.paperTitle">来源：《{{ item.paperTitle || '文献阅读' }}》</span>
              </div>
              <blockquote class="citation-quote" v-html="highlightWordInSentence(item.contextSentence, item.word)"></blockquote>
              <p v-if="item.contextTranslation" class="citation-trans">{{ item.contextTranslation }}</p>
            </div>

            <!-- 常用学术搭配标签 -->
            <div v-if="item.collocations && item.collocations.length > 0" class="collocation-tags-wrap">
              <span class="collocation-lbl">搭配:</span>
              <div class="collocation-tags">
                <span v-for="(col, cIdx) in item.collocations.slice(0, 3)" :key="cIdx" class="col-pill" :title="col.cn">
                  {{ col.en }}
                </span>
              </div>
            </div>

            <!-- 词根词缀助记 -->
            <div v-if="item.etymology" class="etymology-box">
              <span class="etymology-icon">💡 助记：</span>
              <span class="etymology-text">{{ item.etymology }}</span>
            </div>

            <!-- 卡片底栏操作区 -->
            <div class="card-foot">
              <div class="mastery-quick-toggle">
                <button
                  class="mastery-dot-btn"
                  :class="{ active: item.masteryLevel === 0 }"
                  title="标记为待学习"
                  @click="vocabStore.updateMastery(item.id, 0)"
                >
                  生词
                </button>
                <button
                  class="mastery-dot-btn"
                  :class="{ active: item.masteryLevel === 1 }"
                  title="标记为熟悉"
                  @click="vocabStore.updateMastery(item.id, 1)"
                >
                  熟悉
                </button>
                <button
                  class="mastery-dot-btn"
                  :class="{ active: item.masteryLevel === 2 }"
                  title="标记为已掌握"
                  @click="vocabStore.updateMastery(item.id, 2)"
                >
                  掌握
                </button>
              </div>

              <div class="card-actions">
                <button
                  class="action-icon-btn spelling-btn"
                  :class="{ active: getSpellingState(item.id).open }"
                  title="自主拼写练习"
                  @click="toggleSpelling(item.id)"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                </button>
                <button
                  class="action-icon-btn detail-btn"
                  title="在双栏词典中查看详情"
                  @click="viewInSplit(item.id)"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 3h6v6"/><path d="M10 14 21 3"/><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/></svg>
                </button>
                <button
                  class="action-icon-btn delete-btn"
                  title="从生词本删除"
                  @click="confirmDeleteWord(item)"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
                </button>
              </div>
            </div>

            <!-- 单词拼写自测抽屉卡片 -->
            <Transition name="spelling-expand">
              <div
                v-if="getSpellingState(item.id).open"
                class="card-spelling-drawer"
                :class="{
                  'is-correct': getSpellingState(item.id).isCorrect,
                  'is-shake': getSpellingState(item.id).shake
                }"
              >
                <div class="spelling-drawer-header">
                  <div class="spelling-title">
                    <span class="spelling-badge">✍️ 拼写自测</span>
                    <small class="letter-count-hint">{{ item.word.length }} 个字母</small>
                  </div>
                  <button class="spelling-close-btn" @click="toggleSpelling(item.id)" title="收起拼写">×</button>
                </div>

                <div class="spelling-hints-row">
                  <span class="spelling-meaning-cn">{{ item.meaningCn }}</span>
                  <button class="spelling-audio-btn" type="button" title="听发音辅助拼写" @click.stop="playWordAudio(item.word)">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>
                    <span>听发音</span>
                  </button>
                </div>

                <form class="spelling-input-group" @submit.prevent="checkSpelling(item)">
                  <input
                    v-model="getSpellingState(item.id).input"
                    type="text"
                    class="spelling-text-input"
                    :class="{
                      'input-correct': getSpellingState(item.id).checked && getSpellingState(item.id).isCorrect,
                      'input-wrong': getSpellingState(item.id).checked && !getSpellingState(item.id).isCorrect
                    }"
                    placeholder="输入该词英文拼写…"
                    autocomplete="off"
                    spellcheck="false"
                    :disabled="getSpellingState(item.id).isCorrect"
                  />
                  <button
                    v-if="!getSpellingState(item.id).isCorrect"
                    type="submit"
                    class="spelling-submit-btn"
                    :disabled="!getSpellingState(item.id).input.trim()"
                  >
                    检查
                  </button>
                  <button
                    v-else
                    type="button"
                    class="spelling-retry-btn"
                    @click="resetSpelling(item.id)"
                  >
                    再练一次
                  </button>
                </form>

                <div v-if="getSpellingState(item.id).checked" class="spelling-feedback">
                  <div v-if="getSpellingState(item.id).isCorrect" class="feedback-success">
                    <span>🎉 拼写完全正确！</span>
                    <small>掌握度已提升</small>
                  </div>
                  <div v-else class="feedback-error">
                    <span>❌ 拼写有误，再试一次</span>
                    <button
                      v-if="!getSpellingState(item.id).showAnswer"
                      type="button"
                      class="peek-answer-btn"
                      @click="revealSpellingAnswer(item.id)"
                    >
                      查看答案
                    </button>
                    <div v-else class="revealed-answer">
                      正确答案：<strong class="answer-highlight">{{ item.word }}</strong>
                    </div>
                  </div>
                </div>
              </div>
            </Transition>
          </article>
        </div>

        <!-- 分页控制器 -->
        <div class="vocab-pagination-card" v-if="vocabStore.filteredItems.length > 0">
          <div class="pagination-info">
            显示第 <strong>{{ (vocabStore.currentPage - 1) * vocabStore.pageSize + 1 }}</strong> -
            <strong>{{ Math.min(vocabStore.currentPage * vocabStore.pageSize, vocabStore.filteredItems.length) }}</strong> 条，
            共 <strong>{{ vocabStore.filteredItems.length }}</strong> 条词汇
          </div>

          <div class="pagination-controls">
            <button
              class="page-nav-btn"
              :disabled="vocabStore.currentPage <= 1"
              @click="vocabStore.setPage(vocabStore.currentPage - 1)"
            >
              ◀ 上一页
            </button>

            <div class="page-numbers">
              <button
                v-for="p in visiblePageNumbers"
                :key="p"
                class="page-num-btn"
                :class="{ active: vocabStore.currentPage === p, ellipsis: p === '...' }"
                :disabled="p === '...'"
                @click="p !== '...' && vocabStore.setPage(p)"
              >
                {{ p }}
              </button>
            </div>

            <button
              class="page-nav-btn"
              :disabled="vocabStore.currentPage >= vocabStore.totalPages"
              @click="vocabStore.setPage(vocabStore.currentPage + 1)"
            >
              下一页 ▶
            </button>

            <div class="page-size-selector">
              <select v-model="vocabStore.pageSize" @change="vocabStore.setPageSize($event.target.value)">
                <option :value="12">12 条/页</option>
                <option :value="24">24 条/页</option>
                <option :value="48">48 条/页</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 视图 2：双栏词典详情视图 (Split View) -->
    <section v-else-if="vocabStore.currentViewMode === 'split'" class="vocab-split-view">
      <!-- 左栏：紧凑生词目录列表 -->
      <aside class="split-sidebar">
        <div class="sidebar-count-bar">
          <span>共 {{ vocabStore.filteredItems.length }} 个词汇</span>
        </div>
        <div class="split-word-list">
          <div
            v-for="w in vocabStore.filteredItems"
            :key="w.id"
            class="split-word-item"
            :class="{ active: vocabStore.selectedWord?.id === w.id }"
            @click="vocabStore.selectedWordId = w.id"
          >
            <div class="split-item-head">
              <strong class="split-word-text">{{ w.word }}</strong>
              <span class="split-mastery-dot" :class="`level-${w.masteryLevel || 0}`"></span>
            </div>
            <p class="split-item-def">{{ w.meaningCn || '未解析' }}</p>
            <small v-if="w.paperTitle" class="split-item-paper">{{ truncateText(w.paperTitle, 20) }}</small>
          </div>
        </div>
      </aside>

      <!-- 右栏：牛津级深度词典卡片 -->
      <main class="split-main-card" v-if="vocabStore.selectedWord">
        <div class="oxford-detail-wrapper">
          <!-- 头部信息 -->
          <div class="detail-header-card">
            <div class="detail-title-row">
              <div class="detail-word-group">
                <h1 class="detail-word-heading">{{ vocabStore.selectedWord.word }}</h1>
                <span class="detail-phonetic">{{ vocabStore.selectedWord.phonetic }}</span>
                <button class="audio-play-large-btn" @click="playWordAudio(vocabStore.selectedWord.word)">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/></svg>
                  <span>朗读</span>
                </button>
              </div>

              <div class="detail-actions-group">
                <span v-if="vocabStore.selectedWord.oxfordLevel" class="oxford-pill-large">
                  {{ vocabStore.selectedWord.oxfordLevel }}
                </span>
                <div class="mastery-segmented-control">
                  <button
                    :class="{ active: vocabStore.selectedWord.masteryLevel === 0 }"
                    @click="vocabStore.updateMastery(vocabStore.selectedWord.id, 0)"
                  >
                    生词
                  </button>
                  <button
                    :class="{ active: vocabStore.selectedWord.masteryLevel === 1 }"
                    @click="vocabStore.updateMastery(vocabStore.selectedWord.id, 1)"
                  >
                    熟悉
                  </button>
                  <button
                    :class="{ active: vocabStore.selectedWord.masteryLevel === 2 }"
                    @click="vocabStore.updateMastery(vocabStore.selectedWord.id, 2)"
                  >
                    牢记
                  </button>
                </div>
              </div>
            </div>

            <!-- 原文献出处溯源框 -->
            <div v-if="vocabStore.selectedWord.contextSentence" class="detail-provenance-card">
              <div class="provenance-tag-row">
                <span class="provenance-badge">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
                  文献原文摘录
                </span>
                <span class="provenance-title">{{ vocabStore.selectedWord.paperTitle || '文献阅读' }}</span>
              </div>
              <blockquote class="provenance-quote" v-html="highlightWordInSentence(vocabStore.selectedWord.contextSentence, vocabStore.selectedWord.word)"></blockquote>
              <p v-if="vocabStore.selectedWord.contextTranslation" class="provenance-translation">
                💡 {{ vocabStore.selectedWord.contextTranslation }}
              </p>
            </div>
          </div>

          <!-- 牛津英汉权威双解模块 -->
          <section class="oxford-section-card">
            <h3 class="section-title">
              <span class="section-num">01</span>
              <span>牛津/柯林斯 权威双解</span>
            </h3>
            <div class="definition-main-row">
              <span v-if="vocabStore.selectedWord.partOfSpeech" class="pos-badge-lg">{{ vocabStore.selectedWord.partOfSpeech }}</span>
              <div class="def-text-wrap">
                <div class="def-chinese">{{ vocabStore.selectedWord.meaningCn }}</div>
                <div class="def-english">{{ vocabStore.selectedWord.meaningEn }}</div>
              </div>
            </div>
          </section>

          <!-- 学术搭配与常用短语 -->
          <section class="oxford-section-card" v-if="vocabStore.selectedWord.collocations && vocabStore.selectedWord.collocations.length > 0">
            <h3 class="section-title">
              <span class="section-num">02</span>
              <span>学术常用搭配 (Academic Collocations)</span>
            </h3>
            <div class="collocations-grid">
              <div v-for="(col, idx) in vocabStore.selectedWord.collocations" :key="idx" class="collocation-card">
                <div class="col-en">{{ col.en }}</div>
                <div class="col-cn">{{ col.cn }}</div>
              </div>
            </div>
          </section>

          <!-- 顶级期刊学术例句 -->
          <section class="oxford-section-card" v-if="vocabStore.selectedWord.academicExamples && vocabStore.selectedWord.academicExamples.length > 0">
            <h3 class="section-title">
              <span class="section-num">03</span>
              <span>权威期刊经典例句</span>
            </h3>
            <div class="academic-examples-list">
              <div v-for="(eg, idx) in vocabStore.selectedWord.academicExamples" :key="idx" class="example-item-card">
                <div class="eg-header">
                  <span class="eg-num">例句 {{ idx + 1 }}</span>
                  <span v-if="eg.source" class="eg-source-badge">{{ eg.source }}</span>
                </div>
                <p class="eg-en" v-html="highlightWordInSentence(eg.en, vocabStore.selectedWord.word)"></p>
                <p class="eg-cn">{{ eg.cn }}</p>
              </div>
            </div>
          </section>

          <!-- 词根词缀与联想记忆 -->
          <section class="oxford-section-card" v-if="vocabStore.selectedWord.etymology">
            <h3 class="section-title">
              <span class="section-num">04</span>
              <span>词根词缀与联想记忆 (Etymology & Mnemonics)</span>
            </h3>
            <div class="etymology-detail-box">
              <p class="etymology-body">{{ vocabStore.selectedWord.etymology }}</p>
            </div>
          </section>

          <!-- 同义词与近义辨析 -->
          <section class="oxford-section-card" v-if="vocabStore.selectedWord.synonyms && vocabStore.selectedWord.synonyms.length > 0">
            <h3 class="section-title">
              <span class="section-num">05</span>
              <span>同义替换与近义辨析</span>
            </h3>
            <div class="synonyms-pill-list">
              <span v-for="(syn, idx) in vocabStore.selectedWord.synonyms" :key="idx" class="syn-pill">
                {{ syn }}
              </span>
            </div>
          </section>

          <!-- 单词自主拼写强化自测模块 -->
          <section class="oxford-section-card oxford-spelling-section">
            <h3 class="section-title">
              <span class="section-num">06</span>
              <span>单词拼写自测 (Spelling Practice)</span>
            </h3>
            <div class="detail-spelling-box" :class="{ 'is-correct': getSpellingState(vocabStore.selectedWord.id).isCorrect, 'is-shake': getSpellingState(vocabStore.selectedWord.id).shake }">
              <div class="detail-spelling-prompt">
                <div class="spelling-clue">
                  <span class="clue-tag">释义提示</span>
                  <strong class="clue-text">{{ vocabStore.selectedWord.meaningCn }}</strong>
                </div>
                <button class="spelling-audio-btn-lg" type="button" title="听发音辅助拼写" @click="playWordAudio(vocabStore.selectedWord.word)">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>
                  <span>听发音 ({{ vocabStore.selectedWord.word.length }} 字母)</span>
                </button>
              </div>

              <form class="detail-spelling-form" @submit.prevent="checkSpelling(vocabStore.selectedWord)">
                <input
                  v-model="getSpellingState(vocabStore.selectedWord.id).input"
                  type="text"
                  class="detail-spelling-input"
                  :class="{
                    'input-correct': getSpellingState(vocabStore.selectedWord.id).checked && getSpellingState(vocabStore.selectedWord.id).isCorrect,
                    'input-wrong': getSpellingState(vocabStore.selectedWord.id).checked && !getSpellingState(vocabStore.selectedWord.id).isCorrect
                  }"
                  placeholder="请在键盘输入该词拼写并回车…"
                  autocomplete="off"
                  spellcheck="false"
                  :disabled="getSpellingState(vocabStore.selectedWord.id).isCorrect"
                />
                <button
                  v-if="!getSpellingState(vocabStore.selectedWord.id).isCorrect"
                  type="submit"
                  class="detail-spelling-submit"
                  :disabled="!getSpellingState(vocabStore.selectedWord.id).input.trim()"
                >
                  验证拼写
                </button>
                <button
                  v-else
                  type="button"
                  class="detail-spelling-retry"
                  @click="resetSpelling(vocabStore.selectedWord.id)"
                >
                  再测一次
                </button>
              </form>

              <div v-if="getSpellingState(vocabStore.selectedWord.id).checked" class="detail-spelling-feedback">
                <div v-if="getSpellingState(vocabStore.selectedWord.id).isCorrect" class="feedback-success-lg">
                  <span>🎉 恭喜！拼写完全正确！</span>
                  <small>已为您同步强化记忆并提升掌握等级</small>
                </div>
                <div v-else class="feedback-error-lg">
                  <span>❌ 拼写有误，请核对并重试</span>
                  <button
                    v-if="!getSpellingState(vocabStore.selectedWord.id).showAnswer"
                    type="button"
                    class="peek-answer-btn-lg"
                    @click="revealSpellingAnswer(vocabStore.selectedWord.id)"
                  >
                    查看正确拼写
                  </button>
                  <div v-else class="revealed-answer-lg">
                    正确拼写：<strong class="answer-highlight">{{ vocabStore.selectedWord.word }}</strong>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </main>
    </section>

    <!-- 视图 3：记忆闪卡自测模式 (Flashcard Mode) -->
    <section v-else-if="vocabStore.currentViewMode === 'flashcard'" class="vocab-flashcard-view">
      <div v-if="vocabStore.filteredItems.length === 0" class="vocab-empty-card">
        <h3>当前筛选条件下无可用闪卡</h3>
        <button class="spatial-btn spatial-btn-accent" @click="vocabStore.activeFilter.masteryLevel = 'all'">重置筛选</button>
      </div>

      <div v-else class="flashcard-deck-container">
        <!-- 顶部闪卡进度 -->
        <div class="flashcard-nav-bar">
          <div class="card-counter">
            <span>闪卡进度：</span>
            <strong>{{ vocabStore.flashcardIndex + 1 }}</strong> / {{ vocabStore.filteredItems.length }}
          </div>
          <div class="deck-progress-track">
            <div class="deck-progress-fill" :style="{ width: `${((vocabStore.flashcardIndex + 1) / vocabStore.filteredItems.length) * 100}%` }"></div>
          </div>
          <button class="spatial-btn spatial-btn-ghost exit-flashcard-btn" @click="vocabStore.currentViewMode = 'list'">
            退出自测
          </button>
        </div>

        <!-- 3D 翻转闪卡主体 -->
        <div class="flashcard-scene" @click="vocabStore.flashcardFlipped = !vocabStore.flashcardFlipped">
          <div class="flashcard-cube" :class="{ 'is-flipped': vocabStore.flashcardFlipped }">
            <!-- 正面 (Front): 单词 + 语境例句挖空 -->
            <div class="flashcard-face flashcard-front">
              <div class="card-face-header">
                <span class="badge-front">自测正面 · 点击卡片翻转</span>
                <span class="source-tag" v-if="currentFlashcard.paperTitle">来源：《{{ truncateText(currentFlashcard.paperTitle, 20) }}》</span>
              </div>

              <div class="front-center">
                <h1 class="flashcard-word">{{ currentFlashcard.word }}</h1>
                <div class="front-phonetic-row">
                  <span class="flashcard-phonetic">{{ currentFlashcard.phonetic }}</span>
                  <button class="audio-tts-btn-round" @click.stop="playWordAudio(currentFlashcard.word)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>
                  </button>
                </div>

                <div v-if="currentFlashcard.contextSentence" class="front-context-quote">
                  <p class="quote-title">文献语境例句（试着回忆该词含义）：</p>
                  <blockquote v-html="maskWordInSentence(currentFlashcard.contextSentence, currentFlashcard.word)"></blockquote>
                </div>
              </div>

              <div class="card-face-footer">
                <span class="flip-prompt-text">💡 提示：尝试在脑海中回忆中文释义与搭配，点击卡片查看答案</span>
              </div>
            </div>

            <!-- 背面 (Back): 权威双解 + 搭配 + 助记 -->
            <div class="flashcard-face flashcard-back">
              <div class="card-face-header">
                <span class="badge-back">释义与学术解析 · 点击翻回正面</span>
                <span class="oxford-pill-sm">{{ currentFlashcard.oxfordLevel || 'Academic' }}</span>
              </div>

              <div class="back-scroll-content">
                <div class="back-word-row">
                  <h2>{{ currentFlashcard.word }}</h2>
                  <span class="back-pos">{{ currentFlashcard.partOfSpeech }}</span>
                  <span class="back-phonetic">{{ currentFlashcard.phonetic }}</span>
                </div>

                <div class="back-meaning-card">
                  <div class="back-cn">{{ currentFlashcard.meaningCn }}</div>
                  <div class="back-en">{{ currentFlashcard.meaningEn }}</div>
                </div>

                <div v-if="currentFlashcard.contextSentence" class="back-original-context">
                  <span class="orig-lbl">原文例句完整对照：</span>
                  <p class="orig-en" v-html="highlightWordInSentence(currentFlashcard.contextSentence, currentFlashcard.word)"></p>
                  <p class="orig-cn">{{ currentFlashcard.contextTranslation }}</p>
                </div>

                <div v-if="currentFlashcard.collocations && currentFlashcard.collocations.length > 0" class="back-collocation-row">
                  <span class="col-lbl">常用学术搭配：</span>
                  <div class="back-cols">
                    <span v-for="(c, cIdx) in currentFlashcard.collocations" :key="cIdx" class="back-col-tag">
                      <b>{{ c.en }}</b> ({{ c.cn }})
                    </span>
                  </div>
                </div>

                <div v-if="currentFlashcard.etymology" class="back-etymology-box">
                  <span>💡 助记：{{ currentFlashcard.etymology }}</span>
                </div>
              </div>

              <div class="card-face-footer">
                <span class="flip-prompt-text">请对本次回忆效果进行评估打分</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部打分与切换控制器 -->
        <div class="flashcard-control-bar">
          <button class="flashcard-arrow-btn" :disabled="vocabStore.flashcardIndex <= 0" @click="prevFlashcard">
            ← 上一张
          </button>

          <div class="grade-buttons-group">
            <button class="grade-btn grade-forgot" @click="rateFlashcard('forgot')">
              <span class="grade-icon">❌</span>
              <span>遗忘 (生词)</span>
            </button>
            <button class="grade-btn grade-vague" @click="rateFlashcard('vague')">
              <span class="grade-icon">⚠️</span>
              <span>模糊 (需巩固)</span>
            </button>
            <button class="grade-btn grade-mastered" @click="rateFlashcard('mastered')">
              <span class="grade-icon">✅</span>
              <span>牢记 (已掌握)</span>
            </button>
          </div>

          <button class="flashcard-arrow-btn" :disabled="vocabStore.flashcardIndex >= vocabStore.filteredItems.length - 1" @click="nextFlashcard">
            下一张 →
          </button>
        </div>
      </div>
    </section>

    <!-- 手动添加生词模态框 -->
    <div v-if="showAddModal" class="vocab-modal-overlay" @click.self="showAddModal = false">
      <div class="vocab-modal-card">
        <header class="modal-head">
          <h3>手动添加学术词汇</h3>
          <button class="close-modal-btn" @click="showAddModal = false">×</button>
        </header>
        <div class="modal-body">
          <label class="form-item">
            <span class="form-lbl">单词或学术短语 <em class="req">*</em></span>
            <input
              v-model="newWordForm.word"
              type="text"
              placeholder="例如：preponderance, ablation study…"
              autofocus
            />
          </label>
          <label class="form-item">
            <span class="form-lbl">原文献例句 / 语境（可选）</span>
            <textarea
              v-model="newWordForm.contextSentence"
              rows="3"
              placeholder="粘贴该词在论文中出现的句子，系统将自动高亮并生成精准语境翻译…"
            ></textarea>
          </label>
          <label class="form-item">
            <span class="form-lbl">来源文献名称（可选）</span>
            <input
              v-model="newWordForm.paperTitle"
              type="text"
              placeholder="例如：Attention Is All You Need"
            />
          </label>
        </div>
        <footer class="modal-foot">
          <button class="spatial-btn spatial-btn-ghost" @click="showAddModal = false">取消</button>
          <button
            class="spatial-btn spatial-btn-accent"
            :disabled="!newWordForm.word.trim() || isAddingWord"
            @click="submitAddWord"
          >
            {{ isAddingWord ? '正在收录…' : '立即收录并解析' }}
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from "vue";
import { useVocabularyStore } from "../stores/vocabulary";

const vocabStore = useVocabularyStore();

const showAddModal = ref(false);
const isAddingWord = ref(false);

const newWordForm = ref({
  word: "",
  contextSentence: "",
  paperTitle: ""
});

// ── 单词自主拼写练习状态管理 ──
const spellingStates = reactive({});

function getSpellingState(wordId) {
  if (!spellingStates[wordId]) {
    spellingStates[wordId] = {
      open: false,
      input: "",
      checked: false,
      isCorrect: false,
      showAnswer: false,
      shake: false,
      attempts: 0
    };
  }
  return spellingStates[wordId];
}

function toggleSpelling(wordId) {
  const state = getSpellingState(wordId);
  state.open = !state.open;
  if (state.open) {
    state.input = "";
    state.checked = false;
    state.isCorrect = false;
    state.showAnswer = false;
    state.shake = false;
  }
}

function checkSpelling(item) {
  if (!item?.id || !item?.word) return;
  const state = getSpellingState(item.id);
  const typed = String(state.input || "").trim().toLowerCase();
  const target = String(item.word || "").trim().toLowerCase();
  if (!typed) return;

  state.checked = true;
  state.attempts++;

  if (typed === target) {
    state.isCorrect = true;
    state.shake = false;
    playWordAudio(item.word);
    // 自动提升掌握度
    if (item.masteryLevel < 2) {
      vocabStore.updateMastery(item.id, Math.min(2, (item.masteryLevel || 0) + 1));
    }
  } else {
    state.isCorrect = false;
    state.shake = true;
    setTimeout(() => {
      state.shake = false;
    }, 600);
  }
}

function revealSpellingAnswer(wordId) {
  const state = getSpellingState(wordId);
  state.showAnswer = true;
}

function resetSpelling(wordId) {
  const state = getSpellingState(wordId);
  state.input = "";
  state.checked = false;
  state.isCorrect = false;
  state.showAnswer = false;
  state.shake = false;
}

onMounted(() => {
  vocabStore.init();
});

const stats = computed(() => vocabStore.stats);

const currentFlashcard = computed(() => {
  const items = vocabStore.filteredItems;
  return items[vocabStore.flashcardIndex] || items[0] || {};
});

// 分页数字生成算法（支持智能折叠省略号）
const visiblePageNumbers = computed(() => {
  const total = vocabStore.totalPages;
  const current = vocabStore.currentPage;
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1);
  }
  const pages = [];
  pages.push(1);
  if (current > 3) {
    pages.push("...");
  }
  const start = Math.max(2, current - 1);
  const end = Math.min(total - 1, current + 1);
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  if (current < total - 2) {
    pages.push("...");
  }
  pages.push(total);
  return pages;
});

function onFilterChange() {
  vocabStore.currentPage = 1;
}

function clearKeyword() {
  vocabStore.activeFilter.keyword = "";
  vocabStore.currentPage = 1;
}

function setMasteryFilter(level) {
  vocabStore.activeFilter.masteryLevel = level;
  vocabStore.currentPage = 1;
}

function truncateText(str, maxLen = 20) {
  if (!str) return "";
  return str.length > maxLen ? str.slice(0, maxLen) + "…" : str;
}

function playWordAudio(word) {
  if (!word) return;
  if ("speechSynthesis" in window) {
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(word);
    utterance.lang = "en-US";
    utterance.rate = 0.9;
    window.speechSynthesis.speak(utterance);
  }
}

function highlightWordInSentence(sentence, targetWord) {
  if (!sentence || !targetWord) return sentence || "";
  try {
    const escaped = targetWord.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const regex = new RegExp(`(\\b${escaped}\\b|${escaped})`, "gi");
    return sentence.replace(regex, `<mark class="vocab-word-mark">$1</mark>`);
  } catch {
    return sentence;
  }
}

function maskWordInSentence(sentence, targetWord) {
  if (!sentence || !targetWord) return sentence || "";
  try {
    const escaped = targetWord.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const regex = new RegExp(`(\\b${escaped}\\b|${escaped})`, "gi");
    return sentence.replace(regex, `<span class="vocab-mask-blank">[ _______ ]</span>`);
  } catch {
    return sentence;
  }
}

function viewInSplit(wordId) {
  vocabStore.selectedWordId = wordId;
  vocabStore.currentViewMode = "split";
}

function startFlashcards() {
  vocabStore.flashcardIndex = 0;
  vocabStore.flashcardFlipped = false;
  vocabStore.currentViewMode = "flashcard";
}

function prevFlashcard() {
  if (vocabStore.flashcardIndex > 0) {
    vocabStore.flashcardIndex--;
    vocabStore.flashcardFlipped = false;
  }
}

function nextFlashcard() {
  if (vocabStore.flashcardIndex < vocabStore.filteredItems.length - 1) {
    vocabStore.flashcardIndex++;
    vocabStore.flashcardFlipped = false;
  }
}

async function rateFlashcard(grade) {
  const word = currentFlashcard.value;
  if (word?.id) {
    await vocabStore.recordReview(word.id, grade);
  }
  if (vocabStore.flashcardIndex < vocabStore.filteredItems.length - 1) {
    nextFlashcard();
  } else {
    vocabStore.flashcardFlipped = false;
    alert("🎉 本轮闪卡自测已完成！");
  }
}

function openAddWordModal() {
  newWordForm.value = { word: "", contextSentence: "", paperTitle: "" };
  showAddModal.value = true;
}

async function submitAddWord() {
  if (!newWordForm.value.word.trim()) return;
  isAddingWord.value = true;
  try {
    await vocabStore.addWord({
      word: newWordForm.value.word,
      contextSentence: newWordForm.value.contextSentence,
      paperTitle: newWordForm.value.paperTitle
    });
    showAddModal.value = false;
  } finally {
    isAddingWord.value = false;
  }
}

function confirmDeleteWord(item) {
  if (confirm(`确定要从生词库中删除「${item.word}」吗？`)) {
    vocabStore.removeWord(item.id);
  }
}
</script>

<style scoped>
.vocabulary-page {
  max-width: 1380px;
  margin: 0 auto;
  padding: 24px 28px 80px;
  box-sizing: border-box;
}

/* 顶部标题与统计概览 */
.vocab-header {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 24px;
  align-items: center;
  margin-bottom: 24px;
}
@media (max-width: 1024px) {
  .vocab-header {
    grid-template-columns: 1fr;
  }
}

.vocab-brand-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(79, 70, 229, 0.1);
  color: #4f46e5;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 10px;
}
:root[data-theme="dark"] .vocab-brand-badge {
  background: rgba(99, 102, 241, 0.2);
  color: #818cf8;
}

.vocab-title-wrap h1 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
}
:root[data-theme="dark"] .vocab-title-wrap h1 {
  color: #f8fafc;
}

.vocab-title-wrap p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
}
:root[data-theme="dark"] .vocab-title-wrap p {
  color: #94a3b8;
}

/* 统计卡片网格 */
.vocab-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 150px);
  gap: 14px;
}
@media (max-width: 768px) {
  .vocab-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
}
:root[data-theme="dark"] .stat-card {
  background: #1e293b;
  border-color: #334155;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.stat-lbl {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 6px;
}
:root[data-theme="dark"] .stat-lbl {
  color: #94a3b8;
}

.stat-val-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 4px;
}

.stat-val {
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
}
:root[data-theme="dark"] .stat-val {
  color: #f8fafc;
}

.highlight-blue { color: #2563eb; }
.highlight-amber { color: #d97706; }
.highlight-emerald { color: #059669; }

.stat-unit {
  font-size: 12px;
  color: #64748b;
}

.stat-progress-wrap {
  width: 100%;
  height: 5px;
  background: #f1f5f9;
  border-radius: 999px;
  overflow: hidden;
  margin: 4px 0 2px;
}
:root[data-theme="dark"] .stat-progress-wrap {
  background: #334155;
}

.stat-progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #10b981, #059669);
  border-radius: 999px;
  transition: width 0.3s ease;
}

.stat-hint {
  font-size: 11px;
  color: #94a3b8;
  margin-top: auto;
}

/* 工具栏与筛选区 */
.vocab-toolbar-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 14px 18px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 24px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
}
:root[data-theme="dark"] .vocab-toolbar-card {
  background: #1e293b;
  border-color: #334155;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.vocab-search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 7px 12px;
  width: 260px;
  color: #64748b;
  transition: all 0.2s;
}
:root[data-theme="dark"] .vocab-search-box {
  background: #0f172a;
  border-color: #334155;
  color: #94a3b8;
}
.vocab-search-box:focus-within {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
  background: #ffffff;
}
:root[data-theme="dark"] .vocab-search-box:focus-within {
  background: #0f172a;
  border-color: #6366f1;
}

.vocab-search-box input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #0f172a;
  width: 100%;
}
:root[data-theme="dark"] .vocab-search-box input {
  color: #f8fafc;
}

.clear-btn {
  border: none;
  background: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
}

.vocab-select-wrap select {
  height: 34px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  border-radius: 10px;
  padding: 0 10px;
  font-size: 13px;
  color: #334155;
  outline: none;
  cursor: pointer;
}
:root[data-theme="dark"] .vocab-select-wrap select {
  background: #0f172a;
  border-color: #334155;
  color: #e2e8f0;
}

.mastery-tabs {
  display: flex;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 10px;
  gap: 2px;
}
:root[data-theme="dark"] .mastery-tabs {
  background: #0f172a;
}

.tab-btn {
  border: none;
  background: transparent;
  padding: 5px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}
:root[data-theme="dark"] .tab-btn {
  color: #94a3b8;
}
.tab-btn.active {
  background: #ffffff;
  color: #4f46e5;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}
:root[data-theme="dark"] .tab-btn.active {
  background: #334155;
  color: #818cf8;
}

.view-switch-btns {
  display: flex;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 10px;
  gap: 2px;
}
:root[data-theme="dark"] .view-switch-btns {
  background: #0f172a;
}

.view-btn {
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}
:root[data-theme="dark"] .view-btn {
  color: #94a3b8;
}
.view-btn.active {
  background: #ffffff;
  color: #4f46e5;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}
:root[data-theme="dark"] .view-btn.active {
  background: #334155;
  color: #818cf8;
}

.flashcard-view-btn.active {
  color: #d97706;
}

/* 导出下拉菜单 */
.action-dropdown-wrap {
  position: relative;
}

.export-menu-popover {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 6px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.12);
  z-index: 50;
  width: 250px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
:root[data-theme="dark"] .export-menu-popover {
  background: #1e293b;
  border-color: #334155;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
}

.export-menu-popover button {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s;
}
.export-menu-popover button:hover {
  background: #f8fafc;
}
:root[data-theme="dark"] .export-menu-popover button:hover {
  background: #334155;
}

.export-icon {
  font-size: 18px;
}
.export-meta strong {
  display: block;
  font-size: 13px;
  color: #0f172a;
}
:root[data-theme="dark"] .export-meta strong {
  color: #f8fafc;
}
.export-meta small {
  display: block;
  font-size: 11px;
  color: #64748b;
}

/* 网格卡片列表 */
.vocab-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
}

.vocab-empty-card {
  text-align: center;
  padding: 60px 20px;
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
}
:root[data-theme="dark"] .vocab-empty-card {
  background: #1e293b;
  border-color: #334155;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.vocab-empty-card h3 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #0f172a;
}
:root[data-theme="dark"] .vocab-empty-card h3 {
  color: #f8fafc;
}
.vocab-empty-card p {
  color: #64748b;
  max-width: 480px;
  margin: 0 auto 20px;
  font-size: 14px;
  line-height: 1.6;
}

.vocab-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
  position: relative;
}
:root[data-theme="dark"] .vocab-card {
  background: #1e293b;
  border-color: #334155;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
.vocab-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  border-color: #cbd5e1;
}
:root[data-theme="dark"] .vocab-card:hover {
  border-color: #475569;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.word-title-group {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
}

.word-name {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.01em;
}
:root[data-theme="dark"] .word-name {
  color: #f8fafc;
}

.word-phonetic {
  font-size: 13px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
:root[data-theme="dark"] .word-phonetic {
  color: #94a3b8;
}

.audio-tts-btn {
  border: none;
  background: #f1f5f9;
  color: #4f46e5;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.15s;
}
:root[data-theme="dark"] .audio-tts-btn {
  background: #334155;
  color: #818cf8;
}
.audio-tts-btn:hover {
  background: #4f46e5;
  color: #ffffff;
  transform: scale(1.08);
}

.card-badges {
  display: flex;
  align-items: center;
  gap: 6px;
}

.oxford-level-badge {
  font-size: 10px;
  font-weight: 700;
  background: rgba(79, 70, 229, 0.08);
  color: #4f46e5;
  padding: 2px 6px;
  border-radius: 6px;
}
:root[data-theme="dark"] .oxford-level-badge {
  background: rgba(99, 102, 241, 0.2);
  color: #a5b4fc;
}

.mastery-pill {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
}
.mastery-pill.level-0 {
  background: #fee2e2;
  color: #b91c1c;
}
.mastery-pill.level-1 {
  background: #fef3c7;
  color: #d97706;
}
.mastery-pill.level-2 {
  background: #d1fae5;
  color: #047857;
}

.card-meaning-block {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.pos-badge {
  font-size: 11px;
  font-weight: 700;
  color: #4f46e5;
  font-style: italic;
}
:root[data-theme="dark"] .pos-badge {
  color: #818cf8;
}

.meaning-cn {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}
:root[data-theme="dark"] .meaning-cn {
  color: #f1f5f9;
}

.meaning-en {
  margin: 0;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}
:root[data-theme="dark"] .meaning-en {
  color: #94a3b8;
}

/* 语境摘录卡片 */
.context-citation-box {
  background: #f8fafc;
  border-left: 3px solid #4f46e5;
  border-radius: 8px;
  padding: 10px 12px;
  margin: 4px 0;
}
:root[data-theme="dark"] .context-citation-box {
  background: #0f172a;
  border-left-color: #6366f1;
}

.citation-header {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 6px;
}
:root[data-theme="dark"] .citation-header {
  color: #94a3b8;
}

.citation-quote {
  margin: 0 0 6px;
  font-size: 13px;
  color: #334155;
  line-height: 1.6;
  font-style: normal;
}
:root[data-theme="dark"] .citation-quote {
  color: #cbd5e1;
}

:deep(.vocab-word-mark) {
  background: transparent;
  text-decoration: underline;
  text-decoration-color: #4f46e5;
  text-decoration-thickness: 2.5px;
  text-underline-offset: 3px;
  font-weight: 700;
  color: #4f46e5;
}
:root[data-theme="dark"] :deep(.vocab-word-mark) {
  color: #818cf8;
  text-decoration-color: #818cf8;
}

.citation-trans {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}
:root[data-theme="dark"] .citation-trans {
  color: #94a3b8;
}

.collocation-tags-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.collocation-lbl {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
}
.col-pill {
  font-size: 11px;
  background: #f1f5f9;
  color: #334155;
  padding: 2px 7px;
  border-radius: 6px;
  font-weight: 600;
}
:root[data-theme="dark"] .col-pill {
  background: #334155;
  color: #e2e8f0;
}

.etymology-box {
  background: #fffbeb;
  border: 1px solid #fef3c7;
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 11px;
  color: #92400e;
  line-height: 1.5;
}
:root[data-theme="dark"] .etymology-box {
  background: rgba(245, 158, 11, 0.12);
  border-color: rgba(245, 158, 11, 0.2);
  color: #fcd34d;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f1f5f9;
  padding-top: 12px;
  margin-top: auto;
}
:root[data-theme="dark"] .card-foot {
  border-top-color: #334155;
}

.mastery-quick-toggle {
  display: flex;
  background: #f1f5f9;
  padding: 2px;
  border-radius: 8px;
  gap: 2px;
}
:root[data-theme="dark"] .mastery-quick-toggle {
  background: #0f172a;
}

.mastery-dot-btn {
  border: none;
  background: transparent;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  padding: 3px 8px;
  border-radius: 6px;
  cursor: pointer;
}
:root[data-theme="dark"] .mastery-dot-btn {
  color: #94a3b8;
}
.mastery-dot-btn.active {
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
:root[data-theme="dark"] .mastery-dot-btn.active {
  background: #334155;
  color: #f8fafc;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-icon-btn {
  border: none;
  background: #f8fafc;
  color: #64748b;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.15s;
}
:root[data-theme="dark"] .action-icon-btn {
  background: #0f172a;
  color: #94a3b8;
}
.action-icon-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}
:root[data-theme="dark"] .action-icon-btn:hover {
  background: #334155;
  color: #f8fafc;
}
.action-icon-btn.delete-btn:hover {
  background: #fee2e2;
  color: #b91c1c;
}

/* 分页条控制组件 */
.vocab-pagination-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 12px 18px;
  margin-top: 24px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}
:root[data-theme="dark"] .vocab-pagination-card {
  background: #1e293b;
  border-color: #334155;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.pagination-info {
  font-size: 13px;
  color: #64748b;
}
:root[data-theme="dark"] .pagination-info {
  color: #94a3b8;
}
.pagination-info strong {
  color: #0f172a;
  font-weight: 700;
}
:root[data-theme="dark"] .pagination-info strong {
  color: #f8fafc;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.page-nav-btn {
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #334155;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}
:root[data-theme="dark"] .page-nav-btn {
  background: #0f172a;
  border-color: #334155;
  color: #e2e8f0;
}
.page-nav-btn:hover:not(:disabled) {
  background: #4f46e5;
  border-color: #4f46e5;
  color: #ffffff;
}
.page-nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

.page-num-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #334155;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
  display: grid;
  place-items: center;
}
:root[data-theme="dark"] .page-num-btn {
  background: #0f172a;
  border-color: #334155;
  color: #cbd5e1;
}
.page-num-btn:hover:not(.active):not(.ellipsis) {
  background: #f1f5f9;
  border-color: #cbd5e1;
}
:root[data-theme="dark"] .page-num-btn:hover:not(.active):not(.ellipsis) {
  background: #334155;
}
.page-num-btn.active {
  background: #4f46e5;
  border-color: #4f46e5;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(79, 70, 229, 0.25);
}
:root[data-theme="dark"] .page-num-btn.active {
  background: #6366f1;
  border-color: #6366f1;
}
.page-num-btn.ellipsis {
  border: none;
  background: transparent;
  cursor: default;
}

.page-size-selector select {
  height: 32px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  border-radius: 8px;
  padding: 0 8px;
  font-size: 12px;
  color: #334155;
  outline: none;
  cursor: pointer;
}
:root[data-theme="dark"] .page-size-selector select {
  background: #0f172a;
  border-color: #334155;
  color: #e2e8f0;
}

/* 视图 2：双栏词典视图 */
.vocab-split-view {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
  align-items: start;
}
@media (max-width: 900px) {
  .vocab-split-view {
    grid-template-columns: 1fr;
  }
}

.split-sidebar {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 12px;
  max-height: calc(100vh - 280px);
  overflow-y: auto;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}
:root[data-theme="dark"] .split-sidebar {
  background: #1e293b;
  border-color: #334155;
}

.sidebar-count-bar {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  padding: 4px 8px 8px;
  border-bottom: 1px solid #f1f5f9;
}
:root[data-theme="dark"] .sidebar-count-bar {
  border-bottom-color: #334155;
}

.split-word-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}

.split-word-item {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
}
.split-word-item:hover {
  background: #f8fafc;
}
:root[data-theme="dark"] .split-word-item:hover {
  background: #334155;
}
.split-word-item.active {
  background: rgba(79, 70, 229, 0.08);
  border-color: #c7d2fe;
}
:root[data-theme="dark"] .split-word-item.active {
  background: rgba(99, 102, 241, 0.18);
  border-color: #4f46e5;
}

.split-item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.split-word-text {
  font-size: 15px;
  color: #0f172a;
}
:root[data-theme="dark"] .split-word-text {
  color: #f8fafc;
}
.split-mastery-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.split-mastery-dot.level-0 { background: #ef4444; }
.split-mastery-dot.level-1 { background: #f59e0b; }
.split-mastery-dot.level-2 { background: #10b981; }

.split-item-def {
  margin: 4px 0 2px;
  font-size: 12px;
  color: #475569;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
:root[data-theme="dark"] .split-item-def {
  color: #94a3b8;
}

.split-item-paper {
  font-size: 11px;
  color: #94a3b8;
}

.split-main-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 28px 32px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.05);
}
:root[data-theme="dark"] .split-main-card {
  background: #1e293b;
  border-color: #334155;
}

.oxford-detail-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
}

.detail-word-group {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;
}

.detail-word-heading {
  margin: 0;
  font-size: 32px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
}
:root[data-theme="dark"] .detail-word-heading {
  color: #f8fafc;
}

.detail-phonetic {
  font-size: 16px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.audio-play-large-btn {
  border: 1px solid #c7d2fe;
  background: #eef2ff;
  color: #4f46e5;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.15s;
}
:root[data-theme="dark"] .audio-play-large-btn {
  background: rgba(99, 102, 241, 0.2);
  border-color: #4f46e5;
  color: #a5b4fc;
}
.audio-play-large-btn:hover {
  background: #4f46e5;
  color: #ffffff;
}

.detail-actions-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.oxford-pill-large {
  background: #4f46e5;
  color: #ffffff;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
}

.mastery-segmented-control {
  display: flex;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 10px;
}
:root[data-theme="dark"] .mastery-segmented-control {
  background: #0f172a;
}
.mastery-segmented-control button {
  border: none;
  background: transparent;
  padding: 5px 12px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  cursor: pointer;
}
.mastery-segmented-control button.active {
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}
:root[data-theme="dark"] .mastery-segmented-control button.active {
  background: #334155;
  color: #f8fafc;
}

.detail-provenance-card {
  background: #f8fafc;
  border-left: 4px solid #4f46e5;
  border-radius: 12px;
  padding: 16px 20px;
  margin-top: 14px;
}
:root[data-theme="dark"] .detail-provenance-card {
  background: #0f172a;
  border-left-color: #6366f1;
}

.provenance-tag-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.provenance-badge {
  background: rgba(79, 70, 229, 0.1);
  color: #4f46e5;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.provenance-title {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.provenance-quote {
  margin: 0 0 8px;
  font-size: 15px;
  color: #1e293b;
  line-height: 1.7;
}
:root[data-theme="dark"] .provenance-quote {
  color: #f1f5f9;
}

.provenance-translation {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.oxford-section-card {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  padding: 20px 24px;
}
:root[data-theme="dark"] .oxford-section-card {
  background: #0f172a;
  border-color: #1e293b;
}

.section-title {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 800;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 8px;
}
:root[data-theme="dark"] .section-title {
  color: #cbd5e1;
}
.section-num {
  color: #4f46e5;
  font-family: monospace;
}

.definition-main-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.pos-badge-lg {
  font-size: 14px;
  font-weight: 800;
  color: #4f46e5;
  font-style: italic;
  padding-top: 2px;
}
.def-chinese {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 4px;
}
:root[data-theme="dark"] .def-chinese {
  color: #f8fafc;
}
.def-english {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
}
:root[data-theme="dark"] .def-english {
  color: #94a3b8;
}

.collocations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 10px;
}
.collocation-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 14px;
}
:root[data-theme="dark"] .collocation-card {
  background: #1e293b;
  border-color: #334155;
}
.col-en {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}
:root[data-theme="dark"] .col-en {
  color: #f8fafc;
}
.col-cn {
  font-size: 12px;
  color: #64748b;
}

.academic-examples-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.example-item-card {
  background: #f8fafc;
  border-radius: 10px;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
}
:root[data-theme="dark"] .example-item-card {
  background: #1e293b;
  border-color: #334155;
}
.eg-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}
.eg-num {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
}
.eg-source-badge {
  font-size: 11px;
  background: #e0e7ff;
  color: #3730a3;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 700;
}
.eg-en {
  margin: 0 0 4px;
  font-size: 14px;
  color: #1e293b;
  line-height: 1.6;
}
:root[data-theme="dark"] .eg-en {
  color: #f1f5f9;
}
.eg-cn {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.etymology-detail-box {
  background: #fffbeb;
  border: 1px solid #fef3c7;
  border-radius: 10px;
  padding: 14px 18px;
}
:root[data-theme="dark"] .etymology-detail-box {
  background: rgba(245, 158, 11, 0.1);
  border-color: rgba(245, 158, 11, 0.2);
}
.etymology-body {
  margin: 0;
  font-size: 14px;
  color: #92400e;
  line-height: 1.7;
}
:root[data-theme="dark"] .etymology-body {
  color: #fcd34d;
}

.synonyms-pill-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.syn-pill {
  background: #f1f5f9;
  color: #334155;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}
:root[data-theme="dark"] .syn-pill {
  background: #1e293b;
  color: #e2e8f0;
}

/* 视图 3：记忆闪卡自测 */
.vocab-flashcard-view {
  max-width: 720px;
  margin: 0 auto;
}

.flashcard-nav-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.card-counter {
  font-size: 14px;
  color: #64748b;
  white-space: nowrap;
}
.card-counter strong {
  color: #0f172a;
  font-size: 18px;
}
:root[data-theme="dark"] .card-counter strong {
  color: #f8fafc;
}

.deck-progress-track {
  flex: 1;
  height: 8px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}
:root[data-theme="dark"] .deck-progress-track {
  background: #334155;
}
.deck-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #4f46e5);
  border-radius: 999px;
  transition: width 0.3s;
}

/* 3D 翻转卡片场景 */
.flashcard-scene {
  perspective: 1200px;
  min-height: 420px;
  cursor: pointer;
  margin-bottom: 24px;
}

.flashcard-cube {
  width: 100%;
  min-height: 420px;
  position: relative;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  transform-style: preserve-3d;
}
.flashcard-cube.is-flipped {
  transform: rotateY(180deg);
}

.flashcard-face {
  position: absolute;
  inset: 0;
  width: 100%;
  min-height: 420px;
  backface-visibility: hidden;
  border-radius: 20px;
  padding: 32px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.flashcard-front {
  background: #ffffff;
  border: 2px solid #e2e8f0;
}
:root[data-theme="dark"] .flashcard-front {
  background: #1e293b;
  border-color: #334155;
}

.flashcard-back {
  background: #ffffff;
  border: 2px solid #6366f1;
  transform: rotateY(180deg);
}
:root[data-theme="dark"] .flashcard-back {
  background: #0f172a;
  border-color: #6366f1;
}

.card-face-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.badge-front {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  background: #f1f5f9;
  padding: 3px 10px;
  border-radius: 6px;
}
:root[data-theme="dark"] .badge-front {
  background: #334155;
  color: #94a3b8;
}
.badge-back {
  font-size: 12px;
  font-weight: 700;
  color: #4f46e5;
  background: #eef2ff;
  padding: 3px 10px;
  border-radius: 6px;
}

.front-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  gap: 16px;
}

.flashcard-word {
  margin: 0;
  font-size: 38px;
  font-weight: 800;
  color: #0f172a;
}
:root[data-theme="dark"] .flashcard-word {
  color: #f8fafc;
}

.front-phonetic-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.flashcard-phonetic {
  font-size: 18px;
  color: #64748b;
  font-family: monospace;
}
.audio-tts-btn-round {
  border: none;
  background: #eef2ff;
  color: #4f46e5;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  cursor: pointer;
}

.front-context-quote {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px 20px;
  margin-top: 10px;
  max-width: 580px;
  border: 1px solid #e2e8f0;
}
:root[data-theme="dark"] .front-context-quote {
  background: #0f172a;
  border-color: #334155;
}
.quote-title {
  margin: 0 0 6px;
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
}
.front-context-quote blockquote {
  margin: 0;
  font-size: 15px;
  color: #334155;
  line-height: 1.6;
}
:root[data-theme="dark"] .front-context-quote blockquote {
  color: #cbd5e1;
}

:deep(.vocab-mask-blank) {
  background: #e0e7ff;
  color: #3730a3;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 800;
}
:root[data-theme="dark"] :deep(.vocab-mask-blank) {
  background: rgba(99, 102, 241, 0.3);
  color: #c7d2fe;
}

.card-face-footer {
  margin-top: auto;
  text-align: center;
}
.flip-prompt-text {
  font-size: 12px;
  color: #94a3b8;
}

.back-scroll-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
}

.back-word-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.back-word-row h2 {
  margin: 0;
  font-size: 26px;
  color: #0f172a;
}
:root[data-theme="dark"] .back-word-row h2 {
  color: #f8fafc;
}
.back-pos {
  font-size: 14px;
  font-weight: 800;
  color: #4f46e5;
  font-style: italic;
}
.back-phonetic {
  font-size: 14px;
  color: #64748b;
  font-family: monospace;
}

.back-meaning-card {
  background: #f8fafc;
  border-radius: 10px;
  padding: 12px 16px;
}
:root[data-theme="dark"] .back-meaning-card {
  background: #1e293b;
}
.back-cn {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 4px;
}
:root[data-theme="dark"] .back-cn {
  color: #f8fafc;
}
.back-en {
  font-size: 13px;
  color: #64748b;
}

.back-original-context {
  background: #f1f5f9;
  border-left: 3px solid #4f46e5;
  border-radius: 8px;
  padding: 10px 14px;
}
:root[data-theme="dark"] .back-original-context {
  background: #1e293b;
}
.orig-lbl {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
}
.orig-en {
  margin: 4px 0 2px;
  font-size: 13px;
  color: #1e293b;
  line-height: 1.5;
}
:root[data-theme="dark"] .orig-en {
  color: #f1f5f9;
}
.orig-cn {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}

.back-collocation-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.col-lbl {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}
.back-cols {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.back-col-tag {
  font-size: 12px;
  background: #f1f5f9;
  padding: 3px 8px;
  border-radius: 6px;
  color: #334155;
}
:root[data-theme="dark"] .back-col-tag {
  background: #1e293b;
  color: #e2e8f0;
}

.back-etymology-box {
  background: #fffbeb;
  border: 1px solid #fef3c7;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 12px;
  color: #92400e;
}
:root[data-theme="dark"] .back-etymology-box {
  background: rgba(245, 158, 11, 0.12);
  border-color: rgba(245, 158, 11, 0.2);
  color: #fcd34d;
}

/* 闪卡底部打分条 */
.flashcard-control-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.flashcard-arrow-btn {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  padding: 10px 18px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}
:root[data-theme="dark"] .flashcard-arrow-btn {
  background: #1e293b;
  border-color: #334155;
  color: #e2e8f0;
}
.flashcard-arrow-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.grade-buttons-group {
  display: flex;
  gap: 10px;
}

.grade-btn {
  border: none;
  padding: 10px 18px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}
.grade-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.grade-forgot {
  background: #fee2e2;
  color: #991b1b;
}
.grade-vague {
  background: #fef3c7;
  color: #92400e;
}
.grade-mastered {
  background: #d1fae5;
  color: #065f46;
}

/* 手动添加生词弹窗 */
.vocab-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  display: grid;
  place-items: center;
  z-index: 1000;
  padding: 20px;
}

.vocab-modal-card {
  background: #ffffff;
  border-radius: 18px;
  width: min(520px, 100%);
  padding: 24px;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
}
:root[data-theme="dark"] .vocab-modal-card {
  background: #1e293b;
}

.modal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.modal-head h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}
:root[data-theme="dark"] .modal-head h3 {
  color: #f8fafc;
}
.close-modal-btn {
  border: none;
  background: none;
  font-size: 22px;
  color: #94a3b8;
  cursor: pointer;
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-lbl {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}
:root[data-theme="dark"] .form-lbl {
  color: #cbd5e1;
}
.form-lbl .req {
  color: #ef4444;
}

.form-item input,
.form-item textarea {
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  color: #0f172a;
  outline: none;
}
:root[data-theme="dark"] .form-item input,
:root[data-theme="dark"] .form-item textarea {
  background: #0f172a;
  border-color: #334155;
  color: #f8fafc;
}
.form-item input:focus,
.form-item textarea:focus {
  border-color: #4f46e5;
}

.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* ── 单词自主拼写练习 (Spelling Practice) ── */
.action-icon-btn.spelling-btn {
  color: #4f46e5;
  background: rgba(99, 102, 241, 0.08);
  border-color: rgba(99, 102, 241, 0.25);
  font-size: 11.5px;
  font-weight: 750;
  width: auto;
  padding: 0 8px;
  gap: 4px;
}
.action-icon-btn.spelling-btn:hover,
.action-icon-btn.spelling-btn.active {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.3);
}
:root[data-theme="dark"] .action-icon-btn.spelling-btn {
  color: #a5b4fc;
  background: rgba(99, 102, 241, 0.18);
  border-color: rgba(99, 102, 241, 0.35);
}
:root[data-theme="dark"] .action-icon-btn.spelling-btn.active {
  background: #6366f1;
  color: #ffffff;
}

/* 列表卡片内嵌拼写抽屉 */
.card-spelling-drawer {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(238, 242, 255, 0.95), rgba(245, 243, 255, 0.9));
  border: 1.5px solid rgba(99, 102, 241, 0.28);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.08);
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
:root[data-theme="dark"] .card-spelling-drawer {
  background: linear-gradient(135deg, rgba(30, 27, 75, 0.7), rgba(24, 24, 47, 0.8));
  border-color: rgba(99, 102, 241, 0.4);
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.35);
}

.card-spelling-drawer.is-correct {
  border-color: #10b981;
  background: linear-gradient(135deg, rgba(236, 253, 245, 0.95), rgba(240, 253, 250, 0.9));
  box-shadow: 0 0 0 1px rgba(16, 185, 129, 0.4), 0 4px 16px rgba(16, 185, 129, 0.12);
}
:root[data-theme="dark"] .card-spelling-drawer.is-correct {
  background: linear-gradient(135deg, rgba(6, 78, 59, 0.6), rgba(15, 23, 42, 0.8));
  border-color: #10b981;
}

.card-spelling-drawer.is-shake {
  animation: spelling-shake 0.45s ease-in-out;
}

.spelling-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.spelling-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spelling-badge {
  font-size: 11.5px;
  font-weight: 850;
  color: #4f46e5;
  background: rgba(99, 102, 241, 0.12);
  padding: 2px 8px;
  border-radius: 6px;
}
:root[data-theme="dark"] .spelling-badge {
  color: #a5b4fc;
  background: rgba(99, 102, 241, 0.25);
}

.letter-count-hint {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
}

.spelling-close-btn {
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  padding: 0 4px;
}
.spelling-close-btn:hover {
  color: #1e293b;
}

.spelling-hints-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 6px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(226, 232, 240, 0.8);
}
:root[data-theme="dark"] .spelling-hints-row {
  background: rgba(15, 23, 42, 0.6);
  border-color: rgba(71, 85, 105, 0.4);
}

.spelling-meaning-cn {
  font-size: 12.5px;
  font-weight: 750;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
:root[data-theme="dark"] .spelling-meaning-cn {
  color: #f1f5f9;
}

.spelling-audio-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid rgba(99, 102, 241, 0.2);
  background: #ffffff;
  color: #4f46e5;
  font-size: 11px;
  font-weight: 750;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s ease;
}
.spelling-audio-btn:hover {
  background: #eef2ff;
  border-color: #4f46e5;
}
:root[data-theme="dark"] .spelling-audio-btn {
  background: rgba(30, 41, 59, 0.8);
  border-color: rgba(99, 102, 241, 0.35);
  color: #a5b4fc;
}

.spelling-input-group {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.spelling-text-input {
  width: 100%;
  height: 38px;
  border-radius: 10px;
  border: 1.5px solid rgba(148, 163, 184, 0.4);
  background: #ffffff;
  color: #0f172a;
  padding: 0 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.5px;
  outline: none;
  box-sizing: border-box;
  transition: all 0.2s ease;
}
.spelling-text-input:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.18);
}
:root[data-theme="dark"] .spelling-text-input {
  background: rgba(15, 23, 42, 0.85);
  border-color: rgba(71, 85, 105, 0.6);
  color: #f8fafc;
}
:root[data-theme="dark"] .spelling-text-input:focus {
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.3);
}

.spelling-text-input.input-correct {
  border-color: #10b981 !important;
  color: #047857 !important;
  background: #ecfdf5 !important;
}
:root[data-theme="dark"] .spelling-text-input.input-correct {
  color: #6ee7b7 !important;
  background: rgba(6, 78, 59, 0.5) !important;
}

.spelling-text-input.input-wrong {
  border-color: #ef4444 !important;
  color: #b91c1c !important;
  background: #fef2f2 !important;
}
:root[data-theme="dark"] .spelling-text-input.input-wrong {
  color: #fca5a5 !important;
  background: rgba(127, 29, 29, 0.5) !important;
}

.spelling-submit-btn {
  height: 38px;
  padding: 0 14px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: #ffffff;
  font-size: 12.5px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}
.spelling-submit-btn:hover:not(:disabled) {
  filter: brightness(1.08);
  transform: translateY(-1px);
}
.spelling-submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spelling-retry-btn {
  height: 38px;
  padding: 0 14px;
  border: 1.5px solid #10b981;
  border-radius: 10px;
  background: #ffffff;
  color: #047857;
  font-size: 12.5px;
  font-weight: 800;
  cursor: pointer;
}
:root[data-theme="dark"] .spelling-retry-btn {
  background: rgba(6, 78, 59, 0.8);
  color: #6ee7b7;
}

.spelling-feedback {
  font-size: 12px;
  font-weight: 750;
  padding: 4px 2px;
}

.feedback-success {
  color: #047857;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.feedback-success small {
  color: #10b981;
  font-weight: 800;
}
:root[data-theme="dark"] .feedback-success {
  color: #6ee7b7;
}

.feedback-error {
  color: #b91c1c;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 6px;
}
:root[data-theme="dark"] .feedback-error {
  color: #fca5a5;
}

.peek-answer-btn {
  border: none;
  background: transparent;
  color: #4f46e5;
  font-size: 11.5px;
  font-weight: 800;
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
}
:root[data-theme="dark"] .peek-answer-btn {
  color: #a5b4fc;
}

.revealed-answer {
  color: #334155;
  font-size: 12px;
}
:root[data-theme="dark"] .revealed-answer {
  color: #cbd5e1;
}

.answer-highlight {
  color: #4f46e5;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  letter-spacing: 0.5px;
}
:root[data-theme="dark"] .answer-highlight {
  color: #818cf8;
}

/* 词典详情页拼写强化模块 */
.oxford-spelling-section {
  border: 1.5px solid rgba(99, 102, 241, 0.24) !important;
  background: linear-gradient(135deg, rgba(248, 250, 255, 0.95), rgba(243, 244, 255, 0.9)) !important;
}
:root[data-theme="dark"] .oxford-spelling-section {
  background: linear-gradient(135deg, rgba(30, 27, 75, 0.5), rgba(15, 23, 42, 0.8)) !important;
  border-color: rgba(99, 102, 241, 0.35) !important;
}

.detail-spelling-box {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.06);
}
:root[data-theme="dark"] .detail-spelling-box {
  background: rgba(15, 23, 42, 0.9);
  border-color: rgba(71, 85, 105, 0.5);
}

.detail-spelling-prompt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.spelling-clue {
  display: flex;
  align-items: center;
  gap: 8px;
}

.clue-tag {
  font-size: 11.5px;
  font-weight: 850;
  color: #4f46e5;
  background: rgba(99, 102, 241, 0.12);
  padding: 3px 8px;
  border-radius: 6px;
}
:root[data-theme="dark"] .clue-tag {
  color: #a5b4fc;
  background: rgba(99, 102, 241, 0.25);
}

.clue-text {
  font-size: 14px;
  font-weight: 800;
  color: #0f172a;
}
:root[data-theme="dark"] .clue-text {
  color: #f8fafc;
}

.spelling-audio-btn-lg {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 9px;
  border: 1.5px solid rgba(99, 102, 241, 0.24);
  background: #ffffff;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
}
.spelling-audio-btn-lg:hover {
  background: #eff2fe;
  border-color: #4f46e5;
  transform: translateY(-1px);
}
:root[data-theme="dark"] .spelling-audio-btn-lg {
  background: rgba(30, 41, 59, 0.85);
  border-color: rgba(99, 102, 241, 0.35);
  color: #a5b4fc;
}

.detail-spelling-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.detail-spelling-input {
  height: 44px;
  border-radius: 12px;
  border: 1.5px solid rgba(148, 163, 184, 0.4);
  background: #f8fafc;
  color: #0f172a;
  padding: 0 16px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 15px;
  font-weight: 750;
  letter-spacing: 0.6px;
  outline: none;
  box-sizing: border-box;
  transition: all 0.2s ease;
}
.detail-spelling-input:focus {
  border-color: #4f46e5;
  background: #ffffff;
  box-shadow: 0 0 0 3.5px rgba(99, 102, 241, 0.18);
}
:root[data-theme="dark"] .detail-spelling-input {
  background: rgba(15, 23, 42, 0.85);
  border-color: rgba(71, 85, 105, 0.6);
  color: #f8fafc;
}
:root[data-theme="dark"] .detail-spelling-input:focus {
  border-color: #818cf8;
  background: rgba(15, 23, 42, 0.95);
  box-shadow: 0 0 0 3.5px rgba(99, 102, 241, 0.3);
}

.detail-spelling-submit {
  height: 44px;
  padding: 0 22px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: #ffffff;
  font-size: 13.5px;
  font-weight: 850;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(79, 70, 229, 0.35);
  transition: all 0.2s ease;
}
.detail-spelling-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.08);
}

.detail-spelling-retry {
  height: 44px;
  padding: 0 22px;
  border: 1.5px solid #10b981;
  border-radius: 12px;
  background: #ffffff;
  color: #047857;
  font-size: 13.5px;
  font-weight: 850;
  cursor: pointer;
}
:root[data-theme="dark"] .detail-spelling-retry {
  background: rgba(6, 78, 59, 0.8);
  color: #6ee7b7;
}

.detail-spelling-feedback {
  font-size: 13px;
  font-weight: 750;
  padding: 4px 2px;
}

.feedback-success-lg {
  color: #047857;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.feedback-success-lg small {
  color: #10b981;
  font-weight: 800;
}
:root[data-theme="dark"] .feedback-success-lg {
  color: #6ee7b7;
}

.feedback-error-lg {
  color: #b91c1c;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}
:root[data-theme="dark"] .feedback-error-lg {
  color: #fca5a5;
}

.peek-answer-btn-lg {
  border: none;
  background: transparent;
  color: #4f46e5;
  font-size: 12.5px;
  font-weight: 850;
  cursor: pointer;
  text-decoration: underline;
}
:root[data-theme="dark"] .peek-answer-btn-lg {
  color: #a5b4fc;
}

.revealed-answer-lg {
  color: #334155;
  font-size: 13.5px;
}
:root[data-theme="dark"] .revealed-answer-lg {
  color: #cbd5e1;
}

@keyframes spelling-shake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-6px); }
  40%, 80% { transform: translateX(6px); }
}

.spelling-expand-enter-active,
.spelling-expand-leave-active {
  transition: all 0.25s ease-out;
}
.spelling-expand-enter-from,
.spelling-expand-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
