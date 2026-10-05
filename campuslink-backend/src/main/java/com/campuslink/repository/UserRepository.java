package com.campuslink.repository;

import com.campuslink.entity.User;
import com.campuslink.enums.AccountStatus;
import com.campuslink.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux données pour l'entité {@link User}.
 *
 * <p>Étend {@link JpaSpecificationExecutor} pour la recherche dynamique du
 * module Administration (voir {@link com.campuslink.specification.UserSpecification}).
 * Les méthodes de comptage ci-dessous alimentent {@code AdminUserServiceImpl.getStatistics()}.</p>
 */
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByLegacyId(Long legacyId);

    boolean existsByEmail(String email);

    long countByStatus(AccountStatus status);

    long countByEmailVerifiedTrue();

    long countByEmailVerifiedFalse();

    long countByCreatedAtAfter(Instant since);

    /**
     * Compte les utilisateurs possédant un rôle donné. Notation explicite avec
     * underscore ({@code Roles_Name}) pour lever toute ambiguïté sur la
     * traversée de la relation Many-to-Many {@code User.roles} → {@code Role.name}.
     */
    long countByRoles_Name(RoleName roleName);

}
