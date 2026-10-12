package com.campuslink.realtime.service;

import com.campuslink.config.ChatMediaProperties;
import com.campuslink.exception.BadRequestException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ChatMediaFileValidator {
    private static final Map<String, String> MIME_BY_EXTENSION = Map.ofEntries(
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".png", "image/png"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".mp4", "video/mp4"),
            Map.entry(".mov", "video/quicktime"),
            Map.entry(".webm", "video/webm"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".doc", "application/msword"),
            Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    );

    private ChatMediaFileValidator() {}

    public static ValidatedChatMedia validate(MultipartFile file, ChatMediaProperties properties) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide ou manquant.");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BadRequestException("Le fichier dépasse la taille maximale autorisée de "
                    + properties.getMaxFileSizeBytes() / (1024 * 1024) + " Mo.");
        }

        String fileName = safeFileName(file.getOriginalFilename());
        int dot = fileName.lastIndexOf('.');
        String extension = dot < 0 ? "" : fileName.substring(dot).toLowerCase(Locale.ROOT);
        String expectedMime = MIME_BY_EXTENSION.get(extension);
        String declaredMime = file.getContentType();
        if (expectedMime == null || !expectedMime.equalsIgnoreCase(declaredMime == null ? "" : declaredMime.trim())) {
            throw new BadRequestException("Extension et type MIME du fichier non autorisés ou incompatibles.");
        }

        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(16);
            if (!matchesSignature(extension, header)) {
                throw new BadRequestException("Le contenu réel du fichier ne correspond pas à son type déclaré.");
            }
        } catch (IOException ex) {
            throw new BadRequestException("Impossible de vérifier le contenu du fichier.");
        }
        if (".docx".equals(extension) && !hasWordDocument(file)) {
            throw new BadRequestException("Le fichier DOCX est invalide.");
        }
        return new ValidatedChatMedia(fileName, extension, expectedMime, file.getSize());
    }

    private static String safeFileName(String originalName) {
        String name = StringUtils.hasText(originalName) ? originalName.replace('\\', '/') : "";
        name = name.substring(name.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        if (name.isBlank() || name.length() > 255 || ".".equals(name) || "..".equals(name)) {
            throw new BadRequestException("Nom de fichier invalide.");
        }
        return name;
    }

    private static boolean matchesSignature(String extension, byte[] bytes) {
        return switch (extension) {
            case ".jpg", ".jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
            case ".png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case ".webp" -> ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP");
            case ".gif" -> ascii(bytes, 0, "GIF87a") || ascii(bytes, 0, "GIF89a");
            case ".mp4", ".mov" -> ascii(bytes, 4, "ftyp");
            case ".webm" -> startsWith(bytes, 0x1A, 0x45, 0xDF, 0xA3);
            case ".pdf" -> ascii(bytes, 0, "%PDF-");
            case ".doc" -> startsWith(bytes, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case ".docx" -> startsWith(bytes, 0x50, 0x4B, 0x03, 0x04);
            default -> false;
        };
    }

    private static boolean hasWordDocument(MultipartFile file) {
        boolean contentTypesFound = false;
        boolean documentFound = false;
        long expandedBytes = 0;
        int entryCount = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (++entryCount > 10_000) return false;
                if ("[Content_Types].xml".equals(entry.getName())) contentTypesFound = true;
                if ("word/document.xml".equals(entry.getName())) documentFound = true;
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    expandedBytes += read;
                    if (expandedBytes > 100L * 1024 * 1024) return false;
                }
            }
        } catch (IOException ex) {
            return false;
        }
        return contentTypesFound && documentFound;
    }

    private static boolean startsWith(byte[] bytes, int... expected) {
        if (bytes.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[i] & 0xFF) != expected[i]) return false;
        }
        return true;
    }

    private static boolean ascii(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) return false;
        for (int i = 0; i < value.length(); i++) {
            if (bytes[offset + i] != (byte) value.charAt(i)) return false;
        }
        return true;
    }

    public record ValidatedChatMedia(String fileName, String extension, String contentType, long sizeBytes) {}
}
