package ru.itmopai.backend.controller;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.annotation.permission.RequiresPermissions;
import ru.itmopai.backend.dto.CaseMainInfoDto;
import ru.itmopai.backend.dto.UserDto;
import ru.itmopai.backend.dto.request.LocationUpdateRequest;
import ru.itmopai.backend.dto.request.RegisterRequest;
import ru.itmopai.backend.dto.request.UserPermissionsRequest;
import ru.itmopai.backend.dto.response.UserInfoResponse;
import ru.itmopai.backend.dto.response.UserPermissionsResponse;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.exception.UserExistsException;
import ru.itmopai.backend.mapper.CaseMainInfoMapper;
import ru.itmopai.backend.mapper.UserInfoMapper;
import ru.itmopai.backend.mapper.UserMapper;
import ru.itmopai.backend.service.CaseService;
import ru.itmopai.backend.service.UserService;

@Controller("/users")
@AllArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class UsersController {
  private final UserService userService;
  private final UserMapper userMapper;
  private final UserInfoMapper userInfoMapper;
  private final CaseService caseService;
  private final CaseMainInfoMapper caseMainInfoMapper;

  /**
   * Get users
   */
  @RequiresPermissions({ "USERS:*:READ" })
  @Get
  public HttpResponse<List<UserInfoResponse>> getUsers(Authentication authentication) {
    return HttpResponse.ok(userInfoMapper.toDto(userService.getAllUsers()));
  }

  /**
   * Get user
   */
  @RequiresPermissions({ "USERS:*:READ" })
  @Get("{id}")
  public HttpResponse<UserDto> getUser(@PathVariable @NotNull Long id,
      Authentication authentication) {
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
  @RequiresPermissions({ "USERS:*:DELETE" })
  @Delete("{id}")
  public HttpResponse<Void> deleteUser(@PathVariable @NotNull Long id,
      Authentication authentication) {
    userService.removeUser(id);
    return HttpResponse.noContent();
  }

  /**
   * Add user
   */
  @RequiresPermissions({ "USERS:*:ADD" })
  @Post
  public HttpResponse<UserDto> addUser(@Body RegisterRequest registerRequest,
      Authentication authentication) {
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
  @RequiresPermissions({ "USERS:*:UPDATE", "USERS:{id}:UPDATE" })
  @Put("{id}/location")
  public HttpResponse<UserDto> updateUserLocation(@PathVariable @NotNull Long id,
      @Body LocationUpdateRequest locationUpdateRequest, Authentication authentication) {
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
  @RequiresPermissions({ "USERS:*:READ" })
  @Get("{id}/cases")
  public HttpResponse<List<CaseMainInfoDto>> getUserCases(@PathVariable @NotNull Long id,
      Authentication authentication) {
    return HttpResponse.ok(caseMainInfoMapper.toDto(caseService.getAllCasesByUserId(id)));
  }

  /**
   * Get user privileges
   */
  @RequiresPermissions({ "USERS:*:READ" })
  @Get("{id}/privileges")
  public HttpResponse<UserPermissionsResponse> getUserPrivileges(@PathVariable @NotNull Long id,
      Authentication authentication) {
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
  @RequiresPermissions({ "USERS:*:UPDATE" })
  @Put("{id}/privileges")
  public HttpResponse<Object> updateUserPrivileges(@PathVariable @NotNull Long id,
      @Body UserPermissionsRequest userPermissionsRequest, Authentication authentication) {
    try {
      userService.setUserPermissions(id, userPermissionsRequest.permissions());
      return HttpResponse.ok(new UserPermissionsResponse(userService.getUserPermissions(id)));
    } catch (NoSuchElementException e) {
      return HttpResponse.status(HttpStatus.NOT_FOUND);
    }
  }

}
