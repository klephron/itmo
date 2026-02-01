package ru.itmo.zavar.brainstop.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.zavar.brainstop.admin.client.ProfileAdminClient;
import ru.itmo.zavar.brainstop.admin.client.VideoAdminClient;
import ru.itmo.zavar.brainstop.admin.controller.AdminController;
import ru.itmo.zavar.brainstop.admin.controller.ModerationController;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@FR({"FR-14.2", "FR-14.3", "NFR-1.1", "NFR-1.6"})
@TestType(TestType.Type.BASIC)
@Layer(TestLayer.BE_INT)
class AdminIntegrationTest {

    @Autowired(required = false)
    private AdminController adminController;

    @Autowired(required = false)
    private ModerationController moderationController;

    @MockBean
    private VideoAdminClient videoAdminClient;

    @MockBean
    private ProfileAdminClient profileAdminClient;

    @Test
    @DisplayName("BE-INT-ADMIN-01: Контекст приложения загружается")
    void contextLoads() {
        assertThat(adminController).isNotNull();
        assertThat(moderationController).isNotNull();
    }

    @Test
    @DisplayName("BE-INT-ADMIN-02: Все необходимые beans созданы")
    void requiredBeansCreated() {
        assertThat(adminController).isNotNull();
        assertThat(moderationController).isNotNull();
        assertThat(videoAdminClient).isNotNull();
        assertThat(profileAdminClient).isNotNull();
    }
}
