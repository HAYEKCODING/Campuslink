package com.campuslink.realtime.entity;

import com.campuslink.realtime.entity.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_message_match", columnList = "match_id"),
                @Index(name = "idx_message_expediteur", columnList = "expediteur_id"),
                @Index(name = "idx_message_date_envoi", columnList = "date_envoi")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "expediteur_id", nullable = false)
    private Long expediteurId;

    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "date_envoi", nullable = false, updatable = false)
    private Instant dateEnvoi;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_lecture", nullable = false, length = 20)
    @Builder.Default
    private MessageStatus statutLecture = MessageStatus.ENVOYE;

    @Column(name = "date_lecture")
    private Instant dateLecture;

    @PrePersist
    protected void onCreate() {
        this.dateEnvoi = Instant.now();
        if (this.statutLecture == null) {
            this.statutLecture = MessageStatus.ENVOYE;
        }
    }
}
