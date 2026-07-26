const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("paperSolverDesktop", {
  isDesktop: true,
  getRuntimeInfo: () => ipcRenderer.invoke("desktop:get-runtime-info"),
  getBackendConfig: () => ipcRenderer.invoke("desktop:get-backend-config"),
  setBackendConfig: (payload) => ipcRenderer.invoke("desktop:set-backend-config", payload),
  resetBackendConfig: () => ipcRenderer.invoke("desktop:reset-backend-config"),
  setCaptureSession: (payload) => ipcRenderer.invoke("desktop:set-capture-session", payload),
  reloadApp: () => ipcRenderer.invoke("desktop:reload-app"),
  selectPdfStorageDir: () => ipcRenderer.invoke("desktop:select-pdf-storage-dir"),
  importZoteroLocal: (options) => ipcRenderer.invoke("desktop:zotero-import-local", options),
  readZoteroPdf: (pdfRef) => ipcRenderer.invoke("desktop:read-zotero-pdf", pdfRef),
  cachePdf: (payload) => ipcRenderer.invoke("desktop:cache-pdf", payload),
  getCachedPdf: (payload) => ipcRenderer.invoke("desktop:get-cached-pdf", payload),
  getCacheInfo: () => ipcRenderer.invoke("desktop:get-cache-info"),
  clearPdfCache: () => ipcRenderer.invoke("desktop:clear-pdf-cache"),
  openCacheDir: () => ipcRenderer.invoke("desktop:open-cache-dir"),
  getTranslationProviders: () => ipcRenderer.invoke("desktop:get-translation-providers"),
  testTranslationProvider: (payload) => ipcRenderer.invoke("desktop:test-translation-provider", payload),
  translate: (payload) => ipcRenderer.invoke("desktop:translate", payload)
});
