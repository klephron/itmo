package ru.itmo.zavar.brainstop.video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.brainstop.common.api.search.dto.EmbeddingResponse;
import ru.itmo.zavar.brainstop.search.client.SearchEmbeddingClient;
import ru.itmo.zavar.brainstop.video.model.UserFeedTopicEntity;
import ru.itmo.zavar.brainstop.video.repo.UserFeedTopicRepository;
import ru.itmo.zavar.brainstop.video.streaming.dto.request.FeedTopicUpdateRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFeedTopicService {

    private final UserFeedTopicRepository repository;
    private final SearchEmbeddingClient embeddingClient;

    @Transactional(readOnly = true)
    public List<UserFeedTopicEntity> getTopics(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return repository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    @Transactional
    public List<UserFeedTopicEntity> replaceTopics(Long userId, List<FeedTopicUpdateRequest> topics) {
        if (userId == null) {
            return List.of();
        }
        repository.deleteByUserId(userId);
        repository.flush();
        if (topics == null || topics.isEmpty()) {
            return List.of();
        }
        Map<String, FeedTopicUpdateRequest> unique = new LinkedHashMap<>();
        for (FeedTopicUpdateRequest topic : topics) {
            if (topic == null) {
                continue;
            }
            String term = topic.term();
            if (term == null) {
                continue;
            }
            String normalized = term.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            String key = normalized.toLowerCase(Locale.ROOT);
            unique.put(key, new FeedTopicUpdateRequest(normalized, topic.weight()));
        }
        List<UserFeedTopicEntity> toSave = new ArrayList<>();
        for (FeedTopicUpdateRequest topic : unique.values()) {
            Double weight = topic.weight();
            if (weight == null || !Double.isFinite(weight) || weight == 0d) {
                continue;
            }
            EmbeddingResponse response = null;
            try {
                response = embeddingClient.embed(topic.term());
            } catch (Exception e) {
                log.warn("Failed to embed feed topic '{}': {}", topic.term(), e.toString());
            }
            float[] embedding = toVector(response);
            if (embedding == null || embedding.length == 0) {
                log.warn("Skipping feed topic '{}' due to empty embedding", topic.term());
                continue;
            }
            UserFeedTopicEntity entity = new UserFeedTopicEntity();
            entity.setUserId(userId);
            entity.setTerm(topic.term());
            entity.setWeight(weight);
            entity.setEmbedding(embedding);
            toSave.add(entity);
        }
        if (toSave.isEmpty()) {
            return List.of();
        }
        return repository.saveAll(toSave);
    }

    private float[] toVector(EmbeddingResponse response) {
        if (response == null || response.vector() == null || response.vector().isEmpty()) {
            return null;
        }
        float[] vector = new float[response.vector().size()];
        for (int i = 0; i < response.vector().size(); i++) {
            Float value = response.vector().get(i);
            vector[i] = value != null ? value : 0f;
        }
        return vector;
    }
}
