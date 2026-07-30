<template>
  <main class="legal-root">
    <section class="legal-shell">
      <router-link class="legal-brand" to="/">
        <span class="legal-brand-mark">P</span>
        <span>PaperSlover</span>
      </router-link>

      <nav class="legal-tabs" aria-label="Legal documents">
        <router-link v-for="item in legalTabs" :key="item.key" :to="item.path">
          {{ item.label }}
        </router-link>
      </nav>

      <article class="legal-card">
        <p class="legal-kicker">LEGAL CENTER</p>
        <h1>{{ page.title }}</h1>
        <p class="legal-updated">更新日期：2026-07-30</p>

        <section v-for="block in page.blocks" :key="block.title" class="legal-section">
          <h2>{{ block.title }}</h2>
          <p v-for="line in block.lines" :key="line">{{ line }}</p>
        </section>
      </article>
    </section>
  </main>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();

const legalTabs = [
  { key: "terms", label: "用户协议", path: "/terms" },
  { key: "privacy", label: "隐私政策", path: "/privacy" },
  { key: "disclaimer", label: "免责声明", path: "/disclaimer" },
];

const pages = {
  terms: {
    title: "用户协议",
    blocks: [
      {
        title: "服务范围",
        lines: [
          "PaperSlover 为文献管理、PDF 阅读、AI 研读、学术论坛、会员权益和桌面端本地依赖管理提供软件服务。",
          "你应保证上传、导入、发布或处理的内容来源合法，不侵犯第三方版权、隐私权或其他合法权益。",
        ],
      },
      {
        title: "账号与权限",
        lines: [
          "账号仅限本人使用。管理员、导师、学生等身份会对应不同操作权限，后台关键操作会记录审计日志。",
          "如出现刷量、攻击、恶意请求、违规发布内容或绕过额度限制等行为，平台可限制、封禁或终止相关账号能力。",
        ],
      },
      {
        title: "会员与付费",
        lines: [
          "会员套餐、限时优惠、额度和权益以购买页与管理员后台实际配置为准。",
          "涉及人工审核、支付异常、退款或权益补发时，请以平台公告、订单记录和客服处理结果为准。",
        ],
      },
    ],
  },
  privacy: {
    title: "隐私政策",
    blocks: [
      {
        title: "数据保存",
        lines: [
          "桌面端导入的 PDF 优先保存在用户本机目录；服务器主要保存题录、笔记、标注索引、会员权益和必要的运行记录。",
          "论坛图片、头像、背景图等用户主动上传的资源会用于页面展示和内容审核。",
        ],
      },
      {
        title: "AI 与第三方服务",
        lines: [
          "AI 研读、综述、PPT、审核或翻译功能可能调用平台配置的模型服务、翻译服务或本机依赖服务。",
          "平台会记录必要的调用时间、模块、模型标识、消耗额度、状态和错误摘要，用于计费、排障和风控，不用于公开展示个人隐私。",
        ],
      },
      {
        title: "安全与删除",
        lines: [
          "我们会对管理员关键操作、异常访问、可疑 IP、异常请求量进行记录和监控。",
          "你可以在产品提供的入口管理个人资料和本地缓存；法律法规要求或安全风控需要保留的记录除外。",
        ],
      },
    ],
  },
  disclaimer: {
    title: "免责声明",
    blocks: [
      {
        title: "学术辅助定位",
        lines: [
          "PaperSlover 的 AI 输出仅用于文献阅读、思路整理和研究辅助，不构成投稿、署名、实验、医学、法律或投资等专业结论。",
          "AI 可能出现事实错误、引用遗漏、翻译偏差或格式不完整，正式使用前需要用户自行核验原文、数据和引用。",
        ],
      },
      {
        title: "版权与来源",
        lines: [
          "用户应确认自己有权下载、导入和处理相关论文、图片、附件或网页内容。",
          "因用户未经授权传播、下载、分享或公开他人内容产生的责任，由用户自行承担。",
        ],
      },
      {
        title: "服务可用性",
        lines: [
          "模型接口、翻译引擎、PDF 解析依赖、Zotero 本机通信和第三方网站可用性可能受网络、地区、账号、额度或版本影响。",
          "我们会尽力提升稳定性，但不承诺任何功能在所有时间、所有设备和所有第三方页面上完全可用。",
        ],
      },
    ],
  },
};

const page = computed(() => pages[route.meta.legalType] || pages.terms);
</script>

<style scoped>
.legal-root {
  min-height: 100vh;
  color: #e5edff;
  background:
    radial-gradient(circle at 18% 12%, rgba(59, 130, 246, .22), transparent 28%),
    radial-gradient(circle at 78% 8%, rgba(20, 184, 166, .16), transparent 26%),
    linear-gradient(135deg, #07111f 0%, #0f172a 52%, #111827 100%);
  padding: 34px 20px 56px;
}
.legal-shell { width: min(980px, 100%); margin: 0 auto; }
.legal-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #f8fbff;
  text-decoration: none;
  font-weight: 900;
  letter-spacing: .02em;
}
.legal-brand-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  background: linear-gradient(135deg, #2563eb, #8b5cf6);
  box-shadow: 0 18px 38px rgba(37, 99, 235, .28);
}
.legal-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin: 26px 0 18px;
}
.legal-tabs a {
  padding: 10px 16px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, .24);
  color: #93a4c3;
  text-decoration: none;
  font-size: 13px;
  font-weight: 800;
}
.legal-tabs a.router-link-active {
  color: #ffffff;
  border-color: rgba(96, 165, 250, .55);
  background: rgba(37, 99, 235, .22);
}
.legal-card {
  border: 1px solid rgba(148, 163, 184, .22);
  border-radius: 18px;
  background: rgba(15, 23, 42, .78);
  box-shadow: 0 28px 70px rgba(2, 6, 23, .34);
  padding: clamp(28px, 5vw, 56px);
}
.legal-kicker {
  margin: 0 0 10px;
  color: #60a5fa;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: .16em;
}
.legal-card h1 {
  margin: 0;
  font-size: clamp(32px, 5vw, 54px);
  line-height: 1.05;
}
.legal-updated {
  margin: 14px 0 34px;
  color: #94a3b8;
  font-size: 14px;
}
.legal-section {
  padding: 24px 0;
  border-top: 1px solid rgba(148, 163, 184, .16);
}
.legal-section h2 {
  margin: 0 0 12px;
  color: #bfdbfe;
  font-size: 19px;
}
.legal-section p {
  margin: 10px 0 0;
  color: #cbd5e1;
  line-height: 1.85;
  font-size: 15px;
}
@media (max-width: 640px) {
  .legal-root { padding: 20px 12px 36px; }
  .legal-card { border-radius: 14px; }
}
</style>
