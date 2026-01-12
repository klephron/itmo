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
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.client.SearchInternalClient;
import ru.itmo.zavar.brainstop.search.embedding.EmbeddingService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.1", "FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchControllerAdvancedTest {

    @Mock
    private SearchInternalClient searchInternalClient;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private SearchController controller;

    @BeforeEach
    void setUp() {
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-ADV-01: Поиск с большой страницей")
    void searchVideos_largePage() {
        PageResponse<FileMetadataDto> response = new PageResponse<>(List.of(), 100, 50, 5000);
        when(searchInternalClient.searchVideos(anyString(), eq(100), eq(50)))
                .thenReturn(response);

        var result = controller.searchVideos("query", 100, 50);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().page()).isEqualTo(100);
        assertThat(result.getBody().size()).isEqualTo(50);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-ADV-02: Поиск с нулевым размером страницы")
    void searchVideos_zeroSize() {
        PageResponse<FileMetadataDto> response = new PageResponse<>(List.of(), 0, 0, 0);
        when(searchInternalClient.searchVideos(anyString(), eq(0), eq(0)))
                .thenReturn(response);

        var result = controller.searchVideos("query", 0, 0);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().content()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-ADV-03: Поиск по unicode запросу")
    void searchVideos_unicodeQuery() {
        when(searchInternalClient.searchVideos(anyString(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0));

        controller.searchVideos("机器学习视频", 0, 10);

        verify(embeddingService).embed("机器学习视频");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-ADV-04: Поиск с emoji")
    void searchVideos_emojiQuery() {
        controller.searchVideos("test 😀 🎥", 0, 10);

        verify(embeddingService).embed("test 😀 🎥");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-ADV-05: Поиск с очень длинным запросом")
    void searchVideos_veryLongQuery() {
        String longQuery = "word ".repeat(100);
        when(searchInternalClient.searchVideos(anyString(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0));

        controller.searchVideos(longQuery, 0, 10);

        verify(embeddingService).embed(longQuery);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-ADV-06: Suggest с нулевым лимитом")
    void suggestVideos_zeroLimit() {
        when(searchInternalClient.suggest(anyString(), eq(0)))
                .thenReturn(List.of());

        var result = controller.suggestVideos("query", 0);

        assertThat(result.getBody()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-ADV-07: Suggest с большим лимитом")
    void suggestVideos_largeLimit() {
        when(searchInternalClient.suggest(anyString(), eq(100)))
                .thenReturn(List.of());

        controller.suggestVideos("query", 100);

        verify(searchInternalClient).suggest(anyString(), eq(100));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-ADV-10: Множественные вызовы с одним запросом")
    void searchVideos_multipleCallsSameQuery() {
        PageResponse<FileMetadataDto> response = new PageResponse<>(List.of(), 0, 10, 0);
        when(searchInternalClient.searchVideos(anyString(), anyInt(), anyInt()))
                .thenReturn(response);

        for (int i = 0; i < 5; i++) {
            controller.searchVideos("same query", 0, 10);
        }

        verify(embeddingService, times(5)).embed("same query");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-ADV-11: Поиск с пробелами по краям")
    void searchVideos_queryWithWhitespace() {
        controller.searchVideos("  test query  ", 0, 10);

        verify(embeddingService).embed("  test query  ");
    }
}
