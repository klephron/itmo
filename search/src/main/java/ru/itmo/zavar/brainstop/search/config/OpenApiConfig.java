package ru.itmo.zavar.brainstop.search.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI searchOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BrainStop API поиска")
                        .description("Поиск, подсказки и эмбеддинги (search-service)")
                        .version("v1"));
    }
}
