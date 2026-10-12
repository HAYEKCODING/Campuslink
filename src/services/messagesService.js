import { api } from "../lib/api";

/**
 * Service messagerie — aligné sur le backend réel (module realtime).
 *
 * Une "conversation" côté API est un match : `GET /matches` liste les matchs
 * actifs avec l'identité de l'autre membre (voir matchesService), et l'historique
 * d'une conversation est `GET /matches/{matchId}/messages` (Page Spring,
 * triée du plus récent au plus ancien).
 *
 * L'envoi se fait via `POST /matches/{matchId}/messages`, équivalent HTTP du
 * point d'entrée temps réel `/app/chat.send`.
 */

export async function getMessages(matchId) {
  const page = await api.get(`/matches/${matchId}/messages?size=100`);
  const messages = page?.content ?? [];
  // L'API renvoie les messages du plus récent au plus ancien : on inverse
  // pour l'affichage chronologique.
  return [...messages].reverse();
}

export function sendMessage(matchId, text) {
  return api.post(`/matches/${matchId}/messages`, { matchId, contenu: text });
}

export function markMessagesRead(matchId) {
  return api.post(`/matches/${matchId}/messages/lu`);
}

export function sendChatMedia(matchId, file) {
  const formData = new FormData();
  formData.append("file", file);
  return api.post(`/matches/${matchId}/media`, formData);
}

export function getChatMediaLibrary(matchId, category, page = 0) {
  const params = new URLSearchParams({ category, page: String(page), size: "50" });
  return api.get(`/matches/${matchId}/media?${params.toString()}`);
}

export function getChatMediaContent(matchId, messageId) {
  return api.get(`/matches/${matchId}/media/${messageId}/content`, { responseType: "blob" });
}
