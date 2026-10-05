package com.campuslink.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Classe mère de toutes les entités JPA de l'application.
 *
 * <p>Fournit un identifiant technique de type {@link UUID} ainsi que
 * les champs d'audit temporel {@code createdAt} / {@code updatedAt},
 * renseignés automatiquement grâce à {@link AuditingEntityListener}.</p>
 *
 * <p>{@code equals()} et {@code hashCode()} sont volontairement écrits à la
 * main (et non générés par Lombok) : ils se basent uniquement sur
 * l'identifiant technique, conformément aux bonnes pratiques JPA. Générer
 * ces méthodes via Lombok sur des entités possédant des relations
 * bidirectionnelles (via {@code @Data} ou {@code @EqualsAndHashCode})
 * expose à des boucles infinies et à des comparaisons instables.</p>
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity other)) {
            return false;
        }
        // Deux entités transitoires (id == null) ne sont jamais égales entre elles.
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

}
