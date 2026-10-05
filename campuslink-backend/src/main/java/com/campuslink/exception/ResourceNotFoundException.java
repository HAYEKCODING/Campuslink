package com.campuslink.exception;

/**
 * Exception levée lorsqu'une ressource demandée est introuvable (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s introuvable avec %s = '%s'", resourceName, fieldName, fieldValue));
    }

}
