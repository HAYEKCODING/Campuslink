package com.campuslink.dto.response;

import com.campuslink.enums.OtpType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Réponse renvoyée par {@code /otp/send} et {@code /otp/resend}.
 *
 * <p>Ne confirme jamais explicitement qu'un email correspond à un compte
 * existant (voir {@code OtpServiceImpl.generateAndSend} — la même réponse est
 * renvoyée que le compte existe ou non, pour ne pas exposer d'oracle
 * d'énumération d'emails).</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpResponse {

    /**
     * Email partiellement masqué (ex. {@code e***t@campuslink.io}).
     */
    private String email;

    private OtpType type;

    private Instant expiresAt;

    /**
     * Instant à partir duquel un nouvel envoi sera accepté (délai anti-spam).
     */
    private Instant resendAvailableAt;

}
