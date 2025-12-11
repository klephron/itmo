package ru.itmo.zavar.ticktokcoretest.profile.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itmo.zavar.ticktokcoretest.common.api.PageResponse;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.PublicUserVideoDto;

@FeignClient(name = "core-service", path = "/internal/videos")
public interface VideoInternalClient {

    @GetMapping("/users/{uploaderId}")
    PageResponse<PublicUserVideoDto> getUserVideos(@PathVariable("uploaderId") Long uploaderId,
                                                   @RequestParam("page") int page,
                                                   @RequestParam("size") int size);
}
