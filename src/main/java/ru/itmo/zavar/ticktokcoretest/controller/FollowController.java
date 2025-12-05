package ru.itmo.zavar.ticktokcoretest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.dto.PublicUserShortDto;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;
import ru.itmo.zavar.ticktokcoretest.service.AuthService;
import ru.itmo.zavar.ticktokcoretest.service.FollowService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final UserRepository userRepository;

    @PostMapping("/{id}/follow")
    public ResponseEntity<Void> follow(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = AuthService.extractUserId(authentication);

        if (!this.userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        this.followService.follow(currentUserId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = AuthService.extractUserId(authentication);

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

