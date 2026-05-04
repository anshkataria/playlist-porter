package com.playlistporter.dto;

import com.playlistporter.entity.Platform;
import lombok.*;
import java.util.List;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PlaylistDTO {
    private String platformPlaylistId;
    private String name;
    private String description;
    private String imageUrl;
    private Integer trackCount;
    private Platform platform;
    private List<TrackDTO> tracks;
}