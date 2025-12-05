package ru.itmo.zavar.ticktokcoretest.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.itmo.zavar.ticktokcoretest.security.jwt.JwtAuthenticationFilter;

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
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/users/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/me/avatar").authenticated()

                        .requestMatchers(HttpMethod.GET,"/api/videos/*/comments").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/videos/*/comments").authenticated()
                        .requestMatchers(HttpMethod.DELETE,"/api/videos/*/comments").authenticated()
                        .requestMatchers(HttpMethod.GET,"/api/videos/*/likes").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/videos/*/likes").authenticated()
                        .requestMatchers(HttpMethod.DELETE,"/api/videos/*/likes").authenticated()
                        .requestMatchers(HttpMethod.GET,"/api/videos/*/view").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/videos/*/view").authenticated()
                        .requestMatchers("/api/users/*/follow").authenticated()
                        .requestMatchers("/api/users/*/followers").permitAll()
                        .requestMatchers("/api/users/*/following").permitAll()

                        .requestMatchers("/api/videos/**").permitAll()
                        .requestMatchers("/streaming/video/**").permitAll()

                        .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())
                        .requestMatchers("/moderation/**").hasRole(Role.MODERATOR.name())

                        .requestMatchers("/", "/index.html", "/static/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("""
            ROLE_ADMIN > ROLE_MODERATOR
            ROLE_MODERATOR > ROLE_USER
        """);
        return hierarchy;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(RoleHierarchy roleHierarchy) {
        return (web) -> {
            DefaultWebSecurityExpressionHandler handler =
                    new DefaultWebSecurityExpressionHandler();
            handler.setRoleHierarchy(roleHierarchy);
            web.expressionHandler(handler);  // <— аналог старого .expressionHandler()
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}

