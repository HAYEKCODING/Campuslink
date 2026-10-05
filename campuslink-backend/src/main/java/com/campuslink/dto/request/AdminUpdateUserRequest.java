package com.campuslink.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload de modification administrative d'un utilisateur —
 * {@code PATCH /admin/users/{userId}}.
 *
 * <p>Mise à jour partielle : les deux champs sont optionnels, seuls ceux
 * fournis sont appliqués. Volontairement restreint à {@code email} et
 * {@code emailVerified} — le statut du compte et le rôle ont leurs propres
 * endpoints dédiés ({@code /suspend}, {@code /activate}, {@code /role}),
 * et le mot de passe ne se modifie jamais depuis l'administration (flux
 * de réinitialisation dédié).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUpdateUserRequest {

    @Email(message = "Le format de l'email est invalide.")
    @Size(max = 180, message = "L'email ne doit pas dépasser 180 caractères.")
    private String email;

    private Boolean emailVerified;

}
