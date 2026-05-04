package com.playlistporter.dto;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class TrackDTO {
    private String platformTrackId;
    private String title;
    private String artist;
    private String album;
    private Integer durationMs;
    private String isrc;            // universal identifier
    private String imageUrl;

    // audio features (Spotify provides these, useful for vibe matching)
    private Float tempo;
    private Float energy;
    private Float danceability;
    private Float valence;
    private Float acousticness;
    private Integer musicalKey;
    private Integer mode;
}