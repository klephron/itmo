package ru.itmo.zavar.brainstop.profile.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.profile.cache.UserSummaryCache;
import ru.itmo.zavar.brainstop.profile.dto.PublicUserProfileDto;
import ru.itmo.zavar.brainstop.profile.dto.response.UserAvatarResponse;
import ru.itmo.zavar.brainstop.profile.mapper.UserToPublicProfileMapper;
import ru.itmo.zavar.brainstop.profile.model.UserEntity;
import ru.itmo.zavar.brainstop.profile.repo.UserRepository;
import ru.itmo.zavar.brainstop.common.api.video.VideoProfileApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Профиль, аватар и видео пользователя")
// MSPLIT: HTTP API that will move to User Profile Service
public class UserProfileController {

    private final UserRepository userRepository;
    private final VideoObjectStorage videoObjectStorage;
    private final UserToPublicProfileMapper userToPublicProfileMapper;
    private final VideoProfileApi videoProfileApi;
    private final UserSummaryCache userSummaryCache;

    @PostMapping("/me/avatar")
    @Operation(summary = "Загрузить аватар текущего пользователя")
    public ResponseEntity<UserAvatarResponse> uploadAvatar(@RequestParam("avatar") MultipartFile avatar, @AuthenticationPrincipal CurrentUser principal) throws Exception {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = principal.getId();
        UserEntity userEntity = this.userRepository.findById(userId).orElseThrow();

        try (InputStream is = avatar.getInputStream()) {
            String key = this.videoObjectStorage.uploadAvatar(is, avatar.getSize(), avatar.getContentType(), userId);
            userEntity.setAvatarKey(key);
            this.userRepository.save(userEntity);
            this.userSummaryCache.evict(userId);
            return ResponseEntity.ok(new UserAvatarResponse(
                    "/api/users/" + userId + "/avatar"
            ));
        }
    }

    @GetMapping("/{id}/avatar")
    @Operation(summary = "Получить аватар пользователя")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long id) throws Exception {
        UserEntity userEntity = this.userRepository.findById(id).orElseThrow();
        if (userEntity.getAvatarKey() == null) {
            return ResponseEntity.notFound().build();
        }
        InputStream is = this.videoObjectStorage.downloadAvatar(userEntity.getAvatarKey());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new InputStreamResource(is));
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Получить публичный профиль пользователя")
    public ResponseEntity<PublicUserProfileDto> getProfile(@PathVariable Long id, @AuthenticationPrincipal CurrentUser principal) {
        Long currentId = principal != null ? principal.getId() : null;
        return this.userRepository.findById(id)
                .map(user -> {
                    this.userToPublicProfileMapper.setCurrentUserId(currentId);
                    return this.userToPublicProfileMapper.toDto(user);
                })
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/videos")
    @Operation(summary = "Список видео пользователя", description = "Пагинация и безопасная сортировка")
    public ResponseEntity<Page<PublicUserVideoDto>> getUserVideos(@PathVariable Long id, Pageable pageable) {
        if (!this.userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        Pageable sanitized = sanitizePageable(pageable);
        return ResponseEntity.ok(this.videoProfileApi.findUserVideos(id, sanitized));
    }

    private Pageable sanitizePageable(Pageable pageable) {
        Pageable p = pageable != null ? pageable : PageRequest.of(0, 20);

        Set<String> allowed = new HashSet<>(Set.of(
                "uploadedAt",
                "likesCount",
                "viewsCount",
                "id"
        ));

        Sort filteredSort = Sort.by(
                p.getSort().stream()
                        .filter(o -> allowed.contains(o.getProperty()))
                        .toList()
        );

        if (filteredSort.isUnsorted()) {
            filteredSort = Sort.by(Sort.Order.desc("uploadedAt"));
        }

        return PageRequest.of(p.getPageNumber(), p.getPageSize(), filteredSort);
    }
}
