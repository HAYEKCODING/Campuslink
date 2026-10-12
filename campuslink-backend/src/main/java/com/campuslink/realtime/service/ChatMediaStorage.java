package com.campuslink.realtime.service;

import com.campuslink.config.CloudinaryProperties;
import com.campuslink.exception.MediaUploadException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FilterInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMediaStorage {
    private static final String CLOUDINARY_FOLDER = "campuslink-chat";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final String STORAGE_KEY_PATTERN =
            "^(image|video|raw):campuslink-chat/[0-9a-fA-F-]{36}(\\.(jpg|jpeg|png|webp|gif|mp4|mov|webm|pdf|doc|docx))?$";

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    public String store(MultipartFile file, String extension) {
        requireCloudinaryConfiguration();
        String resourceType = resourceType(extension);
        String publicId = CLOUDINARY_FOLDER + "/" + UUID.randomUUID()
                + ("raw".equals(resourceType) ? extension.toLowerCase(Locale.ROOT) : "");
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "type", "authenticated",
                    "public_id", publicId,
                    "overwrite", false,
                    "unique_filename", false,
                    "use_filename", false
            ));
            Object uploadedId = result.get("public_id");
            if (!publicId.equals(uploadedId)) {
                throw new MediaUploadException("Cloudinary n'a pas confirmé l'enregistrement de la pièce jointe.");
            }
            return resourceType + ":" + publicId;
        } catch (IOException ex) {
            throw new MediaUploadException("Impossible d'enregistrer la pièce jointe sur Cloudinary.", ex);
        }
    }

    public Optional<Resource> load(String storageKey) {
        Asset asset = parseKey(storageKey).orElse(null);
        if (asset == null) return Optional.empty();
        requireCloudinaryConfiguration();

        HttpURLConnection connection = null;
        try {
            String signedUrl = cloudinary.url()
                    .resourceType(asset.resourceType())
                    .type("authenticated")
                    .secure(true)
                    .signed(true)
                    .generate(asset.publicId());
            URI uri = URI.create(signedUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"res.cloudinary.com".equalsIgnoreCase(uri.getHost())) {
                throw new MediaUploadException("L'adresse de livraison Cloudinary n'est pas valide.");
            }

            connection = (HttpURLConnection) new URL(signedUrl).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND || status == HttpURLConnection.HTTP_GONE) {
                connection.disconnect();
                return Optional.empty();
            }
            if (status < 200 || status >= 300) {
                connection.disconnect();
                throw new MediaUploadException("Cloudinary n'a pas pu fournir la pièce jointe (HTTP "
                        + status + ").");
            }

            HttpURLConnection activeConnection = connection;
            return Optional.of(new InputStreamResource(new FilterInputStream(activeConnection.getInputStream()) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        activeConnection.disconnect();
                    }
                }
            }));
        } catch (IOException | IllegalArgumentException ex) {
            if (connection != null) connection.disconnect();
            throw new MediaUploadException("Impossible de récupérer la pièce jointe depuis Cloudinary.", ex);
        }
    }

    public void delete(String storageKey) {
        Asset asset = parseKey(storageKey).orElse(null);
        if (asset == null) return;
        requireCloudinaryConfiguration();
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(asset.publicId(), ObjectUtils.asMap(
                    "resource_type", asset.resourceType(),
                    "type", "authenticated"
            ));
            String status = String.valueOf(result.get("result"));
            if (!"ok".equals(status) && !"not found".equals(status)) {
                throw new MediaUploadException("Échec de la suppression de la pièce jointe sur Cloudinary.");
            }
        } catch (IOException ex) {
            throw new MediaUploadException("Impossible de supprimer la pièce jointe sur Cloudinary.", ex);
        }
    }

    private Optional<Asset> parseKey(String storageKey) {
        if (!StringUtils.hasText(storageKey) || !storageKey.matches(STORAGE_KEY_PATTERN)) {
            return Optional.empty();
        }
        int separator = storageKey.indexOf(':');
        return Optional.of(new Asset(storageKey.substring(0, separator), storageKey.substring(separator + 1)));
    }

    private String resourceType(String extension) {
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case ".jpg", ".jpeg", ".png", ".webp", ".gif" -> "image";
            case ".mp4", ".mov", ".webm" -> "video";
            case ".pdf", ".doc", ".docx" -> "raw";
            default -> throw new MediaUploadException("Type de pièce jointe Cloudinary non pris en charge.");
        };
    }

    private void requireCloudinaryConfiguration() {
        if (!StringUtils.hasText(cloudinaryProperties.getCloudName())
                || !StringUtils.hasText(cloudinaryProperties.getApiKey())
                || !StringUtils.hasText(cloudinaryProperties.getApiSecret())) {
            throw new MediaUploadException("Le stockage Cloudinary des pièces jointes n'est pas configuré.");
        }
    }

    private record Asset(String resourceType, String publicId) {}
}
