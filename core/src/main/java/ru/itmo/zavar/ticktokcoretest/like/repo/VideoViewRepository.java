package ru.itmo.zavar.ticktokcoretest.like.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.ticktokcoretest.like.model.VideoViewEntity;

public interface VideoViewRepository extends JpaRepository<VideoViewEntity, Long> {
    boolean existsByVideoIdAndUserId(Long videoId, Long userId);
}
