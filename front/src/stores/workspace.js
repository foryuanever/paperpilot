import { computed, reactive, ref, watch } from "vue";
import { defineStore } from "pinia";
import { paperpilotApi } from "../services/paperpilotApi";

const STORAGE_KEYS = {
  modelConfig: "paperpilot-model-config",
  paperDraft: "paperpilot-paper-draft",
};

function splitSentences(text) {
  return String(text || "")
    .replace(/\s+/g, " ")
    .split(/(?<=[.!?。！？])\s+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function deriveResearchDirection(paper) {
  const context = `${paper.title} ${paper.abstract} ${paper.source}`.toLowerCase();
  if (/(vision|image|coco|imagenet|vit|diffusion)/.test(context)) return "计算机视觉 / 多模态";
  if (/(graph|gnn|node|molecule)/.test(context)) return "图学习 / 科学计算";
  if (/(retrieval|rag|language|nlp|transformer|bert|gpt|translation)/.test(context)) return "自然语言处理 / 大模型";
  if (/(medical|biomed|pubmed|clinical)/.test(context)) return "生物医学文本";
  return "通用机器学习 / 学术研究";
}

function deriveDatasets(paper) {
  const text = `${paper.title} ${paper.abstract} ${paper.note}`.toLowerCase();
  const candidates = [
    "GLUE",
    "SQuAD",
    "WikiText",
    "C4",
    "ImageNet",
    "COCO",
    "CIFAR",
    "PubMed",
    "OpenWebText",
    "MS MARCO",
    "LibriSpeech",
  ];
  const matches = candidates.filter((item) => text.includes(item.toLowerCase()));
  if (matches.length) return matches;
  if (/dataset|benchmark|corpus|evaluation/.test(text)) return ["论文正文提到 benchmark / dataset，但当前摘要未明确列出名称"];
  return ["当前摘要中未显式给出数据集名称"];
}

function deriveContributionBullets(paper) {
  const sentences = splitSentences(paper.abstract).slice(0, 3);
  if (sentences.length) return sentences;
  return [
    "当前论文尚未同步详细摘要。",
    "建议先从搜索页重新导入带摘要的版本。",
  ];
}

const actionTemplates = {
  translate: (paper) => {
    const lead = splitSentences(paper.abstract)[0] || "当前尚未获取到摘要原文。";
    return `【摘要精译】
${lead}

【适合导师汇报的说法】
这篇工作主要落在“${deriveResearchDirection(paper)}”方向，重点是 ${paper.title} 所代表的方法路线。

【建议下一步】
先精读 Introduction 与 Method，再把实验设置、数据集和局限性补进下方信息区。`;
  },
  analyze: (paper) => {
    const bullets = deriveContributionBullets(paper);
    return `【研究方向】
${deriveResearchDirection(paper)}

【核心要点】
1. ${bullets[0] || "当前摘要未给出明确信息。"}
2. ${bullets[1] || "建议继续查看方法与实验部分。"}
3. ${bullets[2] || "可在下方补充局限性与后续工作。"}

【数据集线索】
${deriveDatasets(paper).join(" / ")}`;
  },
  summary: (paper) => `【一页综述】
论文《${paper.title}》来自 ${paper.source}，作者为 ${paper.authors || "未补全"}。

【适合组会的三句话】
1. 该工作属于 ${deriveResearchDirection(paper)}。
2. 核心摘要：${splitSentences(paper.abstract)[0] || "当前未同步摘要。"}
3. 重点跟进：数据集、实验指标和可复现设置。

【延展阅读建议】
继续沿着同一方向补充 2-3 篇对比论文，形成 related work 结构。`,
};

const openCodeFreeModels = [
  { id: "deepseek-v4-flash-free", name: "DeepSeek V4 Flash Free" },
  { id: "mimo-v2.5-free", name: "MiMo V2.5 Free" },
  { id: "north-mini-code-free", name: "North Mini Code Free" },
  { id: "nemotron-3-ultra-free", name: "Nemotron 3 Ultra Free" },
  { id: "big-pickle", name: "Big Pickle" },
];

const nineRouterOpenCodeFreeModels = [
  { id: "oc/deepseek-v4-flash-free", name: "DeepSeek V4 Flash Free" },
  { id: "oc/north-mini-code-free", name: "North Mini Code Free" },
  { id: "oc/mimo-v2.5-free", name: "MiMo V2.5 Free" },
  { id: "oc/nemotron-3-ultra-free", name: "Nemotron 3 Ultra Free" },
];

const openRouterFreeModels = [
  { id: "nex-agi/nex-n2-pro:free", name: "Nex N2 Pro Free" },
  { id: "qwen/qwen3-next-80b-a3b-instruct:free", name: "Qwen3 Next 80B Free" },
  { id: "openai/gpt-oss-120b:free", name: "GPT OSS 120B Free" },
  { id: "meta-llama/llama-3.3-70b-instruct:free", name: "Llama 3.3 70B Instruct Free" },
  { id: "nvidia/nemotron-3-ultra-550b-a55b:free", name: "Nemotron 3 Ultra Free" },
];

const deepSeekOfficialModels = [
  { id: "deepseek-v4-flash", name: "DeepSeek V4 Flash" },
  { id: "deepseek-v4-pro", name: "DeepSeek V4 Pro" },
  { id: "deepseek-chat", name: "DeepSeek Chat · legacy" },
  { id: "deepseek-reasoner", name: "DeepSeek Reasoner · legacy" },
];
const nineRouterModelAliases = Object.fromEntries(
  [
    ...nineRouterOpenCodeFreeModels.flatMap((model) => [
    [model.id, model.id],
    [model.name, model.id],
    [model.name.toLowerCase(), model.id],
    ]),
    ["oc/nemotron-3-super-free", "oc/deepseek-v4-flash-free"],
    ["Nemotron 3 Super Free", "oc/deepseek-v4-flash-free"],
    ["nemotron 3 super free", "oc/deepseek-v4-flash-free"],
    ["oc/minimax-m3-free", "oc/deepseek-v4-flash-free"],
    ["MiniMax M3 Free", "oc/deepseek-v4-flash-free"],
    ["minimax m3 free", "oc/deepseek-v4-flash-free"],
    ["oc/qwen3.6-plus-free", "oc/deepseek-v4-flash-free"],
    ["Qwen 3.6 Plus Free", "oc/deepseek-v4-flash-free"],
    ["qwen 3.6 plus free", "oc/deepseek-v4-flash-free"],
    ["oc/minimax-m2.5-free", "oc/deepseek-v4-flash-free"],
    ["MiniMax M2.5 Free", "oc/deepseek-v4-flash-free"],
    ["minimax m2.5 free", "oc/deepseek-v4-flash-free"],
  ],
);

const presetMap = {
  "opencode-free": {
    providerName: "OpenCode Zen",
    baseUrl: "https://opencode.ai/zen/v1",
    apiKey: "",
    modelName: "deepseek-v4-flash-free",
    apiFormat: "openai_chat",
    authType: "bearer",
    scene: "analyze",
  },
  "9router-free": {
    providerName: "9Router OpenCode Free",
    baseUrl: "https://rnr5845.abc-tunnel.us/v1",
    apiKey: "",
    modelName: "oc/deepseek-v4-flash-free",
    apiFormat: "openai_chat",
    authType: "bearer",
    scene: "analyze",
  },
  "relay": {
    providerName: "自定义中转站",
    baseUrl: "",
    apiKey: "",
    modelName: "",
    apiFormat: "openai_chat",
    authType: "bearer",
    scene: "analyze",
  },
  "openrouter-free": {
    providerName: "OpenRouter Free",
    baseUrl: "https://openrouter.ai/api/v1",
    apiKey: "",
    modelName: "nex-agi/nex-n2-pro:free",
    apiFormat: "openai_chat",
    authType: "bearer",
    scene: "analyze",
  },
  "deepseek-official": {
    providerName: "DeepSeek 官方",
    baseUrl: "https://api.deepseek.com",
    apiKey: "",
    modelName: "deepseek-v4-flash",
    apiFormat: "openai_chat",
    authType: "bearer",
    scene: "analyze",
  },
};

function loadState(key, fallback) {
  const saved = localStorage.getItem(key);
  if (!saved) {
    return fallback;
  }

  try {
    return JSON.parse(saved);
  } catch {
    return fallback;
  }
}

function friendlyModelError(message) {
  const text = String(message || "");
  if (/HTTP\s*530|error code:\s*1016/i.test(text)) {
    return "9Router 中转隧道当前不稳定（HTTP 530 / 1016），不是模型选错。请稍后重试，或切换 OpenCode Zen / 自定义稳定中转站。";
  }
  if (/HTTP\s*50[234]|timed out|timeout/i.test(text)) {
    return "中转站或上游模型临时拥堵，系统会自动重试；仍失败时请稍后再试或换稳定中转。";
  }
  return text || "连接测试失败，请检查网络、地址与密钥";
}

export const useWorkspaceStore = defineStore("workspace", () => {
  const selectedSource = ref("arXiv");
  const currentAction = ref("translate");
  const syncState = reactive({
    importing: false,
    savingModel: false,
    testingModel: false,
    fetchingModels: false,
    modelOptions: [],
    modelTest: null,
    modelSaveResult: null,
    chatTesting: false,
    chatReply: null,
    importedWorkspaceId: "",
    lastSyncMessage: "当前为本地演示模式，后续可切后端接口。",
  });

  const paper = reactive(
    loadState(STORAGE_KEYS.paperDraft, {
      source: "arXiv",
      id: "",
      url: "https://arxiv.org/abs/1706.03762",
      pdfUrl: "https://arxiv.org/pdf/1706.03762.pdf",
      authors: "Ashish Vaswani et al.",
      title: "Attention Is All You Need",
      abstract:
        "The transformer architecture removes recurrence and relies entirely on attention mechanisms to draw global dependencies between input and output.",
    }),
  );

  const modelConfig = reactive(
    {
      fullUrl: false,
      modelsUrl: "",
      customUserAgent: "",
      ...loadState(STORAGE_KEYS.modelConfig, presetMap["9router-free"]),
    },
  );
  if (modelConfig.apiFormat === "openai") modelConfig.apiFormat = "openai_chat";
  if (!modelConfig.authType) modelConfig.authType = modelConfig.apiFormat === "anthropic" ? "x-api-key" : "bearer";
  if (modelConfig.providerName === "OpenCode Free") modelConfig.providerName = "OpenCode Zen";

  selectedSource.value = paper.source;

  watch(
    paper,
    (value) => {
      localStorage.setItem(STORAGE_KEYS.paperDraft, JSON.stringify(value));
    },
    { deep: true },
  );

  watch(
    modelConfig,
    (value) => {
      localStorage.setItem(STORAGE_KEYS.modelConfig, JSON.stringify(value));
    },
    { deep: true },
  );

  const configPreview = computed(() =>
    JSON.stringify(
      {
        providerName: modelConfig.providerName,
        baseUrl: modelConfig.baseUrl,
        apiKey: modelConfig.apiKey ? "sk-***hidden***" : "",
        model: modelConfig.modelName,
        apiFormat: modelConfig.apiFormat || "openai_chat",
        authType: modelConfig.authType,
        fullUrl: Boolean(modelConfig.fullUrl),
        modelsUrl: modelConfig.modelsUrl || "",
        scene: modelConfig.scene,
        endpoint: modelConfig.fullUrl
          ? modelConfig.baseUrl
          : modelConfig.apiFormat === "anthropic"
          ? `${modelConfig.baseUrl || "<baseUrl>"}/v1/messages`
          : modelConfig.apiFormat === "openai_responses"
            ? `${modelConfig.baseUrl || "<baseUrl>"}/responses`
            : `${modelConfig.baseUrl || "<baseUrl>"}/chat/completions`,
      },
      null,
      2,
    ),
  );

  const aiOutput = computed(() => actionTemplates[currentAction.value](paper));

  function selectSource(source) {
    selectedSource.value = source;
    paper.source = source;
  }

  function applyPreset(name) {
    Object.assign(modelConfig, presetMap[name]);
    syncState.modelOptions =
      name === "opencode-free"
        ? [...openCodeFreeModels]
        : name === "9router-free"
          ? [...nineRouterOpenCodeFreeModels]
        : name === "openrouter-free"
          ? [...openRouterFreeModels]
        : name === "deepseek-official"
          ? [...deepSeekOfficialModels]
        : [];
    syncState.modelTest = null;
    syncState.modelSaveResult = null;
  }

  function importOpenCodeModels() {
    Object.assign(modelConfig, presetMap["opencode-free"]);
    syncState.modelOptions = [...openCodeFreeModels];
    syncState.modelTest = {
      success: true,
      message: `已导入 ${openCodeFreeModels.length} 个 OpenCode Zen 免费模型，请填写 Zen Key 后测试`,
    };
  }

  function setAction(action) {
    currentAction.value = action;
  }

  function clearModelFeedback() {
    syncState.modelTest = null;
    syncState.modelSaveResult = null;
    syncState.chatReply = null;
  }

  function modelPayload() {
    const baseUrl = String(modelConfig.baseUrl || "").toLowerCase().replace(/\/+$/, "");
    const inferredFormat = /\/codex(?:\/v\d+)?$/.test(baseUrl) || baseUrl.endsWith("/responses")
      ? "openai_responses"
      : modelConfig.apiFormat || "openai_chat";
    const scene = modelConfig.scene || "general";
    const apiFormat = scene === "meeting_deck" ? "openai_responses" : inferredFormat;
    const modelName =
      modelConfig.providerName === "9Router OpenCode Free"
        ? nineRouterModelAliases[modelConfig.modelName] ||
          nineRouterModelAliases[String(modelConfig.modelName || "").toLowerCase()] ||
          nineRouterOpenCodeFreeModels[0].id
        : modelConfig.modelName;
    return {
      providerName: modelConfig.providerName,
      baseUrl: modelConfig.baseUrl,
      apiKey: modelConfig.apiKey,
      modelName,
      apiFormat,
      authType: modelConfig.authType || "bearer",
      fullUrl: Boolean(modelConfig.fullUrl),
      modelsUrl: modelConfig.modelsUrl || "",
      customUserAgent: modelConfig.customUserAgent || "",
      scene,
    };
  }

  function loadPaper(nextPaper) {
    Object.assign(paper, {
      ...paper,
      ...nextPaper,
    });
    if (nextPaper.source) {
      selectedSource.value = nextPaper.source;
    }
  }

  async function saveModelConfig() {
    syncState.savingModel = true;
    syncState.modelSaveResult = null;
    try {
      const response = await paperpilotApi.saveModelConfig(modelPayload());
      syncState.lastSyncMessage = `模型配置已同步到后端接口：${response.providerName} / ${response.modelName}`;
      syncState.modelTest = null;
      syncState.modelSaveResult = {
        success: true,
        message: `保存成功，${response.providerName} / ${response.modelName} 已启用于当前入口`,
      };
      return response;
    } catch (error) {
      syncState.lastSyncMessage = friendlyModelError(error.response?.data?.message || "模型配置保存失败。");
      syncState.modelSaveResult = {
        success: false,
        message: syncState.lastSyncMessage,
      };
      return null;
    } finally {
      syncState.savingModel = false;
    }
  }

  async function testModelChat(prompt) {
    syncState.chatTesting = true;
    syncState.chatReply = null;
    try {
      const result = await paperpilotApi.chatWithModel(modelPayload(), prompt);
      syncState.chatReply = result;
      return result;
    } catch (error) {
      syncState.chatReply = {
        success: false,
        message: friendlyModelError(error.response?.data?.message || "对话测试失败，请确认配置已保存"),
      };
      return syncState.chatReply;
    } finally {
      syncState.chatTesting = false;
    }
  }

  async function testModelConfig() {
    syncState.testingModel = true;
    syncState.modelTest = null;
    try {
      const result = await paperpilotApi.testModelConfig(modelPayload());
      syncState.modelTest = result;
      const curatedModels =
        modelConfig.providerName === "OpenCode Zen"
          ? openCodeFreeModels
          : modelConfig.providerName === "9Router OpenCode Free"
            ? nineRouterOpenCodeFreeModels
          : modelConfig.providerName === "OpenRouter Free"
            ? openRouterFreeModels
          : null;
      if (result.success && !curatedModels) {
        const discovered = await paperpilotApi.fetchModelList(modelPayload());
        syncState.modelOptions = discovered.models || [];
        if (!modelConfig.modelName && syncState.modelOptions.length) {
          modelConfig.modelName = syncState.modelOptions[0].id;
        }
        syncState.modelTest = {
          ...result,
          message: `${result.message}；已发现 ${syncState.modelOptions.length} 个模型`,
        };
      } else if (result.success && curatedModels) {
        syncState.modelOptions = [...curatedModels];
      }
    } catch (error) {
      syncState.modelTest = {
        success: false,
        message: friendlyModelError(error.response?.data?.message || "连接测试失败，请检查网络、地址与密钥"),
      };
    } finally {
      syncState.testingModel = false;
    }
  }

  async function fetchModelList() {
    syncState.fetchingModels = true;
    try {
      if (modelConfig.providerName === "9Router OpenCode Free") {
        syncState.modelOptions = [...nineRouterOpenCodeFreeModels];
        modelConfig.modelName =
          nineRouterModelAliases[modelConfig.modelName] ||
          nineRouterModelAliases[String(modelConfig.modelName || "").toLowerCase()] ||
          nineRouterOpenCodeFreeModels[0].id;
        syncState.modelTest = {
          success: true,
          message: `已加载 ${nineRouterOpenCodeFreeModels.length} 个 OpenCode Free 免费模型`,
        };
        return syncState.modelTest;
      }
      const result = await paperpilotApi.fetchModelList(modelPayload());
      syncState.modelOptions = result.models || [];
      if (result.success && !modelConfig.modelName && syncState.modelOptions.length) {
        modelConfig.modelName = syncState.modelOptions[0].id;
      }
      syncState.modelTest = result;
      return result;
    } catch (error) {
      syncState.modelTest = {
        success: false,
        message: friendlyModelError(error.response?.data?.message || "模型列表获取失败"),
      };
      return syncState.modelTest;
    } finally {
      syncState.fetchingModels = false;
    }
  }

  async function importPaper() {
    paper.source = selectedSource.value;
    syncState.importing = true;
    try {
      const response = await paperpilotApi.importPaper({
        source: paper.source,
        paperId: paper.id,
        paperUrl: paper.url,
        title: paper.title,
        abstractText: paper.abstract,
        authors: paper.authors,
        publishYear: paper.publishYear,
      });
      syncState.importedWorkspaceId = response.workspaceId;
      syncState.lastSyncMessage = `论文已导入后端工作区：${response.workspaceId}`;
      return response;
    } catch {
      syncState.lastSyncMessage = "后端未连通，当前仅保留本地工作区草稿。";
      throw new Error(syncState.lastSyncMessage);
    } finally {
      syncState.importing = false;
    }
  }

  async function importPaperFromSearch(item) {
    syncState.importing = true;
    try {
      const response = await paperpilotApi.importPaper({
        source: item.source,
        paperId: item.id,
        paperUrl: item.pdfUrl || item.sourceUrl || (String(item.id || "").startsWith("10.") ? `https://doi.org/${item.id}` : ""),
        sourceUrl: item.sourceUrl || "",
        importSource: item.importSource || item.source || "",
        articleType: item.articleType || "",
        subjects: item.subjects || [],
        title: item.title,
        abstractText: item.abstractText,
        authors: item.authors,
        publishYear: item.year,
      });
      syncState.importedWorkspaceId = response.workspaceId;
      syncState.lastSyncMessage = `检索结果已导入：${response.workspaceId}`;
      return response;
    } finally {
      syncState.importing = false;
    }
  }

  async function hydrateFromBackend(scene = "general") {
    try {
      const active = await paperpilotApi.getActiveModelConfig(scene);
      const providerName = active.providerName === "OpenCode Free" ? "OpenCode Zen" : active.providerName;
      const normalizedProviderName =
        ["9Router 免费路由", "9Router 模型路由"].includes(providerName) ? "9Router OpenCode Free" : providerName;
      const isNineRouterConfig = normalizedProviderName === "9Router OpenCode Free";
      const activeModelName =
        isNineRouterConfig && !nineRouterOpenCodeFreeModels.some((model) => model.id === active.modelName)
          ? nineRouterModelAliases[active.modelName] ||
            nineRouterModelAliases[String(active.modelName || "").toLowerCase()] ||
            nineRouterOpenCodeFreeModels[0].id
          : active.modelName;
      Object.assign(modelConfig, {
        providerName: normalizedProviderName,
        baseUrl: active.baseUrl,
        modelName: activeModelName,
        apiFormat: scene === "meeting_deck" ? "openai_responses" : isNineRouterConfig ? "openai_chat" : active.apiFormat || "openai_chat",
        authType: isNineRouterConfig ? "bearer" : active.authType || "bearer",
        fullUrl: Boolean(active.fullUrl),
        modelsUrl: active.modelsUrl || "",
        customUserAgent: active.customUserAgent || "",
        scene: active.scene || scene,
      });
      syncState.modelOptions =
        normalizedProviderName === "OpenCode Zen"
          ? [...openCodeFreeModels]
          : normalizedProviderName === "9Router OpenCode Free"
            ? [...nineRouterOpenCodeFreeModels]
          : normalizedProviderName === "OpenRouter Free"
            ? [...openRouterFreeModels]
          : normalizedProviderName === "DeepSeek 官方" || /api\.deepseek\.com/i.test(active.baseUrl || "")
            ? [...deepSeekOfficialModels]
          : [];
      syncState.lastSyncMessage = scene === "meeting_deck" ? "已读取 PPT 生成专用模型配置。" : "已读取当前入口模型配置。";
    } catch {
      syncState.lastSyncMessage = "未读取到后端配置，已回退到本地缓存。";
    }
  }

  return {
    aiOutput,
    configPreview,
    currentAction,
    modelConfig,
    paper,
    selectedSource,
    syncState,
    applyPreset,
    clearModelFeedback,
    importOpenCodeModels,
    hydrateFromBackend,
    importPaper,
    importPaperFromSearch,
    fetchModelList,
    loadPaper,
    saveModelConfig,
    testModelChat,
    testModelConfig,
    selectSource,
    setAction,
  };
});
