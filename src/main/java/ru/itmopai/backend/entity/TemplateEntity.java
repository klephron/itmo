package ru.itmopai.backend.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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
import lombok.EqualsAndHashCode;
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
   * {@link} {@link Metadata}
   */
  @NotNull
  @NotEmpty
  @Column(nullable = false)
  private String metadata;

  private String description;

  /**
   * URI to access generated template, when status == CREATED should not be null
   */
  private String uri;

  /**
   * URI to access template from Central Database, when status == CDB_ADDED should not be null
   */
  @Column(name = "cdb_uri")
  private String cdbURI;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = false)
  private CaseEntity relatedCase;

  public static enum Status {
    PENDING, CREATED, CDB_ADDED
  }

  @EqualsAndHashCode
  public static class Metadata {
    @JsonProperty("clue_ids")
    public List<Long> clueIds;

    @JsonProperty("suspect_ids")
    public List<Long> suspectIds;

    @JsonProperty("employee_ids")
    public List<Long> employeeIds;
  }
}
