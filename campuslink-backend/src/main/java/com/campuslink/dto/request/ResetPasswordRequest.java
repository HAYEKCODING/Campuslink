package com.campuslink.dto.request;

import com.campuslink.validation.FieldMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload — {@code POST /auth/reset-password}.
 *
 * <p>Le token identifie à lui seul l'utilisateur concerné (voir
 * {@code PasswordResetToken}) — aucun email n'est requis ici.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldMatch(
        field = "newPassword",
        fieldMatch = "confirmPassword",
        message = "Les mots de passe ne correspondent pas."
)
public class ResetPasswordRequest {

    @NotBlank(message = "Le token est obligatoire.")
    @Size(max = 512, message = "Token invalide.")
    private String token;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
    @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Le mot de passe doit contenir au moins une lettre et un chiffre."
    )
    private String newPassword;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire.")
    private String confirmPassword;

}
