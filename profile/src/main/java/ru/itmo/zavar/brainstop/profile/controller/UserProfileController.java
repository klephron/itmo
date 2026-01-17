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
import ru.itmo.zavar.brainstop.profile.service.FollowService;
import feign.FeignException;
import ru.itmo.zavar.brainstop.common.api.profile.UserAccountDto;
import ru.itmo.zavar.brainstop.common.api.video.VideoProfileApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.profile.client.AuthInternalClient;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи", description = "Профиль, аватар и видео пользователя")
// MSPLIT: HTTP API that will move to User Profile Service
public class UserProfileController {

    private final VideoObjectStorage videoObjectStorage;
    private final VideoProfileApi videoProfileApi;
    private final UserSummaryCache userSummaryCache;
    private final AuthInternalClient authInternalClient;
    private final FollowService followService;

    @PostMapping("/me/avatar")
    @Operation(summary = "Загрузить аватар текущего пользователя")
    public ResponseEntity<UserAvatarResponse> uploadAvatar(@RequestParam("avatar") MultipartFile avatar, @AuthenticationPrincipal CurrentUser principal) throws Exception {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = principal.getId();

        try (InputStream is = avatar.getInputStream()) {
            String key = this.videoObjectStorage.uploadAvatar(is, avatar.getSize(), avatar.getContentType(), userId);
            try {
                UserAccountDto updated = this.authInternalClient.updateAvatar(userId, key);
                if (updated == null) {
                    return ResponseEntity.status(503).build();
                }
            } catch (FeignException.NotFound e) {
                return ResponseEntity.notFound().build();
            }
            this.userSummaryCache.evict(userId);
            return ResponseEntity.ok(new UserAvatarResponse(
                    "/api/users/" + userId + "/avatar"
            ));
        }
    }

    @GetMapping("/{id}/avatar")
    @Operation(summary = "Получить аватар пользователя")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long id) throws Exception {
        UserAccountDto user;
        try {
            user = this.authInternalClient.getUser(id);
        } catch (FeignException.NotFound e) {
            return ResponseEntity.notFound().build();
        }
        if (user == null) {
            return ResponseEntity.status(503).build();
        }
        if (user.avatarKey() == null) {
            return ResponseEntity.notFound().build();
        }
        InputStream is = this.videoObjectStorage.downloadAvatar(user.avatarKey());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new InputStreamResource(is));
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Получить публичный профиль пользователя")
    public ResponseEntity<PublicUserProfileDto> getProfile(@PathVariable Long id, @AuthenticationPrincipal CurrentUser principal) {
        Long currentId = principal != null ? principal.getId() : null;
        UserAccountDto user;
        try {
            user = this.authInternalClient.getUser(id);
        } catch (FeignException.NotFound e) {
            return ResponseEntity.notFound().build();
        }
        if (user == null) {
            return ResponseEntity.status(503).build();
        }

        long followers = this.followService.countFollowers(id);
        long following = this.followService.countFollowing(id);
        boolean isFollowing = currentId != null && this.followService.isFollowing(currentId, id);

        PublicUserProfileDto dto = PublicUserProfileDto.builder()
                .id(user.id())
                .username(user.username())
                .displayName(user.displayName())
                .avatarUrl(avatarUrl(user))
                .followersCount(followers)
                .followingCount(following)
                .following(isFollowing)
                .build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/videos")
    @Operation(summary = "Список видео пользователя", description = "Пагинация и безопасная сортировка")
    public ResponseEntity<Page<PublicUserVideoDto>> getUserVideos(@PathVariable Long id,
                                                                  Pageable pageable,
                                                                  @AuthenticationPrincipal CurrentUser principal) {
        Boolean exists;
        try {
            exists = this.authInternalClient.exists(id);
        } catch (FeignException e) {
            return ResponseEntity.notFound().build();
        }
        if (exists == null) {
            return ResponseEntity.status(503).build();
        }
        if (!Boolean.TRUE.equals(exists)) {
            return ResponseEntity.notFound().build();
        }

        Long currentUserId = principal != null ? principal.getId() : null;
        Pageable sanitized = sanitizePageable(pageable);
        return ResponseEntity.ok(this.videoProfileApi.findUserVideos(id, currentUserId, sanitized));
    }

    private String avatarUrl(UserAccountDto user) {
        if (user == null || user.avatarKey() == null) {
            return null;
        }
        return "/api/users/" + user.id() + "/avatar";
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
