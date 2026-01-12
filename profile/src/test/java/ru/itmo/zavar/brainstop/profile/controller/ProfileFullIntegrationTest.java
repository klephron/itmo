package ru.itmo.zavar.brainstop.profile.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.brainstop.common.api.profile.UserAccountDto;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.profile.client.AuthInternalClient;
import ru.itmo.zavar.brainstop.profile.dto.PublicUserShortDto;
import ru.itmo.zavar.brainstop.profile.repo.UserFollowRepository;
import ru.itmo.zavar.brainstop.profile.service.FollowService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@FR({"FR-2.1", "FR-2.4", "NFR-1.1"})
@Layer(TestLayer.BE_INT)
class ProfileFullIntegrationTest {

    @Autowired
    private FollowService followService;

    @Autowired
    private UserFollowRepository followRepository;

    @MockBean
    private AuthInternalClient authInternalClient;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private SetOperations<String, String> setOperations;

    @BeforeEach
    void setUp() {
        followRepository.deleteAll();

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(anyString(), any())).thenReturn(1L);
        when(setOperations.remove(anyString(), any())).thenReturn(1L);
        when(setOperations.members(anyString())).thenReturn(Set.of());
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        when(authInternalClient.exists(anyLong())).thenReturn(true);

        UserAccountDto user1 = new UserAccountDto(
                100L, "user1", "User One", "user1@example.com",
                true, Set.of(Role.USER), LocalDateTime.now(), null
        );
        UserAccountDto user2 = new UserAccountDto(
                200L, "user2", "User Two", "user2@example.com",
                true, Set.of(Role.USER), LocalDateTime.now(), "avatar.jpg"
        );

        when(authInternalClient.getUsers(any())).thenReturn(List.of(user1, user2));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-FULL-01: Полная интеграция follow/unfollow с БД")
    void fullFollowUnfollowScenario() {
        assertThat(followService.isFollowing(100L, 200L)).isFalse();

        followService.follow(100L, 200L);

        assertThat(followService.isFollowing(100L, 200L)).isTrue();
        assertThat(followRepository.existsByFollowerIdAndFolloweeId(100L, 200L)).isTrue();

        followService.unfollow(100L, 200L);

        assertThat(followService.isFollowing(100L, 200L)).isFalse();
        assertThat(followRepository.existsByFollowerIdAndFolloweeId(100L, 200L)).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-FULL-02: Счетчики работают с реальной БД")
    void countersWorkWithRealDB() {
        followService.follow(100L, 200L);
        followService.follow(101L, 200L);
        followService.follow(100L, 201L);

        assertThat(followService.countFollowers(200L)).isEqualTo(2L);
        assertThat(followService.countFollowing(100L)).isEqualTo(2L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-FULL-03: Списки подписок из БД")
    void getFollowersAndFollowingFromDB() {
        followService.follow(100L, 200L);
        followService.follow(101L, 200L);

        List<PublicUserShortDto> followers = followService.getFollowers(200L);
        List<PublicUserShortDto> following = followService.getFollowing(100L);

        assertThat(followers).isNotEmpty();
        assertThat(following).isNotEmpty();
    }
}
