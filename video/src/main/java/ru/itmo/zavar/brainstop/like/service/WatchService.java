package ru.itmo.zavar.brainstop.like.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.like.dto.request.WatchProgressRequest;
import ru.itmo.zavar.brainstop.like.repo.VideoWatchRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchService {

    private final VideoWatchRepository videoWatchRepository;
    private final VideoMetadataPublicApi videoMetadataPublicApi;

    @Transactional
    public void recordProgress(UUID videoUuid, Long userId, WatchProgressRequest request) {
        if (userId == null || request == null) {
            return;
        }
        VideoMetadataSummary video = this.videoMetadataPublicApi.getRequired(videoUuid);

        long duration = resolveDurationSeconds(request);
        long position = resolvePositionSeconds(request, duration);
        if (duration > 0 && position > duration) {
            position = duration;
        }
        if (position < 0) {
            position = 0;
        }

        this.videoWatchRepository.upsertProgress(video.id(), userId, position, duration);
    }

    private long resolveDurationSeconds(WatchProgressRequest request) {
        Long duration = request.durationSeconds();
        return duration != null ? duration : 0L;
    }

    private long resolvePositionSeconds(WatchProgressRequest request, long duration) {
        Long position = request.positionSeconds();
        if (position != null) {
            return position;
        }
        Integer percent = request.percent();
        if (percent != null && duration > 0) {
            return Math.round(duration * (percent / 100.0));
        }
        return 0L;
    }
}
