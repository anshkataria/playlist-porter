package com.playlistporter.controller;

import com.playlistporter.service.*;
import com.playlistporter.util.JwtUtil;
import com.playlistporter.config.SpotifyConfig;
import com.playlistporter.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SpotifyAuthService spotifyAuthService;
    private final YouTubeAuthService youtubeAuthService;
    private final JwtUtil jwtUtil;
private final SpotifyConfig spotifyConfig;
    // --- Spotify ---

    @GetMapping("/spotify")
    public void spotifyLogin(HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString();
        response.sendRedirect(spotifyAuthService.buildAuthorizationUrl(state));
    }

    @GetMapping("/spotify/callback")
    public ResponseEntity<?> spotifyCallback(@RequestParam String code) {
        Map<String, Object> result = spotifyAuthService.handleCallback(code);
        User user = (User) result.get("user");
        String jwt = jwtUtil.generateToken(user.getId());
        return ResponseEntity.ok(Map.of(
            "token", jwt,
            "user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName()
            )
        ));
    }

    // --- YouTube ---

    @GetMapping("/youtube")
    public void youtubeLogin(HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString();
        response.sendRedirect(youtubeAuthService.buildAuthorizationUrl(state));
    }
@GetMapping("/debug")
public ResponseEntity<?> debug() {
    return ResponseEntity.ok(Map.of(
        "clientId", spotifyConfig.getClientId() == null ? "NULL" : spotifyConfig.getClientId(),
        "redirectUri", spotifyConfig.getRedirectUri() == null ? "NULL" : spotifyConfig.getRedirectUri()
    ));
}
    @GetMapping("/youtube/callback")
    public ResponseEntity<?> youtubeCallback(@RequestParam String code) {
        Map<String, Object> result = youtubeAuthService.handleCallback(code, null);
        User user = (User) result.get("user");
        String jwt = jwtUtil.generateToken(user.getId());
        return ResponseEntity.ok(Map.of(
            "token", jwt,
            "user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName()
            )
        ));
    }
}