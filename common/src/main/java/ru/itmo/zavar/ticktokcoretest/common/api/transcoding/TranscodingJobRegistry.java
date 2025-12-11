package ru.itmo.zavar.ticktokcoretest.common.api.transcoding;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface TranscodingJobRegistry {

    void put(UUID id, TranscodingJobStatus status);

    Optional<TranscodingJobStatus> get(UUID id);

    void update(UUID id, Consumer<TranscodingJobStatus> updater);

    Map<UUID, TranscodingJobStatus> getActiveJobsForUser(Long userId);
}
