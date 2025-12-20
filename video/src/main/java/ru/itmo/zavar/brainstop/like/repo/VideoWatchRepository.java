package ru.itmo.zavar.brainstop.like.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.zavar.brainstop.like.model.VideoWatchEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface VideoWatchRepository extends JpaRepository<VideoWatchEntity, Long> {

    Optional<VideoWatchEntity> findByVideoIdAndUserId(Long videoId, Long userId);

    @Modifying
    @Query(value = "insert into video_watch " +
            "(video_id, user_id, total_watched_seconds, last_position_seconds, duration_seconds, updated_at) " +
            "values (:videoId, :userId, :positionSeconds, :positionSeconds, :durationSeconds, now()) " +
            "on conflict (video_id, user_id) do update set " +
            "total_watched_seconds = video_watch.total_watched_seconds + " +
            "case when excluded.last_position_seconds >= video_watch.last_position_seconds " +
            "then excluded.last_position_seconds - video_watch.last_position_seconds " +
            "else excluded.last_position_seconds end, " +
            "last_position_seconds = excluded.last_position_seconds, " +
            "duration_seconds = case when excluded.duration_seconds > 0 " +
            "then excluded.duration_seconds else video_watch.duration_seconds end, " +
            "updated_at = now()",
            nativeQuery = true)
    void upsertProgress(@Param("videoId") Long videoId,
                        @Param("userId") Long userId,
                        @Param("positionSeconds") long positionSeconds,
                        @Param("durationSeconds") long durationSeconds);

    @Query("select w.videoId as videoId, w.totalWatchedSeconds as totalWatchedSeconds, w.durationSeconds as durationSeconds " +
            "from VideoWatchEntity w where w.userId = :userId and w.videoId in :videoIds")
    List<WatchProgressSummary> findWatchProgress(@Param("userId") Long userId,
                                                 @Param("videoIds") Collection<Long> videoIds);

    @Query("select w.videoId from VideoWatchEntity w " +
            "where w.userId = :userId and w.videoId in :videoIds " +
            "and w.durationSeconds > 0 and w.totalWatchedSeconds * 1.0 >= w.durationSeconds * :threshold")
    Set<Long> findWatchedVideoIdsAtOrAbove(@Param("userId") Long userId,
                                           @Param("videoIds") Collection<Long> videoIds,
                                           @Param("threshold") double threshold);
}
