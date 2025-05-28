package ru.itmopai.backend.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.itmopai.backend.entity.CaseEntity;

/**
 * DTO for {@link ru.itmopai.backend.entity.CaseEntity}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Introspected
@Serdeable
public class CaseDto implements Serializable {
  @NotNull
  private Long id;

  @NotNull
  @NotEmpty
  private String title;

  private String description;

  @NotNull
  private Long creatorId;

  @NotNull
  private Instant createdAt;

  @NotNull
  private Long updatorId;

  @NotNull
  private Instant updatedAt;

  @NotNull
  private CaseEntity.Status status;

  @NotNull
  private Long incidentId;

  @NotNull
  @Valid
  private HashMap<@NotNull Long, @NotEmpty String> users;
}
