package ru.itmo.zavar.ticktokcoretest.common.api.rabbit;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record VideoViewEvent(
        Long userId,
        UUID videoUuid,
        Long videoId,
        Long viewsCount,
        Instant timestamp
) implements Serializable {
}
