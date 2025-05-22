package ru.itmopai.backend.service;

import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Singleton
public class BCryptPasswordEncoderService implements PasswordEncoder {

  PasswordEncoder delegate = new BCryptPasswordEncoder();

  @Override
  public String encode(@NotBlank @NonNull CharSequence rawPassword) {
    return delegate.encode(rawPassword);
  }

  @Override
  public boolean matches(@NotBlank @NonNull CharSequence rawPassword,
                         @NotBlank @NonNull String encodedPassword) {
    return delegate.matches(rawPassword, encodedPassword);
  }
}
