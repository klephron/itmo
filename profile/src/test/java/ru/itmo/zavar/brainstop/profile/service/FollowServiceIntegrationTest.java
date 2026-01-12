package ru.itmo.zavar.brainstop.profile.service;

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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@FR({"FR-2.4"})
@Layer(TestLayer.BE_INT)
class FollowServiceIntegrationTest {

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
    @DisplayName("BE-INT-FOLLOW-SVC-01: Реальная подписка сохраняется в БД")
    void follow_savesToDatabase() {
        followService.follow(100L, 200L);

        boolean exists = followRepository.existsByFollowerIdAndFolloweeId(100L, 200L);
        assertThat(exists).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-FOLLOW-SVC-03: Отписка удаляет запись из БД")
    void unfollow_deletesFromDatabase() {
        followService.follow(100L, 200L);
        followService.unfollow(100L, 200L);

        boolean exists = followRepository.existsByFollowerIdAndFolloweeId(100L, 200L);
        assertThat(exists).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-FOLLOW-SVC-04: Проверка статуса подписки работает с БД")
    void isFollowing_checksDatabase() {
        assertThat(followService.isFollowing(100L, 200L)).isFalse();

        followService.follow(100L, 200L);

        assertThat(followService.isFollowing(100L, 200L)).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-FOLLOW-SVC-05: Подсчет подписчиков из БД")
    void countFollowers_fromDatabase() {
        followService.follow(100L, 200L);
        followService.follow(101L, 200L);
        followService.follow(102L, 200L);

        long count = followService.countFollowers(200L);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-FOLLOW-SVC-06: Подсчет подписок из БД")
    void countFollowing_fromDatabase() {
        followService.follow(100L, 200L);
        followService.follow(100L, 201L);
        followService.follow(100L, 202L);

        long count = followService.countFollowing(100L);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-FOLLOW-SVC-07: Получение списка подписчиков из БД")
    void getFollowers_fromDatabase() {
        followService.follow(100L, 200L);
        followService.follow(101L, 200L);

        List<PublicUserShortDto> followers = followService.getFollowers(200L);

        assertThat(followers).hasSize(2);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-FOLLOW-SVC-08: Получение списка подписок из БД")
    void getFollowing_fromDatabase() {
        followService.follow(100L, 200L);
        followService.follow(100L, 201L);

        List<PublicUserShortDto> following = followService.getFollowing(100L);

        assertThat(following).hasSize(2);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-INT-FOLLOW-SVC-09: Подписка на несуществующего пользователя")
    void follow_nonExistentUser() {
        when(authInternalClient.exists(999L)).thenReturn(false);

        assertThatThrownBy(() -> followService.follow(100L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-INT-FOLLOW-SVC-10: Подписка на себя игнорируется")
    void follow_self() {
        followService.follow(100L, 100L);

        boolean exists = followRepository.existsByFollowerIdAndFolloweeId(100L, 100L);
        assertThat(exists).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-FOLLOW-SVC-11: Транзакционность операций")
    void transactionalBehavior() {
        followService.follow(100L, 200L);

        assertThat(followService.isFollowing(100L, 200L)).isTrue();
        assertThat(followRepository.existsByFollowerIdAndFolloweeId(100L, 200L)).isTrue();
    }
}
