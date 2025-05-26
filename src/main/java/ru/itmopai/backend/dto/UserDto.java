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
  Long id;

  @NotNull
  @NotEmpty
  String username;

  @NotNull
  @NotEmpty
  String name;

  @NotNull
  @NotEmpty
  String surname;

  String patronymic;

  @NotNull
  Double locationLatitude;

  @NotNull
  Double locationLongitude;

  @NotNull
  Double locationAltitude;
}
