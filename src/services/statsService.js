import { api } from "../lib/api";

/**
 * Service statistiques publiques (landing page).
 * Réponse attendue: { memberCount: number, avatarPhotos: string[] }
 */
export function getPlatformStats() {
  return api.get("/stats/public");
}
