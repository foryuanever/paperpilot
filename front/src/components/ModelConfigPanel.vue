<template>
  <section class="reader-panel">
    <div class="reader-panel-header">
      <div>
        <span class="section-eyebrow">全局 AI 路由</span>
        <h3>免费模型与中转站</h3>
        <p class="panel-intro">只维护一条全站模型路由。9Router 默认只开放 OpenCode Free 免费模型，避免误选慢模型。</p>
      </div>
      <span class="badge">仅管理员可配置</span>
    </div>

    <div class="route-switch">
      <button
        type="button"
        class="route-option"
        :class="{ active: isOpenCode }"
        @click="$emit('import-opencode')"
      >
        <strong>OpenCode Zen</strong>
        <span>官方免费模型，需要 Zen Key</span>
      </button>
      <button
        type="button"
        class="route-option"
        :class="{ active: isNineRouter }"
        @click="$emit('apply-preset', '9router-free')"
      >
        <strong>9Router OpenCode Free</strong>
        <span>只显示 oc/ 免费模型，默认极速测试</span>
      </button>
      <button
        type="button"
        class="route-option"
        :class="{ active: isOpenRouter }"
        @click="$emit('apply-preset', 'openrouter-free')"
      >
        <strong>OpenRouter Free</strong>
        <span>免费模型更稳，需要 OpenRouter Key</span>
      </button>
      <button
        type="button"
        class="route-option"
        :class="{ active: isRelay }"
        @click="$emit('apply-preset', 'relay')"
      >
        <strong>自定义中转站</strong>
        <span>填写地址和 Key，自动获取支持模型</span>
      </button>
    </div>

    <div v-if="isOpenCode" class="opencode-notice">
      <div>
        <strong>已内置 {{ modelOptions.length || 5 }} 个 OpenCode Zen 免费模型</strong>
        <span>模型价格为 0，但 OpenCode Zen 官方接口仍需要独立的 Zen Key。</span>
      </div>
      <button type="button" class="btn btn-secondary" @click="$emit('import-opencode')">重新导入</button>
    </div>
    <div v-else-if="isNineRouter" class="opencode-notice router-notice">
      <div>
        <strong>通过 9Router 调用 OpenCode Free 免费模型</strong>
        <span>9Router 隧道可能间歇性返回 HTTP 530 / 1016；若频繁失败，建议切换 OpenRouter Free 或自定义稳定中转。</span>
      </div>
      <button type="button" class="btn btn-secondary" @click="$emit('apply-preset', '9router-free')">恢复推荐配置</button>
    </div>
    <div v-else-if="isOpenRouter" class="opencode-notice openrouter-notice">
      <div>
        <strong>通过 OpenRouter 调用免费模型</strong>
        <span>模型 ID 带 `:free` 的模型免费调用；需要在 OpenRouter 获取 Key 后填写到下方。</span>
      </div>
      <button type="button" class="btn btn-secondary" @click="$emit('apply-preset', 'openrouter-free')">恢复推荐配置</button>
    </div>

    <div class="form-grid compact-grid">
      <div v-if="isRelay" class="form-group">
        <label>中转站名称</label>
        <input :value="modelConfig.providerName" @input="updateField('providerName', $event)" placeholder="例如：我的 API 中转站" />
      </div>
      <div class="form-group" :class="{ 'full-row': !isRelay }">
        <label>{{ baseUrlLabel }}</label>
        <input
          :value="modelConfig.baseUrl"
          :readonly="isOpenCode"
          @input="updateField('baseUrl', $event)"
          :placeholder="baseUrlPlaceholder"
        />
        <small v-if="isNineRouter" class="field-help">当前使用 HTTPS 隧道；请填写创建该隧道的 9Router 实例生成的 API Key。</small>
        <small v-else-if="isOpenRouter" class="field-help">OpenRouter 官方 OpenAI 兼容地址；免费模型通常更稳定，但仍需要账号 Key。</small>
        <small v-else-if="isRelay" class="field-help">兼容 OpenAI Chat Completions 的中转站均可，系统会自动处理 `/v1` 路径。</small>
      </div>
      <div class="form-group">
        <label>{{ keyLabel }}</label>
        <input :value="modelConfig.apiKey" type="password" autocomplete="off" @input="updateField('apiKey', $event)" placeholder="粘贴 API Key" />
        <small class="field-help">密钥只保存在本站后端；留空保存时继续使用原密钥。</small>
      </div>
      <div class="form-group">
        <label>默认模型</label>
        <select v-if="modelOptions.length" :value="modelConfig.modelName" @change="updateField('modelName', $event)">
          <option v-for="model in modelOptions" :key="model.id" :value="model.id">
            {{ isNineRouter ? `${model.name || model.id} · ${model.id}` : model.name || model.id }}
          </option>
        </select>
        <input v-else :value="modelConfig.modelName" @input="updateField('modelName', $event)" placeholder="先点击获取模型" />
        <small v-if="modelOptions.length" class="field-help">已加载 {{ modelOptions.length }} 个可用模型。</small>
      </div>

      <div class="reader-toolbar full-row">
        <button
          v-if="isRelay || isNineRouter"
          type="button"
          class="btn btn-secondary"
          :disabled="fetchingModels"
          @click="$emit('fetch-models')"
        >
          {{ fetchingModels ? "获取中..." : isNineRouter ? "加载免费模型" : "获取中转站模型" }}
        </button>
        <button type="button" class="btn btn-secondary" :disabled="testing" @click="$emit('test-model')">
          {{ testing ? "测试中..." : "测试连接" }}
        </button>
        <button type="button" class="btn btn-primary" :disabled="saving" @click="$emit('save-model')">
          {{ saving ? "保存中..." : "保存并全站启用" }}
        </button>
      </div>

      <div v-if="testResult" class="test-result full-row" :class="{ success: testResult.success, error: !testResult.success }">
        <strong>{{ testResult.success ? "配置可用" : "配置失败" }}</strong>
        <span>{{ testResult.message }}{{ usageText(testResult.usage) }}</span>
      </div>
      <div v-if="saveResult" class="test-result full-row" :class="{ success: saveResult.success, error: !saveResult.success }">
        <strong>{{ saveResult.success ? "启用成功" : "保存失败" }}</strong>
        <span>{{ saveResult.message }}</span>
      </div>
    </div>

    <div class="chat-tester">
      <div class="chat-tester-heading">
        <div>
          <h4>模型对话测试</h4>
          <p>使用当前表单中的地址、Key 和所选模型发送真实问题，无需先保存。</p>
        </div>
        <span class="live-mark">实时调用</span>
      </div>
      <div class="chat-composer">
        <textarea
          v-model="chatPrompt"
          rows="3"
          maxlength="1000"
          placeholder="例如：请用一句话说明什么是 Transformer。"
          @keydown.meta.enter="submitChat"
          @keydown.ctrl.enter="submitChat"
        ></textarea>
        <button type="button" class="btn btn-primary" :disabled="chatTesting || !chatPrompt.trim()" @click="submitChat">
          {{ chatTesting ? "模型回答中..." : "发送测试" }}
        </button>
      </div>
      <div v-if="chatReply" class="chat-response" :class="{ error: !chatReply.success }">
        <div class="response-meta">
          <strong>{{ chatReply.success ? "模型回复" : "调用失败" }}</strong>
          <span v-if="chatReply.modelName">{{ chatReply.modelName }}{{ usageText(chatReply.usage) }}</span>
        </div>
        <p>{{ chatReply.success ? chatReply.content : chatReply.message }}</p>
      </div>
      <p v-else class="chat-hint">输入任意问题进行测试。支持 `Ctrl/Command + Enter` 快速发送。</p>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from "vue";

