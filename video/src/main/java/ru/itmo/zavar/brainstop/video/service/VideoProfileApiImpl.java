package ru.itmo.zavar.brainstop.video.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.like.LikeQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.api.video.VideoProfileApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToPublicUserVideoMapper;
import ru.itmo.zavar.brainstop.video.mapper.PublicUserVideoMapperContext;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoProfileApiImpl implements VideoProfileApi {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataToPublicUserVideoMapper fileMetadataToPublicUserVideoMapper;
    private final UserProfilePublicApi userProfilePublicApi;
    private final LikeQueryApi likeQueryApi;

    @Override
    public Page<PublicUserVideoDto> findUserVideos(Long uploaderId, Long currentUserId, Pageable pageable) {
        Map<Long, String> avatarByUserId = resolveAvatar(uploaderId);
        Page<FileMetadataEntity> page = this.fileMetadataRepository.findByUploaderId(uploaderId, pageable);
        List<FileMetadataEntity> content = page.getContent();

        Set<Long> likedVideoIds = Collections.emptySet();
        if (currentUserId != null && content != null && !content.isEmpty()) {
            Set<Long> ids = content.stream()
                    .map(FileMetadataEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!ids.isEmpty()) {
                likedVideoIds = new HashSet<>(this.likeQueryApi.findLikedVideoIds(currentUserId, ids));
            }
        }

        PublicUserVideoMapperContext context = new PublicUserVideoMapperContext(
                currentUserId,
                avatarByUserId,
                likedVideoIds
        );

        List<PublicUserVideoDto> dtos = this.fileMetadataToPublicUserVideoMapper.toDto(content, context);
        return new PageImpl<>(dtos, page.getPageable(), page.getTotalElements());
    }

    private Map<Long, String> resolveAvatar(Long uploaderId) {
        if (uploaderId == null) {
            return Map.of();
        }
        Optional<UserSummaryDto> summary = userProfilePublicApi.findUserSummary(uploaderId);
        if (summary.isPresent() && summary.get().avatarUrl() != null && !summary.get().avatarUrl().isBlank()) {
            return Map.of(uploaderId, summary.get().avatarUrl());
        }
        return Map.of();
    }
}
