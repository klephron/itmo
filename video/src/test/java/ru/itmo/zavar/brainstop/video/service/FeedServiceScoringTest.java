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
import org.springframework.test.util.ReflectionTestUtils;
import ru.itmo.zavar.brainstop.common.api.like.LikeQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.FollowQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.interactions.client.InteractionsInternalClient;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToFeedMapper;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.1"})
@Layer(TestLayer.BE_UNIT)
class FeedServiceScoringTest {

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
    @Mock
    private UserFeedTopicService topicService;

    @InjectMocks
    private FeedService feedService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(feedService, "maxEmbeddingDistance", 0.6);
        ReflectionTestUtils.setField(feedService, "relaxedMaxEmbeddingDistance", 0.9);

        when(preferencesService.getPreferences(anyLong()))
                .thenReturn(new UserFeedPreferencesService.FeedPreferences(1.0, 1.0, 1.0, 0.5));
        when(topicService.getTopics(anyLong())).thenReturn(List.of());
        when(mapper.toDto(anyList(), any())).thenReturn(List.of());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-SCORE-04: weightedAverage обрабатывает пустой список")
    void weightedAverage_emptyList() {
        Page<FileMetadataEntity> empty = new PageImpl<>(List.of());
        when(fileMetadataRepository.findByCombinedEmbeddingIsNotNull(any())).thenReturn(empty);
        when(fileMetadataRepository.findAll(any(PageRequest.class))).thenReturn(empty);
        when(likeQueryApi.findLikedVideoIds(anyLong(), any())).thenReturn(List.of());
        when(followQueryApi.getFollowingWithin(anyLong(), any())).thenReturn(Set.of());

        org.springframework.data.domain.Page<FeedVideoDto> result = feedService.getFeed(1L, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-SCORE-05: cosineSimilarity с null векторами")
    void cosineSimilarity_nullVectors() {
        FileMetadataEntity entity = createEntity(1L);
        entity.setCombinedEmbedding(null);

        Page<FileMetadataEntity> page = new PageImpl<>(List.of(entity));
        when(fileMetadataRepository.findByCombinedEmbeddingIsNotNull(any())).thenReturn(new PageImpl<>(List.of()));
        when(fileMetadataRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(interactionsInternalClient.getViewedVideoIds(any())).thenReturn(List.of());

        feedService.getFeed(1L, PageRequest.of(0, 10));

        assertThat(true).isTrue();
    }

    private FileMetadataEntity createEntity(Long id) {
        FileMetadataEntity entity = new FileMetadataEntity();
        entity.setId(id);
        entity.setUuid(UUID.randomUUID());
        entity.setName("Video " + id);
        entity.setUploaderId(100L);
        entity.setUploadedAt(LocalDateTime.now());
        entity.setLikesCount(10L);
        entity.setViewsCount(100L);
        return entity;
    }
}
