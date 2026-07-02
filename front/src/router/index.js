import { createRouter, createWebHistory } from "vue-router";
import LibraryView from "../views/LibraryView.vue";
import LoginView from "../views/LoginView.vue";
import ModelCenterView from "../views/ModelCenterView.vue";
import SearchView from "../views/SearchView.vue";
import ReaderView from "../views/ReaderView.vue";
import LiteratureReadingView from "../views/LiteratureReadingView.vue";
import MeetingReportDeckView from "../views/MeetingReportDeckView.vue";
import DualTranslateView from "../views/DualTranslateView.vue";
import ForumView from "../views/ForumView.vue";
import ForumPostView from "../views/ForumPostView.vue";
import TeamView from "../views/TeamView.vue";
import ProfileView from "../views/ProfileView.vue";
import MessagesView from "../views/MessagesView.vue";
import ReferralView from "../views/ReferralView.vue";
import { useAuthStore } from "../stores/auth";

const routes = [
  {
    path: "/",
    name: "login",
    component: LoginView,
    meta: { public: true },
  },
  {
    path: "/login",
    redirect: "/",
  },
  {
    path: "/dashboard",
    redirect: "/library",
  },
  {
    path: "/library",
    name: "library",
    component: LibraryView,
    meta: { requiresAuth: true },
  },
  {
    path: "/search",
    name: "search",
    component: SearchView,
    meta: { requiresAuth: true },
  },
  {
    path: "/reading",
    name: "literature-reading",
    component: LiteratureReadingView,
    meta: { requiresAuth: true },
  },
  {
    path: "/reader",
    name: "reader",
    component: ReaderView,
    meta: { requiresAuth: true, fullBleed: true },
  },
  {
    path: "/reader/dual",
    name: "reader-dual",
    component: DualTranslateView,
    meta: { requiresAuth: true, fullBleed: true },
  },
  {
    path: "/thesis",
    redirect: "/library?tab=add",
  },
  {
    path: "/meeting-report",
    name: "meeting-report",
    component: MeetingReportDeckView,
    meta: { requiresAuth: true },
  },
  {
    path: "/forum",
    name: "forum",
    component: ForumView,
    meta: { requiresAuth: true },
  },
  {
    path: "/forum/post/:id",
    name: "forum-post",
    component: ForumPostView,
    meta: { requiresAuth: true },
  },
  {
    path: "/register",
    redirect: "/?auth=register",
  },
  {
    path: "/admin",
    name: "admin",
    component: () => import("../views/AdminView.vue"),
    meta: { requiresAuth: true, adminOnly: true },
  },
  {
    path: "/models",
    name: "models",
    component: ModelCenterView,
    meta: { requiresAuth: true },
  },
  {
    path: "/referral",
    name: "referral",
    component: ReferralView,
    meta: { requiresAuth: true },
  },
  {
    path: "/billing",
    redirect: "/models?tab=billing",
  },
  {
    path: "/team",
    name: "team",
    component: TeamView,
    meta: { requiresAuth: true },
  },
  {
    path: "/profile",
    name: "profile",
    component: ProfileView,
    meta: { requiresAuth: true },
  },
  {
    path: "/messages",
    name: "messages",
    component: MessagesView,
    meta: { requiresAuth: true },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  if (to.meta.requiresAuth && !authStore.session.isAuthenticated) {
    return { name: "login" };
  }
  const userRole = authStore.session.user?.role;
  if (to.meta.adminOnly && userRole !== "管理员") {
    return { path: "/library" };
  }
  if (authStore.session.isAuthenticated && (to.path === "/" || to.path === "/login")) {
    if (userRole === "管理员") {
      return { path: "/admin" };
    }
    return { path: "/library" };
  }
  return true;
});

export default router;
