package ru.itmopai.backend.entity;

import java.time.Instant;
import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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
  private String title;

  @NotNull
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
  @Introspected
  @Serdeable
  @Getter
  @Setter
  public static class Metadata {
    private List<Long> clueIds;
    private List<Long> suspectIds;
    private List<Long> employeeIds;
  }
}
