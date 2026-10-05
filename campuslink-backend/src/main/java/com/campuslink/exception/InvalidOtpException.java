package com.campuslink.exception;

/**
 * Exception levée lorsqu'un code OTP est incorrect, expiré, ou qu'aucun code
 * actif n'existe pour la demande (HTTP 400).
 *
 * <p>Le message ne doit jamais confirmer ni infirmer l'existence d'un compte
 * pour l'email fourni — voir {@code OtpServiceImpl}.</p>
 */
public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException(String message) {
        super(message);
    }

}
