package ru.itmo.zavar.brainstop.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.zavar.brainstop.auth.security.jwt.JwtTokenProvider;
import ru.itmo.zavar.brainstop.auth.service.AuthService;
import ru.itmo.zavar.brainstop.common.security.JwtAuthConverter;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@FR({"NFR-1.1", "NFR-1.6"})
@TestType(TestType.Type.BASIC)
@Layer(TestLayer.BE_INT)
class GatewayIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private AuthService authService;

    @Autowired(required = false)
    private UserRepository userRepository;

    @Autowired(required = false)
    private JwtTokenProvider jwtTokenProvider;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JwtAuthConverter jwtAuthConverter;

    @Test
    @DisplayName("BE-INT-GW-01: Spring контекст загружается")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("BE-INT-GW-02: Все основные beans созданы")
    void allBeansExist() {
        assertThat(authService).isNotNull();
        assertThat(userRepository).isNotNull();
        assertThat(jwtTokenProvider).isNotNull();
        assertThat(passwordEncoder).isNotNull();
        assertThat(jwtAuthConverter).isNotNull();
    }

    @Test
    @DisplayName("BE-INT-GW-03: PasswordEncoder работает")
    void passwordEncoderWorks() {
        String raw = "password123";
        String encoded = passwordEncoder.encode(raw);

        assertThat(encoded).isNotNull();
        assertThat(encoded).isNotEqualTo(raw);
        assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
    }

    @Test
    @DisplayName("BE-INT-GW-04: JpaConfig правильно сканирует entities")
    void jpaConfigScansEntities() {
        assertThat(context.getBean("jpaConfig")).isNotNull();
    }

    @Test
    @DisplayName("BE-INT-GW-05: OpenAPI конфигурация загружена")
    void openApiConfigured() {
        assertThat(context.containsBean("gatewayOpenApi")).isTrue();
    }
}
