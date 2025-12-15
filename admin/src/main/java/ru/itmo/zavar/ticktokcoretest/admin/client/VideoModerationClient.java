package ru.itmo.zavar.ticktokcoretest.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.itmo.zavar.ticktokcoretest.common.api.PageResponse;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.UserVideoDto;

import java.util.List;

@FeignClient(name = "core-service", contextId = "videoModerationClient", path = "/internal/admin")
public interface VideoModerationClient {

    @GetMapping("/users/{id}/videos")
    List<UserVideoDto> getUserVideos(@PathVariable("id") Long userId);
}
