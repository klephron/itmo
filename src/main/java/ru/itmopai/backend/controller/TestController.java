package ru.itmopai.backend.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import lombok.RequiredArgsConstructor;
import ru.itmopai.backend.annotation.permission.RequiresPermissions;
import ru.itmopai.backend.dto.UserDto;
import ru.itmopai.backend.dto.request.RegisterRequest;
import ru.itmopai.backend.mapper.UserMapper;
import ru.itmopai.backend.service.PermissionService;
import ru.itmopai.backend.service.UserService;

@Controller("/test")
@RequiredArgsConstructor
public class TestController {
  private final UserService service;
  private final UserMapper userMapper;
  private final PermissionService permissionService;

  @Post("/register")
  @Secured(SecurityRule.IS_ANONYMOUS)
  public HttpResponse<UserDto> register(@Body RegisterRequest req) {
    UserDto user = userMapper.toDto(service.register(req.username(), req.password()));
    return HttpResponse.created(user);
  }

  @Get("/hello")
  @Produces(MediaType.TEXT_PLAIN)
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public String index() {
    return "Hello world!";
  }

  @RequiresPermissions({"document:{id}:read"})
  @Get("/check/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public String checkPermissions(@PathVariable Long id, Authentication auth) {
    return "Access granted for user: " + auth.getName();
  }

  @RequiresPermissions({"device:10:edit"})
  @Get("/check2/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public String checkPermissions2(@PathVariable Long id, Authentication auth) {
    return "Access granted for user: " + auth.getName();
  }

  @RequiresPermissions({"document:*:delete"})
  @Get("/check3/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public String checkPermissions3(@PathVariable Long id, Authentication auth) {
    return "Access granted for user: " + auth.getName();
  }

  @RequiresPermissions({"document:*:delete", "document:{id}:view"})
  @Get("/check4/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public String checkPermissions4(@PathVariable Long id, Authentication auth) {
    return "Access granted for user: " + auth.getName();
  }

  @Get("/manual/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public String manualCheck(@PathVariable Long id, Authentication auth) {
    permissionService.checkOrThrow(auth, "document", id, "read");
    return "Access to document " + id + " granted";
  }

}
