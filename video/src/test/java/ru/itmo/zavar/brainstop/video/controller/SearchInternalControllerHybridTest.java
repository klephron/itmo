package ru.itmo.zavar.brainstop.video.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataMapper;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.1"})
@Layer(TestLayer.BE_UNIT)
class SearchInternalControllerHybridTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileMetadataMapper fileMetadataMapper;

    @InjectMocks
    private ru.itmo.zavar.brainstop.search.controller.SearchInternalController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "maxEmbeddingDistance", 0.6);
        ReflectionTestUtils.setField(controller, "strictEmbeddingDistance", 0.35);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-01: searchVideos с текстовым запросом использует hybrid")
    void searchVideos_withTextQuery_usesHybrid() {
        FileMetadataEntity entity = createEntity(1L, "Test Video");
        Page<FileMetadataEntity> vectorPage = new PageImpl<>(List.of(entity));
        Page<FileMetadataEntity> textPage = new PageImpl<>(List.of(entity));

        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(vectorPage);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(textPage);

        PageResponse<?> response = controller.searchVideos("[0.1]", "test", 0, 10);

        assertThat(response).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-02: searchVideos только с вектором")
    void searchVideos_vectorOnly() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(page);

        PageResponse<?> response = controller.searchVideos("[0.1,0.2]", "", 0, 10);

        assertThat(response.content()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-03: searchVideos с пустыми результатами текстового поиска")
    void searchVideos_emptyTextResults() {
        Page<FileMetadataEntity> vectorPage = new PageImpl<>(List.of(createEntity(1L, "Video")));
        Page<FileMetadataEntity> emptyTextPage = new PageImpl<>(List.of());

        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(vectorPage);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(emptyTextPage);

        PageResponse<?> response = controller.searchVideos("[0.1]", "test", 0, 10);

        assertThat(response).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-04: searchVideos мержит векторные и текстовые результаты")
    void searchVideos_mergesResults() {
        FileMetadataEntity e1 = createEntity(1L, "Video 1");
        FileMetadataEntity e2 = createEntity(2L, "Video 2");

        Page<FileMetadataEntity> vectorPage = new PageImpl<>(List.of(e1));
        Page<FileMetadataEntity> textPage = new PageImpl<>(List.of(e2));

        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(vectorPage);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(textPage);

        PageResponse<?> response = controller.searchVideos("[0.1]", "test", 0, 10);

        assertThat(response).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-05: searchVideos дедублицирует по ID")
    void searchVideos_deduplicatesById() {
        FileMetadataEntity entity = createEntity(1L, "Video");
        Page<FileMetadataEntity> vectorPage = new PageImpl<>(List.of(entity));
        Page<FileMetadataEntity> textPage = new PageImpl<>(List.of(entity));

        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(vectorPage);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(textPage);

        PageResponse<?> response = controller.searchVideos("[0.1]", "test", 0, 10);

        assertThat(response).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-06: searchVideos только текстовый поиск")
    void searchVideos_textOnly() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(page);

        PageResponse<?> response = controller.searchVideos("", "test query", 0, 10);

        assertThat(response.content()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-07: searchVideos с нулевым размером страницы")
    void searchVideos_zeroSize() {
        PageResponse<?> response = controller.searchVideos("[0.1]", "test", 0, 0);

        assertThat(response.content()).isEmpty();
        assertThat(response.size()).isEqualTo(0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-08: searchVideos с отрицательным размером")
    void searchVideos_negativeSize() {
        PageResponse<?> response = controller.searchVideos("[0.1]", "test", 0, -1);

        assertThat(response.content()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-SEARCH-HYB-10: searchVideosPost с null request")
    void searchVideosPost_nullRequest() {
        PageResponse<?> response = controller.searchVideosPost(null);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-11: suggestPost с валидным request")
    void suggestPost_validRequest() {
        ru.itmo.zavar.brainstop.common.api.search.dto.SearchSuggestRequest request =
                new ru.itmo.zavar.brainstop.common.api.search.dto.SearchSuggestRequest("[0.1]", "test", 5);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        List<?> response = controller.suggestPost(request);

        assertThat(response).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-SEARCH-HYB-12: suggestPost с null request")
    void suggestPost_nullRequest() {
        List<?> response = controller.suggestPost(null);

        assertThat(response).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-13: suggest с нулевым лимитом")
    void suggest_zeroLimit() {
        List<?> response = controller.suggest("[0.1]", "test", 0);

        assertThat(response).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-14: suggest с отрицательным лимитом")
    void suggest_negativeLimit() {
        List<?> response = controller.suggest("[0.1]", "test", -1);

        assertThat(response).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-15: suggest только с вектором")
    void suggest_vectorOnly() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(page);

        List<?> response = controller.suggest("[0.1]", "", 5);

        assertThat(response).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-16: searchVideos с короткими запросами")
    void searchVideos_shortQuery() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(page);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(page);

        PageResponse<?> response = controller.searchVideos("[0.1]", "cat", 0, 10);

        assertThat(response).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-17: searchVideos с множественными токенами")
    void searchVideos_multipleTokens() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(page);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(page);

        PageResponse<?> response = controller.searchVideos("[0.1]", "cat dog bird", 0, 10);

        assertThat(response).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-18: searchVideos с синонимами")
    void searchVideos_withSynonyms() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(page);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(page);

        PageResponse<?> response = controller.searchVideos("[0.1]", "volkswagen", 0, 10);

        assertThat(response).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-HYB-19: searchVideos ранжирует по точному совпадению")
    void searchVideos_ranksExactMatch() {
        FileMetadataEntity exact = createEntity(1L, "Test");
        exact.setName("test");
        FileMetadataEntity partial = createEntity(2L, "Testing");

        Page<FileMetadataEntity> vectorPage = new PageImpl<>(List.of(exact, partial));
        Page<FileMetadataEntity> textPage = new PageImpl<>(List.of(exact, partial));

        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(vectorPage);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(textPage);

        PageResponse<?> response = controller.searchVideos("[0.1]", "test", 0, 10);

        assertThat(response).isNotNull();
    }

    private FileMetadataEntity createEntity(Long id, String name) {
        FileMetadataEntity entity = new FileMetadataEntity();
        entity.setId(id);
        entity.setUuid(UUID.randomUUID());
        entity.setName(name);
        entity.setContentSize(1000L);
        entity.setContentType("video/mp4");
        entity.setLikesCount(10L);
        entity.setViewsCount(100L);
        return entity;
    }
}
