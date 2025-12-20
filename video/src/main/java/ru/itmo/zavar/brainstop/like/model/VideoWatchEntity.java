package ru.itmo.zavar.brainstop.like.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "video_watch",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_watch_video_user", columnNames = {"video_id", "user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoWatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_watched_seconds", nullable = false)
    private Long totalWatchedSeconds = 0L;

    @Column(name = "last_position_seconds", nullable = false)
    private Long lastPositionSeconds = 0L;

    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds = 0L;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
