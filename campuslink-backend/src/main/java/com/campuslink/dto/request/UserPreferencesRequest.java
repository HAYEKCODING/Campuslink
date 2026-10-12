package com.campuslink.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserPreferencesRequest {
    private Boolean profileVisible;
    private Boolean notificationLikes;
    private Boolean notificationMatches;
    private Boolean notificationMessages;
    private Boolean notificationReports;
}
