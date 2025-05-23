package ru.itmopai.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "cases")
public class CaseEntity {
  private @Id @GeneratedValue(strategy = GenerationType.AUTO) @Column(name = "case_id") Long id;

  private @NotNull @NotEmpty @Column(nullable = false) String title;

  private String description;

  private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "creator_user_id", nullable = false) UserEntity creator;

  private @NotNull @Column(nullable = false) LocalDateTime createdAt;

  private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "updator_user_id", nullable = false) UserEntity updator;

  private @NotNull @Column(nullable = false) LocalDateTime updatedAt;

  private @NotNull @NotEmpty @Column(nullable = false) @Enumerated(EnumType.STRING) Status status;

  private @NotNull @OneToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "incident_id", nullable = false) IncidentEntity incident;

  public static enum Status {
    OPENED, FROZEN, CLOSED, REJECTED,
  }
}
