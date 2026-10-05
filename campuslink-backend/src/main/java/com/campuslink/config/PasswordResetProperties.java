package com.campuslink.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriétés de configuration du module Forgot Password, mappées depuis
 * {@code application.yml} (préfixe {@code application.password-reset}).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.password-reset")
public class PasswordResetProperties {

    /**
     * Durée de validité d'un lien de réinitialisation, en minutes.
     */
    private long tokenExpirationMinutes;

    /**
     * Délai minimal entre deux demandes de réinitialisation pour un même
     * utilisateur (anti-spam), en secondes.
     */
    private long resendCooldownSeconds;

    /**
     * URL de base du frontend sur laquelle le token est ajouté en paramètre
     * de requête pour former le lien envoyé par email
     * (ex. {@code https://app.campuslink.io/reset-password}).
     */
    private String resetUrl;

    /**
     * Expression cron du job de purge automatique des tokens expirés/utilisés.
     */
    private String cleanupCron;

}
