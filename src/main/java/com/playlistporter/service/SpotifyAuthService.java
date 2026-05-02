package com.playlistporter.service;

import com.playlistporter.config.SpotifyConfig;
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
public class SpotifyAuthService {

    private final SpotifyConfig spotifyConfig;
    private final UserRepository userRepository;
    private final PlatformConnectionRepository connectionRepository;
    private final WebClient webClient;

    // Step 1: build the URL we redirect the user to
    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder
            .fromUriString("https://accounts.spotify.com/authorize")
            .queryParam("client_id", spotifyConfig.getClientId())
            .queryParam("response_type", "code")
            .queryParam("redirect_uri", spotifyConfig.getRedirectUri())
            .queryParam("scope", spotifyConfig.getScopes())
            .queryParam("state", state)
            .toUriString();
    }

    // Step 2: Spotify redirects back with a code — exchange it for tokens
    public Map<String, Object> handleCallback(String code) {
        // exchange code for access + refresh tokens
        Map tokenResponse = webClient.post()
            .uri("https://accounts.spotify.com/api/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", "Basic " + encodeCredentials())
            .bodyValue("grant_type=authorization_code" +
                       "&code=" + code +
                       "&redirect_uri=" + spotifyConfig.getRedirectUri())
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        Integer expiresIn = (Integer) tokenResponse.get("expires_in");

        // fetch their Spotify profile
        Map profile = webClient.get()
            .uri("https://api.spotify.com/v1/me")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        String spotifyUserId = (String) profile.get("id");
        String email = (String) profile.get("email");
        String displayName = (String) profile.get("display_name");

        // find or create user in our DB
        User user = userRepository.findByEmail(email)
            .orElseGet(() -> userRepository.save(
                User.builder()
                    .email(email)
                    .displayName(displayName)
                    .build()
            ));

        // save or update their Spotify connection
        PlatformConnection connection = connectionRepository
            .findByUserAndPlatform(user, Platform.SPOTIFY)
            .orElse(PlatformConnection.builder()
                .user(user)
                .platform(Platform.SPOTIFY)
                .build());

        connection.setAccessToken(accessToken);
        connection.setRefreshToken(refreshToken);
        connection.setPlatformUserId(spotifyUserId);
        connection.setPlatformDisplayName(displayName);
        connection.setTokenExpiresAt(
            LocalDateTime.now().plusSeconds(expiresIn)
        );
        connectionRepository.save(connection);

        return Map.of("user", user, "accessToken", accessToken);
    }

    private String encodeCredentials() {
        String credentials = spotifyConfig.getClientId() + ":" + spotifyConfig.getClientSecret();
        return java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }
}