package com.campuslink.entity;

import com.campuslink.enums.OtpType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
 * Code à usage unique (OTP) rattaché à un utilisateur : vérification d'email,
 * réinitialisation de mot de passe, vérification de téléphone, double authentification.
 *
 * <p>Table : {@code otp_codes}. Relation Many-to-One vers {@link User}
 * (un utilisateur peut posséder plusieurs codes au fil du temps).</p>
 */
@Entity
@Table(
        name = "otp_codes",
        indexes = {
                @Index(name = "idx_otp_codes_user_id", columnList = "user_id"),
                @Index(name = "idx_otp_codes_user_type_used", columnList = "user_id, type, used"),
                @Index(name = "idx_otp_codes_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true, callSuper = false)
public class OtpCode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_otp_codes_user"))
    @NotNull(message = "Le code OTP doit être rattaché à un utilisateur.")
    private User user;

    @ToString.Include
    @NotBlank(message = "Le code est obligatoire.")
    @Pattern(regexp = "^[0-9]{4,8}$", message = "Le code OTP doit être numérique (4 à 8 chiffres).")
    @Column(name = "code", nullable = false, length = 8)
    private String code;

    @ToString.Include
    @NotNull(message = "Le type de code OTP est obligatoire.")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private OtpType type;

    @ToString.Include
    @NotNull(message = "La date d'expiration est obligatoire.")
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ToString.Include
    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;

    @Min(value = 0, message = "Le nombre de tentatives ne peut pas être négatif.")
    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private int attempts = 0;

    /**
     * Indique si le code a dépassé sa date d'expiration.
     * Simple lecture d'état — ne constitue pas une logique métier de validation.
     */
    @Transient
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

}
