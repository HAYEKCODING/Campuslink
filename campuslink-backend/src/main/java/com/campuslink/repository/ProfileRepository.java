package com.campuslink.repository;

import com.campuslink.entity.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
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

    // ===================== Contenu public de la landing (PublicContentService) =====================

    /**
     * Échantillon d'avatars réels pour illustrer le compteur d'inscrits
     * ({@code GET /stats/public}). La borne est portée par {@link Pageable}
     * (converti en {@code setMaxResults}) plutôt que par {@code LIMIT} dans
     * la JPQL, non portable entre H2 (tests), MySQL et PostgreSQL.
     */
    @Query("select p.avatarUrl from Profile p where p.avatarUrl is not null")
    List<String> findAvatarUrls(Pageable pageable);

    /** Référentiel {@code /reference/universities} — valeurs distinctes réelles. */
    @Query("select distinct p.university from Profile p "
            + "where p.university is not null and p.university <> '' "
            + "order by p.university")
    List<String> findDistinctUniversities();

    /** Référentiel {@code /reference/faculties} — valeurs distinctes réelles. */
    @Query("select distinct p.fieldOfStudy from Profile p "
            + "where p.fieldOfStudy is not null and p.fieldOfStudy <> '' "
            + "order by p.fieldOfStudy")
    List<String> findDistinctFieldOfStudies();

    /** Référentiel {@code /reference/neighborhoods} — valeurs distinctes réelles. */
    @Query("select distinct p.neighborhood from Profile p "
            + "where p.neighborhood is not null and p.neighborhood <> '' "
            + "order by p.neighborhood")
    List<String> findDistinctNeighborhoods();

    /** Référentiel {@code /reference/interests} — centres d'intérêt distincts. */
    @Query("select distinct i from Profile p join p.interests i order by i")
    List<String> findDistinctInterests();

}

