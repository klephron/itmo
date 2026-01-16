package ru.itmo.zavar.brainstop.video.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.video.model.UserFeedPreferencesEntity;
import ru.itmo.zavar.brainstop.video.repo.UserFeedPreferencesRepository;
import ru.itmo.zavar.brainstop.video.streaming.dto.request.FeedPreferencesUpdateRequest;

@Service
@RequiredArgsConstructor
public class UserFeedPreferencesService {

    public static final double DEFAULT_LIKE_WEIGHT = 1d;
    public static final double DEFAULT_FOLLOW_WEIGHT = 1d;
    public static final double DEFAULT_WATCH_WEIGHT = 1d;
    public static final double DEFAULT_WATCH_THRESHOLD = 0.5d;

    private final UserFeedPreferencesRepository repository;

    public FeedPreferences getPreferences(Long userId) {
        UserFeedPreferencesEntity entity = getOrCreate(userId);
        return new FeedPreferences(
                entity.getLikeWeight(),
                entity.getFollowWeight(),
                entity.getWatchWeight(),
                entity.getWatchThreshold()
        );
    }

    public UserFeedPreferencesEntity updatePreferences(Long userId, FeedPreferencesUpdateRequest request) {
        UserFeedPreferencesEntity entity = getOrCreate(userId);
        if (request.likeWeight() != null) {
            entity.setLikeWeight(request.likeWeight());
        }
        if (request.followWeight() != null) {
            entity.setFollowWeight(request.followWeight());
        }
        if (request.watchWeight() != null) {
            entity.setWatchWeight(request.watchWeight());
        }
        if (request.watchThreshold() != null) {
            entity.setWatchThreshold(request.watchThreshold());
        }
        return repository.save(entity);
    }

    private UserFeedPreferencesEntity getOrCreate(Long userId) {
        return repository.findById(userId)
                .orElseGet(() -> repository.save(defaultEntity(userId)));
    }

    private UserFeedPreferencesEntity defaultEntity(Long userId) {
        UserFeedPreferencesEntity entity = new UserFeedPreferencesEntity();
        entity.setUserId(userId);
        entity.setLikeWeight(DEFAULT_LIKE_WEIGHT);
        entity.setFollowWeight(DEFAULT_FOLLOW_WEIGHT);
        entity.setWatchWeight(DEFAULT_WATCH_WEIGHT);
        entity.setWatchThreshold(DEFAULT_WATCH_THRESHOLD);
        return entity;
    }

    public record FeedPreferences(
            double likeWeight,
            double followWeight,
            double watchWeight,
            double watchThreshold
    ) {
    }
}
