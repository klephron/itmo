package ru.itmo.zavar.brainstop.common.api.video;

public interface VideoAdminApi {

    int regenerateThumbnails(boolean force);

    void deleteVideo(java.util.UUID uuid);
}
