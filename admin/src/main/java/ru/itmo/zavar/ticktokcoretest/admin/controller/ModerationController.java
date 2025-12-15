package ru.itmo.zavar.ticktokcoretest.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.ticktokcoretest.admin.client.ProfileAdminClient;
import ru.itmo.zavar.ticktokcoretest.admin.client.VideoModerationClient;
import ru.itmo.zavar.ticktokcoretest.admin.dto.UserDetailDto;
import ru.itmo.zavar.ticktokcoretest.admin.mapper.ProfileModerationMapper;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.UserVideoDto;

import java.util.List;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@Tag(name = "Moderation", description = "Модерация пользователей и их видео (отдельный сервис)")
public class ModerationController {

    private final ProfileAdminClient profileAdminClient;
    private final VideoModerationClient videoModerationClient;
    private final ProfileModerationMapper profileModerationMapper;

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @Operation(summary = "Список пользователей для модерации")
    public List<UserDetailDto> listUsers() {
        return this.profileAdminClient.findAllUsers().stream()
                .map(this.profileModerationMapper::toDto)
                .toList();
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @Operation(summary = "Получить детали пользователя для модерации")
    public ResponseEntity<UserDetailDto> getUser(@PathVariable Long id) {
        try {
            ProfileUserDetails details = this.profileAdminClient.findUser(id);
            return ResponseEntity.ok(this.profileModerationMapper.toDto(details));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/users/{id}/videos")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @Operation(summary = "Получить видео пользователя")
    public ResponseEntity<List<UserVideoDto>> getUserVideos(@PathVariable Long id) {
        Boolean exists = this.profileAdminClient.exists(id);
        if (exists != null && !exists) {
            return ResponseEntity.notFound().build();
        }
        List<UserVideoDto> videos = this.videoModerationClient.getUserVideos(id);
        return ResponseEntity.ok(videos);
    }

    @PostMapping("/users/{id}/ban")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @Operation(summary = "Забанить пользователя")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        ProfileUserDetails updated = this.profileAdminClient.setEnabled(id, false);
        return updated != null ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/users/{id}/unban")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @Operation(summary = "Разбанить пользователя")
    public ResponseEntity<Void> unbanUser(@PathVariable Long id) {
        ProfileUserDetails updated = this.profileAdminClient.setEnabled(id, true);
        return updated != null ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
