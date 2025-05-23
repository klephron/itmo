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
  private @Id @GeneratedValue(strategy = GenerationType.AUTO) @Column(name = "incident_id") Long id;

  private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "creator_user_id", nullable = false) UserEntity creator;

  private @NotNull @Column(nullable = false) LocalDateTime createdAt;

  private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "updator_user_id", nullable = false) UserEntity updator;

  private @NotNull @Column(nullable = false) LocalDateTime updatedAt;

  private String applicant;

  private @NotNull @NotEmpty @Column(nullable = false) String type;

  private @NotNull @NotEmpty @Column(nullable = false) String reason;

  private @NotNull @Column(nullable = false) Double locationLatitude;

  private @NotNull @Column(nullable = false) Double locationLongitude;

  private @NotNull @Column(nullable = false) Double locationAltitude;

  private @NotNull @Column(nullable = false) LocalDateTime estimatedTime;

  private @Nullable @OneToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "case_id", nullable = true) CaseEntity relatedCase;
}
