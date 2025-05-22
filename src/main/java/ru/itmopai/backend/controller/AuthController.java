package ru.itmopai.backend.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import ru.itmopai.backend.mapper.UserMapper;
import ru.itmopai.backend.dto.UserDto;
import ru.itmopai.backend.dto.request.RegisterRequest;
import ru.itmopai.backend.service.UserService;

@Controller("/auth")
public class AuthController {
  private final UserService service;
  private final UserMapper userMapper;

  public AuthController(UserService service, UserMapper userMapper) {
    this.service = service;
    this.userMapper = userMapper;
  }

  @Post("/register")
  public HttpResponse<UserDto> register(@Body RegisterRequest req) {
    UserDto user = userMapper.toDto(service.register(req.username(), req.password()));
    return HttpResponse.created(user);
  }
}
