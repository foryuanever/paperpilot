<template>
  <section class="reader-panel relay-config-panel">
    <div class="reader-panel-header relay-config-head">
      <div>
        <span class="section-eyebrow">OpenAI Compatible Relay</span>
        <h3>第三方中转配置</h3>
        <p class="panel-intro">每个业务入口只保存自己的中转地址、Key 和模型名。接口按 OpenAI 格式调用，PPT 入口可切到 Responses 协议。</p>
      </div>
      <span class="badge">仅第三方中转</span>
    </div>

    <div class="relay-protocol-strip">
      <span>请求格式</span>
      <strong>{{ protocolLabel }}</strong>
      <small>{{ endpointPreview }}</small>
    </div>

    <div class="form-grid compact-grid">
      <div class="form-group">
        <label>中转站名称</label>
        <input :value="modelConfig.providerName" @input="updateField('providerName', $event)" placeholder="例如：NIM / OpenRouter / 自建中转" />
      </div>
      <div class="form-group">
        <label>Base URL</label>
        <input
          :value="modelConfig.baseUrl"
          @input="updateField('baseUrl', $event)"
          placeholder="例如：https://api.example.com/v1"
        />
        <small class="field-help">兼容 OpenAI 的中转地址即可，系统会自动处理 `/v1`、`/chat/completions` 或 `/responses` 路径。</small>
      </div>
      <div class="form-group">
        <label>中转站 Key</label>
        <input :value="modelConfig.apiKey" type="password" autocomplete="off" @input="updateField('apiKey', $event)" placeholder="粘贴 API Key" />
        <small class="field-help">密钥只保存在本站后端；留空保存时继续使用原密钥。</small>
      </div>
      <div class="form-group">
        <label>模型 ID</label>
        <select v-if="modelOptions.length" :value="modelConfig.modelName" @change="updateField('modelName', $event)">
          <option v-for="model in modelOptions" :key="model.id" :value="model.id">
            {{ model.name || model.id }}
          </option>
        </select>
        <input v-else :value="modelConfig.modelName" @input="updateField('modelName', $event)" placeholder="例如：gpt-5.4 / deepseek-v4-flash / qwen3..." />
        <small v-if="modelOptions.length" class="field-help">已加载 {{ modelOptions.length }} 个可用模型。</small>
      </div>
      <div class="form-group">
        <label>OpenAI 协议</label>
        <select :value="modelConfig.apiFormat || 'openai_chat'" @change="updateField('apiFormat', $event)">
          <option value="openai_chat">Chat Completions</option>
          <option value="openai_responses">Responses</option>
        </select>
        <small class="field-help">普通入口用 Chat Completions；PPT Agent 推荐 Responses。</small>
      </div>

      <div class="reader-toolbar full-row">
        <button
          type="button"
          class="btn btn-secondary"
          :disabled="fetchingModels"
          @click="$emit('fetch-models')"
        >
          {{ fetchingModels ? "获取中..." : "获取中转站模型" }}
        </button>
        <button type="button" class="btn btn-secondary" :disabled="testing" @click="$emit('test-model')">
          {{ testing ? "测试中..." : "测试连接" }}
        </button>
        <button type="button" class="btn btn-primary" :disabled="saving" @click="$emit('save-model')">
          {{ saving ? "保存中..." : "保存并启用此入口" }}
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

const protocolLabel = computed(() => props.modelConfig.apiFormat === "openai_responses" ? "OpenAI Responses" : "OpenAI Chat Completions");
const endpointPreview = computed(() => {
  const base = String(props.modelConfig.baseUrl || "<baseUrl>").replace(/\/+$/, "");
  return props.modelConfig.apiFormat === "openai_responses"
    ? `${base}/responses`
    : `${base}/chat/completions`;
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
.relay-config-panel { padding: 20px; }
.relay-config-head { align-items: flex-start; margin-bottom: 14px; }
.relay-protocol-strip {
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  margin: 0 0 16px;
  padding: 10px 12px;
  border: 1px solid #dbe6f4;
  border-radius: 12px;
  background: linear-gradient(135deg, #f8fbff, #f4f7fb);
}
.relay-protocol-strip span { color: #64748b; font-size: 11px; font-weight: 700; }
.relay-protocol-strip strong { color: #143c7d; font-size: 12px; }
.relay-protocol-strip small {
  min-width: 0;
  color: #64748b;
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
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
  .relay-protocol-strip { grid-template-columns: 1fr; }
  .chat-composer { grid-template-columns: 1fr; }
}
</style>
