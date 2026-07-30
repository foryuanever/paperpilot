<template>
  <Transition name="card-fade">
    <div v-if="store.state.open" class="user-card-overlay" @click="handleOverlayClick">
      <section class="user-card" @click.stop>
        <button class="card-close" @click="handleCloseClick">×</button>
        <div v-if="store.state.loading" class="card-state">正在读取个人资料...</div>
        <div v-else-if="store.state.error" class="card-state error">{{ store.state.error }}</div>
        <template v-else-if="user">
          <div class="card-cover">
            <img v-if="user.backgroundUrl" :src="user.backgroundUrl" alt="" />
            <span class="cover-orbit one"></span>
            <span class="cover-orbit two"></span>
          </div>
          <div class="card-profile">
            <div class="card-avatar">
              <img v-if="user.avatarUrl" :src="user.avatarUrl" :alt="user.name" />
              <span v-else>{{ user.avatar }}</span>
            </div>
            <div class="card-identity">
              <span class="role-badge">Lv.{{ levelInfo.level }}</span>
              <h2>{{ user.name }}</h2>
              <p>{{ membershipLabel }}</p>
            </div>
          </div>
          <div class="card-stats">
            <div><span>等级</span><strong>Lv.{{ levelInfo.level }}</strong></div>
            <div><span>会员</span><strong>{{ membershipShort }}</strong></div>
          </div>
          <p class="card-note">
            <template v-if="user.contactInfo">联系方式：{{ user.contactInfo }}</template>
            <template v-else>对方联系方式默认隐藏。发起申请并被接受后，这里会显示对方主动填写的微信或 QQ。</template>
          </p>
          <footer>
            <button
              v-if="!user.isSelf"
              class="contact-action"
              :disabled="contactButtonDisabled"
              @click="store.requestContact"
            >
              {{ contactLabel }}
            </button>
            <button
              v-if="!user.isSelf"
              class="report-user-btn"
              @click="openUserReport"
            >
              举报
            </button>
            <router-link v-else to="/profile" class="friend-action" @click="store.close">查看我的主页</router-link>
          </footer>

          <!-- Nested User Report Modal Overlay -->
          <Transition name="report-fade">
            <div v-if="showUserReportModal" class="user-report-overlay" @click.self="handleReportOverlayClick">
              <section class="user-report-modal">
                <header>
                  <h3>举报用户</h3>
                  <p>被举报人: {{ user.name }}</p>
                </header>

                <label class="report-textarea-label">
                  <span>违规详情</span>
                  <textarea v-model.trim="userReportDetail" rows="3" maxlength="800" placeholder="请描述该用户的违规行为..."></textarea>
                  <small>{{ userReportDetail.length }}/800，至少 6 个字。</small>
                </label>

                <div class="report-upload-section">
                  <span>附上截图证据 (可选)</span>
                  <div class="report-upload-row">
                    <label class="report-upload-box">
                      <input type="file" accept="image/*" @change="handleUserReportScreenshotUpload" style="display: none;" />
                      <div v-if="!userReportScreenshot" class="upload-placeholder">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 16px; height: 16px;"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
                        <span>点击上传截图</span>
                      </div>
                      <div v-else class="upload-preview">
                        <img :src="userReportScreenshot" alt="截图" />
                        <button class="remove-preview-btn" @click.stop.prevent="userReportScreenshot = ''">删除</button>
                      </div>
                    </label>
                    <small v-if="userReportUploadError" class="upload-error">{{ userReportUploadError }}</small>
                  </div>
                </div>

                <div class="user-report-actions">
                  <button class="cancel-btn" @click="handleReportOverlayClick">取消</button>
                  <button class="submit-btn" :disabled="userReporting || userReportDetail.length < 6" @click="submitUserReport">
                    {{ userReporting ? "提交中..." : "提交举报" }}
                  </button>
                </div>
              </section>
            </div>
          </Transition>
        </template>
      </section>
    </div>
  </Transition>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useUserCardStore } from "../stores/userCard";
