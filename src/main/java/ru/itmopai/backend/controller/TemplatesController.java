package ru.itmopai.backend.controller;

import java.util.NoSuchElementException;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.request.UpdateTemplateRequest;
import ru.itmopai.backend.entity.TemplateEntity;
import ru.itmopai.backend.mapper.TemplateMapper;
import ru.itmopai.backend.service.TemplateService;

@Controller("/templates")
@AllArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class TemplatesController {
  private final TemplateService templateService;
  private final TemplateMapper templateMapper;

  /**
   * Get template
   */
  @Get("/{id}")
  public HttpResponse<?> getTemplate(@PathVariable @NotNull Long id) {
    try {
      return HttpResponse.ok(templateMapper.toDto(templateService.getTemplate(id)));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Update template (generic)
   */
  @Put("/{id}")
  public HttpResponse<Object> updateTemplate(@PathVariable @NotNull Long id,
      @Body UpdateTemplateRequest updateTemplateRequest, Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      TemplateEntity templateEntity =
          templateService.updateTemplate(userId, id, updateTemplateRequest.title(),
              updateTemplateRequest.status() == null ? null
                  : TemplateEntity.Status.valueOf(updateTemplateRequest.status()),
              updateTemplateRequest.metadata(), updateTemplateRequest.description());
      return HttpResponse.ok(templateMapper.toDto(templateEntity));
    } catch (NoSuchElementException | IllegalArgumentException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Delete template
   */
  @Delete("/{id}")
  public HttpResponse<Void> deleteTemplate(@PathVariable @NotNull Long id) {
    templateService.deleteTemplate(id);
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
