package ru.itmo.zavar.brainstop.profile.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.profile.client.AuthInternalClient;
import ru.itmo.zavar.brainstop.profile.dto.PublicUserShortDto;
import ru.itmo.zavar.brainstop.profile.service.FollowService;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.4"})
@Layer(TestLayer.BE_UNIT)
class FollowControllerTest {

    @Mock
    private FollowService followService;

    @Mock
    private AuthInternalClient authInternalClient;

    @InjectMocks
    private FollowController controller;

    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new CurrentUser(100L, "user", "User", null, Set.of(Role.USER));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-CTL-01: Подписка на пользователя")
    void follow_success() {
        when(authInternalClient.exists(200L)).thenReturn(true);
        doNothing().when(followService).follow(anyLong(), anyLong());

        ResponseEntity<Void> response = controller.follow(200L, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(followService).follow(100L, 200L);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-FOLLOW-CTL-02: Подписка без авторизации")
    void follow_unauthorized() {
        ResponseEntity<Void> response = controller.follow(200L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(followService, never()).follow(anyLong(), anyLong());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-FOLLOW-CTL-03: Подписка на несуществующего")
    void follow_userNotFound() {
        when(authInternalClient.exists(999L)).thenReturn(false);

        ResponseEntity<Void> response = controller.follow(999L, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-CTL-04: Отписка от пользователя")
    void unfollow_success() {
        when(authInternalClient.exists(200L)).thenReturn(true);
        doNothing().when(followService).unfollow(anyLong(), anyLong());

        ResponseEntity<Void> response = controller.unfollow(200L, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(followService).unfollow(100L, 200L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-CTL-05: Получение списка подписчиков")
    void getFollowers_success() {
        PublicUserShortDto user = new PublicUserShortDto(100L, "follower", "Follower", null);
        when(authInternalClient.exists(200L)).thenReturn(true);
        when(followService.getFollowers(200L)).thenReturn(List.of(user));

        ResponseEntity<List<PublicUserShortDto>> response = controller.followers(200L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-CTL-06: Получение списка подписок")
    void getFollowing_success() {
        PublicUserShortDto user = new PublicUserShortDto(200L, "followed", "Followed", null);
        when(authInternalClient.exists(100L)).thenReturn(true);
        when(followService.getFollowing(100L)).thenReturn(List.of(user));

        ResponseEntity<List<PublicUserShortDto>> response = controller.following(100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}
