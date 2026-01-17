package ru.itmo.zavar.brainstop.interactions.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.like.dto.request.WatchProgressRequest;
import ru.itmo.zavar.brainstop.like.service.WatchService;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Tag(name = "Прогресс просмотра", description = "Прогресс просмотра видео (interactions-service)")
public class WatchExternalController {

    private final WatchService watchService;

    @PostMapping("/{uuid}/watch")
    @Operation(summary = "Сохранить прогресс просмотра видео")
    public ResponseEntity<Void> watch(@PathVariable UUID uuid,
                                      @AuthenticationPrincipal CurrentUser principal,
                                      @Valid @RequestBody WatchProgressRequest request) {
        Long userId = principal != null ? principal.getId() : null;
        this.watchService.recordProgress(uuid, userId, request);
        return ResponseEntity.noContent().build();
    }
}
