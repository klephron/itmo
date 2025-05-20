package ru.itmo.zavar.account.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link ru.itmo.zavar.account.entity.UserEntity}
 */
@Value
public class UserDto implements Serializable {
    @NotNull
    @NotEmpty
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
