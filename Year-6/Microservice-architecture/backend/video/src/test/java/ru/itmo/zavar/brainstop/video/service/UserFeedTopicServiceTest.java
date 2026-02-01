package ru.itmo.zavar.brainstop.video.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.api.search.dto.EmbeddingResponse;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.client.SearchEmbeddingClient;
import ru.itmo.zavar.brainstop.video.model.UserFeedTopicEntity;
import ru.itmo.zavar.brainstop.video.repo.UserFeedTopicRepository;
import ru.itmo.zavar.brainstop.video.streaming.dto.request.FeedTopicUpdateRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.2"})
@Layer(TestLayer.BE_UNIT)
class UserFeedTopicServiceTest {

    @Mock
    private UserFeedTopicRepository repository;

    @Mock
    private SearchEmbeddingClient embeddingClient;

    @InjectMocks
    private UserFeedTopicService service;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TOPIC-01: getTopics возвращает темы пользователя")
    void getTopics_returnsUserTopics() {
        UserFeedTopicEntity topic = new UserFeedTopicEntity();
        topic.setId(1L);
        topic.setUserId(1L);
        topic.setTerm("cats");
        topic.setWeight(1.0);

        when(repository.findByUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(topic));

        List<UserFeedTopicEntity> result = service.getTopics(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTerm()).isEqualTo("cats");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TOPIC-02: getTopics с null userId")
    void getTopics_nullUserId() {
        List<UserFeedTopicEntity> result = service.getTopics(null);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TOPIC-03: replaceTopics удаляет старые и создает новые")
    void replaceTopics_deletesOldCreatesNew() {
        when(embeddingClient.embed(anyString()))
                .thenReturn(new EmbeddingResponse(List.of(0.1f, 0.2f, 0.3f)));

        List<FeedTopicUpdateRequest> topics = List.of(
                new FeedTopicUpdateRequest("cats", 1.0),
                new FeedTopicUpdateRequest("dogs", 0.8)
        );

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<UserFeedTopicEntity> result = service.replaceTopics(1L, topics);

        verify(repository).deleteByUserId(1L);
        verify(repository).flush();
        verify(repository).saveAll(anyList());
        assertThat(result).hasSize(2);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TOPIC-04: replaceTopics с null userId")
    void replaceTopics_nullUserId() {
        List<UserFeedTopicEntity> result = service.replaceTopics(null, List.of());

        assertThat(result).isEmpty();
        verify(repository, never()).deleteByUserId(anyLong());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TOPIC-05: replaceTopics с пустым списком")
    void replaceTopics_emptyList() {
        List<UserFeedTopicEntity> result = service.replaceTopics(1L, List.of());

        verify(repository).deleteByUserId(1L);
        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TOPIC-07: replaceTopics пропускает пустые термины")
    void replaceTopics_skipsEmptyTerms() {
        when(embeddingClient.embed(anyString()))
                .thenReturn(new EmbeddingResponse(List.of(0.1f)));

        List<FeedTopicUpdateRequest> topics = List.of(
                new FeedTopicUpdateRequest("", 1.0),
                new FeedTopicUpdateRequest("cats", 0.8)
        );

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<UserFeedTopicEntity> result = service.replaceTopics(1L, topics);

        assertThat(result).hasSize(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TOPIC-08: replaceTopics пропускает нулевые веса")
    void replaceTopics_skipsZeroWeights() {
        when(embeddingClient.embed(anyString()))
                .thenReturn(new EmbeddingResponse(List.of(0.1f)));

        List<FeedTopicUpdateRequest> topics = List.of(
                new FeedTopicUpdateRequest("cats", 0.0),
                new FeedTopicUpdateRequest("dogs", 0.8)
        );

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<UserFeedTopicEntity> result = service.replaceTopics(1L, topics);

        assertThat(result).hasSize(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TOPIC-09: replaceTopics пропускает NaN веса")
    void replaceTopics_skipsNanWeights() {
        when(embeddingClient.embed(anyString()))
                .thenReturn(new EmbeddingResponse(List.of(0.1f)));

        List<FeedTopicUpdateRequest> topics = List.of(
                new FeedTopicUpdateRequest("cats", Double.NaN),
                new FeedTopicUpdateRequest("dogs", 0.8)
        );

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<UserFeedTopicEntity> result = service.replaceTopics(1L, topics);

        assertThat(result).hasSize(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TOPIC-10: replaceTopics дедублицирует термины")
    void replaceTopics_deduplicatesTerms() {
        when(embeddingClient.embed(anyString()))
                .thenReturn(new EmbeddingResponse(List.of(0.1f)));

        List<FeedTopicUpdateRequest> topics = List.of(
                new FeedTopicUpdateRequest("cats", 1.0),
                new FeedTopicUpdateRequest("Cats", 0.8),
                new FeedTopicUpdateRequest("CATS", 0.5)
        );

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<UserFeedTopicEntity> result = service.replaceTopics(1L, topics);

        assertThat(result).hasSize(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TOPIC-11: replaceTopics пропускает темы с пустыми embeddings")
    void replaceTopics_skipsEmptyEmbeddings() {
        when(embeddingClient.embed(anyString()))
                .thenReturn(new EmbeddingResponse(List.of()));

        List<FeedTopicUpdateRequest> topics = List.of(
                new FeedTopicUpdateRequest("cats", 1.0)
        );

        List<UserFeedTopicEntity> result = service.replaceTopics(1L, topics);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TOPIC-12: replaceTopics пропускает темы с null embeddings")
    void replaceTopics_skipsNullEmbeddings() {
        when(embeddingClient.embed(anyString())).thenReturn(null);

        List<FeedTopicUpdateRequest> topics = List.of(
                new FeedTopicUpdateRequest("cats", 1.0)
        );

        List<UserFeedTopicEntity> result = service.replaceTopics(1L, topics);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TOPIC-13: replaceTopics обрабатывает ошибку embedding")
    void replaceTopics_handlesEmbeddingError() {
        when(embeddingClient.embed(anyString())).thenThrow(new RuntimeException("Error"));

        List<FeedTopicUpdateRequest> topics = List.of(
                new FeedTopicUpdateRequest("cats", 1.0)
        );

        List<UserFeedTopicEntity> result = service.replaceTopics(1L, topics);

        assertThat(result).isEmpty();
    }
}
