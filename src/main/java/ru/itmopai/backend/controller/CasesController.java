package ru.itmopai.backend.controller;

import java.util.List;
import java.util.NoSuchElementException;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import ru.itmopai.backend.dto.CaseDto;
import ru.itmopai.backend.dto.ClueMainInfoDto;
import ru.itmopai.backend.dto.TemplateMainInfoDto;
import ru.itmopai.backend.dto.request.AddTemplateRequest;
import ru.itmopai.backend.dto.request.AddUpdateClueRequest;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.ClueEntity;
import ru.itmopai.backend.entity.TemplateEntity;
import ru.itmopai.backend.mapper.CaseMapper;
import ru.itmopai.backend.mapper.ClueMainInfoMapper;
import ru.itmopai.backend.mapper.TemplateMainInfoMapper;
import ru.itmopai.backend.mapper.TemplateMapper;
import ru.itmopai.backend.service.CaseService;
import ru.itmopai.backend.service.ClueService;
import ru.itmopai.backend.service.TemplateService;

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

  /**
   * Get case
   */
  @Get("/{id}")
  public HttpResponse<CaseDto> getCase(@PathVariable @NotNull Long id) {
    try {
      CaseEntity caseById = caseService.getCaseById(id);
      CaseDto dto = caseMapper.toDto(caseById);
      return HttpResponse.ok(dto);
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound();
    }
  }

  /**
   * Get case templates
   */
  @Get("/{id}/templates")
  public HttpResponse<List<TemplateMainInfoDto>> getCaseTemplates(@PathVariable @NotNull Long id) {
    return HttpResponse.ok(templateMainInfoMapper.toDto(templateService.getTemplatesByCaseId(id)));
  }

  /**
   * Get case clues
   */
  @Get("/{id}/clues")
  public HttpResponse<List<ClueMainInfoDto>> getCaseClues(@PathVariable @NotNull Long id) {
    return HttpResponse.ok(clueMainInfoMapper.toDto(clueService.getAllClueByCase(id)));
  }

  /**
   * Add case template
   */
  @Post("/{id}/templates")
  public HttpResponse<?> addCaseTemplate(@PathVariable @NotNull Long id,
      @Body AddTemplateRequest addTemplateRequest, Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      TemplateEntity templateEntity =
          templateService.addTemplate(userId, id, addTemplateRequest.title(),
              addTemplateRequest.metadata(), addTemplateRequest.description());
      return HttpResponse.ok(templateMapper.toDto(templateEntity));
    } catch (NoSuchElementException | IllegalArgumentException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }

  /**
   * Add case clue
   */
  @Post("/{id}/clues")
  public HttpResponse<?> addCaseClue(@PathVariable @NotNull Long id,
      @Body AddUpdateClueRequest addUpdateClueRequest, Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      ClueEntity clueEntity =
          clueService.addClue(userId,
              addUpdateClueRequest.title(),
              addUpdateClueRequest.description(),
              addUpdateClueRequest.status() == null ? null
                  : ClueEntity.Status.valueOf(addUpdateClueRequest.status()),
              id);
      return HttpResponse.ok(clueMainInfoMapper.toDto(clueEntity));
    } catch (NoSuchElementException | IllegalArgumentException e) {
      return HttpResponse.notFound().body(e.getMessage());
    }
  }
}
