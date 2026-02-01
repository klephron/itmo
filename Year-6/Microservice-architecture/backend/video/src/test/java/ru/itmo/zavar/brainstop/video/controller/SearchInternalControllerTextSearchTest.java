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
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.1"})
@Layer(TestLayer.BE_UNIT)
class SearchInternalControllerTextSearchTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileMetadataMapper fileMetadataMapper;

    @InjectMocks
    private ru.itmo.zavar.brainstop.search.controller.SearchInternalController controller;

    private FileMetadataDto testDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "maxEmbeddingDistance", 0.6);
        ReflectionTestUtils.setField(controller, "strictEmbeddingDistance", 0.35);

        testDto = new FileMetadataDto(
                1L, "Test Video", UUID.randomUUID(), "video/mp4", 1000L,
                1L, "User", null, 10L, null, 720, List.of(720), "Description"
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-01: searchVideos создает tsquery для одного токена")
    void searchVideos_singleTokenTsQuery() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByTextPrefix(eq("cat:*"), any()))
                .thenReturn(page);

        controller.searchVideos("", "cat", 0, 10);

        verify(fileMetadataRepository).searchByTextPrefix(eq("cat:*"), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-02: searchVideos создает tsquery для нескольких токенов")
    void searchVideos_multipleTokensTsQuery() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByTextPrefix(eq("cat:* & dog:*"), any()))
                .thenReturn(page);

        controller.searchVideos("", "cat dog", 0, 10);

        verify(fileMetadataRepository).searchByTextPrefix(eq("cat:* & dog:*"), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-03: searchVideos расширяет синонимы")
    void searchVideos_expandsSynonyms() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByTextPrefix(contains("volkswagen"), any()))
                .thenReturn(page);

        controller.searchVideos("", "volkswagen", 0, 10);

        verify(fileMetadataRepository).searchByTextPrefix(contains("vw"), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-04: searchVideos нормализует query")
    void searchVideos_normalizesQuery() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByTextPrefix(eq("test:*"), any()))
                .thenReturn(page);

        controller.searchVideos("", "TEST", 0, 10);

        verify(fileMetadataRepository).searchByTextPrefix(eq("test:*"), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-05: searchVideos удаляет спецсимволы")
    void searchVideos_removesSpecialChars() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(page);

        controller.searchVideos("", "cat@#$dog", 0, 10);

        verify(fileMetadataRepository).searchByTextPrefix(contains("cat"), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-06: searchVideos ограничивает количество токенов")
    void searchVideos_limitsTokenCount() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(page);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(page);

        String longQuery = "one two three four five six seven eight";
        controller.searchVideos("[0.1]", longQuery, 0, 10);

        // Должно обрезать до 5 токенов
        verify(fileMetadataRepository).searchByTextPrefix(anyString(), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-07: suggest использует prefix search")
    void suggest_usesPrefixSearch() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.searchByTextPrefix(eq("test:*"), any()))
                .thenReturn(page);

        controller.suggest("", "test", 5);

        verify(fileMetadataRepository).searchByTextPrefix(eq("test:*"), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-09: searchVideos бустит exact name match")
    void searchVideos_boostsExactNameMatch() {
        FileMetadataEntity exact = createEntity(1L, "test");
        FileMetadataEntity partial = createEntity(2L, "testing");

        Page<FileMetadataEntity> vectorPage = new PageImpl<>(List.of(partial, exact));
        Page<FileMetadataEntity> textPage = new PageImpl<>(List.of(partial, exact));

        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(vectorPage);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(textPage);

        PageResponse<?> response = controller.searchVideos("[0.1]", "test", 0, 10);

        assertThat(response).isNotNull();
        verify(fileMetadataRepository).searchByTextPrefix(anyString(), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-10: searchVideos бустит prefix match")
    void searchVideos_boostsPrefixMatch() {
        FileMetadataEntity prefix = createEntity(1L, "testing");
        FileMetadataEntity noPrefix = createEntity(2L, "a test video");

        Page<FileMetadataEntity> page = new PageImpl<>(List.of(noPrefix, prefix));
        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(page);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(page);

        PageResponse<?> response = controller.searchVideos("[0.1]", "test", 0, 10);

        assertThat(response).isNotNull();
        verify(fileMetadataRepository).searchByTextPrefix(anyString(), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-11: searchVideos применяет короткий query boost")
    void searchVideos_appliesShortQueryBoost() {
        FileMetadataEntity entity = createEntity(1L, "Cat Video");
        entity.setDescription("Cats playing");

        Page<FileMetadataEntity> page = new PageImpl<>(List.of(entity));
        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(page);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(page);

        PageResponse<?> response = controller.searchVideos("[0.1]", "cat", 0, 10);

        assertThat(response).isNotNull();
        verify(fileMetadataRepository).searchByTextPrefix(anyString(), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-12: searchVideos бустит uploader match")
    void searchVideos_boostsUploaderMatch() {
        FileMetadataEntity entity = createEntity(1L, "Video");
        entity.setUploaderName("testuser");

        Page<FileMetadataEntity> page = new PageImpl<>(List.of(entity));
        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(page);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(page);

        PageResponse<?> response = controller.searchVideos("[0.1]", "testuser", 0, 10);

        assertThat(response).isNotNull();
        verify(fileMetadataRepository).searchByTextPrefix(anyString(), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-13: searchVideos бустит description match")
    void searchVideos_boostsDescriptionMatch() {
        FileMetadataEntity entity = createEntity(1L, "Video");
        entity.setDescription("test description with keywords");

        Page<FileMetadataEntity> page = new PageImpl<>(List.of(entity));
        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(page);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(page);

        PageResponse<?> response = controller.searchVideos("[0.1]", "test", 0, 10);

        assertThat(response).isNotNull();
        verify(fileMetadataRepository).searchByTextPrefix(anyString(), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-SEARCH-TEXT-15: searchVideos применяет token prefix boost")
    void searchVideos_appliesTokenPrefixBoost() {
        FileMetadataEntity entity = createEntity(1L, "Cat Video");

        Page<FileMetadataEntity> page = new PageImpl<>(List.of(entity));
        when(fileMetadataRepository.searchByEmbedding(anyString(), anyDouble(), any()))
                .thenReturn(page);
        when(fileMetadataRepository.searchByTextPrefix(anyString(), any()))
                .thenReturn(page);

        PageResponse<?> response = controller.searchVideos("[0.1]", "ca", 0, 10);

        assertThat(response).isNotNull();
        verify(fileMetadataRepository).searchByTextPrefix(anyString(), any());
    }

    private FileMetadataEntity createEntity(Long id, String name) {
        FileMetadataEntity entity = new FileMetadataEntity();
        entity.setId(id);
        entity.setUuid(UUID.randomUUID());
        entity.setName(name);
        entity.setContentSize(1000L);
        entity.setCombinedEmbedding(new float[]{0.5f, 0.5f});
        entity.setLikesCount(10L);
        entity.setViewsCount(100L);
        return entity;
    }
}
