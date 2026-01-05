package ru.itmo.zavar.brainstop.profile.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.brainstop.profile.dto.PublicUserShortDto;
import ru.itmo.zavar.brainstop.profile.mapper.UserToPublicShortUserMapper;
import ru.itmo.zavar.brainstop.profile.model.UserFollowEntity;
import ru.itmo.zavar.brainstop.profile.repo.UserFollowRepository;
import ru.itmo.zavar.brainstop.profile.repo.UserRepository;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
// MSPLIT: boundary for future Follow/Profile domain logic
@RequiredArgsConstructor
public class FollowService {

    private static final Duration FOLLOWING_TTL = Duration.ofMinutes(10);
    private static final String FOLLOW_KEY_PREFIX = "profile:following:";

    private final UserFollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserToPublicShortUserMapper userToPublicShortUserMapper;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void follow(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return;
        }
        if (followerId.equals(followeeId)) {
            return;
        }

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
        addToCache(followerId, followeeId);
    }

    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return;
        }
        this.followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .ifPresent(entity -> {
                    this.followRepository.delete(entity);
                    removeFromCache(followerId, followeeId);
                });
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return false;
        }
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

        String key = followKey(followerId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Set<Long> cached = readCache(key);
            if (cached != null) {
                return cached.stream()
                        .filter(candidateIds::contains)
                        .collect(Collectors.toSet());
            }
        }

        Set<Long> followees = this.followRepository.findByFollowerId(followerId)
                .stream()
                .map(UserFollowEntity::getFolloweeId)
                .collect(Collectors.toSet());

        writeCache(key, followees);

        return followees.stream()
                .filter(candidateIds::contains)
                .collect(Collectors.toSet());
    }

    private void addToCache(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return;
        }
        String key = followKey(followerId);
        setOps().add(key, String.valueOf(followeeId));
        redisTemplate.expire(key, FOLLOWING_TTL);
    }

    private void removeFromCache(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return;
        }
        String key = followKey(followerId);
        setOps().remove(key, String.valueOf(followeeId));
    }

    private Set<Long> readCache(String key) {
        Set<String> members = setOps().members(key);
        if (members == null) {
            return null;
        }
        return members.stream()
                .map(this::parseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void writeCache(String key, Set<Long> followees) {
        if (followees == null || followees.isEmpty()) {
            return;
        }
        String[] payload = followees.stream()
                .map(String::valueOf)
                .toArray(String[]::new);
        setOps().add(key, payload);
        redisTemplate.expire(key, FOLLOWING_TTL);
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String followKey(Long followerId) {
        return FOLLOW_KEY_PREFIX + followerId;
    }

    private SetOperations<String, String> setOps() {
        return redisTemplate.opsForSet();
    }
}
