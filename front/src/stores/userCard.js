import { defineStore } from "pinia";
import { reactive } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";

export const useUserCardStore = defineStore("user-card", () => {
  const state = reactive({
    open: false,
    loading: false,
    user: null,
    error: "",
  });

  async function open(userId) {
    if (!userId) return;
    state.open = true;
    state.loading = true;
    state.error = "";
    try {
      state.user = await paperpilotApi.getUserCard(userId);
    } catch {
      state.user = null;
      state.error = "未能读取用户资料";
    } finally {
      state.loading = false;
    }
  }

  async function openByEmail(email) {
    if (!email) return;
    state.open = true;
    state.loading = true;
    state.error = "";
    try {
      state.user = await paperpilotApi.getUserCardByEmail(email);
    } catch {
      state.user = null;
      state.error = "未能读取用户资料";
    } finally {
      state.loading = false;
    }
  }

  function close() {
    state.open = false;
  }

  async function addFriend() {
    if (!state.user?.userId) return;
    const result = await paperpilotApi.sendFriendRequest(state.user.userId, {
      message: "希望与你建立站内科研联系",
    });
    state.user.friendshipStatus = result.status;
    window.dispatchEvent(new CustomEvent("paperpilot:friend-requests-changed"));
  }

  return { state, open, openByEmail, close, addFriend };
});
