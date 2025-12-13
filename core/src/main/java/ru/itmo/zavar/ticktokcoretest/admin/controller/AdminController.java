package ru.itmo.zavar.ticktokcoretest.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.ticktokcoretest.admin.dto.UpdateUserRolesRequest;
import ru.itmo.zavar.ticktokcoretest.common.security.Role;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.ProfileAdminApi;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.ticktokcoretest.common.api.video.VideoAdminApi;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Админские операции (мини-модуль)")
// MSPLIT: Admin boundary for future Admin/Moderation service
public class AdminController {

    private final VideoAdminApi videoAdminApi;
    private final ProfileAdminApi profileAdminApi;

    @PostMapping("/thumbnails/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Пересоздать превью всех видео")
    public ResponseEntity<Map<String, Object>> regenerateThumbnails(@RequestParam(name = "force", defaultValue = "true") boolean force) {
        int updated = this.videoAdminApi.regenerateThumbnails(force);
        return ResponseEntity.ok(Map.of(
                "updated", updated,
                "force", force
        ));
    }

    @GetMapping("/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить роли пользователя")
    public ResponseEntity<Set<Role>> getUserRoles(@PathVariable Long id) {
        return this.profileAdminApi.findUser(id)
                .map(ProfileUserDetails::roles)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить роли пользователя")
    public ResponseEntity<Set<Role>> updateUserRoles(@PathVariable Long id,
                                                     @RequestBody UpdateUserRolesRequest req) {
        try {
            ProfileUserDetails updated = this.profileAdminApi.updateRoles(id, req.roles() == null ? Set.of(Role.USER) : req.roles());
            return ResponseEntity.ok(updated.roles());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
