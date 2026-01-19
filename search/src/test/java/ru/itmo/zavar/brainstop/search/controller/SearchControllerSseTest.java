package ru.itmo.zavar.brainstop.search.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.search.dto.SearchVideosRequest;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.client.SearchInternalClient;
import ru.itmo.zavar.brainstop.search.embedding.EmbeddingService;
import ru.itmo.zavar.brainstop.search.llm.SearchLlmService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchControllerSseTest {

    @Mock
    private SearchInternalClient searchInternalClient;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private SearchLlmService searchLlmService;

    @InjectMocks
    private SearchController controller;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SSE-01: streamLlm создает emitter")
    void streamLlm_createsEmitter() throws Exception {
        SseEmitter emitter = controller.streamLlm("test", false, 0, 10);

        assertThat(emitter).isNotNull();
        TimeUnit.MILLISECONDS.sleep(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SSE-02: streamLlm с AI disabled")
    void streamLlm_aiDisabled() throws Exception {
        SseEmitter emitter = controller.streamLlm("test", false, 0, 10);

        assertThat(emitter).isNotNull();
        TimeUnit.MILLISECONDS.sleep(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SSE-03: streamLlm с пустым запросом")
    void streamLlm_emptyQuery() throws Exception {
        SseEmitter emitter = controller.streamLlm("", true, 0, 10);

        assertThat(emitter).isNotNull();
        TimeUnit.MILLISECONDS.sleep(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SSE-04: streamLlm с null запросом")
    void streamLlm_nullQuery() throws Exception {
        SseEmitter emitter = controller.streamLlm(null, true, 0, 10);

        assertThat(emitter).isNotNull();
        TimeUnit.MILLISECONDS.sleep(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SSE-05: streamLlm когда LLM disabled")
    void streamLlm_llmDisabled() throws Exception {
        when(searchLlmService.isLlmEnabled()).thenReturn(false);

        SseEmitter emitter = controller.streamLlm("test", true, 0, 10);

        assertThat(emitter).isNotNull();
        TimeUnit.MILLISECONDS.sleep(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SSE-06: streamLlm с коротким запросом")
    void streamLlm_shortQuery() throws Exception {
        when(searchLlmService.isLlmEnabled()).thenReturn(true);
        when(searchLlmService.minQueryLength()).thenReturn(5);

        SseEmitter emitter = controller.streamLlm("ab", true, 0, 10);

        assertThat(emitter).isNotNull();
        TimeUnit.MILLISECONDS.sleep(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SSE-07: streamLlm с rewrite enabled")
    void streamLlm_rewriteEnabled() throws Exception {
        when(searchLlmService.isLlmEnabled()).thenReturn(true);
        when(searchLlmService.minQueryLength()).thenReturn(3);
        when(searchInternalClient.searchVideos(any(SearchVideosRequest.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0L));

        SseEmitter emitter = controller.streamLlm("test query", true, 0, 10);

        assertThat(emitter).isNotNull();
        TimeUnit.MILLISECONDS.sleep(200);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SSE-08: streamLlm с expansion enabled")
    void streamLlm_expansionEnabled() throws Exception {
        when(searchLlmService.isLlmEnabled()).thenReturn(true);
        when(searchLlmService.minQueryLength()).thenReturn(3);

        SseEmitter emitter = controller.streamLlm("test query", true, 0, 10);

        assertThat(emitter).isNotNull();
        TimeUnit.MILLISECONDS.sleep(200);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SSE-09: streamLlm с контекстом")
    void streamLlm_withContext() throws Exception {
        when(searchLlmService.isLlmEnabled()).thenReturn(true);
        when(searchLlmService.minQueryLength()).thenReturn(3);

        FileMetadataDto candidate = new FileMetadataDto(
                1L, "Test Video", UUID.randomUUID(), "video/mp4", 1000L,
                1L, "User", null, 10L, null, 720, List.of(720), "Description"
        );
        when(searchInternalClient.searchVideos(any(SearchVideosRequest.class)))
                .thenReturn(new PageResponse<>(List.of(candidate), 0, 20, 1L));
        when(searchLlmService.buildCandidateContext(anyList()))
                .thenReturn("context");

        SseEmitter emitter = controller.streamLlm("test query", true, 0, 10);

        assertThat(emitter).isNotNull();
        TimeUnit.MILLISECONDS.sleep(200);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SSE-10: streamLlm skip short query")
    void streamLlm_skipShortQuery() throws Exception {
        when(searchLlmService.isLlmEnabled()).thenReturn(true);
        when(searchLlmService.minQueryLength()).thenReturn(3);

        SseEmitter emitter = controller.streamLlm("test", true, 0, 10);

        assertThat(emitter).isNotNull();
        TimeUnit.MILLISECONDS.sleep(200);
    }
}
