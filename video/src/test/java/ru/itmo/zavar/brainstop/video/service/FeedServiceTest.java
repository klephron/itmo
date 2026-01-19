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
import org.springframework.test.util.ReflectionTestUtils;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.WatchProgressSummaryDto;
import ru.itmo.zavar.brainstop.common.api.like.LikeQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.FollowQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.interactions.client.InteractionsInternalClient;
import ru.itmo.zavar.brainstop.video.mapper.FeedMapperContext;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToFeedMapper;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.1", "FR-1.2"})
@Layer(TestLayer.BE_UNIT)
class FeedServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private UserProfilePublicApi userProfilePublicApi;

    @Mock
    private LikeQueryApi likeQueryApi;

    @Mock
    private FollowQueryApi followQueryApi;

    @Mock
    private InteractionsInternalClient interactionsInternalClient;

    @Mock
    private FileMetadataToFeedMapper mapper;

    @Mock
    private UserFeedPreferencesService preferencesService;

    @InjectMocks
    private FeedService feedService;

    private FileMetadataEntity testEntity;
    private FeedVideoDto testFeedDto;

    @BeforeEach
    void setUp() {
        testEntity = new FileMetadataEntity();
        testEntity.setId(1L);
        testEntity.setUuid(UUID.randomUUID());
        testEntity.setName("Test Video");
        testEntity.setUploaderId(100L);
        testEntity.setUploadedAt(LocalDateTime.now());
        testEntity.setLikesCount(10L);
        testEntity.setViewsCount(100L);

        testFeedDto = new FeedVideoDto(
                testEntity.getUuid(),
                "Test Video",
                100L,
                "TestUser",
                null,
                1000L,
                "video/mp4",
                null,
                10L,
                100L,
                5L,
                false,
                false,
                720,
                List.of(720),
                null
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-01: getFeed возвращает ленту для анонимного пользователя")
    void getFeed_returnsForAnonymousUser() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<FileMetadataEntity> page = new PageImpl<>(List.of(testEntity));

        when(fileMetadataRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(interactionsInternalClient.getCommentsCount(any())).thenReturn(List.of());
        when(mapper.toDto(anyList(), any(FeedMapperContext.class))).thenReturn(List.of(testFeedDto));

        Page<FeedVideoDto> result = feedService.getFeed(null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(fileMetadataRepository).findAll(any(PageRequest.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-02: getFeed возвращает пустую ленту")
    void getFeed_returnsEmptyFeed() {
        Pageable pageable = PageRequest.of(0, 10);
        when(fileMetadataRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(mapper.toDto(anyList(), any())).thenReturn(List.of());

        Page<FeedVideoDto> result = feedService.getFeed(null, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-05: getFeed обрабатывает null uploaderId")
    void getFeed_handlesNullUploaderId() {
        FileMetadataEntity entityWithoutUploader = new FileMetadataEntity();
        entityWithoutUploader.setId(2L);
        entityWithoutUploader.setUuid(UUID.randomUUID());
        entityWithoutUploader.setName("Video without uploader");
        entityWithoutUploader.setUploaderId(null);

        Page<FileMetadataEntity> page = new PageImpl<>(List.of(entityWithoutUploader));
        when(fileMetadataRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(interactionsInternalClient.getCommentsCount(any())).thenReturn(List.of());
        when(mapper.toDto(anyList(), any())).thenReturn(List.of());

        Page<FeedVideoDto> result = feedService.getFeed(null, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-07: getFeed обрабатывает пагинацию")
    void getFeed_handlesPagination() {
        Pageable pageable = PageRequest.of(1, 5);
        when(fileMetadataRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 20));
        when(mapper.toDto(anyList(), any())).thenReturn(List.of());

        Page<FeedVideoDto> result = feedService.getFeed(null, pageable);

        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(20);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-08: getFeed вызывает comments count")
    void getFeed_fetchesCommentsCount() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of(testEntity));
        when(fileMetadataRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(interactionsInternalClient.getCommentsCount(any())).thenReturn(List.of());
        when(mapper.toDto(anyList(), any())).thenReturn(List.of(testFeedDto));

        feedService.getFeed(null, PageRequest.of(0, 10));

        verify(interactionsInternalClient).getCommentsCount(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-09: getFeed обрабатывает множественные видео")
    void getFeed_handlesMultipleVideos() {
        FileMetadataEntity entity2 = new FileMetadataEntity();
        entity2.setId(2L);
        entity2.setUuid(UUID.randomUUID());
        entity2.setName("Video 2");
        entity2.setUploaderId(200L);

        Page<FileMetadataEntity> page = new PageImpl<>(List.of(testEntity, entity2));
        when(fileMetadataRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(interactionsInternalClient.getCommentsCount(any())).thenReturn(List.of());
        when(mapper.toDto(anyList(), any())).thenReturn(List.of(testFeedDto, testFeedDto));

        Page<FeedVideoDto> result = feedService.getFeed(null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-10: getFeed не вызывает likes api для анонимного пользователя")
    void getFeed_doesNotCallLikesApiForAnonymous() {
        Page<FileMetadataEntity> page = new PageImpl<>(List.of(testEntity));
        when(fileMetadataRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(interactionsInternalClient.getCommentsCount(any())).thenReturn(List.of());
        when(mapper.toDto(anyList(), any())).thenReturn(List.of(testFeedDto));

        feedService.getFeed(null, PageRequest.of(0, 10));

        verify(likeQueryApi, never()).findLikedVideoIds(anyLong(), anyCollection());
        verify(followQueryApi, never()).getFollowingWithin(anyLong(), anySet());
    }

    private FileMetadataEntity entityWithEmbedding(Long id, Long uploaderId, float[] embedding) {
        FileMetadataEntity e = new FileMetadataEntity();
        e.setId(id);
        e.setUuid(UUID.randomUUID());
        e.setUploaderId(uploaderId);
        e.setUploadedAt(LocalDateTime.now());
        e.setCombinedEmbedding(embedding);
        e.setLikesCount(0L);
        e.setViewsCount(0L);
        return e;
    }

    private WatchProgressSummaryDto progress(Long videoId, long watched, long duration) {
        return new WatchProgressSummaryDto(videoId, watched, duration);
    }
}
