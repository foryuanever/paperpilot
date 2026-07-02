<template>
  <Transition name="card-fade">
    <div v-if="store.state.open" class="user-card-overlay" @click="store.close">
      <section class="user-card" @click.stop>
        <button class="card-close" @click="store.close">×</button>
        <div v-if="store.state.loading" class="card-state">正在读取个人资料...</div>
        <div v-else-if="store.state.error" class="card-state error">{{ store.state.error }}</div>
        <template v-else-if="user">
          <div class="card-cover">
            <span class="cover-orbit one"></span>
            <span class="cover-orbit two"></span>
          </div>
          <div class="card-profile">
            <div class="card-avatar">{{ user.avatar }}</div>
            <div class="card-identity">
              <span class="role-badge">{{ user.role }}</span>
              <h2>{{ user.name }}</h2>
              <p>{{ user.email }}</p>
            </div>
          </div>
          <div class="card-stats">
            <div><span>注册时间</span><strong>{{ user.registerTime || "未知" }}</strong></div>
            <div><span>科研活跃</span><strong>{{ activeTimeText }}</strong></div>
            <div><span>团队状态</span><strong>{{ user.teamId ? "已加入团队" : "个人研究者" }}</strong></div>
          </div>
          <p class="card-note">站内科研身份卡片。添加好友后可在私信模块集中管理联系与申请。</p>
          <footer>
            <button v-if="!user.isSelf" class="message-action" @click="openMessages">发私信</button>
            <button
              v-if="!user.isSelf"
              class="friend-action"
              :disabled="friendButtonDisabled"
              @click="store.addFriend"
            >
              {{ friendshipLabel }}
            </button>
            <router-link v-else to="/profile" class="friend-action" @click="store.close">查看我的主页</router-link>
          </footer>
        </template>
      </section>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useUserCardStore } from "../stores/userCard";

const store = useUserCardStore();
const router = useRouter();
const user = computed(() => store.state.user);
const activeTimeText = computed(() => {
  const seconds = Number(user.value?.activeTime || 0);
  const hours = Math.floor(seconds / 3600);
  return hours ? `${hours} 小时` : `${Math.floor(seconds / 60)} 分钟`;
});
const friendshipLabel = computed(() => ({
  friends: "已是好友",
  outgoing_pending: "申请已发送",
  incoming_pending: "对方申请了你",
  none: "添加好友",
}[user.value?.friendshipStatus] || "添加好友"));
const friendButtonDisabled = computed(() =>
  ["friends", "outgoing_pending", "incoming_pending"].includes(user.value?.friendshipStatus),
);

function openMessages() {
  const userId = user.value.userId;
  store.close();
  router.push({ path: "/messages", query: { contact: userId } });
}
</script>

<style scoped>
.user-card-overlay { position: fixed; inset: 0; z-index: 12000; display: grid; place-items: center; padding: 24px; background: rgba(15,23,38,.46); }
.user-card { position: relative; width: min(430px, calc(100vw - 32px)); overflow: hidden; border: 1px solid rgba(255,255,255,.8); border-radius: 24px; background: #fff; box-shadow: 0 30px 90px rgba(15,28,52,.28); }
.card-close { position: absolute; z-index: 3; top: 13px; right: 13px; width: 34px; height: 34px; border: 0; border-radius: 50%; color: #fff; background: rgba(24,35,54,.36); font-size: 22px; }
.card-state { padding: 90px 30px; color: #7d899b; text-align: center; }
.card-state.error { color: #bd4056; }
.card-cover { position: relative; height: 118px; overflow: hidden; background: linear-gradient(135deg, #10234c, #0865ee 62%, #6b52df); }
.cover-orbit { position: absolute; border: 1px solid rgba(255,255,255,.22); border-radius: 50%; }
.cover-orbit.one { width: 180px; height: 180px; top: -90px; right: -20px; }
.cover-orbit.two { width: 110px; height: 110px; left: 22px; bottom: -80px; }
.card-profile { display: flex; align-items: flex-end; gap: 15px; padding: 0 24px; margin-top: -34px; position: relative; }
.card-avatar { width: 78px; height: 78px; flex: 0 0 auto; display: grid; place-items: center; border: 5px solid #fff; border-radius: 23px; color: #fff; background: linear-gradient(135deg, #176ce4, #683fd5); box-shadow: 0 10px 25px rgba(41,68,135,.22); font-size: 27px; font-weight: 850; }
.card-identity { min-width: 0; padding-bottom: 5px; }
.role-badge { padding: 3px 7px; border-radius: 6px; color: #0865ee; background: #eaf3ff; font-size: 9px; font-weight: 800; }
.card-identity h2 { margin: 7px 0 3px; color: #172033; font-size: 20px; }
.card-identity p { margin: 0; overflow: hidden; color: #8b96a7; text-overflow: ellipsis; white-space: nowrap; font-size: 10px; }
.card-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; padding: 22px 24px 14px; }
.card-stats div { padding: 11px 8px; display: flex; flex-direction: column; gap: 5px; border-radius: 11px; background: #f5f7fa; text-align: center; }
.card-stats span { color: #929cab; font-size: 8px; }
.card-stats strong { color: #344158; font-size: 10px; }
.card-note { margin: 0 24px; padding: 12px 14px; border: 1px solid #e3eaf4; border-radius: 11px; color: #69768a; background: #f9fbfe; font-size: 10px; line-height: 1.65; }
footer { display: flex; gap: 9px; padding: 18px 24px 23px; }
footer button, footer a { min-height: 40px; flex: 1; display: grid; place-items: center; border-radius: 10px; font-size: 11px; font-weight: 800; text-decoration: none; }
.message-action { border: 1px solid #dce4ef; color: #526077; background: #fff; }
.friend-action { border: 0; color: #fff; background: #0865ee; }
.friend-action:disabled { color: #788498; background: #edf1f6; }
.card-fade-enter-active, .card-fade-leave-active { transition: opacity .18s ease; }
.card-fade-enter-from, .card-fade-leave-to { opacity: 0; }
</style>
