package ru.itmo.zavar.accountservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link ru.itmo.zavar.accountservice.entity.UserEntity}
 */
@Value
public class UserDto implements Serializable {
    @NotNull
    String id;
    @NotNull
    @NotEmpty
    String username;
    @NotNull
    @NotEmpty
    String email;
    @NotNull
    List<String> authorities;
}