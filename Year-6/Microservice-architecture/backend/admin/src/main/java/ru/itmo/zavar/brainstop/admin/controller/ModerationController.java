package ru.itmo.zavar.brainstop.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.admin.client.ProfileAdminClient;
import ru.itmo.zavar.brainstop.admin.client.VideoModerationClient;
import ru.itmo.zavar.brainstop.admin.dto.UserDetailDto;
import ru.itmo.zavar.brainstop.admin.mapper.ProfileModerationMapper;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.api.video.dto.UserVideoDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@Tag(name = "Модерация", description = "Модерация пользователей и их видео (отдельный сервис)")
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
            if (details == null) {
                return ResponseEntity.status(503).build();
            }
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
        if (exists == null) {
            return ResponseEntity.status(503).build();
        }
        if (exists != null && !exists) {
            return ResponseEntity.notFound().build();
        }
        List<UserVideoDto> videos = this.videoModerationClient.getUserVideos(id);
        return ResponseEntity.ok(videos);
    }

    @PostMapping("/videos/reembed")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @Operation(summary = "Пересчитать эмбеддинги видео для поиска")
    public ResponseEntity<Map<String, Object>> reembedVideos() {
        Map<String, Object> response = this.videoModerationClient.rebuildEmbeddings();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{id}/ban")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @Operation(summary = "Заблокировать пользователя")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        ProfileUserDetails updated = this.profileAdminClient.setEnabled(id, false);
        if (updated == null) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/unban")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @Operation(summary = "Разблокировать пользователя")
    public ResponseEntity<Void> unbanUser(@PathVariable Long id) {
        ProfileUserDetails updated = this.profileAdminClient.setEnabled(id, true);
        if (updated == null) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok().build();
    }
}
