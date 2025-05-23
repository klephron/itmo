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
@Table(name = "templates", indexes = { @Index(columnList = "case_id") })
public class TemplateEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "template_id")
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

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  private String title;

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Status status;

  /**
   * E.g. ids of clues, suspects, employees, Central Database Links. TODO: specify format
   */
  @NotNull
  private String metadata;

  private String description;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = false)
  private CaseEntity relatedCase;

  public static enum Status {
    PENDING, CREATED, CDB_ADDED
  }
}
