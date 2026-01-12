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
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.client.SearchInternalClient;
import ru.itmo.zavar.brainstop.search.embedding.EmbeddingService;

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

    @InjectMocks
    private SearchController controller;

    @BeforeEach
    void setUp() {
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-CTL-02: Поиск с пагинацией")
    void searchVideos_withPagination() {
        PageResponse<FileMetadataDto> pageResponse = new PageResponse<>(List.of(), 2, 20, 0);
        when(searchInternalClient.searchVideos(anyString(), eq(2), eq(20)))
                .thenReturn(pageResponse);

        ResponseEntity<PageResponse<FileMetadataDto>> response =
                controller.searchVideos("query", 2, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().page()).isEqualTo(2);
        assertThat(response.getBody().size()).isEqualTo(20);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-CTL-04: Пустой результат поиска")
    void searchVideos_emptyResults() {
        PageResponse<FileMetadataDto> pageResponse = new PageResponse<>(List.of(), 0, 10, 0);
        when(searchInternalClient.searchVideos(anyString(), anyInt(), anyInt()))
                .thenReturn(pageResponse);

        ResponseEntity<PageResponse<FileMetadataDto>> response =
                controller.searchVideos("nonexistent", 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-CTL-05: Suggest с кастомным лимитом")
    void suggestVideos_customLimit() {
        when(searchInternalClient.suggest(anyString(), eq(3)))
                .thenReturn(List.of());

        controller.suggestVideos("query", 3);

        verify(searchInternalClient).suggest(anyString(), eq(3));
    }
}
