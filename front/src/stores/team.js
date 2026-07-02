import { computed, reactive } from "vue";
import { defineStore } from "pinia";
import { paperpilotApi } from "../services/paperpilotApi";
import { useAuthStore } from "./auth";

function parseAttachments(value) {
  if (Array.isArray(value)) return value;
  if (!value) return [];
  try {
    return JSON.parse(value);
  } catch {
    return [];
  }
}

export const useTeamStore = defineStore("team", () => {
  const authStore = useAuthStore();
  const state = reactive({
    members: [],
    totalSeats: 8,
    groupTokenPool: 10000000,
    tasks: [],
    announcements: [],
    checkins: [],
    resources: [],
    teamInfo: null,
    loading: false,
    loadError: "",
  });
  let loadPromise = null;

  const members = computed(() => state.members.map(member => ({
    ...member,
    isCurrentUser: member.email === authStore.profile.email,
  })));
  const totalSeats = computed(() => state.totalSeats);
  const usedSeats = computed(() => state.members.length);
  const groupTokenPool = computed(() => state.groupTokenPool);
  const tasks = computed(() => state.tasks);
  const announcements = computed(() => state.announcements);
  const checkins = computed(() => state.checkins);
  const resources = computed(() => state.resources);
  const teamInfo = computed(() => state.teamInfo);
  const teamIdentifier = computed(() => state.teamInfo?.identifier || "未分配");

  async function loadFromServer() {
    if (loadPromise) return loadPromise;
    state.loading = true;
    state.loadError = "";
    loadPromise = Promise.allSettled([
        paperpilotApi.getTeamInfo(),
        paperpilotApi.getTeamMembers(),
        paperpilotApi.getTeamTasks(),
        paperpilotApi.getTeamAnnouncements(),
        paperpilotApi.getTeamResources(),
        paperpilotApi.getTeamCheckins(),
      ])
      .then((results) => {
        const [teamData, memberData, taskData, announcementData, resourceData, checkinData] = results;
        if (teamData.status === "fulfilled") {
          state.teamInfo = teamData.value;
          if (teamData.value && typeof teamData.value.seatLimit === "number") {
            state.totalSeats = teamData.value.seatLimit;
          }
        }
        if (memberData.status === "fulfilled") state.members = memberData.value;
        if (taskData.status === "fulfilled") {
          state.tasks = taskData.value.map(task => ({ ...task, attachments: parseAttachments(task.attachments) }));
        }
        if (announcementData.status === "fulfilled") state.announcements = announcementData.value;
        if (resourceData.status === "fulfilled") state.resources = resourceData.value;
        if (checkinData.status === "fulfilled") state.checkins = checkinData.value;

        const failed = results.filter(result => result.status === "rejected");
        if (failed.length) {
          state.loadError = `有 ${failed.length} 项团队数据暂时加载失败`;
          console.error("Failed to load part of team data from MySQL:", failed.map(item => item.reason));
        }
      })
      .finally(() => {
        loadPromise = null;
        state.loading = false;
      });
    try {
      await loadPromise;
    } finally {
      state.loading = false;
    }
  }

  async function addMember(member) {
    if (state.members.length >= state.totalSeats) throw new Error("席位不足，请升级套餐以增加席位");
    const saved = await paperpilotApi.addMember(member);
    state.members.push(saved);
  }

  async function removeMember(memberId) {
    await paperpilotApi.deleteMember(memberId);
    state.members = state.members.filter(member => member.id !== memberId);
    state.checkins = state.checkins.filter(checkin => checkin.memberId !== memberId);
  }

  async function updateRole(memberId, role) {
    await paperpilotApi.updateMemberRole(memberId, { role });
    const member = state.members.find(item => item.id === memberId);
    if (member) member.role = role;
  }

  async function updateQuota(memberId, tokenLimit) {
    await paperpilotApi.updateMemberQuota(memberId, { tokenLimit });
    const member = state.members.find(item => item.id === memberId);
    if (member) member.tokenLimit = tokenLimit;
  }

  async function addTask(title, description, deadline, attachments = []) {
    const saved = await paperpilotApi.createTeamTask({
      title,
      description,
      deadline,
      attachments: JSON.stringify(attachments),
    });
    state.tasks.unshift({ ...saved, attachments: parseAttachments(saved.attachments) });
  }

  async function updateTask(id, title, description, deadline, attachments = []) {
    const saved = await paperpilotApi.updateTeamTask(id, {
      title,
      description,
      deadline,
      attachments: JSON.stringify(attachments),
    });
    const index = state.tasks.findIndex(task => task.id === id);
    if (index >= 0) {
      state.tasks[index] = { ...saved, attachments: parseAttachments(saved.attachments) };
    }
  }

  async function deleteTask(id) {
    await paperpilotApi.deleteTeamTask(id);
    state.tasks = state.tasks.filter(task => task.id !== id);
  }

  async function addAnnouncement(
    title,
    content,
    image = "",
    link = "",
    attachmentName = "",
    attachmentType = "",
    attachmentData = "",
    attachmentSize = "",
  ) {
    const saved = await paperpilotApi.createTeamAnnouncement({
      title,
      content,
      image,
      link,
      attachmentName,
      attachmentType,
      attachmentData,
      attachmentSize,
    });
    state.announcements.unshift(saved);
  }

  async function updateAnnouncement(id, payload) {
    const saved = await paperpilotApi.updateTeamAnnouncement(id, payload);
    const index = state.announcements.findIndex(announcement => announcement.id === id);
    if (index >= 0) state.announcements[index] = saved;
  }

  async function deleteAnnouncement(id) {
    await paperpilotApi.deleteTeamAnnouncement(id);
    state.announcements = state.announcements.filter(announcement => announcement.id !== id);
  }

  async function performCheckin(memberId) {
    const saved = await paperpilotApi.performTeamCheckin({ memberId, status: "已打卡" });
    const index = state.checkins.findIndex(item => item.memberId === memberId);
    if (index >= 0) state.checkins[index] = saved;
    else state.checkins.push(saved);
  }

  async function incrementActiveTime(memberId, seconds) {
    const member = state.members.find(item => item.id === memberId);
    if (!member) return;
    member.activeTime = (member.activeTime || 0) + seconds;
    await paperpilotApi.incrementActiveTime({ email: member.email, seconds });
  }

  async function addResource(name, size, type, data, uploader) {
    const saved = await paperpilotApi.uploadTeamResource({ name, size, type, data, uploader });
    state.resources.unshift(saved);
  }

  async function deleteResource(id) {
    await paperpilotApi.deleteTeamResource(id);
    state.resources = state.resources.filter(resource => resource.id !== id);
  }

  function persist() {
    // Team data is persisted by each API mutation in local MySQL.
  }

  loadFromServer().catch(error => console.error("Failed to load team data from MySQL:", error));

  return {
    state,
    members,
    totalSeats,
    usedSeats,
    groupTokenPool,
    tasks,
    announcements,
    checkins,
    resources,
    teamInfo,
    teamIdentifier,
    loadFromServer,
    addMember,
    removeMember,
    updateRole,
    updateQuota,
    addTask,
    updateTask,
    deleteTask,
    addAnnouncement,
    updateAnnouncement,
    deleteAnnouncement,
    performCheckin,
    incrementActiveTime,
    addResource,
    deleteResource,
    persist,
  };
});
