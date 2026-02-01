package ru.itmopai.backend.dto;

import java.io.Serializable;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.itmopai.backend.entity.UserCaseEntity;

/**
 * DTO for {@link ru.itmopai.backend.entity.UserCaseEntity}
 */
@AllArgsConstructor
@NoArgsConstructor
@Introspected
@Serdeable
@Setter
@Getter
public class UserCaseDto implements Serializable {
  Long idUserId;
  Long idRelatedCaseId;
  String idRelatedCaseTitle;
  @NotNull
  @NotEmpty
  String position;
  @NotNull
  UserCaseEntity.Status status;
}
