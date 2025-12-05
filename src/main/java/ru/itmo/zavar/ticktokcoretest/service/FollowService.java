package ru.itmo.zavar.ticktokcoretest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.ticktokcoretest.dto.PublicUserShortDto;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;
import ru.itmo.zavar.ticktokcoretest.entity.UserFollowEntity;
import ru.itmo.zavar.ticktokcoretest.mapper.UserToPublicShortUserMapper;
import ru.itmo.zavar.ticktokcoretest.repo.UserFollowRepository;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserFollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserToPublicShortUserMapper userToPublicShortUserMapper;

    @Transactional
    public void follow(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) return;
        if (followerId.equals(followeeId)) return;

        if (!this.userRepository.existsById(followeeId)) {
            throw new IllegalArgumentException("Target user not found");
        }

        if (this.followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            return;
        }

        UserFollowEntity e = new UserFollowEntity();
        e.setFollowerId(followerId);
        e.setFolloweeId(followeeId);
        this.followRepository.save(e);
    }

    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null)
            return;
        this.followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .ifPresent(this.followRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null)
            return false;
        return this.followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    @Transactional(readOnly = true)
    public long countFollowers(Long userId) {
        return this.followRepository.countByFolloweeId(userId);
    }

    @Transactional(readOnly = true)
    public long countFollowing(Long userId) {
        return this.followRepository.countByFollowerId(userId);
    }

    @Transactional(readOnly = true)
    public List<PublicUserShortDto> getFollowers(Long userId) {
        List<UserFollowEntity> links = this.followRepository.findByFolloweeId(userId);
        List<Long> ids = links.stream().map(UserFollowEntity::getFollowerId).toList();
        return this.userToPublicShortUserMapper.toDto(this.userRepository.findAllById(ids));
    }

    @Transactional(readOnly = true)
    public List<PublicUserShortDto> getFollowing(Long userId) {
        List<UserFollowEntity> links = this.followRepository.findByFollowerId(userId);
        List<Long> ids = links.stream().map(UserFollowEntity::getFolloweeId).toList();
        return this.userToPublicShortUserMapper.toDto(this.userRepository.findAllById(ids));
    }

    @Transactional(readOnly = true)
    public Set<Long> getFollowingWithin(Long followerId, Set<Long> candidateIds) {
        if (followerId == null || candidateIds == null || candidateIds.isEmpty()) {
            return Set.of();
        }

        return this.followRepository
                .findByFollowerIdAndFolloweeIdIn(followerId, candidateIds)
                .stream()
                .map(UserFollowEntity::getFolloweeId)
                .collect(Collectors.toSet());
    }
}
