package com.playlistporter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import com.playlistporter.entity.Platform;
import com.playlistporter.entity.User;
import com.playlistporter.entity.PlatformConnection;

public interface PlatformConnectionRepository extends JpaRepository<PlatformConnection,Long>{
    Optional<PlatformConnection> findByUserAndPlatform(User user, Platform platform);
}