const emit = defineEmits([
  "apply-preset",
  "import-opencode",
  "save-model",
  "test-model",
  "fetch-models",
  "chat-test",
  "update:model-config",
]);
const props = defineProps({
  configPreview: { type: String, required: true },
  modelConfig: { type: Object, required: true },
  saving: { type: Boolean, default: false },
  testing: { type: Boolean, default: false },
  fetchingModels: { type: Boolean, default: false },
  modelOptions: { type: Array, default: () => [] },
  testResult: { type: Object, default: null },
  saveResult: { type: Object, default: null },
  chatTesting: { type: Boolean, default: false },
  chatReply: { type: Object, default: null },
});

const isOpenCode = computed(() => props.modelConfig.providerName === "OpenCode Zen");
const isNineRouter = computed(() =>
  ["9Router OpenCode Free", "9Router 模型路由", "9Router 免费路由"].includes(props.modelConfig.providerName),
);
const isOpenRouter = computed(() => props.modelConfig.providerName === "OpenRouter Free");
const isRelay = computed(() => !isOpenCode.value && !isNineRouter.value && !isOpenRouter.value);
const baseUrlLabel = computed(() => {
  if (isOpenCode.value) return "OpenCode Zen API 地址";
  if (isNineRouter.value) return "9Router Base URL";
  if (isOpenRouter.value) return "OpenRouter Base URL";
  return "中转站 Base URL";
});
const baseUrlPlaceholder = computed(() => {
  if (isOpenCode.value) return "https://opencode.ai/zen/v1";
  if (isNineRouter.value) return "https://rnr5845.abc-tunnel.us/v1";
  if (isOpenRouter.value) return "https://openrouter.ai/api/v1";
  return "例如：https://api.example.com/v1";
});
const keyLabel = computed(() => {
  if (isOpenCode.value) return "OpenCode Zen Key";
  if (isNineRouter.value) return "9Router Key";
  if (isOpenRouter.value) return "OpenRouter Key";
  return "中转站 Key";
});
const chatPrompt = ref("");

