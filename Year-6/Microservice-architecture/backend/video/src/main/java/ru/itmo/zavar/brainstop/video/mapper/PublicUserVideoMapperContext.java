package ru.itmo.zavar.brainstop.video.mapper;

import java.util.Map;
import java.util.Set;

public record PublicUserVideoMapperContext(
        Long currentUserId,
        Map<Long, String> avatarByUserId,
        Set<Long> likedVideoIds
) {
}
