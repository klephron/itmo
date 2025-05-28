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
import lombok.AllArgsConstructor;
import ru.itmopai.backend.annotation.permission.RequiresPermissions;
import ru.itmopai.backend.dto.CaseDto;
import ru.itmopai.backend.dto.IncidentDto;
import ru.itmopai.backend.dto.IncidentMainInfoDto;
import ru.itmopai.backend.dto.request.AddCaseRequest;
import ru.itmopai.backend.dto.request.AddIncidentRequest;
import ru.itmopai.backend.dto.request.UpdateCaseRequest;
import ru.itmopai.backend.dto.request.UpdateIncidentRequest;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.IncidentEntity;
import ru.itmopai.backend.mapper.CaseMapper;
import ru.itmopai.backend.mapper.IncidentMainInfoMapper;
import ru.itmopai.backend.mapper.IncidentMapper;
import ru.itmopai.backend.service.CaseService;
import ru.itmopai.backend.service.IncidentService;
import ru.itmopai.backend.service.NotificationService;

@Validated
@AllArgsConstructor
@Controller("/incidents")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class IncidentsController {
  private final IncidentService incidentService;
  private final IncidentMainInfoMapper incidentMainInfoMapper;
  private final IncidentMapper incidentMapper;
  private final CaseService caseService;
  private final CaseMapper caseMapper;
  private final NotificationService notificationService;

  /**
   * Создать новое происшествие (инцидент)
   */
  @RequiresPermissions({ "INCIDENTS:*:ADD" })
  @Post
  @Operation(
      summary = "Создать инцидент",
      description = "Создаёт новый инцидент с заданными параметрами")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Инцидент успешно создан",
          content = @Content(schema = @Schema(implementation = IncidentMainInfoDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Ошибка создания (не найден связанный объект)",
          content = @Content(schema = @Schema(implementation = String.class)))
  })
  public HttpResponse<?> addIncident(
      @RequestBody(
          description = "Параметры нового инцидента",
          required = true,
          content = @Content(schema = @Schema(
              implementation = AddIncidentRequest.class))) @Body @Valid AddIncidentRequest request,
      Authentication auth) {
    Long userId = (Long) auth.getAttributes().get("id");
    try {
      IncidentEntity incidentEntity =
          incidentService.addIncident(
              userId, request.applicant(), request.type(), request.reason(),
              request.locationLatitude(), request.locationLongitude(), request.locationAltitude(),
              request.estimatedTime());
      notificationService.addNotification(userId, "PAI Notification", "New incident created");
      return HttpResponse.ok(incidentMainInfoMapper.toDto(incidentEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Обновить информацию о происшествии (инциденте)
   */
  @RequiresPermissions({ "INCIDENTS:*:UPDATE" })
  @Put("/{id}")
  @Operation(
      summary = "Обновить инцидент",
      description = "Обновляет данные существующего инцидента по идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Инцидент успешно обновлён",
          content = @Content(schema = @Schema(implementation = IncidentMainInfoDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Инцидент не найден",
          content = @Content(schema = @Schema(implementation = String.class)))
  })
  public HttpResponse<?> updateIncident(
      @Parameter(description = "ID инцидента", example = "1") @PathVariable @NotNull Long id,
      @RequestBody(
          description = "Новые параметры инцидента",
          required = true,
          content = @Content(schema = @Schema(
              implementation = UpdateIncidentRequest.class))) @Body @Valid UpdateIncidentRequest request,
      Authentication auth) {
    Long userId = (Long) auth.getAttributes().get("id");
    try {
      IncidentEntity incidentEntity =
          incidentService.updateIncident(
              userId, id, request.applicant(), request.type(),
              request.applicant(), request.locationLatitude(), request.locationLongitude(),
              request.locationAltitude(), request.estimatedTime());
      notificationService.addNotification(userId, "PAI Notification",
          "Incident #%d updated".formatted(id));
      return HttpResponse.ok(incidentMainInfoMapper.toDto(incidentEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Создать дело по инциденту
   */
  @RequiresPermissions({ "INCIDENTS:*:UPDATE" })
  @Post("/{id}/case")
  @Operation(
      summary = "Создать дело по инциденту",
      description = "Добавляет новое дело к указанному инциденту")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Дело успешно создано",
          content = @Content(schema = @Schema(implementation = CaseDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Инцидент не найден",
          content = @Content(schema = @Schema(implementation = String.class)))
  })
  public HttpResponse<?> addCaseForIncident(
      @Parameter(description = "ID инцидента", example = "1") @PathVariable @NotNull Long id,
      @RequestBody(
          description = "Параметры нового дела",
          required = true,
          content = @Content(schema = @Schema(
              implementation = AddCaseRequest.class))) @Body @Valid AddCaseRequest addCaseRequest,
      Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      CaseEntity caseEntity =
          caseService.addCase(
              userId, addCaseRequest.title(), addCaseRequest.description(), id,
              addCaseRequest.users());
      notificationService.addNotification(userId, "PAI Notification",
          "Case %s created for incident #%d".formatted(addCaseRequest.title(), id));
      return HttpResponse.ok(caseMapper.toDto(caseEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Обновить дело по инциденту
   */
  @RequiresPermissions({ "INCIDENTS:*:UPDATE" })
  @Put("/{id}/case")
  @Operation(
      summary = "Обновить дело по инциденту",
      description = "Обновляет существующее дело, связанное с инцидентом")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Дело успешно обновлено",
          content = @Content(schema = @Schema(implementation = CaseDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Инцидент или дело не найдены",
          content = @Content(schema = @Schema(implementation = String.class)))
  })
  public HttpResponse<?> updateCaseForIncident(
      @Parameter(description = "ID инцидента", example = "1") @PathVariable @NotNull Long id,
      @RequestBody(
          description = "Новые параметры дела",
          required = true,
          content = @Content(schema = @Schema(
              implementation = UpdateCaseRequest.class))) @Body @Valid UpdateCaseRequest updateCaseRequest,
      Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      CaseEntity caseEntity =
          caseService.updateCase(
              userId, id, updateCaseRequest.title(), updateCaseRequest.description(),
              updateCaseRequest.status(), updateCaseRequest.users());
      notificationService.addNotification(userId, "PAI Notification",
          "Case %s updated for incident #%d".formatted(caseEntity.getTitle(), id));
      return HttpResponse.ok(caseMapper.toDto(caseEntity));
    } catch (NoSuchElementException | IllegalArgumentException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Удалить инцидент
   */
  @RequiresPermissions({ "INCIDENTS:*:DELETE" })
  @Delete("/{id}")
  @Operation(
      summary = "Удалить инцидент",
      description = "Удаляет инцидент по идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "204",
          description = "Инцидент успешно удалён",
          content = @Content(schema = @Schema(implementation = Object.class))),
      @ApiResponse(responseCode = "400",
          description = "Ошибка удаления (инцидент не найден или уже удалён)",
          content = @Content(schema = @Schema(implementation = String.class)))
  })
  public HttpResponse<?> deleteIncident(
      @Parameter(description = "ID инцидента", example = "1") @PathVariable @NotNull Long id,
      Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      incidentService.deleteIncident(id);
      notificationService.addNotification(userId, "PAI Notification",
          "Incident #%d deleted".formatted(id));
      return HttpResponse.noContent();
    } catch (NoSuchElementException | IllegalStateException e) {
      return HttpResponse.badRequest().body(e.getMessage());
    }
  }

  /**
   * Получить список инцидентов
   */
  @RequiresPermissions({ "INCIDENTS:*:READ" })
  @Get
  @Operation(
      summary = "Получить список инцидентов",
      description = "Возвращает все инциденты")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Список инцидентов",
          content = @Content(
              array = @ArraySchema(schema = @Schema(implementation = IncidentMainInfoDto.class))))
  })
  public HttpResponse<List<IncidentMainInfoDto>> getIncidents(Authentication authentication) {
    List<IncidentEntity> allIncidents = incidentService.getAllIncidents();
    return HttpResponse.ok(incidentMainInfoMapper.toDto(allIncidents));
  }

  /**
   * Получить информацию об инциденте по id
   */
  @RequiresPermissions({ "INCIDENTS:*:READ", "INCIDENTS:{id}:READ" })
  @Get("/{id}")
  @Operation(
      summary = "Получить инцидент по ID",
      description = "Возвращает подробную информацию об инциденте")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Инцидент найден",
          content = @Content(schema = @Schema(implementation = IncidentDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Инцидент не найден",
          content = @Content(schema = @Schema(implementation = String.class)))
  })
  public HttpResponse<?> getIncident(
      @Parameter(description = "ID инцидента", example = "1") @PathVariable @NotNull Long id,
      Authentication authentication) {
    try {
      return HttpResponse.ok(incidentMapper.toDto(incidentService.getIncident(id)));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

}
