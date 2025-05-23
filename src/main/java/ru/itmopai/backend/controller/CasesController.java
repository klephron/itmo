package ru.itmopai.backend.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Controller("/cases")
@RequiredArgsConstructor
public class CasesController {

  /**
   * Get case
   */
  @Get("/{id}")
  public HttpResponse<Object> getCase(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
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
