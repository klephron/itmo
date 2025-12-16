package ru.itmo.zavar.brainstop.profile.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI profileOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TickTok Profile API")
                        .description("Profile service API (users, follows, roles, avatars)")
                        .version("v1"));
    }
}
