package ru.itmo.zavar.brainstop.interactions.video.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "file_meta")
@Getter
@Setter
@NoArgsConstructor
public class VideoMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "likes_count", nullable = false)
    private Long likesCount = 0L;

    @Column(name = "views_count", nullable = false)
    private Long viewsCount = 0L;
}
