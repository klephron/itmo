package ru.itmo.zavar.brainstop.video.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.api.video.VideoProfileApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToPublicUserVideoMapper;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VideoProfileApiImpl implements VideoProfileApi {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataToPublicUserVideoMapper fileMetadataToPublicUserVideoMapper;
    private final UserProfilePublicApi userProfilePublicApi;

    @Override
    public Page<PublicUserVideoDto> findUserVideos(Long uploaderId, Pageable pageable) {
        Map<Long, String> avatarByUserId = resolveAvatar(uploaderId);
        return this.fileMetadataRepository
                .findByUploaderId(uploaderId, pageable)
                .map(file -> this.fileMetadataToPublicUserVideoMapper.toDto(file, avatarByUserId));
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
