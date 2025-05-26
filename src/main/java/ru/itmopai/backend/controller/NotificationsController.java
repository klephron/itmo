package ru.itmopai.backend.controller;

import java.time.LocalDateTime;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.format.Format;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;

@Controller("/notifications")
@AllArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class NotificationsController {

  /**
   * Get notification
   */
  @Get("/{id}")
  public HttpResponse<Object> getNotification(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Update notification (for example set date when read)
   */
  @Put("/{id}")
  public HttpResponse<Object> updateNotification(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Delete notification
   */
  @Delete("/{id}")
  public HttpResponse<Object> deleteNotification(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Get notifications
   */
  @Get
  public HttpResponse<Object> getNotifications(
      @QueryValue @Nullable Status status,
      @QueryValue @Nullable @Format("yyyy-MM-dd'T'HH:mm:ss") LocalDateTime before,
      @QueryValue @Nullable @Format("yyyy-MM-dd'T'HH:mm:ss") LocalDateTime after) {
    return HttpResponse.ok();
  }

  public static enum Status {
    READ, UNREAD, ALL
  }
}
