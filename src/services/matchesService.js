import { api } from "../lib/api";

/**
 * Service matchs — GET /matches (matchs actifs de l'utilisateur connecté).
 *
 * Réponse (MatchResponse) :
 *   { id, autreUtilisateurId, autreUtilisateurProfilId, autreUtilisateurNom,
 *     autreUtilisateurPhoto, dateMatch, statut }
 *
 * `autreUtilisateurProfilId` (UUID) sert à ouvrir la fiche profil ;
 * `autreUtilisateurId` est l'identifiant du module temps réel (likes/messages).
 */
export function getMatches() {
  return api.get("/matches");
}