import { paperpilotApi } from "../services/paperpilotApi";
import { useAuthStore } from "../stores/auth";

const store = useUserCardStore();
const authStore = useAuthStore();
const user = computed(() => store.state.user);

const showUserReportModal = ref(false);
const userReportDetail = ref("");
const userReportScreenshot = ref("");
const userReportUploadError = ref("");
const userReporting = ref(false);

const levelInfo = computed(() => {
  const score = Math.max(0, Number(user.value?.fruitScore || 0));
  return {
    score,
    level: Math.floor(score / 100) + 1,
  };
});
const membershipNameMap = {
  free: "普通用户",
  basic: "基础会员",
  pro: "Pro 会员",
  premium: "高级会员",
  vip: "VIP 会员",
};
const membershipLabel = computed(() => membershipNameMap[user.value?.membershipPlan] || "普通用户");
const membershipShort = computed(() => membershipLabel.value.replace("会员", ""));
const contactLabel = computed(() => ({
  friends: "已获得联系方式",
  outgoing_pending: "申请已发送",
  incoming_pending: "对方已申请你",
  none: "申请联系方式",
}[user.value?.contactStatus || user.value?.friendshipStatus] || "申请联系方式"));
const contactButtonDisabled = computed(() =>
  ["friends", "outgoing_pending", "incoming_pending"].includes(user.value?.friendshipStatus),
);

function openUserReport() {
  showUserReportModal.value = true;
  userReportDetail.value = "";
  userReportScreenshot.value = "";
  userReportUploadError.value = "";
}

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

async function handleUserReportScreenshotUpload(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file) return;
  if (!file.type?.startsWith("image/")) {
    userReportUploadError.value = "请上传图片格式的截图。";
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    userReportUploadError.value = "截图图片不能超过 5MB。";
    return;
  }
  try {
    const dataUrl = await fileToDataUrl(file);
    userReportScreenshot.value = dataUrl;
    userReportUploadError.value = "";
  } catch (e) {
    userReportUploadError.value = "文件读取失败";
  }
}

async function submitUserReport() {
  if (!user.value || userReportDetail.value.length < 6 || userReporting.value) return;
  userReporting.value = true;
  try {
    await paperpilotApi.reportUser(user.value.userId, {
      detail: userReportDetail.value,
      screenshot: userReportScreenshot.value
    });
    authStore.addNotification({
      title: "举报已提交",
      desc: `已提交对用户 ${user.value.name} 的举报。`
    });
    showUserReportModal.value = false;
  } catch (error) {
    userReportUploadError.value = error.response?.data?.message || "举报提交失败";
  } finally {
    userReporting.value = false;
  }
}

function handleReportOverlayClick() {
  if (userReportDetail.value.length >= 6) {
    if (!confirm("您已输入举报内容，确定要取消并关闭吗？")) {
      return;
    }
  }
  showUserReportModal.value = false;
}

function handleOverlayClick() {
  if (showUserReportModal.value) {
    handleReportOverlayClick();
  } else {
    store.close();
  }
}

function handleCloseClick() {
  if (showUserReportModal.value) {
    handleReportOverlayClick();
  } else {
    store.close();
  }
}

async function refreshOpenCard() {
  if (store.state.open && user.value?.userId) {
    await store.open(user.value.userId);
  }
}

onMounted(() => {
  window.addEventListener("paperpilot:contact-requests-changed", refreshOpenCard);
});

onBeforeUnmount(() => {
  window.removeEventListener("paperpilot:contact-requests-changed", refreshOpenCard);
});
</script>

