package ru.itmo.zavar.brainstop.like.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import ru.itmo.zavar.brainstop.like.model.VideoLikeEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VideoLikeRepository extends JpaRepository<VideoLikeEntity, Long> {

    Optional<VideoLikeEntity> findByVideoIdAndUserId(Long videoId, Long userId);

    boolean existsByVideoIdAndUserId(Long videoId, Long userId);

    long countByVideoId(Long videoId);

    @Modifying
    @Query("delete from VideoLikeEntity v where v.videoId = :videoId and v.userId = :userId")
    int deleteByVideoIdAndUserId(@Param("videoId") Long videoId, @Param("userId") Long userId);

    @Query("""
            select vl.videoId
            from VideoLikeEntity vl
            where vl.userId = :userId
              and vl.videoId in :videoIds
            """)
    List<Long> findLikedVideoIdsByUserIdAndVideoIdIn(@Param("userId") Long userId,
                                                     @Param("videoIds") Collection<Long> videoIds);
}

