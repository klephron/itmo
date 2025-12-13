package ru.itmo.zavar.ticktokcoretest.like.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.ticktokcoretest.like.dto.response.ViewResponse;
import ru.itmo.zavar.ticktokcoretest.like.service.ViewService;
import ru.itmo.zavar.ticktokcoretest.common.security.CurrentUser;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Tag(name = "Views", description = "Просмотры видео")
// MSPLIT: metrics API for future Like/Metrics service
public class ViewController {

    private final ViewService viewService;

    @PostMapping("/{uuid}/view")
    @Operation(summary = "Отметить просмотр видео")
    public ResponseEntity<ViewResponse> view(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        long count = this.viewService.viewVideo(uuid, principal.getId());
        return ResponseEntity.ok(new ViewResponse(count));
    }

    @GetMapping("/{uuid}/view")
    @Operation(summary = "Получить число просмотров")
    public ResponseEntity<ViewResponse> getViews(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(new ViewResponse(this.viewService.getViews(uuid)));
    }
}
