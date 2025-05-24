package ru.itmopai.backend.entity;

import java.time.LocalDateTime;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
@Table(name = "incidents")
public class IncidentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "incident_id")
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_user_id", nullable = false)
  private UserEntity creator;

  @NotNull
  @Column(nullable = false)
  private LocalDateTime createdAt;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updator_user_id", nullable = false)
  private UserEntity updator;

  @NotNull
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  private String applicant;

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  private String type;

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  private String reason;

  @NotNull
  @Column(nullable = false)
  private Double locationLatitude;

  @NotNull
  @Column(nullable = false)
  private Double locationLongitude;

  @NotNull
  @Column(nullable = false)
  private Double locationAltitude;

  @NotNull
  @Column(nullable = false)
  private LocalDateTime estimatedTime;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = true)
  private CaseEntity relatedCase;
}
