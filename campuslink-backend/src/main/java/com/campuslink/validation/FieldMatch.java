package com.campuslink.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Contrainte de validation croisée entre deux champs d'un même DTO
 * (typiquement : {@code password} / {@code confirmPassword}).
 *
 * <p>Contrainte au niveau classe (et non champ), car la comparaison porte sur
 * deux propriétés de l'objet validé. Réutilisable pour tout DTO ayant besoin
 * de ce type de validation (ex. futur changement de mot de passe).</p>
 *
 * <pre>{@code
 * @FieldMatch(field = "password", fieldMatch = "confirmPassword")
 * public class RegisterRequest { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FieldMatchValidator.class)
public @interface FieldMatch {

    String message() default "Les champs ne correspondent pas.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Nom de la propriété de référence.
     */
    String field();

    /**
     * Nom de la propriété devant correspondre à {@link #field()}.
     */
    String fieldMatch();

}
