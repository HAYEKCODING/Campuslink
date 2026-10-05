package com.campuslink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Résultat d'une opération d'upload ou de remplacement sur Cloudinary.
 *
 * <p>{@code publicId} doit être conservé côté appelant (ex. sur
 * {@code Profile.avatarUrl} associé à un identifiant séparé, ou dans une
 * future entité média dédiée) : c'est lui qui permettra de supprimer ou
 * remplacer ce fichier plus tard — il ne se retrouve pas dans l'URL.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediaResponse {

    /**
     * URL HTTPS sécurisée ({@code secure_url}) — celle à afficher au client.
     */
    private String url;

    /**
     * Identifiant Cloudinary du fichier — à fournir lors d'une suppression
     * ou d'un remplacement ultérieur.
     */
    private String publicId;

    private String format;
    private Integer width;
    private Integer height;

    /**
     * Poids du fichier après compression, en octets.
     */
    private Long bytes;

}
