package ru.itmopai.backend.entity;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "clues", indexes = { @Index(columnList = "case_id") })
public class ClueEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "clue_id")
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_user_id")
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
  @NotEmpty
  private String title;

  private String description;

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Status status;

  /**
   * {@link} {@link Metadata}
   */
  private String metadata;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = false)
  private CaseEntity relatedCase;

  public static enum Status {
    ACCEPTED, REJECTED, PENDING
  }

  @EqualsAndHashCode
  public static class Metadata {
    @JsonProperty("imageUris")
    public List<String> imagesURIs;

    @JsonProperty("videoUris")
    public List<String> videoURIs;

    @JsonProperty("reportUris")
    public List<String> reportURIs;

    @JsonProperty("location")
    public Location location;

    @EqualsAndHashCode
    public static class Location {
      @JsonProperty("latitude")
      public Double latitude;

      @JsonProperty("longitude")
      public Double longitude;

      @JsonProperty("altitude")
      public Double altitude;
    }
  }
}
