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
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.annotation.permission.RequiresPermissions;
import ru.itmopai.backend.dto.UserCaseDto;
import ru.itmopai.backend.dto.UserDto;
import ru.itmopai.backend.dto.request.LocationUpdateRequest;
import ru.itmopai.backend.dto.request.RegisterRequest;
import ru.itmopai.backend.dto.request.UserPermissionsRequest;
import ru.itmopai.backend.dto.response.UserInfoResponse;
import ru.itmopai.backend.dto.response.UserPermissionsResponse;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.exception.UserExistsException;
import ru.itmopai.backend.mapper.CaseMainInfoMapper;
import ru.itmopai.backend.mapper.UserCaseMapper;
import ru.itmopai.backend.mapper.UserInfoMapper;
import ru.itmopai.backend.mapper.UserMapper;
import ru.itmopai.backend.service.CaseService;
import ru.itmopai.backend.service.UserService;

@Validated
@Controller("/users")
@AllArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
public class UsersController {
  private final UserService userService;
  private final UserMapper userMapper;
  private final UserInfoMapper userInfoMapper;
  private final CaseService caseService;
  private final CaseMainInfoMapper caseMainInfoMapper;
  private final UserCaseMapper userCaseMapper;

  /**
   * Получить всех пользователей
   */
  @RequiresPermissions({ "USERS:*:READ" })
  @Get
  @Operation(
      summary = "Получить список пользователей",
      description = "Возвращает список всех пользователей")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Список пользователей",
          content = @Content(
              array = @ArraySchema(schema = @Schema(implementation = UserInfoResponse.class))))
  })
  public HttpResponse<List<UserInfoResponse>> getUsers(Authentication authentication) {
    return HttpResponse.ok(userInfoMapper.toDto(userService.getAllUsers()));
  }

  /**
   * Получить пользователя по ID
   */
  @RequiresPermissions({ "USERS:*:READ" })
  @Get("{id}")
  @Operation(
      summary = "Получить пользователя по ID",
      description = "Возвращает информацию о пользователе по идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Пользователь найден",
          content = @Content(schema = @Schema(implementation = UserDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Пользователь не найден")
  })
  public HttpResponse<UserDto> getUser(
      @Parameter(description = "ID пользователя", example = "1") @PathVariable @NotNull Long id,
      Authentication authentication) {
    Optional<UserEntity> user = userService.getUser(id);
    if (user.isPresent()) {
      return HttpResponse.ok(userMapper.toDto(user.get()));
    } else {
      return HttpResponse.notFound();
    }
  }

  /**
   * Удалить пользователя
   */
  @RequiresPermissions({ "USERS:*:DELETE" })
  @Delete("{id}")
  @Operation(
      summary = "Удалить пользователя",
      description = "Удаляет пользователя по идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "204",
          description = "Пользователь успешно удалён"),
      @ApiResponse(responseCode = "404",
          description = "Пользователь не найден")
  })
  public HttpResponse<Void> deleteUser(
      @Parameter(description = "ID пользователя", example = "1") @PathVariable @NotNull Long id,
      Authentication authentication) {
    userService.removeUser(id);
    return HttpResponse.noContent();
  }

  /**
   * Добавить пользователя
   */
  @RequiresPermissions({ "USERS:*:ADD" })
  @Post
  @Operation(
      summary = "Добавить пользователя",
      description = "Регистрирует нового пользователя")
  @ApiResponses({
      @ApiResponse(responseCode = "201",
          description = "Пользователь успешно создан",
          content = @Content(schema = @Schema(implementation = UserDto.class))),
      @ApiResponse(responseCode = "409",
          description = "Пользователь с таким логином уже существует")
  })
  public HttpResponse<UserDto> addUser(
      @RequestBody(
          description = "Данные для регистрации пользователя",
          required = true,
          content = @Content(schema = @Schema(
              implementation = RegisterRequest.class))) @Body @Valid RegisterRequest registerRequest,
      Authentication authentication) {
    try {
      UserDto user =
          userMapper
              .toDto(userService.register(registerRequest.username(), registerRequest.password(),
                  registerRequest.name(), registerRequest.surname(), registerRequest.patronymic(),
                  registerRequest.primaryPosition()));
      return HttpResponse.created(user);
    } catch (UserExistsException e) {
      return HttpResponse.status(HttpStatus.CONFLICT);
    }
  }

  /**
   * Обновить местоположение пользователя
   */
  @RequiresPermissions({ "USERS:*:UPDATE", "USERS:{id}:UPDATE" })
  @Put("{id}/location")
  @Operation(
      summary = "Обновить местоположение пользователя",
      description = "Обновляет координаты пользователя по идентификатору")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Местоположение успешно обновлено",
          content = @Content(schema = @Schema(implementation = UserDto.class))),
      @ApiResponse(responseCode = "404",
          description = "Пользователь не найден")
  })
  public HttpResponse<UserDto> updateUserLocation(
      @Parameter(description = "ID пользователя", example = "1") @PathVariable @NotNull Long id,
      @RequestBody(
          description = "Новые координаты пользователя",
          required = true,
          content = @Content(schema = @Schema(
              implementation = LocationUpdateRequest.class))) @Body @Valid LocationUpdateRequest locationUpdateRequest,
      Authentication authentication) {
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
   * Получить дела пользователя
   */
  @RequiresPermissions({ "USERS:*:READ" })
  @Get("{id}/cases")
  @Operation(
      summary = "Получить дела пользователя",
      description = "Возвращает список дел, связанных с пользователем")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Список дел пользователя",
          content = @Content(
              array = @ArraySchema(schema = @Schema(implementation = UserCaseDto.class))))
  })
  public HttpResponse<List<UserCaseDto>> getUserCases(
      @Parameter(description = "ID пользователя", example = "1") @PathVariable @NotNull Long id,
      Authentication authentication) {
    return HttpResponse.ok(userCaseMapper.toDto(caseService.getAllUserCasesByUserId(id)));
  }

  /**
   * Получить права пользователя
   */
  @RequiresPermissions({ "USERS:*:READ" })
  @Get("{id}/privileges")
  @Operation(
      summary = "Получить права пользователя",
      description = "Возвращает список привилегий пользователя")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Права пользователя",
          content = @Content(schema = @Schema(implementation = UserPermissionsResponse.class))),
      @ApiResponse(responseCode = "404",
          description = "Пользователь не найден")
  })
  public HttpResponse<UserPermissionsResponse> getUserPermissions(
      @Parameter(description = "ID пользователя", example = "1") @PathVariable @NotNull Long id,
      Authentication authentication) {
    try {
      List<String> userPermissions = userService.getUserPermissions(id);
      return HttpResponse.ok(new UserPermissionsResponse(userPermissions));
    } catch (NoSuchElementException e) {
      return HttpResponse.status(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Обновить права пользователя
   */
  @RequiresPermissions({ "USERS:*:UPDATE" })
  @Put("{id}/privileges")
  @Operation(
      summary = "Обновить права пользователя",
      description = "Изменяет список прав пользователя")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Права пользователя обновлены",
          content = @Content(schema = @Schema(implementation = UserPermissionsResponse.class))),
      @ApiResponse(responseCode = "404",
          description = "Пользователь не найден")
  })
  public HttpResponse<UserPermissionsResponse> updateUserPermissions(
      @Parameter(description = "ID пользователя", example = "1") @PathVariable @NotNull Long id,
      @RequestBody(
          description = "Новый список прав пользователя",
          required = true,
          content = @Content(schema = @Schema(
              implementation = UserPermissionsRequest.class))) @Body UserPermissionsRequest userPermissionsRequest,
      Authentication authentication) {
    try {
      userService.setUserPermissions(id, userPermissionsRequest.permissions());
      return HttpResponse.ok(new UserPermissionsResponse(userService.getUserPermissions(id)));
    } catch (NoSuchElementException e) {
      return HttpResponse.status(HttpStatus.NOT_FOUND);
    }
  }

}
