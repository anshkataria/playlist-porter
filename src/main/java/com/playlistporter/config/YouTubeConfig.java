package com.playlistporter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix="youtube")
@Getter
@Setter
public class YouTubeConfig {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scopes;
}
