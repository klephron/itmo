package ru.itmo.zavar.ticktokcoretest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.dto.FeedVideoDto;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;
import ru.itmo.zavar.ticktokcoretest.mapper.FileMetadataToFeedMapper;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepository;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;
import ru.itmo.zavar.ticktokcoretest.repo.VideoLikeRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;
    private final VideoLikeRepository likeRepository;
    private final FollowService followService;
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
        List<FeedVideoDto> base = this.mapper.toDto(entities);

        Set<Long> uploaderIds = entities.stream()
                .map(FileMetadataEntity::getUploaderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> videoIds = entities.stream()
                .map(FileMetadataEntity::getId)
                .collect(Collectors.toSet());

        Map<Long, String> avatarByUserId = this.userRepository
                .findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(
                        UserEntity::getId,
                        u -> u.getAvatarKey() == null ? null : "/api/users/" + u.getId() + "/avatar"));

        Set<Long> likedByMe;
        Set<Long> followingIds;

        if (currentUserId != null) {
            likedByMe = new HashSet<>(this.likeRepository.findLikedVideoIdsByUserIdAndVideoIdIn(currentUserId, videoIds));
            followingIds = this.followService.getFollowingWithin(currentUserId, uploaderIds);
        } else {
            likedByMe = Collections.emptySet();
            followingIds = Collections.emptySet();
        }

        List<FeedVideoDto> enriched = base.stream()
                .map(dto -> {
                    String avatarUrl = dto.uploaderId() == null
                            ? null
                            : avatarByUserId.get(dto.uploaderId());

                    boolean liked = currentUserId != null && likedByMe.contains(dto.uuid());
                    boolean subscribed = currentUserId != null
                            && dto.uploaderId() != null
                            && followingIds.contains(dto.uploaderId());

                    return new FeedVideoDto(
                            dto.uuid(),
                            dto.name(),
                            dto.uploaderId(),
                            dto.uploaderName(),
                            avatarUrl,
                            dto.contentSize(),
                            dto.contentType(),
                            dto.uploadedAt(),
                            dto.likesCount(),
                            dto.viewsCount(),
                            liked,
                            subscribed
                    );
                })
                .toList();

        return new PageImpl<>(enriched, page.getPageable(), page.getTotalElements());
    }
}
