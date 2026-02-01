package ru.itmopai.backend.controller;

import java.util.List;
import java.util.NoSuchElementException;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import ru.itmopai.backend.annotation.permission.RequiresPermissions;
import ru.itmopai.backend.dto.CaseDto;
import ru.itmopai.backend.dto.CaseSuspectMainInfoDto;
import ru.itmopai.backend.dto.ClueMainInfoDto;
import ru.itmopai.backend.dto.TemplateMainInfoDto;
import ru.itmopai.backend.dto.request.AddCaseSuspectRequest;
import ru.itmopai.backend.dto.request.AddClueRequest;
import ru.itmopai.backend.dto.request.AddTemplateRequest;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.ClueEntity;
import ru.itmopai.backend.entity.TemplateEntity;
import ru.itmopai.backend.mapper.*;
import ru.itmopai.backend.service.*;

@Validated
@Controller("/cases")
@RequiredArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class CasesController {
  private final CaseService caseService;
  private final CaseMapper caseMapper;
  private final ClueService clueService;
  private final ClueMainInfoMapper clueMainInfoMapper;
  private final TemplateService templateService;
  private final TemplateMapper templateMapper;
  private final TemplateMainInfoMapper templateMainInfoMapper;
  private final NotificationService notificationService;
  private final CaseSuspectService caseSuspectService;
  private final CaseSuspectMainInfoMapper caseSuspectMainInfoMapper;
  private final CaseSuspectMapper caseSuspectMapper;

  // TODO: also may add handle that checks permissions for each returned element
  // (to return cases to which user has access) because permissions like INCIDENTS:{id}:READ

  /**
   * Получить дело по ID
   */
  @RequiresPermissions({ "CASES:{id}:READ" })
  @Get("/{id}")
  @Operation(
      summary = "Получить дело",
      description = "Возвращает дело по его идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Дело найдено",
          content = @Content(schema = @Schema(implementation = CaseDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Дело не найдено")
  })
  public HttpResponse<CaseDto> getCase(
      @Parameter(description = "ID дела", example = "101") @PathVariable @NotNull Long id,
      Authentication authentication) {
    try {
      CaseEntity caseById = caseService.getCaseById(id);
      CaseDto dto = caseMapper.toDto(caseById);
      return HttpResponse.ok(dto);
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound();
    }
  }

  /**
   * Получить шаблоны дела
   */
  @RequiresPermissions({ "CASES:{id}:READ" })
  @Get("/{id}/templates")
  @Operation(
      summary = "Получить шаблоны дела",
      description = "Возвращает список шаблонов, связанных с делом")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Список шаблонов",
          content = @Content(
              array = @ArraySchema(schema = @Schema(implementation = TemplateMainInfoDto.class))))
  })
  public HttpResponse<List<TemplateMainInfoDto>> getCaseTemplates(
      @Parameter(description = "ID дела", example = "101") @PathVariable @NotNull Long id,
      Authentication authentication) {
    return HttpResponse.ok(templateMainInfoMapper.toDto(templateService.getTemplatesByCaseId(id)));
  }

  /**
   * Получить улики дела
   */
  @RequiresPermissions({ "CASES:{id}:READ" })
  @Get("/{id}/clues")
  @Operation(
      summary = "Получить улики дела",
      description = "Возвращает список улик, связанных с делом")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Список улик",
          content = @Content(
              array = @ArraySchema(schema = @Schema(implementation = ClueMainInfoDto.class))))
  })
  public HttpResponse<List<ClueMainInfoDto>> getCaseClues(
      @Parameter(description = "ID дела", example = "101") @PathVariable @NotNull Long id,
      Authentication authentication) {
    return HttpResponse.ok(clueMainInfoMapper.toDto(clueService.getAllClueByCase(id)));
  }

  /**
   * Добавить шаблон к делу
   */
  @RequiresPermissions({ "CASES:{id}:TEMPLATES_ADD" })
  @Post("/{id}/templates")
  @Operation(
      summary = "Добавить шаблон к делу",
      description = "Создаёт новый шаблон, связанный с делом")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Шаблон успешно добавлен",
          content = @Content(
              schema = @Schema(implementation = ru.itmopai.backend.dto.TemplateDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Ошибка создания шаблона (дело не найдено или параметры некорректны)",
          content = @Content(schema = @Schema(implementation = String.class)))
  })
  public HttpResponse<?> addCaseTemplate(
      @Parameter(description = "ID дела", example = "101") @PathVariable @NotNull Long id,
      @RequestBody(
          description = "Данные для создания шаблона",
          required = true,
          content = @Content(schema = @Schema(
              implementation = AddTemplateRequest.class))) @Body @Valid AddTemplateRequest addTemplateRequest,
      Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      TemplateEntity templateEntity =
          templateService.addTemplate(userId, id, addTemplateRequest.title(),
              addTemplateRequest.metadata(), addTemplateRequest.description());
      notificationService.addNotification(userId, "PAI Notification",
          "Template %s added to #%d case".formatted(addTemplateRequest.title(),
              templateEntity.getRelatedCase().getId()));
      return HttpResponse.ok(templateMapper.toDto(templateEntity));
    } catch (NoSuchElementException | IllegalArgumentException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Добавить улику к делу
   */
  @RequiresPermissions({ "CASES:{id}:CLUES_ADD" })
  @Post("/{id}/clues")
  @Operation(
      summary = "Добавить улику к делу",
      description = "Создаёт новую улику, связанную с делом")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Улика успешно добавлена",
          content = @Content(schema = @Schema(implementation = ClueMainInfoDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Ошибка создания улики (дело не найдено или параметры некорректны)",
          content = @Content(schema = @Schema(implementation = String.class)))
  })
  public HttpResponse<?> addCaseClue(
      @Parameter(description = "ID дела", example = "101") @PathVariable @NotNull Long id,
      @RequestBody(
          description = "Данные для создания улики",
          required = true,
          content = @Content(schema = @Schema(
              implementation = AddClueRequest.class))) @Body @Valid AddClueRequest updateClueRequest,
      Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      ClueEntity clueEntity =
          clueService.addClue(userId,
              updateClueRequest.title(),
              updateClueRequest.description(),
              updateClueRequest.status(),
              id);
      notificationService.addNotification(userId, "PAI Notification",
          "Clue %s added to #%d case".formatted(updateClueRequest.title(),
              clueEntity.getRelatedCase().getId()));
      return HttpResponse.ok(clueMainInfoMapper.toDto(clueEntity));
    } catch (NoSuchElementException | IllegalArgumentException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  @RequiresPermissions({ "CASES:{id}:READ" })
  @Get("/{id}/suspects")
  public HttpResponse<List<CaseSuspectMainInfoDto>> getAllCaseSuspect(
      @PathVariable @NotNull Long id,
      Authentication authentication) {
    return HttpResponse
        .ok(caseSuspectMainInfoMapper.toDto(caseSuspectService.getAllCaseSuspectByCaseId(id)));
  }

  @RequiresPermissions({ "CASES:{id}:SUSPECT_ADD" })
  @Post("/{id}/suspects")
  public HttpResponse<?> addCaseSuspect(@PathVariable @NotNull Long id,
      @Body @Valid AddCaseSuspectRequest addCaseSuspectRequest,
      Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      return HttpResponse
          .ok(caseSuspectMapper
              .toDto(caseSuspectService.addCaseSuspect(userId, addCaseSuspectRequest.name(),
                  addCaseSuspectRequest.surname(), addCaseSuspectRequest.patronymic(),
                  addCaseSuspectRequest.identifier(), addCaseSuspectRequest.status(), id,
                  addCaseSuspectRequest.verdict(), addCaseSuspectRequest.accusation())));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound(e.getMessage());
    }
  }

}
