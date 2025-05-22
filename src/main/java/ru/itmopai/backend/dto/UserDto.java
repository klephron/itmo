package ru.itmopai.backend.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmopai.backend.entity.UserEntity}
 */
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
    Double locationLatitude;
    @NotNull
    Double locationLongitude;
    @NotNull
    Double locationAltitude;
}
