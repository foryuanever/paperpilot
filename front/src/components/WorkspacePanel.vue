<template>
  <section class="reader-panel">
    <div class="reader-panel-header">
      <div>
        <span class="section-eyebrow">AI 助手</span>
        <h3>翻译 / 解析 / 汇总</h3>
      </div>
      <span class="badge badge-muted">{{ paper.source }} 工作区</span>
    </div>

    <div class="mini-metrics mb-16">
      <div class="mini-metric">
        <strong>{{ paper.source }}</strong>
        <span>来源站点</span>
      </div>
      <div class="mini-metric">
        <strong>{{ importedWorkspaceId || "本地" }}</strong>
        <span>工作区状态</span>
      </div>
      <div class="mini-metric">
        <strong>{{ currentActionLabel }}</strong>
        <span>当前模式</span>
      </div>
    </div>

    <div class="assistant-tabs">
      <button
        class="preset-chip"
        :class="{ active: currentAction === 'translate' }"
        @click="$emit('set-action', 'translate')"
      >
        学术翻译
      </button>
      <button
        class="preset-chip"
        :class="{ active: currentAction === 'analyze' }"
        @click="$emit('set-action', 'analyze')"
      >
        论文解析
      </button>
      <button
        class="preset-chip"
        :class="{ active: currentAction === 'summary' }"
        @click="$emit('set-action', 'summary')"
      >
        汇总综述
      </button>
    </div>

    <div class="code-block assistant-output">{{ aiOutput }}</div>
  </section>
</template>

<script setup>
import { computed } from "vue";

defineEmits(["set-action"]);

const props = defineProps({
  aiOutput: { type: String, required: true },
  currentAction: { type: String, required: true },
  importedWorkspaceId: { type: String, default: "" },
  paper: { type: Object, required: true },
});

const currentActionLabel = computed(() => {
  const map = {
    translate: "翻译",
    analyze: "解析",
    summary: "综述",
  };
  return map[props.currentAction];
});
</script>
