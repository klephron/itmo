package ru.itmo.zavar.ticktokcoretest.common.api.rabbit;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record VideoLikeEvent(
        String type,        // "VIDEO_LIKED" / "VIDEO_UNLIKED"
        Long userId,
        UUID videoUuid,
        Long videoId,
        Long likesCount,
        Instant occurredAt
) implements Serializable {
}
