<template>
  <div class="messages-page">
    <header class="messages-heading">
      <div>
        <span>PRIVATE MESSAGES</span>
        <h1>站内私信</h1>
        <p>与站内研究者沟通，支持图片预览、科研附件与联系人管理。</p>
      </div>
      <div class="heading-stat">
        <span class="online-dot"></span>
        <b>{{ messagesStore.state.contacts.length }} 位联系人</b>
        <strong>{{ messagesStore.state.unreadCount }} 条未读</strong>
      </div>
    </header>

    <section class="message-shell">
      <aside class="contacts-panel">
        <div class="message-tabs">
          <button :class="{ active: activePanel === 'chats' }" @click="activePanel = 'chats'">会话</button>
          <button :class="{ active: activePanel === 'requests' }" @click="activePanel = 'requests'">
            好友申请
            <b v-if="messagesStore.state.friendRequests.pendingCount">{{ messagesStore.state.friendRequests.pendingCount }}</b>
          </button>
        </div>
        <label class="contact-search">
          <span>⌕</span>
          <input v-model="keyword" placeholder="搜索联系人" />
        </label>
        <div v-if="activePanel === 'chats'" class="contact-list">
          <button
            v-for="contact in filteredContacts"
            :key="contact.userId"
            :class="{ active: contact.userId === messagesStore.state.activeContactId }"
            @click="selectContact(contact.userId)"
          >
            <span class="contact-avatar" :data-user-id="contact.userId" title="查看个人卡片">{{ contact.avatar }}</span>
            <span class="contact-copy">
              <strong>{{ contact.name }}</strong>
              <small>{{ contact.lastMessage || contact.role }}</small>
            </span>
            <span class="contact-meta">
              <time>{{ contact.lastTime }}</time>
              <b v-if="contact.unreadCount">{{ contact.unreadCount }}</b>
            </span>
          </button>
        </div>
        <div v-else class="request-list">
          <section v-if="messagesStore.state.friendRequests.incoming.length">
            <h3>收到的申请</h3>
            <article v-for="request in messagesStore.state.friendRequests.incoming" :key="request.requestId">
              <span class="request-avatar" :data-user-id="request.userId">{{ request.avatar }}</span>
              <div>
                <strong>{{ request.name }}</strong>
                <small>{{ request.role }} · {{ request.message || "希望添加你为好友" }}</small>
                <time>{{ request.time }}</time>
              </div>
              <footer>
                <button @click="handleRequest(request.requestId, 'reject')">拒绝</button>
                <button class="accept" @click="handleRequest(request.requestId, 'accept')">同意</button>
              </footer>
            </article>
          </section>
          <section v-if="messagesStore.state.friendRequests.outgoing.length">
            <h3>已发送</h3>
            <article v-for="request in messagesStore.state.friendRequests.outgoing" :key="request.requestId">
              <span class="request-avatar" :data-user-id="request.userId">{{ request.avatar }}</span>
              <div>
                <strong>{{ request.name }}</strong>
                <small>等待对方处理</small>
                <time>{{ request.time }}</time>
              </div>
              <span class="pending-pill">待通过</span>
            </article>
          </section>
          <div v-if="!hasFriendRequests" class="requests-empty">暂无好友申请</div>
        </div>
      </aside>

      <main class="conversation-panel">
        <template v-if="activeContact">
          <header class="conversation-head">
            <span class="contact-avatar" :data-user-id="activeContact.userId" title="查看个人卡片">{{ activeContact.avatar }}</span>
            <div>
              <strong>{{ activeContact.name }}</strong>
              <small>{{ activeContact.role }} · {{ activeContact.email }}</small>
            </div>
            <span class="conversation-status"><i></i> 站内联系人</span>
          </header>

          <div ref="threadElement" class="message-thread">
            <div class="thread-date"><span>科研私信记录</span></div>
            <div v-if="messagesStore.state.loading" class="thread-empty">正在加载会话...</div>
            <article
              v-for="message in messagesStore.state.messages"
              v-else
              :key="message.id"
              :class="{ mine: message.mine }"
            >
              <span v-if="!message.mine" class="message-avatar" :data-user-id="activeContact.userId">{{ activeContact.avatar }}</span>
              <div class="message-bubble">
                <p v-if="message.content">{{ message.content }}</p>
                <button
                  v-for="image in imageAttachments(message)"
                  :key="image.name"
                  class="message-image"
                  @click="previewImage = image"
                >
                  <img :src="attachmentUrl(image)" :alt="image.name" />
                  <span>{{ image.name }}</span>
                </button>
                <a
                  v-for="file in fileAttachments(message)"
                  :key="file.name"
                  :href="attachmentUrl(file)"
                  :download="file.name"
                  class="message-file"
                >
                  <span>{{ fileExtension(file.name) }}</span>
                  <strong>{{ file.name }}</strong>
                  <small>{{ formatFileSize(file.size) }}</small>
                </a>
              </div>
              <time>{{ message.time }}</time>
            </article>
            <div v-if="!messagesStore.state.loading && !messagesStore.state.messages.length" class="thread-empty">
              还没有私信，发送第一条科研问候吧。
            </div>
          </div>

          <footer class="message-composer">
            <div v-if="attachments.length" class="composer-preview">
              <article v-for="(file, index) in attachments" :key="`${file.name}-${index}`" :class="{ image: isImage(file) }">
                <img v-if="isImage(file)" :src="attachmentUrl(file)" :alt="file.name" />
                <span v-else class="preview-file-icon">{{ fileExtension(file.name) }}</span>
                <div>
                  <strong>{{ file.name }}</strong>
                  <small>{{ formatFileSize(file.size) }}</small>
                </div>
                <button @click="attachments.splice(index, 1)">×</button>
              </article>
            </div>
            <div class="composer-input">
              <textarea v-model="content" rows="3" placeholder="输入消息，Enter 发送，Shift + Enter 换行" @keydown.enter.exact.prevent="send"></textarea>
            </div>
            <div class="composer-toolbar">
              <div>
                <label class="attach-button image-upload">
                  <input type="file" accept="image/*" multiple @change="onFiles" />
                  <span>▧</span> 图片
                </label>
                <label class="attach-button">
                  <input type="file" multiple @change="onFiles" />
                  <span>⌕</span> 附件
                </label>
                <small>最多 6 个文件，单个不超过 8MB</small>
              </div>
              <button class="send-button" :disabled="sending || (!content.trim() && !attachments.length)" @click="send">
                {{ sending ? "发送中..." : "发送" }} <span>↑</span>
              </button>
            </div>
          </footer>
        </template>
        <div v-else class="conversation-empty">
          <span>✉</span>
          <h2>选择一位联系人</h2>
          <p>你的站内私信、附件和未读消息会显示在这里。</p>
        </div>
      </main>
    </section>

    <div v-if="previewImage" class="image-lightbox" @click="previewImage = null">
      <div @click.stop>
        <header>
          <strong>{{ previewImage.name }}</strong>
          <a :href="attachmentUrl(previewImage)" :download="previewImage.name">下载原图</a>
          <button @click="previewImage = null">×</button>
        </header>
        <img :src="attachmentUrl(previewImage)" :alt="previewImage.name" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from "vue";
