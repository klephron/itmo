package ru.itmo.zavar.brainstop.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI coreOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TickTok Core API")
                        .description("Core service API (videos, streaming, search)")
                        .version("v1"));
    }
}
