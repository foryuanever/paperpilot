import { computed, reactive, watch } from "vue";
import { defineStore } from "pinia";
import { paperpilotApi } from "../services/paperpilotApi";

const STORAGE_KEY = "paperpilot-usage";

function readJson(key, fallback) {
  const raw = localStorage.getItem(key);
  if (!raw) return fallback;
  try {
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

const defaultState = {
  planId: "pro",
  planName: "Pro 深度阅读",
  tokenQuota: 0,
  tokenUsed: 0,
  tokenRemaining: 0,
  resetAt: "",
  promptTokens: 0,
  completionTokens: 0,
  weekTokens: 0,
  estimatedCost: 0,
  totalRequests: 0,
  todayRequests: 0,
  todayTokens: 0,
  rpm: 0,
  tpm: 0,
  mpm: 0,
  currentMinuteCost: 0,
  dailyUsage: [],
  modelBreakdown: [],
  sceneBreakdown: [],
  actionBreakdown: [],
  recentCalls: [],
};

export const useUsageStore = defineStore("usage", () => {
  const state = reactive(readJson(STORAGE_KEY, defaultState));

  watch(state, (value) => localStorage.setItem(STORAGE_KEY, JSON.stringify(value)), { deep: true });

  const tokenRemaining = computed(() => Math.max(0, state.tokenQuota - state.tokenUsed));
  const usagePercent = computed(() => {
    if (!state.tokenQuota) return 0;
    return Math.min(100, Math.round((state.tokenUsed / state.tokenQuota) * 100));
  });

  async function fetchSummary() {
    const summary = await paperpilotApi.getUsageSummary();
    Object.assign(state, defaultState, summary || {});
  }

  function applyPlan(plan) {
    state.planId = plan.id;
    state.planName = plan.name;
    state.tokenQuota = plan.tokenQuota;
    if (state.tokenUsed > plan.tokenQuota) {
      state.tokenUsed = Math.floor(plan.tokenQuota * 0.1);
    }
  }

  function recordUsage(tokens, meta = {}) {
    state.tokenUsed = Math.min(state.tokenQuota, state.tokenUsed + tokens);
    if (meta.action) {
      state.recentCalls.unshift({
        time: "刚刚",
        action: meta.action,
        tokens,
        paper: meta.paper || "当前论文",
      });
      state.recentCalls = state.recentCalls.slice(0, 8);
    }
  }

  return {
    state,
    tokenRemaining,
    usagePercent,
    applyPlan,
    fetchSummary,
    recordUsage,
  };
});
