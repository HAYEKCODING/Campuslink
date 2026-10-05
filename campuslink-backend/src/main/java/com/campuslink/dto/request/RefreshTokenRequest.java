package com.campuslink.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload commun à {@code POST /auth/refresh-token} et {@code POST /auth/logout} —
 * les deux opérations ont besoin exactement du même refresh token en entrée.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest {

    @NotBlank(message = "Le refresh token est obligatoire.")
    private String refreshToken;

}
