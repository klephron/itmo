package ru.itmo.zavar.ticktokcoretest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.dto.UserDetailDto;
import ru.itmo.zavar.ticktokcoretest.dto.UserVideoDto;
import ru.itmo.zavar.ticktokcoretest.mapper.FileMetadataToUserVideoMapper;
import ru.itmo.zavar.ticktokcoretest.mapper.UserMapper;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepository;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserMapper userMapper;
    private final FileMetadataToUserVideoMapper fileMetadataToUserVideoMapper;

    @GetMapping("/users")
    @PreAuthorize("hasRole('MODERATOR')")
    public List<UserDetailDto> listUsers() {
        return userMapper.toDto(this.userRepository.findAll());
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<UserDetailDto> getUser(@PathVariable Long id) {
        return this.userRepository.findById(id)
                .map(this.userMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{id}/videos")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<List<UserVideoDto>> getUserVideos(@PathVariable Long id) {
        if (!this.userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(this.fileMetadataToUserVideoMapper.toDto(this.fileMetadataRepository.findByUploaderId(id)));
    }

    @PostMapping("/users/{id}/ban")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        return this.userRepository.findById(id)
                .map(u -> {
                    u.setEnabled(false);
                    this.userRepository.save(u);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/unban")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> unbanUser(@PathVariable Long id) {
        return this.userRepository.findById(id)
                .map(u -> {
                    u.setEnabled(true);
                    this.userRepository.save(u);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
