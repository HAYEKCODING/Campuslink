package com.campuslink.util;

import com.campuslink.config.MediaProperties;
import com.campuslink.exception.BadRequestException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validation d'un fichier média avant envoi à Cloudinary.
 *
 * <p>Classe utilitaire statique et sans état (même approche que
 * {@link EmailMasker}, {@link OtpEmailTemplateBuilder}) plutôt qu'un bean
 * Spring : ne dépend que de son paramètre d'entrée, facilement testable en
 * isolation, réutilisable par tout futur module ayant besoin de valider un
 * upload sans passer par {@code MediaService}.</p>
 *
 * <p>Défense en profondeur volontairement à deux niveaux : cette validation
 * couvre taille et type MIME déclaré (rapide, avant tout appel réseau) ;
 * Cloudinary lui-même rejette ensuite tout contenu qui ne se décode pas
 * comme une image valide (voir {@code MediaServiceImpl}, qui traduit cet
 * échec en {@code MediaUploadException}).</p>
 */
public final class MediaFileValidator {

    private MediaFileValidator() {
        // Classe utilitaire : instanciation interdite
    }

    /**
     * @throws BadRequestException si le fichier est absent, vide, trop volumineux
     *         ou d'un type MIME non autorisé.
     */
    public static void validate(MultipartFile file, MediaProperties properties) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide ou manquant.");
        }

        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            long maxSizeMb = properties.getMaxFileSizeBytes() / (1024 * 1024);
            throw new BadRequestException(
                    "Le fichier dépasse la taille maximale autorisée (" + maxSizeMb + " Mo).");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)
                || !properties.getAllowedContentTypes().contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Type de fichier non autorisé. Formats acceptés : "
                            + String.join(", ", properties.getAllowedContentTypes()));
        }
    }

}
