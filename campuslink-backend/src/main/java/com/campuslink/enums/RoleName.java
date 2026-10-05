package com.campuslink.enums;

/**
 * Valeurs possibles pour le nom d'un rôle applicatif ({@link com.campuslink.entity.Role}).
 *
 * <p>Utilisé notamment pour construire les autorités Spring Security
 * (préfixées par {@code ROLE_} au moment de l'authentification).</p>
 */
public enum RoleName {

    STUDENT,
    TEACHER,
    ADMIN

}
