import { apiClient } from "./apiClient";

export const paperpilotApi = {
  async login(payload) {
    const { data } = await apiClient.post("/api/auth/login", payload, { timeout: 5000 });
    return data;
  },
  async register(payload) {
    const { data } = await apiClient.post("/api/auth/register", payload, { timeout: 5000 });
    return data;
  },
  async sendRegisterCode(email) {
    const { data } = await apiClient.post(`/api/auth/register/send-code?email=${encodeURIComponent(email)}`);
    return data;
  },
  async changePassword(payload) {
    const { data } = await apiClient.post("/api/auth/change-password", payload);
    return data;
  },
  async updateProfile(payload) {
    const { data } = await apiClient.patch("/api/auth/profile", payload);
    return data;
  },
  async sendForgotPasswordCode(email) {
    const { data } = await apiClient.post(`/api/auth/forgot-password/send-code?email=${encodeURIComponent(email)}`);
    return data;
  },
  async resetPasswordWithCode(payload) {
    const { data } = await apiClient.post("/api/auth/forgot-password/reset", payload);
    return data;
  },
  async getActiveModelConfig(scene = "general") {
    const { data } = await apiClient.get("/api/admin/model-config/active", { params: { scene } });
    return data;
  },
  async saveModelConfig(payload) {
    const { data } = await apiClient.post("/api/admin/model-config", payload);
    return data;
  },
  async testModelConfig(payload) {
    const { data } = await apiClient.post("/api/admin/model-config/test", payload, { timeout: 210000 });
    return data;
  },
  async fetchModelList(payload) {
    const { data } = await apiClient.post("/api/admin/model-config/models", payload);
    return data;
  },
  async chatWithModel(config, prompt) {
    const { data } = await apiClient.post("/api/admin/model-config/chat", { config, prompt }, { timeout: 210000 });
    return data;
  },
  async getModelPool(scene = "general") {
    const { data } = await apiClient.get("/api/admin/model-config/pool", { params: { scene } });
    return data;
  },
  async refreshModelPool(scene = "general") {
    const { data } = await apiClient.post("/api/admin/model-config/pool/refresh", null, { params: { scene }, timeout: 300000 });
    return data;
  },
  async seedModelPool(scene = "general") {
    const { data } = await apiClient.post("/api/admin/model-config/pool/seed", null, { params: { scene } });
    return data;
  },
  async cleanupModelPool(scene = "general") {
    const { data } = await apiClient.post("/api/admin/model-config/pool/cleanup", null, { params: { scene } });
    return data;
  },
  async activateModelPoolRoute(id, scene = "general") {
    const { data } = await apiClient.post(`/api/admin/model-config/pool/${id}/activate`, null, { params: { scene } });
    return data;
  },
  async assignModelPoolRoute(id, scene = "general", enabled = true) {
    const { data } = await apiClient.post(`/api/admin/model-config/pool/${id}/assign`, null, { params: { scene, enabled } });
    return data;
  },
  async assignRelayModelToScene(id, modelName, scene = "general", enabled = true) {
    const { data } = await apiClient.post(`/api/admin/model-config/pool/${id}/assign-model`, null, { params: { modelName, scene, enabled } });
    return data;
  },
  async fetchModelsForConfig(config) {
    const { data } = await apiClient.post("/api/admin/model-config/models", config, { timeout: 120000 });
    return data;
  },
  async fetchRelayRouteModels(id) {
    const { data } = await apiClient.get(`/api/admin/model-config/pool/${id}/models`, { timeout: 120000 });
    return data;
  },
  async deleteModelRoute(id) {
    await apiClient.delete(`/api/admin/model-config/pool/${id}`);
  },
  async deleteRelayRoute(id) {
    const { data } = await apiClient.delete(`/api/admin/model-config/pool/${id}/relay`);
    return data;
  },
  async testRelayRouteModel(id, modelName) {
    const { data } = await apiClient.post(`/api/admin/model-config/pool/${id}/test-model`, null, { params: { modelName }, timeout: 210000 });
    return data;
  },
  async getRelayResearchTop() {
    const { data } = await apiClient.get("/api/admin/model-config/relay-research/top", { timeout: 60000 });
    return data;
  },
  async getAdminAiUsageCalls(params = {}) {
    const { data } = await apiClient.get("/api/admin/ai-usage/calls", { params, timeout: 20000 });
    return data;
  },
  async clearAdminAiUsageCalls() {
    const { data } = await apiClient.delete("/api/admin/ai-usage/calls", { timeout: 20000 });
    return data;
  },
  async importPaper(payload) {
    const { data } = await apiClient.post("/api/papers/import", payload);
    return data;
  },
  async importZoteroFile(file) {
    const formData = new FormData();
    formData.append("file", file);
    const { data } = await apiClient.post("/api/papers/import-zotero", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
      timeout: 180000,
    });
    return data;
  },
  async importZoteroOnline(payload) {
    const { data } = await apiClient.post("/api/papers/import-zotero-online", payload, { timeout: 180000 });
    return data;
  },
  async importZoteroLocal(payload = {}) {
    const { data } = await apiClient.post("/api/papers/import-zotero-local", payload, { timeout: 180000 });
    return data;
  },
  async getDashboardSummary() {
    const { data } = await apiClient.get("/api/dashboard/summary");
    return data;
  },
  async getLibraryPapers(params = {}) {
    const { data } = await apiClient.get("/api/library/papers", { params });
    return data;
  },
  async updateLibraryPaper(workspaceId, payload) {
    const { data } = await apiClient.patch(`/api/library/papers/${workspaceId}`, payload);
    return data;
  },
  async deleteLibraryPaper(workspaceId) {
    const { data } = await apiClient.delete(`/api/library/papers/${workspaceId}`);
    return data;
  },
  async getMeetingReport(workspaceId) {
    const { data } = await apiClient.get(`/api/meeting-reports/${workspaceId}`);
    return data;
  },
  async generateMeetingReport(workspaceId) {
    const { data } = await apiClient.post(`/api/meeting-reports/${workspaceId}/generate`, null, { timeout: 30000 });
    return data;
  },
  async getMeetingReportGenerateStatus(workspaceId) {
    const { data } = await apiClient.get(`/api/meeting-reports/${workspaceId}/generate/status`);
    return data;
  },
  async askPaperSelection(workspaceId, payload) {
    const { data } = await apiClient.post(`/api/meeting-reports/${workspaceId}/ask`, payload, { timeout: 120000 });
    return data;
  },
  async saveMeetingReport(workspaceId, payload) {
    const { data } = await apiClient.put(`/api/meeting-reports/${workspaceId}`, payload);
    return data;
  },
  async generateMeetingDeck(payload) {
    if (payload instanceof FormData) {
      const { data } = await apiClient.post("/api/meeting-reports/deck/generate", payload, {
        timeout: 45000,
        headers: { "Content-Type": "multipart/form-data" },
      });
      return data;
    }
    const { data } = await apiClient.post("/api/meeting-reports/deck/generate", payload, { timeout: 45000 });
    return data;
  },
  async getMeetingDeckStatus(jobId) {
    const { data } = await apiClient.get(`/api/meeting-reports/deck/jobs/${jobId}/status`, { timeout: 20000 });
    return data;
  },
  async analyzeMeetingDeck(payload) {
    const { data } = await apiClient.post("/api/meeting-reports/deck/analyze", payload, { timeout: 180000 });
    return data;
  },
  async fuseMeetingReport(payload) {
    const { data } = await apiClient.post("/api/meeting-reports/fuse", payload, { timeout: 180000 });
    return data;
  },
  async createSearchSession(payload) {
    const { data } = await apiClient.post("/api/search/session", payload);
    return data;
  },
  async searchPapers(params = {}) {
    const { data } = await apiClient.get("/api/search/papers", { params });
    return data;
  },
  // New method for external academic search (Crossref/Unpaywall)
  async externalSearch(query, page = 1, pageSize = 20, source = "crossref") {
    const { data } = await apiClient.get("/api/external/search", { params: { q: query, page, pageSize, source } });
    return data;
  },
  async importByUrl(payload) {
    const { data } = await apiClient.post("/api/papers/import-by-url", payload);
    return data;
  },
  async uploadPaperPdf(workspaceId, file) {
    const formData = new FormData();
    formData.append("file", file);
    const { data } = await apiClient.post(`/api/papers/${workspaceId}/upload`, formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return data;
  },
  async uploadLibraryPaper(file) {
    const formData = new FormData();
    formData.append("file", file);
    const { data } = await apiClient.post("/api/papers/upload", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
      timeout: 120000,
    });
    return data;
  },
  normalizePdfUrl(url) {
    if (!url) return "";
    if (url.startsWith("blob:") || url.startsWith("data:")) return url;
    let nextUrl = url.trim();
    if (nextUrl.includes("/api/papers/proxy?url=")) {
      const query = nextUrl.split("?url=")[1];
      if (query) {
        nextUrl = decodeURIComponent(query);
      }
    }
    nextUrl = nextUrl
      .replace("http://arxiv.org/", "https://arxiv.org/")
      .replace("http://export.arxiv.org/", "https://export.arxiv.org/");
    if (nextUrl.includes("arxiv.org/abs/")) {
      nextUrl = nextUrl.replace("/abs/", "/pdf/");
      if (!nextUrl.endsWith(".pdf")) {
        nextUrl = `${nextUrl}.pdf`;
      }
    }
    if (nextUrl.includes("aclanthology.org/") && !nextUrl.toLowerCase().endsWith(".pdf")) {
      nextUrl = `${nextUrl.replace(/\/$/, "")}.pdf`;
    }
    return nextUrl;
  },
  toApiUrl(path) {
    if (!path) return "";
    if (/^https?:\/\//i.test(path)) return path;
    const apiBase = apiClient.defaults.baseURL || "";
    if (!apiBase) return path;
    return `${apiBase.replace(/\/$/, "")}/${String(path).replace(/^\//, "")}`;
  },
  isLikelyPdfUrl(url) {
    const normalized = this.normalizePdfUrl(url || "").toLowerCase();
    if (!normalized) return false;
    if (normalized.startsWith("blob:") || normalized.startsWith("data:")) return true;
    if (normalized.includes("/api/papers/uploads/")) return true;
    return normalized.includes(".pdf")
      || normalized.includes("/pdf/")
      || normalized.includes("arxiv.org/pdf/");
  },
  async translate(payload, options = {}) {
    if (canUseDesktopTranslation(payload?.provider) && window.paperSolverDesktop?.translate) {
      return window.paperSolverDesktop.translate(payload);
    }
    const { data } = await apiClient.post("/api/translate", payload, { timeout: options.timeout || 45000 });
    return data;
  },
  async getTranslationProviders() {
    const providers = [];
    if (window.paperSolverDesktop?.getTranslationProviders) {
      try {
        providers.push(...await window.paperSolverDesktop.getTranslationProviders());
      } catch {
        // 桌面本机翻译能力不可用时继续读取后端列表。
      }
    }
    try {
      const { data } = await apiClient.get("/api/translate/providers");
      providers.push(...(Array.isArray(data) ? data : []));
    } catch (error) {
      if (!providers.length) throw error;
    }
    return mergeTranslationProviders(providers);
  },
  async startPdfMathTranslation(workspaceId, service = "google") {
    const { data } = await apiClient.post(`/api/pdfmathtranslate/${workspaceId}/translate`, { service }, { timeout: 60000 });
    return data;
  },
  async getPdfMathTranslationStatus(workspaceId) {
    const { data } = await apiClient.get(`/api/pdfmathtranslate/${workspaceId}/status`, { timeout: 20000 });
    return data;
  },
  async getPdfMathDualPdf(workspaceId) {
    const { data } = await apiClient.get(`/api/pdfmathtranslate/${workspaceId}/dual.pdf`, {
      responseType: "blob",
      timeout: 120000,
    });
    return data;
  },
  async startMineruParse(workspaceId, force = false) {
    const { data } = await apiClient.post(`/api/mineru/${workspaceId}/parse`, null, {
      params: { force },
      timeout: 30000,
    });
    return data;
  },
  async getMineruParseStatus(workspaceId) {
    const { data } = await apiClient.get(`/api/mineru/${workspaceId}/status`, { timeout: 20000 });
    return data;
  },
  async getMineruDocument(workspaceId) {
    const { data } = await apiClient.get(`/api/mineru/${workspaceId}/document`, { timeout: 60000 });
    return data;
  },
  async getMineruAsset(path) {
    const { data } = await apiClient.get(path, {
      responseType: "blob",
      timeout: 60000,
    });
    return data;
  },
  async getUsageSummary() {
    const { data } = await apiClient.get("/api/usage/summary");
    return data;
  },
  buildPdfProxyUrl(url) {
    if (!url) return "";
    const normalized = this.normalizePdfUrl(url);
    const apiBase = apiClient.defaults.baseURL || "";
    if (
      normalized.startsWith("blob:") ||
      normalized.startsWith("data:") ||
      normalized.startsWith("/api/") ||
      (apiBase && normalized.startsWith(apiBase)) ||
      normalized.includes("/api/papers/proxy?url=")
    ) {
      if (normalized.startsWith("/api/")) {
        return this.toApiUrl(normalized);
      }
      return normalized;
    }
    const base = apiBase.replace(/\/$/, "");
    return `${base}/api/papers/proxy?url=${encodeURIComponent(normalized)}`;
  },

  // Admin endpoints
  async getAdminUsers() {
    const { data } = await apiClient.get("/api/admin/users");
    return data;
  },
  async addAdminUser(payload) {
    const { data } = await apiClient.post("/api/admin/users", payload);
    return data;
  },
  async updateUserQuota(userId, tokenLimit) {
    const payload = typeof tokenLimit === "object" ? tokenLimit : { tokenLimit };
    const { data } = await apiClient.patch(`/api/admin/users/${userId}/quota`, payload);
    return data;
  },
  async updateAdminUserMembership(userId, payload) {
    const { data } = await apiClient.patch(`/api/admin/users/${userId}/membership`, payload);
    return data;
  },
  async updateUserRole(userId, role) {
    const { data } = await apiClient.patch(`/api/admin/users/${userId}/role`, { role });
    return data;
  },
  async updateUserPassword(userId, password) {
    const { data } = await apiClient.patch(`/api/admin/users/${userId}/password`, { password });
    return data;
  },
  async deleteUser(userId) {
    const { data } = await apiClient.delete(`/api/admin/users/${userId}`);
    return data;
  },
  async getRechargeRecords() {
    const { data } = await apiClient.get("/api/admin/recharges");
    return data;
  },
  async getBillingSettings() {
    const { data } = await apiClient.get("/api/admin/billing");
    return data;
  },
  async updateBillingSettings(payload) {
    const { data } = await apiClient.patch("/api/admin/billing", payload);
    return data;
  },
  async getAdminPayments() {
    const { data } = await apiClient.get("/api/admin/payments");
    return data;
  },
  async updatePaymentTicket(id, payload) {
    const { data } = await apiClient.patch(`/api/admin/payments/tickets/${id}`, payload);
    return data;
  },
  async addRechargeRecord(payload) {
    const { data } = await apiClient.post("/api/admin/recharges", payload);
    return data;
  },
  async getTeams() {
    const { data } = await apiClient.get("/api/admin/teams");
    return data;
  },
  async createTeam(payload) {
    const { data } = await apiClient.post("/api/admin/teams", payload);
    return data;
  },
  async getTeamMembersById(teamId) {
    const { data } = await apiClient.get(`/api/admin/teams/${teamId}/members`);
    return data;
  },
  async deleteTeam(teamId) {
    const { data } = await apiClient.delete(`/api/admin/teams/${teamId}`);
    return data;
  },
  async getSystemLogs() {
    const { data } = await apiClient.get("/api/admin/logs");
    return data;
  },
  async clearSystemLogs() {
    const { data } = await apiClient.delete("/api/admin/logs");
    return data;
  },
  async getAdminStats() {
    const { data } = await apiClient.get("/api/admin/stats");
    return data;
  },
  async getCampusVerificationMe() {
    const { data } = await apiClient.get("/api/campus-verification/me");
    return data;
  },
  async submitCampusVerification(payload) {
    const { data } = await apiClient.post("/api/campus-verification/submit", payload, { timeout: 30000 });
    return data;
  },
  async getAdminCampusVerifications() {
    const { data } = await apiClient.get("/api/admin/campus-verifications", { timeout: 15000 });
    return data;
  },
  async reviewCampusVerification(id, payload) {
    const { data } = await apiClient.patch(`/api/admin/campus-verifications/${id}`, payload, { timeout: 15000 });
    return data;
  },
  async getActiveSiteMessages() {
    const { data } = await apiClient.get("/api/site-messages/active");
    return data;
  },
  async getAdminSiteMessages() {
    const { data } = await apiClient.get("/api/admin/site-messages");
    return data;
  },
  async publishSiteMessage(payload) {
    const { data } = await apiClient.post("/api/admin/site-messages", payload);
    return data;
  },
  async updateSiteMessageStatus(id, active) {
    const { data } = await apiClient.patch(`/api/admin/site-messages/${id}/status`, { active });
    return data;
  },
  async deleteSiteMessage(id) {
    await apiClient.delete(`/api/admin/site-messages/${id}`);
  },

  // Admin Promotions
  async getPromotions() {
    const { data } = await apiClient.get("/api/admin/promotions");
    return data;
  },
  async auditPromotion(id, action, adminNote = "") {
    const { data } = await apiClient.post(`/api/admin/promotions/${id}/audit`, { action, adminNote });
    return data;
  },
  async getTutorials() {
    const { data } = await apiClient.get("/api/tutorials");
    return data;
  },
  async getAdminTutorials() {
    const { data } = await apiClient.get("/api/admin/tutorials");
    return data;
  },
  async publishTutorial(payload) {
    const { data } = await apiClient.post("/api/admin/tutorials", payload);
    return data;
  },
  async updateTutorial(id, payload) {
    const { data } = await apiClient.patch(`/api/admin/tutorials/${id}`, payload);
    return data;
  },
  async updateTutorialStatus(id, active) {
    const { data } = await apiClient.patch(`/api/admin/tutorials/${id}/status`, { active });
    return data;
  },
  async deleteTutorial(id) {
    await apiClient.delete(`/api/admin/tutorials/${id}`);
  },

  // Forum API
  async getForumPosts() {
    const { data } = await apiClient.get("/api/forum/posts", { timeout: 15000 });
    return data;
  },
  async getForumActiveUsers() {
    const { data } = await apiClient.get("/api/forum/active-users", { timeout: 15000 });
    return data;
  },
  async createForumPost(payload) {
    const { data } = await apiClient.post("/api/forum/posts", payload, { timeout: 90000 });
    return data;
  },
  async updateForumPost(id, payload) {
    const { data } = await apiClient.patch(`/api/forum/posts/${id}`, payload, { timeout: 15000 });
    return data;
  },
  async deleteForumPost(id) {
    await apiClient.delete(`/api/forum/posts/${id}`);
  },
  async toggleForumPostPin(id) {
    const { data } = await apiClient.post(`/api/forum/posts/${id}/pin`);
    return data;
  },
  async toggleForumPostBan(id) {
    const { data } = await apiClient.post(`/api/forum/posts/${id}/ban`);
    return data;
  },
  async likeForumPost(id) {
    const { data } = await apiClient.post(`/api/forum/posts/${id}/like`);
    return data;
  },
  async reportForumPost(id, payload) {
    const { data } = await apiClient.post(`/api/forum/posts/${id}/report`, payload, { timeout: 15000 });
    return data;
  },
  async viewForumPost(id) {
    const { data } = await apiClient.post(`/api/forum/posts/${id}/view`);
    return data;
  },
  async bookmarkForumPost(id) {
    const { data } = await apiClient.post(`/api/forum/posts/${id}/bookmark`);
    return data;
  },
  async replyForumPost(id, payload) {
    const { data } = await apiClient.post(`/api/forum/posts/${id}/reply`, payload);
    return data;
  },
  async likeForumReply(postId, replyId) {
    const { data } = await apiClient.post(`/api/forum/posts/${postId}/reply/${replyId}/like`);
    return data;
  },
  async getMessageContacts() {
    const { data } = await apiClient.get("/api/messages/contacts");
    return data;
  },
  async getMessageThread(userId) {
    const { data } = await apiClient.get(`/api/messages/thread/${userId}`);
    return data;
  },
  async sendDirectMessage(userId, payload) {
    const { data } = await apiClient.post(`/api/messages/thread/${userId}`, payload);
    return data;
  },
  async getUserCard(userId) {
    const { data } = await apiClient.get(`/api/friends/profile/${userId}`);
    return data;
  },
  async getForumReports() {
    const { data } = await apiClient.get("/api/admin/forum/reports", { timeout: 15000 });
    return data;
  },
  async updateForumReport(id, payload) {
    const { data } = await apiClient.patch(`/api/admin/forum/reports/${id}`, payload, { timeout: 15000 });
    return data;
  },
  async getUserCardByEmail(email) {
    const { data } = await apiClient.get("/api/friends/profile", { params: { email } });
    return data;
  },
  async getFriendRequests() {
    const { data } = await apiClient.get("/api/friends/requests");
    return data;
  },
  async sendFriendRequest(userId, payload = {}) {
    const { data } = await apiClient.post(`/api/friends/requests/${userId}`, payload);
    return data;
  },
  async handleFriendRequest(requestId, action) {
    await apiClient.patch(`/api/friends/requests/${requestId}`, { action });
  },

  // Topic square API
  async getTopics(params = {}) {
    const { data } = await apiClient.get("/api/topics", { params, timeout: 20000 });
    return data;
  },
  async generateTopic(payload) {
    const { data } = await apiClient.post("/api/topics/generate", payload, { timeout: 300000 });
    return data;
  },
  async saveTopic(id) {
    const { data } = await apiClient.post(`/api/topics/${id}/save`, null, { timeout: 15000 });
    return data;
  },
  async markTopicInterested(id) {
    const { data } = await apiClient.post(`/api/topics/${id}/interested`, null, { timeout: 15000 });
    return data;
  },
  async exportTopicOutline(id, target = "review") {
    const { data } = await apiClient.post(`/api/topics/${id}/outline`, { target }, { timeout: 30000 });
    return data;
  },
  async getAdminTopics(params = {}) {
    const { data } = await apiClient.get("/api/admin/topics", { params, timeout: 20000 });
    return data;
  },
  async generateAdminHotTopics(payload = {}) {
    const { data } = await apiClient.post("/api/admin/topics/generate-hot", payload, { timeout: 300000 });
    return data;
  },
  async deleteAdminTopic(id) {
    const { data } = await apiClient.delete(`/api/admin/topics/${id}`, { timeout: 15000 });
    return data;
  },

  // Team API
  async getTeamInfo() {
    const { data } = await apiClient.get("/api/team/info");
    return data;
  },
  async getTeamMembers() {
    const { data } = await apiClient.get("/api/team/members");
    return data;
  },
  async incrementActiveTime(payload) {
    const { data } = await apiClient.post("/api/team/members/active-time", payload);
    return data;
  },
  async addMember(payload) {
    const { data } = await apiClient.post("/api/team/members", payload);
    return data;
  },
  async deleteMember(id) {
    const { data } = await apiClient.delete(`/api/team/members/${id}`);
    return data;
  },
  async updateMemberQuota(id, payload) {
    const { data } = await apiClient.patch(`/api/team/members/${id}/quota`, payload);
    return data;
  },
  async updateMemberRole(id, payload) {
    const { data } = await apiClient.patch(`/api/team/members/${id}/role`, payload);
    return data;
  },
  async getTeamTasks() {
    const { data } = await apiClient.get("/api/team/tasks");
    return data;
  },
  async createTeamTask(payload) {
    const { data } = await apiClient.post("/api/team/tasks", payload);
    return data;
  },
  async updateTeamTask(id, payload) {
    const { data } = await apiClient.patch(`/api/team/tasks/${id}`, payload);
    return data;
  },
  async deleteTeamTask(id) {
    await apiClient.delete(`/api/team/tasks/${id}`);
  },
  async getTeamAnnouncements() {
    const { data } = await apiClient.get("/api/team/announcements");
    return data;
  },
  async createTeamAnnouncement(payload) {
    const { data } = await apiClient.post("/api/team/announcements", payload);
    return data;
  },
  async updateTeamAnnouncement(id, payload) {
    const { data } = await apiClient.patch(`/api/team/announcements/${id}`, payload);
    return data;
  },
  async deleteTeamAnnouncement(id) {
    await apiClient.delete(`/api/team/announcements/${id}`);
  },
  async downloadTeamAnnouncementAttachment(id) {
    return apiClient.get(`/api/team/announcements/${id}/attachment`, {
      responseType: "blob",
    });
  },
  async getNotifications() {
    const { data } = await apiClient.get("/api/notifications");
    return data;
  },
  async markNotificationRead(id) {
    const { data } = await apiClient.patch(`/api/notifications/${id}/read`);
    return data;
  },
  async getTeamResources() {
    const { data } = await apiClient.get("/api/team/resources");
    return data;
  },
  async uploadTeamResource(payload) {
    const { data } = await apiClient.post("/api/team/resources", payload);
    return data;
  },
  async deleteTeamResource(id) {
    const { data } = await apiClient.delete(`/api/team/resources/${id}`);
    return data;
  },
  async getTeamCheckins(date) {
    const { data } = await apiClient.get("/api/team/checkins", { params: { date } });
    return data;
  },
  async performTeamCheckin(payload) {
    const { data } = await apiClient.post("/api/team/checkins", payload);
    return data;
  },
  async drawTeamCheckinFruit(payload) {
    const { data } = await apiClient.post("/api/team/checkins/draw", payload);
    return data;
  },
  async getTeamCheckinHistory(memberId, year) {
    const { data } = await apiClient.get("/api/team/checkins/history", { params: { memberId, year } });
    return data;
  },
  async createPaymentOrder(payload) {
    const { data } = await apiClient.post("/api/payments/orders", payload);
    return data;
  },
  async getPaymentOrders() {
    const { data } = await apiClient.get("/api/payments/orders");
    return data;
  },
  async createPaymentTicket(payload) {
    const { data } = await apiClient.post("/api/payments/tickets", payload);
    return data;
  },
};

function canUseDesktopTranslation(provider) {
  if (!window.paperSolverDesktop?.isDesktop) return false;
  const normalized = String(provider || "google").trim().toLowerCase();
  return ["google", "google-web", "deeplx", "libretranslate", "mtranserver"].includes(normalized);
}

function mergeTranslationProviders(providers) {
  const map = new Map();
  for (const provider of providers) {
    if (!provider?.id) continue;
    const id = String(provider.id);
    const existing = map.get(id) || {};
    if (existing.local && !provider.local) {
      map.set(id, {
        ...provider,
        ...existing,
        id,
        configured: String(existing.configured ?? provider.configured ?? false),
        local: true,
      });
      continue;
    }
    map.set(id, {
      ...existing,
      ...provider,
      id,
      configured: String(provider.configured ?? existing.configured ?? false),
      local: Boolean(provider.local ?? existing.local),
    });
  }
  return Array.from(map.values());
}
