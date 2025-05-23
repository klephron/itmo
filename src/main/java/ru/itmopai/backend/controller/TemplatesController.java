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

@Controller("/templates")
public class TemplatesController {

  /**
   * Get template
   */
  @Get("/{id}")
  public HttpResponse<Object> getTemplate(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Update template (generic)
   */
  @Put("/{id}")
  public HttpResponse<Object> updateTemplate(@PathVariable @NotNull Long id, @Body Object template) {
    return HttpResponse.ok();
  }

  /**
   * Delete template
   */
  @Delete("/{id}")
  public HttpResponse<Object> deleteTemplate(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Publish template in Central Database
   */
  @Post("/{id}/cdb")
  public HttpResponse<Object> addTemplateToCDB(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

}
