package com.campuslink.realtime.dto.response;

import com.campuslink.realtime.entity.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long id;
    private Long emetteurId;
    private Long cibleId;
    private Long matchId;
    private String motif;
    private String description;
    private ReportStatus statut;
    private Instant dateCreation;
    private Instant dateTraitement;
}
