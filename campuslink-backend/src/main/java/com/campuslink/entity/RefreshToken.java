package com.campuslink.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
 * Enregistrement de suivi d'un refresh token émis pour un utilisateur.
 *
 * <p>Table : {@code refresh_tokens}. Ne stocke <strong>jamais</strong> le JWT
 * lui-même — seul son identifiant technique ({@link #getId()}, embarqué comme
 * claim standard {@code jti} dans le token signé par {@link com.campuslink.security.JwtService})
 * est persisté. Une fuite de cette table ne permet donc pas de rejouer un token :
 * il faudrait encore connaître la clé de signature du serveur.</p>
 *
 * <p>Permet deux opérations impossibles avec un JWT purement stateless :</p>
 * <ul>
 *     <li>Révocation immédiate (logout, avant l'expiration naturelle du token)</li>
 *     <li>Rotation à chaque rafraîchissement (l'ancien token est marqué {@code revoked})</li>
 * </ul>
 */
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true, callSuper = false)
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_tokens_user"))
    @NotNull(message = "Le refresh token doit être rattaché à un utilisateur.")
    private User user;

    @ToString.Include
    @NotNull(message = "La date d'expiration est obligatoire.")
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ToString.Include
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

}
