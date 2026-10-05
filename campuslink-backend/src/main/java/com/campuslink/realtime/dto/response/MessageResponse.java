package com.campuslink.realtime.dto.response;

import com.campuslink.realtime.entity.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long matchId;
    private Long expediteurId;
    private String contenu;
    private Instant dateEnvoi;
    private MessageStatus statutLecture;
}
