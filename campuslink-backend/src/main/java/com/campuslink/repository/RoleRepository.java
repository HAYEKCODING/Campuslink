package com.campuslink.repository;

import com.campuslink.entity.Role;
import com.campuslink.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux données pour l'entité {@link Role}.
 */
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Recherche un rôle par son nom — utilisé notamment pour attribuer le rôle
     * {@code STUDENT} par défaut lors de l'inscription.
     */
    Optional<Role> findByName(RoleName name);

}
