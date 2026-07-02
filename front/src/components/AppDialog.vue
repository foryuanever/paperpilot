<template>
  <Transition name="app-dialog">
    <div v-if="dialogStore.state.open" class="app-dialog-overlay" @click.self="dialogStore.cancel">
      <section class="app-dialog-card" role="dialog" aria-modal="true" :aria-labelledby="titleId">
        <div class="app-dialog-icon" :class="{ danger: dialogStore.state.danger }">
          {{ dialogStore.state.danger ? "!" : "i" }}
        </div>
        <h2 :id="titleId">{{ dialogStore.state.title }}</h2>
        <p>{{ dialogStore.state.message }}</p>
        <div class="app-dialog-actions">
          <button
            v-if="dialogStore.state.showCancel"
            type="button"
            class="app-dialog-btn secondary"
            @click="dialogStore.cancel"
          >
            {{ dialogStore.state.cancelText }}
          </button>
          <button
            type="button"
            class="app-dialog-btn"
            :class="{ danger: dialogStore.state.danger }"
            autofocus
            @click="dialogStore.accept"
          >
            {{ dialogStore.state.confirmText }}
          </button>
        </div>
      </section>
    </div>
  </Transition>
</template>

<script setup>
import { onMounted, onUnmounted } from "vue";
import { useDialogStore } from "../stores/dialog";

const dialogStore = useDialogStore();
const titleId = "paper-slover-dialog-title";

function handleKeydown(event) {
  if (!dialogStore.state.open) return;
  if (event.key === "Escape") dialogStore.cancel();
  if (event.key === "Enter") dialogStore.accept();
}

onMounted(() => window.addEventListener("keydown", handleKeydown));
onUnmounted(() => window.removeEventListener("keydown", handleKeydown));
</script>

<style scoped>
.app-dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 2147483600;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.38);
}

.app-dialog-card {
  width: min(440px, 100%);
  padding: 28px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.22);
  color: #1d1d1f;
}

.app-dialog-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  margin-bottom: 18px;
  border-radius: 12px;
  background: rgba(0, 102, 255, 0.09);
  color: #0066ff;
  font-size: 18px;
  font-weight: 800;
}

.app-dialog-icon.danger {
  background: rgba(255, 59, 48, 0.09);
  color: #ff3b30;
}

.app-dialog-card h2 {
  margin: 0 0 10px;
  font-size: 21px;
  line-height: 1.35;
}

.app-dialog-card p {
  margin: 0;
  color: #60646c;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.app-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 26px;
}

.app-dialog-btn {
  min-width: 88px;
  padding: 10px 18px;
  border: 0;
  border-radius: 12px;
  background: #1d1d1f;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.app-dialog-btn.secondary {
  background: #f0f1f3;
  color: #36383d;
}

.app-dialog-btn.danger {
  background: #ff3b30;
}

.app-dialog-enter-active,
.app-dialog-leave-active {
  transition: opacity 0.18s ease;
}

.app-dialog-enter-active .app-dialog-card,
.app-dialog-leave-active .app-dialog-card {
  transition: transform 0.18s ease, opacity 0.18s ease;
}

.app-dialog-enter-from,
.app-dialog-leave-to {
  opacity: 0;
}

.app-dialog-enter-from .app-dialog-card,
.app-dialog-leave-to .app-dialog-card {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
}
</style>
