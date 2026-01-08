package ru.itmo.zavar.brainstop.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.admin.client.ProfileAdminClient;
import ru.itmo.zavar.brainstop.admin.client.VideoAdminClient;
import ru.itmo.zavar.brainstop.admin.dto.UpdateUserRolesRequest;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Админские операции (вынесены в отдельный сервис)")
public class AdminController {

    private final VideoAdminClient videoAdminClient;
    private final ProfileAdminClient profileAdminClient;

    @PostMapping("/thumbnails/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Пересоздать превью всех видео")
    public ResponseEntity<Map<String, Object>> regenerateThumbnails(@RequestParam(name = "force", defaultValue = "true") boolean force) {
        Map<String, Object> resp = this.videoAdminClient.regenerateThumbnails(force);
        if (resp != null && "unavailable".equals(resp.get("status"))) {
            return ResponseEntity.status(503).body(resp);
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить роли пользователя")
    public ResponseEntity<Set<Role>> getUserRoles(@PathVariable Long id) {
        try {
            ProfileUserDetails details = this.profileAdminClient.findUser(id);
            if (details == null) {
                return ResponseEntity.status(503).build();
            }
            return ResponseEntity.ok(details.roles());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить роли пользователя")
    public ResponseEntity<Set<Role>> updateUserRoles(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateUserRolesRequest req) {
        try {
            ProfileUserDetails updated = this.profileAdminClient.updateRoles(id, req.roles());
            if (updated == null) {
                return ResponseEntity.status(503).build();
            }
            return ResponseEntity.ok(updated.roles());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
