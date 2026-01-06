package ru.itmo.zavar.brainstop.profile.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import feign.FeignException;
import ru.itmo.zavar.brainstop.profile.client.AuthInternalClient;
import ru.itmo.zavar.brainstop.profile.dto.PublicUserShortDto;
import ru.itmo.zavar.brainstop.profile.service.FollowService;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Follows", description = "Подписки/отписки и списки фолловеров")
// MSPLIT: boundary for future User Profile/Follow API
public class FollowController {

    private final FollowService followService;
    private final AuthInternalClient authInternalClient;

    @PostMapping("/{id}/follow")
    @Operation(summary = "Подписаться на пользователя")
    public ResponseEntity<Void> follow(@PathVariable Long id, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long currentUserId = principal.getId();

        if (!userExists(id)) {
            return ResponseEntity.notFound().build();
        }

        this.followService.follow(currentUserId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/follow")
    @Operation(summary = "Отписаться от пользователя")
    public ResponseEntity<Void> unfollow(@PathVariable Long id, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long currentUserId = principal.getId();

        if (!userExists(id)) {
            return ResponseEntity.notFound().build();
        }

        this.followService.unfollow(currentUserId, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/followers")
    @Operation(summary = "Список подписчиков пользователя")
    public ResponseEntity<List<PublicUserShortDto>> followers(@PathVariable Long id) {
        if (!userExists(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(this.followService.getFollowers(id));
    }

    @GetMapping("/{id}/following")
    @Operation(summary = "Кого читает пользователь")
    public ResponseEntity<List<PublicUserShortDto>> following(@PathVariable Long id) {
        if (!userExists(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(this.followService.getFollowing(id));
    }


    private boolean userExists(Long id) {
        try {
            return Boolean.TRUE.equals(this.authInternalClient.exists(id));
        } catch (FeignException e) {
            return false;
        }
    }
}
