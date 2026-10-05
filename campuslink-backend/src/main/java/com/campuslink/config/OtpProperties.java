package com.campuslink.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriétés de configuration du module OTP, mappées depuis {@code application.yml}
 * (préfixe {@code application.otp}).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.otp")
public class OtpProperties {

    /**
     * Longueur du code généré (nombre de chiffres).
     */
    private int length;

    /**
     * Durée de validité d'un code, en minutes.
     */
    private long expirationMinutes;

    /**
     * Délai minimal entre deux générations de code pour un même utilisateur
     * et un même type (anti-spam), en secondes.
     */
    private long resendCooldownSeconds;

    /**
     * Nombre maximal de tentatives de vérification autorisées pour un même code
     * avant qu'il ne soit invalidé.
     */
    private int maxAttempts;

    /**
     * Expression cron du job de purge automatique des codes expirés/utilisés.
     */
    private String cleanupCron;

}
