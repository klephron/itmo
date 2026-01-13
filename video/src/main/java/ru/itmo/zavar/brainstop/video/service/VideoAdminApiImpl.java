package ru.itmo.zavar.brainstop.video.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.video.VideoAdminApi;
import ru.itmo.zavar.brainstop.video.streaming.service.VideoStreamingService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoAdminApiImpl implements VideoAdminApi {

    private final VideoStreamingService videoStreamingService;

    @Override
    public int regenerateThumbnails(boolean force) {
        return this.videoStreamingService.regenerateAllThumbnails(force);
    }

    @Override
    public void deleteVideo(UUID uuid) {
        try {
            this.videoStreamingService.deleteVideo(uuid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete video " + uuid, e);
        }
    }

    @Override
    public int rebuildEmbeddings() {
        return this.videoStreamingService.rebuildEmbeddings();
    }
}
