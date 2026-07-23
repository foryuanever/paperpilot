<template>
  <section class="checkin-lottery-card" :class="{ checked: todayCheckin?.status === '已打卡' }">
    <div class="checkin-card-time">
      <strong>实验室学术签到</strong>
      <span>{{ currentDate }}</span>
      <b>{{ currentClock }}</b>
    </div>
    <div class="checkin-card-copy">
      <p>{{ statusSentence }}</p>
    </div>
    <button class="checkin-card-action" type="button" :disabled="busy" @click="handleCheckinClick">
      {{ actionLabel }}
    </button>
  </section>

  <Teleport to="body">
    <div v-if="showModal" class="checkin-modal-backdrop" @click.self="closeModal">
      <section class="checkin-lottery-modal">
        <button class="checkin-modal-close" type="button" @click="closeModal">关闭</button>
        <div class="lottery-stage">
          <div class="lottery-pointer"></div>
          <svg
            class="lottery-wheel"
            :class="{ spinning }"
            :style="{ transform: `rotate(${wheelRotation}deg)` }"
            viewBox="0 0 220 220"
            role="img"
            aria-label="硕果抽奖转盘"
          >
            <g v-for="segment in wheelSegments" :key="segment.value">
              <path :d="segment.path" :fill="segment.fill" />
              <text
                :x="segment.textX"
                :y="segment.textY"
                text-anchor="middle"
                dominant-baseline="middle"
              >
                +{{ segment.value }}
              </text>
            </g>
            <circle cx="110" cy="110" r="48" class="wheel-center-outer" />
            <circle cx="110" cy="110" r="27" class="wheel-center-inner" />
            <text x="110" y="113" class="wheel-center-text" text-anchor="middle">硕果</text>
          </svg>
        </div>

        <div class="lottery-copy">
          <span>Daily Research Check-in</span>
          <h3>{{ modalTitle }}</h3>
          <p>{{ modalText }}</p>
        </div>

        <button
          v-if="todayCheckin?.status === '已打卡' && !todayCheckin?.fruitClaimed"
          class="lottery-primary"
          type="button"
          :disabled="busy"
          @click="drawFruit"
        >
          {{ busy ? "抽取中..." : "抽取今日硕果" }}
        </button>

        <div v-if="todayCheckin?.fruitClaimed" class="lottery-result">
          <span>今日获得</span>
          <strong>+{{ todayCheckin?.fruitAward || 0 }} 硕果</strong>
        </div>

        <div class="lottery-streak">
          <span>连续签到</span>
          <strong>{{ todayCheckin?.streak || 0 }} 天</strong>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useTeamStore } from "../stores/team";
import { useAuthStore } from "../stores/auth";

const emit = defineEmits(["toast"]);

const teamStore = useTeamStore();
const authStore = useAuthStore();

const showModal = ref(false);
const busy = ref(false);
const spinning = ref(false);
const wheelRotation = ref(0);
const wheelValues = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
const wheelColors = ["#e7f7ee", "#edf5ff", "#fff4df", "#f3edff", "#e6fbf5", "#fff0f4", "#ebf7ff", "#f7f2e7", "#edfdf4", "#f4f7ff"];
const currentDate = ref("");
const currentClock = ref("");
let clockTimer = null;

const currentMemberId = computed(() => {
  const current = teamStore.members.find((member) => member.isCurrentUser);
  return current?.id || authStore.profile.email || "";
});

const todayCheckin = computed(() => {
  return teamStore.checkins.find((item) => item.memberId === currentMemberId.value);
});

const statusSentence = computed(() => {
  if (!todayCheckin.value?.status) return "今天还没签到，点击右侧按钮记录今天的科研节奏。";
  if (todayCheckin.value?.fruitClaimed) {
    return `您今日已于 ${todayCheckin.value.time || "今天"} 完成签到，获得 ${todayCheckin.value.fruitAward || 0} 枚硕果，已连续 ${todayCheckin.value.streak || 1} 天。`;
  }
  return `您今日已于 ${todayCheckin.value.time || "今天"} 完成签到，已连续 ${todayCheckin.value.streak || 1} 天，待抽取硕果。`;
});

