package com.playlistporter.adapter;

import com.playlistporter.config.SpotifyConfig;
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
public class SpotifyAdapter implements MusicPlatformAdapter {

    private final WebClient webClient;
    private final SpotifyConfig spotifyConfig;

    private static final String BASE_URL = "https://api.spotify.com/v1";

    @Override
    public Platform getPlatform() {
        return Platform.SPOTIFY;
    }

    // ─── Fetch all playlists ───────────────────────────────────────────────

    @Override
    public List<PlaylistDTO> fetchUserPlaylists(String accessToken) {
        List<PlaylistDTO> allPlaylists = new ArrayList<>();
        String url = BASE_URL + "/me/playlists?limit=50";

        // Spotify paginates results — keep fetching until there's no next page
        while (url != null) {
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

            url = (String) response.get("next"); // null when last page
        }

        return allPlaylists;
    }

    private PlaylistDTO mapToPlaylistDTO(Map item) {
        String imageUrl = null;
        List<Map> images = (List<Map>) item.get("images");
        if (images != null && !images.isEmpty()) {
            imageUrl = (String) images.get(0).get("url");
        }

        Map tracksInfo = (Map) item.get("tracks");

        return PlaylistDTO.builder()
            .platformPlaylistId((String) item.get("id"))
            .name((String) item.get("name"))
            .description((String) item.get("description"))
            .imageUrl(imageUrl)
            .trackCount(tracksInfo != null ? (Integer) tracksInfo.get("total") : 0)
            .platform(Platform.SPOTIFY)
            .build();
    }

    // ─── Fetch tracks in a playlist ───────────────────────────────────────

    @Override
    public List<TrackDTO> fetchPlaylistTracks(String playlistId, String accessToken) {
        List<TrackDTO> allTracks = new ArrayList<>();
        String url = BASE_URL + "/playlists/" + playlistId + "/tracks?limit=100";

        while (url != null) {
            Map response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            List<Map> items = (List<Map>) response.get("items");
            List<TrackDTO> tracks = items.stream()
                .filter(item -> item.get("track") != null)
                .map(item -> mapToTrackDTO((Map) item.get("track")))
                .collect(Collectors.toList());

            allTracks.addAll(tracks);
            url = (String) response.get("next");
        }

        // fetch audio features for all tracks in one batch call
        enrichWithAudioFeatures(allTracks, accessToken);

        return allTracks;
    }

    private TrackDTO mapToTrackDTO(Map track) {
        // artist can be multiple — join them
        List<Map> artists = (List<Map>) track.get("artists");
        String artist = artists.stream()
            .map(a -> (String) a.get("name"))
            .collect(Collectors.joining(", "));

        Map album = (Map) track.get("album");
        String albumName = album != null ? (String) album.get("name") : null;

        // ISRC lives inside external_ids
        Map externalIds = (Map) track.get("external_ids");
        String isrc = externalIds != null ? (String) externalIds.get("isrc") : null;

        return TrackDTO.builder()
            .platformTrackId((String) track.get("id"))
            .title((String) track.get("name"))
            .artist(artist)
            .album(albumName)
            .durationMs((Integer) track.get("duration_ms"))
            .isrc(isrc)
            .build();
    }

    // Spotify lets you fetch audio features for up to 100 tracks at once
    private void enrichWithAudioFeatures(List<TrackDTO> tracks, String accessToken) {
        if (tracks.isEmpty()) return;

        String ids = tracks.stream()
            .map(TrackDTO::getPlatformTrackId)
            .collect(Collectors.joining(","));

        try {
            Map response = webClient.get()
                .uri(BASE_URL + "/audio-features?ids=" + ids)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            List<Map> features = (List<Map>) response.get("audio_features");

            for (int i = 0; i < tracks.size() && i < features.size(); i++) {
                Map f = features.get(i);
                if (f == null) continue;
                TrackDTO track = tracks.get(i);
                track.setTempo(toFloat(f.get("tempo")));
                track.setEnergy(toFloat(f.get("energy")));
                track.setDanceability(toFloat(f.get("danceability")));
                track.setValence(toFloat(f.get("valence")));
                track.setAcousticness(toFloat(f.get("acousticness")));
                track.setMusicalKey(f.get("key") != null ? (Integer) f.get("key") : null);
                track.setMode(f.get("mode") != null ? (Integer) f.get("mode") : null);
            }
        } catch (Exception e) {
            log.warn("Could not fetch audio features: {}", e.getMessage());
        }
    }

    private Float toFloat(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return ((Double) value).floatValue();
        if (value instanceof Float) return (Float) value;
        return null;
    }

    // ─── Search for a track ───────────────────────────────────────────────

    @Override
    public List<TrackDTO> searchTrack(String title, String artist, String accessToken) {
        String query = "track:" + title + " artist:" + artist;

        Map response = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("api.spotify.com")
                .path("/v1/search")
                .queryParam("q", query)
                .queryParam("type", "track")
                .queryParam("limit", "5")
                .build())
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        Map tracks = (Map) response.get("tracks");
        List<Map> items = (List<Map>) tracks.get("items");

        return items.stream()
            .map(this::mapToTrackDTO)
            .collect(Collectors.toList());
    }

    // ─── Create playlist ──────────────────────────────────────────────────

    @Override
    public String createPlaylist(String name, String description, String accessToken) {
        // first get the user's spotify id
        Map profile = webClient.get()
            .uri(BASE_URL + "/me")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        String spotifyUserId = (String) profile.get("id");

        Map response = webClient.post()
            .uri(BASE_URL + "/users/" + spotifyUserId + "/playlists")
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .bodyValue(Map.of(
                "name", name,
                "description", description != null ? description : "",
                "public", false
            ))
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        return (String) response.get("id");
    }

    // ─── Add tracks to playlist ───────────────────────────────────────────

    @Override
    public void addTracksToPlaylist(String playlistId, List<String> trackIds, String accessToken) {
        // Spotify accepts max 100 tracks per request — batch if needed
        List<List<String>> batches = partition(trackIds, 100);

        for (List<String> batch : batches) {
            List<String> uris = batch.stream()
                .map(id -> "spotify:track:" + id)
                .collect(Collectors.toList());

            webClient.post()
                .uri(BASE_URL + "/playlists/" + playlistId + "/tracks")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of("uris", uris))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        }
    }

    // ─── Refresh token ────────────────────────────────────────────────────

    @Override
    public String refreshAccessToken(String refreshToken) {
        Map response = webClient.post()
            .uri("https://accounts.spotify.com/api/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", "Basic " + encodeCredentials())
            .bodyValue("grant_type=refresh_token&refresh_token=" + refreshToken)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        return (String) response.get("access_token");
    }

    private String encodeCredentials() {
        String creds = spotifyConfig.getClientId() + ":" + spotifyConfig.getClientSecret();
        return Base64.getEncoder().encodeToString(creds.getBytes());
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}