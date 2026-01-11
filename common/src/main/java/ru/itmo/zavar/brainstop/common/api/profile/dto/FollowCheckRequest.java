package ru.itmo.zavar.brainstop.common.api.profile.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record FollowCheckRequest(
        @NotNull
        Long followerId,
        @NotNull
        Set<Long> candidateIds
) {
}
