package ru.itmo.zavar.brainstop.like.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.zavar.brainstop.like.model.VideoViewEntity;

import java.util.Collection;
import java.util.Set;

public interface VideoViewRepository extends JpaRepository<VideoViewEntity, Long> {
    boolean existsByVideoIdAndUserId(Long videoId, Long userId);

    @Query("select v.videoId from VideoViewEntity v where v.userId = :userId and v.videoId in :videoIds")
    Set<Long> findViewedVideoIds(@Param("userId") Long userId, @Param("videoIds") Collection<Long> videoIds);
}
