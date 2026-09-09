function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function safeColor(value) {
  const match = String(value || "").match(/^#[0-9a-fA-F]{6}$/);
  return match ? match[0] : "";
}

export function sanitizeSiteMessageHtml(value) {
  const raw = String(value ?? "");
  if (!raw.includes("<")) return escapeHtml(raw).replaceAll("\n", "<br>");
  const doc = new DOMParser().parseFromString(`<div>${raw}</div>`, "text/html");
  const render = (node) => {
    if (node.nodeType === Node.TEXT_NODE) return escapeHtml(node.nodeValue);
    if (node.nodeType !== Node.ELEMENT_NODE) return "";
    const tag = node.tagName.toLowerCase();
    if (tag === "br") return "<br>";
    const children = Array.from(node.childNodes).map(render).join("");
    if (tag === "font" || tag === "span") {
      const color = safeColor(node.getAttribute("color") || node.style.color);
      return color ? `<span style="color:${color}">${children}</span>` : children;
    }
    if (["div", "p"].includes(tag)) return `${children}<br>`;
    return children;
  };
  return Array.from(doc.body.firstElementChild.childNodes).map(render).join("");
}
