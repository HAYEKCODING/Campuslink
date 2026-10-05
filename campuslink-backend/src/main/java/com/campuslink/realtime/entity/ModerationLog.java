package com.campuslink.realtime.entity;

import com.campuslink.realtime.entity.enums.ModerationActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Journal des actions de moderation (avertissement, suspension, bannissement, debannissement).
 * Ne modifie jamais l'entite User elle-meme (propriete du Dev B) : la mise a jour reelle du
 * statut utilisateur passe par le port UserModerationPort (voir package integration).
 */
@Entity
@Table(
        name = "moderation_logs",
        indexes = {
                @Index(name = "idx_moderationlog_cible", columnList = "utilisateur_cible_id"),
                @Index(name = "idx_moderationlog_moderateur", columnList = "moderateur_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "utilisateur_cible_id", nullable = false)
    private Long utilisateurCibleId;

    @Column(name = "moderateur_id", nullable = false)
    private Long moderateurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private ModerationActionType action;

    @Column(name = "motif", length = 500)
    private String motif;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "date_action", nullable = false, updatable = false)
    private Instant dateAction;

    @PrePersist
    protected void onCreate() {
        this.dateAction = Instant.now();
    }
}
