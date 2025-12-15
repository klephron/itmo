package ru.itmo.zavar.ticktokcoretest.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "core-service", contextId = "videoAdminClient", path = "/internal/admin")
public interface VideoAdminClient {

    @PostMapping("/thumbnails/rebuild")
    Map<String, Object> regenerateThumbnails(@RequestParam("force") boolean force);
}
