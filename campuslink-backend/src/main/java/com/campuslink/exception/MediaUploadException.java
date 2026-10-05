package com.campuslink.exception;

/**
 * Exception levée lorsqu'une opération Cloudinary échoue côté infrastructure
 * (API injoignable, erreur réseau, réponse d'erreur du service) — HTTP 502.
 *
 * <p>Distincte des erreurs de validation du fichier (taille, type — voir
 * {@link BadRequestException}, HTTP 400) : il ne s'agit pas d'une faute du
 * client, mais d'une indisponibilité ou d'une erreur du service externe.</p>
 */
public class MediaUploadException extends RuntimeException {

    public MediaUploadException(String message) {
        super(message);
    }

    public MediaUploadException(String message, Throwable cause) {
        super(message, cause);
    }

}
