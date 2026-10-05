package com.campuslink.exception;

/**
 * Exception levée lors d'une tentative de création d'une ressource
 * entrant en conflit avec une ressource existante (HTTP 409).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

}
