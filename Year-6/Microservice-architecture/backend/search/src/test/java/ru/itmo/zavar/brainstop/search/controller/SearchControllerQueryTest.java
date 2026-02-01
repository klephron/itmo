package ru.itmo.zavar.brainstop.search.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.1", "FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchControllerQueryTest {

    @Mock
    private SearchInternalClient searchInternalClient;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private SearchLlmService searchLlmService;

    @InjectMocks
    private SearchController controller;

    @BeforeEach
    void setUp() {
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-INT-02: searchVideos с AI disabled")
    void searchVideos_aiDisabled() {
        List<FileMetadataDto> items = createTestItems(3);
        when(searchInternalClient.searchVideos(any(SearchVideosRequest.class)))
                .thenReturn(new PageResponse<>(items, 0, 10, 3L));

        var response = controller.searchVideos("test", 0, 10, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(searchLlmService, never()).buildQueryVariants(anyString(), anyString());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-INT-06: searchVideos с большой страницей")
    void searchVideos_largePagination() {
        when(searchInternalClient.searchVideos(any()))
                .thenReturn(new PageResponse<>(List.of(), 5, 20, 0L));

        var response = controller.searchVideos("test", 5, 20, false);

        assertThat(response.getBody().page()).isEqualTo(5);
        assertThat(response.getBody().size()).isEqualTo(20);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-INT-07: searchVideos подсчитывает total")
    void searchVideos_totalElements() {
        when(searchInternalClient.searchVideos(any()))
                .thenReturn(new PageResponse<>(createTestItems(10), 0, 10, 100L));

        var response = controller.searchVideos("test", 0, 10, false);

        assertThat(response.getBody().totalElements()).isGreaterThan(0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-INT-08: searchVideos создает эмбеддинги")
    void searchVideos_createsEmbeddings() {
        when(searchInternalClient.searchVideos(any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0L));

        controller.searchVideos("test", 0, 10, false);

        verify(embeddingService, atLeastOnce()).embed(anyString());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-INT-09: searchVideos обрабатывает пустые результаты")
    void searchVideos_emptyResults() {
        when(searchInternalClient.searchVideos(any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0L));

        var response = controller.searchVideos("nonexistent", 0, 10, false);

        assertThat(response.getBody().content()).isEmpty();
        assertThat(response.getBody().totalElements()).isEqualTo(0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-INT-10: searchVideos с null запросом")
    void searchVideos_nullQuery() {
        when(searchInternalClient.searchVideos(any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0L));

        var response = controller.searchVideos(null, 0, 10, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private List<FileMetadataDto> createTestItems(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new FileMetadataDto(
                        (long) i,
                        "Video " + i,
                        UUID.randomUUID(),
                        "video/mp4",
                        1000L,
                        (long) i,
                        "uploader" + i,
                        null,
                        10L,
                        null,
                        720,
                        List.of(720),
                        "Description " + i
                ))
                .toList();
    }
}
