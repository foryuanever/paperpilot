export const sourceUrlMap = {
  arXiv: "https://arxiv.org/search/?query=transformer",
  PubMed: "https://pubmed.ncbi.nlm.nih.gov/?term=transformer",
  "Semantic Scholar": "https://www.semanticscholar.org/search?q=transformer",
  Crossref: "https://search.crossref.org/?q=transformer",
  DOAJ: "https://doaj.org/search/articles/transformer",
};

export const sourceItems = [
  { name: "arXiv", desc: "计算机 / AI / 数学预印本" },
  { name: "PubMed", desc: "医学 / 生物 / 公卫文献" },
  { name: "Semantic Scholar", desc: "引文和相似论文发现" },
  { name: "Crossref", desc: "DOI 元数据抓取入口" },
  { name: "DOAJ", desc: "开放获取期刊资源" },
];

export const architectureSteps = [
  {
    id: 1,
    title: "文献源网关",
    description: "统一代理 arXiv、Crossref、PubMed，避免前端直连跨域。",
  },
  {
    id: 2,
    title: "导入与元数据服务",
    description: "处理 DOI、URL、标题摘要导入，生成 paper、paper_file 记录。",
  },
  {
    id: 3,
    title: "AI 网关",
    description: "把 Base URL、Key、Model 统一成 OpenAI Compatible 调用。",
  },
  {
    id: 4,
    title: "阅读器工作区",
    description: "保存阅读进度、翻译结果、解析摘要和问答历史。",
  },
];

export const apiItems = [
  {
    title: "POST /api/admin/model-config",
    description: "保存用户配置的 Base URL、API Key、默认模型和场景偏好。",
  },
  {
    title: "POST /api/papers/import",
    description: "通过 DOI、URL 或手填元数据创建论文工作区。",
  },
  {
    title: "POST /api/ai/translate",
    description: "根据选中段落和风格，返回学术翻译结果。",
  },
  {
    title: "POST /api/ai/analyze",
    description: "提取研究问题、创新点、方法、实验和局限性。",
  },
  {
    title: "POST /api/ai/summary",
    description: "输出适合组会和开题阶段使用的汇总摘要。",
  },
];

export const pageNavItems = [
  { to: "/library", label: "文献库", icon: "library" },
  { to: "/reading", label: "文献阅读", icon: "reading" },
  { to: "/meeting-report", label: "组会汇报", icon: "slides" },
  { to: "/search", label: "学术搜索", icon: "search" },
  { to: "/forum", label: "学术论坛", icon: "forum" },
  { to: "/models", label: "模型与额度", icon: "models" },
  { to: "/referral", label: "邀请推广", icon: "referral" },
  { to: "/team", label: "我的team", icon: "team" },
];
