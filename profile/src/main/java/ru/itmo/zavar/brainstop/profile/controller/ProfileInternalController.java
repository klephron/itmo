package ru.itmo.zavar.brainstop.profile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.brainstop.common.api.profile.FollowQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileAdminApi;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.api.profile.dto.FollowCheckRequest;
import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/internal/profile")
@RequiredArgsConstructor
@Tag(name = "Internal-Profile", description = "Внутренние эндпоинты профилей (summary, follow-check, админские операции)")
public class ProfileInternalController {

    private final UserProfilePublicApi userProfilePublicApi;
    private final FollowQueryApi followQueryApi;
    private final ProfileAdminApi profileAdminApi;

    @GetMapping("/users/{id}/summary")
    @Operation(summary = "Внутренний: summary профиля по userId")
    public ResponseEntity<UserSummaryDto> getSummary(@PathVariable Long id) {
        return this.userProfilePublicApi.findUserSummary(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/follow/check")
    @Operation(summary = "Внутренний: проверка подписок follower -> candidateIds")
    public ResponseEntity<Set<Long>> checkFollowing(@Valid @RequestBody FollowCheckRequest request) {
        if (request == null || request.followerId() == null || request.candidateIds() == null || request.candidateIds().isEmpty()) {
            return ResponseEntity.ok(Collections.emptySet());
        }
        Set<Long> matches = this.followQueryApi.getFollowingWithin(request.followerId(), request.candidateIds());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/admin/users")
    @Operation(summary = "Внутренний: список пользователей (админ)")
    public List<ProfileUserDetails> findAllUsers() {
        return this.profileAdminApi.findAllUsers();
    }

    @GetMapping("/admin/users/{id}")
    @Operation(summary = "Внутренний: получить пользователя (админ)")
    public ResponseEntity<ProfileUserDetails> findUser(@PathVariable Long id) {
        return this.profileAdminApi.findUser(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/users/{id}/exists")
    @Operation(summary = "Внутренний: проверить наличие пользователя (админ)")
    public ResponseEntity<Boolean> exists(@PathVariable Long id) {
        return ResponseEntity.ok(this.profileAdminApi.existsById(id));
    }

    @PostMapping("/admin/users/{id}/enabled")
    @Operation(summary = "Внутренний: включить/выключить пользователя (админ)")
    public ResponseEntity<ProfileUserDetails> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        ProfileUserDetails updated = this.profileAdminApi.setEnabled(id, enabled);
        if (updated == null) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/admin/users/{id}/roles")
    @Operation(summary = "Внутренний: обновить роли пользователя (админ)")
    public ResponseEntity<ProfileUserDetails> updateRoles(@PathVariable Long id, @RequestBody Set<Role> roles) {
        ProfileUserDetails updated = this.profileAdminApi.updateRoles(id, roles);
        if (updated == null) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok(updated);
    }
}
