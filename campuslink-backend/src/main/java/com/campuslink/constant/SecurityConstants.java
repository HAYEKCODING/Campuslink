package com.campuslink.constant;

/**
 * Constantes utilisées par la couche de sécurité (headers, préfixes, routes publiques).
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Classe utilitaire : instanciation interdite
    }

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE_CLAIM = "type";
    public static final String ACCESS_TOKEN_TYPE = "ACCESS";
    public static final String REFRESH_TOKEN_TYPE = "REFRESH";
    public static final String AUTHORITIES_CLAIM = "authorities";

    /**
     * Endpoints publics, accessibles sans authentification.
     */
    public static final String[] PUBLIC_ENDPOINTS = {
            "/auth/**",
            "/otp/**",
            "/profiles/*/public",
            "/profiles/search",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/ws/**"
    };

    /**
     * Endpoints d'administration — exigent le rôle {@code ADMIN} en plus d'être
     * authentifié. Ne doit jamais recouper {@link #PUBLIC_ENDPOINTS}.
     */
    public static final String[] ADMIN_ENDPOINTS = {
            "/admin/**"
    };

}
