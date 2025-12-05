package ru.itmo.zavar.ticktokcoretest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.dto.response.ViewResponse;
import ru.itmo.zavar.ticktokcoretest.service.AuthService;
import ru.itmo.zavar.ticktokcoretest.service.ViewService;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class ViewController {

    private final ViewService viewService;

    @PostMapping("/{uuid}/view")
    public ResponseEntity<ViewResponse> view(@PathVariable UUID uuid, Authentication authentication) {
        Long userId = AuthService.extractUserId(authentication);
        long count = this.viewService.viewVideo(uuid, userId);
        return ResponseEntity.ok(new ViewResponse(count));
    }

    @GetMapping("/{uuid}/view")
    public ResponseEntity<ViewResponse> getViews(@PathVariable UUID uuid, Authentication authentication) {
        return ResponseEntity.ok(new ViewResponse(this.viewService.getViews(uuid)));
    }
}