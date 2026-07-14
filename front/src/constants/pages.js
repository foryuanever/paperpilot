export const dashboardStats = [
  { label: "总文献", value: "128", detail: "本周新增 12 篇" },
  { label: "已精读", value: "36", detail: "Method 深读占 61%" },
  { label: "AI 问答", value: "412", detail: "今日已调用 19 次" },
  { label: "综述草稿", value: "9", detail: "2 份待导师确认" },
];

export const recentPapers = [
  {
    title: "Scaling Vision Transformers",
    meta: "CV / 2h 前打开 / 阅读进度 67%",
    tag: "速读完成",
  },
  {
    title: "Graph Foundation Models Survey",
    meta: "Graph / 昨天 / 阅读进度 24%",
    tag: "待解析",
  },
  {
    title: "RAG for Scientific Discovery",
    meta: "LLM / 本周 / 阅读进度 81%",
    tag: "已加笔记",
  },
];

export const libraryFolders = [
  { name: "开题阶段", count: 23, desc: "方向摸排、基线综述、经典工作" },
  { name: "实验复现", count: 41, desc: "数据集、代码仓、指标对照" },
  { name: "组会候选", count: 12, desc: "适合汇报的重点论文" },
  { name: "综述素材", count: 52, desc: "后续写作语料和引用备份" },
];

export const searchSuggestions = [
  "multimodal reasoning benchmark",
  "retrieval augmented generation for science",
  "graph neural network survey 2025",
  "efficient long context transformer",
];

export const searchEnginePresets = [
  {
    id: "sciencedirect",
    name: "Science Direct",
    shortName: "Science Direct",
    url: "https://www.sciencedirect.com/",
    searchPrefix: "https://www.sciencedirect.com/search?qs=",
  },
  {
    id: "semantic-scholar",
    name: "Semantic Scholar",
    shortName: "Semantic Scholar",
    url: "https://www.semanticscholar.org/",
    searchPrefix: "https://www.semanticscholar.org/search?q=",
  },
  {
    id: "pubmed",
    name: "PubMed",
    shortName: "PubMed",
    url: "https://pubmed.ncbi.nlm.nih.gov/",
    searchPrefix: "https://pubmed.ncbi.nlm.nih.gov/?term=",
  },
  {
    id: "web-of-science",
    name: "Web of Science",
    shortName: "Web of Science",
    url: "https://www.webofscience.com/",
    searchPrefix: "https://www.webofscience.com/wos/woscc/basic-search?search_mode=GeneralSearch&q=",
  },
  {
    id: "cnki",
    name: "知网",
    shortName: "知网",
    url: "https://www.cnki.net/",
    searchPrefix: "https://kns.cnki.net/kns8s/AdvSearch?kw=",
  },
  {
    id: "wanfang",
    name: "万方",
    shortName: "万方",
    url: "https://www.wanfangdata.com.cn/",
    searchPrefix: "https://s.wanfangdata.com.cn/paper?q=",
  },
  {
    id: "research-rabbit",
    name: "Research Rabbit",
    shortName: "Research Rabbit",
    url: "https://www.researchrabbit.ai/",
    searchPrefix: "https://www.researchrabbit.ai/",
  },
  {
    id: "connected-papers",
    name: "Connected Papers",
    shortName: "Connected Papers",
    url: "https://www.connectedpapers.com/",
    searchPrefix: "https://www.connectedpapers.com/",
  },
];

export const searchResultRows = [
  {
    id: "sr-transformer",
    title: "Attention Is All You Need",
    source: "arXiv",
    authors: "Ashish Vaswani et al.",
    year: "2017",
    abstract: "The transformer architecture removes recurrence and relies entirely on attention mechanisms.",
    pdfUrl: "https://arxiv.org/pdf/1706.03762.pdf",
  },
  {
    id: "sr-bert",
    title: "BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding",
    source: "ACL Anthology",
    authors: "Jacob Devlin et al.",
    year: "2019",
    abstract: "BERT pre-trains deep bidirectional representations from unlabeled text.",
    pdfUrl: "https://aclanthology.org/N19-1423.pdf",
  },
  {
    id: "sr-rag",
    title: "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks",
    source: "NeurIPS",
    authors: "Patrick Lewis et al.",
    year: "2020",
    abstract: "A hybrid parametric and non-parametric memory approach for generation.",
    pdfUrl: "https://papers.nips.cc/paper_files/paper/2020/file/6b493230205f780e1bc26945df7481e5-Paper.pdf",
  },
];

export const modelCards = [
  {
    id: "ollama-local",
    preset: "ollama-local",
    title: "Ollama 本地模型",
    desc: "模型运行在自己的电脑上，不产生第三方 API 调用费用。",
    badge: "完全本地",
    provider: "Ollama",
    latency: "取决于设备",
    cost: "API 免费",
    status: "online",
  },
  {
    id: "gemini-free",
    preset: "gemini-free",
    title: "Gemini Flash",
    desc: "支持 Google AI Studio Key，可在平台免费额度内调用。",
    badge: "免费额度",
    provider: "Google Gemini",
    latency: "云端",
    cost: "额度内免费",
    status: "online",
  },
  {
    id: "openrouter-free",
    preset: "openrouter-free",
    title: "OpenRouter Free",
    desc: "使用免费模型路由，具体模型与频率限制由 OpenRouter 决定。",
    badge: "免费模型",
    provider: "OpenRouter",
    latency: "动态",
    cost: "模型免费",
    status: "online",
  },
  {
    id: "openai",
    preset: "openai",
    title: "OpenAI API",
    desc: "使用独立 OpenAI API Key；ChatGPT 或 Codex 订阅不等于 API 免费额度。",
    badge: "独立计费",
    provider: "OpenAI",
    latency: "云端",
    cost: "按量计费",
    status: "online",
  },
  {
    id: "anthropic",
    preset: "anthropic",
    title: "Claude API",
    desc: "原生 Anthropic Messages API，使用 Anthropic Console 创建的 Key。",
    badge: "独立计费",
    provider: "Anthropic",
    latency: "云端",
    cost: "按量计费",
    status: "online",
  },
];

