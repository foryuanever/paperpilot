<template>
  <section class="checkin-lottery-card" :class="{ checked: todayCheckin?.status === '已打卡' }">
    <div class="checkin-card-time">
      <strong>学术签到</strong>
      <span>{{ currentDate }}</span>
      <b>{{ currentClock }}</b>
    </div>

    <!-- 科研黄历：无背景大日历图标 + 今日宜 / 今日忌 / 学术运势 3 行更大字号 -->
    <div class="checkin-almanac-strip">
      <div class="almanac-calendar-icon-wrap" title="今日科研黄历">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="4" width="18" height="18" rx="3" ry="3"></rect>
          <line x1="16" y1="2" x2="16" y2="6"></line>
          <line x1="8" y1="2" x2="8" y2="6"></line>
          <line x1="3" y1="10" x2="21" y2="10"></line>
        </svg>
      </div>
      <div class="almanac-3rows">
        <div class="almanac-line almanac-good" :title="`今日科研宜：${todayAlmanac.good.join('、')}`">
          <span class="almanac-badge good-badge">宜</span>
          <span class="almanac-text">{{ todayAlmanac.good.join(" · ") }}</span>
        </div>
        <div class="almanac-line almanac-bad" :title="`今日科研忌：${todayAlmanac.bad.join('、')}`">
          <span class="almanac-badge bad-badge">忌</span>
          <span class="almanac-text">{{ todayAlmanac.bad.join(" · ") }}</span>
        </div>
        <div class="almanac-line almanac-fortune">
          <span class="almanac-sparkle">✨</span>
          <span class="almanac-fortune-text">学术运势：{{ todayAlmanac.fortune }}</span>
        </div>
      </div>
    </div>

    <div class="checkin-card-copy">
      <p class="greeting-salute">尊敬的硕博，</p>
      <p class="greeting-status">
        <template v-if="todayCheckin?.status === '已打卡'">
          您于 <span class="checkin-time-highlight">{{ checkinTimeFormatted }}</span> 完成打卡
        </template>
        <template v-else>
          您今日尚未打卡，点击打卡
        </template>
      </p>
    </div>
    <button class="checkin-card-action" type="button" :disabled="busy" @click="handleCheckinClick">
      {{ actionLabel }}
    </button>
  </section>

  <Teleport to="body">
    <div v-if="showModal" class="checkin-modal-backdrop" @click.self="closeModal">
      <section class="checkin-lottery-modal checkin-modal-horizontal">
        <button class="checkin-modal-close" type="button" @click="closeModal">关闭</button>
        
        <!-- 左侧：签到信息、日历、连续签到天数与结果 -->
        <div class="checkin-modal-left">
          <div class="lottery-copy">
            <span>DAILY RESEARCH CHECK-IN</span>
            <h3>{{ modalTitle }}</h3>
            <p>{{ modalText }}</p>
          </div>

          <div class="checkin-exp-tip">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
            <span>每日签到自动增加 <strong>+2 经验值</strong>（用于科研等级晋升）</span>
          </div>

          <!-- 签到日历：已签到的日期打勾（无 emoji） -->
          <div class="checkin-calendar-box">
            <div class="calendar-header">
              <strong>{{ calendarMonthLabel }} 签到记录</strong>
              <span class="calendar-stat">本月已签 {{ currentMonthCheckedCount }} 天</span>
            </div>
            <div class="calendar-grid">
              <div class="calendar-weekdays">
                <span>一</span><span>二</span><span>三</span><span>四</span><span>五</span><span>六</span><span>日</span>
              </div>
              <div class="calendar-days">
                <div
                  v-for="day in calendarDays"
                  :key="day.dateKey"
                  class="calendar-day"
                  :class="{
                    'empty-day': !day.dayNum,
                    'is-today': day.isToday,
                    'is-checked': day.checked,
                  }"
                  :title="day.tooltip"
                >
                  <template v-if="day.dayNum">
                    <span class="day-num">{{ day.dayNum }}</span>
                    <svg v-if="day.checked" class="check-icon" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                    </svg>
                  </template>
                </div>
              </div>
            </div>
          </div>

          <div class="checkin-bottom-info">
            <div v-if="isClaimed" class="lottery-result">
              <span>今日获得</span>
              <strong>{{ isNoPrize ? "本次未中奖" : `+${awardDisplay}` }}</strong>
            </div>

            <div class="lottery-streak">
              <span>连续签到</span>
              <strong>{{ todayCheckin?.streak || 0 }} 天</strong>
            </div>
          </div>
        </div>

        <!-- 右侧：大转盘抽奖区域 -->
        <div class="checkin-modal-right">
          <div class="lottery-stage">
            <div class="lottery-pointer"></div>
            <svg
              class="lottery-wheel"
              :class="{ spinning }"
              :style="{ transform: `rotate(${wheelRotation}deg)` }"
              viewBox="0 0 260 260"
              role="img"
              aria-label="积分抽奖转盘"
            >
              <g v-for="segment in wheelSegments" :key="segment.id">
                <path :d="segment.path" :fill="segment.fill" />
                <!-- 扇形文字沿径向排版，在顶部时文字方向正对用户 -->
                <g :transform="`rotate(${segment.angleDeg + 90}, 130, 130)`">
                  <text
                    v-if="segment.line2"
                    x="130"
                    y="52"
                    text-anchor="middle"
                    dominant-baseline="middle"
                    font-size="11.5px"
                    font-weight="900"
                  >
                    {{ segment.line1 }}
                  </text>
                  <text
                    v-if="segment.line2"
                    x="130"
                    y="67"
                    text-anchor="middle"
                    dominant-baseline="middle"
                    font-size="10.5px"
                    font-weight="800"
                    fill="#4338ca"
                  >
                    {{ segment.line2 }}
                  </text>
                  <text
                    v-else
                    x="130"
                    y="58"
                    text-anchor="middle"
                    dominant-baseline="middle"
                    font-size="13px"
                    font-weight="900"
                  >
                    {{ segment.line1 }}
                  </text>
                </g>
              </g>
              <circle cx="130" cy="130" r="50" class="wheel-center-outer" />
              <circle cx="130" cy="130" r="30" class="wheel-center-inner" />
              <text x="130" y="134" class="wheel-center-text" text-anchor="middle">抽奖</text>
            </svg>
          </div>

          <button
            v-if="todayCheckin?.status === '已打卡' && !todayCheckin?.fruitClaimed"
            class="lottery-primary"
            type="button"
            :disabled="busy"
            @click="drawFruit"
          >
            {{ busy ? "转盘抽取中..." : "抽取今日奖励" }}
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useTeamStore } from "../stores/team";
import { useAuthStore } from "../stores/auth";
import { paperpilotApi } from "../services/paperpilotApi";