import { useRoute } from "vue-router";
import { useMessagesStore } from "../stores/messages";

const route = useRoute();
const messagesStore = useMessagesStore();
const keyword = ref("");
const content = ref("");
const attachments = ref([]);
const sending = ref(false);
const threadElement = ref(null);
const previewImage = ref(null);
const activePanel = ref("chats");

const filteredContacts = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  if (!term) return messagesStore.state.contacts;
  return messagesStore.state.contacts.filter(item =>
    `${item.name} ${item.email} ${item.role}`.toLowerCase().includes(term),
  );
});

const activeContact = computed(() =>
  messagesStore.state.contacts.find(item => item.userId === messagesStore.state.activeContactId),
);
const hasFriendRequests = computed(() =>
  messagesStore.state.friendRequests.incoming.length || messagesStore.state.friendRequests.outgoing.length,
);

onMounted(async () => {
  const [contacts] = await Promise.all([messagesStore.fetchContacts(), messagesStore.fetchFriendRequests()]);
  const requested = Number(route.query.contact);
  const firstId = contacts.some(item => item.userId === requested) ? requested : contacts[0]?.userId;
  if (firstId) await selectContact(firstId);
});

function refreshFriendRequests() {
  messagesStore.fetchFriendRequests().catch(() => {});
}

