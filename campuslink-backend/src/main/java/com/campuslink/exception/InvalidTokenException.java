package com.campuslink.exception;

/**
 * Exception levée lorsqu'un token à usage unique (ex. lien de réinitialisation
 * de mot de passe) est invalide, déjà utilisé ou expiré — HTTP 400.
 *
 * <p>Nom volontairement générique (non préfixé "PasswordReset") : réutilisable
 * pour tout futur mécanisme basé sur un token opaque à usage unique.</p>
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

}
