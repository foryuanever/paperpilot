<template>
  <div class="grid-2">
    <div class="card">
      <div class="card-header">
        <h3 class="section-title">后端服务拆分</h3>
        <span class="badge badge-muted">Architecture</span>
      </div>
      <div class="card-body">
        <div class="timeline">
          <div v-for="step in architectureSteps" :key="step.id" class="timeline-item">
            <div class="tl-title">{{ step.title }}</div>
            <div class="tl-desc">{{ step.description }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3 class="section-title">关键接口</h3>
        <span class="badge">API</span>
      </div>
      <div class="card-body">
        <div class="list-card-item" v-for="item in apiItems" :key="item.title">
          <div>
            <div class="item-title">{{ item.title }}</div>
            <div class="item-meta">{{ item.description }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { apiItems, architectureSteps } from "../constants/workspace";
</script>
