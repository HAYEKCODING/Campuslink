package com.campuslink.realtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long nombreMatchsActifs;
    private long nombreMessagesEnvoyes;
    private long nombreSignalementsOuverts;
    private long nombreUtilisateursSuspendusOuBannis;
}
