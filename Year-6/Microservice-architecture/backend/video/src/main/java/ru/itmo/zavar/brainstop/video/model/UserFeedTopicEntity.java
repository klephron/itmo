package ru.itmo.zavar.brainstop.video.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.itmo.zavar.brainstop.common.infra.VectorConverter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_feed_topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserFeedTopicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "term", nullable = false, columnDefinition = "text")
    private String term;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Convert(converter = VectorConverter.class)
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    @ColumnTransformer(write = "?::vector")
    private float[] embedding;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
