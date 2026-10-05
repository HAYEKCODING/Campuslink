package com.campuslink.realtime.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Represente un "Like" emis par un utilisateur vers un autre.
 * NB: aucune relation JPA vers User/Profile (proprietes du Dev B) :
 * on ne stocke que les identifiants pour rester decouple du module Backend Core.
 */
@Entity
@Table(
        name = "likes",
        uniqueConstraints = @UniqueConstraint(name = "uk_like_emetteur_cible", columnNames = {"emetteur_id", "cible_id"}),
        indexes = {
                @Index(name = "idx_like_emetteur", columnList = "emetteur_id"),
                @Index(name = "idx_like_cible", columnList = "cible_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emetteur_id", nullable = false)
    private Long emetteurId;

    @Column(name = "cible_id", nullable = false)
    private Long cibleId;

    @Column(name = "date_action", nullable = false, updatable = false)
    private Instant dateAction;

    @PrePersist
    protected void onCreate() {
        this.dateAction = Instant.now();
    }
}
