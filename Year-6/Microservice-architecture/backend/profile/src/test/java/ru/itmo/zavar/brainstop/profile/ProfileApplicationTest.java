package ru.itmo.zavar.brainstop.profile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.profile.controller.FollowController;
import ru.itmo.zavar.brainstop.profile.controller.ProfileInternalController;
import ru.itmo.zavar.brainstop.profile.controller.UserProfileController;
import ru.itmo.zavar.brainstop.profile.service.FollowService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@FR({"NFR-1.1", "NFR-1.6"})
@TestType(TestType.Type.BASIC)
@Layer(TestLayer.BE_INT)
class ProfileApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private FollowService followService;

    @Autowired(required = false)
    private FollowController followController;

    @Autowired(required = false)
    private UserProfileController userProfileController;

    @Autowired(required = false)
    private ProfileInternalController internalController;

    @Test
    @DisplayName("BE-INT-PROF-01: Spring контекст загружается")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("BE-INT-PROF-02: Все сервисы созданы")
    void servicesExist() {
        assertThat(followService).isNotNull();
    }

    @Test
    @DisplayName("BE-INT-PROF-03: Все контроллеры созданы")
    void controllersExist() {
        assertThat(followController).isNotNull();
        assertThat(userProfileController).isNotNull();
        assertThat(internalController).isNotNull();
    }

    @Test
    @DisplayName("BE-INT-PROF-04: Security конфигурация загружена")
    void securityConfigured() {
        assertThat(context.getBean("profileSecurityFilterChain")).isNotNull();
        assertThat(context.getBean("jwtDecoder")).isNotNull();
    }
}
