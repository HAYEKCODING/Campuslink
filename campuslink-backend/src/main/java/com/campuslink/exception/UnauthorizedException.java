package com.campuslink.exception;

/**
 * Exception levée lorsqu'une opération requiert une authentification
 * absente ou invalide (HTTP 401).
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

}
