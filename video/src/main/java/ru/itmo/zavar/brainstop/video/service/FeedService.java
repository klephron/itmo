package ru.itmo.zavar.brainstop.video.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.like.LikeQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.FollowQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.brainstop.video.mapper.FeedMapperContext;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToFeedMapper;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FileMetadataRepository fileMetadataRepository;
    private final UserProfilePublicApi userProfilePublicApi;
    private final LikeQueryApi likeQueryApi;
    private final FollowQueryApi followQueryApi;
    private final FileMetadataToFeedMapper mapper;

    public Page<FeedVideoDto> getFeed(Long currentUserId, Pageable pageable) {

        Page<FileMetadataEntity> page = this.fileMetadataRepository.findAll(
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "uploadedAt")
                )
        );

        List<FileMetadataEntity> entities = page.getContent();

        Set<Long> uploaderIds = entities.stream()
                .map(FileMetadataEntity::getUploaderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> videoIds = entities.stream()
                .map(FileMetadataEntity::getId)
                .collect(Collectors.toSet());

        Map<Long, String> avatarByUserId = uploaderIds.stream()
                .map(id -> userProfilePublicApi.findUserSummary(id)
                        .map(summary -> Map.entry(id, summary.avatarUrl()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<Long> likedByMe;
        Set<Long> followingIds;

        if (currentUserId != null) {
            likedByMe = new HashSet<>(this.likeQueryApi.findLikedVideoIds(currentUserId, videoIds));
            followingIds = this.followQueryApi.getFollowingWithin(currentUserId, uploaderIds);
        } else {
            likedByMe = Collections.emptySet();
            followingIds = Collections.emptySet();
        }

        FeedMapperContext mapperContext = new FeedMapperContext(
                currentUserId,
                avatarByUserId,
                likedByMe,
                followingIds
        );

        List<FeedVideoDto> feedItems = this.mapper.toDto(entities, mapperContext);

        return new PageImpl<>(feedItems, page.getPageable(), page.getTotalElements());
    }
}
