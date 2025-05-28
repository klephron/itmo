package ru.itmopai.backend.controller;

import java.util.NoSuchElementException;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.request.UpdateCaseSuspectRequest;
import ru.itmopai.backend.mapper.CaseSuspectMapper;
import ru.itmopai.backend.service.CaseSuspectService;

@Validated
@Controller("/suspects")
@AllArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class CaseSuspectController {
  private final CaseSuspectService caseSuspectService;
  private final CaseSuspectMapper caseSuspectMapper;

  @Get("/{id}")
  public HttpResponse<?> getCaseSuspect(@PathVariable @NotNull Long id) {
    try {
      return HttpResponse.ok(caseSuspectMapper.toDto(caseSuspectService.getCaseSuspect(id)));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound(e.getMessage());
    }
  }

  @Delete("/{id}")
  public HttpResponse<?> deleteCaseSuspect(@PathVariable @NotNull Long id) {
    caseSuspectService.deleteCaseSuspect(id);
    return HttpResponse.ok();
  }

  @Post("/{id}")
  public HttpResponse<?> updateCaseSuspect(@PathVariable @NotNull Long id,
      @Body @Valid UpdateCaseSuspectRequest updateCaseSuspectRequest,
      Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      return HttpResponse
          .ok(caseSuspectService.updateCaseSuspect(id, userId, updateCaseSuspectRequest.name(),
              updateCaseSuspectRequest.surname(), updateCaseSuspectRequest.patronymic(),
              updateCaseSuspectRequest.identifier(), updateCaseSuspectRequest.status(),
              updateCaseSuspectRequest.verdict(), updateCaseSuspectRequest.accusation()));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound(e.getMessage());
    }
  }

}
