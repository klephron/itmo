package ru.itmopai.backend.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import ru.itmopai.backend.dto.CaseDto;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.mapper.CaseMapper;
import ru.itmopai.backend.service.CaseService;

import java.util.NoSuchElementException;

@Controller("/cases")
@RequiredArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class CasesController {
  private final CaseService caseService;
  private final CaseMapper caseMapper;

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
  public HttpResponse<Object> getCaseTemplates(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Get case clues
   */
  @Get("/{id}/clues")
  public HttpResponse<Object> getCaseClues(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Add case template
   */
  @Post("/{id}/templates")
  public HttpResponse<Object> addCaseTemplate(@PathVariable @NotNull Long id,
      @Body Object caseTemplate) {
    return HttpResponse.ok();
  }

  /**
   * Add case clue
   */
  @Post("/{id}/clues")
  public HttpResponse<Object> addCaseClue(@PathVariable @NotNull Long id, @Body Object caseClue) {
    return HttpResponse.ok();
  }
}
