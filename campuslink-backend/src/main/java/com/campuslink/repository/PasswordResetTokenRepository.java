package com.campuslink.repository;

import com.campuslink.entity.PasswordResetToken;
import com.campuslink.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux données pour l'entité {@link PasswordResetToken}.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Recherche un token actif (non utilisé) par son empreinte SHA-256 —
     * point d'entrée utilisé par {@code PasswordResetServiceImpl.resetPassword}.
     */
    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);

    /**
     * Dernier token émis pour un utilisateur, quel que soit son état — utilisé
     * pour calculer le délai anti-spam avant une nouvelle demande.
     */
    Optional<PasswordResetToken> findFirstByUserOrderByCreatedAtDesc(User user);

    /**
     * Invalide les tokens non utilisés d'un utilisateur avant l'émission d'un
     * nouveau : un seul lien actif à la fois par utilisateur.
     */
    void deleteByUserAndUsedFalse(User user);

    /**
     * Purge en masse les tokens expirés ou déjà utilisés — appelé
     * périodiquement par {@link com.campuslink.service.impl.PasswordResetTokenCleanupScheduler}.
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now OR t.used = true")
    int deleteExpiredOrUsed(@Param("now") Instant now);

}
