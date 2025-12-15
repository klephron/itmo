package ru.itmo.zavar.ticktokcoretest.interactions.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.ticktokcoretest.common.security.CurrentUser;
import ru.itmo.zavar.ticktokcoretest.interactions.client.ViewInternalClient;
import ru.itmo.zavar.ticktokcoretest.like.dto.response.ViewResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Tag(name = "Views", description = "Просмотры (interactions-service)")
public class ViewExternalController {

    private final ViewInternalClient viewInternalClient;

    @PostMapping("/{uuid}/view")
    @Operation(summary = "Отметить просмотр видео")
    public ResponseEntity<ViewResponse> view(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        Long userId = principal != null ? principal.getId() : null;
        ViewResponse resp = this.viewInternalClient.view(uuid, userId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{uuid}/view")
    @Operation(summary = "Получить число просмотров")
    public ResponseEntity<ViewResponse> getViews(@PathVariable UUID uuid) {
        return ResponseEntity.ok(this.viewInternalClient.count(uuid));
    }
}
