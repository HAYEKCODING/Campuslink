package com.campuslink.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriétés de configuration Cloudinary, mappées depuis {@code application.yml}
 * (préfixe {@code application.cloudinary}).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.cloudinary")
public class CloudinaryProperties {

    private String cloudName;
    private String apiKey;
    private String apiSecret;

}
