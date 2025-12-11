package ru.itmo.zavar.ticktokcoretest.profile.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.FollowQueryApi;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.ProfileAdminApi;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.UserSummary;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.dto.FollowCheckRequest;
import ru.itmo.zavar.ticktokcoretest.common.security.Role;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/internal/profile")
@RequiredArgsConstructor
public class ProfileInternalController {

    private final UserProfilePublicApi userProfilePublicApi;
    private final FollowQueryApi followQueryApi;
    private final ProfileAdminApi profileAdminApi;

    @GetMapping("/users/{id}/summary")
    public ResponseEntity<UserSummary> getSummary(@PathVariable Long id) {
        return this.userProfilePublicApi.findUserSummary(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/follow/check")
    public ResponseEntity<Set<Long>> checkFollowing(@RequestBody FollowCheckRequest request) {
        if (request == null || request.followerId() == null || request.candidateIds() == null || request.candidateIds().isEmpty()) {
            return ResponseEntity.ok(Collections.emptySet());
        }
        Set<Long> matches = this.followQueryApi.getFollowingWithin(request.followerId(), request.candidateIds());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/admin/users")
    public List<ProfileUserDetails> findAllUsers() {
        return this.profileAdminApi.findAllUsers();
    }

    @GetMapping("/admin/users/{id}")
    public ResponseEntity<ProfileUserDetails> findUser(@PathVariable Long id) {
        return this.profileAdminApi.findUser(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/users/{id}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable Long id) {
        return ResponseEntity.ok(this.profileAdminApi.existsById(id));
    }

    @PostMapping("/admin/users/{id}/enabled")
    public ResponseEntity<ProfileUserDetails> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        ProfileUserDetails updated = this.profileAdminApi.setEnabled(id, enabled);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/admin/users/{id}/roles")
    public ResponseEntity<ProfileUserDetails> updateRoles(@PathVariable Long id, @RequestBody Set<Role> roles) {
        ProfileUserDetails updated = this.profileAdminApi.updateRoles(id, roles);
        return ResponseEntity.ok(updated);
    }
}
