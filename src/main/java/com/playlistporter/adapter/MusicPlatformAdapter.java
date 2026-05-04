package com.playlistporter.adapter;

import com.playlistporter.dto.PlaylistDTO;
import com.playlistporter.dto.TrackDTO;
import com.playlistporter.entity.Platform;
import java.util.List;

public interface MusicPlatformAdapter {

    // which platform this adapter handles
    Platform getPlatform();

    // fetch all playlists for the logged-in user
    List<PlaylistDTO> fetchUserPlaylists(String accessToken);

    // fetch all tracks in a specific playlist
    List<TrackDTO> fetchPlaylistTracks(String playlistId, String accessToken);

    // search for a track by title + artist (used during matching)
    List<TrackDTO> searchTrack(String title, String artist, String accessToken);

    // create a new playlist and return its platform id
    String createPlaylist(String name, String description, String accessToken);

    // add track ids to an existing playlist
    void addTracksToPlaylist(String playlistId, List<String> trackIds, String accessToken);

    // refresh an expired access token
    String refreshAccessToken(String refreshToken);
}