package ru.itmopai.backend.entity;

import java.time.Instant;

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
  private Instant createdAt;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updator_user_id", nullable = false)
  private UserEntity updator;

  @NotNull
  @Column(nullable = false)
  private Instant updatedAt;

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
  private Instant estimatedTime;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = true)
  private CaseEntity relatedCase;
}
