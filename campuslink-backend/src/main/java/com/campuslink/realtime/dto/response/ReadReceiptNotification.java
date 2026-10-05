package com.campuslink.realtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Diffuse a l'expediteur d'origine quand ses messages viennent d'etre lus. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptNotification {
    private Long matchId;
    private Long lecteurId;
}