const emit = defineEmits(["toast"]);

const teamStore = useTeamStore();
const authStore = useAuthStore();

const showModal = ref(false);
const busy = ref(false);
const spinning = ref(false);
const wheelRotation = ref(0);

// 奖项与后端保持同序，翻译奖励只进入当天额度。
const WHEEL_ITEMS = [
  { id: 0, line1: "+1 积分", line2: "", type: "points", points: 1 },
  { id: 1, line1: "对照翻译", line2: "1次", type: "bilingual_translate", points: 0 },
  { id: 2, line1: "沉浸翻译", line2: "1次", type: "full_translate", points: 0 },
  { id: 3, line1: "对照翻译", line2: "2次", type: "bilingual_translate", points: 0 },
  { id: 4, line1: "沉浸翻译", line2: "2次", type: "full_translate", points: 0 },
  { id: 5, line1: "遗憾未中奖", line2: "", type: "nothing", points: 0 }
];

const ALMANAC_GOOD_ITEMS = [
  "跑通 Baseline", "投递顶刊", "一键 Rebuttal", "精读高引文献",
  "导师秒回邮件", "实验完美收敛", "GPU 算力空闲", "论文被高赞引用",
  "推导公式一遍过", "发现 SOTA 新思路", "收到录用通知", "消融实验效果显著",
  "审稿人给 Minor Revision", "顺利通过开题答辩", "发现绝妙开源代码", "组会汇报行云流水",
  "重构实验主代码", "写好 Related Work", "通读顶级 Review", "补充对比实验",
  "优化损失函数", "申请青年基金", "整理实验笔记", "复现经典论文",
  "优化模型超参", "一键生成高清矢量图", "跨学科思想碰撞", "与合作者高效讨论",
  "修复顽固显存泄漏", "论文排版通过初审", "提前跑完测试集", "思路清晰下笔如神"
];

