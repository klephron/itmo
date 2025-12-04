package ru.itmo.zavar.ticktokcoretest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.ticktokcoretest.service.VideoStreamingService;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final VideoStreamingService videoStreamingService;

    @PostMapping("/thumbnails/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> regenerateThumbnails(
            @RequestParam(name = "force", defaultValue = "true") boolean force
    ) {
        int updated = videoStreamingService.regenerateAllThumbnails(force);
        return ResponseEntity.ok(Map.of(
                "updated", updated,
                "force", force
        ));
    }
}

