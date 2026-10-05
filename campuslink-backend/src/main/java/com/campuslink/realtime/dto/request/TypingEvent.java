package com.campuslink.realtime.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Evenement STOMP entrant "utilisateur en train d'ecrire" (non persiste). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TypingEvent {
    private Long matchId;
    private boolean enTrainDecrire;
}
