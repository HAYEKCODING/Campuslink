package com.campuslink.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload du formulaire de contact public — {@code POST /contact}.
 *
 * <p>Forme attendue par {@code src/services/contactService.js}
 * ({@code { name, email, message }}). La route est publique : la
 * validation stricte de ces trois champs (présence, format, longueur)
 * est le principal garde-fou contre le spam.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactRequest {

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    private String name;

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Le format de l'email est invalide.")
    @Size(max = 180, message = "L'email ne doit pas dépasser 180 caractères.")
    private String email;

    @NotBlank(message = "Le message est obligatoire.")
    @Size(max = 2000, message = "Le message ne doit pas dépasser 2000 caractères.")
    private String message;

}
