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
  private @NotNull Long id;

  private @NotNull @NotEmpty String username;

  private @NotNull Double locationLatitude;

  private @NotNull Double locationLongitude;

  private @NotNull Double locationAltitude;
}
