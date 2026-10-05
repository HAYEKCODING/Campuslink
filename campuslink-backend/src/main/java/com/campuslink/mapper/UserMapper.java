package com.campuslink.mapper;

import com.campuslink.dto.response.AdminUserResponse;
import com.campuslink.dto.response.UserResponse;
import com.campuslink.entity.Role;
import com.campuslink.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper MapStruct entre {@link User} et ses représentations exposées par l'API.
 *
 * <p>Référence {@link MapperConfig} pour une stratégie de mapping homogène avec
 * le reste de l'application (composant Spring, injection par constructeur).</p>
 */
@Mapper(config = MapperConfig.class)
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToRoleNames")
    UserResponse toUserResponse(User user);

    /**
     * Vue administrative d'un utilisateur (voir {@link AdminUserResponse} pour
     * la justification de son existence séparée de {@link #toUserResponse}).
     */
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToRoleNames")
    AdminUserResponse toAdminUserResponse(User user);

    /**
     * Convertit l'ensemble des entités {@link Role} en noms de rôles (String),
     * seule information pertinente à exposer côté client.
     */
    @Named("rolesToRoleNames")
    default Set<String> rolesToRoleNames(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }

}
