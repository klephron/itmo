package ru.itmopai.backend.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Put;
import jakarta.validation.constraints.NotNull;

@Controller("/clues")
public class CluesController {

  /**
   * Get clue
   */
  @Get("/{id}")
  public HttpResponse<Object> getClue(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Update clue (for example approve)
   */
  @Put("/{id}")
  public HttpResponse<Object> updateClue(@PathVariable @NotNull Long id, @Body Object clue) {
    return HttpResponse.ok();
  }

  /**
   * Delete clue
   */
  @Delete("/{id}")
  public HttpResponse<Object> deleteClue(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

}
