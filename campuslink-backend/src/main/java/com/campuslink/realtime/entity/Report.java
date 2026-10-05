package com.campuslink.realtime.entity;

import com.campuslink.realtime.entity.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "reports",
        indexes = {
                @Index(name = "idx_report_cible", columnList = "cible_id"),
                @Index(name = "idx_report_emetteur", columnList = "emetteur_id"),
                @Index(name = "idx_report_statut", columnList = "statut")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emetteur_id", nullable = false)
    private Long emetteurId;

    @Column(name = "cible_id", nullable = false)
    private Long cibleId;

    /** Optionnel : identifiant d'une conversation/match signale, en plus du profil. */
    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "motif", nullable = false, length = 100)
    private String motif;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus statut = ReportStatus.EN_ATTENTE;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "date_traitement")
    private Instant dateTraitement;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = Instant.now();
        if (this.statut == null) {
            this.statut = ReportStatus.EN_ATTENTE;
        }
    }
}
