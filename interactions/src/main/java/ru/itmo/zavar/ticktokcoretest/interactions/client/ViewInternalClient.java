package ru.itmo.zavar.ticktokcoretest.interactions.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itmo.zavar.ticktokcoretest.like.dto.response.ViewResponse;

import java.util.UUID;

@FeignClient(name = "core-service", contextId = "viewInternalClient", path = "/internal/interactions")
public interface ViewInternalClient {

    @PostMapping("/videos/{uuid}/view")
    ViewResponse view(@PathVariable("uuid") UUID uuid, @RequestParam(value = "userId", required = false) Long userId);

    @GetMapping("/videos/{uuid}/view")
    ViewResponse count(@PathVariable("uuid") UUID uuid);
}
