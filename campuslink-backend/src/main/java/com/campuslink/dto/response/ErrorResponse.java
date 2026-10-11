package com.campuslink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Structure homogène retournée pour toute réponse d'erreur de l'API.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldErrorDetail> fieldErrors;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldErrorDetail {
        private String field;
        private String message;

        /**
         * Valeur refusée par la validation. Masquée à {@code "***"} pour les
         * champs sensibles (mot de passe, token, code OTP, email) ; conservée
         * telle quelle pour les autres champs afin de diagnostiquer la saisie.
         */
        private Object rejectedValue;
    }

}