onMounted(() => window.addEventListener("paperpilot:friend-requests-changed", refreshFriendRequests));
onUnmounted(() => window.removeEventListener("paperpilot:friend-requests-changed", refreshFriendRequests));

async function selectContact(userId) {
  await messagesStore.openThread(userId);
  await nextTick();
  if (threadElement.value) threadElement.value.scrollTop = threadElement.value.scrollHeight;
}

async function handleRequest(requestId, action) {
  await messagesStore.handleFriendRequest(requestId, action);
}

function fileToData(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve({ name: file.name, size: file.size, type: file.type, dataUrl: reader.result });
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

async function onFiles(event) {
  const remaining = Math.max(0, 6 - attachments.value.length);
  const files = Array.from(event.target.files || [])
    .filter(file => file.size <= 8 * 1024 * 1024)
    .slice(0, remaining);
  attachments.value.push(...await Promise.all(files.map(fileToData)));
  event.target.value = "";
}

async function send() {
  if (sending.value) return;
  sending.value = true;
  try {
    await messagesStore.sendMessage({ content: content.value.trim(), attachments: attachments.value });
    content.value = "";
    attachments.value = [];
    await nextTick();
    if (threadElement.value) threadElement.value.scrollTop = threadElement.value.scrollHeight;
  } finally {
    sending.value = false;
  }
}

function formatFileSize(size) {
  if (!size) return "";
  return size < 1024 * 1024 ? `${Math.ceil(size / 1024)}KB` : `${(size / 1024 / 1024).toFixed(1)}MB`;
}

function attachmentUrl(file) {
  return file?.dataUrl || file?.data || "";
}

function isImage(file) {
  return String(file?.type || "").startsWith("image/") || /\.(png|jpe?g|gif|webp|bmp|svg)$/i.test(file?.name || "");
}

function imageAttachments(message) {
  return (message.attachments || []).filter(isImage);
}

function fileAttachments(message) {
  return (message.attachments || []).filter(file => !isImage(file));
}

function fileExtension(name) {
  const extension = String(name || "").split(".").pop();
  return extension && extension.length <= 5 ? extension.toUpperCase() : "FILE";
}
</script>

<style scoped>
.messages-page { width: min(1480px, calc(100vw - 48px)); margin: 0 auto; padding: 28px 0 70px; color: #172033; }
.messages-heading { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 18px; }
.messages-heading span { color: #0865ee; font-size: 10px; font-weight: 900; letter-spacing: .14em; }
.messages-heading h1 { margin: 5px 0; font-size: 30px; letter-spacing: -.03em; }
.messages-heading p { margin: 0; color: #7d899b; font-size: 12px; }
.heading-stat { display: flex; align-items: center; gap: 9px; padding: 7px; border: 1px solid #e2e8f1; border-radius: 12px; background: #fff; }
.heading-stat .online-dot { width: 8px; height: 8px; border-radius: 50%; background: #28c76f; box-shadow: 0 0 0 4px rgba(40,199,111,.1); }
.heading-stat b { color: #667287; font-size: 10px; }
.heading-stat strong { padding: 7px 10px; border-radius: 8px; color: #0865ee; background: #eaf2ff; font-size: 10px; }
.message-shell { height: min(740px, calc(100vh - 245px)); min-height: 600px; display: grid; grid-template-columns: 350px minmax(0, 1fr); overflow: hidden; border: 1px solid #dfe6f0; border-radius: 22px; background: #fff; box-shadow: 0 18px 55px rgba(32, 54, 91, .08); }
.contacts-panel { min-height: 0; padding: 16px; border-right: 1px solid #e7ebf1; background: linear-gradient(180deg, #fbfcfe, #f6f8fc); }
.message-tabs { display: grid; grid-template-columns: 1fr 1fr; gap: 5px; margin-bottom: 10px; padding: 4px; border-radius: 11px; background: #edf1f6; }
.message-tabs button { position: relative; padding: 8px; border: 0; border-radius: 8px; color: #68758a; background: transparent; font-size: 10px; font-weight: 800; }
.message-tabs button.active { color: #0865ee; background: #fff; box-shadow: 0 3px 9px rgba(32,54,91,.07); }
.message-tabs b { min-width: 17px; height: 17px; margin-left: 5px; display: inline-grid; place-items: center; border-radius: 50%; color: #fff; background: #f04444; font-size: 8px; }
.contact-search { height: 46px; display: flex; align-items: center; gap: 8px; padding: 0 13px; border: 1px solid #dce4ef; border-radius: 12px; background: #fff; box-shadow: 0 4px 12px rgba(35,52,81,.035); }
.contact-search input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; }
.contact-list { height: calc(100% - 110px); margin-top: 12px; padding-right: 2px; display: flex; flex-direction: column; gap: 6px; overflow-y: auto; }
.contact-list button { width: 100%; display: grid; grid-template-columns: 44px minmax(0, 1fr) auto; gap: 11px; align-items: center; padding: 12px; border: 1px solid transparent; border-radius: 13px; color: #344158; background: transparent; text-align: left; transition: .18s ease; }
.contact-list button:hover { background: #f0f5fc; }
.contact-list button.active { border-color: #bfd5fa; background: #eaf3ff; box-shadow: inset 3px 0 #0865ee; }
.contact-avatar { width: 44px; height: 44px; display: grid; place-items: center; border-radius: 14px; color: #fff; background: linear-gradient(135deg, #176ce4, #663fd3); font-weight: 800; box-shadow: 0 6px 14px rgba(52,82,190,.17); }
.contact-copy { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.contact-copy strong, .contact-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.contact-copy strong { font-size: 12px; }
.contact-copy small, .contact-meta time { color: #929cac; font-size: 9px; }
.contact-meta { display: flex; align-items: flex-end; flex-direction: column; gap: 6px; }
.contact-meta b { min-width: 19px; height: 19px; display: grid; place-items: center; border-radius: 50%; color: #fff; background: #f04444; font-size: 9px; }
.request-list { height: calc(100% - 110px); overflow-y: auto; padding-top: 4px; }
.request-list section + section { margin-top: 18px; }
.request-list h3 { margin: 8px 4px; color: #8994a5; font-size: 9px; letter-spacing: .08em; }
.request-list article { display: grid; grid-template-columns: 40px minmax(0, 1fr); gap: 9px; padding: 11px; border: 1px solid #e3e9f1; border-radius: 12px; background: #fff; }
.request-list article + article { margin-top: 7px; }
.request-avatar { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 12px; color: #fff; background: linear-gradient(135deg, #176ce4, #663fd3); font-weight: 800; cursor: pointer; }
.request-list article > div { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.request-list strong { color: #344158; font-size: 11px; }
.request-list small { overflow: hidden; color: #8792a3; text-overflow: ellipsis; white-space: nowrap; font-size: 9px; }
.request-list time { color: #a0a9b6; font-size: 8px; }
.request-list footer { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 6px; }
.request-list footer button { padding: 6px 10px; border: 1px solid #dfe5ee; border-radius: 7px; color: #697589; background: #fff; font-size: 9px; }
.request-list footer button.accept { border-color: #0865ee; color: #fff; background: #0865ee; }
.pending-pill { grid-column: 1 / -1; justify-self: end; padding: 4px 7px; border-radius: 6px; color: #9a6a00; background: #fff2cc; font-size: 8px; }
.requests-empty { padding: 70px 10px; color: #9aa4b3; text-align: center; font-size: 10px; }
.conversation-panel { min-width: 0; display: flex; flex-direction: column; background: #fff; }
.conversation-head { display: flex; align-items: center; gap: 12px; padding: 15px 20px; border-bottom: 1px solid #e8ecf2; background: rgba(255,255,255,.96); }
.conversation-head div { display: flex; flex-direction: column; gap: 3px; }
.conversation-head strong { font-size: 13px; }
.conversation-head small { color: #929cac; font-size: 10px; }
.conversation-status { margin-left: auto; display: flex; align-items: center; gap: 6px; padding: 6px 9px; border-radius: 8px; color: #68758a; background: #f2f5f9; font-size: 9px; }
.conversation-status i { width: 6px; height: 6px; border-radius: 50%; background: #28c76f; }
.message-thread { min-height: 0; flex: 1; overflow-y: auto; padding: 20px 26px 28px; display: flex; flex-direction: column; gap: 13px; background: radial-gradient(circle at 50% 0, #fff 0, #f8faff 42%, #f3f6fb 100%); }
.thread-date { display: flex; align-items: center; gap: 10px; margin: 2px 0 6px; color: #a0a9b7; font-size: 9px; }
.thread-date::before, .thread-date::after { content: ""; height: 1px; flex: 1; background: #e5eaf1; }
.message-thread article { max-width: 72%; align-self: flex-start; display: grid; grid-template-columns: 30px minmax(0, 1fr); column-gap: 8px; }
.message-thread article.mine { align-self: flex-end; }
.message-thread article.mine { grid-template-columns: minmax(0, 1fr); }
.message-avatar { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 9px; color: #fff; background: linear-gradient(135deg, #176ce4, #663fd3); font-size: 9px; font-weight: 800; }
.message-bubble { min-width: 80px; padding: 11px 13px; border: 1px solid #dfe6ef; border-radius: 5px 15px 15px; background: #fff; box-shadow: 0 5px 16px rgba(32,54,91,.05); }
.mine .message-bubble { border-color: #0865ee; border-radius: 14px 5px 14px 14px; color: #fff; background: #0865ee; }
.message-bubble p { margin: 0; white-space: pre-wrap; font-size: 12px; line-height: 1.65; }
.message-thread article > time { grid-column: 2; display: block; margin-top: 5px; color: #9ba4b2; font-size: 9px; }
.mine > time { text-align: right; }
.mine > time { grid-column: 1; }
.message-image { width: min(320px, 46vw); margin-top: 8px; padding: 0; overflow: hidden; display: block; border: 0; border-radius: 10px; color: inherit; background: rgba(255,255,255,.13); cursor: zoom-in; }
.message-image img { width: 100%; max-height: 260px; display: block; object-fit: cover; }
.message-image span { display: block; overflow: hidden; padding: 6px 8px; text-overflow: ellipsis; white-space: nowrap; font-size: 9px; opacity: .75; }
.message-file { margin-top: 7px; display: grid; grid-template-columns: 30px minmax(0, 1fr) auto; align-items: center; gap: 8px; padding: 8px; border-radius: 9px; color: inherit; background: rgba(120, 140, 170, .12); text-decoration: none; }
.message-file span { font-size: 8px; font-weight: 900; }
.message-file strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 10px; }
.message-file small { font-size: 9px; opacity: .7; }
.thread-empty, .conversation-empty { margin: auto; color: #929cac; text-align: center; font-size: 12px; }
.conversation-empty span { width: 58px; height: 58px; margin: 0 auto; display: grid; place-items: center; border-radius: 18px; color: #0865ee; background: #eaf2ff; font-size: 24px; }
.conversation-empty h2 { margin: 14px 0 5px; color: #344158; }
.conversation-empty p { margin: 0; }
.message-composer { padding: 12px 18px 14px; border-top: 1px solid #e3e9f1; background: #fff; }
.composer-input { padding: 2px 0; }
.message-composer textarea { width: 100%; padding: 10px 2px; border: 0; outline: 0; resize: none; box-sizing: border-box; color: #2c384d; background: transparent; line-height: 1.6; }
.composer-toolbar { margin-top: 4px; padding-top: 9px; display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #edf0f4; }
.composer-toolbar > div { display: flex; align-items: center; gap: 7px; }
.composer-toolbar small { margin-left: 5px; color: #a0a9b6; font-size: 9px; }
.attach-button, .send-button { padding: 8px 13px; border-radius: 9px; font-size: 11px; font-weight: 700; cursor: pointer; }
.attach-button { display: inline-flex; align-items: center; gap: 5px; color: #56647a; background: #f0f3f7; }
.attach-button.image-upload { color: #0865ee; background: #eaf3ff; }
.attach-button input { display: none; }
.send-button { min-width: 82px; display: flex; justify-content: center; align-items: center; gap: 8px; border: 0; color: #fff; background: #0865ee; box-shadow: 0 7px 16px rgba(8,101,238,.2); }
.send-button:disabled { opacity: .45; }
.composer-preview { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 7px; margin-bottom: 8px; }
.composer-preview article { min-width: 0; position: relative; display: grid; grid-template-columns: 34px minmax(0, 1fr) 22px; align-items: center; gap: 8px; padding: 7px; border: 1px solid #e0e7f0; border-radius: 10px; background: #f8faff; }
.composer-preview article.image { grid-template-columns: 46px minmax(0, 1fr) 22px; }
.composer-preview img { width: 46px; height: 40px; object-fit: cover; border-radius: 7px; }
.preview-file-icon { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 8px; color: #fff; background: #3b73d9; font-size: 8px; font-weight: 900; }
.composer-preview article > div { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.composer-preview strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #344158; font-size: 9px; }
.composer-preview small { color: #9aa4b3; font-size: 8px; }
.composer-preview button { width: 22px; height: 22px; border: 0; border-radius: 50%; color: #9a4050; background: #fff0f2; }
.image-lightbox { position: fixed; inset: 0; z-index: 10000; display: grid; place-items: center; padding: 30px; background: rgba(12,19,31,.84); }
.image-lightbox > div { width: min(1100px, 94vw); height: min(860px, 88vh); display: flex; flex-direction: column; }
.image-lightbox header { flex: 0 0 auto; display: flex; align-items: center; gap: 10px; padding: 10px 0; color: #fff; }
.image-lightbox header strong { min-width: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.image-lightbox header a { color: #bcd6ff; text-decoration: none; font-size: 11px; }
.image-lightbox header button { width: 34px; height: 34px; border: 0; border-radius: 50%; color: #fff; background: rgba(255,255,255,.16); font-size: 22px; }
.image-lightbox img { min-height: 0; max-width: 100%; max-height: calc(100% - 54px); margin: auto; object-fit: contain; border-radius: 12px; }
@media (max-width: 780px) {
  .messages-page { width: min(100% - 24px, 1420px); }
  .message-shell { grid-template-columns: 1fr; }
  .contacts-panel { border-right: 0; border-bottom: 1px solid #e7ebf1; }
  .contact-list { max-height: 230px; overflow-y: auto; }
  .composer-toolbar { align-items: flex-end; gap: 10px; }
  .composer-toolbar small { display: none; }
}
</style>
