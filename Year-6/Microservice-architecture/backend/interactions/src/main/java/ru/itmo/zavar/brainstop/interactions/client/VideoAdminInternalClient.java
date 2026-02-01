package ru.itmo.zavar.brainstop.interactions.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "video-service", contextId = "videoAdminInternalClient", path = "/internal/admin", fallbackFactory = VideoAdminInternalClientFallbackFactory.class)
public interface VideoAdminInternalClient {

    @DeleteMapping("/videos/{uuid}")
    void deleteVideo(@PathVariable("uuid") UUID videoUuid);
}
