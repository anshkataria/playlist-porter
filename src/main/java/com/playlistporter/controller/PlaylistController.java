package com.playlistporter.controller;

import com.playlistporter.dto.PlaylistDTO;
import com.playlistporter.entity.*;
import com.playlistporter.repository.UserRepository;
import com.playlistporter.service.PlaylistService;
import com.playlistporter.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // GET /api/playlists?platform=SPOTIFY
    @GetMapping
    public ResponseEntity<List<PlaylistDTO>> getUserPlaylists(
        @RequestHeader("Authorization") String authHeader,
        @RequestParam Platform platform
    ) {
        User user = getUserFromToken(authHeader);
        List<PlaylistDTO> playlists = playlistService.getUserPlaylists(user, platform);
        return ResponseEntity.ok(playlists);
    }

    // GET /api/playlists/{playlistId}/tracks?platform=SPOTIFY
    @GetMapping("/{playlistId}/tracks")
    public ResponseEntity<PlaylistDTO> getPlaylistTracks(
        @RequestHeader("Authorization") String authHeader,
        @PathVariable String playlistId,
        @RequestParam Platform platform
    ) {
        User user = getUserFromToken(authHeader);
        PlaylistDTO playlist = playlistService
            .getPlaylistWithTracks(user, platform, playlistId);
        return ResponseEntity.ok(playlist);
    }

    private User getUserFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.extractUserId(token);
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}