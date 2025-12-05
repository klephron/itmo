package ru.itmo.zavar.ticktokcoretest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.dto.UserDetailDto;
import ru.itmo.zavar.ticktokcoretest.dto.UserVideoDto;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepository;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;

import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/moderation")
public class ModerationController {

    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;

    public ModerationController(UserRepository userRepository,
                                FileMetadataRepository fileMetadataRepository) {
        this.userRepository = userRepository;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    // список пользователей
    @GetMapping("/users")
    @PreAuthorize("hasRole('MODERATOR')") // ADMIN тоже пройдёт по иерархии
    public List<UserDetailDto> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserDetailDto(
                        u.getId(),
                        u.getUsername(),
                        u.getDisplayName(),
                        u.getEmail(),
                        u.isEnabled(),
                        u.getRoles(),
                        u.getCreatedAt()
                ))
                .toList();
    }

    // детали пользователя
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<UserDetailDto> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> new UserDetailDto(
                        u.getId(),
                        u.getUsername(),
                        u.getDisplayName(),
                        u.getEmail(),
                        u.isEnabled(),
                        u.getRoles(),
                        u.getCreatedAt()
                ))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // видео пользователя
    @GetMapping("/users/{id}/videos")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<List<UserVideoDto>> getUserVideos(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        List<UserVideoDto> videos = fileMetadataRepository.findByUploaderId(id).stream()
                .map(this::toUserVideoDto)
                .toList();

        return ResponseEntity.ok(videos);
    }

    private UserVideoDto toUserVideoDto(FileMetadataEntity f) {

        String thumbnailUrl = (f.getThumbnailKey() != null)
                ? "/streaming/video/thumbnail/" + f.getUuid()
                : null;

        UserEntity uploader = userRepository.findById(f.getUploaderId())
                .orElse(null);

        String uploaderAvatarUrl = (uploader != null && uploader.getAvatarKey() != null)
                ? "/api/users/" + uploader.getId() + "/avatar"
                : null;

        return new UserVideoDto(
                f.getUuid(),
                f.getName(),
                f.getUploaderName(),
                f.getContentSize(),
                f.getContentType(),
                f.getUploadedAt().toInstant(ZoneOffset.UTC),
                thumbnailUrl,
                uploaderAvatarUrl
        );
    }


    // бан (disable)
    @PostMapping("/users/{id}/ban")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> {
                    u.setEnabled(false);
                    userRepository.save(u);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // разбан (enable)
    @PostMapping("/users/{id}/unban")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> unbanUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> {
                    u.setEnabled(true);
                    userRepository.save(u);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
