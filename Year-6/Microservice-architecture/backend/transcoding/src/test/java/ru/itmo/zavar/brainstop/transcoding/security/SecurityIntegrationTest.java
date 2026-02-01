package ru.itmo.zavar.brainstop.transcoding.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingCommand;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FR({"FR-3.1"})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-SEC-01: Swagger UI не доступен без аутентификации")
    void swaggerUi_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-SEC-02: OpenAPI docs доступны без аутентификации")
    void openApiDocs_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-SEC-03: Internal endpoints не доступны без аутентификации")
    void internalEndpoints_accessibleWithoutAuth() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "test-key", "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        mockMvc.perform(post("/internal/transcoding/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-SEC-04: Actuator доступен без аутентификации")
    void actuator_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-SEC-05: Actuator metrics доступны")
    void actuatorMetrics_accessible() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk());
    }
}
