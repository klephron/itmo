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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "case_suspects", indexes = { @Index(columnList = "case_id") })
public class CaseSuspectEntity {
  private @Id @GeneratedValue(strategy = GenerationType.AUTO) Long id;

  private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "creator_user_id", nullable = false) UserEntity creator;

  private @NotNull @Column(nullable = false) LocalDateTime createdAt;

  private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "updator_user_id", nullable = false) UserEntity updator;

  private @NotNull @Column(nullable = false) LocalDateTime updatedAt;

  private @NotNull @NotEmpty @Column(nullable = false) String name;
  private @NotNull @NotEmpty @Column(nullable = false) String surname;
  private String patronymic;

  /**
   * E.g. Passport, Russian Federation, 0419 845176
   */
  private @NotNull @NotEmpty @Column(unique = true, nullable = false) String identifier;

  private @NotNull @NotEmpty @Column(nullable = false) @Enumerated(EnumType.STRING) Status status;

  private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "case_id", nullable = false) CaseEntity relatedCase;

  public static enum Status {
    SUSPECTED, JUSTIFIED, ACCUSED
  }
}
