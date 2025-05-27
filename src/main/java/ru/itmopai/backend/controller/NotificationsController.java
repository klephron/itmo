package ru.itmopai.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import io.micronaut.core.convert.format.Format;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.Nullable;
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
   * Получить уведомление по идентификатору
   */
  @Get("/{id}")
  @Operation(
      summary = "Получить уведомление",
      description = "Возвращает уведомление по его идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Уведомление найдено",
          content = @Content(schema = @Schema(implementation = NotificationDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Уведомление не найдено")
  })
  public HttpResponse<NotificationDto> getNotification(
      @Parameter(description = "ID уведомления", example = "5") @PathVariable @NotNull Long id) {
    try {
      return HttpResponse.ok(notificationMapper.toDto(notificationService.getNotification(id)));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound();
    }
  }

  /**
   * Обновить уведомление (например, установить дату прочтения)
   */
  @Put("/{id}")
  @Operation(
      summary = "Обновить уведомление",
      description = "Обновляет уведомление, например, устанавливает дату прочтения")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Уведомление успешно обновлено",
          content = @Content(schema = @Schema(implementation = NotificationDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Уведомление не найдено")
  })
  public HttpResponse<NotificationDto> updateNotification(
      @Parameter(description = "ID уведомления", example = "5") @PathVariable @NotNull Long id,
      @RequestBody(
          description = "Данные для обновления уведомления",
          required = true,
          content = @Content(schema = @Schema(
              implementation = UpdateNotificationRequest.class))) @Body UpdateNotificationRequest request) {
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
   * Удалить уведомление
   */
  @Delete("/{id}")
  @Operation(
      summary = "Удалить уведомление",
      description = "Удаляет уведомление по идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "204",
          description = "Уведомление успешно удалено"),
      @ApiResponse(responseCode = "404",
          description = "Уведомление не найдено")
  })
  public HttpResponse<Object> deleteNotification(
      @Parameter(description = "ID уведомления", example = "5") @PathVariable @NotNull Long id) {
    notificationService.deleteNotification(id);
    return HttpResponse.noContent();
  }

  /**
   * Получить список уведомлений (можно фильтровать по статусу и времени)
   */
  @Get
  @Operation(
      summary = "Получить список уведомлений",
      description = "Возвращает список всех уведомлений. Можно фильтровать по статусу (прочитано/непрочитано/все) и диапазону дат.")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Список уведомлений",
          content = @Content(
              array = @ArraySchema(schema = @Schema(implementation = NotificationDto.class))))
  })
  public HttpResponse<List<NotificationDto>> getNotifications(
      @Parameter(
          description = "Статус уведомлений для фильтрации",
          example = "ALL",
          schema = @Schema(implementation = Status.class)) @QueryValue @Nullable Status status,
      @Parameter(
          description = "Дата и время начала интервала (например, 2025-05-26T17:28:43)",
          example = "2025-05-26T17:28:43") @QueryValue @Nullable @Format("yyyy-MM-dd'T'HH:mm:ss") LocalDateTime from,
      @Parameter(
          description = "Дата и время конца интервала (например, 2025-05-26T17:48:43)",
          example = "2025-05-26T17:48:43") @QueryValue @Nullable @Format("yyyy-MM-dd'T'HH:mm:ss") LocalDateTime to) {
    List<NotificationDto> list = notificationMapper.toDto(switch (status) {
      case READ -> notificationService.getReadNotifications(from, to);
      case UNREAD -> notificationService.getUnreadNotifications(from, to);
      case ALL -> notificationService.getAllNotifications(from, to);
      case null -> notificationService.getAllNotifications(from, to);
    });
    return HttpResponse.ok(list);
  }

  public static enum Status {
    READ, UNREAD, ALL
  }
}
