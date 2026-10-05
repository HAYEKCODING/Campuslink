package com.campuslink.enums;

/**
 * Statut administratif d'un compte utilisateur.
 */
public enum AccountStatus {

    /** Compte créé mais email non encore vérifié. */
    PENDING_VERIFICATION,

    /** Compte actif et pleinement opérationnel. */
    ACTIVE,

    /** Compte temporairement suspendu (modération). */
    SUSPENDED,

    /** Compte banni définitivement. */
    BANNED,

    /** Compte désactivé par l'utilisateur lui-même. */
    DEACTIVATED

}
