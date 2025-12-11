package ru.itmo.zavar.ticktokcoretest.common.api.transcoding;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.List;

public interface VideoTranscoder {

    List<TranscodedRendition> transcodeToMp4WithProgress(Path inputTempFile, UUID jobId, List<Integer> targetHeights) throws IOException, InterruptedException;
}
