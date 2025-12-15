package ru.itmo.zavar.ticktokcoretest.common.api.transcoding;

import java.time.Instant;
import java.util.UUID;

public record TranscodingProgressEvent(
        UUID jobId,
        int percent,
        String message,
        TranscodingJobStage stage,
        Instant occurredAt
) {
}
