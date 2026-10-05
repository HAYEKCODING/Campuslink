package com.campuslink.dto.request;

import com.campuslink.enums.RoleName;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload — {@code PATCH /admin/users/{userId}/role}.
 *
 * <p>Remplace l'intégralité des rôles de l'utilisateur par ce rôle unique
 * (voir {@code AdminUserServiceImpl.changeRole} pour la justification).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeRoleRequest {

    @NotNull(message = "Le rôle est obligatoire.")
    private RoleName role;

}
