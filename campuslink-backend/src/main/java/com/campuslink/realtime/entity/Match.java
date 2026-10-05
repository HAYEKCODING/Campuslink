package com.campuslink.realtime.entity;

import com.campuslink.realtime.entity.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "matches",
        uniqueConstraints = @UniqueConstraint(name = "uk_match_utilisateurs", columnNames = {"utilisateur1_id", "utilisateur2_id"}),
        indexes = {
                @Index(name = "idx_match_utilisateur1", columnList = "utilisateur1_id"),
                @Index(name = "idx_match_utilisateur2", columnList = "utilisateur2_id"),
                @Index(name = "idx_match_statut", columnList = "statut")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Convention : utilisateur1Id < utilisateur2Id pour eviter les doublons (a,b)/(b,a). */
    @Column(name = "utilisateur1_id", nullable = false)
    private Long utilisateur1Id;

    @Column(name = "utilisateur2_id", nullable = false)
    private Long utilisateur2Id;

    @Column(name = "date_match", nullable = false, updatable = false)
    private Instant dateMatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private MatchStatus statut = MatchStatus.ACTIF;

    @Column(name = "date_rupture")
    private Instant dateRupture;

    @PrePersist
    protected void onCreate() {
        this.dateMatch = Instant.now();
        if (this.statut == null) {
            this.statut = MatchStatus.ACTIF;
        }
    }

    public boolean concerne(Long userId) {
        return utilisateur1Id.equals(userId) || utilisateur2Id.equals(userId);
    }

    public Long autreUtilisateur(Long userId) {
        return utilisateur1Id.equals(userId) ? utilisateur2Id : utilisateur1Id;
    }
}
