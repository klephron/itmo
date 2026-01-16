package ru.itmo.zavar.brainstop.video.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_feed_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserFeedPreferencesEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "like_weight", nullable = false)
    private Double likeWeight = 1d;

    @Column(name = "follow_weight", nullable = false)
    private Double followWeight = 1d;

    @Column(name = "watch_weight", nullable = false)
    private Double watchWeight = 1d;

    @Column(name = "watch_threshold", nullable = false)
    private Double watchThreshold = 0.5d;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
