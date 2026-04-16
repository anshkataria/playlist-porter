package com.playlistporter.entity;

public enum MatchStatus {
    EXACT,          // ISRC match or perfect title+artist
    FUZZY,          // embedding similarity match
    SUBSTITUTED,    // different track, similar vibe
    NOT_FOUND       // nothing suitable found
}
