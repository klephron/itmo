package ru.itmopai.backend.controller;

import java.util.NoSuchElementException;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.ClueDto;
import ru.itmopai.backend.dto.request.AddUpdateClueRequest;
import ru.itmopai.backend.entity.ClueEntity;
import ru.itmopai.backend.mapper.ClueMainInfoMapper;
import ru.itmopai.backend.mapper.ClueMapper;
import ru.itmopai.backend.service.ClueService;

@Controller("/clues")
@AllArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class CluesController {
  private final ClueService clueService;
  private final ClueMainInfoMapper clueMainInfoMapper;
  private final ClueMapper clueMapper;

  /**
   * Get clue
   */
  @Get("/{id}")
  public HttpResponse<ClueDto> getClue(@PathVariable @NotNull Long id) {
    try {
      return HttpResponse.ok(clueMapper.toDto(clueService.getClue(id)));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound();
    }
  }

  /**
   * Update clue (for example approve)
   */
  @Put("/{id}")
  public HttpResponse<ClueDto> updateClue(@PathVariable @NotNull Long id,
      @Body AddUpdateClueRequest addUpdateClueRequest, Authentication authentication) {
    Long userId = (Long) authentication.getAttributes().get("id");
    try {
      ClueEntity clueEntity =
          clueService.updateClue(userId, id, addUpdateClueRequest.title(),
              addUpdateClueRequest.description(), addUpdateClueRequest.status() == null ? null
                  : ClueEntity.Status.valueOf(addUpdateClueRequest.status()));
      return HttpResponse.ok(clueMapper.toDto(clueEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.notFound();
    }
  }

  /**
   * Delete clue
   */
  @Delete("/{id}")
  public HttpResponse<Object> deleteClue(@PathVariable @NotNull Long id) {
    clueService.deleteClue(id);
    return HttpResponse.noContent();
  }

}
