package com.campuslink.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserPreferencesResponse {
    private boolean profileVisible;
    private boolean notificationLikes;
    private boolean notificationMatches;
    private boolean notificationMessages;
    private boolean notificationReports;
}
