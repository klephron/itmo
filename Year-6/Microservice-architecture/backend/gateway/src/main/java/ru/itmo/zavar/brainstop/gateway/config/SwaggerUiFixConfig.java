package ru.itmo.zavar.brainstop.gateway.config;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.webflux.ui.SwaggerIndexTransformer;
import org.springdoc.webflux.ui.SwaggerResourceResolver;
import org.springdoc.webflux.ui.SwaggerWebFluxConfigurer;
import org.springdoc.webflux.ui.SwaggerWelcomeCommon;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


// * Overrides Springdoc's WebFlux config to avoid invalid patterns like "/swagger-ui/**/*swagger-initializer.js"
// * when running with the strict PathPatternParser used by Spring Boot 3.4+.

@Configuration
public class SwaggerUiFixConfig {

    @Bean(name = "swaggerWebFluxConfigurer")
    public SwaggerWebFluxConfigurer swaggerWebFluxConfigurer(
            SwaggerUiConfigProperties swaggerUiConfigProperties,
            WebProperties webProperties,
            WebFluxProperties webFluxProperties,
            SwaggerIndexTransformer swaggerIndexTransformer,
            SwaggerResourceResolver swaggerResourceResolver,
            SwaggerWelcomeCommon swaggerWelcomeCommon
    ) {
        return new SafeSwaggerWebFluxConfigurer(
                swaggerUiConfigProperties,
                webProperties,
                webFluxProperties,
                swaggerIndexTransformer,
                swaggerResourceResolver,
                swaggerWelcomeCommon
        );
    }

    private static final class SafeSwaggerWebFluxConfigurer extends SwaggerWebFluxConfigurer {

        private final SwaggerUiConfigProperties uiConfig;

        SafeSwaggerWebFluxConfigurer(
                SwaggerUiConfigProperties swaggerUiConfigProperties,
                WebProperties webProperties,
                WebFluxProperties springWebFluxProperties,
                SwaggerIndexTransformer swaggerIndexTransformer,
                SwaggerResourceResolver swaggerResourceResolver,
                SwaggerWelcomeCommon swaggerWelcomeCommon
        ) {
            super(
                    swaggerUiConfigProperties,
                    webProperties,
                    springWebFluxProperties,
                    swaggerIndexTransformer,
                    swaggerResourceResolver,
                    swaggerWelcomeCommon
            );
            this.uiConfig = swaggerUiConfigProperties;
        }

        @Override
        protected SwaggerResourceHandlerConfig[] getSwaggerHandlerConfigs() {
            String uiPath = uiConfig.getPath();
            String uiBase = resolveUiBase(uiPath);
            String uiPattern = uiBase.isEmpty() ? "/swagger-ui/**" : uiBase + "/**";
            String initializerPattern = uiBase.isEmpty() ? "/swagger-ui/swagger-initializer.js" : uiBase + "/swagger-initializer.js";
            String uiLocation = String.format("classpath:/META-INF/resources/webjars/swagger-ui/%s/", uiConfig.getVersion());

            return new SwaggerResourceHandlerConfig[]{
                    SwaggerResourceHandlerConfig.createCached()
                            .setPatterns(new String[]{uiPattern, uiPath})
                            .setLocations(new String[]{uiLocation}),
                    SwaggerResourceHandlerConfig.createUncached()
                            .setPatterns(new String[]{initializerPattern})
                            .setLocations(new String[]{uiLocation})
            };
        }

        @Override
        protected SwaggerResourceHandlerConfig[] getSwaggerWebjarHandlerConfigs() {
            String webjarsLocation = "classpath:/META-INF/resources/webjars/";
            return new SwaggerResourceHandlerConfig[]{
                    SwaggerResourceHandlerConfig.createCached()
                            .setPatterns(new String[]{"/webjars/**"})
                            .setLocations(new String[]{webjarsLocation})
            };
        }

        private String resolveUiBase(String uiPath) {
            int lastSlash = uiPath.lastIndexOf('/');
            if (lastSlash <= 0) {
                return "";
            }
            return uiPath.substring(0, lastSlash);
        }
    }
}
