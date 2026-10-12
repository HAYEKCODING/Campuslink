package com.campuslink.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.chat.media")
public class ChatMediaProperties {
    private long maxFileSizeBytes = 52_428_800;
}
