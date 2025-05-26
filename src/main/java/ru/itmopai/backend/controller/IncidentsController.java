package ru.itmopai.backend.controller;

import java.util.List;
import java.util.NoSuchElementException;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.annotation.permission.RequiresPermissions;
import ru.itmopai.backend.dto.IncidentMainInfoDto;
import ru.itmopai.backend.dto.request.AddCaseRequest;
import ru.itmopai.backend.dto.request.AddUpdateIncidentRequest;
import ru.itmopai.backend.dto.request.UpdateCaseRequest;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.IncidentEntity;
import ru.itmopai.backend.mapper.CaseMapper;
import ru.itmopai.backend.mapper.IncidentMainInfoMapper;
import ru.itmopai.backend.mapper.IncidentMapper;
import ru.itmopai.backend.service.CaseService;
import ru.itmopai.backend.service.IncidentService;
import ru.itmopai.backend.service.NotificationService;

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
   * Create incident
   */
  @RequiresPermissions({ "INCIDENTS:*:ADD" })
  @Post
  public HttpResponse<?> addIncident(@Body AddUpdateIncidentRequest request,
      Authentication auth) {
    Long userId = (Long) auth.getAttributes().get("id");
    try {
      IncidentEntity incidentEntity =
          incidentService.addIncident(userId, request.applicant(), request.type(), request.reason(),
              request.locationLatitude(), request.locationLongitude(), request.locationAltitude(),
              request.estimatedTime());
      notificationService.addNotification(userId, "PAI Notification", "New incident created");
      return HttpResponse.ok(incidentMainInfoMapper.toDto(incidentEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Update incident
   */
  @RequiresPermissions({ "INCIDENTS:*:UPDATE" })
  @Put("/{id}")
  public HttpResponse<?> updateIncident(@PathVariable @NotNull Long id,
      @Body AddUpdateIncidentRequest request, Authentication auth) {
    Long userId = (Long) auth.getAttributes().get("id");
    try {
      IncidentEntity incidentEntity =
          incidentService.updateIncident(userId, id, request.applicant(), request.type(),
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
   * Create case for incident
   */
  @RequiresPermissions({ "INCIDENTS:*:UPDATE" })
  @Post("/{id}/case")
  public HttpResponse<?> addCaseForIncident(@PathVariable @NotNull Long id,
      @Body AddCaseRequest addCaseRequest, Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      CaseEntity caseEntity =
          caseService.addCase(userId, addCaseRequest.title(), addCaseRequest.description(), id,
              addCaseRequest.users());
      notificationService.addNotification(userId, "PAI Notification",
          "Case %s created for incident #%d".formatted(addCaseRequest.title(), id));
      return HttpResponse.ok(caseMapper.toDto(caseEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Update case for incident
   */
  @RequiresPermissions({ "INCIDENTS:*:UPDATE" })
  @Put("/{id}/case")
  public HttpResponse<?> updateCaseForIncident(@PathVariable @NotNull Long id,
      @Body UpdateCaseRequest updateCaseRequest, Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      CaseEntity caseEntity =
          caseService.updateCase(userId, id, updateCaseRequest.title(),
              updateCaseRequest.description(),
              updateCaseRequest.status() == null ? null
                  : CaseEntity.Status.valueOf(updateCaseRequest.status()),
              updateCaseRequest.users());
      notificationService.addNotification(userId, "PAI Notification",
          "Case %s updated for incident #%d".formatted(caseEntity.getTitle(), id));
      return HttpResponse.ok(caseMapper.toDto(caseEntity));
    } catch (NoSuchElementException | IllegalArgumentException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Delete incident
   */
  @RequiresPermissions({ "INCIDENTS:*:DELETE" })
  @Delete("/{id}")
  public HttpResponse<?> deleteIncident(@PathVariable @NotNull Long id,
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
   * Get incidents
   */
  @RequiresPermissions({ "INCIDENTS:*:READ" })
  @Get
  public HttpResponse<List<IncidentMainInfoDto>> getIncidents(Authentication authentication) {
    List<IncidentEntity> allIncidents = incidentService.getAllIncidents();
    return HttpResponse.ok(incidentMainInfoMapper.toDto(allIncidents));
  }

  /**
   * Get incident
   */
  @RequiresPermissions({ "INCIDENTS:*:READ", "INCIDENTS:{id}:READ" })
  @Get("/{id}")
  public HttpResponse<?> getIncident(@PathVariable @NotNull Long id,
      Authentication authentication) {
    try {
      return HttpResponse.ok(incidentMapper.toDto(incidentService.getIncident(id)));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

}
