package ru.itmo.zavar.brainstop.storage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI storageOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TickTok Storage API")
                        .description("Internal storage facade API (processed video, thumbnails, avatars)")
                        .version("v1"));
    }
}
