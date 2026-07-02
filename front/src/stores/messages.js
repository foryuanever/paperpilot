import { defineStore } from "pinia";
import { reactive } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";

export const useMessagesStore = defineStore("messages", () => {
  const state = reactive({
    contacts: [],
    messages: [],
    activeContactId: null,
    unreadCount: 0,
    friendRequests: { incoming: [], outgoing: [], pendingCount: 0 },
    loading: false,
  });

  async function fetchContacts() {
    const result = await paperpilotApi.getMessageContacts();
    state.contacts = result.contacts || [];
    state.unreadCount = result.unreadCount || 0;
    return state.contacts;
  }

  async function fetchFriendRequests() {
    state.friendRequests = await paperpilotApi.getFriendRequests();
    return state.friendRequests;
  }

  async function handleFriendRequest(requestId, action) {
    await paperpilotApi.handleFriendRequest(requestId, action);
    await Promise.all([fetchFriendRequests(), fetchContacts()]);
  }

  async function openThread(userId) {
    state.activeContactId = Number(userId);
    state.loading = true;
    try {
      state.messages = await paperpilotApi.getMessageThread(userId);
      await fetchContacts();
    } finally {
      state.loading = false;
    }
  }

  async function sendMessage(payload) {
    if (!state.activeContactId) return;
    await paperpilotApi.sendDirectMessage(state.activeContactId, payload);
    await openThread(state.activeContactId);
  }

  return { state, fetchContacts, fetchFriendRequests, handleFriendRequest, openThread, sendMessage };
});
