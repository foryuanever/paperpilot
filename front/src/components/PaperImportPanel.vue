<template>
  <section class="reader-panel">
    <div class="reader-panel-header">
      <div>
        <span class="section-eyebrow">导入文献</span>
        <h3>在线导入论文</h3>
      </div>
      <span class="badge badge-muted">支持 DOI / URL</span>
    </div>

    <div class="form-grid">
      <div class="form-group">
        <label>论文来源</label>
        <select :value="selectedSource" @change="emit('update:selected-source', $event.target.value)">
          <option value="arXiv">arXiv</option>
          <option value="PubMed">PubMed</option>
          <option value="Semantic Scholar">Semantic Scholar</option>
          <option value="Crossref">Crossref</option>
        </select>
      </div>
      <div class="form-group">
        <label>DOI / ID</label>
        <input :value="paper.id" @input="updateField('id', $event)" placeholder="10.xxxx/xxxx 或 arXiv ID" />
      </div>
      <div class="form-group">
        <label>论文链接</label>
        <input :value="paper.url" @input="updateField('url', $event)" placeholder="https://..." />
      </div>
      <div class="form-group">
        <label>标题</label>
        <input :value="paper.title" @input="updateField('title', $event)" />
      </div>
      <div class="form-group">
        <label>摘要</label>
        <textarea :value="paper.abstract" rows="6" @input="updateField('abstract', $event)"></textarea>
      </div>
      <div class="reader-toolbar">
        <button type="button" class="btn btn-primary" @click="$emit('import-paper')">
          {{ importing ? "导入中..." : "导入到工作区" }}
        </button>
        <button type="button" class="btn btn-secondary" @click="$emit('quick-analyze')">生成速读草稿</button>
      </div>
    </div>
  </section>
</template>

<script setup>
const emit = defineEmits(["import-paper", "quick-analyze", "update:paper", "update:selected-source"]);
const props = defineProps({
  importing: { type: Boolean, default: false },
  paper: { type: Object, required: true },
  selectedSource: { type: String, required: true },
});

function updateField(field, event) {
  emit("update:paper", { ...props.paper, [field]: event.target.value });
}
</script>
