package com.campuslink.repository;

import com.campuslink.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux données pour l'entité {@link Profile}.
 *
 * <p>Étend {@link JpaSpecificationExecutor} pour exposer {@code findAll(Specification, Pageable)},
 * point d'entrée du moteur de recherche dynamique (voir
 * {@link com.campuslink.specification.ProfileSpecification}).</p>
 */
public interface ProfileRepository extends JpaRepository<Profile, UUID>, JpaSpecificationExecutor<Profile> {

    /**
     * Recherche le profil d'un utilisateur par l'id de celui-ci (et non par
     * l'id propre du profil) — point d'entrée pour "mon profil".
     */
    Optional<Profile> findByUserId(UUID userId);

}