<style scoped>
.user-card-overlay { position: fixed; inset: 0; z-index: 12000; display: grid; place-items: center; padding: 24px; background: rgba(15,23,38,.46); }
.user-card { position: relative; width: min(430px, calc(100vw - 32px)); overflow: hidden; border: 1px solid rgba(148,163,184,.24); border-radius: 24px; background: #0f172a; box-shadow: 0 30px 90px rgba(15,28,52,.34); }
.card-close { position: absolute; z-index: 3; top: 13px; right: 13px; width: 34px; height: 34px; border: 0; border-radius: 50%; color: #fff; background: rgba(24,35,54,.36); font-size: 22px; }
.card-state { padding: 90px 30px; color: #7d899b; text-align: center; }
.card-state.error { color: #bd4056; }
.card-cover { position: relative; height: 118px; overflow: hidden; background: linear-gradient(135deg, #1e293b, #0f172a); }
.card-cover img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.card-cover::after { content: ""; position: absolute; inset: 0; background: linear-gradient(to bottom, rgba(15,23,42,.1), rgba(15,23,42,.5)); }
.cover-orbit { position: absolute; border: 1px solid rgba(255,255,255,.22); border-radius: 50%; }
.cover-orbit.one { width: 180px; height: 180px; top: -90px; right: -20px; }
.cover-orbit.two { width: 110px; height: 110px; left: 22px; bottom: -80px; }
.card-profile { display: flex; align-items: flex-end; gap: 15px; padding: 0 24px; margin-top: -34px; position: relative; }
.card-avatar { width: 78px; height: 78px; flex: 0 0 auto; display: grid; place-items: center; overflow: hidden; border: 5px solid rgba(248,250,252,.95); border-radius: 23px; color: #fff; background: linear-gradient(135deg, #176ce4, #683fd5); box-shadow: 0 10px 25px rgba(2,6,23,.26); font-size: 27px; font-weight: 850; }
.card-avatar img { width: 100%; height: 100%; object-fit: cover; }
.card-identity { min-width: 0; padding-bottom: 5px; }
.role-badge { padding: 3px 8px; border-radius: 999px; color: #bfdbfe; background: rgba(59,130,246,.18); border: 1px solid rgba(147,197,253,.2); font-size: 10px; font-weight: 850; }
.card-identity h2 { margin: 7px 0 3px; color: #f8fafc; font-size: 20px; }
.card-identity p { margin: 0; overflow: hidden; color: #a5b4fc; text-overflow: ellipsis; white-space: nowrap; font-size: 11px; font-weight: 750; }
.card-stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; padding: 22px 24px 14px; }
.card-stats div { padding: 11px 8px; display: flex; flex-direction: column; gap: 5px; border-radius: 11px; background: rgba(15,23,42,.78); border: 1px solid rgba(148,163,184,.14); text-align: center; }
.card-stats span { color: #94a3b8; font-size: 8px; }
.card-stats strong { color: #e0e7ff; font-size: 10px; }
.card-note { margin: 0 24px; padding: 12px 14px; border: 1px solid rgba(148,163,184,.18); border-radius: 11px; color: #cbd5e1; background: rgba(15,23,42,.72); font-size: 10px; line-height: 1.65; }
footer { display: flex; gap: 9px; padding: 18px 24px 23px; }
footer button, footer a { min-height: 40px; flex: 1; display: grid; place-items: center; border-radius: 10px; font-size: 11px; font-weight: 800; text-decoration: none; }
.contact-action,
.friend-action { border: 0; color: #fff; background: #0865ee; }
.contact-action:disabled,
.friend-action:disabled { color: #788498; background: #edf1f6; }

.report-user-btn {
  border: 1px solid rgba(239, 68, 68, 0.4);
  color: #f87171;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}
.report-user-btn:hover {
  background: rgba(239, 68, 68, 0.08);
  border-color: #ef4444;
  color: #ef4444;
}

/* User Report overlay styles */
.user-report-overlay {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(4px);
}

.user-report-modal {
  width: 100%;
  max-width: 320px;
  padding: 20px;
  border-radius: 16px;
  background: #1e293b;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-report-modal header {
  text-align: left;
}

.user-report-modal header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: #f1f5f9;
}

.user-report-modal header p {
  margin: 4px 0 0;
  font-size: 11px;
  color: #94a3b8;
}

.report-textarea-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: left;
}

.report-textarea-label span {
  font-size: 11px;
  font-weight: 700;
  color: #94a3b8;
}

.report-textarea-label textarea {
  width: 100%;
  padding: 8px 10px;
  border-radius: 8px;
  background: #0f172a;
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #f1f5f9;
  font-size: 12px;
  resize: none;
  outline: none;
}

.report-textarea-label textarea:focus {
  border-color: #3b82f6;
}

.report-textarea-label small {
  font-size: 10px;
  color: #64748b;
  text-align: right;
}

.report-upload-section {
  text-align: left;
}

.report-upload-section span {
  font-size: 11px;
  font-weight: 700;
  color: #94a3b8;
  display: block;
  margin-bottom: 6px;
}

.report-upload-box {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  border: 1.5px dashed rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  background: #0f172a;
  transition: all 0.2s;
}

.upload-placeholder {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #94a3b8;
}

.upload-placeholder span {
  margin: 0;
  font-size: 11px;
  font-weight: 500;
}

.upload-preview {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.remove-preview-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border: none;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 10px;
  cursor: pointer;
}

.upload-error {
  color: #ef4444;
  font-size: 10px;
}

.user-report-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.user-report-actions button {
  min-height: 36px;
  flex: 1;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}

.user-report-actions .cancel-btn {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #94a3b8;
}

.user-report-actions .cancel-btn:hover {
  background: rgba(255, 255, 255, 0.04);
}

.user-report-actions .submit-btn {
  background: #be123c;
  color: #fff;
  border: none;
}

.user-report-actions .submit-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.report-fade-enter-active, .report-fade-leave-active { transition: opacity .18s ease; }
.report-fade-enter-from, .report-fade-leave-to { opacity: 0; }

.card-fade-enter-active, .card-fade-leave-active { transition: opacity .18s ease; }
.card-fade-enter-from, .card-fade-leave-to { opacity: 0; }

/* Light Mode Responsive Styles */
:global([data-theme="light"]) .user-card {
  background: #ffffff;
  border-color: rgba(226, 232, 240, 0.8);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
}
:global([data-theme="light"]) .card-close {
  color: #475569;
  background: rgba(0, 0, 0, 0.05);
}
:global([data-theme="light"]) .card-identity h2 {
  color: #0f172a;
}
:global([data-theme="light"]) .card-identity p {
  color: #4f46e5;
}
:global([data-theme="light"]) .role-badge {
  color: #2563eb;
  background: rgba(59, 130, 246, 0.08);
  border-color: rgba(147, 197, 253, 0.3);
}
:global([data-theme="light"]) .card-stats div {
  background: #f8fafc;
  border-color: rgba(226, 232, 240, 0.8);
}
:global([data-theme="light"]) .card-stats span {
  color: #64748b;
}
:global([data-theme="light"]) .card-stats strong {
  color: #0f172a;
}
:global([data-theme="light"]) .card-note {
  background: #f1f5f9;
  border-color: rgba(226, 232, 240, 0.9);
  color: #475569;
}
:global([data-theme="light"]) .contact-action:disabled {
  color: #94a3b8;
  background: #f1f5f9;
}
:global([data-theme="light"]) .user-report-modal {
  background: #ffffff;
  border-color: rgba(226, 232, 240, 0.8);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}
:global([data-theme="light"]) .user-report-modal header h3 {
  color: #0f172a;
}
:global([data-theme="light"]) .report-textarea-label span,
:global([data-theme="light"]) .report-upload-section span {
  color: #475569;
}
:global([data-theme="light"]) .report-textarea-label textarea,
:global([data-theme="light"]) .report-upload-box {
  background: #f8fafc;
  border-color: rgba(226, 232, 240, 0.8);
  color: #0f172a;
}
:global([data-theme="light"]) .report-textarea-label textarea:focus {
  border-color: #3b82f6;
}
:global([data-theme="light"]) .user-report-actions .cancel-btn {
  border-color: rgba(226, 232, 240, 0.8);
  color: #475569;
}
:global([data-theme="light"]) .user-report-actions .cancel-btn:hover {
  background: rgba(0, 0, 0, 0.02);
}
</style>