const actionLabel = computed(() => {
  if (!todayCheckin.value?.status) return "签到";
  if (!todayCheckin.value?.fruitClaimed) return "去抽奖";
  return "查看";
});

const modalTitle = computed(() => {
  if (!todayCheckin.value?.status) return "先完成今日签到";
  if (todayCheckin.value?.fruitClaimed) return "今日硕果已入账";
  return "转动今日硕果盘";
});

const modalText = computed(() => {
  if (!todayCheckin.value?.status) return "点击签到后，再亲手抽取今日硕果。";
  if (todayCheckin.value?.fruitClaimed) return `你今天获得 ${todayCheckin.value.fruitAward || 0} 枚硕果，等级进度已经更新。`;
  return "连续签到越久，高额硕果概率越高。点击下方按钮后才会真正入账。";
});

function notify(message) {
  emit("toast", message);
}

function polarToCartesian(cx, cy, r, angle) {
  const rad = (angle * Math.PI) / 180;
  return {
    x: cx + r * Math.cos(rad),
    y: cy + r * Math.sin(rad),
  };
}

function describeSegment(index) {
  const cx = 110;
  const cy = 110;
  const r = 88;
  const inner = 48;
  const slice = 360 / wheelValues.length;
  const start = -90 - slice / 2 + index * slice;
  const end = start + slice;
  const p1 = polarToCartesian(cx, cy, r, start);
  const p2 = polarToCartesian(cx, cy, r, end);
  const p3 = polarToCartesian(cx, cy, inner, end);
  const p4 = polarToCartesian(cx, cy, inner, start);
  return `M ${p1.x} ${p1.y} A ${r} ${r} 0 0 1 ${p2.x} ${p2.y} L ${p3.x} ${p3.y} A ${inner} ${inner} 0 0 0 ${p4.x} ${p4.y} Z`;
}

const wheelSegments = computed(() => {
  const slice = 360 / wheelValues.length;
  return wheelValues.map((value, index) => {
    const angle = -90 + index * slice;
    const textPoint = polarToCartesian(110, 110, 68, angle);
    return {
      value,
      path: describeSegment(index),
      fill: wheelColors[index % wheelColors.length],
      textX: textPoint.x,
      textY: textPoint.y,
    };
  });
});

function closeModal() {
  if (busy.value) return;
  showModal.value = false;
}

async function handleCheckinClick() {
  if (busy.value) return;
  if (todayCheckin.value?.status === "已打卡") {
    showModal.value = true;
    return;
  }
  busy.value = true;
  try {
    await teamStore.performCheckin(currentMemberId.value);
    showModal.value = true;
  } catch (error) {
    notify(error.response?.data?.message || "签到失败，请稍后重试");
  } finally {
    busy.value = false;
  }
}

function rotationForAward(award) {
  const index = Math.max(0, wheelValues.indexOf(Number(award)));
  const segment = 360 / wheelValues.length;
  const target = (360 - index * segment) % 360;
  const current = ((wheelRotation.value % 360) + 360) % 360;
  const delta = (target - current + 360) % 360;
  return 1440 + delta;
}

async function drawFruit() {
  if (busy.value || todayCheckin.value?.fruitClaimed) return;
  busy.value = true;
  spinning.value = true;
  wheelRotation.value += 360;
  try {
    const saved = await teamStore.drawCheckinFruit(currentMemberId.value);
    wheelRotation.value += rotationForAward(saved.fruitAward);
    window.setTimeout(() => {
      spinning.value = false;
    }, 1050);
  } catch (error) {
    spinning.value = false;
    notify(error.response?.data?.message || "抽取硕果失败，请稍后重试");
  } finally {
    window.setTimeout(() => {
      busy.value = false;
    }, 1120);
  }
}