const ALMANAC_BAD_ITEMS = [
  "硬怼审稿人", "修改超参忘记保存", "直接投稿不看格式", "把未训练权重当 SOTA",
  "深夜连轴改代码", "直接覆盖原始实验数据", "漏写核心引用文献", "汇报 PPT 字号过小",
  "盲目调参玄学炼丹", "组会前五分钟赶 PPT", "忘记备份本地实验 Log", "直接使用过时 Baseline",
  "过度拟合测试集", "在 main 分支盲推代码", "忽视审稿人第 3 条关键意见", "混淆召回率与准确率",
  "盲目相信大模型幻觉", "拖延提交 Camera-Ready", "忽略异常值直接绘图", "把相关性强行当因果性",
  "实验未跑完提前断开 SSH", "将未开源数据集写入主文", "同时启动 10 个训练撑爆显存", "盲目加大初始学习率"
];

const ALMANAC_FORTUNES = [
  "大吉 · 今日实验收敛度 99%",
  "大吉 · 灵感涌现，适合撰写核心方法章节",
  "吉 · GPU 算力充沛，适合批量跑消融实验",
  "吉 · 导师心情愉悦，适合讨论论文大纲",
  "吉 · 审稿人友好度爆棚，修改信心倍增",
  "平 · 稳扎稳打，宜梳理前沿文献脉络",
  "大吉 · 代码无 Bug，今日跑图一遍过"
];

const todayAlmanac = computed(() => {
  const now = new Date();
  const dateStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;

  let hash = 0;
  for (let i = 0; i < dateStr.length; i++) {
    hash = (hash * 31 + dateStr.charCodeAt(i) + i * 7) % 1000000007;
  }

  const gLen = ALMANAC_GOOD_ITEMS.length;
  const bLen = ALMANAC_BAD_ITEMS.length;
  const fLen = ALMANAC_FORTUNES.length;

  const idxG1 = Math.abs(hash) % gLen;
  const idxG2 = (idxG1 + 1 + (Math.abs(hash * 3 + 7) % (gLen - 1))) % gLen;

  const idxB1 = Math.abs(hash * 11 + 13) % bLen;
  const idxB2 = (idxB1 + 1 + (Math.abs(hash * 17 + 19) % (bLen - 1))) % bLen;

  const idxF = Math.abs(hash * 23 + 5) % fLen;

  return {
    good: [ALMANAC_GOOD_ITEMS[idxG1], ALMANAC_GOOD_ITEMS[idxG2]],
    bad: [ALMANAC_BAD_ITEMS[idxB1], ALMANAC_BAD_ITEMS[idxB2]],
    fortune: ALMANAC_FORTUNES[idxF],
  };
});

const wheelColors = ["#e0f2fe", "#fef3c7", "#dcfce7", "#ede9fe", "#ffe4e6", "#f0fdf4", "#e0e7ff"];
const currentDate = ref("");
const currentClock = ref("");
let clockTimer = null;
const userCheckinDates = ref(new Set());
const localSpinningDone = ref(false);
const localWonAward = ref("");
const localFruitClaimed = ref(false);

const currentMemberId = computed(() => {
  return authStore.profile.email || teamStore.members.find((member) => member.isCurrentUser)?.id || "";
});

watch(currentMemberId, (newId) => {
  if (newId) {
    localWonAward.value = "";
    localFruitClaimed.value = false;
    loadCheckinDates();
    teamStore.loadFromServer().catch(() => {});
  }
});

const todayCheckin = computed(() => {
  const today = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai" }).format(new Date());
  return teamStore.checkins
    .filter((item) => item.memberId === currentMemberId.value)
    .find((item) => !item.date || String(item.date || item.checkinDate || item.time || "").slice(0, 10) === today);
});

const isClaimed = computed(() => {
  return (todayCheckin.value?.fruitClaimed || localFruitClaimed.value) && !spinning.value;
});

const isNoPrize = computed(() => {
  return todayCheckin.value?.awardType === "nothing"
    || todayCheckin.value?.awardName === "遗憾未中奖"
    || localWonAward.value === "遗憾未中奖";
});

const awardDisplay = computed(() => {
  if (localWonAward.value) return localWonAward.value;
  if (todayCheckin.value?.awardType === "nothing" || todayCheckin.value?.awardName === "遗憾未中奖") {
    return "未中奖";
  }
  if (todayCheckin.value?.awardName) return todayCheckin.value.awardName;
  if (todayCheckin.value?.fruitAward) return `${todayCheckin.value.fruitAward} 积分`;
  return "签到奖励";
});

