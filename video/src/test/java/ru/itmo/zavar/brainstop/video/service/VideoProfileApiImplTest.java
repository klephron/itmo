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
import org.springframework.data.domain.Pageable;
import ru.itmo.zavar.brainstop.common.api.like.LikeQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToPublicUserVideoMapper;
import ru.itmo.zavar.brainstop.video.mapper.PublicUserVideoMapperContext;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.4"})
@Layer(TestLayer.BE_UNIT)
class VideoProfileApiImplTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileMetadataToPublicUserVideoMapper mapper;

    @Mock
    private UserProfilePublicApi userProfilePublicApi;

    @Mock
    private LikeQueryApi likeQueryApi;

    @InjectMocks
    private VideoProfileApiImpl videoProfileApi;

    private FileMetadataEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new FileMetadataEntity();
        testEntity.setId(1L);
        testEntity.setUuid(UUID.randomUUID());
        testEntity.setName("Test Video");
        testEntity.setUploaderId(1L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROFILE-API-01: findUserVideos возвращает видео пользователя")
    void findUserVideos_returnsUserVideos() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of(testEntity));
        when(fileMetadataRepository.findByUploaderId(eq(1L), any(Pageable.class))).thenReturn(page);
        when(userProfilePublicApi.findUserSummary(1L))
                .thenReturn(Optional.of(new UserSummaryDto(1L, "User", "avatar.jpg")));
        when(mapper.toDto(anyList(), any(PublicUserVideoMapperContext.class)))
                .thenReturn(List.of());

        Page<PublicUserVideoDto> result = videoProfileApi.findUserVideos(1L, null, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        verify(fileMetadataRepository).findByUploaderId(eq(1L), any(Pageable.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROFILE-API-02: findUserVideos с currentUserId")
    void findUserVideos_withCurrentUserId() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of(testEntity));
        when(fileMetadataRepository.findByUploaderId(anyLong(), any())).thenReturn(page);
        when(userProfilePublicApi.findUserSummary(anyLong())).thenReturn(Optional.empty());
        when(likeQueryApi.findLikedVideoIds(eq(2L), anyCollection())).thenReturn(List.of(1L));
        when(mapper.toDto(anyList(), any())).thenReturn(List.of());

        videoProfileApi.findUserVideos(1L, 2L, PageRequest.of(0, 10));

        verify(likeQueryApi).findLikedVideoIds(eq(2L), anyCollection());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROFILE-API-03: findUserVideos без currentUserId")
    void findUserVideos_withoutCurrentUserId() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of(testEntity));
        when(fileMetadataRepository.findByUploaderId(anyLong(), any())).thenReturn(page);
        when(userProfilePublicApi.findUserSummary(anyLong())).thenReturn(Optional.empty());
        when(mapper.toDto(anyList(), any())).thenReturn(List.of());

        videoProfileApi.findUserVideos(1L, null, PageRequest.of(0, 10));

        verify(likeQueryApi, never()).findLikedVideoIds(anyLong(), anyCollection());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROFILE-API-04: findUserVideos с пустым результатом")
    void findUserVideos_withEmptyResult() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of());
        when(fileMetadataRepository.findByUploaderId(anyLong(), any())).thenReturn(page);
        when(mapper.toDto(anyList(), any())).thenReturn(List.of());

        Page<PublicUserVideoDto> result = videoProfileApi.findUserVideos(1L, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROFILE-API-05: findUserVideos создает правильный context")
    void findUserVideos_createsCorrectContext() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of(testEntity));
        when(fileMetadataRepository.findByUploaderId(anyLong(), any())).thenReturn(page);
        when(userProfilePublicApi.findUserSummary(1L))
                .thenReturn(Optional.of(new UserSummaryDto(1L, "User", "avatar.jpg")));
        when(likeQueryApi.findLikedVideoIds(eq(2L), anyCollection())).thenReturn(List.of(1L));
        when(mapper.toDto(anyList(), any())).thenReturn(List.of());

        videoProfileApi.findUserVideos(1L, 2L, PageRequest.of(0, 10));

        verify(mapper).toDto(anyList(), argThat(ctx ->
                ctx.currentUserId().equals(2L) &&
                        ctx.likedVideoIds().contains(1L) &&
                        ctx.avatarByUserId().containsKey(1L)
        ));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROFILE-API-07: findUserVideos обрабатывает отсутствие avatar")
    void findUserVideos_handlesNoAvatar() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of(testEntity));
        when(fileMetadataRepository.findByUploaderId(anyLong(), any())).thenReturn(page);
        when(userProfilePublicApi.findUserSummary(anyLong())).thenReturn(Optional.empty());
        when(mapper.toDto(anyList(), any())).thenReturn(List.of());

        videoProfileApi.findUserVideos(1L, null, PageRequest.of(0, 10));

        verify(mapper).toDto(anyList(), argThat(ctx ->
                ctx.avatarByUserId().isEmpty()
        ));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROFILE-API-08: findUserVideos с пагинацией")
    void findUserVideos_withPagination() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of(testEntity), PageRequest.of(1, 5), 20);
        when(fileMetadataRepository.findByUploaderId(anyLong(), any())).thenReturn(page);
        when(userProfilePublicApi.findUserSummary(anyLong())).thenReturn(Optional.empty());
        when(mapper.toDto(anyList(), any())).thenReturn(List.of());

        Page<PublicUserVideoDto> result = videoProfileApi.findUserVideos(1L, null, PageRequest.of(1, 5));

        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(20);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROFILE-API-09: findUserVideos фильтрует liked videos")
    void findUserVideos_filtersLikedVideos() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of(testEntity));
        when(fileMetadataRepository.findByUploaderId(anyLong(), any())).thenReturn(page);
        when(userProfilePublicApi.findUserSummary(anyLong())).thenReturn(Optional.empty());
        when(likeQueryApi.findLikedVideoIds(eq(2L), anyCollection())).thenReturn(List.of());
        when(mapper.toDto(anyList(), any())).thenReturn(List.of());

        videoProfileApi.findUserVideos(1L, 2L, PageRequest.of(0, 10));

        verify(mapper).toDto(anyList(), argThat(ctx ->
                ctx.likedVideoIds().isEmpty()
        ));
    }
}
