package ru.itmo.zavar.brainstop.video.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.1"})
@Layer(TestLayer.BE_UNIT)
class VideoQueryApiImplTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileMetadataMapper fileMetadataMapper;

    @Mock
    private FeedService feedService;

    @InjectMocks
    private VideoQueryApiImpl videoQueryApi;

    private FileMetadataEntity testEntity;
    private FileMetadataDto testDto;

    @BeforeEach
    void setUp() {
        testEntity = new FileMetadataEntity();
        testEntity.setId(1L);
        testEntity.setUuid(UUID.randomUUID());
        testEntity.setName("Test Video");

        testDto = new FileMetadataDto(
            1L, "Test Video", testEntity.getUuid(), "video/mp4", 1000L,
            1L, "User", null, 10L, null, 720, List.of(720), ""
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-QUERY-API-01: listAll возвращает все видео")
    void listAll_returnsAllVideos() {
        when(fileMetadataRepository.findAll()).thenReturn(List.of(testEntity));
        when(fileMetadataMapper.toDto(anyList())).thenReturn(List.of(testDto));

        List<FileMetadataDto> result = videoQueryApi.listAll();

        assertThat(result).hasSize(1);
        verify(fileMetadataRepository).findAll();
        verify(fileMetadataMapper).toDto(anyList());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-QUERY-API-02: listAll с пустым результатом")
    void listAll_withEmptyResult() {
        when(fileMetadataRepository.findAll()).thenReturn(List.of());
        when(fileMetadataMapper.toDto(anyList())).thenReturn(List.of());

        List<FileMetadataDto> result = videoQueryApi.listAll();

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-QUERY-API-03: listByUploader возвращает видео пользователя")
    void listByUploader_returnsUserVideos() {
        when(fileMetadataRepository.findAllByUploaderId(1L)).thenReturn(List.of(testEntity));
        when(fileMetadataMapper.toDto(anyList())).thenReturn(List.of(testDto));

        List<FileMetadataDto> result = videoQueryApi.listByUploader(1L);

        assertThat(result).hasSize(1);
        verify(fileMetadataRepository).findAllByUploaderId(1L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-QUERY-API-04: listByUploader с несуществующим пользователем")
    void listByUploader_forNonExistentUser() {
        when(fileMetadataRepository.findAllByUploaderId(999L)).thenReturn(List.of());
        when(fileMetadataMapper.toDto(anyList())).thenReturn(List.of());

        List<FileMetadataDto> result = videoQueryApi.listByUploader(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-QUERY-API-05: getFeed делегирует FeedService")
    void getFeed_delegatesToFeedService() {
        Page<FeedVideoDto> expectedPage = new PageImpl<>(List.of());
        when(feedService.getFeed(eq(1L), any())).thenReturn(expectedPage);

        Page<FeedVideoDto> result = videoQueryApi.getFeed(1L, PageRequest.of(0, 10));

        assertThat(result).isEqualTo(expectedPage);
        verify(feedService).getFeed(eq(1L), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-QUERY-API-06: getFeed для анонимного пользователя")
    void getFeed_forAnonymousUser() {
        Page<FeedVideoDto> expectedPage = new PageImpl<>(List.of());
        when(feedService.getFeed(eq(null), any())).thenReturn(expectedPage);

        Page<FeedVideoDto> result = videoQueryApi.getFeed(null, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        verify(feedService).getFeed(eq(null), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-QUERY-API-07: listAll с множественными видео")
    void listAll_withMultipleVideos() {
        FileMetadataEntity e2 = new FileMetadataEntity();
        e2.setId(2L);
        e2.setUuid(UUID.randomUUID());

        when(fileMetadataRepository.findAll()).thenReturn(List.of(testEntity, e2));
        when(fileMetadataMapper.toDto(anyList())).thenReturn(List.of(testDto, testDto));

        List<FileMetadataDto> result = videoQueryApi.listAll();

        assertThat(result).hasSize(2);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-QUERY-API-08: listByUploader с множественными видео")
    void listByUploader_withMultipleVideos() {
        FileMetadataEntity e2 = new FileMetadataEntity();
        e2.setId(2L);
        e2.setUploaderId(1L);

        when(fileMetadataRepository.findAllByUploaderId(1L)).thenReturn(List.of(testEntity, e2));
        when(fileMetadataMapper.toDto(anyList())).thenReturn(List.of(testDto, testDto));

        List<FileMetadataDto> result = videoQueryApi.listByUploader(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-QUERY-API-09: getFeed с пагинацией")
    void getFeed_withPagination() {
        Page<FeedVideoDto> expectedPage = new PageImpl<>(List.of(), PageRequest.of(2, 5), 50);
        when(feedService.getFeed(any(), any())).thenReturn(expectedPage);

        Page<FeedVideoDto> result = videoQueryApi.getFeed(1L, PageRequest.of(2, 5));

        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(50);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-QUERY-API-10: listAll вызывается только один раз")
    void listAll_calledOnlyOnce() {
        when(fileMetadataRepository.findAll()).thenReturn(List.of(testEntity));
        when(fileMetadataMapper.toDto(anyList())).thenReturn(List.of(testDto));

        videoQueryApi.listAll();

        verify(fileMetadataRepository, times(1)).findAll();
        verify(fileMetadataMapper, times(1)).toDto(anyList());
    }
}
