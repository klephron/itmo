package ru.itmopai.backend.exception.handler;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import ru.itmopai.backend.exception.PermissionDeniedException;

import java.util.Map;

@Produces
@Singleton
public class PermissionDeniedExceptionHandler
    implements ExceptionHandler<PermissionDeniedException, HttpResponse<?>> {

  @Override
  public HttpResponse<?> handle(HttpRequest request, PermissionDeniedException exception) {
    return HttpResponse.status(HttpStatus.FORBIDDEN)
        .body(Map.of(
            "error", "permission_denied",
            "message", exception.getMessage()));
  }
}
