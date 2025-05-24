package ru.itmopai.backend.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.UserDto;
import ru.itmopai.backend.dto.request.LocationUpdateRequest;
import ru.itmopai.backend.dto.request.RegisterRequest;
import ru.itmopai.backend.dto.request.UserPermissionsResponse;
import ru.itmopai.backend.dto.response.UserInfoResponse;
import ru.itmopai.backend.dto.response.UserPermissionsRequest;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.exception.UserExistsException;
import ru.itmopai.backend.mapper.UserInfoMapper;
import ru.itmopai.backend.mapper.UserMapper;
import ru.itmopai.backend.service.UserService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Controller("/users")
@AllArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class UsersController {
  private final UserService userService;
  private final UserMapper userMapper;
  private final UserInfoMapper userInfoMapper;

  /**
   * Get users
   */
  @Get
  public HttpResponse<List<UserInfoResponse>> getUsers() {
    return HttpResponse.ok(userInfoMapper.toDto(userService.getAllUsers()));
  }

  /**
   * Get user
   */
  @Get("{id}")
  public HttpResponse<UserDto> getUser(@PathVariable @NotNull Long id) {
    Optional<UserEntity> user = userService.getUser(id);
    if (user.isPresent()) {
      return HttpResponse.ok(userMapper.toDto(user.get()));
    } else {
      return HttpResponse.notFound();
    }
  }

  /**
   * Delete user
   */
  @Delete("{id}")
  public HttpResponse<Void> deleteUser(@PathVariable @NotNull Long id) {
    userService.removeUser(id);
    return HttpResponse.ok();
  }

  /**
   * Add user
   */
  @Post
  public HttpResponse<UserDto> addUser(@Body RegisterRequest registerRequest) {
    try {
      UserDto user =
          userMapper
              .toDto(userService.register(registerRequest.username(), registerRequest.password(),
                  registerRequest.name(), registerRequest.surname(), registerRequest.patronymic()));
      return HttpResponse.created(user);
    } catch (UserExistsException e) {
      return HttpResponse.status(HttpStatus.CONFLICT);
    }
  }

  /**
   * Update user location
   */
  @Put("{id}/location")
  public HttpResponse<UserDto> updateUserLocation(@PathVariable @NotNull Long id,
      @Body LocationUpdateRequest locationUpdateRequest) {
    try {
      UserEntity userEntity =
          userService.updateLocation(id, locationUpdateRequest.latitude(),
              locationUpdateRequest.longitude(), locationUpdateRequest.altitude());
      return HttpResponse.ok(userMapper.toDto(userEntity));
    } catch (NoSuchElementException e) {
      return HttpResponse.status(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Get user cases
   */
  // TODO
  @Get("{id}/cases")
  public HttpResponse<Void> getUserCases(@PathVariable @NotNull Long id) {
    return HttpResponse.ok();
  }

  /**
   * Get user privileges
   */
  @Get("{id}/privileges")
  public HttpResponse<UserPermissionsResponse> getUserPrivileges(@PathVariable @NotNull Long id) {
    try {
      List<String> userPermissions = userService.getUserPermissions(id);
      return HttpResponse.ok(new UserPermissionsResponse(userPermissions));
    } catch (NoSuchElementException e) {
      return HttpResponse.status(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Update user privileges
   */
  @Put("{id}/privileges")
  public HttpResponse<Object> updateUserPrivileges(@PathVariable @NotNull Long id,
      @Body UserPermissionsRequest userPermissionsRequest) {
    try {
      userService.setUserPermissions(id, userPermissionsRequest.permissions());
      return HttpResponse.ok(new UserPermissionsResponse(userService.getUserPermissions(id)));
    } catch (NoSuchElementException e) {
      return HttpResponse.status(HttpStatus.NOT_FOUND);
    }
  }

}
