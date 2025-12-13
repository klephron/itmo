package ru.itmo.zavar.ticktokcoretest.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import ru.itmo.zavar.ticktokcoretest.common.security.JwtAuthConverter;
import ru.itmo.zavar.ticktokcoretest.common.security.Role;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class ResourceServerConfig {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final JwtAuthConverter jwtAuthConverter;

    public ResourceServerConfig(JwtAuthConverter jwtAuthConverter) {
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/me/avatar").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/videos/*/comments").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/videos/*/comments").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/videos/*/comments").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/videos/*/likes").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/videos/*/likes").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/videos/*/likes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/videos/*/view").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/videos/*/view").authenticated()
                        .requestMatchers("/api/users/*/follow").authenticated()
                        .requestMatchers("/api/users/*/followers").permitAll()
                        .requestMatchers("/api/users/*/following").permitAll()

                        .requestMatchers("/api/search/**").permitAll()

                        .requestMatchers("/api/videos/**").permitAll()
                        .requestMatchers("/api/streaming/video/**").permitAll()

                        .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())
                        .requestMatchers("/api/moderation/**").hasAnyRole(Role.MODERATOR.name(), Role.ADMIN.name())

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthConverter)
                        )
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }
}
