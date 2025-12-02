package ru.itmo.zavar.ticktokcoretest.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.ticktokcoretest.model.VideoLike;

import java.util.Optional;

public interface VideoLikeRepository extends JpaRepository<VideoLike, Long> {

    Optional<VideoLike> findByVideoIdAndUserId(Long videoId, Long userId);

    boolean existsByVideoIdAndUserId(Long videoId, Long userId);

    long countByVideoId(Long videoId);
}

