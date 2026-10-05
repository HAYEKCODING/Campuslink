package com.campuslink.realtime.integration;

/**
 * Port (interface) que le module Backend Core (Dev B) doit implementer/exposer
 * pour que le module Temps Reel (Dev C) puisse resoudre l'utilisateur courant
 * SANS dependre directement des entites User/Profile.
 *
 * Integration attendue (voir PROMPT 18 du cahier des charges) :
 *  - Le Dev B expose un bean Spring qui implemente cette interface
 *    (par ex. en s'appuyant sur son SecurityContext / JwtService existant).
 *  - Le Dev C consomme uniquement cette interface, jamais les classes internes du Dev B.
 */
public interface CurrentUserPort {

    /** Retourne l'id de l'utilisateur actuellement authentifie (via le contexte de securite HTTP). */
    Long getCurrentUserId();

    /** Retourne le role principal de l'utilisateur (USER, MODERATOR, ADMIN). */
    String getCurrentUserRole();

    /** Verifie si l'utilisateur donne existe et n'est pas banni/suspendu (utilise par Like/Match/Message). */
    boolean isUtilisateurActif(Long userId);
}
