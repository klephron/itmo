package ru.itmopai.backend.security;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.AuthenticationProvider;
import jakarta.inject.Singleton;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Singleton
public class AuthenticationProviderUserPassword implements AuthenticationProvider<HttpRequest<?>, String, String> {
  private final UserRepository repo;
  private final PasswordEncoder encoder;

  public AuthenticationProviderUserPassword(UserRepository repo, PasswordEncoder encoder) {
    this.repo = repo;
    this.encoder = encoder;
  }

  @Override
  @NonNull
  public AuthenticationResponse authenticate(@Nullable HttpRequest<?> requestContext,
                                             @NonNull AuthenticationRequest<String, String> authRequest) {
    String username = authRequest.getIdentity();
    String password = authRequest.getSecret();

    Optional<UserEntity> userOpt = repo.findByUsername(username);

    if (userOpt.isPresent() && encoder.matches(password, userOpt.get().getPassword())) {
      return AuthenticationResponse.success(username);
    } else {
      throw new AuthenticationException(new AuthenticationFailed());
    }
  }
}

