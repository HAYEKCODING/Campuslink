package com.campuslink.exception;

/**
 * Exception levée lorsqu'un utilisateur authentifié n'a pas les droits
 * nécessaires pour réaliser une opération (HTTP 403).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

}
