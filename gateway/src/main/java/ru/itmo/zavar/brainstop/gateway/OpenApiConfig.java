package ru.itmo.zavar.brainstop.gateway;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TickTok API шлюза")
                        .description("Шлюз API (маршрутизация, авторизация) и агрегированный Swagger UI")
                        .version("v1"));
    }
}
