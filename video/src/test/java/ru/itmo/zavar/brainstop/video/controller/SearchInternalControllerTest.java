package ru.itmo.zavar.brainstop.video.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.controller.SearchInternalController;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataMapper;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.1"})
@Layer(TestLayer.BE_UNIT)
class SearchInternalControllerTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileMetadataMapper fileMetadataMapper;

    @InjectMocks
    private SearchInternalController controller;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-SEARCH-02: searchVideos с пагинацией")
    void searchVideos_withPagination() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of(), PageRequest.of(1, 5), 20);
        when(fileMetadataRepository.searchByEmbedding(anyString(), any())).thenReturn(page);

        PageResponse<FileMetadataDto> response = controller.searchVideos("[0.1,0.2]", 1, 5);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(20);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-SEARCH-03: searchVideos с пустым результатом")
    void searchVideos_withEmptyResult() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByEmbedding(anyString(), any())).thenReturn(page);

        PageResponse<FileMetadataDto> response = controller.searchVideos("[0.1]", 0, 10);

        assertThat(response.content()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-SEARCH-05: suggest с лимитом")
    void suggest_withLimit() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByEmbedding(anyString(), any())).thenReturn(page);

        controller.suggest("[0.1]", 3);

        verify(fileMetadataRepository).searchByEmbedding(anyString(), eq(PageRequest.of(0, 3)));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-SEARCH-06: suggest использует дефолтный лимит")
    void suggest_usesDefaultLimit() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByEmbedding(anyString(), any())).thenReturn(page);

        controller.suggest("[0.1]", 5);

        verify(fileMetadataRepository).searchByEmbedding(anyString(), eq(PageRequest.of(0, 5)));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-SEARCH-08: searchVideos вызывает репозиторий с правильным вектором")
    void searchVideos_callsRepositoryWithCorrectVector() {
        String vector = "[0.1,0.2,0.3,0.4]";
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByEmbedding(anyString(), any())).thenReturn(page);

        controller.searchVideos(vector, 0, 10);

        verify(fileMetadataRepository).searchByEmbedding(eq(vector), any());
    }
}
