package ru.itmo.zavar.ticktokcoretest.video.streaming.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.common.api.video.VideoAdminApi;

@Service
@RequiredArgsConstructor
public class VideoAdminApiImpl implements VideoAdminApi {

    private final VideoStreamingService videoStreamingService;

    @Override
    public int regenerateThumbnails(boolean force) {
        return this.videoStreamingService.regenerateAllThumbnails(force);
    }
}
