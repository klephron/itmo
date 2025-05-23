package ru.itmopai.backend.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.Value;

@Value
@Introspected
@Serdeable
public class UserDto implements Serializable {
  @NotNull
  private Long id;

  @NotNull
  @NotEmpty
  private String username;

  @NotNull
  private Double locationLatitude;

  @NotNull
  private Double locationLongitude;

  @NotNull
  private Double locationAltitude;
}
