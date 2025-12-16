package ru.itmo.zavar.brainstop.video.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "file_rendition")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VideoRenditionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_meta_id", nullable = false)
    private FileMetadataEntity file;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "content_size", nullable = false)
    private Long contentSize;

    @Column(name = "storage_key", nullable = false, length = 255, unique = true)
    private String storageKey;
}
