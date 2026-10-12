import { api, resolveMediaUrl } from "../lib/api";

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
  return api.get("/matches").then((matches) =>
    (matches || []).map((match) => ({
      ...match,
      // Recadre l'URL de photo sur l'origine API courante (les URLs sont
      // persistées en base au moment de l'upload — voir resolveMediaUrl).
      autreUtilisateurPhoto: resolveMediaUrl(match.autreUtilisateurPhoto) || null,
    }))
  );
}
