package ru.itmo.zavar.brainstop.profile.service;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.4"})
@Layer(TestLayer.BE_UNIT)
class FollowServiceTest {

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
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-01: Успешная подписка на пользователя")
    void follow_success() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(authInternalClient.exists(anyLong())).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFolloweeId(anyLong(), anyLong())).thenReturn(false);
        when(followRepository.save(any(UserFollowEntity.class))).thenReturn(new UserFollowEntity());

        followService.follow(100L, 200L);

        verify(followRepository).save(any(UserFollowEntity.class));
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-FOLLOW-02: Повторная подписка игнорируется")
    void follow_alreadyFollowing() {
        when(authInternalClient.exists(anyLong())).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFolloweeId(anyLong(), anyLong())).thenReturn(true);

        followService.follow(100L, 200L);

        verify(followRepository, never()).save(any(UserFollowEntity.class));
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-FOLLOW-03: Подписка на несуществующего пользователя")
    void follow_userNotFound() {
        when(authInternalClient.exists(anyLong())).thenReturn(false);

        assertThatThrownBy(() -> followService.follow(100L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target user not found");

        verify(followRepository, never()).save(any(UserFollowEntity.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-04: Отписка от пользователя")
    void unfollow_success() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        UserFollowEntity follow = new UserFollowEntity();
        follow.setFollowerId(100L);
        follow.setFolloweeId(200L);

        when(followRepository.findByFollowerIdAndFolloweeId(anyLong(), anyLong()))
                .thenReturn(Optional.of(follow));

        followService.unfollow(100L, 200L);

        verify(followRepository).delete(follow);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-FOLLOW-05: Подписка на себя игнорируется")
    void follow_self_ignored() {
        followService.follow(100L, 100L);

        verify(followRepository, never()).save(any(UserFollowEntity.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-06: Проверка подписки")
    void isFollowing_true() {
        when(followRepository.existsByFollowerIdAndFolloweeId(anyLong(), anyLong())).thenReturn(true);

        boolean result = followService.isFollowing(100L, 200L);

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-07: Подсчет подписчиков")
    void countFollowers_success() {
        when(followRepository.countByFolloweeId(anyLong())).thenReturn(42L);

        long count = followService.countFollowers(200L);

        assertThat(count).isEqualTo(42L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-08: Подсчет подписок")
    void countFollowing_success() {
        when(followRepository.countByFollowerId(anyLong())).thenReturn(15L);

        long count = followService.countFollowing(100L);

        assertThat(count).isEqualTo(15L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-09: Получение списка подписчиков")
    void getFollowers_success() {
        UserFollowEntity follow = new UserFollowEntity();
        follow.setFollowerId(100L);
        follow.setFolloweeId(200L);

        UserAccountDto account = new UserAccountDto(
                100L, "follower", "Follower Name", "follower@example.com",
                true, Set.of(Role.USER), LocalDateTime.now(), "avatar.jpg"
        );

        when(followRepository.findByFolloweeId(anyLong())).thenReturn(List.of(follow));
        when(authInternalClient.getUsers(any())).thenReturn(List.of(account));

        List<PublicUserShortDto> result = followService.getFollowers(200L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("follower");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-10: Получение списка подписок")
    void getFollowing_success() {
        UserFollowEntity follow = new UserFollowEntity();
        follow.setFollowerId(100L);
        follow.setFolloweeId(200L);

        UserAccountDto account = new UserAccountDto(
                200L, "followee", "Followee Name", "followee@example.com",
                true, Set.of(Role.USER), LocalDateTime.now(), "avatar.jpg"
        );

        when(followRepository.findByFollowerId(anyLong())).thenReturn(List.of(follow));
        when(authInternalClient.getUsers(any())).thenReturn(List.of(account));

        List<PublicUserShortDto> result = followService.getFollowing(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("followee");
    }
}
