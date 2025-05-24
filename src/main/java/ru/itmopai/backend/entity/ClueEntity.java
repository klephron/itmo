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
  private LocalDateTime createdAt;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updator_user_id", nullable = false)
  private UserEntity updator;

  @NotNull
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @NotNull
  @Column(nullable = false)
  @NotEmpty
  private String title;

  private String description;

  @NotNull
  @NotEmpty
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
    @JsonProperty("image_uris")
    public List<String> imagesURIs;

    @JsonProperty("video_uris")
    public List<String> videoURIs;

    @JsonProperty("report_uris")
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
