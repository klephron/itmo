package ru.itmopai.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications", indexes = { @Index(columnList = "user_id,readAt,createdAt") })
public class NotificationEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private @Column(name = "notification_id") Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  private String title;

  private String description;

  @NotNull
  @Column(nullable = false)
  private LocalDateTime createdAt;

  private LocalDateTime readAt;
}
