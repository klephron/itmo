package ru.itmo.zavar.ticktokcoretest.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.ticktokcoretest.entity.VideoViewEntity;

public interface VideoViewRepository extends JpaRepository<VideoViewEntity, Long> {
    boolean existsByVideoIdAndUserId(Long videoId, Long userId);
}