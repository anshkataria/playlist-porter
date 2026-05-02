package com.playlistporter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "spotify")
@Getter
@Setter
public class SpotifyConfig {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scopes;
}
