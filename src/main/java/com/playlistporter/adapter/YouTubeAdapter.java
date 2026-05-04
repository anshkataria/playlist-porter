package com.playlistporter.adapter;

import com.playlistporter.config.YouTubeConfig;
import com.playlistporter.dto.PlaylistDTO;
import com.playlistporter.dto.TrackDTO;
import com.playlistporter.entity.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class YouTubeAdapter implements MusicPlatformAdapter {

    private final WebClient webClient;
    private final YouTubeConfig youtubeConfig;

    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";

    @Override
    public Platform getPlatform() {
        return Platform.YOUTUBE_MUSIC;
    }

    // ─── Fetch user playlists ─────────────────────────────────────────────

    @Override
    public List<PlaylistDTO> fetchUserPlaylists(String accessToken) {
        List<PlaylistDTO> allPlaylists = new ArrayList<>();
        String pageToken = null;

        do {
            String url = BASE_URL + "/playlists?part=snippet,contentDetails&mine=true&maxResults=50"
                + (pageToken != null ? "&pageToken=" + pageToken : "");

            Map response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            List<Map> items = (List<Map>) response.get("items");
            for (Map item : items) {
                allPlaylists.add(mapToPlaylistDTO(item));
            }

            pageToken = (String) response.get("nextPageToken");
        } while (pageToken != null);

        return allPlaylists;
    }

    private PlaylistDTO mapToPlaylistDTO(Map item) {
        Map snippet = (Map) item.get("snippet");
        Map contentDetails = (Map) item.get("contentDetails");

        String imageUrl = null;
        Map thumbnails = (Map) snippet.get("thumbnails");
        if (thumbnails != null) {
            Map high = (Map) thumbnails.get("high");
            if (high != null) imageUrl = (String) high.get("url");
        }

        return PlaylistDTO.builder()
            .platformPlaylistId((String) item.get("id"))
            .name((String) snippet.get("title"))
            .description((String) snippet.get("description"))
            .imageUrl(imageUrl)
            .trackCount(contentDetails != null ?
                (Integer) contentDetails.get("itemCount") : 0)
            .platform(Platform.YOUTUBE_MUSIC)
            .build();
    }

    // ─── Fetch playlist tracks ────────────────────────────────────────────

    @Override
    public List<TrackDTO> fetchPlaylistTracks(String playlistId, String accessToken) {
        List<TrackDTO> allTracks = new ArrayList<>();
        String pageToken = null;

        do {
            String url = BASE_URL + "/playlistItems?part=snippet,contentDetails"
                + "&playlistId=" + playlistId
                + "&maxResults=50"
                + (pageToken != null ? "&pageToken=" + pageToken : "");

            Map response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            List<Map> items = (List<Map>) response.get("items");
            for (Map item : items) {
                TrackDTO track = mapToTrackDTO(item);
                if (track != null) allTracks.add(track);
            }

            pageToken = (String) response.get("nextPageToken");
        } while (pageToken != null);

        return allTracks;
    }

    private TrackDTO mapToTrackDTO(Map item) {
        Map snippet = (Map) item.get("snippet");
        if (snippet == null) return null;

        Map resourceId = (Map) snippet.get("resourceId");
        if (resourceId == null) return null;

        String videoId = (String) resourceId.get("videoId");
        String title = (String) snippet.get("title");

        // YouTube doesn't separate title/artist cleanly
        // common format is "Artist - Title" — we parse it
        String artist = "";
        if (title != null && title.contains(" - ")) {
            String[] parts = title.split(" - ", 2);
            artist = parts[0].trim();
            title = parts[1].trim();
        }

        String imageUrl = null;
        Map thumbnails = (Map) snippet.get("thumbnails");
        if (thumbnails != null) {
            Map high = (Map) thumbnails.get("high");
            if (high != null) imageUrl = (String) high.get("url");
        }

        return TrackDTO.builder()
            .platformTrackId(videoId)
            .title(title)
            .artist(artist)
            .imageUrl(imageUrl)
            .build();
    }

    // ─── Search for a track ───────────────────────────────────────────────

    @Override
    public List<TrackDTO> searchTrack(String title, String artist, String accessToken) {
        String query = artist + " " + title + " official audio";

        Map response = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("www.googleapis.com")
                .path("/youtube/v3/search")
                .queryParam("part", "snippet")
                .queryParam("q", query)
                .queryParam("type", "video")
                .queryParam("videoCategoryId", "10") // music category
                .queryParam("maxResults", "5")
                .build())
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        List<Map> items = (List<Map>) response.get("items");
        return items.stream()
            .map(item -> {
                Map snippet = (Map) item.get("snippet");
                Map id = (Map) item.get("id");
                return TrackDTO.builder()
                    .platformTrackId((String) id.get("videoId"))
                    .title((String) snippet.get("title"))
                    .artist((String) snippet.get("channelTitle"))
                    .build();
            })
            .collect(Collectors.toList());
    }

    // ─── Create playlist ──────────────────────────────────────────────────

    @Override
    public String createPlaylist(String name, String description, String accessToken) {
        Map response = webClient.post()
            .uri(BASE_URL + "/playlists?part=snippet,status")
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .bodyValue(Map.of(
                "snippet", Map.of(
                    "title", name,
                    "description", description != null ? description : ""
                ),
                "status", Map.of("privacyStatus", "private")
            ))
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        return (String) response.get("id");
    }

    // ─── Add tracks to playlist ───────────────────────────────────────────

    @Override
    public void addTracksToPlaylist(String playlistId, List<String> trackIds, String accessToken) {
        for (int i = 0; i < trackIds.size(); i++) {
            String videoId = trackIds.get(i);
            try {
                webClient.post()
                    .uri(BASE_URL + "/playlistItems?part=snippet")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                        "snippet", Map.of(
                            "playlistId", playlistId,
                            "position", i,
                            "resourceId", Map.of(
                                "kind", "youtube#video",
                                "videoId", videoId
                            )
                        )
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            } catch (Exception e) {
                log.warn("Failed to add track {} to playlist: {}", videoId, e.getMessage());
            }
        }
    }

    // ─── Refresh token ────────────────────────────────────────────────────

    @Override
    public String refreshAccessToken(String refreshToken) {
        Map response = webClient.post()
            .uri("https://oauth2.googleapis.com/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue("grant_type=refresh_token"
                + "&refresh_token=" + refreshToken
                + "&client_id=" + youtubeConfig.getClientId()
                + "&client_secret=" + youtubeConfig.getClientSecret())
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        return (String) response.get("access_token");
    }
}