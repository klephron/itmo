package ru.itmopai.backend.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;
import io.micronaut.core.convert.format.Format;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;

@Controller("/notifications")
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
      @QueryValue(value = "status", defaultValue = "all") Status status,
      @QueryValue @Format("yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAfter,
      @QueryValue @Format("yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdBefore) {

    return HttpResponse.ok();
  }

  public static enum Status {
    READ, UNREAD, ALL
  }
}
