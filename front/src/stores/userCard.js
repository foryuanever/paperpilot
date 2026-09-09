import { defineStore } from "pinia";
import { reactive } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";

export const useUserCardStore = defineStore("user-card", () => {
  const state = reactive({
    open: false,
    loading: false,
    requesting: false,
    requestError: "",
    user: null,
    error: "",
  });

  async function open(userId) {
    if (!userId) return;
    state.open = true;
    state.loading = true;
    state.requestError = "";
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

  async function requestContact() {
    if (!state.user?.userId || state.requesting) return;
    state.requesting = true;
    state.error = "";
    try {
      const result = await paperpilotApi.sendFriendRequest(state.user.userId, {
        message: "希望获取你的联系方式，便于后续科研交流",
      });
      if (result.status === "friends") {
        state.user = await paperpilotApi.getUserCard(state.user.userId);
      } else {
        state.user.friendshipStatus = result.status;
        state.user.contactStatus = result.status;
      }
      window.dispatchEvent(new CustomEvent("paperpilot:contact-requests-changed"));
      return result;
    } catch (error) {
      state.requestError = error?.response?.data?.message || error?.message || "联系方式申请失败，请稍后重试";
      throw error;
    } finally {
      state.requesting = false;
    }
  }

  return { state, open, openByEmail, close, requestContact };
});
