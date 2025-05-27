package ru.itmopai.backend.controller;

import java.util.NoSuchElementException;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.request.UpdateTemplateRequest;
import ru.itmopai.backend.entity.TemplateEntity;
import ru.itmopai.backend.mapper.TemplateMapper;
import ru.itmopai.backend.service.NotificationService;
import ru.itmopai.backend.service.PermissionService;
import ru.itmopai.backend.service.TemplateService;

@Controller("/templates")
@AllArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class TemplatesController {
  private final TemplateService templateService;
  private final TemplateMapper templateMapper;
  private final NotificationService notificationService;
  private final PermissionService permissionService;

  /**
   * Получить шаблон по ID
   */
  @Get("/{id}")
  @Operation(
      summary = "Получить шаблон",
      description = "Возвращает шаблон по идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Шаблон найден",
          content = @Content(
              schema = @Schema(implementation = ru.itmopai.backend.dto.TemplateDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Шаблон не найден",
          content = @Content(schema = @Schema(implementation = String.class)))
  })
  public HttpResponse<?> getTemplate(
      @Parameter(description = "ID шаблона", example = "42") @PathVariable @NotNull Long id,
      Authentication authentication) {
    try {
      TemplateEntity templateEntity = templateService.getTemplate(id);
      permissionService.checkOrThrow(authentication, "CASES",
          templateEntity.getRelatedCase().getId(), "READ");
      return HttpResponse.ok(templateMapper.toDto(templateEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Обновить шаблон (универсальное обновление)
   */
  @Put("/{id}")
  @Operation(
      summary = "Обновить шаблон",
      description = "Обновляет шаблон по идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Шаблон успешно обновлён",
          content = @Content(
              schema = @Schema(implementation = ru.itmopai.backend.dto.TemplateDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Шаблон не найден",
          content = @Content(schema = @Schema(implementation = String.class)))
  })
  public HttpResponse<?> updateTemplate(
      @Parameter(description = "ID шаблона", example = "42") @PathVariable @NotNull Long id,
      @RequestBody(
          description = "Данные для обновления шаблона",
          required = true,
          content = @Content(schema = @Schema(
              implementation = UpdateTemplateRequest.class))) @Body UpdateTemplateRequest updateTemplateRequest,
      Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      TemplateEntity templateEntity = templateService.getTemplate(id);
      permissionService.checkOrThrow(authentication, "CASES",
          templateEntity.getRelatedCase().getId(), "TEMPLATES_UPDATE");

      templateEntity =
          templateService.updateTemplate(userId, id, updateTemplateRequest.title(),
              updateTemplateRequest.status(),
              updateTemplateRequest.metadata(), updateTemplateRequest.description());
      notificationService.addNotification(userId, "PAI Notification",
          "Template %s updated in #%d case".formatted(updateTemplateRequest.title(),
              templateEntity.getRelatedCase().getId()));
      return HttpResponse.ok(templateMapper.toDto(templateEntity));
    } catch (NoSuchElementException | IllegalArgumentException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Удалить шаблон
   */
  @Delete("/{id}")
  @Operation(
      summary = "Удалить шаблон",
      description = "Удаляет шаблон по идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Шаблон успешно удалён"),
      @ApiResponse(responseCode = "404",
          description = "Шаблон не найден",
          content = @Content(schema = @Schema(implementation = String.class)))
  })
  public HttpResponse<?> deleteTemplate(
      @Parameter(description = "ID шаблона", example = "42") @PathVariable @NotNull Long id,
      Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      TemplateEntity templateEntity = templateService.getTemplate(id);
      permissionService.checkOrThrow(authentication, "CASES",
          templateEntity.getRelatedCase().getId(), "TEMPLATES_DELETE");

      templateService.deleteTemplate(id);
      notificationService.addNotification(userId, "PAI Notification",
          "Template #%d deleted".formatted(id));
      return HttpResponse.ok();
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }

  }

  /**
   * Опубликовать шаблон в Центральной Базе Данных
   */
  @Post("/{id}/cdb")
  @Operation(
      summary = "Опубликовать шаблон в Центральной БД",
      description = "Переводит шаблон в статус 'добавлен в CDB' (Центральную Базу Данных)")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Шаблон успешно опубликован",
          content = @Content(
              schema = @Schema(implementation = ru.itmopai.backend.dto.TemplateDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Шаблон не найден",
          content = @Content(schema = @Schema(implementation = String.class)))
  })
  public HttpResponse<Object> addTemplateToCDB(
      @Parameter(description = "ID шаблона", example = "42") @PathVariable @NotNull Long id,
      Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      TemplateEntity templateEntity = templateService.getTemplate(id);
      permissionService.checkOrThrow(authentication, "CASES",
          templateEntity.getRelatedCase().getId(), "CDB_TEMPLATES_ADD");

      return HttpResponse.ok(templateService.updateTemplate(userId, id, templateEntity.getTitle(),
          TemplateEntity.Status.CDB_ADDED, templateEntity.getMetadata(),
          templateEntity.getDescription()));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }
}
