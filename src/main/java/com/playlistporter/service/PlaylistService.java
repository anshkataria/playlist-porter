package com.playlistporter.service;

import com.playlistporter.adapter.AdapterRegistry;
import com.playlistporter.dto.PlaylistDTO;
import com.playlistporter.entity.*;
import com.playlistporter.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final AdapterRegistry adapterRegistry;
    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;
    private final PlatformConnectionRepository connectionRepository;

    public List<PlaylistDTO> getUserPlaylists(User user, Platform platform) {
        // get their access token for this platform
        PlatformConnection connection = connectionRepository
            .findByUserAndPlatform(user, platform)
            .orElseThrow(() -> new RuntimeException(
                "No " + platform + " connection found for user"
            ));

        // refresh token if expired
        String accessToken = getValidToken(connection);

        // fetch from platform
        List<PlaylistDTO> playlists = adapterRegistry
            .getAdapter(platform)
            .fetchUserPlaylists(accessToken);

        // save to DB so we have a record
        savePlaylists(playlists, user);

        return playlists;
    }

    public PlaylistDTO getPlaylistWithTracks(User user, Platform platform, String playlistId) {
        PlatformConnection connection = connectionRepository
            .findByUserAndPlatform(user, platform)
            .orElseThrow(() -> new RuntimeException("No connection found"));

        String accessToken = getValidToken(connection);

        // fetch playlist tracks from platform
        List<com.playlistporter.dto.TrackDTO> tracks = adapterRegistry
            .getAdapter(platform)
            .fetchPlaylistTracks(playlistId, accessToken);

        return PlaylistDTO.builder()
            .platformPlaylistId(playlistId)
            .platform(platform)
            .tracks(tracks)
            .build();
    }

    private String getValidToken(PlatformConnection connection) {
        // if token expires in less than 5 mins, refresh it
        if (connection.getTokenExpiresAt() != null &&
            connection.getTokenExpiresAt().minusMinutes(5)
                .isBefore(java.time.LocalDateTime.now())) {

            log.info("Refreshing token for platform {}", connection.getPlatform());
            String newToken = adapterRegistry
                .getAdapter(connection.getPlatform())
                .refreshAccessToken(connection.getRefreshToken());

            connection.setAccessToken(newToken);
            connection.setTokenExpiresAt(
                java.time.LocalDateTime.now().plusHours(1)
            );
            connectionRepository.save(connection);
            return newToken;
        }
        return connection.getAccessToken();
    }

    private void savePlaylists(List<PlaylistDTO> playlists, User user) {
        playlists.forEach(dto -> {
            boolean exists = playlistRepository
                .findByUser(user)
                .stream()
                .anyMatch(p -> p.getPlatformPlaylistId()
                    .equals(dto.getPlatformPlaylistId()));

            if (!exists) {
                playlistRepository.save(Playlist.builder()
                    .user(user)
                    .platform(dto.getPlatform())
                    .platformPlaylistId(dto.getPlatformPlaylistId())
                    .name(dto.getName())
                    .description(dto.getDescription())
                    .imageUrl(dto.getImageUrl())
                    .trackCount(dto.getTrackCount())
                    .build());
            }
        });
    }
}