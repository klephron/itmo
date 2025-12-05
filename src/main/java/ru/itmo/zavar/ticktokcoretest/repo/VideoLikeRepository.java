package ru.itmo.zavar.ticktokcoretest.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.ticktokcoretest.entity.VideoLikeEntity;

import java.util.Optional;

public interface VideoLikeRepository extends JpaRepository<VideoLikeEntity, Long> {

    Optional<VideoLikeEntity> findByVideoIdAndUserId(Long videoId, Long userId);

    boolean existsByVideoIdAndUserId(Long videoId, Long userId);

    long countByVideoId(Long videoId);
}

