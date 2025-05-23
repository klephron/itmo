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

@Controller("/users")
public class UsersController {

  /**
   * Get users
   */
  @Get
  public HttpResponse<Object> getUsers() {
    return HttpResponse.ok();
  }

  /**
   * Get user
   */
  @Get("{id}")
  public HttpResponse<Object> getUser(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Delete user
   */
  @Delete("{id}")
  public HttpResponse<Object> deleteUser(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Add user
   */
  @Post
  public HttpResponse<Object> addUser(@Body Object user) {
    return HttpResponse.ok();
  }

  /**
   * Update user location
   */
  @Put("{id}/location")
  public HttpResponse<Object> updateUserLocation(@PathVariable @NotNull Long id,
      @Body Object location) {
    return HttpResponse.ok();
  }

  /**
   * Get user cases
   */
  @Get("{id}/cases")
  public HttpResponse<Object> getUserCases(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Get user privileges
   */
  @Get("{id}/privileges")
  public HttpResponse<Object> getUserPrivileges(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Update user privileges
   */
  @Put("{id}/privileges")
  public HttpResponse<Object> updateUserPrivileges(@PathVariable @NotNull Long id,
      @Body Object privileges) {
    return HttpResponse.ok();
  }

}
