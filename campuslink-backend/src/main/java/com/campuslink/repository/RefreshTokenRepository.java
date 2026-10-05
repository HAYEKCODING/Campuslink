package com.campuslink.repository;

import com.campuslink.entity.RefreshToken;
import com.campuslink.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux données pour l'entité {@link RefreshToken}.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Recherche un refresh token actif (non révoqué) par son identifiant
     * (correspond au claim {@code jti} du JWT présenté par le client).
     */
    Optional<RefreshToken> findByIdAndRevokedFalse(UUID id);

    /**
     * Révoque en masse tous les refresh tokens actifs d'un utilisateur —
     * utilisé après une réinitialisation de mot de passe pour invalider
     * toute session déjà ouverte ailleurs.
     *
     * @return le nombre de tokens révoqués.
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true "
            + "WHERE r.user = :user AND r.revoked = false AND r.expiresAt > :now")
    int revokeAllActiveForUser(@Param("user") User user, @Param("now") Instant now);

}

