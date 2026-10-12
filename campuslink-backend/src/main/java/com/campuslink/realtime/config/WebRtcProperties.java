package com.campuslink.realtime.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.webrtc")
public class WebRtcProperties {
    private String stunUrls = "stun:stun.l.google.com:19302";
    private String turnUrls = "";
    private String turnSharedSecret = "";
    private long turnCredentialTtlSeconds = 3600;
}
