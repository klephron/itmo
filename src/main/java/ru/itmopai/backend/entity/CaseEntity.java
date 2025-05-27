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
@Table(name = "cases")
public class CaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private @Column(name = "case_id") Long id;

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  private String title;

  private String description;

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

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Status status;

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "incident_id", nullable = false)
  private IncidentEntity incident;

  public static enum Status {
    OPENED, FROZEN, CLOSED, REJECTED;
  }
}
