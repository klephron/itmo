package ru.itmo.zavar.ticktokcoretest.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.zavar.ticktokcoretest.entity.VideoLikeEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VideoLikeRepository extends JpaRepository<VideoLikeEntity, Long> {

    Optional<VideoLikeEntity> findByVideoIdAndUserId(Long videoId, Long userId);

    boolean existsByVideoIdAndUserId(Long videoId, Long userId);

    long countByVideoId(Long videoId);

    @Query("""
            select vl.videoId
            from VideoLikeEntity vl
            where vl.userId = :userId
              and vl.videoId in :videoIds
            """)
    List<Long> findLikedVideoIdsByUserIdAndVideoIdIn(@Param("userId") Long userId,
                                                     @Param("videoIds") Collection<Long> videoIds);
}

