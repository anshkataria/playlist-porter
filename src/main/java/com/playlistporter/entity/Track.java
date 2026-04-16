package com.playlistporter.entity;

import com.pgvector.PGvector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="tracks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="playlist_id",nullable = false)
    private Playlist playlist;

    private Integer position;

    @Column(nullable=false)
    private String title;

    @Column(nullable = false)
    private String artist;

    private String album;
    private String platformTrackId;
    private String isrc;

    // audio features from Spotify (used for vibe matching)
    private Float tempo;
    private Float energy;
    private Float danceability;
    private Float valence;      // musical positiveness
    private Float acousticness;
    private Integer musicalKey;
    private Integer mode;       // 0 = minor, 1 = major

    // the embedding vector (1536 dims for OpenAI text-embedding-3-small)
    @Column(columnDefinition = "vector(1536)")
    private PGvector embedding;
    
}
