package ru.itmo.zavar.brainstop.video.mapper;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public record FeedMapperContext(
        Long currentUserId,
        Map<Long, String> avatarByUserId,
        Set<Long> likedVideoIds,
        Set<Long> followingUserIds
) {

    public FeedMapperContext {
        avatarByUserId = avatarByUserId == null ? Collections.emptyMap() : avatarByUserId;
        likedVideoIds = likedVideoIds == null ? Collections.emptySet() : likedVideoIds;
        followingUserIds = followingUserIds == null ? Collections.emptySet() : followingUserIds;
    }
}
