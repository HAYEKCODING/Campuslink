package com.campuslink.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Propriétés de configuration du module média (Cloudinary), mappées depuis
 * {@code application.yml} (préfixe {@code application.media}).
 *
 * <p>Distinctes de {@link CloudinaryProperties} : celles-ci portent les
 * identifiants du compte Cloudinary (secrets d'authentification), celles-ci
 * portent les règles métier appliquées aux fichiers (taille, formats,
 * compression).</p>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.media")
public class MediaProperties {

    /**
     * Dossier Cloudinary dans lequel les fichiers sont rangés.
     */
    private String folder;

    /**
     * Taille maximale acceptée pour un fichier, en octets.
     */
    private long maxFileSizeBytes;

    /**
     * Types MIME autorisés (ex. {@code image/jpeg}, {@code image/png}).
     */
    private List<String> allowedContentTypes;

    /**
     * Niveau de qualité appliqué par Cloudinary lors de la compression
     * (ex. {@code auto:good} — laisse Cloudinary choisir le meilleur
     * compromis qualité/poids).
     */
    private String quality;

    /**
     * Dimension maximale (largeur et hauteur, en pixels) au-delà de laquelle
     * l'image est redimensionnée. Une image plus petite n'est jamais agrandie
     * (transformation Cloudinary {@code crop: limit}).
     */
    private int maxDimension;

}
