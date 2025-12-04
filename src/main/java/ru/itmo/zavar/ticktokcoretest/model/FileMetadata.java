package ru.itmo.zavar.ticktokcoretest.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_meta")
@Getter
@Setter
@NoArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "content_size", nullable = false)
    private Long contentSize;

    @Column(name = "uploader_id")
    private Long uploaderId;

    @Column(name = "uploader_name")
    private String uploaderName;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "likes_count", nullable = false)
    private Long likesCount = 0L;

    @Column(name = "thumbnail_key", length = 255)
    private String thumbnailKey;

}
