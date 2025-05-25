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
import ru.itmopai.backend.dto.request.AddUpdateClueRequest;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.ClueEntity;
import ru.itmopai.backend.mapper.CaseMapper;
import ru.itmopai.backend.mapper.ClueMainInfoMapper;
import ru.itmopai.backend.service.CaseService;
import ru.itmopai.backend.service.ClueService;

@Controller("/cases")
@RequiredArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class CasesController {
  private final CaseService caseService;
  private final CaseMapper caseMapper;
  private final ClueService clueService;
  private final ClueMainInfoMapper clueMainInfoMapper;

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
  public HttpResponse<List<ClueMainInfoDto>> getCaseClues(@PathVariable @NotNull Long id) {
    return HttpResponse.ok(clueMainInfoMapper.toDto(clueService.getAllClueByCase(id)));
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
