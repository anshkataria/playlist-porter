package com.playlistporter.repository;
import com.playlistporter.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PortJobRepository extends JpaRepository<PortJob, Long> {
    List<PortJob> findByUserOrderByCreatedAtDesc(User user);
}