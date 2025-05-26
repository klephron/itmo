package ru.itmopai.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import io.micronaut.core.convert.format.Format;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.NotificationDto;
import ru.itmopai.backend.dto.request.UpdateNotificationRequest;
import ru.itmopai.backend.entity.NotificationEntity;
import ru.itmopai.backend.mapper.NotificationMapper;
import ru.itmopai.backend.service.NotificationService;

@Controller("/notifications")
@AllArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class NotificationsController {

  private final NotificationService notificationService;
  private final NotificationMapper notificationMapper;

  /**
   * Get notification
   */
  @Get("/{id}")
  public HttpResponse<NotificationDto> getNotification(@PathVariable @NotNull Long id) {
    try {
      return HttpResponse.ok(notificationMapper.toDto(notificationService.getNotification(id)));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound();
    }
  }

  /**
   * Update notification (for example set date when read)
   */
  @Put("/{id}")
  public HttpResponse<NotificationDto> updateNotification(@PathVariable @NotNull Long id,
      @Body UpdateNotificationRequest request) {
    try {
      NotificationEntity notificationEntity =
          notificationService.updateNotification(id, request.title(), request.description(),
              request.readAt());
      return HttpResponse.ok(notificationMapper.toDto(notificationEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound();
    }
  }

  /**
   * Delete notification
   */
  @Delete("/{id}")
  public HttpResponse<Object> deleteNotification(@PathVariable @NotNull Long id) {
    notificationService.deleteNotification(id);
    return HttpResponse.noContent();
  }

  /**
   * Get notifications
   */
  @Get
  public HttpResponse<List<NotificationDto>> getNotifications(
      @QueryValue Status status,
      @QueryValue @Format("yyyy-MM-dd'T'HH:mm:ss") LocalDateTime from,
      @QueryValue @Format("yyyy-MM-dd'T'HH:mm:ss") LocalDateTime to) {
    List<NotificationDto> list = notificationMapper.toDto(switch (status) {
      case READ -> notificationService.getReadNotifications(from, to);
      case UNREAD -> notificationService.getUnreadNotifications(from, to);
      case ALL -> notificationService.getAllNotifications(from, to);
    });
    return HttpResponse.ok(list);
  }

  public static enum Status {
    READ, UNREAD, ALL
  }
}
