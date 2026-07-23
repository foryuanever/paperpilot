import { reactive } from "vue";
import { defineStore } from "pinia";

export const useDialogStore = defineStore("dialog", () => {
  const state = reactive({
    open: false,
    title: "",
    message: "",
    confirmText: "确定",
    cancelText: "取消",
    showCancel: false,
    danger: false,
    mode: "message",
    inputValue: "",
    inputPlaceholder: "",
  });

  let resolveCurrent = null;

  function openDialog(options) {
    if (resolveCurrent) resolveCurrent(false);
    Object.assign(state, {
      open: true,
      title: options.title || "提示",
      message: options.message || "",
      confirmText: options.confirmText || "确定",
      cancelText: options.cancelText || "取消",
      showCancel: Boolean(options.showCancel),
      danger: Boolean(options.danger),
      mode: options.mode || "message",
      inputValue: options.defaultValue || "",
      inputPlaceholder: options.placeholder || "",
    });
    return new Promise(resolve => {
      resolveCurrent = resolve;
    });
  }

  function alert(message, options = {}) {
    return openDialog({
      ...options,
      message,
      showCancel: false,
    });
  }

  // Compatibility aliases for older admin modules. Keep these inside the
  // app dialog so operational errors never fall back to browser alert().
  function showError(message, options = {}) {
    return alert(message, { ...options, title: options.title || "操作失败", danger: true });
  }

  function showSuccess(message, options = {}) {
    return alert(message, { ...options, title: options.title || "操作完成" });
  }

  function confirm(message, options = {}) {
    return openDialog({
      title: "请确认",
      ...options,
      message,
      showCancel: true,
    });
  }

  function prompt(message, options = {}) {
    return openDialog({
      title: "请输入",
      ...options,
      message,
      showCancel: true,
      mode: "prompt",
    });
  }

  function finish(result) {
    const payload = state.mode === "prompt"
      ? (result ? state.inputValue : null)
      : result;
    state.open = false;
    const resolve = resolveCurrent;
    resolveCurrent = null;
    resolve?.(payload);
  }

  return {
    state,
    alert,
    showError,
    showSuccess,
    confirm,
    prompt,
    accept: () => finish(true),
    cancel: () => finish(false),
  };
});
