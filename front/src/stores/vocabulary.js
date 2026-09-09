import { defineStore } from "pinia";
import { apiClient } from "../services/apiClient";
import { useAuthStore } from "./auth";
import { lookupOxfordDefinition } from "../utils/oxfordDict";

const STORAGE_PREFIX = "paperpilot_vocabulary_";
const DEMO_WORDS = new Set([
  "ablation", "preponderance", "ubiquitous", "parsimonious",
  "salient", "dichotomy", "orthogonal", "stochastic"
]);

function isDemoVocabulary(item) {
  const id = String(item?.id || "");
  const word = String(item?.word || "").trim().toLowerCase();
  const paperId = String(item?.paperId || "");
  const paperTitle = String(item?.paperTitle || "");
  return id.startsWith("vocab_demo_") ||
    paperId === "demo-paper-1" ||
    (paperTitle === "Attention Is All You Need (Vaswani et al.)" && DEMO_WORDS.has(word));
}

export const useVocabularyStore = defineStore("vocabulary", {
  state: () => ({
    items: [],
    loading: false,
    activeFilter: {
      keyword: "",
      paperId: "",
      masteryLevel: "all", // "all", 0, 1, 2
      sortBy: "date_desc", // "date_desc", "date_asc", "alpha_asc", "alpha_desc", "mastery_asc"
    },
    selectedWordId: null,
    currentViewMode: "list", // "list", "split", "flashcard"
    flashcardIndex: 0,
    flashcardFlipped: false,
    currentPage: 1,
    pageSize: 12, // 12, 24, 48
  }),

  getters: {
    storageKey() {
      const authStore = useAuthStore();
      const uid = authStore.session.user?.userId || authStore.profile.email || "guest";
      return `${STORAGE_PREFIX}${uid}`;
    },

    filteredItems(state) {
      let list = [...state.items];
      const { keyword, paperId, masteryLevel, sortBy } = state.activeFilter;

      if (keyword && keyword.trim()) {
        const q = keyword.trim().toLowerCase();
        list = list.filter(item =>
          String(item.word || "").toLowerCase().includes(q) ||
          String(item.meaningCn || "").toLowerCase().includes(q) ||
          String(item.paperTitle || "").toLowerCase().includes(q) ||
          String(item.contextSentence || "").toLowerCase().includes(q)
        );
      }

      if (paperId && paperId !== "all") {
        list = list.filter(item => String(item.paperId || "") === String(paperId));
      }

      if (masteryLevel !== "all" && masteryLevel !== null && masteryLevel !== undefined) {
        const levelNum = Number(masteryLevel);
        list = list.filter(item => Number(item.masteryLevel || 0) === levelNum);
      }

      // Sort
      if (sortBy === "alpha_asc") {
        list.sort((a, b) => String(a.word || "").localeCompare(String(b.word || "")));
      } else if (sortBy === "alpha_desc") {
        list.sort((a, b) => String(b.word || "").localeCompare(String(a.word || "")));
      } else if (sortBy === "mastery_asc") {
        list.sort((a, b) => (a.masteryLevel || 0) - (b.masteryLevel || 0));
      } else if (sortBy === "date_asc") {
        list.sort((a, b) => new Date(a.createdAt || 0) - new Date(b.createdAt || 0));
      } else {
        // date_desc
        list.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
      }

      return list;
    },

    paginatedItems(state) {
      const all = this.filteredItems;
      const start = (state.currentPage - 1) * state.pageSize;
      return all.slice(start, start + state.pageSize);
    },

    totalPages(state) {
      return Math.max(1, Math.ceil(this.filteredItems.length / state.pageSize));
    },

    stats(state) {
      const all = state.items;
      const total = all.length;
      const newWords = all.filter(v => !v.masteryLevel || v.masteryLevel === 0).length;
      const familiarWords = all.filter(v => v.masteryLevel === 1).length;
      const masteredWords = all.filter(v => v.masteryLevel === 2).length;

      const todayStr = new Date().toISOString().slice(0, 10);
      const todayAdded = all.filter(v => String(v.createdAt || "").slice(0, 10) === todayStr).length;

      const oneDayAgo = Date.now() - 24 * 60 * 60 * 1000;
      const needsReview = all.filter(v => {
        if (v.masteryLevel === 2) return false;
        if (!v.lastReviewedAt) return true;
        return new Date(v.lastReviewedAt).getTime() < oneDayAgo;
      }).length;

      const masteryRate = total > 0 ? Math.round((masteredWords / total) * 100) : 0;

      return {
        total,
        newWords,
        familiarWords,
        masteredWords,
        todayAdded,
        needsReview,
        masteryRate
      };
    },

    paperSources(state) {
      const map = new Map();
      for (const item of state.items) {
        if (item.paperTitle && item.paperId) {
          if (!map.has(item.paperId)) {
            map.set(item.paperId, { id: item.paperId, title: item.paperTitle, count: 0 });
          }
          map.get(item.paperId).count++;
        }
      }
      return Array.from(map.values());
    },

    selectedWord(state) {
      if (!state.selectedWordId) {
        return this.paginatedItems[0] || state.items[0] || null;
      }
      return state.items.find(w => w.id === state.selectedWordId) || this.paginatedItems[0] || state.items[0] || null;
    }
  },

  actions: {
    setPage(page) {
      this.currentPage = Math.max(1, Math.min(this.totalPages, Number(page) || 1));
    },

    setPageSize(size) {
      this.pageSize = Number(size) || 12;
      this.currentPage = 1;
    },

    loadLocal() {
      try {
        const raw = localStorage.getItem(this.storageKey);
        if (raw) {
          const parsed = JSON.parse(raw);
          if (Array.isArray(parsed)) {
            this.items = parsed;
          }
        }
      } catch (err) {
        console.warn("Failed to load local vocabulary", err);
      }
    },

    persistLocal() {
      try {
        localStorage.setItem(this.storageKey, JSON.stringify(this.items));
      } catch (err) {
        console.warn("Failed to persist local vocabulary", err);
      }
    },

    async init() {
      this.loadLocal();
      // 清理历史版本写入浏览器的演示词，避免新用户看到同一批示例。
      // 真实词汇仍保留，并由后端按当前登录用户同步。
      const before = this.items.length;
      this.items = this.items.filter(item => !isDemoVocabulary(item));
      if (this.items.length !== before) this.persistLocal();
      this.repairPendingDefinitions();
      this.syncFromBackend();
    },

    async repairPendingDefinitions() {
      let changed = false;
      for (const item of this.items) {
        if (!item.meaningCn || item.meaningCn.includes("正在解析") || item.meaningCn.includes("未解析")) {
          const dict = await lookupOxfordDefinition(item.word, item.contextSentence);
          item.phonetic = dict.phonetic || item.phonetic;
          item.partOfSpeech = dict.partOfSpeech || item.partOfSpeech;
          item.oxfordLevel = dict.oxfordLevel || item.oxfordLevel;
          item.meaningCn = dict.meaningCn;
          item.meaningEn = dict.meaningEn || item.meaningEn;
          if (!item.collocations || item.collocations.length === 0) item.collocations = dict.collocations;
          if (!item.academicExamples || item.academicExamples.length === 0) item.academicExamples = dict.academicExamples;
          if (!item.etymology) item.etymology = dict.etymology;
          if (!item.synonyms || item.synonyms.length === 0) item.synonyms = dict.synonyms;
          changed = true;
        }
      }
      if (changed) {
        this.persistLocal();
      }
    },

    async preloadInitialDemoWords() {
      // 保留旧 action 名称以兼容外部调用，但不再自动生成演示词。
      this.items = [];
      this.persistLocal();
    },

    async syncFromBackend() {
      try {
        const authStore = useAuthStore();
        if (!authStore.session.isAuthenticated) return;
        const res = await apiClient.get("/api/vocabulary");
        if (Array.isArray(res.data) && res.data.length > 0) {
          const map = new Map();
          res.data.filter(item => !isDemoVocabulary(item)).forEach(item => {
            const normalized = this.normalizeEntity(item);
            map.set(normalized.id, normalized);
          });
          this.items.filter(item => !isDemoVocabulary(item)).forEach(item => {
            if (!map.has(item.id)) {
              map.set(item.id, item);
            }
          });
          this.items = Array.from(map.values()).sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
          this.repairPendingDefinitions();
          this.persistLocal();
        }
      } catch (err) {
        console.debug("Backend vocabulary sync skipped:", err.message);
      }
    },

    normalizeEntity(entity) {
      let collocations = [];
      let academicExamples = [];
      let synonyms = [];
      let antonyms = [];
      try { if (entity.collocationsJson) collocations = JSON.parse(entity.collocationsJson); } catch {}
      try { if (entity.academicExamplesJson) academicExamples = JSON.parse(entity.academicExamplesJson); } catch {}
      try { if (entity.synonymsJson) synonyms = JSON.parse(entity.synonymsJson); } catch {}
      try { if (entity.antonymsJson) antonyms = JSON.parse(entity.antonymsJson); } catch {}

      return {
        id: entity.id,
        word: entity.word,
        phonetic: entity.phonetic || `/${entity.word?.toLowerCase()}/`,
        audioUrl: entity.audioUrl || "",
        partOfSpeech: entity.partOfSpeech || "n.",
        oxfordLevel: entity.oxfordLevel || "Academic",
        meaningCn: entity.meaningCn || "",
        meaningEn: entity.meaningEn || "",
        contextSentence: entity.contextSentence || "",
        contextTranslation: entity.contextTranslation || "",
        paperId: entity.paperId || "",
        paperTitle: entity.paperTitle || "",
        sectionName: entity.sectionName || "",
        collocations: Array.isArray(collocations) ? collocations : [],
        academicExamples: Array.isArray(academicExamples) ? academicExamples : [],
        synonyms: Array.isArray(synonyms) ? synonyms : [],
        antonyms: Array.isArray(antonyms) ? antonyms : [],
        etymology: entity.etymology || "",
        masteryLevel: entity.masteryLevel ?? 0,
        reviewCount: entity.reviewCount ?? 0,
        lastReviewedAt: entity.lastReviewedAt || null,
        createdAt: entity.createdAt || new Date().toISOString(),
        updatedAt: entity.updatedAt || new Date().toISOString()
      };
    },

    async addWord({ word, contextSentence = "", paperId = "", paperTitle = "", sectionName = "" }) {
      if (!word || !word.trim()) return null;
      const cleanWord = word.trim().replace(/^[^a-zA-Z]+|[^a-zA-Z]+$/g, "");
      if (!cleanWord) return null;

      const lower = cleanWord.toLowerCase();
      const existingIdx = this.items.findIndex(item => item.word.toLowerCase() === lower);

      // 即时秒级权威牛津词典解析（零等待、零卡顿）
      const dict = await lookupOxfordDefinition(cleanWord, contextSentence);

      let item;
      let isNew = false;

      if (existingIdx !== -1) {
        item = this.items[existingIdx];
        if (contextSentence && !item.contextSentence) item.contextSentence = contextSentence;
        if (paperId && !item.paperId) {
          item.paperId = paperId;
          item.paperTitle = paperTitle;
        }
        if (!item.meaningCn || item.meaningCn.includes("正在解析")) {
          item.meaningCn = dict.meaningCn;
          item.meaningEn = dict.meaningEn;
          item.phonetic = dict.phonetic;
          item.partOfSpeech = dict.partOfSpeech;
          item.collocations = dict.collocations;
          item.academicExamples = dict.academicExamples;
          item.etymology = dict.etymology;
        }
        item.updatedAt = new Date().toISOString();
      } else {
        isNew = true;
        item = {
          id: `vocab_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
          word: cleanWord,
          phonetic: dict.phonetic || `/${cleanWord.toLowerCase()}/`,
          partOfSpeech: dict.partOfSpeech || "adj./n.",
          oxfordLevel: dict.oxfordLevel || "Academic",
          meaningCn: dict.meaningCn,
          meaningEn: dict.meaningEn,
          contextSentence: contextSentence || "",
          contextTranslation: dict.contextTranslation || dict.academicExamples?.[0]?.cn || "",
          paperId: paperId || "",
          paperTitle: paperTitle || "",
          sectionName: sectionName || "",
          collocations: dict.collocations || [],
          academicExamples: dict.academicExamples || [],
          synonyms: dict.synonyms || [],
          antonyms: dict.antonyms || [],
          etymology: dict.etymology || "",
          masteryLevel: 0,
          reviewCount: 0,
          lastReviewedAt: null,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        };
        this.items.unshift(item);
      }

      this.persistLocal();
      this.selectedWordId = item.id;
      this.currentPage = 1;

      // 异步同步到后端
      this.syncSaveToBackend(item);

      return { word: cleanWord, isNew, item };
    },

    async syncSaveToBackend(item) {
      try {
        const payload = {
          id: item.id,
          word: item.word,
          phonetic: item.phonetic,
          audioUrl: item.audioUrl,
          partOfSpeech: item.partOfSpeech,
          oxfordLevel: item.oxfordLevel,
          meaningCn: item.meaningCn,
          meaningEn: item.meaningEn,
          contextSentence: item.contextSentence,
          contextTranslation: item.contextTranslation,
          paperId: item.paperId,
          paperTitle: item.paperTitle,
          sectionName: item.sectionName,
          collocationsJson: JSON.stringify(item.collocations || []),
          academicExamplesJson: JSON.stringify(item.academicExamples || []),
          synonymsJson: JSON.stringify(item.synonyms || []),
          antonymsJson: JSON.stringify(item.antonyms || []),
          etymology: item.etymology,
          masteryLevel: item.masteryLevel
        };
        await apiClient.post("/api/vocabulary", payload);
      } catch (err) {
        console.debug("Save vocabulary to backend queued/skipped:", err.message);
      }
    },

    async removeWord(id) {
      const idx = this.items.findIndex(w => w.id === id);
      if (idx !== -1) {
        this.items.splice(idx, 1);
        this.persistLocal();
        if (this.selectedWordId === id) {
          this.selectedWordId = this.paginatedItems[0]?.id || this.items[0]?.id || null;
        }
        if (this.currentPage > this.totalPages) {
          this.currentPage = this.totalPages;
        }
      }
      try {
        await apiClient.delete(`/api/vocabulary/${id}`);
      } catch (err) {
        console.debug("Delete from backend skipped:", err.message);
      }
    },

    async updateMastery(id, level) {
      const item = this.items.find(w => w.id === id);
      if (!item) return;
      item.masteryLevel = Math.max(0, Math.min(2, Number(level)));
      item.updatedAt = new Date().toISOString();
      this.persistLocal();
      try {
        await apiClient.patch(`/api/vocabulary/${id}/mastery`, { masteryLevel: item.masteryLevel });
      } catch (err) {
        console.debug("Update mastery backend skipped:", err.message);
      }
    },

    async recordReview(id, grade) {
      const item = this.items.find(w => w.id === id);
      if (!item) return;
      item.reviewCount = (item.reviewCount || 0) + 1;
      item.lastReviewedAt = new Date().toISOString();
      if (grade === "forgot") {
        item.masteryLevel = 0;
      } else if (grade === "vague") {
        item.masteryLevel = 1;
      } else if (grade === "mastered") {
        item.masteryLevel = 2;
      }
      this.persistLocal();
      try {
        await apiClient.post(`/api/vocabulary/${id}/review`, { masteryLevel: item.masteryLevel });
      } catch (err) {
        console.debug("Record review backend skipped:", err.message);
      }
    },

    exportToAnki() {
      const lines = this.filteredItems.map(w => {
        const front = `${w.word} ${w.phonetic || ""}<br><br><i>${w.contextSentence || ""}</i>`;
        const back = `<b>${w.meaningCn}</b><br><small>${w.meaningEn || ""}</small><br><br><b>【学术搭配】</b><br>${(w.collocations || []).map(c => `${c.en} : ${c.cn}`).join("<br>")}<br><br><b>【词根助记】</b><br>${w.etymology || ""}`;
        return `${front.replace(/\t/g, " ")}\t${back.replace(/\t/g, " ")}\t${w.paperTitle || ""}`;
      });
      const content = `#separator:tab\n#html:true\n#tags:PaperSolver Vocabulary\n` + lines.join("\n");
      this.downloadFile(content, `PaperSolver-Vocabulary-${new Date().toISOString().slice(0, 10)}.tsv`, "text/tab-separated-values");
    },

    exportToCsv() {
      const headers = ["单词", "音标", "词性", "牛津中文释义", "英文释义", "论文语境例句", "例句翻译", "来源文献", "掌握度"];
      const rows = this.filteredItems.map(w => [
        `"${(w.word || "").replace(/"/g, '""')}"`,
        `"${(w.phonetic || "").replace(/"/g, '""')}"`,
        `"${(w.partOfSpeech || "").replace(/"/g, '""')}"`,
        `"${(w.meaningCn || "").replace(/"/g, '""')}"`,
        `"${(w.meaningEn || "").replace(/"/g, '""')}"`,
        `"${(w.contextSentence || "").replace(/"/g, '""')}"`,
        `"${(w.contextTranslation || "").replace(/"/g, '""')}"`,
        `"${(w.paperTitle || "").replace(/"/g, '""')}"`,
        w.masteryLevel === 2 ? "已掌握" : w.masteryLevel === 1 ? "熟悉" : "待学习"
      ]);
      const csvContent = "\uFEFF" + [headers.join(","), ...rows.map(r => r.join(","))].join("\n");
      this.downloadFile(csvContent, `PaperSolver-Vocabulary-${new Date().toISOString().slice(0, 10)}.csv`, "text/csv;charset=utf-8");
    },

    downloadFile(content, filename, type) {
      const blob = new Blob([content], { type });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }
  }
});
