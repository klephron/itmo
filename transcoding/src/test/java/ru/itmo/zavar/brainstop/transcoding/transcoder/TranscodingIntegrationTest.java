package ru.itmo.zavar.brainstop.transcoding.transcoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingCommand;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FR({"FR-3.1", "FR-3.2"})
class TranscodingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VideoObjectStorage videoObjectStorage;

    @BeforeEach
    void setUp() throws Exception {
        when(videoObjectStorage.downloadProcessed(anyString()))
                .thenReturn(new ByteArrayInputStream(new byte[1000]));
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-INT-TRANS-02: POST без storageKey возвращает 400")
    void postJobs_withoutStorageKey_returns400() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "", "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        mockMvc.perform(post("/internal/transcoding/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-INT-TRANS-03: POST без jobId возвращает 400")
    void postJobs_withoutJobId_returns400() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                null, "key", "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        mockMvc.perform(post("/internal/transcoding/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-INT-TRANS-05: GET для несуществующего jobId возвращает 404")
    void getJobStatus_forNonExistent_returns404() throws Exception {
        mockMvc.perform(get("/internal/transcoding/jobs/{jobId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-INT-TRANS-08: POST с null command возвращает 400")
    void postJobs_withNullCommand_returns400() throws Exception {
        mockMvc.perform(post("/internal/transcoding/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-TRANS-09: Последовательные запросы статуса")
    void sequentialStatusRequests() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "test-key", "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        mockMvc.perform(post("/internal/transcoding/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/internal/transcoding/jobs/{jobId}", jobId))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-TRANS-10: POST создает джоб с правильным userId")
    void postJobs_createsJobWithCorrectUserId() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "test-key", "video.mp4", "tmp", 123L, "tmp", List.of(720)
        );

        mockMvc.perform(post("/internal/transcoding/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)));

        mockMvc.perform(get("/internal/transcoding/jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(123));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-TRANS-11: GET возвращает правильный percent")
    void getJobStatus_returnsCorrectPercent() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "test-key", "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        mockMvc.perform(post("/internal/transcoding/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)));

        mockMvc.perform(get("/internal/transcoding/jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percent").value(0));
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-INT-TRANS-13: POST с некорректным JSON")
    void postJobs_withInvalidJson() throws Exception {
        mockMvc.perform(post("/internal/transcoding/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-TRANS-14: GET возвращает message")
    void getJobStatus_returnsMessage() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "test-key", "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        mockMvc.perform(post("/internal/transcoding/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)));

        mockMvc.perform(get("/internal/transcoding/jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
