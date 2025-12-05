package ru.itmo.zavar.ticktokcoretest.controller;

import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.zavar.ticktokcoretest.dto.PublicUserProfileDto;
import ru.itmo.zavar.ticktokcoretest.dto.PublicUserVideoDto;
import ru.itmo.zavar.ticktokcoretest.dto.response.UserAvatarResponse;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;
import ru.itmo.zavar.ticktokcoretest.integrations.minio.MinioService;
import ru.itmo.zavar.ticktokcoretest.mapper.FileMetadataToPublicUserVideoMapper;
import ru.itmo.zavar.ticktokcoretest.mapper.UserToPublicProfileMapper;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepository;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;
import ru.itmo.zavar.ticktokcoretest.service.AuthService;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;
    private final MinioService minioService;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserToPublicProfileMapper userToPublicProfileMapper;
    private final FileMetadataToPublicUserVideoMapper fileMetadataToPublicUserVideoMapper;

    @PostMapping("/me/avatar")
    public ResponseEntity<UserAvatarResponse> uploadAvatar(@RequestParam("avatar") MultipartFile avatar, Authentication auth) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        Long userId = AuthService.extractUserId(auth);
        UserEntity userEntity = this.userRepository.findById(userId).orElseThrow();

        try (InputStream is = avatar.getInputStream()) {
            String key = this.minioService.uploadAvatar(is, avatar.getSize(), avatar.getContentType(), userId);
            userEntity.setAvatarKey(key);
            this.userRepository.save(userEntity);
            return ResponseEntity.ok(new UserAvatarResponse(
                    "/api/users/" + userId + "/avatar"
            ));
        }
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long id) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        UserEntity userEntity = this.userRepository.findById(id).orElseThrow();
        if (userEntity.getAvatarKey() == null) {
            return ResponseEntity.notFound().build();
        }
        InputStream is = this.minioService.downloadAvatar(userEntity.getAvatarKey());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new InputStreamResource(is));
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<PublicUserProfileDto> getProfile(@PathVariable Long id, Authentication authentication) {
        Long currentId = authentication != null ? AuthService.extractUserId(authentication) : null;
        return this.userRepository.findById(id)
                .map(user -> {
                    this.userToPublicProfileMapper.setCurrentUserId(currentId);
                    return this.userToPublicProfileMapper.toDto(user);
                })
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/videos")
    public ResponseEntity<Page<PublicUserVideoDto>> getUserVideos(@PathVariable Long id, Pageable pageable) {
        if (!this.userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        Page<PublicUserVideoDto> page = this.fileMetadataRepository
                .findByUploaderId(id, pageable)
                .map(this.fileMetadataToPublicUserVideoMapper::toDto);

        return ResponseEntity.ok(page);
    }
}
