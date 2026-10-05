package com.campuslink.exception;

/**
 * Exception levée lorsqu'une requête client est mal formée ou invalide (HTTP 400).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

}
