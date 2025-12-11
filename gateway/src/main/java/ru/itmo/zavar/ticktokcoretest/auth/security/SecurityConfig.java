package ru.itmo.zavar.ticktokcoretest.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.itmo.zavar.ticktokcoretest.auth.security.jwt.JwtAuthenticationFilter;
import ru.itmo.zavar.ticktokcoretest.common.security.Role;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()

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
                        .requestMatchers("/streaming/video/**").permitAll()

                        .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())
                        .requestMatchers("/moderation/**").hasAnyRole(Role.MODERATOR.name(), Role.ADMIN.name())

                        .requestMatchers("/", "/index.html", "/static/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
