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
import ru.itmopai.backend.dto.ClueDto;
import ru.itmopai.backend.dto.request.AddUpdateClueRequest;
import ru.itmopai.backend.entity.ClueEntity;
import ru.itmopai.backend.mapper.ClueMainInfoMapper;
import ru.itmopai.backend.mapper.ClueMapper;
import ru.itmopai.backend.service.ClueService;
import ru.itmopai.backend.service.NotificationService;
import ru.itmopai.backend.service.PermissionService;

@Controller("/clues")
@AllArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class CluesController {
  private final ClueService clueService;
  private final ClueMainInfoMapper clueMainInfoMapper;
  private final ClueMapper clueMapper;
  private final NotificationService notificationService;
  private final PermissionService permissionService;

  /**
   * Получить улику по ID
   */
  @Get("/{id}")
  @Operation(
      summary = "Получить улику",
      description = "Возвращает информацию об улике по её идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Улика найдена",
          content = @Content(schema = @Schema(implementation = ClueDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Улика не найдена")
  })
  public HttpResponse<ClueDto> getClue(
      @Parameter(description = "ID улики", example = "17") @PathVariable @NotNull Long id,
      Authentication authentication) {
    try {
      ClueEntity clueEntity = clueService.getClue(id);
      permissionService.checkOrThrow(authentication, "CASES", clueEntity.getRelatedCase().getId(),
          "READ");
      return HttpResponse.ok(clueMapper.toDto(clueEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound();
    }
  }

  /**
   * Обновить улику (например, утвердить)
   */
  @Put("/{id}")
  @Operation(
      summary = "Обновить улику",
      description = "Обновляет параметры улики по идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Улика успешно обновлена",
          content = @Content(schema = @Schema(implementation = ClueDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Улика не найдена")
  })
  public HttpResponse<ClueDto> updateClue(
      @Parameter(description = "ID улики", example = "17") @PathVariable @NotNull Long id,
      @RequestBody(
          description = "Параметры для обновления улики",
          required = true,
          content = @Content(schema = @Schema(
              implementation = AddUpdateClueRequest.class))) @Body AddUpdateClueRequest addUpdateClueRequest,
      Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      ClueEntity clueEntity = clueService.getClue(id);
      permissionService.checkOrThrow(authentication, "CASES", clueEntity.getRelatedCase().getId(),
          "CLUES_UPDATE");

      clueEntity =
          clueService.updateClue(userId, id, addUpdateClueRequest.title(),
              addUpdateClueRequest.description(), addUpdateClueRequest.status());
      notificationService.addNotification(userId, "PAI Notification",
          "Clue %s updated in #%d case".formatted(addUpdateClueRequest.title(),
              clueEntity.getRelatedCase().getId()));
      return HttpResponse.ok(clueMapper.toDto(clueEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound();
    }
  }

  /**
   * Удалить улику
   */
  @Delete("/{id}")
  @Operation(
      summary = "Удалить улику",
      description = "Удаляет улику по идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "204",
          description = "Улика успешно удалена"),
      @ApiResponse(responseCode = "404",
          description = "Улика не найдена")
  })
  public HttpResponse<Object> deleteClue(
      @Parameter(description = "ID улики", example = "17") @PathVariable @NotNull Long id,
      Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      ClueEntity clueEntity = clueService.getClue(id);
      permissionService.checkOrThrow(authentication, "CASES", clueEntity.getRelatedCase().getId(),
          "CLUES_DELETE");

      clueService.deleteClue(id);
      notificationService.addNotification(userId, "PAI Notification",
          "Clue #%d deleted".formatted(id));
      return HttpResponse.noContent();
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }
}