const checkinTimeFormatted = computed(() => {
  const t = todayCheckin.value?.time || todayCheckin.value?.checkinDate || "";
  const now = new Date();
  const currentMonth = now.getMonth() + 1;
  const currentDay = now.getDate();

  if (!t) return `${currentMonth}月${currentDay}日`;

  const match = String(t).match(/(\d{4})?-?(\d{1,2})[-/](\d{1,2})\s*(\d{1,2}:\d{2})/);
  if (match) {
    const month = parseInt(match[2], 10);
    const day = parseInt(match[3], 10);
    const time = match[4];
    return `${month}月${day}日 ${time}`;
  }

  const timeOnly = String(t).match(/^(\d{1,2}:\d{2})/);
  if (timeOnly) {
    return `${currentMonth}月${currentDay}日 ${timeOnly[1]}`;
  }

  try {
    const d = new Date(String(t).replace(/-/g, "/"));
    if (!isNaN(d.getTime())) {
      const month = d.getMonth() + 1;
      const day = d.getDate();
      const hours = String(d.getHours()).padStart(2, "0");
      const minutes = String(d.getMinutes()).padStart(2, "0");
      return `${month}月${day}日 ${hours}:${minutes}`;
    }
  } catch (e) {}

  return `${currentMonth}月${currentDay}日 ${t}`;
});

const statusSentence = computed(() => {
  if (!todayCheckin.value?.status) return "今天还没签到，点击右侧按钮记录今天的科研节奏。";
  if (isClaimed.value) {
    const rewardText = isNoPrize.value ? "本次未中奖" : `获得 ${awardDisplay.value}`;
    return `您今日已于 ${todayCheckin.value.time || "今天"} 完成签到，${rewardText}，已连续 ${todayCheckin.value.streak || 1} 天。`;
  }
  return `您今日已于 ${todayCheckin.value.time || "今天"} 完成签到，已连续 ${todayCheckin.value.streak || 1} 天，待抽取奖励。`;
});

const actionLabel = computed(() => {
  if (!todayCheckin.value?.status) return "签到";
  if (!todayCheckin.value?.fruitClaimed) return "去抽奖";
  return "查看";
});

const modalTitle = computed(() => {
  if (!todayCheckin.value?.status) return "先完成今日签到";
  if (isClaimed.value) return "今日奖励已入账";
  if (spinning.value) return "正在抽取今日奖励...";
  return "转动今日积分盘";
});

