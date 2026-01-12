package ru.itmo.zavar.brainstop.video.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.api.video.dto.UserVideoDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToUserVideoMapper;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class VideoModerationApiImplTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileMetadataToUserVideoMapper mapper;

    @InjectMocks
    private VideoModerationApiImpl videoModerationApi;

    private FileMetadataEntity testEntity;
    private UserVideoDto testDto;

    @BeforeEach
    void setUp() {
        testEntity = new FileMetadataEntity();
        testEntity.setId(1L);
        testEntity.setUuid(UUID.randomUUID());
        testEntity.setName("Test Video");
        testEntity.setUploaderId(1L);

        testDto = new UserVideoDto(
                testEntity.getUuid(), "Test Video", 1L, "User",
                1000L, "video/mp4", 10L, 100L, null, "", ""
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MOD-API-01: findByUploaderId возвращает видео пользователя")
    void findByUploaderId_returnsUserVideos() {
        when(fileMetadataRepository.findByUploaderId(1L)).thenReturn(List.of(testEntity));
        when(mapper.toDto(anyList())).thenReturn(List.of(testDto));

        List<UserVideoDto> result = videoModerationApi.findByUploaderId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).uuid()).isEqualTo(testEntity.getUuid());
        verify(fileMetadataRepository).findByUploaderId(1L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MOD-API-02: findByUploaderId с пустым результатом")
    void findByUploaderId_withEmptyResult() {
        when(fileMetadataRepository.findByUploaderId(999L)).thenReturn(List.of());
        when(mapper.toDto(anyList())).thenReturn(List.of());

        List<UserVideoDto> result = videoModerationApi.findByUploaderId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MOD-API-03: findByUploaderId с множественными видео")
    void findByUploaderId_withMultipleVideos() {
        FileMetadataEntity e2 = new FileMetadataEntity();
        e2.setId(2L);
        e2.setUuid(UUID.randomUUID());
        e2.setUploaderId(1L);

        when(fileMetadataRepository.findByUploaderId(1L)).thenReturn(List.of(testEntity, e2));
        when(mapper.toDto(anyList())).thenReturn(List.of(testDto, testDto));

        List<UserVideoDto> result = videoModerationApi.findByUploaderId(1L);

        assertThat(result).hasSize(2);
    }
}
