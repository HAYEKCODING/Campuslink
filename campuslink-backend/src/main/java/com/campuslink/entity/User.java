package com.campuslink.entity;

import com.campuslink.enums.AccountStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compte utilisateur — entité centrale du système d'authentification de CampusLink.
 *
 * <p>Table : {@code users}. Porte l'identité et les informations de sécurité
 * (email, mot de passe, statut). Les informations personnelles sont déportées
 * dans {@link Profile} (relation One-to-One), les codes de vérification dans
 * {@link OtpCode} (relation One-to-Many), et les autorisations dans
 * {@link Role} (relation Many-to-Many, table de jointure {@code user_roles}).</p>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_status", columnList = "status")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true, callSuper = false)
public class User extends BaseEntity {

    @ToString.Include
    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Le format de l'email est invalide.")
    @Size(max = 180, message = "L'email ne doit pas dépasser 180 caractères.")
    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    /**
     * Hash du mot de passe (BCrypt). Ne jamais exposer dans un {@code toString()} ou un DTO.
     */
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @ToString.Include
    @NotNull(message = "Le statut du compte est obligatoire.")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private AccountStatus status = AccountStatus.PENDING_VERIFICATION;

    @ToString.Include
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * Identifiant numérique auto-généré par la base (colonne {@code legacy_id},
     * BIGSERIAL), en plus de l'UUID {@code id}. Sert uniquement de pont vers le
     * module {@code com.campuslink.realtime} (DevC), dont les tables (likes,
     * matches, messages...) référencent les utilisateurs via un BIGINT plutôt
     * qu'un UUID. Généré et alimenté par la base, jamais par l'application.
     */
    @Column(name = "legacy_id", insertable = false, updatable = false)
    private Long legacyId;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Profile profile;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private List<OtpCode> otpCodes = new ArrayList<>();

    /**
     * Côté propriétaire de la relation Many-to-Many avec {@link Role}.
     * Table de jointure : {@code user_roles(user_id, role_id)}.
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_roles_user")),
            inverseJoinColumns = @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_user_roles_role")),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_roles_user_role", columnNames = {"user_id", "role_id"})
    )
    private Set<Role> roles = new HashSet<>();

    // ===================== Méthodes utilitaires de cohérence bidirectionnelle =====================

    /**
     * Associe le profil à cet utilisateur en maintenant la cohérence des deux côtés de la relation.
     */
    public void setProfile(Profile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }

    /**
     * Ajoute un code OTP à cet utilisateur en maintenant la cohérence bidirectionnelle.
     */
    public void addOtpCode(OtpCode otpCode) {
        this.otpCodes.add(otpCode);
        otpCode.setUser(this);
    }

    /**
     * Attribue un rôle à cet utilisateur en maintenant la cohérence bidirectionnelle.
     */
    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }

    /**
     * Retire un rôle de cet utilisateur en maintenant la cohérence bidirectionnelle.
     */
    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getUsers().remove(this);
    }

}
