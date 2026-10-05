package com.campuslink.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriétés de configuration JWT, mappées depuis {@code application.yml}
 * (préfixe {@code application.jwt}).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.jwt")
public class JwtProperties {

    /**
     * Clé secrète utilisée pour signer les tokens (HS256).
     */
    private String secret;

    /**
     * Durée de validité du token d'accès, en millisecondes.
     */
    private long accessTokenExpirationMs;

    /**
     * Durée de validité du refresh token, en millisecondes.
     */
    private long refreshTokenExpirationMs;

    /**
     * Émetteur (issuer) inséré dans les claims du token.
     */
    private String issuer;

}
