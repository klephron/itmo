package ru.itmo.zavar.brainstop.interactions.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.interactions.client.ViewInternalClient;
import ru.itmo.zavar.brainstop.like.dto.request.WatchProgressRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Tag(name = "Watch", description = "Прогресс просмотра (interactions-service)")
public class WatchExternalController {

    private final ViewInternalClient viewInternalClient;

    @PostMapping("/{uuid}/watch")
    @Operation(summary = "Зафиксировать прогресс просмотра")
    public ResponseEntity<Void> watch(@PathVariable UUID uuid,
                                      @AuthenticationPrincipal CurrentUser principal,
                                      @RequestBody WatchProgressRequest request) {
        Long userId = principal != null ? principal.getId() : null;
        this.viewInternalClient.watch(uuid, userId, request);
        return ResponseEntity.noContent().build();
    }
}
