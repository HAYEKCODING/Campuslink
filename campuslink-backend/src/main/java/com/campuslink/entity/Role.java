package com.campuslink.entity;

import com.campuslink.enums.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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

import java.util.HashSet;
import java.util.Set;

/**
 * Rôle applicatif attribuable à un {@link User} (RBAC).
 *
 * <p>Table : {@code roles}. Relation Many-to-Many avec {@link User},
 * portée côté propriétaire par {@code User} via la table de jointure
 * {@code user_roles}.</p>
 */
@Entity
@Table(
        name = "roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true, callSuper = false)
public class Role extends BaseEntity {

    @ToString.Include
    @NotNull(message = "Le nom du rôle est obligatoire.")
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 30)
    private RoleName name;

    @ToString.Include
    @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères.")
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Côté inverse de la relation Many-to-Many : ne pilote pas la table de
     * jointure (voir {@link User#getRoles()} pour le côté propriétaire).
     */
    @Builder.Default
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

}
