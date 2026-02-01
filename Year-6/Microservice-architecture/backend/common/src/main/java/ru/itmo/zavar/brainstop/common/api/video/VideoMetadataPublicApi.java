package ru.itmo.zavar.brainstop.common.api.video;

import java.util.Optional;
import java.util.UUID;

public interface VideoMetadataPublicApi {

    VideoMetadataSummary getRequired(UUID uuid);

    Optional<VideoMetadataSummary> findByUuid(UUID uuid);

    long incrementLikesCount(Long videoId, long delta);

    long incrementViewsCount(Long videoId, long delta);

    void updateLikesCount(Long videoId, long newCount);

    void updateViewsCount(Long videoId, long newCount);
}
