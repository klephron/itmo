package ru.itmopai.backend.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import ru.itmopai.backend.dto.UserDto;
import ru.itmopai.backend.dto.request.RegisterRequest;
import ru.itmopai.backend.mapper.UserMapper;
import ru.itmopai.backend.service.UserService;

@Controller("/test")
public class TestController {
  private final UserService service;
  private final UserMapper userMapper;

  public TestController(UserService service, UserMapper userMapper) {
    this.service = service;
    this.userMapper = userMapper;
  }

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
}
