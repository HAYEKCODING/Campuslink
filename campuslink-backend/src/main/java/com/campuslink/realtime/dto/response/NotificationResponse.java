package com.campuslink.realtime.dto.response;

import com.campuslink.realtime.entity.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String contenu;
    private Long referenceId;
    private boolean lu;
    private Instant dateCreation;
}
