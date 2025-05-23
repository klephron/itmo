package ru.itmopai.backend.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import jakarta.validation.constraints.NotNull;

@Controller("/incidents")
public class IncidentsController {

  /**
   * Create incident
   */
  @Post
  public HttpResponse<Object> addIncident(@Body Object incident) {
    return HttpResponse.ok();
  }

  /**
   * Update incident
   */
  @Put("/{id}")
  public HttpResponse<Object> updateIncident(@PathVariable @NotNull Long id,
      @Body Object incident) {
    return HttpResponse.ok();
  }

  /**
   * Create case for incident
   */
  @Post("/{id}/case")
  public HttpResponse<Object> addCaseForIncident(@PathVariable @NotNull Long id,
      @Body Object incidentCase) {
    return HttpResponse.ok();
  }

  /**
   * Update case for incident
   */
  @Put("/{id}/case")
  public HttpResponse<Object> updateCaseForIncident(@PathVariable @NotNull Long id,
      @Body Object incidentCase) {
    return HttpResponse.ok();
  }

  /**
   * Delete incident
   */
  @Delete("/{id}")
  public HttpResponse<Object> deleteIncident(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Get incidents
   */
  @Get
  public HttpResponse<Object> getIncidents() {
    return HttpResponse.ok();
  }

  /**
   * Get incident
   */
  @Get("/{id}")
  public HttpResponse<Object> getIncident(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

}
