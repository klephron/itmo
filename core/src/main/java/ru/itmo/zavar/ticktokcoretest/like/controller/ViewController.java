package ru.itmo.zavar.ticktokcoretest.like.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.like.dto.response.ViewResponse;
import ru.itmo.zavar.ticktokcoretest.like.service.ViewService;
import ru.itmo.zavar.ticktokcoretest.security.CurrentUser;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
// MSPLIT: metrics API for future Like/Metrics service
public class ViewController {

    private final ViewService viewService;

    @PostMapping("/{uuid}/view")
    public ResponseEntity<ViewResponse> view(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        long count = this.viewService.viewVideo(uuid, principal.getId());
        return ResponseEntity.ok(new ViewResponse(count));
    }

    @GetMapping("/{uuid}/view")
    public ResponseEntity<ViewResponse> getViews(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(new ViewResponse(this.viewService.getViews(uuid)));
    }
}
