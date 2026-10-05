package com.campuslink.exception;

import java.util.Map;

/**
 * Exception de validation levée manuellement depuis la couche service
 * (validation croisée ou métier non exprimable via Bean Validation sur un DTO).
 * Distincte de {@link MethodArgumentNotValidException} / {@code ConstraintViolationException}
 * (déjà gérées par {@link GlobalExceptionHandler} pour la validation déclarative des DTO) :
 * celle-ci sert quand la validation se fait explicitement dans le code métier.
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.fieldErrors = Map.of();
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? Map.of() : fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

}
