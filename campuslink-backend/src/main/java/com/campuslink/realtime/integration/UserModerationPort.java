package com.campuslink.realtime.integration;

/**
 * Port permettant au module Moderation (Dev C) de demander une mise a jour du statut
 * de l'utilisateur (suspendu / banni) sans jamais toucher directement l'entite User
 * du Backend Core (Dev B). Le Dev B doit fournir une implementation Spring (@Component)
 * de cette interface qui met a jour son propre modele de donnees.
 */
public interface UserModerationPort {

    void suspendre(Long userId, String motif);

    void bannir(Long userId, String motif);

    void debannir(Long userId, String motif);
}
