package ru.itmo.zavar.brainstop.interactions.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI interactionsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BrainStop API интеракций")
                        .description("Лайки, просмотры, комментарии и жалобы (interactions-service)")
                        .version("v1"));
    }
}