function updateField(field, event) {
  emit("update:model-config", { ...props.modelConfig, [field]: event.target.value });
}

function submitChat() {
  if (!chatPrompt.value.trim() || props.chatTesting) return;
  emit("chat-test", chatPrompt.value.trim());
}

function usageText(usage) {
  if (!usage || !usage.totalTokens) return "";
  return ` · 站内估算 ${Number(usage.totalTokens).toLocaleString()} Token`;
}
</script>

<style scoped>
.panel-intro { margin: 5px 0 0; color: #687488; font-size: 12px; }
.route-switch { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin: 18px 0 14px; }
.route-option { display: grid; gap: 4px; padding: 15px 16px; text-align: left; border: 1px solid #dfe5ee; border-radius: 12px; color: #344158; background: #fff; cursor: pointer; transition: border-color 180ms ease, background 180ms ease; }
.route-option:hover { border-color: #9fc1f7; }
.route-option.active { border-color: #1473e6; color: #075ee5; background: #f2f7ff; }
.route-option strong { font-size: 14px; }
.route-option span { color: #6e7a8d; font-size: 11px; }
.opencode-notice { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; padding: 12px 14px; border-radius: 10px; color: #285e4b; background: #eaf8f1; }
.opencode-notice div { display: grid; gap: 3px; }
.opencode-notice span { font-size: 11px; }
.router-notice { color: #205b76; background: #edf7fc; }
.openrouter-notice { color: #3f4e86; background: #f1f3ff; }
.compact-grid { align-items: start; }
.field-help { display: block; margin-top: 5px; color: #6e7a8d; font-size: 10px; line-height: 1.45; }
.full-row { grid-column: 1 / -1; }
.test-result { display: flex; gap: 8px; align-items: center; padding: 10px 12px; border-radius: 9px; font-size: 11px; }
.test-result.success { color: #087457; background: #e6f7f0; }
.test-result.error { color: #b33d42; background: #fff0f1; }
.chat-tester { margin-top: 20px; padding-top: 18px; border-top: 1px solid #e6eaf0; }
.chat-tester-heading, .response-meta { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.chat-tester-heading h4 { margin: 0; color: #172033; font-size: 15px; }
.chat-tester-heading p { margin: 4px 0 0; color: #687488; font-size: 11px; }
.live-mark { padding: 4px 8px; border-radius: 99px; color: #087457; background: #e6f7f0; font-size: 10px; font-weight: 700; }
.chat-composer { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: end; gap: 10px; margin-top: 14px; }
.chat-composer textarea { min-height: 82px; resize: vertical; }
.chat-response { margin-top: 12px; padding: 13px 14px; border-radius: 10px; color: #26344a; background: #f4f8ff; }
.chat-response.error { color: #a5373d; background: #fff0f1; }
.response-meta span { color: #687488; font-size: 10px; }
.chat-response p { margin: 9px 0 0; white-space: pre-wrap; line-height: 1.7; font-size: 12px; }
.chat-hint { margin: 10px 0 0; color: #7a8596; font-size: 10px; }
button:disabled { cursor: not-allowed; opacity: .55; }
@media (max-width: 720px) {
  .route-switch { grid-template-columns: 1fr; }
  .opencode-notice { align-items: flex-start; flex-direction: column; }
  .chat-composer { grid-template-columns: 1fr; }
}
</style>
