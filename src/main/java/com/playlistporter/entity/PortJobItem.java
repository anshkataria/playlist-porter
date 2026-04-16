package com.playlistporter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="port_job_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortJobItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "port_job_id",nullable = false)
    private PortJob portJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_track_id",nullable = false)
    private Track sourceTrack;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus matchStatus;

    // what we found on the target platform
    private String matchedPlatformTrackId;
    private String matchedTitle;
    private String matchedArtist;

    // 0.0 to 1.0 — how confident we are in this match
    private Float confidenceScore;

    // AI-generated explanation of why this match was chosen
    @Column(columnDefinition = "TEXT")
    private String matchReasoning;

    private Integer position;
}
