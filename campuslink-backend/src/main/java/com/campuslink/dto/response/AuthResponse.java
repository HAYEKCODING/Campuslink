package com.campuslink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Réponse renvoyée par {@code /auth/login} et {@code /auth/refresh-token} :
 * la paire de tokens ainsi qu'un résumé de l'utilisateur authentifié.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Durée de validité de l'access token, en secondes (pas en millisecondes,
     * pour rester cohérent avec la convention OAuth2 {@code expires_in}).
     */
    private long expiresIn;

    private UserResponse user;

}
