package ru.itmo.zavar.brainstop.profile.service;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import ru.itmo.zavar.brainstop.common.api.profile.UserAccountDto;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.profile.client.AuthInternalClient;
import ru.itmo.zavar.brainstop.profile.dto.PublicUserShortDto;
import ru.itmo.zavar.brainstop.profile.model.UserFollowEntity;
import ru.itmo.zavar.brainstop.profile.repo.UserFollowRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.4"})
@Layer(TestLayer.BE_UNIT)
class FollowServiceAdvancedTest {

    @Mock
    private UserFollowRepository followRepository;

    @Mock
    private AuthInternalClient authInternalClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private FollowService followService;

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-FOLLOW-ADV-01: Follow с null followerId")
    void follow_nullFollowerId() {
        followService.follow(null, 200L);

        verify(followRepository, never()).save(any());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-FOLLOW-ADV-02: Follow с null followeeId")
    void follow_nullFolloweeId() {
        followService.follow(100L, null);

        verify(followRepository, never()).save(any());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-FOLLOW-ADV-03: isFollowing с null параметрами")
    void isFollowing_nullParams() {
        assertThat(followService.isFollowing(null, 200L)).isFalse();
        assertThat(followService.isFollowing(100L, null)).isFalse();

        verify(followRepository, never()).existsByFollowerIdAndFolloweeId(anyLong(), anyLong());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-ADV-04: getFollowers с пустым списком")
    void getFollowers_emptyList() {
        when(followRepository.findByFolloweeId(200L)).thenReturn(List.of());

        List<PublicUserShortDto> followers = followService.getFollowers(200L);

        assertThat(followers).isEmpty();
        verify(authInternalClient, never()).getUsers(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-ADV-05: getFollowing с пустым списком")
    void getFollowing_emptyList() {
        when(followRepository.findByFollowerId(100L)).thenReturn(List.of());

        List<PublicUserShortDto> following = followService.getFollowing(100L);

        assertThat(following).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-FOLLOW-ADV-06: getFollowers когда auth сервис недоступен")
    void getFollowers_authServiceDown() {
        UserFollowEntity follow = new UserFollowEntity();
        follow.setFollowerId(100L);
        follow.setFolloweeId(200L);

        when(followRepository.findByFolloweeId(200L)).thenReturn(List.of(follow));
        when(authInternalClient.getUsers(any())).thenThrow(FeignException.ServiceUnavailable.class);

        List<PublicUserShortDto> followers = followService.getFollowers(200L);

        assertThat(followers).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-ADV-07: getFollowingWithin пустой набор")
    void getFollowingWithin_emptyCandidates() {
        Set<Long> result = followService.getFollowingWithin(100L, Set.of());

        assertThat(result).isEmpty();
        verify(followRepository, never()).findByFollowerIdAndFolloweeIdIn(anyLong(), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-ADV-08: getFollowingWithin с null userId")
    void getFollowingWithin_nullUserId() {
        Set<Long> result = followService.getFollowingWithin(null, Set.of(200L));

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-ADV-09: getFollowingWithin из БД (кеш пуст)")
    void getFollowingWithin_fromDatabase() {
        UserFollowEntity follow1 = new UserFollowEntity();
        follow1.setFollowerId(100L);
        follow1.setFolloweeId(200L);

        UserFollowEntity follow2 = new UserFollowEntity();
        follow2.setFollowerId(100L);
        follow2.setFolloweeId(201L);

        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(followRepository.findByFollowerId(100L)).thenReturn(List.of(follow1, follow2));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        Set<Long> result = followService.getFollowingWithin(100L, Set.of(200L, 201L, 202L));

        assertThat(result).containsExactlyInAnyOrder(200L, 201L);
        verify(setOperations).add(anyString(), anyString(), anyString());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-ADV-10: getFollowingWithin из кеша Redis")
    void getFollowingWithin_fromCache() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        when(setOperations.members(anyString())).thenReturn(Set.of("200", "201"));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        Set<Long> result = followService.getFollowingWithin(100L, Set.of(200L, 201L, 202L));

        assertThat(result).containsExactlyInAnyOrder(200L, 201L);
        verify(followRepository, never()).findByFollowerId(anyLong());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-ADV-12: Unfollow несуществующей подписки")
    void unfollow_notExists() {
        when(followRepository.findByFollowerIdAndFolloweeId(100L, 200L))
                .thenReturn(Optional.empty());

        followService.unfollow(100L, 200L);

        verify(followRepository, never()).delete(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-ADV-13: Маппинг avatarUrl из UserAccountDto")
    void getFollowers_mapsAvatarUrl() {
        UserFollowEntity follow = new UserFollowEntity();
        follow.setFollowerId(100L);
        follow.setFolloweeId(200L);

        UserAccountDto account = new UserAccountDto(
                100L, "user", "User", "user@example.com",
                true, Set.of(Role.USER), LocalDateTime.now(), "avatar123.jpg"
        );

        when(followRepository.findByFolloweeId(200L)).thenReturn(List.of(follow));
        when(authInternalClient.getUsers(any())).thenReturn(List.of(account));

        List<PublicUserShortDto> followers = followService.getFollowers(200L);

        assertThat(followers).hasSize(1);
        assertThat(followers.get(0).avatarUrl()).isEqualTo("/api/users/100/avatar");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-ADV-14: Маппинг без avatarKey")
    void getFollowers_noAvatar() {
        UserFollowEntity follow = new UserFollowEntity();
        follow.setFollowerId(100L);
        follow.setFolloweeId(200L);

        UserAccountDto account = new UserAccountDto(
                100L, "user", "User", "user@example.com",
                true, Set.of(Role.USER), LocalDateTime.now(), null
        );

        when(followRepository.findByFolloweeId(200L)).thenReturn(List.of(follow));
        when(authInternalClient.getUsers(any())).thenReturn(List.of(account));

        List<PublicUserShortDto> followers = followService.getFollowers(200L);

        assertThat(followers.get(0).avatarUrl()).isNull();
    }
}