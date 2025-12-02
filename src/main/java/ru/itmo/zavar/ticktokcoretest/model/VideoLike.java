package ru.itmo.zavar.ticktokcoretest.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "video_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_video_user", columnNames = {"video_id", "user_id"})
        }
)
public class VideoLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;  // ссылка на FileMetadata.id

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public VideoLike() {}

    public VideoLike(Long videoId, Long userId) {
        this.videoId = videoId;
        this.userId = userId;
    }

    // getters / setters
}

