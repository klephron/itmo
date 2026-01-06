package ru.itmo.zavar.brainstop.profile.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;

@FeignClient(name = "video-service", path = "/internal/videos")
public interface VideoInternalClient {

    @GetMapping("/users/{uploaderId}")
    PageResponse<PublicUserVideoDto> getUserVideos(@PathVariable("uploaderId") Long uploaderId,
                                                   @RequestParam("page") int page,
                                                   @RequestParam("size") int size,
                                                   @RequestParam(value = "sort", required = false) String sort,
                                                   @RequestParam(value = "userId", required = false) Long currentUserId);
}
