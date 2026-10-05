package com.campuslink.realtime.integration;

import java.util.Optional;
import java.util.UUID;

/**
 * Port permettant au module Temps Real de recuperer les informations d'identite
 * (nom, photo, id public du profil) d'un autre utilisateur a partir de son
 * legacyId, sans jamais dependre des entites du Backend Core.
 *
 * <p>Consomme par {@code MatchServiceImpl} pour enrichir la liste des matchs :
 * sans cette information, l'application ne peut afficher aucun nom ni aucune
 * photo dans "Mes matchs" et dans la liste des conversations.</p>
 */
public interface UserDirectoryPort {

    /**
     * Informations d'affichage minimales d'un utilisateur.
     *
     * @param legacyId  identifiant module realtime (likes / matchs / messages)
     * @param profilId  identifiant public du profil (UUID) — {@code null} si l'utilisateur n'a pas encore de profil
     * @param nom       nom complet affichable — {@code null} si indisponible
     * @param photoUrl  URL de l'avatar — {@code null} si indisponible
     */
    record ApercuUtilisateur(Long legacyId, UUID profilId, String nom, String photoUrl) {
    }

    /** Retourne l'aperçu de l'utilisateur identifié par {@code legacyId}, ou absent s'il est inconnu. */
    Optional<ApercuUtilisateur> trouverApercu(Long legacyId);
}
