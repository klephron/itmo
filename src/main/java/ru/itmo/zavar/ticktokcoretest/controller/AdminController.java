package ru.itmo.zavar.ticktokcoretest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.dto.request.UpdateUserRolesRequest;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;
import ru.itmo.zavar.ticktokcoretest.security.Role;
import ru.itmo.zavar.ticktokcoretest.service.VideoStreamingService;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final VideoStreamingService videoStreamingService;
    private final UserRepository userRepository;

    @PostMapping("/thumbnails/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> regenerateThumbnails(@RequestParam(name = "force", defaultValue = "true") boolean force) {
        int updated = this.videoStreamingService.regenerateAllThumbnails(force);
        return ResponseEntity.ok(Map.of(
                "updated", updated,
                "force", force
        ));
    }

    @GetMapping("/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Set<Role>> getUserRoles(@PathVariable Long id) {
        return this.userRepository.findById(id)
                .map(UserEntity::getRoles)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Set<Role>> updateUserRoles(@PathVariable Long id,
                                                     @RequestBody UpdateUserRolesRequest req) {
        return this.userRepository.findById(id)
                .map(u -> {
                    u.getRoles().clear();
                    if (req.roles() != null && !req.roles().isEmpty()) {
                        u.getRoles().addAll(req.roles());
                    } else {
                        u.getRoles().add(Role.USER);
                    }
                    this.userRepository.save(u);
                    return ResponseEntity.ok(u.getRoles());
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

