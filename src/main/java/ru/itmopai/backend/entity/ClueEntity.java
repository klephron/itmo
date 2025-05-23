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
@Table(name = "clues", indexes = { @Index(columnList = "case_id") })
public class ClueEntity {
  private @Id @GeneratedValue(strategy = GenerationType.AUTO) @Column(name = "clue_id") Long id;

  private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "creator_user_id") UserEntity creator;

  private @NotNull @Column(nullable = false) LocalDateTime createdAt;

  private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "updator_user_id", nullable = false) UserEntity updator;

  private @NotNull @Column(nullable = false) LocalDateTime updatedAt;

  private @NotNull @Column(nullable = false) @NotEmpty String title;

  private String description;

  private @NotNull @NotEmpty @Column(nullable = false) @Enumerated(EnumType.STRING) Status status;

  /**
   * E.g. JSON to store uris for attached images, videos, reports, location. TODO: specify format
   */
  private String metadata;

  private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "case_id", nullable = false) CaseEntity relatedCase;

  public static enum Status {
    ACCEPTED, REJECTED, PENDING
  }
}
