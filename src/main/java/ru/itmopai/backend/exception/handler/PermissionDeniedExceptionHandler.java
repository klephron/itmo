package ru.itmopai.backend.exception.handler;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import ru.itmopai.backend.exception.PermissionDenied;

import java.util.Map;

@Produces
@Singleton
public class PermissionDeniedExceptionHandler implements ExceptionHandler<PermissionDenied, HttpResponse<?>> {

  @Override
  public HttpResponse<?> handle(HttpRequest request, PermissionDenied exception) {
    return HttpResponse.status(HttpStatus.UNAUTHORIZED)
      .body(Map.of(
        "error", "permission_denied",
        "message", exception.getMessage()
      ));
  }
}