onMounted(() => {
  teamStore.loadFromServer().catch(() => {});
  updateClock();
  clockTimer = window.setInterval(updateClock, 1000);
});

onUnmounted(() => {
  if (clockTimer) window.clearInterval(clockTimer);
});

function updateClock() {
  const now = new Date();
  currentDate.value = new Intl.DateTimeFormat("zh-CN", {
    timeZone: "Asia/Shanghai",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
  }).format(now);
  currentClock.value = new Intl.DateTimeFormat("zh-CN", {
    timeZone: "Asia/Shanghai",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(now);
}
</script>

<style scoped>
.checkin-lottery-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  flex: 1 1 auto;
  width: 100%;
  min-height: 106px;
  padding: 10px 0 14px;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.checkin-lottery-card.checked {
  border-color: transparent;
  background: transparent;
}

.checkin-card-copy {
  min-width: 0;
  flex: 1 1 auto;
}

.checkin-card-copy p {
  margin: 0;
  color: #5f6f86;
  font-size: 16px;
  font-weight: 750;
  line-height: 1.65;
  text-align: right;
  text-wrap: pretty;
}

.checkin-card-time {
  display: grid;
  gap: 6px;
  min-width: 196px;
  text-align: left;
}

.checkin-card-time span {
  color: #64748b;
  font-size: 15px;
  font-weight: 800;
}

.checkin-card-time strong {
  color: #14213d;
  font-size: 24px;
  line-height: 1.15;
}

.checkin-card-time b {
  color: #0f172a;
  font-size: 42px;
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.checkin-card-action {
  flex: 0 0 auto;
  min-width: 150px;
  min-height: 58px;
  border: 0;
  border-radius: 999px;
  color: #056245;
  background: #c7efd9;
  font-size: 17px;
  font-weight: 850;
  cursor: pointer;
  box-shadow: 0 12px 22px rgba(16, 185, 129, .14);
}

.checkin-card-action:hover {
  background: #b8e8cd;
}

.checkin-card-action:disabled {
  cursor: not-allowed;
  opacity: .64;
}

.checkin-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1600;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, .42);
  backdrop-filter: blur(10px);
}

.checkin-lottery-modal {
  position: relative;
  width: min(520px, 100%);
  padding: 34px;
  border: 1px solid rgba(148, 163, 184, .24);
  border-radius: 24px;
  background:
    radial-gradient(circle at 18% 8%, rgba(34, 197, 94, .12), transparent 34%),
    radial-gradient(circle at 92% 14%, rgba(37, 99, 235, .14), transparent 34%),
    #ffffff;
  box-shadow: 0 28px 80px rgba(15, 23, 42, .26);
  color: #0f172a;
}

.checkin-modal-close {
  position: absolute;
  top: 18px;
  right: 18px;
  min-width: 64px;
  height: 38px;
  border: 0;
  border-radius: 999px;
  color: #334155;
  background: #eef2f7;
  font-weight: 850;
  cursor: pointer;
}

.lottery-stage {
  position: relative;
  width: 210px;
  height: 210px;
  display: grid;
  place-items: center;
  margin: 6px auto 24px;
}

.lottery-wheel {
  width: 184px;
  height: 184px;
  overflow: visible;
  border-radius: 50%;
  filter: drop-shadow(0 18px 22px rgba(30, 41, 59, .14));
  transition: transform 1050ms cubic-bezier(.16, 1, .3, 1);
}

.lottery-wheel.spinning {
  transition-duration: 1200ms;
}

.lottery-wheel path {
  stroke: rgba(148, 163, 184, .30);
  stroke-width: 1.2;
}

.lottery-wheel text {
  color: #14532d;
  fill: #14532d;
  font-size: 17px;
  font-weight: 950;
}

.wheel-center-outer {
  fill: #fff;
  stroke: rgba(15, 23, 42, .08);
  stroke-width: 1.2;
}

.wheel-center-inner {
  fill: #ecfdf5;
}

.wheel-center-text {
  fill: #047857;
  font-size: 13px;
  font-weight: 950;
}

.lottery-pointer {
  position: absolute;
  top: 2px;
  left: 50%;
  z-index: 5;
  width: 28px;
  height: 34px;
  background: #0f8d66;
  clip-path: polygon(50% 100%, 0 0, 100% 0);
  transform: translateX(-50%);
  filter: drop-shadow(0 9px 10px rgba(15, 141, 102, .22));
}

.lottery-copy span {
  color: #2563eb;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: .08em;
  text-transform: uppercase;
}

.lottery-copy h3 {
  margin: 8px 0 10px;
  color: #111827;
  font-size: 28px;
  line-height: 1.18;
  text-wrap: balance;
}

.lottery-copy p {
  margin: 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.7;
}

.lottery-primary {
  width: 100%;
  min-height: 48px;
  margin-top: 22px;
  border: 0;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(135deg, #0f8d66, #2563eb);
  font-size: 15px;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 12px 22px rgba(37, 99, 235, .16);
}

.lottery-primary:disabled {
  cursor: not-allowed;
  opacity: .7;
}

.lottery-result,
.lottery-streak {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f4f8ff;
}

.lottery-result {
  color: #1e3a8a;
}

.lottery-result span,
.lottery-streak span {
  font-size: 13px;
  font-weight: 850;
}

.lottery-result strong {
  font-size: 26px;
  line-height: 1;
}

.lottery-streak {
  color: #047857;
  background: #ecfdf5;
}

.lottery-streak strong {
  font-size: 24px;
}

@media (prefers-reduced-motion: reduce) {
  .lottery-wheel,
  .lottery-wheel.spinning {
    transition-duration: 1ms;
  }
}

@media (max-width: 560px) {
  .checkin-lottery-card {
    min-width: 0;
    width: 100%;
    flex-wrap: wrap;
  }

  .checkin-card-time {
    text-align: left;
  }

  .checkin-card-copy p {
    text-align: left;
  }

  .checkin-lottery-modal {
    padding: 28px 22px;
  }
}

/* ── DARK MODE ADAPTATIONS FOR CHECKIN LOTTERY ── */
:root[data-theme="dark"] .checkin-lottery-modal {
  background:
    radial-gradient(circle at 18% 8%, rgba(34, 197, 94, .16), transparent 34%),
    radial-gradient(circle at 92% 14%, rgba(37, 99, 235, .16), transparent 34%),
    #121a28 !important;
  border-color: rgba(226, 235, 255, 0.14) !important;
  color: #eef4ff !important;
  box-shadow: 0 28px 80px rgba(0, 0, 0, 0.6) !important;
}

:root[data-theme="dark"] .checkin-modal-close {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #a8b3c7 !important;
}
:root[data-theme="dark"] .checkin-modal-close:hover {
  background: rgba(255, 255, 255, 0.16) !important;
  color: #ffffff !important;
}

:root[data-theme="dark"] .lottery-copy h3 {
  color: #f4f4f6 !important;
}

:root[data-theme="dark"] .lottery-copy p {
  color: #a8b3c7 !important;
}

:root[data-theme="dark"] .lottery-result {
  background: rgba(37, 99, 235, 0.18) !important;
  color: #93c5fd !important;
  border: 1px solid rgba(59, 130, 246, 0.28) !important;
}

:root[data-theme="dark"] .lottery-result strong {
  color: #60a5fa !important;
}

:root[data-theme="dark"] .lottery-streak {
  background: rgba(16, 185, 129, 0.18) !important;
  color: #6ee7b7 !important;
  border: 1px solid rgba(16, 185, 129, 0.28) !important;
}

:root[data-theme="dark"] .lottery-streak strong {
  color: #34d399 !important;
}

</style>
