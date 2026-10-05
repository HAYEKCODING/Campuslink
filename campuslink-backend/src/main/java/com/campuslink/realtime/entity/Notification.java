package com.campuslink.realtime.entity;

import com.campuslink.realtime.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_user", columnList = "user_id"),
                @Index(name = "idx_notification_lu", columnList = "lu")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    @Column(name = "contenu", nullable = false, length = 500)
    private String contenu;

    /** Identifiant de la ressource liee (matchId, messageId, likeId...) pour permettre la navigation cote frontend. */
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "lu", nullable = false)
    @Builder.Default
    private boolean lu = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = Instant.now();
    }
}
