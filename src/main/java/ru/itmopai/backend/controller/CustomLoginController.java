package ru.itmopai.backend.controller;

import java.util.Date;
import java.util.Optional;

import com.nimbusds.jwt.SignedJWT;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.token.generator.AccessRefreshTokenGenerator;
import io.micronaut.security.token.render.AccessRefreshToken;
import jakarta.inject.Inject;
import ru.itmopai.backend.dto.request.LoginRequest;
import ru.itmopai.backend.dto.response.LoginResponse;
import ru.itmopai.backend.security.AuthenticationProviderUserPassword;

@Controller("/login")
@Secured(SecurityRule.IS_ANONYMOUS)
public class CustomLoginController {

  @Inject
  private AuthenticationProviderUserPassword authProvider;

  @Inject
  private AccessRefreshTokenGenerator tokenGenerator;

  @Post
  public HttpResponse<LoginResponse> login(@Body LoginRequest creds, HttpRequest<?> request) {
    UsernamePasswordCredentials upc =
        new UsernamePasswordCredentials(creds.username(), creds.password());
    AuthenticationResponse authResp = authProvider.authenticate(request, upc);

    Optional<Authentication> optional = authResp.getAuthentication();
    if (optional.isEmpty()) {
      return HttpResponse.unauthorized();
    }

    AccessRefreshToken token = tokenGenerator.generate(optional.get()).get();

    Long iat = null;
    try {
      SignedJWT jwt = SignedJWT.parse(token.getAccessToken());
      Date issued = jwt.getJWTClaimsSet().getIssueTime();
      if (issued != null) {
        iat = issued.toInstant().toEpochMilli();
      }
    } catch (Exception ignored) {}

    Long userId = (Long) optional.get().getAttributes().get("id");

    LoginResponse resp =
        new LoginResponse(
            token.getAccessToken(),
            token.getRefreshToken(),
            token.getExpiresIn(),
            optional.get().getName(),
            userId,
            iat);
    return HttpResponse.ok(resp);
  }
}
