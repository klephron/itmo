package ru.itmo.zavar.brainstop.common.api.transcoding;

import java.util.List;
import java.util.UUID;

public record TranscodingCommand(
        UUID jobId,
        String storageKey,
        String videoName,
        String description,
        Long userId,
        String displayName,
        List<Integer> targetHeights
) {
}
