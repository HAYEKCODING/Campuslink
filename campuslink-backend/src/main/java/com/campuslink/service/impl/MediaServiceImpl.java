package com.campuslink.service.impl;

import com.campuslink.config.MediaProperties;
import com.campuslink.dto.response.MediaResponse;
import com.campuslink.exception.MediaUploadException;
import com.campuslink.service.MediaService;
import com.campuslink.util.MediaFileValidator;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Implémentation du module média, basée sur le SDK Java officiel de Cloudinary.
 *
 * <p>La compression est appliquée à l'upload lui-même (transformation dite
 * "à l'entrée" : {@code quality}, {@code fetch_format: auto}, redimensionnement
 * en {@code crop: limit}) plutôt que servie à la demande — le fichier stocké
 * sur Cloudinary est déjà optimisé, chaque livraison ultérieure de l'URL
 * réutilise ce même asset compressé sans retraitement.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final Cloudinary cloudinary;
    private final MediaProperties mediaProperties;

    @Override
    public MediaResponse upload(MultipartFile file) {
        MediaFileValidator.validate(file, mediaProperties);

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), buildUploadOptions());
            MediaResponse response = toMediaResponse(uploadResult);

            log.info("Fichier uploadé sur Cloudinary : {} ({} octets, {}x{})",
                    response.getPublicId(), response.getBytes(), response.getWidth(), response.getHeight());

            return response;

        } catch (IOException ex) {
            log.error("Échec de l'upload Cloudinary : {}", ex.getMessage());
            throw new MediaUploadException(
                    "Impossible d'uploader le fichier pour le moment. Veuillez réessayer.", ex);
        }
    }

    @Override
    public MediaResponse replace(String existingPublicId, MultipartFile newFile) {
        // Le nouveau fichier est mis en ligne avant toute tentative de suppression
        // de l'ancien : en cas d'échec de la suppression, l'utilisateur garde au
        // moins une image valide plutôt que de se retrouver sans rien.
        MediaResponse uploaded = upload(newFile);

        if (StringUtils.hasText(existingPublicId)) {
            try {
                delete(existingPublicId);
            } catch (MediaUploadException ex) {
                log.warn("Nouveau fichier uploadé ({}) mais l'ancien ({}) n'a pas pu être supprimé : {}",
                        uploaded.getPublicId(), existingPublicId, ex.getMessage());
            }
        }

        return uploaded;
    }

    @Override
    public void delete(String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String status = String.valueOf(result.get("result"));

            // "not found" est traité comme un succès : l'objectif ("ce fichier ne
            // doit plus exister sur Cloudinary") est déjà atteint, l'opération est idempotente.
            if (!"ok".equals(status) && !"not found".equals(status)) {
                throw new MediaUploadException(
                        "Échec de la suppression du fichier sur Cloudinary (statut : " + status + ").");
            }

            log.info("Fichier Cloudinary {} traité en suppression (statut : {})", publicId, status);

        } catch (IOException ex) {
            log.error("Erreur lors de la suppression Cloudinary de {} : {}", publicId, ex.getMessage());
            throw new MediaUploadException("Impossible de supprimer le fichier pour le moment.", ex);
        }
    }

    /**
     * Construit les paramètres d'upload : rangement dans le dossier configuré,
     * et transformation de compression/redimensionnement appliquée à l'entrée.
     */
    private Map<String, Object> buildUploadOptions() {
        Map<String, Object> transformation = ObjectUtils.asMap(
                "quality", mediaProperties.getQuality(),
                "fetch_format", "auto",
                "width", mediaProperties.getMaxDimension(),
                "height", mediaProperties.getMaxDimension(),
                "crop", "limit"
        );

        return ObjectUtils.asMap(
                "folder", mediaProperties.getFolder(),
                "resource_type", "image",
                "transformation", transformation,
                "unique_filename", true,
                "overwrite", true
        );
    }

    private MediaResponse toMediaResponse(Map<?, ?> uploadResult) {
        return MediaResponse.builder()
                .url((String) uploadResult.get("secure_url"))
                .publicId((String) uploadResult.get("public_id"))
                .format((String) uploadResult.get("format"))
                .width(asInteger(uploadResult.get("width")))
                .height(asInteger(uploadResult.get("height")))
                .bytes(asLong(uploadResult.get("bytes")))
                .build();
    }

    private Integer asInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

}
