import { defineStore } from "pinia";
import { reactive } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";

export const useForumStore = defineStore("forum", () => {
  const state = reactive({
    posts: [],
    loading: false,
    error: ""
  });

  async function fetchPosts(options = {}) {
    if (!options.silent) state.loading = true;
    state.error = "";
    try {
      state.posts = await paperpilotApi.getForumPosts();
    } catch (error) {
      state.error = "研究社区加载失败，请稍后重试";
      console.error("Failed to fetch forum posts:", error);
    } finally {
      if (!options.silent) state.loading = false;
    }
  }

  async function addPost(payload) {
    const result = await paperpilotApi.createForumPost(payload);
    await fetchPosts({ silent: true });
    return result;
  }

  async function updatePost(postId, payload) {
    const result = await paperpilotApi.updateForumPost(postId, payload);
    await fetchPosts({ silent: true });
    return result;
  }

  async function deletePost(postId) {
    await paperpilotApi.deleteForumPost(postId);
    state.posts = state.posts.filter(item => item.id !== postId);
  }

  async function togglePin(postId) {
    const post = state.posts.find(item => item.id === postId);
    if (post) post.pinned = !post.pinned;
    try {
      await paperpilotApi.toggleForumPostPin(postId);
      await fetchPosts({ silent: true });
    } catch (error) {
      await fetchPosts({ silent: true });
      throw error;
    }
  }

  async function toggleBan(postId) {
    const post = state.posts.find(item => item.id === postId);
    if (post) post.banned = !post.banned;
    try {
      await paperpilotApi.toggleForumPostBan(postId);
      await fetchPosts({ silent: true });
    } catch (error) {
      await fetchPosts({ silent: true });
      throw error;
    }
  }

  async function addReply(postId, payload) {
    await paperpilotApi.replyForumPost(postId, payload);
    await fetchPosts();
  }

  async function likePost(postId) {
    const post = state.posts.find(item => item.id === postId);
    if (post) {
      post.hasLiked = !post.hasLiked;
      post.likes = Math.max(0, post.likes + (post.hasLiked ? 1 : -1));
    }
    try {
      await paperpilotApi.likeForumPost(postId);
    } catch (error) {
      await fetchPosts();
    }
  }

  async function viewPost(postId) {
    const post = state.posts.find(item => item.id === postId);
    try {
      const result = await paperpilotApi.viewForumPost(postId);
      if (post && result?.views !== undefined) post.views = result.views;
    } catch (error) {
      await fetchPosts({ silent: true });
    }
  }

  async function bookmarkPost(postId) {
    const post = state.posts.find(item => item.id === postId);
    if (post) {
      post.hasBookmarked = !post.hasBookmarked;
      post.bookmarks = Math.max(0, post.bookmarks + (post.hasBookmarked ? 1 : -1));
    }
    try {
      await paperpilotApi.bookmarkForumPost(postId);
    } catch (error) {
      await fetchPosts();
    }
  }

  async function likeReply(postId, replyId) {
    const post = state.posts.find(item => item.id === postId);
    const reply = post?.replies.find(item => item.id === replyId);
    if (reply) {
      reply.hasLiked = !reply.hasLiked;
      reply.likes = Math.max(0, reply.likes + (reply.hasLiked ? 1 : -1));
    }
    try {
      await paperpilotApi.likeForumReply(postId, replyId);
    } catch (error) {
      await fetchPosts();
    }
  }

  fetchPosts();

  return { state, fetchPosts, addPost, updatePost, deletePost, togglePin, toggleBan, addReply, likePost, viewPost, bookmarkPost, likeReply };
});
