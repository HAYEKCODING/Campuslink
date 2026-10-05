package com.campuslink.repository;

import com.campuslink.entity.OtpCode;
import com.campuslink.entity.User;
import com.campuslink.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux données pour l'entité {@link OtpCode}.
 */
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    /**
     * Dernier code actif (non utilisé) pour un utilisateur et un type donnés —
     * c'est celui contre lequel {@code OtpService.verify(...)} compare le code soumis.
     */
    Optional<OtpCode> findFirstByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(User user, OtpType type);

    /**
     * Dernier code émis pour un utilisateur et un type donnés, quel que soit son état —
     * utilisé pour calculer le délai anti-spam avant un nouvel envoi.
     */
    Optional<OtpCode> findFirstByUserAndTypeOrderByCreatedAtDesc(User user, OtpType type);

    /**
     * Supprime les codes non utilisés d'un utilisateur/type avant l'émission d'un
     * nouveau code : un seul code actif à la fois par (utilisateur, type).
     */
    void deleteByUserAndTypeAndUsedFalse(User user, OtpType type);

    /**
     * Purge en masse les codes expirés ou déjà utilisés — appelé périodiquement par
     * {@link com.campuslink.service.impl.OtpCleanupScheduler}.
     *
     * @return le nombre de lignes supprimées.
     */
    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :now OR o.used = true")
    int deleteExpiredOrUsed(@Param("now") Instant now);

}
