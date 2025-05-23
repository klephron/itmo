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

  private @NotNull LocalDateTime createdAt;

  private @NotNull @NotEmpty String title;

  private String description;

  private @NotNull @NotEmpty @Enumerated(EnumType.STRING) Status status;

  private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "status_updator_user_id") UserEntity statusUpdator;

  /**
   * E.g. JSON to store uris for attached images, videos, reports, location. TODO: specify format
   */
  private String metadata;

  private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "case_id") CaseEntity relatedCase;

  public static enum Status {
    ACCEPTED, REJECTED, PENDING
  }
}
