import { reactive } from "vue";
import { defineStore } from "pinia";

const STORAGE_KEY = "paperpilot-ui";

function readJson(key, fallback) {
  const raw = localStorage.getItem(key);
  if (!raw) return fallback;
  try {
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

export const useUiStore = defineStore("ui", () => {
  const layout = reactive(
    readJson(STORAGE_KEY, {
      sidebarCollapsed: false,
      showNotifications: false,
      showProfileMenu: false,
    }),
  );

  function persist() {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        ...layout,
        showNotifications: false,
        showProfileMenu: false,
      }),
    );
  }

  function toggleSidebar() {
    layout.sidebarCollapsed = !layout.sidebarCollapsed;
    persist();
  }

  function toggleNotifications() {
    layout.showNotifications = !layout.showNotifications;
    if (layout.showNotifications) {
      layout.showProfileMenu = false;
    }
    persist();
  }

  function toggleProfileMenu() {
    layout.showProfileMenu = !layout.showProfileMenu;
    if (layout.showProfileMenu) {
      layout.showNotifications = false;
    }
    persist();
  }

  function closeOverlays() {
    layout.showNotifications = false;
    layout.showProfileMenu = false;
    persist();
  }

  return {
    closeOverlays,
    layout,
    toggleNotifications,
    toggleProfileMenu,
    toggleSidebar,
  };
});
