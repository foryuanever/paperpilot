<template>
  <div class="home-root" @mousemove="handleMouseMove" :style="{ '--mouse-x': mouseX + 'px', '--mouse-y': mouseY + 'px' }">

    <!-- ═══ NAV ═══ -->
    <header class="home-nav" :class="{ scrolled: navScrolled }">
      <div class="nav-inner">
        <router-link class="brand" to="/">
          <div class="brand-mark" style="display: flex; align-items: center;">
            <img src="/brand/papersolver-mark-v2.png" alt="PaperSolver" style="width: 36px; height: 36px; object-fit: contain;" />
          </div>
          <span class="brand-name">PaperSolver</span>
          <span class="brand-tag">beta</span>
        </router-link>
        <nav v-if="!isDesktopApp" class="nav-links">
          <a href="#features">功能</a>
          <a href="#workflow">使用流程</a>
          <a href="#why">为什么选我们</a>
          <a href="javascript:;" @click="guideModalOpen = true">安装教程</a>
        </nav>

      </div>
    </header>

    <!-- ═══ HERO — Long, Softly-lit Headline with Unequal Line Lengths ═══ -->
    <section class="hero">
      <div class="hero-bg-grid"></div>
      <div class="hero-glow glow-1"></div>
      <div class="hero-glow glow-2"></div>
      <div class="hero-glow glow-3"></div>

      <div class="hero-center" :class="{ in: heroIn }">

        <!-- Pill -->
        <div class="hero-pill">
          <span class="pill-dot"></span>
          AI 学术科研工作台 · 读写研一体化
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
        </div>

        <!-- Longer, Softly Glowing Headline with Unequal Lengths -->
        <h1 class="hero-h1">
          <div class="h1-line-top">从海量 PDF 文献中</div>
          <div class="h1-line-bot">一站式完成精读、综述与组会汇报</div>
        </h1>

        <!-- Sub caption -->
        <p class="hero-sub">
          零散文献一键规整，前沿算法精细解析，智能模型综述提炼，成果直出组会幻灯片——研读全生命周期无缝流转，让思考更纯粹。
        </p>

        <!-- CTAs -->
        <div v-if="!isDesktopApp" class="hero-actions download-actions" style="display: flex; gap: 16px; flex-wrap: wrap; justify-content: center; margin-top: 24px; margin-bottom: 24px;">
          <a href="/downloads/PaperSolver.dmg" class="cta-primary download-btn mac-btn" style="display: flex; align-items: center; justify-content: center; gap: 8px; text-decoration: none; padding: 14px 28px; font-weight: 600; background: linear-gradient(135deg, #2563eb, #7c3aed); border-radius: 8px; color: #fff; box-shadow: var(--sh-md);">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20" style="display: block; flex-shrink: 0;">
              <path d="M12.03 5.41c.83-1.05 1.4-2.52 1.25-3.97-1.22.05-2.73.83-3.6 1.88-.77.9-1.44 2.41-1.25 3.82 1.36.1 2.76-.68 3.6-1.73zm4.5 5.86c-.05-2.24 1.79-3.32 1.87-3.37-1.03-1.54-2.63-1.75-3.2-1.81-1.37-.15-2.69.83-3.39.83-.71 0-1.79-.81-2.94-.79-1.51.02-2.92.89-3.69 2.27-1.57 2.78-.4 6.89 1.13 9.15.75 1.1 1.63 2.32 2.78 2.28 1.11-.05 1.54-.73 2.87-.73 1.33 0 1.73.73 2.89.7 1.2-.02 1.98-1.1 2.72-2.2 1.01-1.48 1.42-2.91 1.44-2.98-.03-.02-2.45-.96-2.48-3.76z"/>
            </svg>
            下载 macOS 客户端
          </a>
          <a href="/downloads/PaperSolver.exe" class="cta-primary download-btn win-btn" style="display: flex; align-items: center; justify-content: center; gap: 8px; text-decoration: none; padding: 14px 28px; font-weight: 600; background: linear-gradient(135deg, #10b981, #059669); border-radius: 8px; color: #fff; box-shadow: var(--sh-md);">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
              <path d="M0 3.449L9.75 2.1v9.45H0V3.449zM0 12.45h9.75v9.45L0 20.551v-8.1zM10.8 1.95L24 0v11.55H10.8V1.95zM10.8 12.45H24v11.55l-13.2-1.95v-9.6z"/>
            </svg>
            下载 Windows 客户端
          </a>
        </div>

        <!-- Desktop app: login/register entry -->
        <div v-else class="hero-actions">
          <button class="cta-primary" @click="openModal('login')">
            开始体验
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
          </button>
          <button class="cta-outline" @click="showContactModal = true">
            联系我们
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
          </button>
        </div>

        <!-- Social proof -->
        <div class="hero-proof">
          <svg class="proof-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" style="color: #10b981; flex-shrink: 0;"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
          <span><strong>本地优先架构</strong> · 文献 PDF 绝不上传服务器，仅同步轻量笔记与对话记录，保障数据隐私</span>
        </div>

        <!-- Stats bar -->
        <div class="hero-stats">
          <div class="stat"><strong>本地私密库</strong><span>文献原档驻留本地，从源头保障知识产权与机密安全</span></div>
          <div class="stat-div"></div>
          <div class="stat"><strong>多端云同步</strong><span>仅同步结构化笔记与对话脑图，轻量化架构跨端流转</span></div>
          <div class="stat-div"></div>
          <div class="stat"><strong>专家级精读</strong><span>深度解构公式、图表与实验方法，秒懂同行评议局限性</span></div>
          <div class="stat-div"></div>
          <div class="stat"><strong>一键生成汇报</strong><span>秒级提炼核心观点至学术幻灯片，轻松驾驭组会答辩</span></div>
        </div>

      </div>
    </section>

    <div v-if="!isDesktopApp">
    <!-- ═══ FEATURES ═══ -->
    <section id="features" class="section" ref="featRef">
      <div class="section-wrap">
        <div class="s-head" :class="{ in: featIn }">
          <span class="eyebrow"><i class="ew-dot"></i>核心模块</span>
          <h2>六大核心模块，覆盖科研工作流全周期</h2>
          <p>从文献获取、智能阅读到组会 PPT 汇报，每一步都为您准备了破局的专业工具</p>
        </div>
        <div class="feat-grid" :class="{ in: featIn }">
          <div v-for="(f, i) in features" :key="f.title" class="feat-card" :style="{ '--c': f.color, '--delay': i * 0.06 + 's' }">
            <div class="fc-icon">
              <svg :viewBox="f.vb" fill="none" stroke="currentColor" stroke-width="1.7" v-html="f.path"></svg>
            </div>
            <div class="fc-body">
              <h3>{{ f.title }}</h3>
              <p>{{ f.desc }}</p>
              <div class="fc-chips">
                <span v-for="t in f.chips" :key="t">{{ t }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ WORKFLOW — Full-Page Unbounded Slide Carousel ═══ -->
    <section id="workflow" class="workflow-section-full">
      <div class="section-wrap">
        <div class="s-head" ref="wfHeadRef" :class="{ in: wfHeadIn }">
          <span class="eyebrow"><i class="ew-dot"></i>使用流程</span>
          <h2>五步，从一篇 PDF 到一场汇报</h2>
          <p>每一步都可以单独使用，也可以顺着走完整条研读链条</p>
        </div>
      </div>

      <!-- FULL-PAGE UNCONTAINED CAROUSEL (No borders, 100vw unbounded slides) -->
      <div class="full-carousel-viewport">
        <div class="full-carousel-track" :style="{ transform: `translateX(-${wfActive * 100}vw)` }">
          <div
            v-for="(step, i) in workflowSteps"
            :key="step.id"
            class="full-slide-item"
            :class="{ active: wfActive === i }"
          >
            <!-- Left Side: Large Viewport Screenshot -->
            <div class="full-slide-media">
              <div class="media-container" @click="openImagePreview(step.img)" title="点击查看高清大图">
                <img :src="step.img" :alt="step.title" loading="lazy" />
                <div class="media-fade-overlay"></div>
                <div class="media-step-badge">STEP 0{{ i + 1 }}</div>
              </div>
            </div>

            <!-- Right Side: Detailed Copy with Staggered Entrance Animations -->
            <div class="full-slide-info">
              <div class="info-content-box">
                <div class="info-meta">
                  <span class="info-counter">0{{ i + 1 }} / 0{{ workflowSteps.length }}</span>
                  <span class="info-label">核心环节</span>
                </div>

                <h3 class="info-title">{{ step.title }}</h3>
                <p class="info-desc">{{ step.desc }}</p>

                <!-- Detailed Highlighting Bullet Points -->
                <div class="info-highlights-list">
                  <div v-for="(hl, idx) in step.highlights" :key="idx" class="info-hl-row">
                    <span class="info-hl-icon">✦</span>
                    <span class="info-hl-text" v-html="hl"></span>
                  </div>
                </div>

                <!-- Tags -->
                <div class="info-tags">
                  <span v-for="t in step.tags" :key="t" class="info-tag-item">{{ t }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Floating Controls Bar -->
        <div class="full-carousel-controls">
          <button class="fc-arrow-btn prev" @click="wfGo(wfActive - 1)" aria-label="上一页">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5"/><path d="m12 5-7 7 7 7"/></svg>
          </button>

          <div class="fc-dots-wrap">
            <span
              v-for="(_, i) in workflowSteps"
              :key="i"
              class="fc-dot-item"
              :class="{ active: wfActive === i }"
              @click="wfGo(i)"
            ></span>
          </div>

          <div class="fc-progress-indicator" title="自动播放中">
            <svg viewBox="0 0 36 36">
              <circle cx="18" cy="18" r="14" fill="none" stroke="rgba(255,255,255,.1)" stroke-width="2.5"/>
              <circle cx="18" cy="18" r="14" fill="none" stroke="#3b82f6" stroke-width="2.5"
                stroke-dasharray="88" :stroke-dashoffset="88 - 88 * wfProgress / 100"
                stroke-linecap="round" transform="rotate(-90 18 18)"/>
            </svg>
          </div>

          <button class="fc-arrow-btn next" @click="wfGo(wfActive + 1)" aria-label="下一页">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
          </button>
        </div>
      </div>
    </section>

    <!-- ═══ WHY CHOOSE US ═══ -->
    <section id="why" class="section" ref="whyRef">
      <div class="section-wrap">
        <div class="s-head" :class="{ in: whyIn }">
          <span class="eyebrow"><i class="ew-dot"></i>为什么选择我们</span>
          <h2>科研路上，你值得更好的工具</h2>
          <p>不只是一个 PDF 阅读器，而是一套从阅读到汇报的完整工作台</p>
        </div>

        <div class="why-danmaku-container" :class="{ in: whyIn }">
          <!-- Track 1: Left Scroll -->
          <div class="danmaku-track track-left">
            <div class="danmaku-content">
              <div v-for="(c, idx) in track1Items" :key="'t1-' + idx" class="danmaku-pill" :style="{ '--c': c.color }">
                <span class="dm-icon">
                  <svg :viewBox="c.vb" fill="none" stroke="currentColor" stroke-width="1.8" v-html="c.path"></svg>
                </span>
                <span class="dm-pain">{{ c.pain }}</span>
                <span class="dm-arrow">➜</span>
                <span class="dm-solution">{{ c.solution }}</span>
              </div>
            </div>
            <div class="danmaku-content" aria-hidden="true">
              <div v-for="(c, idx) in track1Items" :key="'t1-dup-' + idx" class="danmaku-pill" :style="{ '--c': c.color }">
                <span class="dm-icon">
                  <svg :viewBox="c.vb" fill="none" stroke="currentColor" stroke-width="1.8" v-html="c.path"></svg>
                </span>
                <span class="dm-pain">{{ c.pain }}</span>
                <span class="dm-arrow">➜</span>
                <span class="dm-solution">{{ c.solution }}</span>
              </div>
            </div>
          </div>

          <!-- Track 2: Right Scroll -->
          <div class="danmaku-track track-right">
            <div class="danmaku-content">
              <div v-for="(c, idx) in track2Items" :key="'t2-' + idx" class="danmaku-pill" :style="{ '--c': c.color }">
                <span class="dm-icon">
                  <svg :viewBox="c.vb" fill="none" stroke="currentColor" stroke-width="1.8" v-html="c.path"></svg>
                </span>
                <span class="dm-pain">{{ c.pain }}</span>
                <span class="dm-arrow">➜</span>
                <span class="dm-solution">{{ c.solution }}</span>
              </div>
            </div>
            <div class="danmaku-content" aria-hidden="true">
              <div v-for="(c, idx) in track2Items" :key="'t2-dup-' + idx" class="danmaku-pill" :style="{ '--c': c.color }">
                <span class="dm-icon">
                  <svg :viewBox="c.vb" fill="none" stroke="currentColor" stroke-width="1.8" v-html="c.path"></svg>
                </span>
                <span class="dm-pain">{{ c.pain }}</span>
                <span class="dm-arrow">➜</span>
                <span class="dm-solution">{{ c.solution }}</span>
              </div>
            </div>
          </div>

          <!-- Track 3: Left Scroll Slow -->
          <div class="danmaku-track track-left-slow">
            <div class="danmaku-content">
              <div v-for="(c, idx) in track3Items" :key="'t3-' + idx" class="danmaku-pill" :style="{ '--c': c.color }">
                <span class="dm-icon">
                  <svg :viewBox="c.vb" fill="none" stroke="currentColor" stroke-width="1.8" v-html="c.path"></svg>
                </span>
                <span class="dm-pain">{{ c.pain }}</span>
                <span class="dm-arrow">➜</span>
                <span class="dm-solution">{{ c.solution }}</span>
              </div>
            </div>
            <div class="danmaku-content" aria-hidden="true">
              <div v-for="(c, idx) in track3Items" :key="'t3-dup-' + idx" class="danmaku-pill" :style="{ '--c': c.color }">
                <span class="dm-icon">
                  <svg :viewBox="c.vb" fill="none" stroke="currentColor" stroke-width="1.8" v-html="c.path"></svg>
                </span>
                <span class="dm-pain">{{ c.pain }}</span>
                <span class="dm-arrow">➜</span>
                <span class="dm-solution">{{ c.solution }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Quote banner -->
        <div class="why-banner-premium" :class="{ in: whyIn }">
          <div class="wbp-quote-mark-start">“</div>
          <div class="wbp-content">
            <p class="wbp-text">做学术本来就很难了，工具不应该再给你添堵。</p>
            <p class="wbp-by">—— PaperSolver 产品理念</p>
          </div>
          <div class="wbp-quote-mark-end">”</div>
        </div>
      </div>
    </section>

    <!-- ═══ COMPARISON ═══ -->
    <section class="section section-alt" id="comparison">
      <div class="section-wrap">
        <div class="s-head" :class="{ in: cmpIn }">
          <span class="eyebrow"><i class="ew-dot"></i>横向对比</span>
          <h2>全方位对比，看清真正的技术底色</h2>
          <p>对比行业常见竞品，我们坚持在数据隐私安全、核心文献精读与高质量成果产出上做深做透</p>
        </div>
        <div class="cmp-table" :class="{ in: cmpIn }" ref="cmpRef" style="overflow-x: auto;">
          <div class="cmp-header">
            <span>对比维度</span>
            <span>云端翻译/阅读器 (A 类产品)</span>
            <span>重度云端 AI 助手 (B 类产品)</span>
            <span class="cmp-good-h">
              我们的产品 (PaperSolver)
            </span>
          </div>
          <div v-for="row in comparisons" :key="row.feature" class="cmp-row">
            <b>{{ row.feature }}</b>
            <span class="cmp-bad">
              <span class="ci bad"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M18 6 6 18M6 6l12 12"/></svg></span>
              {{ row.a }}
            </span>
            <span class="cmp-bad">
              <span class="ci bad"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M18 6 6 18M6 6l12 12"/></svg></span>
              {{ row.b }}
            </span>
            <span class="cmp-good">
              <span class="ci good"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M20 6 9 17l-5-5"/></svg></span>
              {{ row.our }}
            </span>
          </div>
        </div>
      </div>
    </section>



    <!-- ═══ FOOTER ═══ -->
    <footer class="home-footer">
      <div class="footer-inner">
        <span class="footer-logo">PaperSolver</span>
        <div class="footer-links">
          <a href="#features">功能</a>
          <a href="#workflow">流程</a>
          <a href="#why">为什么选我们</a>
          <a href="javascript:;" @click="openLegalModal('terms')">用户协议</a>
          <a href="javascript:;" @click="openLegalModal('privacy')">隐私政策</a>
          <a href="javascript:;" @click="openLegalModal('disclaimer')">免责声明</a>
        </div>
        <p class="footer-copy">© 2026 PaperSolver · 为每一位科研人设计</p>
      </div>
    </footer>
    </div>

    <!-- ═══ AUTH MODAL ═══ -->
    <!-- ═══ CONTACT US MODAL ═══ -->
    <Transition name="mfade">
      <div v-if="showContactModal" class="modal-mask" @click="showContactModal = false">
        <div class="modal-box contact-modal-box" @click.stop style="max-width: 620px; width: 95%; padding: 32px; border-radius: 20px; background: rgba(20, 20, 30, 0.95); border: 1px solid rgba(165, 180, 252, 0.2); backdrop-filter: blur(25px); color: #fff; text-align: center; box-shadow: 0 25px 60px rgba(0, 0, 0, 0.6); position: relative;">
          <button class="modal-x" @click="showContactModal = false" style="background: transparent; border: none; color: #a1a1aa; cursor: pointer; position: absolute; right: 20px; top: 20px; transition: color 0.2s;" onmouseover="this.style.color='#fff'" onmouseout="this.style.color='#a1a1aa'">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
          </button>
          <h3 style="font-size: 1.5rem; font-weight: 800; background: linear-gradient(135deg, #ffffff 40%, #c7d2fe 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 8px;">联系我们</h3>
          <p style="color: #94a3b8; font-size: 0.88rem; margin-bottom: 28px;">关注官方账号，获取最新动态与专属学术支持</p>
          
          <div style="display: flex; gap: 24px; justify-content: center; flex-wrap: wrap;">
            <!-- QQ QR Card -->
            <div style="flex: 1; min-width: 240px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.05); padding: 20px; border-radius: 16px; display: flex; flex-direction: column; align-items: center; gap: 12px;">
              <img src="/contact/qq-contact.jpg" alt="QQ" @click="openImagePreview('/contact/qq-contact.jpg')" style="width: 160px; height: 260px; object-fit: contain; border-radius: 10px; border: 1px solid rgba(255,255,255,0.1); cursor: zoom-in;" />
              <div style="font-weight: 700; color: #f0f0f4; font-size: 0.95rem; margin-top: 4px;">QQ 官方交流群</div>
              <div style="color: #64748b; font-size: 0.8rem;">群号: 1095440623</div>
            </div>
            
            <!-- Douyin QR Card -->
            <div style="flex: 1; min-width: 240px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.05); padding: 20px; border-radius: 16px; display: flex; flex-direction: column; align-items: center; gap: 12px;">
              <img src="/contact/douyin-contact.jpg" alt="Douyin" @click="openImagePreview('/contact/douyin-contact.jpg')" style="width: 160px; height: 260px; object-fit: contain; border-radius: 10px; border: 1px solid rgba(255,255,255,0.1); cursor: zoom-in;" />
              <div style="font-weight: 700; color: #f0f0f4; font-size: 0.95rem; margin-top: 4px;">抖音官方账号</div>
              <div style="color: #64748b; font-size: 0.8rem;">抖音号: 76517306980</div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ═══ LEGAL DOCUMENTS MODAL ═══ -->
    <Transition name="mfade">
      <div v-if="showLegalModal" class="modal-mask" @click="showLegalModal = false">
        <div class="modal-box legal-modal-box" @click.stop style="max-width: 680px; width: 90%; max-height: 80vh; display: flex; flex-direction: column; padding: 0; border-radius: 20px; background: rgba(20, 20, 30, 0.95); border: 1px solid rgba(165, 180, 252, 0.2); backdrop-filter: blur(25px); color: #fff; box-shadow: 0 25px 60px rgba(0, 0, 0, 0.6); overflow: hidden; position: relative;">
          <button class="modal-x" @click="showLegalModal = false" style="background: transparent; border: none; color: #a1a1aa; cursor: pointer; position: absolute; right: 20px; top: 20px; z-index: 10; transition: color 0.2s;" onmouseover="this.style.color='#fff'" onmouseout="this.style.color='#a1a1aa'">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
          </button>
          
          <!-- Tabs header -->
          <div style="display: flex; gap: 4px; padding: 24px 24px 0; border-bottom: 1px solid rgba(255,255,255,0.06); background: rgba(0,0,0,0.15);">
            <button v-for="tab in ['terms', 'privacy', 'disclaimer']" :key="tab" @click="legalModalTab = tab" :style="{
              background: 'transparent',
              border: 'none',
              borderBottom: legalModalTab === tab ? '2px solid #6366f1' : '2px solid transparent',
              color: legalModalTab === tab ? '#ffffff' : '#94a3b8',
              padding: '12px 16px',
              fontSize: '0.95rem',
              fontWeight: legalModalTab === tab ? '700' : '500',
              cursor: 'pointer',
              transition: 'all 0.2s'
            }">
              {{ legalContent[tab].title }}
            </button>
          </div>
          
          <!-- Content area -->
          <div style="flex: 1; overflow-y: auto; padding: 28px 24px; text-align: left; line-height: 1.7;">
            <div v-for="block in legalContent[legalModalTab].blocks" :key="block.title" style="margin-bottom: 24px;">
              <h4 style="font-size: 1.15rem; font-weight: 700; color: #f0f0f4; margin-bottom: 10px; display: flex; align-items: center; gap: 8px;">
                <span style="width: 4px; height: 16px; background: #6366f1; border-radius: 2px;"></span>
                {{ block.title }}
              </h4>
              <p v-for="line in block.lines" :key="line" style="color: #cbd5e1; font-size: 0.92rem; margin-bottom: 8px; text-align: justify;">{{ line }}</p>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="mfade">
      <div v-if="showDownloadPromptModal" class="modal-mask" @click="showDownloadPromptModal = false">
        <div class="modal-box download-prompt-box" @click.stop style="max-width: 480px; padding: 32px; border-radius: 16px; background: #18181b; border: 1px solid #27272a; text-align: center; color: #fff;">
          <button class="modal-x" @click="showDownloadPromptModal = false" style="background: transparent; border: none; color: #a1a1aa; cursor: pointer; position: absolute; right: 16px; top: 16px;">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
          </button>
          <div class="download-prompt-content" style="display: flex; flex-direction: column; align-items: center; gap: 20px;">
            <div style="background: rgba(37, 99, 235, 0.1); padding: 16px; border-radius: 50%; color: #2563eb; display: inline-flex;">
              <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3"/>
              </svg>
            </div>
            <h3 style="font-size: 1.3rem; margin: 0; font-weight: bold; background: linear-gradient(135deg, #fff, #a1a1aa); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">请下载使用 PaperSolver 桌面端</h3>
            <p style="font-size: 0.95rem; color: #a1a1aa; line-height: 1.6; margin: 0; text-align: left;">
              为保障学术交流与数据安全，PaperSolver 采用了本地优先架构，完整的 **PDF 沉浸式翻译、Zotero 本地附件同步、文献解析** 等核心功能均运行于客户端。网页端仅用作产品介绍与客户端分发。
            </p>
            <div style="display: flex; flex-direction: column; gap: 10px; width: 100%; margin-top: 10px;">
              <a href="/downloads/PaperSolver.dmg" class="cta-primary" style="display: flex; align-items: center; justify-content: center; gap: 8px; text-decoration: none; padding: 12px; border-radius: 8px; font-weight: bold; color: #fff; background: linear-gradient(135deg, #2563eb, #7c3aed);">
                <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                  <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-.1 3.81 1.5.65.27 2.48 1.05 3.32 2.29a7.35 7.35 0 0 0-3.66 6.44c.03 3.83 3.32 5.09 3.36 5.12-.03.09-.53 1.8-1.73 3.4M15.97 4.17c.66-.81 1.11-1.93.99-3.06-1 .04-2.2.67-2.92 1.5-.63.73-1.18 1.87-1.03 2.99 1.11.09 2.24-.59 2.96-1.43"/>
                </svg>
                下载 macOS 客户端
              </a>
              <a href="/downloads/PaperSolver.exe" class="cta-primary" style="display: flex; align-items: center; justify-content: center; gap: 8px; text-decoration: none; padding: 12px; border-radius: 8px; font-weight: bold; color: #fff; background: linear-gradient(135deg, #10b981, #059669);">
                <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                  <path d="M0 3.449L9.75 2.1v9.45H0V3.449zM0 12.45h9.75v9.45L0 20.551v-8.1zM10.8 1.95L24 0v11.55H10.8V1.95zM10.8 12.45H24v11.55l-13.2-1.95v-9.6z"/>
                </svg>
                下载 Windows 客户端
              </a>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="mfade">
      <div v-if="showAuthModal" class="modal-mask" @click="closeModal">
        <div class="modal-box" @click.stop>
          <button class="modal-x" @click="closeModal">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
          </button>
          <!-- OAUTH ONLY LOGIN & REGISTER -->
          <div v-if="authMode === 'login' || authMode === 'register' || authMode === 'forgot_password'" class="auth-pane oauth-only-pane">
            
            <!-- Brand section with glowing logo and gradient title -->
            <div class="oauth-brand-header">
              <div class="oauth-logo-glow" style="display: flex; align-items: center; justify-content: center; background: transparent;">
                <img src="/brand/papersolver-mark-v2.png" alt="PaperSolver" style="width: 44px; height: 44px; object-fit: contain;" />
              </div>
              <h2 class="oauth-brand-title">PaperSolver</h2>
              <p class="oauth-brand-subtitle">下一代 AI 论文辅助研读平台</p>
            </div>

            <!-- Login option card -->
            <!-- Login option card -->
            <div class="oauth-card">
              <!-- QQ Login Method -->
              <div v-if="loginMethod === 'qq'">
                <p class="oauth-hint">为保障学术交流与数据安全，当前支持通过 QQ 一键安全登录与注册，免密且更安全。</p>
                <div v-if="errorText" class="auth-err" style="margin-bottom: 20px;">{{ errorText }}</div>
                
                <button type="button" class="btn-oauth-premium qq" @click="loginWithQQ">
                  QQ 登录
                </button>
                
                <div style="text-align: center; margin-top: 24px;">
                  <a href="javascript:;" @click="loginMethod = 'password'" style="font-size: 0.65rem; color: rgba(255,255,255,0.08); text-decoration: none; transition: color 0.2s;" onmouseover="this.style.color='rgba(255,255,255,0.3)'" onmouseout="this.style.color='rgba(255,255,255,0.08)'">— 内测通道 —</a>
                </div>
              </div>

              <!-- Password Login Method -->
              <div v-else-if="loginMethod === 'password'">
                <p class="oauth-hint" style="margin-bottom: 16px;">使用邮箱与密码登录：</p>
                <div v-if="errorText" class="auth-err" style="margin-bottom: 20px;">{{ errorText }}</div>
                
                <form @submit.prevent="submitLogin" style="display: flex; flex-direction: column; gap: 14px;">
                  <div class="input-group">
                    <input type="email" v-model="email" placeholder="邮箱账号" required style="width: 100%; padding: 12px; border-radius: 8px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: #fff; box-sizing: border-box; outline: none; font-size: 0.9rem;" />
                  </div>
                  <div class="input-group">
                    <input type="password" v-model="password" placeholder="登录密码" required style="width: 100%; padding: 12px; border-radius: 8px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: #fff; box-sizing: border-box; outline: none; font-size: 0.9rem;" />
                  </div>
                  <button type="submit" class="btn-oauth-premium" style="background: linear-gradient(135deg, #2563eb, #7c3aed); border: none; padding: 12px; border-radius: 8px; color: #fff; font-weight: bold; cursor: pointer; display: flex; align-items: center; justify-content: center; width: 100%;">
                    立即登录
                  </button>
                </form>
                
                <div style="text-align: center; margin-top: 20px;">
                  <a href="javascript:;" @click="loginMethod = 'qq'" style="font-size: 0.82rem; color: #71717a; text-decoration: none; border-bottom: 1px dashed rgba(255,255,255,0.2);">返回 QQ 登录</a>
                </div>
              </div>

              <div class="oauth-footer-agreement" style="margin-top: 20px;">
                登录即代表您已同意 <a href="javascript:;" @click="openLegalModal('terms')">用户协议</a> 和 <a href="javascript:;" @click="openLegalModal('privacy')">隐私政策</a>
              </div>
            </div>

            <!-- Optional Desktop Settings card -->
            <div v-if="isDesktopApp" class="desktop-connection-card oauth-desktop-card">
              <span>桌面端状态</span>
              <strong :class="desktopSetupReady ? 'ok' : 'bad'">
                <i></i>{{ desktopSetupReady ? '配置就绪' : '需要配置连接' }}
              </strong>
              <button type="button" @click="openDesktopSettings">设置</button>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="mfade">
      <div v-if="desktopSettingsOpen" class="modal-mask desktop-settings-mask" @click="closeDesktopSettings">
        <div class="modal-box desktop-settings-box" @click.stop>
          <button class="modal-x" @click="closeDesktopSettings">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
          </button>
          <div class="auth-pane desktop-settings-pane">
            <div class="auth-label">DESKTOP SETUP</div>
            <h3 class="auth-title">首次使用配置</h3>
            <p class="desktop-settings-desc">登录前请完成本机保存目录、依赖服务和云端连接检测。配置只保存在这台电脑。</p>
            <div class="auth-form">
              <div class="desktop-setup-status-grid">
                <div :class="['desktop-setup-status-card', desktopConnectionOk ? 'ok' : 'bad']">
                  <span class="status-dot"></span>
                  <strong>云端服务</strong>
                  <small>{{ desktopConnectionOk ? '已验证' : '待检测' }}</small>
                </div>
                <div :class="['desktop-setup-status-card', desktopPdfDirDraft ? 'ok' : 'bad']">
                  <span class="status-dot"></span>
                  <strong>PDF 目录</strong>
                  <small>{{ desktopPdfDirDraft ? '已选择' : '待选择' }}</small>
                </div>
                <div :class="['desktop-setup-status-card', localDependencyState.running ? 'ok' : 'bad']">
                  <span class="status-dot"></span>
                  <strong>本机依赖</strong>
                  <small>{{ localDependencyState.running ? '运行中' : localDependencyState.installed ? '待启动' : '待安装' }}</small>
                </div>
              </div>

              <div class="desktop-settings-section desktop-dependency-section">
                <div class="desktop-settings-section-head">
                  <span>PaperSolver 本机依赖</span>
                  <small>{{ localDependencyState.running ? '可用' : localDependencyState.installed ? '已安装' : '未安装' }}</small>
                </div>
                <p>{{ localDependencyState.message || '用于沉浸翻译和 PDF 结构化解析。请先检测，未安装时可直接安装内置依赖。' }}</p>

                <!-- Lite Mode vs Full Mode selection radio options -->
                <div v-if="!localDependencyState.running" class="lite-mode-selector">
                  <label class="lite-mode-label">
                    <input type="radio" :value="true" v-model="liteModeSelected" />
                    <span>极速精简版（推荐，极速启动，仅下必要模型，日常阅读翻译首选）</span>
                  </label>
                  <label class="lite-mode-label">
                    <input type="radio" :value="false" v-model="liteModeSelected" />
                    <span>专业完整版（需要1.2GB，支持复杂公式与表格重构，下载时间较长）</span>
                  </label>
                </div>

                <div v-if="localDependencyProgress.active" class="desktop-progress">
                  <div class="desktop-progress-bar">
                    <span :style="{ width: `${localDependencyProgress.progress}%` }"></span>
                  </div>
                  <small>{{ localDependencyProgress.message }}</small>
                </div>
                <div class="desktop-dependency-actions">
                  <button type="button" :disabled="localDependencyBusy" @click="refreshLocalDependencyStatus">
                    <span v-if="localDependencyBusy" class="mini-spinner"></span>
                    {{ localDependencyBusy ? '检测中' : '重新检测' }}
                  </button>
                  <button type="button" :disabled="localDependencyBusy" @click="installLocalDependency(false)">
                    {{ localDependencyState.installed ? '修复依赖' : '安装依赖' }}
                  </button>
                  <button type="button" :disabled="localDependencyBusy || !localDependencyState.installed" @click="startLocalDependency">
                    启动依赖
                  </button>
                </div>
              </div>

              <div class="desktop-settings-section desktop-api-section" style="display: none;">
                <div class="desktop-settings-section-head">
                  <span>云端服务地址</span>
                  <small>连接您的私有部署或公网服务</small>
                </div>
                <p>客户端需要与后端服务通信以获取数据与调用 AI 功能。默认主通道为：https://papersolver.cn</p>
                <div class="desktop-pdf-dir-row">
                  <input v-model="desktopApiDraft" type="text" placeholder="例如 https://papersolver.cn" style="color: #fff; background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.1); padding: 8px 12px; border-radius: 6px; flex: 1; font-family: monospace; font-size: 0.85rem;" />
                </div>
              </div>

              <div class="desktop-settings-section desktop-pdf-section">
                <div class="desktop-settings-section-head">
                  <span>PDF 保存目录</span>
                  <small>首次使用先配置</small>
                </div>
                <p>导入、Zotero 同步和手动上传 of PDF 会保存到用户电脑这个目录；后端只记录文献信息和本机缓存标记。</p>
                <div class="desktop-pdf-dir-row">
                  <input v-model="desktopPdfDirDraft" type="text" readonly placeholder="请选择 PDF 保存目录" />
                  <button type="button" :disabled="desktopSettingsSaving" @click="chooseDesktopPdfDir">选择目录</button>
                </div>
              </div>

              <div class="desktop-settings-section desktop-cache-section">
                <div class="desktop-settings-section-head">
                  <span>本机 PDF 缓存</span>
                  <small>{{ desktopCacheInfo.label || '未统计' }}</small>
                </div>
                <p>
                  已缓存 {{ desktopCacheInfo.pdfs || 0 }} 份 PDF，共 {{ desktopCacheInfo.files || 0 }} 个文件。
                  阅读器会优先读取本机缓存，清理后不会删除云端文献记录。
                </p>
                <div v-if="desktopCacheMessage" :class="['desktop-translation-status', desktopCacheMessage.ok ? 'ok' : 'bad']">
                  {{ desktopCacheMessage.text }}
                </div>
                <div class="desktop-cache-actions">
                  <button type="button" :disabled="desktopCacheLoading" @click="refreshDesktopCacheInfo">
                    {{ desktopCacheLoading ? '统计中' : '刷新占用' }}
                  </button>
                  <button type="button" :disabled="desktopCacheLoading" @click="openDesktopCacheDir">打开目录</button>
                  <button type="button" class="danger" :disabled="desktopCacheLoading" @click="clearDesktopPdfCache">清理缓存</button>
                </div>
              </div>

              <div class="desktop-settings-section desktop-update-section compact">
                <div class="desktop-settings-section-head">
                  <span>客户端版本</span>
                  <small>{{ desktopRuntime.version ? `v${desktopRuntime.version}` : '读取中' }}</small>
                </div>
                <div class="desktop-update-row">
                  <span :class="['desktop-update-badge', desktopUpdateStatus.updateAvailable ? 'available' : 'stable']">
                    {{ desktopUpdateStatus.updateAvailable ? '发现更新' : 'Beta 通道' }}
                  </span>
                  <button type="button" :disabled="desktopUpdateChecking" @click="checkDesktopUpdate">
                    {{ desktopUpdateChecking ? '检测中…' : '检测更新' }}
                  </button>
                </div>
                <p>{{ desktopUpdateMessage }}</p>
              </div>
              <div v-if="desktopSettingsMessage" class="auth-ok">{{ desktopSettingsMessage }}</div>
              <div v-if="desktopSettingsError" class="auth-err">{{ desktopSettingsError }}</div>
              <div class="desktop-settings-actions">
                <button class="desktop-test-btn" type="button" :disabled="desktopSettingsSaving || desktopTesting" @click="testDesktopConnection">
                  <span v-if="desktopTesting" class="mini-spinner"></span>
                  {{ desktopTesting ? '检测中' : '检测云端' }}
                </button>
                <button class="auth-submit" :disabled="desktopSettingsSaving || desktopTesting" @click="saveDesktopSettings">
                  {{ desktopSettingsSaving ? '保存中…' : '保存并继续登录' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="mfade">
      <div v-if="desktopGuideOpen" class="modal-mask desktop-guide-mask" @click="closeDesktopGuide">
        <div class="modal-box desktop-guide-box" @click.stop>
          <button class="modal-x" @click="closeDesktopGuide">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
          </button>
          <div class="desktop-guide-kicker">PAPERSOLVER DESKTOP</div>
          <h3>桌面端首次设置</h3>
          <p class="desktop-guide-desc">先确认这几件事，PDF 本机保存、Zotero 和本机翻译会顺很多。</p>
          <div class="desktop-guide-grid">
            <div class="desktop-guide-card">
              <span>01</span>
              <strong>PDF 保存目录</strong>
              <p>先选择一个固定文件夹。导入的 PDF 会留在用户电脑本地，不会堆到服务器。</p>
            </div>
            <div class="desktop-guide-card">
              <span>02</span>
              <strong>后端连接</strong>
              <p>登录、聊天大厅、AI 研读和管理员后台仍需要连接你的后端服务。</p>
            </div>
            <div class="desktop-guide-card">
              <span>03</span>
              <strong>Zotero 本机通信</strong>
              <p>打开 Zotero 设置，在高级里允许本机其他应用通讯，地址为 127.0.0.1:23119。</p>
            </div>
            <div class="desktop-guide-card">
              <span>04</span>
              <strong>本机翻译服务</strong>
              <p>DeepLX、LibreTranslate、MTranServer 需要用户电脑或自部署服务先运行，再填入地址检测。</p>
            </div>
          </div>
          <div class="desktop-guide-actions">
            <button type="button" class="desktop-test-btn" @click="openDesktopSettingsFromGuide">配置保存目录</button>
            <button type="button" class="auth-submit" @click="closeDesktopGuide">知道了</button>
          </div>
        </div>
      </div>
    </Transition>
    <!-- Screenshot Preview Modal -->
    <Transition name="preview-fade">
      <div v-if="previewImage" class="img-preview-overlay" @click="previewImage = null">
        <div class="img-preview-container" @click.stop>
          <img :src="previewImage" alt="Preview Image" class="preview-img-full" />
          <button class="preview-close-btn" @click="previewImage = null">✕</button>
        </div>
      </div>
    </Transition>

    <!-- Detailed Guide Modal -->
    <Transition name="mfade">
      <div v-if="guideModalOpen" class="modal-mask" @click="guideModalOpen = false" style="z-index: 10001; display: flex; align-items: center; justify-content: center; background: rgba(0, 0, 0, 0.6); backdrop-filter: blur(8px);">
        <div class="modal-box" @click.stop style="max-width: 680px; width: 90%; background: #0f172a; border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 16px; padding: 32px; position: relative; box-shadow: var(--sh-xl);">
          <button class="modal-x" @click="guideModalOpen = false" style="position: absolute; right: 20px; top: 20px; font-size: 24px; color: #94a3b8; background: transparent; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center;">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
          
          <h2 style="margin-top: 0; margin-bottom: 24px; color: #fff; font-size: 20px; display: flex; align-items: center; gap: 10px; border-bottom: 1px solid rgba(255,255,255,0.08); padding-bottom: 16px; font-weight: 600;">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color: #6366f1; flex-shrink: 0;">
              <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1-2.5-2.5Z"/>
              <path d="M6 6h10M6 10h10"/>
            </svg>
            PaperSolver 客户端安装与运行配置指引
          </h2>
          
          <div class="guide-scroll-container" style="max-height: 420px; overflow-y: auto; padding-right: 8px;">
            <!-- macOS Guide -->
            <div style="margin-bottom: 28px;">
              <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
                <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20" style="color: #60a5fa; flex-shrink: 0;">
                  <path d="M12.03 5.41c.83-1.05 1.4-2.52 1.25-3.97-1.22.05-2.73.83-3.6 1.88-.77.9-1.44 2.41-1.25 3.82 1.36.1 2.76-.68 3.6-1.73zm4.5 5.86c-.05-2.24 1.79-3.32 1.87-3.37-1.03-1.54-2.63-1.75-3.2-1.81-1.37-.15-2.69.83-3.39.83-.71 0-1.79-.81-2.94-.79-1.51.02-2.92.89-3.69 2.27-1.57 2.78-.4 6.89 1.13 9.15.75 1.1 1.63 2.32 2.78 2.28 1.11-.05 1.54-.73 2.87-.73 1.33 0 1.73.73 2.89.7 1.2-.02 1.98-1.1 2.72-2.2 1.01-1.48 1.42-2.91 1.44-2.98-.03-.02-2.45-.96-2.48-3.76z"/>
                </svg>
                <h3 style="margin: 0; color: #f8fafc; font-size: 15.5px; font-weight: 600;">macOS 客户端安装步骤</h3>
              </div>
              <ol style="margin: 0; padding-left: 20px; color: #cbd5e1; font-size: 14px; line-height: 1.8;">
                <li>下载 <strong>PaperSolver.dmg</strong> 镜像文件。</li>
                <li>双击打开 DMG 镜像文件，将 <strong>PaperSolver.app</strong> 拖拽至右侧的 <strong>Applications (应用程序)</strong> 文件夹中。</li>
                <li>若双击运行时系统拦截并提示 <span style="color: #f43f5e; font-weight: bold;">“已损坏，无法打开。你应该将它移到废纸篓”</span>：
                  <div style="background: rgba(15, 23, 42, 0.6); border: 1px solid rgba(244, 63, 94, 0.2); padding: 12px; border-radius: 8px; margin-top: 10px; margin-bottom: 10px; font-size: 13.5px; color: #cbd5e1;">
                    <div style="display: flex; align-items: center; gap: 8px; color: #f43f5e; font-weight: 600; margin-bottom: 6px; font-size: 13px;">
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0;"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                      Gatekeeper 策略绕过方案
                    </div>
                    打开 Mac 的 <strong>终端 (Terminal)</strong> 应用程序，复制并运行以下命令，接着输入开机锁屏密码并按回车（输密码时屏幕上不会显示字符，直接输完回车即可）：
                    <code style="display: block; background: #020617; padding: 8px 12px; border-radius: 6px; color: #10b981; font-family: monospace; font-size: 12.5px; margin-top: 8px; border: 1px solid rgba(255,255,255,0.05); word-break: break-all;">xattr -cr /Applications/PaperSolver.app</code>
                  </div>
                </li>
                <li>执行命令后重新双击应用，即可完美启动配置并进入系统。</li>
              </ol>
            </div>
            
            <!-- Windows Guide -->
            <div>
              <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
                <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20" style="color: #34d399; flex-shrink: 0;">
                  <path d="M0 3.449L9.75 2.1v9.45H0V3.449zM0 12.45h9.75v9.45L0 20.551v-8.1zM10.8 1.95L24 0v11.55H10.8V1.95zM10.8 12.45H24v11.55l-13.2-1.95v-9.6z"/>
                </svg>
                <h3 style="margin: 0; color: #f8fafc; font-size: 15.5px; font-weight: 600;">Windows 客户端安装步骤</h3>
              </div>
              <ol style="margin: 0; padding-left: 20px; color: #cbd5e1; font-size: 14px; line-height: 1.8;">
                <li>下载 <strong>PaperSolver Setup.exe</strong> 一键安装包。</li>
                <li>双击启动安装，程序会自动释放并生成桌面和开始菜单的快捷方式。</li>
                <li>若系统弹出 Windows SmartScreen 安全警告 <span style="color: #fbbf24; font-weight: bold;">“已阻止无法识别的应用”</span>：
                  <div style="background: rgba(15, 23, 42, 0.6); border: 1px solid rgba(251, 191, 36, 0.2); padding: 12px; border-radius: 8px; margin-top: 10px; margin-bottom: 10px; font-size: 13px; color: #cbd5e1;">
                    点击提示框中的 <strong>“更多信息”</strong> 链接，此时右下角会露出 <strong>“仍要运行”</strong> 按钮，点击即可继续安装。
                  </div>
                </li>
              </ol>
            </div>
          </div>
          
          <div style="margin-top: 24px; text-align: right; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 16px;">
            <button class="btn-solid" @click="guideModalOpen = false" style="background: #fff; border: none; padding: 10px 24px; border-radius: 8px; color: #09090b; font-weight: 600; cursor: pointer; transition: background 0.2s;">我知道了</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { billingPlans } from '../constants/pages'
import { paperpilotApi } from '../services/paperpilotApi'
import { getCurrentApiBaseUrl, normalizeApiBaseUrl, setApiBaseUrl, testApiBaseUrl } from '../services/apiClient'

const authStore = useAuthStore()
const router    = useRouter()
const route     = useRoute()

function decodeBase64Utf8(base64Str) {
  const binaryString = atob(base64Str.replace(/-/g, '+').replace(/_/g, '/'));
  const bytes = new Uint8Array(binaryString.length);
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  const decoder = new TextDecoder("utf-8");
  return decoder.decode(bytes);
}

/* ── Nav scroll ── */
const navScrolled = ref(false)
const onScroll = () => { navScrolled.value = window.scrollY > 40 }

/* ── Hero entrance ── */
const heroIn = ref(false)

/* ── Scroll reveal ── */
const featRef    = ref(null); const featIn    = ref(false)
const wfHeadRef  = ref(null); const wfHeadIn  = ref(false)
const whyRef     = ref(null); const whyIn     = ref(false)
const cmpRef     = ref(null); const cmpIn     = ref(false)


let io
function initIO() {
  io = new IntersectionObserver(entries => {
    entries.forEach(e => {
      if (!e.isIntersecting) return
      if (e.target === featRef.value)   featIn.value   = true
      if (e.target === wfHeadRef.value) wfHeadIn.value = true
      if (e.target === whyRef.value)    whyIn.value    = true
      if (e.target === cmpRef.value)    cmpIn.value    = true

    })
  }, { threshold: 0.1 })
  ;[featRef, wfHeadRef, whyRef, cmpRef].forEach(r => r.value && io.observe(r.value))
}

/* ── Workflow full-slide carousel ── */
const wfActive   = ref(0)
const previewImage = ref(null)

function openImagePreview(url) {
  // In Electron (file:// protocol), absolute paths like /contact/xxx need ./ prefix
  if (isDesktopApp && url && url.startsWith('/')) {
    previewImage.value = '.' + url;
  } else {
    previewImage.value = url;
  }
}
const wfProgress = ref(0)
const WF_DURATION = 5000   // ms per slide
const TICK        = 60     // progress update interval

let wfSlideTimer    = null
let wfProgressTimer = null

function wfGo(i) {
  const len = workflowSteps.length
  wfActive.value = ((i % len) + len) % len
  wfProgress.value = 0
  resetWfAuto()
}

function resetWfAuto() {
  clearTimeout(wfSlideTimer)
  clearInterval(wfProgressTimer)
  let elapsed = 0
  wfProgressTimer = setInterval(() => {
    elapsed += TICK
    wfProgress.value = Math.min(100, (elapsed / WF_DURATION) * 100)
  }, TICK)
  wfSlideTimer = setTimeout(() => {
    wfGo(wfActive.value + 1)
  }, WF_DURATION)
}

/* ── Misc helpers ── */
function elScrollTo(hash) {
  document.querySelector(hash)?.scrollIntoView({ behavior: 'smooth' })
}

/* ── Auth ── */
const showAuthModal      = ref(false)
const showDownloadPromptModal = ref(false)
const showLegalModal      = ref(false)
const legalModalTab      = ref('terms')
const showContactModal    = ref(false)

function openLegalModal(tab) {
  legalModalTab.value = tab
  showLegalModal.value = true
}

const legalContent = {
  terms: {
    title: "用户协议",
    blocks: [
      {
        title: "服务范围",
        lines: [
          "PaperSolver 为文献管理、PDF 阅读、AI 研读、学术论坛、会员权益和桌面端本地依赖管理提供软件服务。",
          "你应保证上传、导入、发布或处理的内容来源合法，不侵犯第三方版权、隐私权或其他合法权益。"
        ]
      },
      {
        title: "账号与权限",
        lines: [
          "账号仅限本人使用。管理员、导师、学生等身份会对应不同操作权限，后台关键操作会记录审计日志。",
          "如出现刷量、攻击、恶意请求、违规发布内容或绕过额度限制等行为，平台可限制、封禁或终止相关账号能力。"
        ]
      },
      {
        title: "会员与付费",
        lines: [
          "会员套餐、限时优惠、额度和权益以购买页与管理员后台实际配置为准。",
          "涉及人工审核、支付异常、退款或权益补发时，请以平台公告、订单记录和客服处理结果为准。"
        ]
      }
    ]
  },
  privacy: {
    title: "隐私政策",
    blocks: [
      {
        title: "数据保存",
        lines: [
          "桌面端导入 of PDF 优先保存在用户本机目录；服务器主要保存题录、笔记、标注索引、会员权益和必要的运行记录。",
          "论坛图片、头像、背景图等用户主动上传的资源会用于页面展示和内容审核。"
        ]
      },
      {
        title: "AI 与第三方服务",
        lines: [
          "AI 研读、综述、PPT、审核或翻译功能可能调用平台配置的模型服务、翻译服务或本机依赖服务。",
          "平台会记录必要的调用时间、模块、模型标识、消耗额度、状态和错误摘要，用于计费、排障和风控，不用于公开展示个人隐私。"
        ]
      },
      {
        title: "安全与删除",
        lines: [
          "我们会对管理员关键操作、异常访问、可疑 IP、异常请求量进行记录 and 监控。",
          "你可以在产品提供的入口管理个人资料和本地缓存；法律法规要求或安全风控需要保留的记录除外。"
        ]
      }
    ]
  },
  disclaimer: {
    title: "免责声明",
    blocks: [
      {
        title: "学术辅助定位",
        lines: [
          "PaperSolver 的 AI 输出仅用于文献阅读、思路整理和研究辅助，不构成投稿、署名、实验、医学、法律或投资等专业结论。",
          "AI 可能出现事实错误、引用遗漏、翻译偏差或格式不完整，正式使用前需要用户自行核验原文、数据和引用。"
        ]
      },
      {
        title: "版权与来源",
        lines: [
          "用户应确认自己有权下载、导入和处理相关论文、图片、附件或网页内容。",
          "因用户未经授权传播、下载、分享或公开他人内容产生的责任，由用户自行承担。"
        ]
      }
    ]
  }
}

const loginMethod        = ref('qq')
const isLocal            = ref(window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
const authMode           = ref('login')
const email              = ref('')
const password           = ref('')
const name               = ref('')
const inviteCode         = ref('')
const role               = ref('学生')
const mentorInviteCode   = ref('')
const verificationCode   = ref('')
const errorText          = ref('')
const loading            = ref(false)
const sendingRegisterCode= ref(false)
const registerCodeCooldown=ref(0)
const registerSuccessText= ref('')
const forgotEmail        = ref('')
const forgotCode         = ref('')
const forgotNewPassword  = ref('')
const sendingCode        = ref(false)
const forgotSuccessText  = ref('')
const showPassword       = ref(false)
const isDesktopApp       = Boolean(window.paperSolverDesktop?.isDesktop)
const guideModalOpen     = ref(false)
const mouseX             = ref(-999)
const mouseY             = ref(-999)
function handleMouseMove(e) {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}
const desktopApiBase     = ref(getCurrentApiBaseUrl())
const desktopApiDraft    = ref(desktopApiBase.value)
const desktopPdfDirDraft = ref('')
const desktopSettingsOpen= ref(false)
const desktopSettingsSaving = ref(false)
const desktopTesting = ref(false)
const desktopSettingsError = ref('')
const desktopSettingsMessage = ref('')
const desktopGuideOpen = ref(false)
const desktopCacheLoading = ref(false)
const desktopCacheMessage = ref(null)
const desktopUpdateChecking = ref(false)
const desktopRuntime = reactive({
  version: '',
  channel: 'beta',
})
const desktopUpdateStatus = reactive({
  updateAvailable: false,
  latestVersion: '',
  message: '',
})
const desktopConnectionOk = ref(false)
const desktopSetupCompleted = ref(false)
const localDependencyBusy = ref(false)
const liteModeSelected = ref(true)
watch(liteModeSelected, (newVal) => {
  if (window.paperSolverDesktop?.setBackendConfig) {
    window.paperSolverDesktop.setBackendConfig({ localDependencyLiteMode: newVal }).catch(() => {})
  }
})
const localDependencyState = reactive({
  ok: false,
  installed: false,
  running: false,
  structuredParserAvailable: false,
  message: '',
  latencyMs: 0,
})
const localDependencyProgress = reactive({
  active: false,
  progress: 0,
  message: '',
  stage: '',
})
const desktopCacheInfo = reactive({
  label: '',
  bytes: 0,
  files: 0,
  pdfs: 0,
  path: '',
})
const desktopTranslationDraft = reactive({
  deeplxEndpoint: '',
  libreTranslateEndpoint: '',
  mtranServerEndpoint: '',
})
const desktopTranslationTesting = reactive({
  deeplx: false,
  libretranslate: false,
  mtranserver: false,
})
const desktopTranslationStatus = reactive({
  deeplx: null,
  libretranslate: null,
  mtranserver: null,
})
const DESKTOP_GUIDE_KEY = 'papersolver_desktop_first_setup_seen'
let rcTimer = null
let unsubscribeDependencyProgress = null

const desktopSetupReady = computed(() => {
  if (!isDesktopApp) return true
  return Boolean(desktopConnectionOk.value && desktopPdfDirDraft.value && localDependencyState.running)
})

const eyeIcon    = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:18px;height:18px"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`
const eyeOffIcon = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:18px;height:18px"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>`

const registerCodeButtonText = computed(() => {
  if (sendingRegisterCode.value) return '发送中'
  if (registerCodeCooldown.value > 0) return `${registerCodeCooldown.value}s`
  return '获取验证码'
})
function isQqEmail(v) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/i.test(String(v||'').trim()) }
function startCooldown() {
  registerCodeCooldown.value = 60
  if (rcTimer) clearInterval(rcTimer)
  rcTimer = setInterval(() => { registerCodeCooldown.value -= 1; if (registerCodeCooldown.value <= 0) { clearInterval(rcTimer); rcTimer = null } }, 1000)
}
async function sendRegisterCode() {
  const e = email.value.trim().toLowerCase()
  if (!isQqEmail(e)) { errorText.value = '请填写有效的邮箱地址'; return }
  sendingRegisterCode.value = true; errorText.value = ''; registerSuccessText.value = ''
  try { await paperpilotApi.sendRegisterCode(e); email.value = e; registerSuccessText.value = '验证码已发送，10 分钟内有效。'; startCooldown() }
  catch (err) { errorText.value = err.response?.data?.message || err.message }
  finally { sendingRegisterCode.value = false }
}
async function sendForgotCode() {
  if (!forgotEmail.value) { errorText.value = '请输入邮箱'; return }
  sendingCode.value = true; errorText.value = ''; forgotSuccessText.value = ''
  try { await paperpilotApi.sendForgotPasswordCode(forgotEmail.value); forgotSuccessText.value = '验证码已生成，请在系统日志中查看。' }
  catch (err) { errorText.value = err.response?.data?.message || err.message }
  finally { sendingCode.value = false }
}
async function submitResetPassword() {
  if (!forgotEmail.value || !forgotCode.value || !forgotNewPassword.value) { errorText.value = '请填写全部字段'; return }
  loading.value = true; errorText.value = ''; forgotSuccessText.value = ''
  try {
    await paperpilotApi.resetPasswordWithCode({ email: forgotEmail.value, code: forgotCode.value, newPassword: forgotNewPassword.value })
    forgotSuccessText.value = '密码已重置，即将跳转登录…'
    setTimeout(() => { authMode.value = 'login'; forgotSuccessText.value = ''; email.value = forgotEmail.value; password.value = forgotNewPassword.value }, 2000)
  } catch (err) { errorText.value = err.response?.data?.message || err.message }
  finally { loading.value = false }
}
function openModal(mode) {
  authMode.value = mode; errorText.value = ''; registerSuccessText.value = ''; forgotSuccessText.value = ''; showPassword.value = false
  if (mode === 'login') { email.value = 'student@papersolver.app'; password.value = 'Student2026!' }
  else { email.value = ''; password.value = ''; verificationCode.value = ''; inviteCode.value = ''; name.value = '' }
  showAuthModal.value = true
}
function closeModal() { showAuthModal.value = false; showPassword.value = false; errorText.value = '' }
async function loadDesktopBackendConfig() {
  if (!window.paperSolverDesktop?.getBackendConfig) return
  try {
    await loadDesktopRuntimeInfo()
    const config = await window.paperSolverDesktop.getBackendConfig()
    const nextUrl = normalizeApiBaseUrl(config?.apiBaseUrl) || getCurrentApiBaseUrl()
    applyDesktopTranslationConfig(config?.translationEndpoints)
    desktopPdfDirDraft.value = config?.pdfStorageDir || ''
    desktopSetupCompleted.value = Boolean(config?.setupCompleted)
    if (config?.localDependencyLiteMode !== undefined) {
      liteModeSelected.value = Boolean(config.localDependencyLiteMode)
    }
    desktopApiBase.value = nextUrl
    desktopApiDraft.value = nextUrl
    setApiBaseUrl(nextUrl, { persist: true })
    await Promise.allSettled([
      refreshLocalDependencyStatus({ silent: true }),
      testDesktopConnection({ silent: true }),
      refreshDesktopCacheInfo(),
    ])
  } catch {
    desktopApiBase.value = getCurrentApiBaseUrl()
    desktopApiDraft.value = desktopApiBase.value
  } finally {
    if (isDesktopApp) {
      setTimeout(() => openDesktopSettings(), 500)
    }
  }
}
async function loadDesktopRuntimeInfo() {
  if (!window.paperSolverDesktop?.getRuntimeInfo) return
  try {
    const info = await window.paperSolverDesktop.getRuntimeInfo()
    desktopRuntime.version = info?.version || ''
    desktopRuntime.channel = info?.channel || 'beta'
    desktopUpdateStatus.message = info?.updatePolicy?.message || ''
  } catch {
    desktopRuntime.version = ''
  }
}
const desktopUpdateMessage = computed(() => {
  if (desktopUpdateStatus.message) return desktopUpdateStatus.message
  return desktopRuntime.version
    ? `当前客户端版本 v${desktopRuntime.version}，正式分发后支持检测更新、下载并重启安装。`
    : '正在读取客户端版本信息。'
})
async function checkDesktopUpdate() {
  if (!window.paperSolverDesktop?.checkUpdate) {
    desktopUpdateStatus.message = '当前桌面壳不支持检测更新，请安装新版客户端。'
    return
  }
  desktopUpdateChecking.value = true
  desktopUpdateStatus.message = ''
  try {
    const result = await window.paperSolverDesktop.checkUpdate()
    desktopUpdateStatus.updateAvailable = Boolean(result?.updateAvailable)
    desktopUpdateStatus.latestVersion = result?.latestVersion || ''
    desktopUpdateStatus.message = result?.message || '检测完成。'
  } catch (err) {
    desktopUpdateStatus.updateAvailable = false
    desktopUpdateStatus.message = err?.message || '更新检测失败，请稍后重试。'
  } finally {
    desktopUpdateChecking.value = false
  }
}
function openDesktopSettings() {
  desktopApiDraft.value = "https://papersolver.cn"
  desktopConnectionOk.value = true
  desktopSettingsError.value = ''
  desktopSettingsMessage.value = ''
  desktopSettingsOpen.value = true
  refreshDesktopCacheInfo()
  refreshLocalDependencyStatus({ silent: true })
}
async function chooseDesktopPdfDir() {
  if (!window.paperSolverDesktop?.selectPdfStorageDir) {
    desktopSettingsError.value = '当前桌面壳不支持选择保存目录，请重启或更新桌面端。'
    return
  }
  desktopSettingsError.value = ''
  desktopSettingsMessage.value = ''
  try {
    const result = await window.paperSolverDesktop.selectPdfStorageDir()
    if (!result?.canceled && result?.path) {
      desktopPdfDirDraft.value = result.path
      applyDesktopCacheInfo({ ...desktopCacheInfo, path: result.path })
      desktopSettingsMessage.value = 'PDF 保存目录已更新。'
      refreshDesktopCacheInfo()
    }
  } catch (err) {
    desktopSettingsError.value = err?.message || '选择目录失败'
  }
}
function closeDesktopSettings() {
  desktopSettingsOpen.value = false
  desktopSettingsError.value = ''
}
function closeDesktopGuide() {
  desktopGuideOpen.value = false
  try { localStorage.setItem(DESKTOP_GUIDE_KEY, '1') } catch {}
}
function openDesktopSettingsFromGuide() {
  closeDesktopGuide()
  openDesktopSettings()
}
async function testDesktopConnection(options = {}) {
  const apiBaseUrl = normalizeApiBaseUrl(desktopApiDraft.value)
  if (!apiBaseUrl) {
    desktopSettingsError.value = '请输入有效地址，例如 https://api.papersolver.cn'
    return
  }
  desktopTesting.value = true
  if (!options.silent) {
    desktopSettingsError.value = ''
    desktopSettingsMessage.value = ''
  }
  try {
    await testApiBaseUrl(apiBaseUrl)
    desktopConnectionOk.value = true
    if (!options.silent) desktopSettingsMessage.value = '云端连接正常。'
  } catch (err) {
    desktopConnectionOk.value = false
    if (!options.silent) desktopSettingsError.value = err?.message || '连接失败'
  } finally {
    desktopTesting.value = false
  }
}
async function testDesktopTranslationProvider(provider) {
  if (!window.paperSolverDesktop?.testTranslationProvider) {
    desktopTranslationStatus[provider] = { ok: false, message: '当前桌面壳不支持本机翻译检测，请重启应用。' }
    return
  }
  const endpoints = normalizedDesktopTranslationDraft()
  const endpointByProvider = {
    deeplx: endpoints.deeplxEndpoint,
    libretranslate: endpoints.libreTranslateEndpoint,
    mtranserver: endpoints.mtranServerEndpoint,
  }
  if (!endpointByProvider[provider]) {
    desktopTranslationStatus[provider] = { ok: false, message: '请先填写有效的 http:// 或 https:// 地址。' }
    return
  }
  desktopTranslationTesting[provider] = true
  desktopTranslationStatus[provider] = null
  try {
    const result = await window.paperSolverDesktop.testTranslationProvider({
      provider,
      translationEndpoints: endpoints,
    })
    desktopTranslationStatus[provider] = {
      ok: true,
      message: `连接正常，测试译文：${String(result?.translatedText || '').slice(0, 28) || '已返回结果'}`
    }
  } catch (err) {
    desktopTranslationStatus[provider] = {
      ok: false,
      message: err?.message || '检测失败，请确认本机服务已启动。'
    }
  } finally {
    desktopTranslationTesting[provider] = false
  }
}
async function refreshDesktopCacheInfo() {
  if (!window.paperSolverDesktop?.getCacheInfo) return
  desktopCacheLoading.value = true
  desktopCacheMessage.value = null
  try {
    const info = await window.paperSolverDesktop.getCacheInfo()
    applyDesktopCacheInfo(info)
  } catch (err) {
    desktopCacheMessage.value = { ok: false, text: err?.message || '缓存统计失败' }
  } finally {
    desktopCacheLoading.value = false
  }
}
function applyLocalDependencyStatus(status = {}) {
  localDependencyState.ok = Boolean(status.ok)
  localDependencyState.installed = Boolean(status.installed)
  localDependencyState.running = Boolean(status.running)
  localDependencyState.structuredParserAvailable = Boolean(status.structuredParserAvailable)
  localDependencyState.message = status.message || ''
  localDependencyState.latencyMs = Number(status.latencyMs) || 0
  if (status.liteMode !== undefined) {
    liteModeSelected.value = Boolean(status.liteMode)
  }
}
async function refreshLocalDependencyStatus(options = {}) {
  if (!window.paperSolverDesktop?.getLocalDependencyStatus) return
  localDependencyBusy.value = true
  if (!options.silent) {
    desktopSettingsError.value = ''
    desktopSettingsMessage.value = ''
  }
  try {
    const status = await window.paperSolverDesktop.getLocalDependencyStatus()
    applyLocalDependencyStatus(status)
    if (!options.silent) {
      desktopSettingsMessage.value = status.running ? '本机依赖运行正常。' : status.message || '检测完成。'
    }
  } catch (err) {
    applyLocalDependencyStatus({ ok: false, installed: false, running: false, message: err?.message || '依赖检测失败。' })
    if (!options.silent) desktopSettingsError.value = err?.message || '依赖检测失败。'
  } finally {
    localDependencyBusy.value = false
  }
}
async function installLocalDependency(force = false) {
  if (!window.paperSolverDesktop?.downloadLocalDependency) return
  localDependencyBusy.value = true
  desktopSettingsError.value = ''
  desktopSettingsMessage.value = ''
  localDependencyProgress.active = true
  localDependencyProgress.progress = 4
  localDependencyProgress.message = force ? '正在重新安装本机依赖...' : '正在安装本机依赖...'
  try {
    const status = await window.paperSolverDesktop.downloadLocalDependency({ force, liteMode: liteModeSelected.value })
    applyLocalDependencyStatus(status)
    desktopSettingsMessage.value = '本机依赖已安装并启动。'
  } catch (err) {
    desktopSettingsError.value = err?.message || '安装依赖失败。'
  } finally {
    localDependencyBusy.value = false
    setTimeout(() => {
      localDependencyProgress.active = false
    }, 900)
  }
}
async function startLocalDependency() {
  if (!window.paperSolverDesktop?.startLocalDependency) return
  localDependencyBusy.value = true
  desktopSettingsError.value = ''
  desktopSettingsMessage.value = ''
  localDependencyProgress.active = true
  localDependencyProgress.progress = 35
  localDependencyProgress.message = '正在启动本机依赖...'
  try {
    await window.paperSolverDesktop.startLocalDependency()
    await refreshLocalDependencyStatus({ silent: true })
    desktopSettingsMessage.value = '本机依赖已启动。'
  } catch (err) {
    desktopSettingsError.value = err?.message || '启动依赖失败。'
  } finally {
    localDependencyBusy.value = false
    localDependencyProgress.progress = localDependencyState.running ? 100 : localDependencyProgress.progress
    setTimeout(() => {
      localDependencyProgress.active = false
    }, 900)
  }
}
async function openDesktopCacheDir() {
  if (!window.paperSolverDesktop?.openCacheDir) return
  desktopCacheLoading.value = true
  desktopCacheMessage.value = null
  try {
    await window.paperSolverDesktop.openCacheDir()
    desktopCacheMessage.value = { ok: true, text: '已打开本机缓存目录。' }
  } catch (err) {
    desktopCacheMessage.value = { ok: false, text: err?.message || '打开缓存目录失败' }
  } finally {
    desktopCacheLoading.value = false
  }
}
async function clearDesktopPdfCache() {
  if (!window.paperSolverDesktop?.clearPdfCache) return
  desktopCacheLoading.value = true
  desktopCacheMessage.value = null
  try {
    const result = await window.paperSolverDesktop.clearPdfCache()
    applyDesktopCacheInfo({ bytes: 0, files: 0, pdfs: 0, label: '0 B', path: result?.path || desktopCacheInfo.path })
    desktopCacheMessage.value = { ok: true, text: `已清理 ${result?.label || '缓存'}。` }
  } catch (err) {
    desktopCacheMessage.value = { ok: false, text: err?.message || '清理缓存失败' }
  } finally {
    desktopCacheLoading.value = false
  }
}
function applyDesktopCacheInfo(info = {}) {
  desktopCacheInfo.label = info.label || '0 B'
  desktopCacheInfo.bytes = Number(info.bytes) || 0
  desktopCacheInfo.files = Number(info.files) || 0
  desktopCacheInfo.pdfs = Number(info.pdfs) || 0
  desktopCacheInfo.path = info.path || ''
}
async function saveDesktopSettings() {
  const apiBaseUrl = "https://papersolver.cn"
  desktopSettingsSaving.value = true
  desktopSettingsError.value = ''
  desktopSettingsMessage.value = ''
  try {
    desktopConnectionOk.value = true
    await refreshLocalDependencyStatus({ silent: true })
    if (!desktopPdfDirDraft.value) {
      throw new Error('请先选择 PDF 保存目录。')
    }
    if (!localDependencyState.running) {
      throw new Error('本机依赖未运行，请先安装或启动依赖。')
    }
    if (window.paperSolverDesktop?.setBackendConfig) {
      await window.paperSolverDesktop.setBackendConfig({
        apiBaseUrl,
        pdfStorageDir: desktopPdfDirDraft.value,
        translationEndpoints: normalizedDesktopTranslationDraft(),
        setupCompleted: true,
      })
    }
    setApiBaseUrl(apiBaseUrl, { persist: true })
    desktopApiBase.value = apiBaseUrl
    desktopSetupCompleted.value = true
    desktopSettingsMessage.value = '配置已完成，可以继续登录。'
    setTimeout(() => { desktopSettingsOpen.value = false }, 900)
  } catch (err) {
    desktopSettingsError.value = err?.message || '保存失败'
  } finally {
    desktopSettingsSaving.value = false
  }
}
async function resetDesktopSettings() {
  desktopSettingsSaving.value = true
  desktopSettingsError.value = ''
  desktopSettingsMessage.value = ''
  try {
    const config = window.paperSolverDesktop?.resetBackendConfig
      ? await window.paperSolverDesktop.resetBackendConfig()
      : { apiBaseUrl: 'https://papersolver.cn' }
    const nextUrl = normalizeApiBaseUrl(config?.apiBaseUrl) || 'https://papersolver.cn'
    applyDesktopTranslationConfig(config?.translationEndpoints)
    desktopPdfDirDraft.value = config?.pdfStorageDir || ''
    setApiBaseUrl(nextUrl, { persist: true })
    desktopApiBase.value = nextUrl
    desktopApiDraft.value = nextUrl
    desktopSettingsMessage.value = '已恢复默认地址，并清空本机翻译配置。'
  } catch (err) {
    desktopSettingsError.value = err?.message || '恢复失败'
  } finally {
    desktopSettingsSaving.value = false
  }
}
function applyDesktopTranslationConfig(endpoints = {}) {
  desktopTranslationDraft.deeplxEndpoint = endpoints.deeplxEndpoint || ''
  desktopTranslationDraft.libreTranslateEndpoint = endpoints.libreTranslateEndpoint || ''
  desktopTranslationDraft.mtranServerEndpoint = endpoints.mtranServerEndpoint || ''
}
function normalizedDesktopTranslationDraft() {
  return {
    deeplxEndpoint: normalizeApiBaseUrl(desktopTranslationDraft.deeplxEndpoint) || '',
    libreTranslateEndpoint: normalizeApiBaseUrl(desktopTranslationDraft.libreTranslateEndpoint) || '',
    mtranServerEndpoint: normalizeApiBaseUrl(desktopTranslationDraft.mtranServerEndpoint) || '',
  }
}
async function loginWithQQ() {
  const appId = '1905318043';
  const redirectUri = encodeURIComponent('https://papersolver.cn/api/auth/qq/callback');
  const qqAuthUrl = `https://graph.qq.com/oauth2.0/authorize?response_type=code&client_id=${appId}&redirect_uri=${redirectUri}&state=papersolver`;
  if (isDesktopApp && window.paperSolverDesktop?.oauthQQ) {
    console.log("[LoginView] oauthQQ desktop flow started with url:", qqAuthUrl);
    try {
      const result = await window.paperSolverDesktop.oauthQQ(qqAuthUrl);
      console.log("[LoginView] oauthQQ resolved. Result:", result);
      if (result?.qqSession) {
        const json = decodeBase64Utf8(result.qqSession);
        console.log("[LoginView] Decoded JSON:", json);
        const user = JSON.parse(json);
        console.log("[LoginView] Applying session for user:", user);
        authStore.applySession(user);
        console.log("[LoginView] Session applied. Routing to library...");
        router.push(authStore.session.role === '管理员' ? '/admin' : '/library');
      } else {
        console.warn("[LoginView] oauthQQ resolved but qqSession was missing:", result);
      }
    } catch (e) {
      console.error("[LoginView] oauthQQ catch block error:", e);
      errorText.value = e.message || 'QQ 登录失败，请重试。';
      openModal('login');
    }
    return;
  }
  // Web browser flow
  window.location.href = qqAuthUrl;
}
async function submitLogin() {
  if (isDesktopApp && !desktopSetupReady.value) {
    errorText.value = '请先完成桌面端首次配置。'
    openDesktopSettings()
    return
  }
  loading.value = true; errorText.value = ''
  try { await authStore.login({ email: email.value, password: password.value }); router.push(authStore.session.role === '管理员' ? '/admin' : '/library') }
  catch (err) { errorText.value = authErrorMessage(err) }
  finally { loading.value = false }
}
async function submitRegister() {
  if (isDesktopApp && !desktopSetupReady.value) {
    errorText.value = '请先完成桌面端首次配置。'
    openDesktopSettings()
    return
  }
  if (!isQqEmail(email.value)) { errorText.value = '请填写有效的邮箱地址'; return }
  if (!verificationCode.value || verificationCode.value.length !== 6) { errorText.value = '请输入 6 位验证码'; return }
  loading.value = true; errorText.value = ''
  try { await authStore.register({ inviteCode: inviteCode.value, name: name.value, email: email.value, password: password.value, role: role.value, mentorInviteCode: mentorInviteCode.value, verificationCode: verificationCode.value }); router.push(authStore.session.role === '管理员' ? '/admin' : '/library') }
  catch (err) { errorText.value = authErrorMessage(err) }
  finally { loading.value = false }
}

function authErrorMessage(err) {
  if (isDesktopApp && (err?.message === 'Network Error' || err?.code === 'ECONNABORTED' || !err?.response)) {
    return '后端连接失败。请点击下方“连接设置”，先测试后端地址是否可用。'
  }
  return err?.response?.data?.message || err?.message || '请求失败，请稍后重试'
}

/* ── Static data ── */
const features = [
  { title:'文献库',   desc:'本地 PDF 文献管理与 Zotero 题录极速双向同步。自动补全元数据，文献全文均安全留存于个人本地电脑，严防隐私泄露。',  vb:'0 0 24 24', path:'<path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H19a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H6.5a1 1 0 0 1 0-5H20"/><line x1="8" y1="7" x2="16" y2="7"/><line x1="8" y1="11" x2="13" y2="11"/>',  color:'#3b82f6', chips:['本地物理留存','Zotero 极速同步','元数据智能补全'] },
  { title:'多维翻译', desc:'双栏对照与沉浸式翻译。支持句子级精准划线与选区批注。内置自研大模型，选中任何不懂的专业词汇或复杂段落，可一键提问深度解析。', vb:'0 0 24 24', path:'<path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>',                                                             color:'#8b5cf6', chips:['双栏对照与沉浸翻译','自研模型智能提问','选区划线精准批注'] },
  { title:'论文综述', desc:'一键智能提炼研究背景、发现亮点与汇报价值。自动高亮核心数字指标与关键英文术语，为组会分享提供标准输出。',     vb:'0 0 24 24', path:'<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/>', color:'#10b981', chips:['核心要点智能提炼','关键数字自动高亮','组会综述大纲生成'] },
  { title:'调研广场', desc:'一键开启深度学术调研。智能生成定制研究选题卡，系统性梳理代表性文献，并精准捕捉现有工作的关键研究空白。',       vb:'0 0 24 24', path:'<circle cx="12" cy="12" r="10"></circle><polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"></polygon>',                                                                                                                                    color:'#06b6d4', chips:['深度选题卡生成','代表论文梳理','研究空白敏锐捕捉'] },
  { title:'组会汇报', desc:'支持将最多三篇文献深度串联融汇。采用原生 PPTmaster 引擎制作，高保真还原精美排版，一键生成完整的宣讲讲稿与导师问答建议。',             vb:'0 0 24 24', path:'<rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/><path d="m7 10 3 3 5-5"/>',                                                                                                   color:'#f59e0b', chips:['原生 PPTmaster 制作','多文献深度串联','宣讲讲稿自动生成'] },
  { title:'学术论坛', desc:'高校邮箱实名认证的学术互助社区。支持同校专属帖子精确筛选，内置 AI 发帖规范性审查，学术交流更合规更纯粹。',           vb:'0 0 24 24', path:'<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><line x1="9" y1="10" x2="15" y2="10"/><line x1="9" y1="14" x2="13" y2="14"/>',                               color:'#ec4899', chips:['邮箱实名认证','AI 发帖规范审查','同校学术圈筛选'] },
]

const workflowSteps = [
  {
    id: 'lib',
    title: '全部文献归纳与题录沉淀',
    desc: '支持快速导入本地 PDF 论文，或一键从 Zotero 题录导入。系统会自动识别并智能补全作者、期刊、中科院分区与影响因子标签。',
    highlights: [
      '<b>无缝同步 Zotero 题录</b>：快速拉取分类与元数据，免去手动键入',
      '<b>核心分区指标一览</b>：直观查看文献的 IF、JCR 分区与中科院等级',
      '<b>文献生命周期管理</b>：阅读进度、个人批注与笔记一目了然'
    ],
    tags: ['PDF 导入', 'Zotero 同步', '文献属性自动补全'],
    img: '/workflow/library.png'
  },
  {
    id: 'read',
    title: '学术助手 PaperSolver 深度对话',
    desc: '内置专门优化的自研 PaperSolver 学术大模型。支持对论文全文、特定选区段落或图表进行智能多轮追问，瞬间理解复杂学术概念。',
    highlights: [
      '<b>自研学术大模型</b>：专门针对学术论文及图表解析进行定制优化',
      '<b>划词/选区智能追问</b>：选中复杂数学公式或实验方法，即可一键追问',
      '<b>提问与生成时间标记</b>：消息时间精确记录，阅读进度更清晰'
    ],
    tags: ['AI 研读助手', '多轮追问', 'PaperSolver 模型'],
    img: '/workflow/reader.png'
  },
  {
    id: 'review',
    title: '文献结构化提炼与综述生成',
    desc: '自动从文献中深度萃取主要发现、对比证据、机制阐释、贡献价值与核心创新，生成高保真结构化解析大纲。',
    highlights: [
      '<b>研究发现智能提取</b>：提炼核心立论与机制演变路径',
      '<b>对比证据全面捕获</b>：系统归纳实验数据与关键增幅比例',
      '<b>一键流转至组会</b>：综述内容直接对接汇报主线，免去复制粘贴'
    ],
    tags: ['文献结构化综述', '数据要点高亮', '一键转流组会'],
    img: '/workflow/review.png'
  },
  {
    id: 'meet',
    title: '组会汇报与原生 PPTX 极速导出',
    desc: '将日常阅读的文献笔记与精读记录，一键转化为组会汇报主线。基于原生 PPTmaster 引擎自动排版，快速导出完整幻灯片。',
    highlights: [
      '<b>笔记记录一键流转</b>：从多篇精读论文笔记秒级转为组会主线大纲',
      '<b>原生 PPT 引擎制作</b>：高保真还原精美排版，一键导出 PPTX 文件',
      '<b>自动生成宣讲讲稿</b>：系统预估汇报用时，提供针对性的导师问答建议'
    ],
    tags: ['多文献融合', '原生 PPT 导出', '汇报讲稿提炼'],
    img: '/workflow/meeting.png'
  },
  {
    id: 'forum',
    title: '选题大厅与研究方向规划',
    desc: '开题找不到方向？根据您的研究兴趣和投递定位，智能规划具有高可行性与高创新度的科研选题，并系统梳理现有研究空白。',
    highlights: [
      '<b>创新度与可行性量化</b>：评估选题发表成功率与落地难易度分数',
      '<b>研究问题与空白梳理</b>：罗列该选题需解答的核心学术疑问',
      '<b>推荐研究方向推荐</b>：提供文献调研结构与针对性的实验方案'
    ],
    tags: ['科研选题评分', '研究空白梳理', '推荐方向生成'],
    img: '/workflow/forum.png'
  }
]

const whyClaims = [
  {
    pain: '苦恼文献需要一个个手动下载？',
    solution: 'PaperSolver 自带智能浏览器插件，精准识别网页文献，一键极速导入工作台。',
    color: '#38bdf8',
    vb: '0 0 24 24',
    path: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>'
  },
  {
    pain: '苦恼无法订阅 ChatGPT、用不上高质量学术模型？',
    solution: '内置专门针对论文优化的自研大模型，开箱即用，精准解析文献，无需魔法，不用再丢 PDF 给网页。',
    color: '#818cf8',
    vb: '0 0 24 24',
    path: '<path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/>'
  },
  {
    pain: '苦恼 PDF 阅读太单一、排版不喜欢？',
    solution: '提供三大阅读模式：1. PDF 原生排版全文对照看；2. 逐段翻译分拆精细读；3. 沉浸原生英文手动划词译。',
    color: '#34d399',
    vb: '0 0 24 24',
    path: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>'
  },
  {
    pain: '苦恼看完长篇大论依然一头雾水？',
    solution: '左侧边栏提供一键生成高质量论文综述与核心贡献总结，论点脉络一目了然。',
    color: '#fb7185',
    vb: '0 0 24 24',
    path: '<path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>'
  },
  {
    pain: '苦恼看完论文要花大量时间写讲稿、做组会 PPT？',
    solution: '内置 Codex 原生大模型引擎并无缝对接 PPTMaster，帮您一键梳理组会主线并产出精美 PPT。',
    color: '#fbbf24',
    vb: '0 0 24 24',
    path: '<rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/><path d="m7 10 3 3 5-5"/>'
  },
  {
    pain: '苦恼选题开题没有方向、找不到研究空白？',
    solution: '开放智能“选题大厅”，汇集前沿科研灵感并结合 AI 助力，量身定制生成属于您自己的选题方向。',
    color: '#a78bfa',
    vb: '0 0 24 24',
    path: '<circle cx="12" cy="12" r="10"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>'
  },
  {
    pain: '苦恼找不到实验数据集？论文卡壳没人讨论？',
    solution: '开放高质化学术论坛，供您共享数据集、交流讨论学术瓶颈，在研究道路上结识同行知己。',
    color: '#22d3ee',
    vb: '0 0 24 24',
    path: '<path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>'
  }
]

const track1Items = computed(() => [whyClaims[0], whyClaims[1], whyClaims[2], whyClaims[3]])
const track2Items = computed(() => [whyClaims[4], whyClaims[5], whyClaims[6], whyClaims[0]])
const track3Items = computed(() => [whyClaims[2], whyClaims[3], whyClaims[4], whyClaims[5]])

const whyBadges = [
  { label:'永久免费基础功能',    color:'#22c55e', vb:'0 0 24 24', path:'<path d="M20 6 9 17l-5-5"/>'                                                                                    },
  { label:'无需安装，浏览器直用', color:'#3b82f6', vb:'0 0 24 24', path:'<circle cx="12" cy="12" r="10"/><path d="M2 12h20"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>' },
  { label:'校园认证用户专属权益', color:'#8b5cf6', vb:'0 0 24 24', path:'<path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/>'                                },
  { label:'数据安全，随时导出',   color:'#f59e0b', vb:'0 0 24 24', path:'<rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>'             },
]

const comparisons = [
  {
    feature: '数据存储与隐私安全',
    a: '云端强制上传文件，有严重泄露学术成果和机密的风险',
    b: 'PDF 全文托管在第三方云服务器，学术成果受制于平台',
    our: '100% 本地物理留存，云端仅加密同步轻量级笔记与对话，无隐私泄露风险'
  },
  {
    feature: '文献对照翻译体验',
    a: '覆盖原排版的粗糙机翻，排版极易错乱，且中英对照割裂',
    b: '翻译需每次重复上传文件并反复产生计费，加载排版受限',
    our: '双栏高保真对照翻译，选区精准批注，流畅沉浸且永不破版'
  },
  {
    feature: '大模型对话提问交互',
    a: '仅支持简单的全局聊天，无法针对选区特定词句定向追问',
    b: '限制使用云端固定大模型，调用按次收费非常昂贵',
    our: '支持选区一键定向提问，内置自研大模型，支持极低成本自主配置'
  },
  {
    feature: '组会汇报与幻灯片输出',
    a: '仅生成纯文本综述大纲，无法辅助排版设计和输出 PPT',
    b: '套用通用模板转换，内容逻辑较平，排版呆板且后期难修改',
    our: '深度融合多篇文献主线，采用原生 PPTmaster 制作，自动输出高美感幻灯片'
  },
  {
    feature: '选题调研与空白敏锐捕捉',
    a: '无相关功能，无法进行深度的选题开拓',
    b: '提供极高价格且数据源受限的文献脉络分析图',
    our: '独创“调研广场”，全自动提炼代表作、生成选题卡并寻找现有工作空白'
  },
  {
    feature: '学术交流',
    a: '仅限制于展示静态文本，不支持对数据集与核心方法的深度关联探究',
    b: 'AI 对话仅限于常规聊天，无法直接解析及探讨特定的公式算法与外部数据集',
    our: '内置学术问答与社区模块，支持直接针对数据集细节提问、探讨论文核心方法与公式逻辑'
  }
]

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  setTimeout(() => { heroIn.value = true }, 80)
  setTimeout(initIO, 200)
  setTimeout(resetWfAuto, 1200)
  if (isDesktopApp && window.paperSolverDesktop?.onLocalDependencyProgress) {
    unsubscribeDependencyProgress = window.paperSolverDesktop.onLocalDependencyProgress((payload = {}) => {
      localDependencyProgress.active = true
      localDependencyProgress.progress = Math.max(0, Math.min(100, Number(payload.progress) || 0))
      localDependencyProgress.message = payload.message || '正在处理本机依赖...'
      localDependencyProgress.stage = payload.stage || ''
    })
  }
  loadDesktopBackendConfig()
  if (isDesktopApp) {
    try {
      if (localStorage.getItem(DESKTOP_GUIDE_KEY) !== '1' && desktopSetupCompleted.value) {
        setTimeout(() => { desktopGuideOpen.value = true }, 650)
      }
    } catch {
      setTimeout(() => { desktopGuideOpen.value = true }, 650)
    }
  }
  if (route.query.qqSession) {
    try {
      const base64 = route.query.qqSession;
      const json = decodeBase64Utf8(base64);
      const user = JSON.parse(json);
      authStore.applySession(user);
      router.push(authStore.session.role === '管理员' ? '/admin' : '/library');
    } catch (e) {
      console.error("Failed to parse QQ login session:", e);
      errorText.value = "QQ 登录会话解析失败，请重试。";
      openModal('login');
    }
  } else if (route.query.error) {
    errorText.value = route.query.error;
    openModal('login');
  }

  if (route.query.auth === 'register' || route.query.show === 'register') openModal('register')
  else if (route.query.auth === 'login'  || route.query.show === 'login')  openModal('login')
})
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  io?.disconnect()
  clearTimeout(wfSlideTimer)
  clearInterval(wfProgressTimer)
  if (rcTimer) clearInterval(rcTimer)
  unsubscribeDependencyProgress?.()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Geist:wght@100;200;300;400;500;600;700;800;900&family=Geist+Mono:wght@400;500;600&display=swap');

.lite-mode-selector {
  margin: 10px 0 14px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: rgba(255, 255, 255, 0.03);
  padding: 10px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}
.lite-mode-label {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  cursor: pointer;
  font-size: 0.8rem;
  color: #94a3b8;
  transition: color 0.15s ease;
}
.lite-mode-label input {
  margin-top: 3px;
  cursor: pointer;
}
.lite-mode-label span {
  line-height: 1.3;
  text-align: left;
}
.lite-mode-label:hover {
  color: #f1f5f9;
}


*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0 }
.home-root {
  min-height: 100vh;
  background: #08080c;
  color: #e2e2e6;
  font-family: 'Geist', system-ui, -apple-system, sans-serif;
  -webkit-font-smoothing: antialiased;
  overflow-x: hidden;
  position: relative;
}
.home-root::before {
  content: "";
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  background: radial-gradient(350px circle at var(--mouse-x, -999px) var(--mouse-y, -999px), rgba(99, 102, 241, 0.11), rgba(168, 85, 247, 0.04), transparent 70%);
  z-index: 99;
}

/* ══ NAV ══ */
.home-nav {
  position: fixed; top: 0; left: 0; right: 0; z-index: 200;
  transition: background .3s, border-color .3s;
  border-bottom: 1px solid transparent;
}
.home-nav.scrolled { background: rgba(8,8,12,.84); backdrop-filter: blur(22px); -webkit-backdrop-filter: blur(22px); border-bottom-color: rgba(255,255,255,.055); }
.nav-inner { max-width: 100%; margin: 0 auto; padding: 0 48px; height: 62px; display: flex; align-items: center; gap: 24px; }
.brand { display: flex; align-items: center; gap: 10px; text-decoration: none; color: inherit; flex-shrink: 0; }
.brand-mark img {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), filter 0.3s ease;
  filter: drop-shadow(0 0 12px rgba(99, 102, 241, 0.35));
}
.brand:hover .brand-mark img {
  transform: scale(1.08) rotate(3deg);
  filter: drop-shadow(0 0 16px rgba(99, 102, 241, 0.7));
}
.brand-name {
  font-size: 1.15rem;
  font-weight: 800;
  background: linear-gradient(135deg, #ffffff 40%, #c7d2fe 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 8px rgba(99, 102, 241, 0.25));
  letter-spacing: -.025em;
  transition: opacity 0.2s ease;
}
.brand:hover .brand-name {
  opacity: 0.9;
}
.brand-tag  { font-size: .6rem; font-weight: 600; text-transform: uppercase; letter-spacing: .1em; color: #a5b4fc; border: 1px solid rgba(165, 180, 252, 0.25); background: rgba(99, 102, 241, 0.08); border-radius: 99px; padding: 2px 7px; box-shadow: 0 0 10px rgba(99, 102, 241, 0.15); }
.nav-links  { display: flex; gap: 2px; flex: 1; }
.nav-links a {
  color: #94a3b8;
  text-decoration: none;
  font-size: .875rem;
  font-weight: 500;
  padding: 5px 13px;
  border-radius: 99px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  text-shadow: 0 0 10px rgba(99, 102, 241, 0);
}
.nav-links a:hover {
  color: #ffffff;
  background: rgba(99, 102, 241, 0.12);
  box-shadow: 0 0 15px rgba(99, 102, 241, 0.3), inset 0 0 0 1px rgba(165, 180, 252, 0.25);
  text-shadow: 0 0 8px rgba(165, 180, 252, 0.6);
}
.nav-ctas { display: flex; align-items: center; gap: 8px; margin-left: auto; }
.btn-ghost { background: transparent; border: 1px solid rgba(255,255,255,.1); color: #a1a1aa; font-size: .875rem; font-weight: 500; padding: 7px 18px; border-radius: 8px; cursor: pointer; transition: border-color .2s, color .2s; font-family: inherit; }
.btn-ghost:hover { border-color: rgba(255,255,255,.22); color: #f0f0f4; }
.btn-solid { display: inline-flex; align-items: center; gap: 6px; background: #fff; color: #09090b; border: none; font-size: .875rem; font-weight: 650; padding: 7px 20px; border-radius: 8px; cursor: pointer; transition: background .2s, transform .15s; font-family: inherit; }
.btn-solid:hover { background: #e4e4e7; transform: translateY(-1px); }

/* ══ HERO — Longer, Softly Glowing Headline ══ */
.hero {
  position: relative;
  min-height: 100svh;
  display: flex; align-items: center; justify-content: center;
  padding: 120px 28px 80px;
  text-align: center;
  overflow: hidden;
}

.hero-bg-grid {
  position: absolute; inset: 0; z-index: 0; pointer-events: none;
  background-image:
    linear-gradient(rgba(255,255,255,.018) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,.018) 1px, transparent 1px);
  background-size: 80px 80px;
  mask-image: linear-gradient(180deg, transparent 0%, #000 12%, #000 72%, transparent 100%);
}
.hero-glow  { position: absolute; border-radius: 50%; pointer-events: none; z-index: 0; filter: blur(110px); }
.glow-1 { width: 750px; height: 750px; top: -200px; left: -200px; background: radial-gradient(circle,rgba(37,99,235,.22) 0%,transparent 70%);  animation: gf 9s ease-in-out infinite; }
.glow-2 { width: 650px; height: 650px; top:  100px; right: -160px; background: radial-gradient(circle,rgba(124,58,237,.18) 0%,transparent 70%); animation: gf 11s ease-in-out infinite reverse; }
.glow-3 { width: 550px; height: 550px; bottom: 0;  left:   25%;   background: radial-gradient(circle,rgba(5,150,105,.12) 0%,transparent 70%);  animation: gf 13s ease-in-out infinite; animation-delay:-4s; }
@keyframes gf { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-30px)} }

.hero-center {
  position: relative; z-index: 1;
  max-width: 900px; width: 100%;
  opacity: 0; transform: translateY(28px);
  transition: opacity .85s cubic-bezier(.22,1,.36,1), transform .85s cubic-bezier(.22,1,.36,1);
}
.hero-center.in { opacity: 1; transform: translateY(0); }

/* Pill */
.hero-pill {
  display: inline-flex; align-items: center; gap: 9px;
  border: 1px solid rgba(255,255,255,.09); background: rgba(255,255,255,.04);
  backdrop-filter: blur(10px); border-radius: 99px;
  padding: 7px 18px 7px 10px; font-size: .82rem; color: #a1a1aa;
  margin-bottom: 32px; cursor: default; transition: border-color .3s;
}
.hero-pill:hover { border-color: rgba(255,255,255,.18); }
.hero-pill svg   { color: #52525b; }
.pill-dot { width: 7px; height: 7px; border-radius: 50%; background: #22c55e; box-shadow: 0 0 8px #22c55e88; animation: blink 2.4s ease infinite; }
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:.3} }

/* ── Hero Headline: Long, Softly-lit text with unequal line lengths ── */
.hero-h1 {
  margin-bottom: 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  line-height: 1.12;
}

/* Line 1: Shorter line, softly muted white (淡淡半透明显示) */
.h1-line-top {
  font-size: clamp(2.4rem, 5.5vw, 3.8rem);
  font-weight: 750;
  letter-spacing: -.03em;
  color: rgba(255, 255, 255, 0.48);  /* 淡淡半透明显示 */
  white-space: nowrap;
}

/* Line 2: Longer line, soft gradient glow (淡淡高质感渐变) */
.h1-line-bot {
  font-size: clamp(2.8rem, 6.8vw, 4.8rem);
  font-weight: 850;
  letter-spacing: -.04em;
  background: linear-gradient(135deg, rgba(255,255,255,0.92) 0%, rgba(147,197,253,0.85) 50%, rgba(192,132,252,0.85) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
}

.hero-sub { font-size: 1.05rem; line-height: 1.75; color: #71717a; margin: 0 auto 38px; max-width: 580px; }

.hero-actions { display: flex; gap: 14px; justify-content: center; margin-bottom: 38px; flex-wrap: wrap; }
.cta-primary  { display: inline-flex; align-items: center; gap: 9px; background: #fff; color: #09090b; border: none; font-size: 1rem; font-weight: 700; padding: 14px 32px; border-radius: 99px; cursor: pointer; font-family: inherit; transition: background .2s, transform .2s, box-shadow .2s; }
.cta-primary:hover { background: #e4e4e7; transform: translateY(-2px); box-shadow: 0 12px 36px rgba(255,255,255,.08); }
.cta-primary:hover svg { transform: translateX(3px); }
.cta-primary svg { transition: transform .2s; }
.cta-outline  { display: inline-flex; align-items: center; background: transparent; border: 1px solid rgba(255,255,255,.12); color: #a1a1aa; font-size: 1rem; font-weight: 500; padding: 14px 32px; border-radius: 99px; cursor: pointer; font-family: inherit; transition: border-color .2s, color .2s, background .2s; }
.cta-outline:hover { border-color: rgba(255,255,255,.24); color: #d4d4d8; background: rgba(255,255,255,.03); }

.hero-proof { display: flex; align-items: center; justify-content: center; gap: 12px; font-size: .82rem; color: #52525b; margin-bottom: 32px; }
.proof-avatars { display: flex; }
.proof-avatars span { width: 28px; height: 28px; border-radius: 50%; border: 2px solid #08080c; display: block; margin-left: -7px; }
.proof-avatars span:first-child { margin-left: 0; }
.hero-proof strong { color: #a1a1aa; font-weight: 600; }

.hero-stats {
  display: flex;
  align-items: stretch;
  justify-content: center;
  gap: 20px;
  padding: 24px 32px;
  border: 1px solid rgba(165, 180, 252, 0.12);
  background: linear-gradient(180deg, rgba(30, 30, 45, 0.45), rgba(15, 15, 25, 0.65));
  border-radius: 20px;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  max-width: 1120px;
  margin: 0 auto;
}
.stat {
  flex: 1;
  text-align: left;
  padding: 8px 12px;
  transition: transform 0.3s ease, filter 0.3s ease;
}
.stat:hover {
  transform: translateY(-2px);
  filter: drop-shadow(0 0 12px rgba(99, 102, 241, 0.45));
}
.stat strong {
  display: block;
  font-size: 1.15rem;
  font-weight: 800;
  background: linear-gradient(135deg, #ffffff 40%, #c7d2fe 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 6px;
  letter-spacing: -0.02em;
}
.stat span {
  font-size: 0.78rem;
  color: #94a3b8;
  line-height: 1.55;
  font-weight: 400;
  display: block;
}
.stat-div {
  width: 1px;
  background: linear-gradient(180deg, transparent, rgba(165, 180, 252, 0.18) 50%, transparent);
}

/* ══ SECTIONS ══ */
.section { padding: 88px 28px; }
.section-alt { background: rgba(255,255,255,.013); }
.section-wrap { max-width: 100%; margin: 0 auto; }
.s-head { text-align: center; margin-bottom: 48px; opacity: 0; transform: translateY(20px); transition: opacity .65s ease, transform .65s ease; }
.s-head.in { opacity: 1; transform: translateY(0); }
.eyebrow { display: inline-flex; align-items: center; gap: 8px; font-size: .68rem; font-weight: 600; text-transform: uppercase; letter-spacing: .2em; color: #3f3f46; border: 1px solid rgba(255,255,255,.06); background: rgba(255,255,255,.025); padding: 5px 14px; border-radius: 99px; margin-bottom: 20px; }
.ew-dot  { width: 6px; height: 6px; border-radius: 50%; background: #22c55e; }
.s-head h2 { font-size: clamp(1.65rem, 3.4vw, 2.5rem); font-weight: 750; color: #f0f0f4; letter-spacing: -.03em; line-height: 1.2; margin-bottom: 14px; }
.s-head p  { font-size: .95rem; color: #52525b; max-width: 500px; margin: 0 auto; line-height: 1.75; }

/* ══ FEATURES ══ */
.feat-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 14px; opacity: 0; transform: translateY(18px); transition: opacity .65s ease .1s, transform .65s ease .1s; }
.feat-grid.in { opacity: 1; transform: translateY(0); }
.feat-card { background: rgba(14,14,20,.75); border: 1px solid rgba(255,255,255,.055); border-radius: 16px; padding: 22px; display: flex; gap: 16px; align-items: flex-start; position: relative; overflow: hidden; transition: border-color .25s, transform .25s, box-shadow .25s; animation: fcIn .5s ease calc(var(--delay,0s)) both; }
@keyframes fcIn { from{opacity:0;transform:translateY(12px)} to{opacity:1;transform:none} }
.feat-card:hover { border-color: rgba(255,255,255,.1); transform: translateY(-3px); box-shadow: 0 16px 48px rgba(0,0,0,.28); }
.fc-icon { width: 42px; height: 42px; border-radius: 11px; flex-shrink: 0; background: color-mix(in srgb,var(--c) 10%,transparent); border: 1px solid color-mix(in srgb,var(--c) 20%,transparent); display: flex; align-items: center; justify-content: center; color: var(--c); }
.fc-icon svg { width: 19px; height: 19px; }
.fc-body h3 { font-size: .9rem; font-weight: 650; color: #e2e2e6; margin-bottom: 7px; }
.fc-body p  { font-size: .8rem; color: #52525b; line-height: 1.65; margin-bottom: 12px; }
.fc-chips { display: flex; gap: 5px; flex-wrap: wrap; }
.fc-chips span { font-size: .65rem; font-weight: 500; padding: 2px 8px; border-radius: 99px; color: var(--c); border: 1px solid color-mix(in srgb,var(--c) 20%,transparent); background: color-mix(in srgb,var(--c) 7%,transparent); }

/* ══ WORKFLOW — FULL PAGE UNBOUNDED CAROUSEL (NO BORDER BOX) ══ */
.workflow-section-full {
  padding: 88px 0;
  background: rgba(255,255,255,.013);
  overflow: hidden;
}

.full-carousel-viewport {
  position: relative;
  width: 100%;
  margin-top: 40px;
}

.full-carousel-track {
  display: flex;
  width: 100%;
  transition: transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

.full-slide-item {
  min-width: 100vw;
  width: 100vw;
  flex-shrink: 0;
  display: grid;
  grid-template-columns: 60% 40%;
  align-items: center;
  padding: 0 max(28px, calc((100vw - 1160px) / 2));
  box-sizing: border-box;
}

/* Left Side: Unbounded Page Image */
.full-slide-media {
  padding-right: 48px;
}
.media-container {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 30px 100px rgba(0, 0, 0, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.07);
  cursor: zoom-in;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.media-container:hover {
  transform: scale(1.01);
  box-shadow: 0 35px 110px rgba(99, 102, 241, 0.2);
}
.media-container img {
  width: 100%;
  height: 100%;
  max-height: 540px;
  object-fit: cover;
  object-position: top left;
  display: block;
}
.media-fade-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to right, transparent 75%, rgba(8, 8, 12, 0.95) 100%);
  pointer-events: none;
}
.media-step-badge {
  position: absolute;
  top: 20px; left: 20px;
  font-family: 'Geist Mono', monospace;
  font-size: .78rem; font-weight: 600;
  color: rgba(255,255,255,.45);
  background: rgba(0,0,0,.6);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,.1);
  border-radius: 8px; padding: 6px 12px;
}

/* Right Side: Staggered Entrance Animations for Info Copy */
.full-slide-info {
  display: flex;
  align-items: center;
  padding-left: 12px;
}
.info-content-box {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 460px;
}

/* Entrance Animation Base States */
.full-slide-item .info-meta,
.full-slide-item .info-title,
.full-slide-item .info-desc,
.full-slide-item .info-hl-row,
.full-slide-item .info-tags {
  opacity: 0;
  transform: translateY(22px);
  transition: opacity 0.55s cubic-bezier(0.22, 1, 0.36, 1), transform 0.55s cubic-bezier(0.22, 1, 0.36, 1);
}

/* Slide Active Entrance Staggered Delays */
.full-slide-item.active .info-meta {
  opacity: 1; transform: translateY(0); transition-delay: 0.12s;
}
.full-slide-item.active .info-title {
  opacity: 1; transform: translateY(0); transition-delay: 0.20s;
}
.full-slide-item.active .info-desc {
  opacity: 1; transform: translateY(0); transition-delay: 0.28s;
}
.full-slide-item.active .info-hl-row:nth-child(1) {
  opacity: 1; transform: translateY(0); transition-delay: 0.36s;
}
.full-slide-item.active .info-hl-row:nth-child(2) {
  opacity: 1; transform: translateY(0); transition-delay: 0.44s;
}
.full-slide-item.active .info-hl-row:nth-child(3) {
  opacity: 1; transform: translateY(0); transition-delay: 0.52s;
}
.full-slide-item.active .info-tags {
  opacity: 1; transform: translateY(0); transition-delay: 0.60s;
}

/* Text & Highlight Styling */
.info-meta {
  display: flex; align-items: center; gap: 8px;
  font-family: 'Geist Mono', monospace; font-size: .78rem;
}
.info-counter { color: #60a5fa; font-weight: 700; background: rgba(59,130,246,.12); padding: 3px 10px; border-radius: 99px; }
.info-label   { color: #52525b; text-transform: uppercase; letter-spacing: .1em; font-size: .7rem; }

.info-title { font-size: 1.85rem; font-weight: 750; color: #f0f0f4; letter-spacing: -.03em; line-height: 1.25; }
.info-desc  { font-size: .92rem; color: #80808a; line-height: 1.7; }

/* Detailed Highlights Bullet List */
.info-highlights-list {
  display: flex;
  flex-direction: column;
  gap: 11px;
  margin: 6px 0;
  background: rgba(255,255,255,0.02);
  border-left: 3px solid #3b82f6;
  padding: 14px 18px;
  border-radius: 0 12px 12px 0;
}
.info-hl-row { display: flex; align-items: flex-start; gap: 9px; font-size: .86rem; color: #d4d4d8; line-height: 1.5; }
.info-hl-icon { color: #60a5fa; font-size: .8rem; margin-top: 1px; flex-shrink: 0; }
.info-hl-text :deep(b) { color: #93c5fd; font-weight: 600; background: rgba(59,130,246,0.12); padding: 1px 6px; border-radius: 4px; }

.info-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.info-tag-item { font-size: .72rem; font-weight: 500; padding: 4px 12px; border-radius: 99px; color: #a1a1aa; border: 1px solid rgba(255,255,255,.08); background: rgba(255,255,255,.025); }

/* Floating Carousel Control Bar */
.full-carousel-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-top: 48px;
}
.fc-arrow-btn {
  width: 42px; height: 42px; border-radius: 99px;
  background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.1);
  color: #a1a1aa; display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: background .2s, color .2s; font-family: inherit;
}
.fc-arrow-btn:hover { background: rgba(255,255,255,.12); color: #f0f0f4; }
.fc-dots-wrap { display: flex; gap: 8px; }
.fc-dot-item { width: 7px; height: 7px; border-radius: 99px; background: rgba(255,255,255,.14); cursor: pointer; transition: background .25s, width .3s cubic-bezier(.22,1,.36,1); }
.fc-dot-item.active { background: #3b82f6; width: 28px; }
.fc-progress-indicator { width: 34px; height: 34px; position: relative; }
.fc-progress-indicator svg { width: 100%; height: 100%; }
.fc-progress-indicator circle:last-child { transition: stroke-dashoffset .08s linear; }

/* ══ WHY CHOOSE US ══ */
.why-danmaku-container { position: relative; width: 100%; margin-top: 50px; display: flex; flex-direction: column; gap: 20px; overflow: hidden; padding: 20px 0; mask-image: linear-gradient(to right, transparent 0%, #000 8%, #000 92%, transparent 100%); -webkit-mask-image: linear-gradient(to right, transparent 0%, #000 8%, #000 92%, transparent 100%); opacity: 0; transform: translateY(20px); transition: opacity 0.8s cubic-bezier(0.25,1,0.5,1), transform 0.8s cubic-bezier(0.25,1,0.5,1); }
.why-danmaku-container.in { opacity: 1; transform: translateY(0); }
.danmaku-track { display: flex; width: max-content; gap: 24px; user-select: none; }
.danmaku-content { display: flex; gap: 24px; width: max-content; }
.track-left .danmaku-content { animation: marquee-left 85s linear infinite; }
.track-left-slow .danmaku-content { animation: marquee-left 105s linear infinite; }
.track-right .danmaku-content { animation: marquee-right 95s linear infinite; }
.danmaku-track:hover .danmaku-content { animation-play-state: paused; }
.danmaku-pill { display: flex; align-items: center; gap: 12px; background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.05); border-radius: 99px; padding: 14px 28px; white-space: nowrap; transition: all 0.3s ease; cursor: pointer; box-shadow: 0 4px 20px rgba(0,0,0,0.15); }
.danmaku-pill:hover { border-color: var(--c); background: rgba(255,255,255,0.04); transform: scale(1.03); }
.dm-icon { display: flex; align-items: center; justify-content: center; color: var(--c); flex-shrink: 0; }
.dm-icon svg { width: 16px; height: 16px; }
.dm-pain { font-size: 0.95rem; font-weight: 600; color: #e4e4e7; }
.dm-arrow { font-size: 0.85rem; color: rgba(255,255,255,0.2); margin: 0 4px; }
.dm-solution { font-size: 0.95rem; font-weight: 700; color: var(--c); }
@keyframes marquee-left {
  0% { transform: translateX(0); }
  100% { transform: translateX(-100%); }
}
@keyframes marquee-right {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(0); }
}

.why-banner-premium { position: relative; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; background: linear-gradient(135deg, rgba(37,99,235,0.08) 0%, rgba(124,58,237,0.05) 50%, rgba(0,0,0,0) 100%); border: 1px solid rgba(124,58,237,0.15); border-radius: 24px; padding: 60px 48px; opacity: 0; transform: translateY(16px); transition: opacity 0.8s cubic-bezier(0.25,1,0.5,1), transform 0.8s cubic-bezier(0.25,1,0.5,1); overflow: hidden; box-shadow: 0 20px 40px rgba(0,0,0,0.2); }
.why-banner-premium.in { opacity: 1; transform: translateY(0); }
.wbp-content { position: relative; z-index: 2; max-width: 720px; }
.wbp-text { font-size: 1.8rem; font-weight: 700; line-height: 1.6; letter-spacing: -0.01em; background: linear-gradient(135deg, #ffffff 40%, #a5b4fc 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 20px; }
.wbp-by { font-size: 0.95rem; font-weight: 600; color: #818cf8; letter-spacing: 0.05em; text-transform: uppercase; }
.wbp-quote-mark-start, .wbp-quote-mark-end { position: absolute; font-family: Georgia, serif; font-size: 10rem; line-height: 1; font-weight: 900; color: rgba(124, 58, 237, 0.12); user-select: none; z-index: 1; }
.wbp-quote-mark-start { top: -10px; left: 30px; }
.wbp-quote-mark-end { bottom: -60px; right: 30px; }

/* ══ COMPARISON ══ */
.cmp-table { border: 1px solid rgba(255,255,255,.07); border-radius: 18px; overflow: hidden; opacity: 0; transform: translateY(18px); transition: opacity .65s ease .1s, transform .65s ease .1s; }
.cmp-table.in { opacity: 1; transform: translateY(0); }
.cmp-header { display: grid; grid-template-columns: 1.2fr 1.3fr 1.3fr 1.8fr; padding: 14px 24px; background: rgba(255,255,255,.022); border-bottom: 1px solid rgba(255,255,255,.055); font-size: .95rem; font-weight: 600; text-transform: uppercase; letter-spacing: .08em; align-items: center; color: #38bdf8; }
.cmp-header span { display: flex; align-items: center; justify-content: flex-start; text-align: left; }
.cmp-bad-h  { display: flex; align-items: center; justify-content: flex-start; gap: 6px; color: #f87171; }
.cmp-good-h { display: flex; align-items: center; justify-content: flex-start; gap: 6px; color: #38bdf8; }
.cmp-row { display: grid; grid-template-columns: 1.2fr 1.3fr 1.3fr 1.8fr; padding: 22px 24px; border-bottom: 1px solid rgba(255,255,255,.038); align-items: start; transition: background .2s; }
.cmp-row:last-child { border-bottom: none; }
.cmp-row:hover { background: rgba(255,255,255,.014); }
.cmp-row b { font-size: 1.15rem; font-weight: 700; color: #f4f4f5; }
.cmp-bad, .cmp-good { display: flex; gap: 10px; font-size: 1.05rem; line-height: 1.7; align-items: flex-start; justify-content: flex-start; text-align: left; padding-right: 12px; }
.cmp-bad  { color: #d1d5db; }
.cmp-good { color: #a7f3d0; }
.ci { display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-top: 5px; opacity: 0; transform: translateY(12px); transition: opacity 0.8s cubic-bezier(0.25, 1, 0.5, 1), transform 0.8s cubic-bezier(0.25, 1, 0.5, 1); }
.ci.bad  { background: transparent; color: #f87171; border: none; }
.ci.good { background: transparent; color: #4ade80; border: none; }
.cmp-table.in .cmp-row .ci { opacity: 1; transform: translateY(0); }
.cmp-row:hover .ci { transform: scale(1.12); }
.cmp-table.in .cmp-row:nth-child(2) .ci { transition-delay: 0.1s; }
.cmp-table.in .cmp-row:nth-child(3) .ci { transition-delay: 0.22s; }
.cmp-table.in .cmp-row:nth-child(4) .ci { transition-delay: 0.34s; }
.cmp-table.in .cmp-row:nth-child(5) .ci { transition-delay: 0.46s; }
.cmp-table.in .cmp-row:nth-child(6) .ci { transition-delay: 0.58s; }
.cmp-table.in .cmp-row:nth-child(7) .ci { transition-delay: 0.70s; }

/* ══ FOOTER ══ */
.home-footer { border-top: 1px solid rgba(255,255,255,.05); padding: 44px 28px; }
.footer-inner { max-width: 100%; margin: 0 auto; padding: 0 48px; display: flex; flex-direction: column; align-items: center; gap: 20px; text-align: center; }
.footer-logo  { font-size: 1rem; font-weight: 700; color: #f0f0f4; }
.footer-links { display: flex; gap: 24px; flex-wrap: wrap; justify-content: center; }
.footer-links a { font-size: .82rem; color: #52525b; text-decoration: none; transition: color .2s; }
.footer-links a:hover { color: #a1a1aa; }
.footer-copy  { font-size: .75rem; color: #27272a; }

/* ══ MODAL ══ */
.mfade-enter-active,.mfade-leave-active{transition:opacity .22s ease}
.mfade-enter-from,.mfade-leave-to{opacity:0}
.modal-mask { position: fixed; inset: 0; z-index: 500; background: rgba(0,0,0,.72); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: center; padding: 24px; }
.modal-box  { background: rgba(12,12,18,.96); border: 1px solid rgba(255,255,255,.1); border-radius: 20px; padding: 36px; width: 100%; max-width: 410px; position: relative; box-shadow: 0 48px 120px rgba(0,0,0,.65); }
.modal-x    { position: absolute; top: 14px; right: 14px; width: 30px; height: 30px; background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.08); border-radius: 7px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #71717a; transition: color .2s, background .2s; font-family: inherit; }
.modal-x:hover { color: #e2e2e6; background: rgba(255,255,255,.08); }
.auth-label { font-size: .62rem; font-weight: 700; letter-spacing: .18em; text-transform: uppercase; color: #2563eb; margin-bottom: 8px; }
.auth-title { font-size: 1.45rem; font-weight: 750; color: #f0f0f4; margin-bottom: 26px; letter-spacing: -.025em; }
.auth-form  { display: flex; flex-direction: column; gap: 12px; }
.scroll-form { max-height: 62vh; overflow-y: auto; padding-right: 4px; }
.scroll-form::-webkit-scrollbar { width: 3px; }
.scroll-form::-webkit-scrollbar-thumb { background: rgba(255,255,255,.1); border-radius: 99px; }
.auth-form label { font-size: .78rem; font-weight: 600; color: #71717a; margin-bottom: -4px; }
.auth-form input { background: rgba(255,255,255,.04); border: 1px solid rgba(255,255,255,.08); border-radius: 10px; color: #f0f0f4; font-size: .9rem; padding: 11px 14px; width: 100%; font-family: inherit; outline: none; transition: border-color .2s, background .2s; }
.auth-form input:focus { border-color: rgba(59,130,246,.5); background: rgba(59,130,246,.04); }
.auth-form input::placeholder { color: #3f3f46; }
.pw-wrap { position: relative; }
.pw-wrap input { padding-right: 42px; }
.pw-eye { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); background: transparent; border: none; color: #52525b; cursor: pointer; padding: 0; display: flex; align-items: center; transition: color .2s; }
.pw-eye:hover { color: #a1a1aa; }
.row-input { display: flex; gap: 8px; }
.row-input input { flex: 1; min-width: 0; }
.code-btn { background: rgba(59,130,246,.1); border: 1px solid rgba(59,130,246,.22); color: #60a5fa; font-size: .76rem; font-weight: 600; padding: 0 13px; border-radius: 99px; cursor: pointer; white-space: nowrap; font-family: inherit; transition: background .2s; }
.code-btn:hover:not(:disabled) { background: rgba(59,130,246,.18); }
.code-btn:disabled { opacity: .45; cursor: not-allowed; }
.opt { color: #3f3f46; font-weight: 400; }
.role-group { display: flex; background: rgba(255,255,255,.03); border: 1px solid rgba(255,255,255,.07); border-radius: 10px; padding: 3px; gap: 3px; }
.role-group button { flex: 1; background: transparent; border: none; color: #52525b; font-size: .82rem; font-weight: 500; padding: 7px; border-radius: 7px; cursor: pointer; font-family: inherit; transition: all .2s; }
.role-group button.on { background: rgba(255,255,255,.07); color: #f0f0f4; }
.auth-err { font-size: .8rem; color: #f87171; background: rgba(239,68,68,.08); border: 1px solid rgba(239,68,68,.14); padding: 9px 13px; border-radius: 8px; }
.auth-ok  { font-size: .8rem; color: #86efac; background: rgba(34,197,94,.07); border: 1px solid rgba(34,197,94,.14); padding: 9px 13px; border-radius: 8px; }
.auth-submit { background: #fff; color: #09090b; border: none; font-size: .93rem; font-weight: 700; padding: 13px; border-radius: 10px; cursor: pointer; font-family: inherit; margin-top: 2px; transition: background .2s, transform .15s; }
.auth-submit:hover:not(:disabled) { background: #e4e4e7; transform: translateY(-1px); }
.auth-submit:disabled { opacity: .45; cursor: not-allowed; }
.auth-links { display: flex; justify-content: space-between; }
.auth-links a { font-size: .78rem; color: #71717a; text-decoration: none; transition: color .2s; }
.auth-links a:hover { color: #a1a1aa; }
.auth-links a.dim { color: #3f3f46; }
.oauth-only-pane { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 15px 10px 10px; width: 100%; box-sizing: border-box; }
.oauth-brand-header { display: flex; flex-direction: column; align-items: center; margin-bottom: 22px; text-align: center; }
.oauth-logo-glow { display: flex; align-items: center; justify-content: center; width: 56px; height: 56px; border-radius: 18px; background: rgba(124, 58, 237, 0.03); border: 1px solid rgba(124, 58, 237, 0.15); box-shadow: 0 0 20px rgba(37, 99, 235, 0.08); margin-bottom: 12px; transition: all 0.3s ease; }
.oauth-logo-glow:hover { transform: translateY(-2px); box-shadow: 0 0 25px rgba(124, 58, 237, 0.2); background: rgba(124, 58, 237, 0.06); }
.oauth-brand-title { font-size: 1.5rem; font-weight: 800; background: linear-gradient(135deg, #ffffff 30%, #a1a1aa 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin: 0 0 4px; letter-spacing: -0.02em; }
.oauth-brand-subtitle { font-size: 0.78rem; color: #71717a; margin: 0; font-weight: 500; letter-spacing: 0.05em; }
.oauth-card { width: 100%; padding: 20px; border-radius: 16px; background: rgba(255, 255, 255, 0.015); border: 1px solid rgba(255, 255, 255, 0.05); box-sizing: border-box; }
.oauth-hint { font-size: 0.8rem; color: #a1a1aa; line-height: 1.6; text-align: center; margin: 0 0 20px; }
.btn-oauth-premium { display: flex; align-items: center; justify-content: center; gap: 10px; width: 100%; padding: 14px; background: linear-gradient(135deg, #128eed 0%, #0066cc 100%); border: none; border-radius: 12px; color: #ffffff; font-size: 0.95rem; font-weight: 700; cursor: pointer; transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease; box-shadow: 0 4px 15px rgba(18, 142, 237, 0.25); }
.btn-oauth-premium:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(18, 142, 237, 0.4); filter: brightness(1.08); }
.btn-oauth-premium:active { transform: translateY(1px); box-shadow: 0 2px 10px rgba(18, 142, 237, 0.2); }
.btn-oauth-premium.qq {
  background: rgba(18, 142, 237, 0.12) !important;
  border: 1px solid rgba(18, 142, 237, 0.35) !important;
  color: #7ad0ff !important;
  box-shadow: 0 4px 15px rgba(18, 142, 237, 0.15) !important;
  transition: all 0.2s ease;
  padding: 12px;
  border-radius: 8px;
}
.btn-oauth-premium.qq:hover {
  background: rgba(18, 142, 237, 0.22) !important;
  border: 1px solid rgba(18, 142, 237, 0.5) !important;
  color: #a3e0ff !important;
  box-shadow: 0 6px 20px rgba(18, 142, 237, 0.3) !important;
  transform: translateY(-1px);
}
.btn-oauth-premium.qq:active {
  transform: translateY(1px);
}
.icon-qq-svg { color: #ffffff; }
.oauth-footer-agreement { font-size: 0.72rem; color: #52525b; text-align: center; margin-top: 18px; }
.oauth-footer-agreement a { color: #71717a; text-decoration: none; border-bottom: 1px dotted #52525b; transition: all 0.2s; }
.oauth-footer-agreement a:hover { color: #a1a1aa; border-bottom-color: #a1a1aa; }


.oauth-desktop-card { margin-top: 15px; width: 100%; border: 1px solid rgba(255, 255, 255, 0.03) !important; background: rgba(255, 255, 255, 0.01) !important; }
.desktop-connection-card {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 6px 12px;
  align-items: center;
  margin-top: 4px;
  padding: 10px 12px;
  border: 1px solid rgba(96, 165, 250, .14);
  border-radius: 12px;
  background: rgba(37, 99, 235, .08);
}
.desktop-connection-card span {
  color: #818cf8;
  font-size: .68rem;
  font-weight: 800;
  letter-spacing: .08em;
  text-transform: uppercase;
}
.desktop-connection-card strong {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  overflow: hidden;
  color: #c7d2fe;
  font-size: .75rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.desktop-connection-card strong i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f87171;
  box-shadow: 0 0 0 4px rgba(248, 113, 113, .12);
}
.desktop-connection-card strong.ok {
  color: #bbf7d0;
}
.desktop-connection-card strong.ok i {
  background: #22c55e;
  box-shadow: 0 0 0 4px rgba(34, 197, 94, .14);
}
.desktop-connection-card strong.bad {
  color: #fecaca;
}
.desktop-connection-card button {
  grid-row: 1 / span 2;
  grid-column: 2;
  border: 1px solid rgba(129, 140, 248, .24);
  border-radius: 9px;
  padding: 7px 10px;
  color: #e0e7ff;
  background: rgba(129, 140, 248, .14);
  cursor: pointer;
  font-family: inherit;
  font-size: .72rem;
  font-weight: 800;
}
.desktop-connection-card button:hover {
  border-color: rgba(129, 140, 248, .42);
  background: rgba(129, 140, 248, .22);
}
.desktop-settings-mask {
  z-index: 560;
  align-items: center;
  padding: 28px;
}
.desktop-settings-box {
  max-width: 680px;
  max-height: calc(100vh - 72px);
  overflow: hidden;
  padding: 0;
}
.desktop-settings-pane {
  max-height: calc(100vh - 72px);
  overflow-y: auto;
  padding: 30px;
}
.desktop-settings-pane::-webkit-scrollbar {
  width: 8px;
}
.desktop-settings-pane::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, .35);
}
.desktop-settings-pane::-webkit-scrollbar-thumb {
  background: rgba(96, 165, 250, .28);
  border-radius: 99px;
  border: 2px solid rgba(15, 23, 42, .65);
}
.desktop-settings-desc {
  margin: -8px 0 16px;
  color: #71717a;
  font-size: .82rem;
  line-height: 1.65;
}
.desktop-setup-status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.desktop-setup-status-card {
  position: relative;
  display: grid;
  gap: 4px;
  min-height: 74px;
  padding: 14px 14px 13px 36px;
  border: 1px solid rgba(248, 113, 113, .18);
  border-radius: 16px;
  background: rgba(127, 29, 29, .12);
}
.desktop-setup-status-card .status-dot {
  position: absolute;
  left: 15px;
  top: 18px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #f87171;
  box-shadow: 0 0 0 5px rgba(248, 113, 113, .12);
}
.desktop-setup-status-card strong {
  color: #f8fafc;
  font-size: .86rem;
  font-weight: 850;
}
.desktop-setup-status-card small {
  color: #fca5a5;
  font-size: .72rem;
  font-weight: 800;
}
.desktop-setup-status-card.ok {
  border-color: rgba(34, 197, 94, .2);
  background: rgba(6, 78, 59, .16);
}
.desktop-setup-status-card.ok .status-dot {
  background: #22c55e;
  box-shadow: 0 0 0 5px rgba(34, 197, 94, .13);
}
.desktop-setup-status-card.ok small {
  color: #86efac;
}
.desktop-settings-actions {
  display: grid;
  grid-template-columns: .9fr 1.1fr;
  gap: 10px;
}
.desktop-settings-section {
  display: grid;
  gap: 10px;
  margin: 4px 0 2px;
  padding: 14px;
  border: 1px solid rgba(129, 140, 248, .16);
  border-radius: 14px;
  background:
    linear-gradient(135deg, rgba(59, 130, 246, .08), rgba(14, 165, 233, .035)),
    rgba(255, 255, 255, .025);
}
.desktop-settings-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.desktop-settings-section-head span {
  color: #e5e7eb;
  font-size: .9rem;
  font-weight: 850;
}
.desktop-settings-section-head small {
  color: #93c5fd;
  font-size: .72rem;
  font-weight: 800;
}
.desktop-settings-section p {
  margin: 0;
  color: #71717a;
  font-size: .76rem;
  line-height: 1.6;
}
.desktop-update-section {
  border-color: rgba(99, 102, 241, .24);
  background:
    linear-gradient(135deg, rgba(79, 70, 229, .14), rgba(14, 165, 233, .05)),
    rgba(15, 23, 42, .26);
}
.desktop-update-section.compact {
  gap: 8px;
  padding: 13px 15px;
}
.desktop-update-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.desktop-update-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: .72rem;
  font-weight: 900;
}
.desktop-update-badge.stable {
  color: #bfdbfe;
  background: rgba(37, 99, 235, .16);
  border: 1px solid rgba(96, 165, 250, .24);
}
.desktop-update-badge.available {
  color: #fef3c7;
  background: rgba(245, 158, 11, .16);
  border: 1px solid rgba(245, 158, 11, .28);
}
.desktop-update-row button {
  min-height: 34px;
  padding: 0 13px;
  border: 1px solid rgba(96, 165, 250, .28);
  border-radius: 10px;
  color: #dbeafe;
  background: rgba(37, 99, 235, .12);
  cursor: pointer;
  font-family: inherit;
  font-size: .76rem;
  font-weight: 850;
}
.desktop-update-row button:hover:not(:disabled) {
  border-color: rgba(96, 165, 250, .5);
  background: rgba(37, 99, 235, .22);
}
.desktop-update-row button:disabled {
  opacity: .55;
  cursor: not-allowed;
}
.desktop-dependency-section {
  border-color: rgba(56, 189, 248, .22);
  background:
    linear-gradient(135deg, rgba(37, 99, 235, .11), rgba(14, 165, 233, .07)),
    rgba(15, 23, 42, .36);
}
.desktop-progress {
  display: grid;
  gap: 6px;
}
.desktop-progress-bar {
  height: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(148, 163, 184, .16);
}
.desktop-progress-bar span {
  display: block;
  height: 100%;
  width: 0;
  border-radius: inherit;
  background: linear-gradient(90deg, #6366f1, #06b6d4);
  transition: width .22s ease;
}
.desktop-progress small {
  color: #93c5fd;
  font-size: .72rem;
  font-weight: 700;
}
.desktop-dependency-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.desktop-dependency-actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 38px;
  border: 1px solid rgba(96, 165, 250, .24);
  border-radius: 11px;
  color: #dbeafe;
  background: rgba(37, 99, 235, .12);
  cursor: pointer;
  font-family: inherit;
  font-size: .77rem;
  font-weight: 850;
}
.desktop-dependency-actions button:hover:not(:disabled) {
  border-color: rgba(34, 211, 238, .44);
  background: rgba(14, 165, 233, .18);
}
.desktop-dependency-actions button:disabled {
  opacity: .48;
  cursor: not-allowed;
}
.mini-spinner {
  width: 13px;
  height: 13px;
  border-radius: 50%;
  border: 2px solid rgba(191, 219, 254, .24);
  border-top-color: #bfdbfe;
  animation: miniSpin .72s linear infinite;
}
@keyframes miniSpin {
  to { transform: rotate(360deg); }
}
.desktop-translation-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 76px;
  gap: 8px;
}
.desktop-pdf-dir-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 92px;
  gap: 8px;
}
.desktop-pdf-dir-row input {
  font-family: 'Geist Mono', ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: .75rem;
}
.desktop-pdf-dir-row button {
  border: 1px solid rgba(20, 184, 166, .28);
  border-radius: 10px;
  color: #ccfbf1;
  background: rgba(20, 184, 166, .14);
  cursor: pointer;
  font-family: inherit;
  font-size: .78rem;
  font-weight: 850;
}
.desktop-pdf-dir-row button:hover:not(:disabled) {
  border-color: rgba(20, 184, 166, .5);
  background: rgba(20, 184, 166, .22);
}
.desktop-pdf-dir-row button:disabled {
  opacity: .55;
  cursor: not-allowed;
}
.desktop-translation-row button {
  border: 1px solid rgba(96, 165, 250, .2);
  border-radius: 10px;
  color: #dbeafe;
  background: rgba(37, 99, 235, .12);
  cursor: pointer;
  font-family: inherit;
  font-size: .78rem;
  font-weight: 800;
}
.desktop-translation-row button:hover:not(:disabled) {
  border-color: rgba(96, 165, 250, .42);
  background: rgba(37, 99, 235, .22);
}
.desktop-translation-row button:disabled {
  opacity: .55;
  cursor: not-allowed;
}
.desktop-translation-status {
  margin-top: -4px;
  border-radius: 9px;
  padding: 7px 9px;
  font-size: .74rem;
  line-height: 1.45;
}
.desktop-translation-status.ok {
  color: #bbf7d0;
  background: rgba(34, 197, 94, .1);
  border: 1px solid rgba(34, 197, 94, .18);
}
.desktop-translation-status.bad {
  color: #fecaca;
  background: rgba(239, 68, 68, .1);
  border: 1px solid rgba(239, 68, 68, .18);
}
.desktop-cache-section p {
  color: #94a3b8;
}
.desktop-cache-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.desktop-cache-actions button {
  border: 1px solid rgba(96, 165, 250, .18);
  border-radius: 10px;
  padding: 10px 8px;
  color: #dbeafe;
  background: rgba(37, 99, 235, .1);
  cursor: pointer;
  font-family: inherit;
  font-size: .78rem;
  font-weight: 800;
}
.desktop-cache-actions button:hover:not(:disabled) {
  border-color: rgba(96, 165, 250, .38);
  background: rgba(37, 99, 235, .18);
}
.desktop-cache-actions button.danger {
  color: #fecaca;
  border-color: rgba(248, 113, 113, .2);
  background: rgba(239, 68, 68, .09);
}
.desktop-cache-actions button.danger:hover:not(:disabled) {
  border-color: rgba(248, 113, 113, .42);
  background: rgba(239, 68, 68, .15);
}
.desktop-cache-actions button:disabled {
  opacity: .55;
  cursor: not-allowed;
}
.desktop-settings-actions .auth-submit {
  margin-top: 0;
}
.desktop-test-btn {
  border: 1px solid rgba(96, 165, 250, .22);
  border-radius: 10px;
  padding: 12px;
  color: #bfdbfe;
  background: rgba(37, 99, 235, .12);
  cursor: pointer;
  font-family: inherit;
  font-size: .86rem;
  font-weight: 800;
}
.desktop-test-btn:hover:not(:disabled) {
  border-color: rgba(96, 165, 250, .42);
  color: #eff6ff;
  background: rgba(37, 99, 235, .2);
}
.desktop-test-btn:disabled {
  opacity: .5;
  cursor: not-allowed;
}
.desktop-reset-btn {
  border: 1px solid rgba(255,255,255,.1);
  border-radius: 10px;
  padding: 12px;
  color: #a1a1aa;
  background: rgba(255,255,255,.03);
  cursor: pointer;
  font-family: inherit;
  font-size: .86rem;
  font-weight: 700;
}
.desktop-reset-btn:hover:not(:disabled) {
  border-color: rgba(255,255,255,.2);
  color: #e4e4e7;
}
.desktop-reset-btn:disabled {
  opacity: .5;
  cursor: not-allowed;
}
.desktop-guide-mask {
  z-index: 555;
}
.desktop-guide-box {
  max-width: 760px;
  padding: 30px;
  background:
    radial-gradient(circle at 12% 10%, rgba(59, 130, 246, .18), transparent 32%),
    linear-gradient(135deg, rgba(15, 23, 42, .98), rgba(9, 9, 15, .98));
  border-color: rgba(129, 140, 248, .2);
}
.desktop-guide-kicker {
  color: #60a5fa;
  font-family: 'Geist Mono', monospace;
  font-size: .72rem;
  font-weight: 900;
  letter-spacing: .12em;
}
.desktop-guide-box h3 {
  margin: 8px 0 8px;
  color: #f8fafc;
  font-size: 1.45rem;
  font-weight: 850;
}
.desktop-guide-desc {
  margin: 0 0 18px;
  color: #94a3b8;
  font-size: .9rem;
  line-height: 1.7;
}
.desktop-guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.desktop-guide-card {
  min-height: 168px;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, .14);
  border-radius: 16px;
  background: rgba(15, 23, 42, .66);
}
.desktop-guide-card span {
  display: inline-flex;
  color: #38bdf8;
  font-family: 'Geist Mono', monospace;
  font-size: .72rem;
  font-weight: 900;
  margin-bottom: 18px;
}
.desktop-guide-card strong {
  display: block;
  color: #e2e8f0;
  font-size: .95rem;
  font-weight: 850;
  margin-bottom: 8px;
}
.desktop-guide-card p {
  margin: 0;
  color: #94a3b8;
  font-size: .8rem;
  line-height: 1.65;
}
.desktop-guide-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
.desktop-guide-actions .desktop-test-btn,
.desktop-guide-actions .auth-submit {
  width: auto;
  min-width: 132px;
  margin-top: 0;
}

/* ══ RESPONSIVE ══ */
@media (max-width: 1000px) {
  .nav-links { display: none; }
  .feat-grid { grid-template-columns: repeat(2,1fr); }

  .why-banner-premium { padding: 40px 24px; }
  .wbp-text { font-size: 1.4rem; }
  .wbp-quote-mark-start { top: -20px; left: 10px; font-size: 8rem; }
  .wbp-quote-mark-end { bottom: -50px; right: 10px; font-size: 8rem; }
  .full-slide-item { grid-template-columns: 1fr; padding: 0 24px; }
  .full-slide-media { padding-right: 0; margin-bottom: 24px; }
  .full-slide-info { padding-left: 0; }
  .info-title { font-size: 1.5rem; }
  .desktop-guide-grid { grid-template-columns: 1fr; }
}
@media (max-width: 720px) {
  .desktop-settings-mask {
    padding: 14px;
  }
  .desktop-settings-box,
  .desktop-settings-pane {
    max-height: calc(100vh - 28px);
  }
  .desktop-settings-pane {
    padding: 24px 18px;
  }
  .desktop-setup-status-grid,
  .desktop-dependency-actions,
  .desktop-cache-actions,
  .desktop-settings-actions {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  .feat-grid { grid-template-columns: 1fr; }

  .price-grid { grid-template-columns: 1fr; }
  .cmp-header { grid-template-columns: 1fr; }
  .cmp-header span:not(:first-child) { display: none; }
  .cmp-row { grid-template-columns: 1fr; gap: 8px; }
  .hero-stats { gap: 16px; }
  .stat-div { display: none; }
  .h1-line-top { font-size: clamp(1.8rem, 7vw, 2.6rem); }
}

/* Image Preview Overlay */
.img-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(4, 4, 6, 0.94);
  backdrop-filter: blur(20px);
  display: grid;
  place-items: center;
  cursor: zoom-out;
}
.img-preview-container {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  justify-content: center;
  align-items: center;
}
.preview-img-full {
  max-width: 100%;
  max-height: 90vh;
  border-radius: 12px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.08);
  object-fit: contain;
}
.preview-close-btn {
  position: absolute;
  top: -40px;
  right: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(0, 0, 0, 0.6);
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: all 0.2s;
}
.preview-close-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  border-color: rgba(255, 255, 255, 0.35);
}

.preview-fade-enter-active,
.preview-fade-leave-active {
  transition: opacity 0.26s cubic-bezier(0.25, 1, 0.5, 1);
}
.preview-fade-enter-from,
.preview-fade-leave-to {
  opacity: 0;
}
</style>
