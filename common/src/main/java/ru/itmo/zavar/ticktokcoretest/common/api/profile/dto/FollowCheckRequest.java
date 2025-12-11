package ru.itmo.zavar.ticktokcoretest.common.api.profile.dto;

import java.util.Set;

public record FollowCheckRequest(
        Long followerId,
        Set<Long> candidateIds
) {
}
