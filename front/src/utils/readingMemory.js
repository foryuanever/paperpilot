export function readingMemoryKey(user) {
  const identity = user?.userId || user?.email || "guest";
  return `papersolver-last-reading:${identity}`;
}

export function rememberLastReading(user, paper) {
  const workspaceId = String(paper?.workspaceId || paper?.id || "");
  if (workspaceId) localStorage.setItem(readingMemoryKey(user), workspaceId);
}

export function getLastReadingId(user) {
  return localStorage.getItem(readingMemoryKey(user)) || "";
}
