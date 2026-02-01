package ru.itmo.zavar.brainstop.video.transcoding.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStage;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upload_jobs")
@Getter
@Setter
public class UploadJobEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "video_name")
    private String videoName;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private TranscodingJobStage stage;

    @Column(name = "percent", nullable = false)
    private int percent;

    @Column(name = "message")
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
