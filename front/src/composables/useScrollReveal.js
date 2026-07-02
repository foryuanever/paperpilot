import { nextTick, onMounted, onUnmounted } from "vue";

export function useScrollReveal(rootSelector = ".spatial-page") {
  let observer = null;
  let fallbackTimer = null;

  function markVisibleInViewport(root) {
    const viewportBottom = window.innerHeight;
    root.querySelectorAll("[data-reveal]:not(.is-visible)").forEach((el) => {
      const rect = el.getBoundingClientRect();
      // 容差：元素在视口内（含轻微偏移）即标记可见，避免路由过渡时 rect 不稳定导致漏判
      if (rect.top < viewportBottom + 80 && rect.bottom > -80) {
        el.classList.add("is-visible");
      }
    });
  }

  onMounted(async () => {
    await nextTick();
    const root = document.querySelector(rootSelector);
    if (!root) return;

    root.classList.add("reveal-ready");

    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("is-visible");
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.08, rootMargin: "0px 0px -4% 0px" },
    );

    // 先用 rAF 等一帧布局稳定，再判断视口内元素，避免路由 Transition 期间 rect 异常
    requestAnimationFrame(() => {
      markVisibleInViewport(root);
      // 兜底：短延时后再扫一次，把过渡结束后才落入视口的元素补上，防止首次进入空白
      fallbackTimer = setTimeout(() => markVisibleInViewport(root), 120);
    });

    root.querySelectorAll("[data-reveal]").forEach((el) => {
      if (!el.classList.contains("is-visible")) {
        observer.observe(el);
      }
    });
  });

  onUnmounted(() => {
    observer?.disconnect();
    if (fallbackTimer) clearTimeout(fallbackTimer);
    document.querySelector(rootSelector)?.classList.remove("reveal-ready");
  });
}
