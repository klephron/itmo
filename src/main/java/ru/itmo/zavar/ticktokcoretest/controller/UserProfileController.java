package ru.itmo.zavar.ticktokcoretest.controller;

import io.minio.errors.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.zavar.ticktokcoretest.dto.PublicUserProfileDto;
import ru.itmo.zavar.ticktokcoretest.dto.PublicUserVideoDto;
import ru.itmo.zavar.ticktokcoretest.dto.response.UserAvatarResponse;
import ru.itmo.zavar.ticktokcoretest.integrations.minio.MinioService;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepository;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;
import ru.itmo.zavar.ticktokcoretest.security.SecurityBeans;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserRepository userRepository;
    private final MinioService minioService;
    private final FileMetadataRepository fileMetadataRepository;

    public UserProfileController(UserRepository userRepository,
                                 MinioService minioService, FileMetadataRepository fileMetadataRepository) {
        this.userRepository = userRepository;
        this.minioService = minioService;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<UserAvatarResponse> uploadAvatar(
            @RequestParam("avatar") MultipartFile avatar,
            Authentication auth
    ) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        Long userId = extractUserId(auth); // как в лайках
        UserEntity u = userRepository.findById(userId).orElseThrow();

        try (InputStream is = avatar.getInputStream()) {
            String key = minioService.uploadAvatar(is, avatar.getSize(),
                    avatar.getContentType(), userId);
            u.setAvatarKey(key);
            userRepository.save(u);
            return ResponseEntity.ok(new UserAvatarResponse(
                    "/api/users/" + userId + "/avatar"
            ));
        }
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long id) throws Exception {
        UserEntity u = userRepository.findById(id).orElseThrow();
        if (u.getAvatarKey() == null) {
            return ResponseEntity.notFound().build();
        }
        InputStream is = minioService.downloadAvatar(u.getAvatarKey());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new InputStreamResource(is));
    }

    public Long extractUserId(Authentication authentication) {
        if (authentication == null) throw new RuntimeException("Unauthenticated");
        Object details = authentication.getDetails();
        if (details instanceof Map<?,?> map && map.get("id") != null) {
            return Long.valueOf(map.get("id").toString());
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityBeans.UserPrincipal up) {
            return up.getId();
        }
        throw new RuntimeException("Cannot extract user id");
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<PublicUserProfileDto> getProfile(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> new PublicUserProfileDto(
                        u.getId(),
                        u.getUsername(),
                        u.getDisplayName(),
                        u.getAvatarKey() == null ? null : "/api/users/" + u.getId() + "/avatar"
                ))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/videos")
    public ResponseEntity<List<PublicUserVideoDto>> getUserVideos(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<PublicUserVideoDto> res = fileMetadataRepository.findByUploaderId(id).stream()
                .map(m -> new PublicUserVideoDto(
                        m.getUuid(),
                        m.getName(),
                        "/streaming/video/thumbnail/" + m.getUuid(),
                        m.getUploadedAt().toInstant(ZoneOffset.UTC)
                ))
                .toList();
        return ResponseEntity.ok(res);
    }
}
