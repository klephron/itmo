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
@Table(name = "case_suspects", indexes = { @Index(columnList = "case_id") })
public class CaseSuspectEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
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

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  private String name;

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  private String surname;

  private String patronymic;

  private String verdict;

  private String accusation;

  /**
   * E.g. Passport, Russian Federation, 0419 845176
   */
  @NotNull
  @NotEmpty
  @Column(unique = true, nullable = false)
  private String identifier;

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Status status;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = false)
  private CaseEntity relatedCase;

  public static enum Status {
    SUSPECTED, JUSTIFIED, ACCUSED
  }
}
