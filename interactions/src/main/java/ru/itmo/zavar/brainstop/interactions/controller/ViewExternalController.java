package ru.itmo.zavar.brainstop.interactions.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.like.dto.response.ViewResponse;
import ru.itmo.zavar.brainstop.like.service.ViewService;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Tag(name = "Views", description = "Просмотры (interactions-service)")
public class ViewExternalController {

    private final ViewService viewService;

    @PostMapping("/{uuid}/view")
    @Operation(summary = "Отметить просмотр видео")
    public ResponseEntity<ViewResponse> view(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        Long userId = principal != null ? principal.getId() : null;
        long count = this.viewService.viewVideo(uuid, userId);
        return ResponseEntity.ok(new ViewResponse(count));
    }

    @GetMapping("/{uuid}/view")
    @Operation(summary = "Получить число просмотров")
    public ResponseEntity<ViewResponse> getViews(@PathVariable UUID uuid) {
        long count = this.viewService.getViews(uuid);
        return ResponseEntity.ok(new ViewResponse(count));
    }
}
