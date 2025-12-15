package ru.itmo.zavar.ticktokcoretest.common.api.transcoding;

import java.util.List;
import java.util.UUID;

public record TranscodingCommand(
        UUID jobId,
        String sourcePath,
        List<Integer> targetHeights
) {
}
