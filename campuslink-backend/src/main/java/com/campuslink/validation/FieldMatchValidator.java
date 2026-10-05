package com.campuslink.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Objects;

/**
 * Implémentation de la contrainte {@link FieldMatch}.
 *
 * <p>Utilise {@link BeanWrapperImpl} (Spring) pour lire dynamiquement la valeur
 * des deux propriétés désignées par leur nom, sans dépendre du type concret du DTO.</p>
 */
public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {

    private String field;
    private String fieldMatch;
    private String message;

    @Override
    public void initialize(FieldMatch constraintAnnotation) {
        this.field = constraintAnnotation.field();
        this.fieldMatch = constraintAnnotation.fieldMatch();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        Object fieldValue = new BeanWrapperImpl(value).getPropertyValue(field);
        Object fieldMatchValue = new BeanWrapperImpl(value).getPropertyValue(fieldMatch);

        boolean isValid = Objects.equals(fieldValue, fieldMatchValue);

        if (!isValid) {
            // Rattache l'erreur au champ de confirmation plutôt qu'à la classe entière,
            // pour un retour d'erreur exploitable directement par le client (fieldErrors).
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(fieldMatch)
                    .addConstraintViolation();
        }

        return isValid;
    }

}