export const billingPlans = [
  {
    id: "lite",
    billingCycle: "月度",
    tier: "Lite",
    name: "个人 Lite",
    price: "¥9.9",
    period: "/月",
    highlight: false,
    features: ["论文导入与基础翻译不限次", "论文综述 3 次", "AI 文章对话 20 次", "适合个人轻量阅读"],
  },
  {
    id: "plus",
    billingCycle: "月度",
    tier: "Plus",
    name: "个人 Plus",
    price: "¥19.9",
    period: "/月",
    highlight: true,
    features: ["论文导入与基础翻译不限次", "论文综述 10 次", "组会 PPT 2 次", "AI 文章对话 80 次", "适合课程论文与周会"],
  },
  {
    id: "pro",
    billingCycle: "月度",
    tier: "Pro",
    name: "个人 Pro",
    price: "¥39.9",
    period: "/月",
    highlight: false,
    features: ["论文综述 25 次", "组会 PPT 4 次", "AI 文章对话 500 次", "论坛彩色姓名", "发帖列表波浪特效"],
  },
  {
    id: "max",
    billingCycle: "月度",
    tier: "Max",
    name: "个人 Max",
    price: "¥69.9",
    period: "/月",
    highlight: false,
    features: ["论文综述 60 次", "组会 PPT 10 次", "AI 文章对话 1200 次", "论坛彩色姓名", "发帖列表波浪特效"],
  },
  {
    id: "team_plus",
    billingCycle: "月度",
    tier: "团队 Plus",
    name: "团队 Plus",
    price: "¥129",
    period: "/月",
    highlight: false,
    features: ["导师一人开通，全队共享", "论文综述 120 次", "组会 PPT 16 次", "AI 文章对话 2600 次", "团队席位 8 席"],
  },
  {
    id: "team_pro",
    billingCycle: "月度",
    tier: "团队 Pro",
    name: "团队 Pro",
    price: "¥229",
    period: "/月",
    highlight: false,
    features: ["导师一人开通，全队共享", "论文综述 260 次", "组会 PPT 36 次", "AI 文章对话 6000 次", "团队席位 15 席", "论坛彩色姓名与发帖波浪"],
  },
];

export const dashboardBento = [
  {
    title: "今日科研任务",
    desc: "继续精读 Transformer 系列 3 篇，完成实验对照表。",
    size: "large",
  },
  {
    title: "翻译引擎偏好",
    desc: "NLP 类论文更适合学科翻译 + 术语库校正。",
    size: "small",
  },
  {
    title: "导师汇报准备",
    desc: "还差 2 篇 related work 的对比结论，可直接转成汇报提纲。",
    size: "tall",
  },
  {
    title: "引用语料积累",
    desc: "本周新增 18 条高质量句式，适合 Methods 和 Conclusion 改写。",
    size: "wide",
  },
];

export const libraryPaperRows = [
  {
    title: "Attention Is All You Need",
    source: "arXiv",
    authors: "Vaswani et al.",
    progress: "81%",
    importance: "A",
    note: "重点关注自注意力结构和实验对比。",
    journalTags: ["IF 17.9", "CCF A", "NLP"],
    publishYear: "2017",
    readAt: "2026-06-04 20:18",
    uploadedAt: "2026-06-04",
  },
  {
    title: "BERT: Pre-training of Deep Bidirectional Transformers",
    source: "NAACL",
    authors: "Devlin et al.",
    progress: "64%",
    importance: "A",
    note: "可作为预训练范式和下游迁移学习代表。",
    journalTags: ["NAACL", "预训练", "经典"],
    publishYear: "2019",
    readAt: "2026-06-03 15:42",
    uploadedAt: "2026-06-02",
  },
  {
    title: "Retrieval-Augmented Generation for Knowledge-Intensive NLP",
    source: "NeurIPS",
    authors: "Lewis et al.",
    progress: "43%",
    importance: "B",
    note: "适合和自己的课题做 RAG 方向对照。",
    journalTags: ["NeurIPS", "RAG", "LLM"],
    publishYear: "2020",
    readAt: "2026-06-01 09:26",
    uploadedAt: "2026-05-30",
  },
];

export const searchSources = [
  { name: "arXiv", desc: "预印本与计算机科学热点最全" },
  { name: "PubMed", desc: "医学和生命科学权威检索" },
  { name: "Crossref", desc: "DOI 元数据抓取和跳转" },
  { name: "Semantic Scholar", desc: "适合追踪引文脉络和相似论文" },
];

export const readerNotes = [
  "Abstract 先做一句话摘要",
  "Method 标记可复现细节",
  "Experiment 提取对比基线和指标",
  "Conclusion 提取未来工作和局限性",
];
