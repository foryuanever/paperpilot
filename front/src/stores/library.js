import { computed, reactive, ref, watch } from "vue";
import { defineStore } from "pinia";
import { paperpilotApi } from "../services/paperpilotApi";

const STORAGE_KEY = "paperpilot-library";

function readJson(key, fallback) {
  const raw = localStorage.getItem(key);
  if (!raw) return fallback;
  try {
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

const defaultDocuments = [];
function normalizeJournalTags(tags) {
  return Array.from(new Set((Array.isArray(tags) ? tags : [])
    .map(tag => String(tag || "").trim().replace(/\s+/g, " "))
    .filter(tag => tag && tag.length <= 32)));
}

function sanitizeTitleFields(paper = {}) {
  const title = String(paper.title || "").trim();
  const combined = [
    paper.source,
    paper.importSource,
    paper.sourceUrl,
    paper.paperUrl,
    title,
  ].join(" ").toLowerCase();
  const poisonedScienceDirect =
    combined.includes("pdf.sciencedirectassets.com") &&
    (combined.includes("las vegas sands") ||
      combined.includes("unknown registrants") ||
      combined.includes("gaming law review") ||
      /www\.wn\d+\.com/.test(combined));
  const urlTitle =
    /^(https?:\/\/|www\.)/i.test(title) ||
    /\.(pdf|html?)(\?|#|$)/i.test(title) ||
    title.includes("/uploads/");
  return poisonedScienceDirect || urlTitle ? "元数据待补全" : title || "元数据待补全";
}

function normalizeStoredState(value) {
  const documents = Array.isArray(value?.documents) ? value.documents : defaultDocuments;
  const seen = new Set();
  const cleanDocuments = [];
  for (const document of documents) {
    const key = [document.sourceUrl, document.paperUrl, document.importSource, document.title]
      .map((item) => String(item || "").trim().toLowerCase())
      .filter(Boolean)
      .join("|");
    if (key && seen.has(key)) continue;
    if (key) seen.add(key);
    cleanDocuments.push({
      ...document,
      title: sanitizeTitleFields(document),
      source: String(document.source || "").toLowerCase().includes("gaming law review")
        ? "ScienceDirect PDF 资源"
        : document.source,
    });
  }
  return {
    activeDocumentId: value?.activeDocumentId || cleanDocuments[0]?.id || "",
    documents: cleanDocuments,
  };
}

export const useLibraryStore = defineStore("library", () => {
  const state = reactive(
    normalizeStoredState(readJson(STORAGE_KEY, {
      activeDocumentId: "",
      documents: defaultDocuments,
    })),
  );

  const openTabIds = ref(readJson("paperpilot-open-tabs", []));

  watch(
    openTabIds,
    (val) => {
      localStorage.setItem("paperpilot-open-tabs", JSON.stringify(val));
    },
    { deep: true },
  );

  watch(
    state,
    (value) => {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
    },
    { deep: true },
  );

  const activeDocument = computed(
    () => state.documents.find((item) => item.id === state.activeDocumentId) || state.documents[0] || null,
  );

  const openTabs = computed(() => {
    return openTabIds.value
      .map((id) => state.documents.find((doc) => doc.id === id))
      .filter(Boolean);
  });

  function addDocument(document) {
    if (!state.documents.some((item) => item.id === document.id || item.title === document.title)) {
      state.documents.unshift(document);
    }
    setActiveDocument(document.id);
  }

  function replaceDocuments(documents) {
    state.documents = documents;
    if (!documents.some((item) => item.id === state.activeDocumentId)) {
      state.activeDocumentId = documents[0]?.id || "";
    }
  }

  function setActiveDocument(id) {
    state.activeDocumentId = id;
    if (id && !openTabIds.value.includes(id)) {
      openTabIds.value.push(id);
    }
  }

  function closeTab(id) {
    openTabIds.value = openTabIds.value.filter((tabId) => tabId !== id);
    if (state.activeDocumentId === id) {
      const nextId = openTabIds.value[openTabIds.value.length - 1] || "";
      state.activeDocumentId = nextId;
      return nextId;
    }
    return state.activeDocumentId;
  }

  function closeAllTabs() {
    openTabIds.value = [];
    state.activeDocumentId = "";
  }

  function updateProgress(id, progress) {
    const target = state.documents.find((item) => item.id === id);
    if (target) {
      target.progress = progress;
    }
  }

  function updateDocument(id, patch) {
    const target = state.documents.find((item) => item.id === id);
    if (target) {
      Object.assign(target, patch);
    }
  }

  async function hydrateLibrary(params = {}) {
    const rows = await paperpilotApi.getLibraryPapers(params);
    replaceDocuments(rows.map(normalizeBackendPaper));
  }

  async function persistDocumentPatch(id, patch) {
    const target = state.documents.find((item) => item.id === id);
    if (!target?.workspaceId) {
      updateDocument(id, patch);
      return;
    }
    const next = await paperpilotApi.updateLibraryPaper(target.workspaceId, patch);
    updateDocument(id, normalizeBackendPaper(next));
  }

  async function deleteDocument(id) {
    const target = state.documents.find((item) => item.id === id);
    if (target?.workspaceId) {
      try {
        await paperpilotApi.deleteLibraryPaper(target.workspaceId);
      } catch (err) {
        console.warn("Backend delete failed, falling back to local-only delete", err);
      }
    }
    state.documents = state.documents.filter((item) => item.id !== id);
    if (state.activeDocumentId === id) {
      state.activeDocumentId = state.documents[0]?.id || "";
    }
  }

  function normalizeBackendPaper(paper) {
    const pdfUrl = paperpilotApi.isLikelyPdfUrl(paper.paperUrl) ? paper.paperUrl : "";
    const title = sanitizePaperTitle(paper);
    return {
      id: paper.workspaceId,
      workspaceId: paper.workspaceId,
      title,
      source: paper.source,
      authors: paper.authors,
      progress: paper.progress,
      importance: paper.importance,
      note: paper.note,
      journalTags: normalizeJournalTags(paper.journalTags),
      venueType: paper.venueType || "期刊",
      venueRanking: paper.venueRanking || "JCR --",
      publishYear: paper.publishYear,
      readAt: paper.readAt,
      uploadedAt: paper.uploadedAt,
      paperUrl: paper.paperUrl,
      sourceUrl: paper.sourceUrl || "",
      importSource: paper.importSource || paper.source || "",
      pdfUrl,
      abstract: paper.abstractText || "",
    };
  }

  function sanitizePaperTitle(paper = {}) {
    return sanitizeTitleFields(paper);
  }

  return {
    activeDocument,
    addDocument,
    closeAllTabs,
    closeTab,
    deleteDocument,
    hydrateLibrary,
    normalizeBackendPaper,
    openTabs,
    persistDocumentPatch,
    replaceDocuments,
    setActiveDocument,
    state,
    updateDocument,
    updateProgress,
  };
});
