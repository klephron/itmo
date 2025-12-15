package ru.itmo.zavar.ticktokcoretest.interactions.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itmo.zavar.ticktokcoretest.like.dto.response.LikeResponse;

import java.util.UUID;

@FeignClient(name = "core-service", contextId = "likeInternalClient", path = "/internal/interactions")
public interface LikeInternalClient {

    @PostMapping("/videos/{uuid}/like")
    LikeResponse like(@PathVariable("uuid") UUID uuid, @RequestParam("userId") Long userId);

    @DeleteMapping("/videos/{uuid}/like")
    LikeResponse unlike(@PathVariable("uuid") UUID uuid, @RequestParam("userId") Long userId);

    @GetMapping("/videos/{uuid}/like")
    LikeResponse status(@PathVariable("uuid") UUID uuid, @RequestParam("userId") Long userId);
}
