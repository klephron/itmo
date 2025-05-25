package ru.itmopai.backend.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.IncidentDto;
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

import java.util.List;
import java.util.NoSuchElementException;

@AllArgsConstructor
@Controller("/incidents")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class IncidentsController {
  private final IncidentService incidentService;
  private final IncidentMainInfoMapper incidentMainInfoMapper;
  private final IncidentMapper incidentMapper;
  private final CaseService caseService;
  private final CaseMapper caseMapper;

  /**
   * Create incident
   */
  @Post
  public HttpResponse<?> addIncident(@Body AddUpdateIncidentRequest request,
      Authentication auth) {
    Long userId = (Long) auth.getAttributes().get("id");
    try {
      IncidentEntity incidentEntity =
          incidentService.addIncident(userId, request.applicant(), request.type(), request.type(),
              request.locationLatitude(), request.locationLongitude(), request.locationAltitude(),
              request.estimatedTime());
      return HttpResponse.ok(incidentMainInfoMapper.toDto(incidentEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Update incident
   */
  @Put("/{id}")
  public HttpResponse<?> updateIncident(@PathVariable @NotNull Long id,
      @Body AddUpdateIncidentRequest request, Authentication auth) {
    Long userId = (Long) auth.getAttributes().get("id");
    try {
      IncidentEntity incidentEntity =
          incidentService.updateIncident(userId, id, request.applicant(), request.type(),
              request.applicant(), request.locationLatitude(), request.locationLongitude(),
              request.locationAltitude(), request.estimatedTime());
      return HttpResponse.ok(incidentMainInfoMapper.toDto(incidentEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Create case for incident
   */
  @Post("/{id}/case")
  public HttpResponse<?> addCaseForIncident(@PathVariable @NotNull Long id,
      @Body AddCaseRequest addCaseRequest, Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      CaseEntity caseEntity =
          caseService.addCase(userId, addCaseRequest.title(), addCaseRequest.description(), id,
              addCaseRequest.users());
      return HttpResponse.ok(caseMapper.toDto(caseEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Update case for incident
   */
  @Put("/{id}/case")
  public HttpResponse<?> updateCaseForIncident(@PathVariable @NotNull Long id,
      @Body UpdateCaseRequest updateCaseRequest, Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      CaseEntity caseEntity =
          caseService.updateCase(userId, id, updateCaseRequest.title(),
              updateCaseRequest.description(),
              CaseEntity.Status.valueOf(updateCaseRequest.status()),
              updateCaseRequest.users());
      return HttpResponse.ok(caseMapper.toDto(caseEntity));
    } catch (NoSuchElementException | IllegalArgumentException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Delete incident
   */
  @Delete("/{id}")
  public HttpResponse<Void> deleteIncident(@PathVariable @NotNull Long id) {
    incidentService.deleteIncident(id);
    return HttpResponse.noContent();
  }

  /**
   * Get incidents
   */
  @Get
  public HttpResponse<List<IncidentMainInfoDto>> getIncidents() {
    List<IncidentEntity> allIncidents = incidentService.getAllIncidents();
    return HttpResponse.ok(incidentMainInfoMapper.toDto(allIncidents));
  }

  /**
   * Get incident
   */
  @Get("/{id}")
  public HttpResponse<?> getIncident(@PathVariable @NotNull Long id) {
    try {
      return HttpResponse.ok(incidentMapper.toDto(incidentService.getIncident(id)));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

}
