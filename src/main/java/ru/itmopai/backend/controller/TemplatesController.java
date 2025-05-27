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
   * Get template
   */
  @Get("/{id}")
  public HttpResponse<?> getTemplate(@PathVariable @NotNull Long id,
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
   * Update template (generic)
   */
  @Put("/{id}")
  public HttpResponse<?> updateTemplate(@PathVariable @NotNull Long id,
      @Body UpdateTemplateRequest updateTemplateRequest, Authentication authentication) {
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
   * Delete template
   */
  @Delete("/{id}")
  public HttpResponse<?> deleteTemplate(@PathVariable @NotNull Long id,
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
   * Publish template in Central Database
   */
  @Post("/{id}/cdb")
  public HttpResponse<Object> addTemplateToCDB(@PathVariable @NotNull Long id,
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
