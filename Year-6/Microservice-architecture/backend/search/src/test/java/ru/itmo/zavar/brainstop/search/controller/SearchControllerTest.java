package ru.itmo.zavar.brainstop.search.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.search.dto.SearchSuggestRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.1", "FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchControllerTest {

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

    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-CTL-02: Поиск с пагинацией")
    void searchVideos_withPagination() {
        ResponseEntity<PageResponse<FileMetadataDto>> response =
                controller.searchVideos("query", 2, 20, true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().page()).isEqualTo(2);
        assertThat(response.getBody().size()).isEqualTo(20);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-CTL-04: Пустой результат поиска")
    void searchVideos_emptyResults() {
        PageResponse<FileMetadataDto> pageResponse = new PageResponse<>(List.of(), 0, 10, 0);

        ResponseEntity<PageResponse<FileMetadataDto>> response =
                controller.searchVideos("nonexistent", 0, 10, true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-CTL-05: Suggest с кастомным лимитом")
    void suggestVideos_customLimit() {
        when(searchInternalClient.suggest(any(SearchSuggestRequest.class)))
                .thenReturn(List.of());

        controller.suggestVideos("query", 3, true);

        verify(searchInternalClient).suggest(argThat(req ->
                req != null && "query".equals(req.q()) && Integer.valueOf(3).equals(req.limit())));
    }
}
