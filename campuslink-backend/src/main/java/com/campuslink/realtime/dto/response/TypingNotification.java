package com.campuslink.realtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Diffuse aux autres membres de la conversation via /topic/matches/{matchId}/typing */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingNotification {
    private Long matchId;
    private Long utilisateurId;
    private boolean enTrainDecrire;
}
