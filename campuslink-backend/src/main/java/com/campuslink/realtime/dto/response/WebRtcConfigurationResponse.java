package com.campuslink.realtime.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WebRtcConfigurationResponse {
    private List<IceServer> iceServers;

    @Getter
    @Builder
    public static class IceServer {
        private List<String> urls;
        private String username;
        private String credential;
    }
}
