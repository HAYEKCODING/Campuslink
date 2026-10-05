package com.campuslink.exception;

/**
 * Exception générique pour toute violation de règle métier ne correspondant
 * à aucune exception plus spécifique déjà existante (ex. {@link DuplicateResourceException},
 * {@link InvalidTokenException}...). Mappée sur HTTP 422 (Unprocessable Entity) :
 * la requête est syntaxiquement valide mais ne peut être traitée en l'état.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

}
