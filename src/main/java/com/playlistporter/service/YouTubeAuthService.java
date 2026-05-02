package com.playlistporter.service;

import com.playlistporter.config.YouTubeConfig;
import com.playlistporter.entity.*;
import com.playlistporter.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeAuthService {

    private final YouTubeConfig youtubeConfig;
    private final UserRepository userRepository;
    private final PlatformConnectionRepository connectionRepository;
    private final WebClient webClient;

    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder
            .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
            .queryParam("client_id", youtubeConfig.getClientId())
            .queryParam("response_type", "code")
            .queryParam("redirect_uri", youtubeConfig.getRedirectUri())
            .queryParam("scope", youtubeConfig.getScopes() +
                " https://www.googleapis.com/auth/userinfo.email" +
                " https://www.googleapis.com/auth/userinfo.profile")
            .queryParam("access_type", "offline")
            .queryParam("state", state)
            .toUriString();
    }

    public Map<String, Object> handleCallback(String code, User existingUser) {
        // exchange code for tokens
        Map tokenResponse = webClient.post()
            .uri("https://oauth2.googleapis.com/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue("grant_type=authorization_code" +
                       "&code=" + code +
                       "&redirect_uri=" + youtubeConfig.getRedirectUri() +
                       "&client_id=" + youtubeConfig.getClientId() +
                       "&client_secret=" + youtubeConfig.getClientSecret())
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        Integer expiresIn = (Integer) tokenResponse.get("expires_in");

        // fetch Google profile
        Map profile = webClient.get()
            .uri("https://www.googleapis.com/oauth2/v2/userinfo")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        String googleUserId = (String) profile.get("id");
        String email = (String) profile.get("email");
        String displayName = (String) profile.get("name");

        // if no existing user (first login via YouTube), find or create
        User user = existingUser != null ? existingUser :
            userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                    User.builder()
                        .email(email)
                        .displayName(displayName)
                        .build()
                ));

        // save YouTube connection
        PlatformConnection connection = connectionRepository
            .findByUserAndPlatform(user, Platform.YOUTUBE_MUSIC)
            .orElse(PlatformConnection.builder()
                .user(user)
                .platform(Platform.YOUTUBE_MUSIC)
                .build());

        connection.setAccessToken(accessToken);
        connection.setRefreshToken(refreshToken);
        connection.setPlatformUserId(googleUserId);
        connection.setPlatformDisplayName(displayName);
        connection.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
        connectionRepository.save(connection);

        return Map.of("user", user, "accessToken", accessToken);
    }
}