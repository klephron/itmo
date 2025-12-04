package ru.itmo.zavar.ticktokcoretest.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID videoUuid;   // FileMetadata.uuid

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 128)
    private String authorName;

    @Column(nullable = false, length = 1024)
    private String text;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

