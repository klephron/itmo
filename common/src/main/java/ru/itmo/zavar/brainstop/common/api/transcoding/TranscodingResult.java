package ru.itmo.zavar.brainstop.common.api.transcoding;

import java.util.List;
import java.util.UUID;

public record TranscodingResult(
        UUID jobId,
        List<TranscodedRenditionMetadata> renditions
) {
}
