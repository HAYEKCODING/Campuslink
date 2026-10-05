package com.campuslink.dto.request;

import com.campuslink.validation.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload d'inscription — {@code POST /auth/register}.
 *
 * <p>La contrainte {@link FieldMatch} au niveau classe vérifie que
 * {@code password} et {@code confirmPassword} sont identiques ; en cas
 * d'échec, l'erreur est rattachée au champ {@code confirmPassword} dans
 * la réponse de validation (voir {@code GlobalExceptionHandler}).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldMatch(
        field = "password",
        fieldMatch = "confirmPassword",
        message = "Les mots de passe ne correspondent pas."
)
public class RegisterRequest {

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Le format de l'email est invalide.")
    @Size(max = 180, message = "L'email ne doit pas dépasser 180 caractères.")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Le mot de passe doit contenir au moins une lettre et un chiffre."
    )
    private String password;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire.")
    private String confirmPassword;

    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    private String lastName;

}
