package ru.itmo.zavar.ticktokcoretest.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.admin.dto.UserDetailDto;
import ru.itmo.zavar.ticktokcoretest.admin.mapper.ProfileModerationMapper;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.ProfileAdminApi;
import ru.itmo.zavar.ticktokcoretest.common.api.video.VideoModerationApi;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.UserVideoDto;

import java.util.List;

@RestController
@RequestMapping("/moderation")
@RequiredArgsConstructor
// MSPLIT: Moderation API for future Admin/Moderation service
public class ModerationController {

    private final ProfileAdminApi profileAdminApi;
    private final VideoModerationApi videoModerationApi;
    private final ProfileModerationMapper profileModerationMapper;

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public List<UserDetailDto> listUsers() {
        return this.profileAdminApi.findAllUsers().stream()
                .map(this.profileModerationMapper::toDto)
                .toList();
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public ResponseEntity<UserDetailDto> getUser(@PathVariable Long id) {
        return this.profileAdminApi.findUser(id)
                .map(this.profileModerationMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{id}/videos")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public ResponseEntity<List<UserVideoDto>> getUserVideos(@PathVariable Long id) {
        if (!this.profileAdminApi.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(this.videoModerationApi.findByUploaderId(id));
    }

    @PostMapping("/users/{id}/ban")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        return this.profileAdminApi.findUser(id)
                .map(u -> {
                    this.profileAdminApi.setEnabled(id, false);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/unban")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public ResponseEntity<Void> unbanUser(@PathVariable Long id) {
        return this.profileAdminApi.findUser(id)
                .map(u -> {
                    this.profileAdminApi.setEnabled(id, true);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
