package ru.itmo.zavar.ticktokcoretest.video.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import ru.itmo.zavar.ticktokcoretest.common.infra.VectorConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "file_meta",
        indexes = {
                @Index(name = "idx_file_meta_uploader_uploaded", columnList = "uploader_id, uploaded_at"),
                @Index(name = "idx_file_meta_uploader_likes", columnList = "uploader_id, likes_count"),
                @Index(name = "idx_file_meta_uploader_views", columnList = "uploader_id, views_count")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileMetadataEntity {

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

    @Column(name = "views_count", nullable = false)
    private Long viewsCount = 0L;

    @Column(name = "thumbnail_key", length = 255)
    private String thumbnailKey;

    @Column(name = "default_height")
    private Integer defaultHeight;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VideoRenditionEntity> renditions = new ArrayList<>();

    @Convert(converter = VectorConverter.class)
    @Column(name = "title_embedding", columnDefinition = "vector(384)")
    @ColumnTransformer(write = "?::vector")
    private float[] titleEmbedding;

    @Convert(converter = VectorConverter.class)
    @Column(name = "description_embedding", columnDefinition = "vector(384)")
    @ColumnTransformer(write = "?::vector")
    private float[] descriptionEmbedding;

    @Convert(converter = VectorConverter.class)
    @Column(name = "combined_embedding", columnDefinition = "vector(384)")
    @ColumnTransformer(write = "?::vector")
    private float[] combinedEmbedding;

}
