package ru.itmopai.backend;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.micronaut.openapi.annotation.OpenAPIInclude;


@OpenAPIInclude(
  classes = {
    io.micronaut.security.endpoints.LoginController.class,
    io.micronaut.security.endpoints.LogoutController.class
  },
  tags = @Tag(name = "Security")
)
@OpenAPIDefinition(
  info = @Info(
    title = "PAI API",
    version = "1.0"
  ),
  security = { @SecurityRequirement(name = "bearerAuth") }
)
@SecurityScheme(
  name = "bearerAuth",
  type = SecuritySchemeType.HTTP,
  scheme = "bearer",
  bearerFormat = "JWT"
)
public class Application {
  public static void main(String[] args) {
    Micronaut.run(Application.class, args);
  }
}
