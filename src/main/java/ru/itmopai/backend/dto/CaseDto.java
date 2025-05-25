package ru.itmopai.backend.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.itmopai.backend.entity.CaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * DTO for {@link ru.itmopai.backend.entity.CaseEntity}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Introspected
@Serdeable
public class CaseDto implements Serializable {

  private Long id;

  @NotNull
  @NotEmpty
  private String title;

  private String description;

  private Long creatorId;

  @NotNull
  private LocalDateTime createdAt;

  private Long updatorId;

  @NotNull
  private LocalDateTime updatedAt;

  @NotNull
  private CaseEntity.Status status;

  private Long incidentId;

  @NotNull
  @Valid
  private HashMap<@NotNull Long, @NotEmpty String> users;
}
