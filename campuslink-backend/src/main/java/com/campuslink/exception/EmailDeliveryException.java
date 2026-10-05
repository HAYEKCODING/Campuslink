package com.campuslink.exception;

/**
 * Exception levée lorsque l'envoi d'un email échoue côté infrastructure
 * (serveur SMTP injoignable, identifiants invalides, etc.) — HTTP 503.
 *
 * <p>Distincte des erreurs de validation ou métier : il ne s'agit pas d'une
 * faute du client, mais d'une indisponibilité temporaire d'un service externe.</p>
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message) {
        super(message);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

}
