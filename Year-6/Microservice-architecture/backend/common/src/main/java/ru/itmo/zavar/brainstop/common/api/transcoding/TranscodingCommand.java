package ru.itmo.zavar.brainstop.common.api.transcoding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record TranscodingCommand(
        @NotNull
        UUID jobId,
        @NotBlank
        String storageKey,
        String videoName,
        String description,
        @NotNull
        Long userId,
        String displayName,
        @NotEmpty
        List<Integer> targetHeights
) {
}
