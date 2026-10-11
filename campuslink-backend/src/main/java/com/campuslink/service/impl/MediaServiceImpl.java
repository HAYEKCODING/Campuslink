package com.campuslink.service.impl;

import com.campuslink.config.CloudinaryProperties;
import com.campuslink.config.MediaProperties;
import com.campuslink.dto.response.MediaResponse;
import com.campuslink.exception.MediaUploadException;
import com.campuslink.service.MediaService;
import com.campuslink.util.MediaFileValidator;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implémentation du module média, basée sur le SDK Java officiel de Cloudinary.
 *
 * <p><strong>Deux modes de stockage, détection automatique</strong> :</p>
 * <ul>
 *   <li><strong>Cloudinary</strong> (credentials renseignés — production) :
 *       compression et redimensionnement appliqués à l'upload lui-même
 *       (transformation « à l'entrée » : {@code quality},
 *       {@code fetch_format: auto}, {@code crop: limit}), plutôt que servis à
 *       la demande.</li>
 *   <li><strong>Stockage local de repli</strong> (credentials absents — dev,
 *       démo, CI) : le fichier est écrit dans {@code application.media.local-dir}
 *       et servi via {@code GET /media/files/{filename}}. Sans ce repli, un
 *       environnement sans identifiants Cloudinary répond 500 sur chaque
 *       upload et bloque l'onboarding à l'étape photo.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;
    private final MediaProperties mediaProperties;

    @Override
    public MediaResponse upload(MultipartFile file) {
        MediaFileValidator.validate(file, mediaProperties);

        if (!isCloudinaryConfigured()) {
            return uploadToLocal(file);
        }

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
        if (!isCloudinaryConfigured()) {
            deleteLocal(publicId);
            return;
        }

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

    @Override
    public Optional<Resource> findLocalFile(String filename) {
        if (!StringUtils.hasText(filename) || !filename.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
            return Optional.empty();
        }

        Path dir = localDirectory().toAbsolutePath().normalize();
        Path path = dir.resolve(filename).normalize();
        if (!path.startsWith(dir) || !Files.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(path));
    }

    // ===================== Stockage local de repli =====================

    /** Les trois identifiants Cloudinary doivent être présents pour utiliser le cloud. */
    private boolean isCloudinaryConfigured() {
        return StringUtils.hasText(cloudinaryProperties.getCloudName())
                && StringUtils.hasText(cloudinaryProperties.getApiKey())
                && StringUtils.hasText(cloudinaryProperties.getApiSecret());
    }

    private Path localDirectory() {
        return Paths.get(StringUtils.hasText(mediaProperties.getLocalDir())
                ? mediaProperties.getLocalDir()
                : "uploads");
    }

    /**
     * Écrit le fichier dans le dossier local avec un nom unique (UUID +
     * extension) et renvoie son URL publique construite depuis le contexte
     * de requête (context-path {@code /api} compris).
     */
    private MediaResponse uploadToLocal(MultipartFile file) {
        try {
            Path dir = localDirectory();
            Files.createDirectories(dir);

            String originalName = StringUtils.cleanPath(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "fichier");
            String extension = extensionOf(originalName);
            String filename = UUID.randomUUID() + extension;

            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(dir.toAbsolutePath().normalize()) && !target.startsWith(dir)) {
                throw new MediaUploadException("Chemin de fichier refusé.");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/media/files/{filename}")
                    .buildAndExpand(filename)
                    .toUriString();

            log.warn("Cloudinary non configuré : fichier stocké localement → {} ({} octets)",
                    filename, file.getSize());

            return MediaResponse.builder()
                    .url(url)
                    .publicId(filename)
                    .format(extension.startsWith(".") ? extension.substring(1) : null)
                    .bytes(file.getSize())
                    .build();

        } catch (IOException ex) {
            log.error("Écriture locale impossible : {}", ex.getMessage());
            throw new MediaUploadException("Impossible d'enregistrer le fichier pour le moment.", ex);
        }
    }

    /** Suppression idempotente côté local : fichier absent = succès. */
    private void deleteLocal(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            return;
        }
        try {
            Path dir = localDirectory().toAbsolutePath().normalize();
            Path path = dir.resolve(publicId).normalize();
            if (!path.startsWith(dir)) {
                log.warn("Suppression locale refusée (hors dossier) : {}", publicId);
                return;
            }
            boolean deleted = Files.deleteIfExists(path);
            log.info("Suppression locale de {} ({})", publicId,
                    deleted ? "fichier supprimé" : "introuvable — traité comme idempotent");
        } catch (IOException ex) {
            log.error("Erreur lors de la suppression locale de {} : {}", publicId, ex.getMessage());
            throw new MediaUploadException("Impossible de supprimer le fichier pour le moment.", ex);
        }
    }

    /** Extension normalisée (ex. {@code .jpg}) ou chaîne vide. */
    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        String extension = filename.substring(dot).replaceAll("[^A-Za-z0-9.]", "");
        return extension.length() <= 10 ? extension : "";
    }

    // ===================== Mode Cloudinary =====================

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
