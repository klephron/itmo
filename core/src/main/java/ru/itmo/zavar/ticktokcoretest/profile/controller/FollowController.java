package ru.itmo.zavar.ticktokcoretest.profile.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.profile.dto.PublicUserShortDto;
import ru.itmo.zavar.ticktokcoretest.profile.repo.UserRepository;
import ru.itmo.zavar.ticktokcoretest.profile.service.FollowService;
import ru.itmo.zavar.ticktokcoretest.security.CurrentUser;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
// MSPLIT: boundary for future User Profile/Follow API
public class FollowController {

    private final FollowService followService;
    private final UserRepository userRepository;

    @PostMapping("/{id}/follow")
    public ResponseEntity<Void> follow(@PathVariable Long id, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long currentUserId = principal.getId();

        if (!this.userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        this.followService.follow(currentUserId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable Long id, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long currentUserId = principal.getId();

        if (!this.userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        this.followService.unfollow(currentUserId, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/followers")
    public ResponseEntity<List<PublicUserShortDto>> followers(@PathVariable Long id) {
        if (!this.userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(this.followService.getFollowers(id));
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<List<PublicUserShortDto>> following(@PathVariable Long id) {
        if (!this.userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(this.followService.getFollowing(id));
    }
}