const modalText = computed(() => {
  if (!todayCheckin.value?.status) return "点击签到后，再亲手抽取今日奖励。";
  if (isClaimed.value) return `你今天获得 ${awardDisplay.value}，权益与进度已经更新。`;
  if (spinning.value) return "幸运转盘正在高速旋转，好运马上揭晓...";
  return "连续签到越久，高额积分与翻译次数概率越高。点击下方按钮抽取今日奖励。";
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
  const cx = 130;
  const cy = 130;
  const r = 110;
  const inner = 50;
  const slice = 360 / WHEEL_ITEMS.length;
  const start = -90 - slice / 2 + index * slice;
  const end = start + slice;
  const p1 = polarToCartesian(cx, cy, r, start);
  const p2 = polarToCartesian(cx, cy, r, end);
  const p3 = polarToCartesian(cx, cy, inner, end);
  const p4 = polarToCartesian(cx, cy, inner, start);
  return `M ${p1.x} ${p1.y} A ${r} ${r} 0 0 1 ${p2.x} ${p2.y} L ${p3.x} ${p3.y} A ${inner} ${inner} 0 0 0 ${p4.x} ${p4.y} Z`;
}

const wheelSegments = computed(() => {
  const slice = 360 / WHEEL_ITEMS.length;
  return WHEEL_ITEMS.map((item, index) => {
    const angle = -90 + index * slice;
    const textPoint = polarToCartesian(130, 130, 80, angle);
    return {
      ...item,
      angleDeg: angle,
      path: describeSegment(index),
      fill: wheelColors[index % wheelColors.length],
      textX: textPoint.x,
      textY: textPoint.y,
    };
  });
});

// ── 月度日历逻辑 ──
const nowTime = new Date();
const currentYear = nowTime.getFullYear();
const currentMonth = nowTime.getMonth();

const calendarMonthLabel = computed(() => {
  return `${currentYear} 年 ${currentMonth + 1} 月`;
});

const currentMonthCheckedCount = computed(() => {
  let count = 0;
  userCheckinDates.value.forEach(dateStr => {
    if (dateStr.startsWith(`${currentYear}-${String(currentMonth + 1).padStart(2, '0')}`)) {
      count += 1;
    }
  });
  return count;
});

const calendarDays = computed(() => {
  const firstDayOfMonth = new Date(currentYear, currentMonth, 1);
  const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
  // Monday is 0, Sunday is 6
  let firstDayWeekday = firstDayOfMonth.getDay() - 1;
  if (firstDayWeekday < 0) firstDayWeekday = 6;

  const todayStr = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(nowTime.getDate()).padStart(2, '0')}`;
  const days = [];

  // Pad empty days
  for (let i = 0; i < firstDayWeekday; i++) {
    days.push({ dateKey: `empty-${i}`, dayNum: 0 });
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const dateKey = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    const isToday = dateKey === todayStr;
    const checked = userCheckinDates.value.has(dateKey);
    days.push({
      dateKey,
      dayNum: d,
      isToday,
      checked,
      tooltip: `${dateKey}${checked ? '：已签到打卡' : isToday ? '：今天待打卡' : ''}`
    });
  }

  return days;
});

async function loadCheckinDates() {
  const memberId = currentMemberId.value;
  if (!memberId) return;
  try {
    const history = await paperpilotApi.getTeamCheckinHistory(memberId, currentYear);
    const set = new Set();
    (history || []).forEach(item => {
      if (item.date && (item.status === '已打卡' || item.fruitClaimed)) {
        set.add(item.date);
      }
    });
    if (todayCheckin.value?.status === '已打卡') {
      const today = new Date().toISOString().slice(0, 10);
      set.add(today);
    }
    userCheckinDates.value = set;
  } catch (err) {
    console.warn("Failed to load checkin dates:", err);
  }
}

function closeModal() {
  if (busy.value) return;
  showModal.value = false;
}

async function handleCheckinClick() {
  if (busy.value) return;
  await loadCheckinDates();
  if (todayCheckin.value?.status === "已打卡") {
    showModal.value = true;
    return;
  }
  busy.value = true;
  try {
    await teamStore.performCheckin(currentMemberId.value);
    const today = new Date().toISOString().slice(0, 10);
    userCheckinDates.value.add(today);
    showModal.value = true;
  } catch (error) {
    notify(error.response?.data?.message || "签到失败，请稍后重试");
  } finally {
    busy.value = false;
  }
}

function calculateTargetRotation(prizeIndex) {
  const segment = 360 / WHEEL_ITEMS.length;
  // Prize index 0 is at top (-90deg relative to wheel canvas).
  // Pointer is at top (0deg from center top).
  // Target wheel angle to align prizeIndex with pointer:
  const targetOffset = (360 - prizeIndex * segment) % 360;
  const currentNormalized = ((wheelRotation.value % 360) + 360) % 360;
  let delta = (targetOffset - currentNormalized + 360) % 360;
  if (delta < 180) delta += 360; // Make at least 1 full revolution delta
  return 1440 + delta; // 4 extra complete 360 spins for exciting physics
}

async function drawFruit() {
  if (busy.value || todayCheckin.value?.fruitClaimed) return;
  busy.value = true;
  spinning.value = true;

  try {
    const saved = await teamStore.drawCheckinFruit(currentMemberId.value);
    localFruitClaimed.value = true;
    localWonAward.value = saved?.awardName || (Number(saved?.fruitAward) > 0 ? `${saved.fruitAward} 积分` : "遗憾未中奖");
    let prizeIndex = Number(saved?.prizeIndex);
    if (isNaN(prizeIndex) || prizeIndex < 0 || prizeIndex >= WHEEL_ITEMS.length) {
      if (saved?.awardType === "full_translate") prizeIndex = 5;
      else if (saved?.awardType === "bilingual_translate") prizeIndex = 6;
      else {
        const pts = Number(saved?.fruitAward || 1);
        prizeIndex = Math.min(4, Math.max(0, pts - 1));
      }
    }

    const deltaRotation = calculateTargetRotation(prizeIndex);
    wheelRotation.value += deltaRotation;

    const today = new Date().toISOString().slice(0, 10);
    userCheckinDates.value.add(today);

    // Refresh auth user profile to ensure quotas and scores sync
    authStore.refreshProfile();

    window.setTimeout(() => {
      spinning.value = false;
      busy.value = false;
      const noPrize = saved?.awardType === "nothing" || saved?.awardName === "遗憾未中奖";
      notify(noPrize ? "本次未中奖，明天再来。" : `恭喜获得：${saved.awardName || awardDisplay.value}！`);
    }, 1250);
  } catch (error) {
    spinning.value = false;
    busy.value = false;
    notify(error.response?.data?.message || "抽取奖励失败，请稍后重试");
  }
}

onMounted(() => {
  teamStore.loadFromServer().catch(() => {});
  updateClock();
  clockTimer = window.setInterval(updateClock, 1000);
  loadCheckinDates();
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
  gap: 24px;
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

/* ── Academic Almanac (今日宜忌 - 3行对齐无背景大日历) ── */
.checkin-almanac-strip {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  flex: 1 1 auto;
  padding: 0 6px;
}

.almanac-calendar-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4f46e5;
  flex-shrink: 0;
  background: none !important;
  border: none !important;
  box-shadow: none !important;
  padding: 0;
  transition: transform 0.2s ease, color 0.2s ease;
}
:root[data-theme="dark"] .almanac-calendar-icon-wrap {
  color: #818cf8;
}

.almanac-3rows {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  min-width: 0;
  flex: 1 1 auto;
}

.almanac-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  font-weight: 750;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.almanac-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 19px;
  height: 19px;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 900;
  line-height: 1;
  color: #ffffff;
  flex-shrink: 0;
}
.good-badge {
  background: #10b981;
}
.bad-badge {
  background: #ef4444;
}

.almanac-line.almanac-good .almanac-text {
  color: #047857;
  font-weight: 800;
  letter-spacing: 0.15px;
}
:root[data-theme="dark"] .almanac-line.almanac-good .almanac-text {
  color: #6ee7b7;
}

.almanac-line.almanac-bad .almanac-text {
  color: #b91c1c;
  font-weight: 800;
  letter-spacing: 0.15px;
}
:root[data-theme="dark"] .almanac-line.almanac-bad .almanac-text {
  color: #fca5a5;
}

.almanac-sparkle {
  font-size: 13.5px;
  line-height: 1;
}

.almanac-line.almanac-fortune {
  color: #4f46e5;
  font-size: 13px;
  font-weight: 750;
}
:root[data-theme="dark"] .almanac-line.almanac-fortune {
  color: #a5b4fc;
}
.checkin-card-copy {
  min-width: 0;
  flex: 0 1 auto;
}

.checkin-exp-tip {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 8px;
  font-size: 12px;
  color: #4f46e5;
  line-height: 1.4;
}

.checkin-exp-tip svg {
  flex-shrink: 0;
  color: #6366f1;
}

:root[data-theme="dark"] .checkin-exp-tip {
  background: rgba(99, 102, 241, 0.15);
  border-color: rgba(99, 102, 241, 0.35);
  color: #a5b4fc;
}

/* ── Checkin Calendar Box ── */
.checkin-calendar-box {
  margin: 14px 0 10px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  text-align: left;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 13px;
  color: #334155;
}

.calendar-header strong {
  font-weight: 700;
}

.calendar-stat {
  font-size: 12px;
  color: #6366f1;
  font-weight: 600;
}

.calendar-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.calendar-weekdays,
.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  text-align: center;
}

.calendar-weekdays span {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 600;
  padding: 2px 0;
}

.calendar-day {
  position: relative;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 500;
  color: #475569;
  background: transparent;
  transition: all 0.15s ease;
}

.calendar-day.is-today {
  background: #e0e7ff;
  color: #4338ca;
  font-weight: 700;
}

.calendar-day.is-checked {
  background: #dcfce7 !important;
  color: #15803d !important;
  font-weight: 700;
}

.check-icon {
  position: absolute;
  right: 1px;
  bottom: 1px;
  width: 12px;
  height: 12px;
  color: #16a34a;
}

:root[data-theme="dark"] .checkin-calendar-box {
  background: #1e293b !important;
  border-color: #334155 !important;
}

:root[data-theme="dark"] .calendar-header {
  color: #e2e8f0 !important;
}

:root[data-theme="dark"] .calendar-stat {
  color: #818cf8 !important;
}

:root[data-theme="dark"] .calendar-day {
  color: #94a3b8 !important;
}

:root[data-theme="dark"] .calendar-day.is-today {
  background: rgba(99, 102, 241, 0.25) !important;
  color: #a5b4fc !important;
}

:root[data-theme="dark"] .calendar-day.is-checked {
  background: rgba(34, 197, 94, 0.25) !important;
  color: #86efac !important;
}

:root[data-theme="dark"] .check-icon {
  color: #4ade80 !important;
}

.checkin-card-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  text-align: right;
  min-width: 0;
  flex: 0 0 auto;
  padding-right: 2px;
}

.greeting-salute {
  margin: 0;
  color: #64748b;
  font-size: 13.5px;
  font-weight: 750;
  line-height: 1.35;
  letter-spacing: 0.2px;
}
:root[data-theme="dark"] .greeting-salute {
  color: #94a3b8;
}

.greeting-status {
  margin: 2px 0 0;
  color: #1e293b;
  font-size: 15px;
  font-weight: 850;
  line-height: 1.35;
  letter-spacing: 0.2px;
  white-space: nowrap;
}
:root[data-theme="dark"] .greeting-status {
  color: #f1f5f9;
}

.checkin-lottery-card.checked .greeting-status {
  color: #0f172a;
}
:root[data-theme="dark"] .checkin-lottery-card.checked .greeting-status {
  color: #f8fafc;
}

.checkin-time-highlight {
  color: #4f46e5;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}
:root[data-theme="dark"] .checkin-time-highlight {
  color: #818cf8;
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
  border: 1px solid rgba(79, 70, 229, 0.18);
  border-radius: 999px;
  color: #ffffff;
  background:
    radial-gradient(circle at 18% 20%, rgba(125, 211, 252, 0.52), transparent 34%),
    linear-gradient(135deg, #4f46e5 0%, #2563eb 54%, #0891b2 100%);
  font-size: 17px;
  font-weight: 850;
  cursor: pointer;
  box-shadow: 0 16px 30px rgba(37, 99, 235, .22);
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease, background 160ms ease;
}

.checkin-card-action:hover {
  transform: translateY(-1px);
  border-color: rgba(96, 165, 250, 0.42);
  box-shadow: 0 18px 34px rgba(37, 99, 235, .28);
}

.checkin-lottery-card.checked .checkin-card-action {
  color: #ffffff;
  border-color: rgba(99, 102, 241, 0.38);
  background:
    radial-gradient(circle at 20% 18%, rgba(216, 180, 254, 0.44), transparent 32%),
    linear-gradient(135deg, #5b5cf6 0%, #3b82f6 58%, #06b6d4 100%);
  box-shadow: 0 16px 34px rgba(59, 130, 246, .26);
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
  width: min(840px, 94vw);
  padding: 34px 38px;
  border: 1px solid rgba(148, 163, 184, .24);
  border-radius: 28px;
  background:
    radial-gradient(circle at 18% 8%, rgba(34, 211, 238, .12), transparent 34%),
    radial-gradient(circle at 92% 14%, rgba(79, 70, 229, .16), transparent 34%),
    #ffffff;
  box-shadow: 0 28px 80px rgba(15, 23, 42, .26);
  color: #0f172a;
}

.checkin-modal-horizontal {
  display: grid;
  grid-template-columns: 1.15fr 0.95fr;
  gap: 36px;
  align-items: center;
}

.checkin-modal-left {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.checkin-modal-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.8), rgba(241, 245, 249, 0.6));
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 24px;
  padding: 24px 20px;
}

:root[data-theme="dark"] .checkin-modal-right {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(51, 65, 85, 0.8);
}

.checkin-bottom-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
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
  z-index: 10;
}

.lottery-stage {
  position: relative;
  width: 270px;
  height: 270px;
  display: grid;
  place-items: center;
  margin: 0 auto 16px;
}

.lottery-wheel {
  width: 246px;
  height: 246px;
  overflow: visible;
  border-radius: 50%;
  filter: drop-shadow(0 18px 26px rgba(30, 41, 59, .16));
  transition: transform 1050ms cubic-bezier(.16, 1, .3, 1);
}

.lottery-wheel.spinning {
  transition-duration: 1200ms;
}

.lottery-wheel path {
  stroke: rgba(148, 163, 184, .35);
  stroke-width: 1.2;
}

.lottery-wheel text {
  color: #0f172a;
  fill: #0f172a;
}

/* Pale reward slices need a high-contrast halo in the light theme. */
.lottery-wheel > g text {
  paint-order: stroke fill;
  stroke: rgba(255, 255, 255, .96);
  stroke-width: 2.6px;
  stroke-linejoin: round;
}

:root[data-theme="dark"] .lottery-wheel > g text {
  /* Keep the same light halo in dark mode: the slices remain pale and the
     reward labels stay dark, so a dark halo would merge into the glyphs. */
  stroke: rgba(255, 255, 255, .96);
}

.wheel-center-outer {
  fill: #fff;
  stroke: rgba(15, 23, 42, .1);
  stroke-width: 1.5;
}

.wheel-center-inner {
  fill: #eff6ff;
}

.wheel-center-text {
  fill: #4f46e5;
  font-size: 15px;
  font-weight: 950;
}

.lottery-pointer {
  position: absolute;
  top: 0px;
  left: 50%;
  z-index: 5;
  width: 32px;
  height: 38px;
  background: #4f46e5;
  clip-path: polygon(50% 100%, 0 0, 100% 0);
  transform: translateX(-50%);
  filter: drop-shadow(0 9px 10px rgba(79, 70, 229, .3));
}

.lottery-copy span {
  color: #2563eb;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: .08em;
  text-transform: uppercase;
}

.lottery-copy h3 {
  margin: 6px 0 8px;
  color: #111827;
  font-size: 24px;
  line-height: 1.2;
  text-wrap: balance;
}

.lottery-copy p {
  margin: 0;
  color: #475569;
  font-size: 13.5px;
  line-height: 1.6;
}

.lottery-primary {
  width: 100%;
  max-width: 240px;
  min-height: 46px;
  margin-top: 8px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: linear-gradient(135deg, #4f46e5, #2563eb);
  font-size: 15px;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 12px 22px rgba(37, 99, 235, .24);
  transition: all 0.2s ease;
}

.lottery-primary:hover:not(:disabled) {
  transform: translateY(-1.5px);
  box-shadow: 0 16px 28px rgba(37, 99, 235, .32);
}

.lottery-primary:disabled {
  cursor: not-allowed;
  opacity: .7;
}

.checkin-bottom-info {
  display: flex;
  align-items: stretch;
  gap: 14px;
  width: 100%;
}

.checkin-bottom-info > div {
  flex: 1;
}

.lottery-result,
.lottery-streak {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0;
  min-height: 52px;
  padding: 10px 18px;
  border-radius: 14px;
  background: #f4f8ff;
  box-sizing: border-box;
}

.lottery-result {
  color: #1e3a8a;
  background: #eff6ff;
  border: 1px solid rgba(191, 219, 254, 0.6);
}

.lottery-result span,
.lottery-streak span {
  font-size: 13px;
  font-weight: 800;
  color: #475569;
}

.lottery-result strong {
  font-size: 20px;
  font-weight: 900;
  color: #2563eb;
  line-height: 1;
}

.lottery-streak {
  color: #1e3a8a;
  background: #f8fafc;
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.lottery-streak strong {
  font-size: 20px;
  font-weight: 900;
  color: #0f172a;
  line-height: 1;
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
    radial-gradient(circle at 18% 8%, rgba(34, 211, 238, .12), transparent 34%),
    radial-gradient(circle at 92% 14%, rgba(99, 102, 241, .2), transparent 34%),
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

:root[data-theme="dark"] .checkin-card-copy p {
  color: #a8b3c7 !important;
}

:root[data-theme="dark"] .checkin-card-time span {
  color: #94a3b8 !important;
}

:root[data-theme="dark"] .checkin-card-time strong {
  color: #eef4ff !important;
}

:root[data-theme="dark"] .checkin-card-time b {
  color: #f8fafc !important;
}

:root[data-theme="dark"] .checkin-lottery-card.checked .checkin-card-action {
  color: #ffffff !important;
  border-color: rgba(165, 180, 252, 0.58) !important;
  background:
    radial-gradient(circle at 22% 20%, rgba(216, 180, 254, 0.5), transparent 34%),
    linear-gradient(135deg, #6366f1 0%, #3b82f6 58%, #06b6d4 100%) !important;
  text-shadow: 0 1px 0 rgba(15, 23, 42, .22) !important;
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, .06) inset,
    0 16px 34px rgba(37, 99, 235, .32),
    0 0 22px rgba(99, 102, 241, .22) !important;
}

:root[data-theme="dark"] .lottery-streak {
  color: #c7d2fe !important;
  background: rgba(79, 70, 229, .14) !important;
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
