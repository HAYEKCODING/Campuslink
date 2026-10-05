package com.campuslink.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Jeton de réinitialisation de mot de passe envoyé sous forme de lien par email.
 *
 * <p>Table : {@code password_reset_tokens}. Ne stocke <strong>jamais</strong> le
 * token en clair — uniquement son empreinte SHA-256 ({@link #getTokenHash()}).
 * Une fuite de cette table ne permet donc pas de réutiliser un lien : il
 * faudrait retrouver une préimage du hash, ce qui est infaisable pour un
 * token aléatoire de 256 bits.</p>
 *
 * <p>Distincte de {@link OtpCode} : un token de réinitialisation est un
 * secret long (32 octets aléatoires, encodé en base64url) destiné à être
 * intégré tel quel dans une URL et cliqué, alors qu'un OTP est un code court
 * pensé pour une saisie manuelle. Les deux mécanismes coexistent
 * volontairement dans l'application.</p>
 */
@Entity
@Table(
        name = "password_reset_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_password_reset_tokens_token_hash", columnNames = "token_hash"),
        indexes = {
                @Index(name = "idx_password_reset_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_password_reset_tokens_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true, callSuper = false)
public class PasswordResetToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_password_reset_tokens_user"))
    @NotNull(message = "Le token doit être rattaché à un utilisateur.")
    private User user;

    /**
     * Empreinte SHA-256 (hexadécimale, 64 caractères) du token brut envoyé au client.
     */
    @NotBlank
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ToString.Include
    @NotNull(message = "La date d'expiration est obligatoire.")
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ToString.Include
    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;

}
