package ru.itmo.zavar.brainstop.search.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.itmo.zavar.brainstop.common.api.search.dto.SearchSuggestRequest;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.client.SearchInternalClient;
import ru.itmo.zavar.brainstop.search.embedding.EmbeddingService;
import ru.itmo.zavar.brainstop.search.llm.SearchLlmService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.1"})
@Layer(TestLayer.BE_UNIT)
class SearchControllerSuggestExtendedTest {

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
    @DisplayName("BE-UNIT-SUGGEST-EXT-01: suggestVideos с результатами")
    void suggestVideos_withResults() {
        List<FileMetadataDto> suggestions = List.of(
                new FileMetadataDto(1L, "Video 1", UUID.randomUUID(), "video/mp4", 1000L,
                        1L, "User", null, 10L, null, 720, List.of(720), "Desc")
        );
        when(searchInternalClient.suggest(any(SearchSuggestRequest.class)))
                .thenReturn(suggestions);

        var response = controller.suggestVideos("test", 5, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(searchInternalClient).suggest(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SUGGEST-EXT-02: suggestVideos с пустым запросом")
    void suggestVideos_emptyQuery() {
        var response = controller.suggestVideos("", 5, false);

        assertThat(response.getBody()).isEmpty();
        verify(searchInternalClient, never()).suggest(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SUGGEST-EXT-03: suggestVideos с пробелами")
    void suggestVideos_whitespaceQuery() {
        var response = controller.suggestVideos("   ", 5, false);

        assertThat(response.getBody()).isEmpty();
        verify(searchInternalClient, never()).suggest(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SUGGEST-EXT-04: suggestVideos с null запросом")
    void suggestVideos_nullQuery() {
        var response = controller.suggestVideos(null, 5, false);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SUGGEST-EXT-05: suggestVideos с отрицательным лимитом")
    void suggestVideos_negativeLimit() {
        var response = controller.suggestVideos("test", -1, false);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SUGGEST-EXT-06: suggestVideos с лимитом 1")
    void suggestVideos_limitOne() {
        when(searchInternalClient.suggest(any(SearchSuggestRequest.class)))
                .thenReturn(List.of());

        controller.suggestVideos("test", 1, false);

        verify(searchInternalClient).suggest(argThat(req ->
                req != null && Integer.valueOf(1).equals(req.limit())));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SUGGEST-EXT-07: suggestVideos передает запрос в клиент")
    void suggestVideos_passesQueryToClient() {
        when(searchInternalClient.suggest(any(SearchSuggestRequest.class)))
                .thenReturn(List.of());

        controller.suggestVideos("test query", 5, false);

        verify(searchInternalClient).suggest(argThat(req ->
                req != null && "test query".equals(req.q())));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SUGGEST-EXT-08: suggestVideos с большим лимитом")
    void suggestVideos_largeLimit() {
        when(searchInternalClient.suggest(any(SearchSuggestRequest.class)))
                .thenReturn(List.of());

        controller.suggestVideos("test", 1000, false);

        verify(searchInternalClient).suggest(argThat(req ->
                req != null && Integer.valueOf(1000).equals(req.limit())));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SUGGEST-EXT-10: suggestVideos с дефолтным лимитом")
    void suggestVideos_defaultLimit() {
        when(searchInternalClient.suggest(any(SearchSuggestRequest.class)))
                .thenReturn(List.of());

        controller.suggestVideos("test", 5, false);

        verify(searchInternalClient).suggest(argThat(req ->
                req != null && Integer.valueOf(5).equals(req.limit())));
    }
}
