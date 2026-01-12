package ru.itmo.zavar.brainstop.gateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.auth.controller.UserInternalController;
import ru.itmo.zavar.brainstop.common.api.profile.UserAccountDto;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.user.UserEntity;
import ru.itmo.zavar.brainstop.user.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.1", "FR-2.1"})
@Layer(TestLayer.BE_UNIT)
class UserInternalControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserInternalController userInternalController;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(Role.USER));
        testUser.setAvatarKey("avatar.jpg");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-USER-INT-01: Получение пользователя по ID")
    void getUser_success() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        ResponseEntity<UserAccountDto> response = userInternalController.getUser(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().username()).isEqualTo("testuser");
        assertThat(response.getBody().avatarKey()).isEqualTo("avatar.jpg");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-USER-INT-02: Пользователь не найден")
    void getUser_notFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        ResponseEntity<UserAccountDto> response = userInternalController.getUser(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-USER-INT-03: Проверка существования пользователя")
    void exists_true() {
        when(userRepository.existsById(anyLong())).thenReturn(true);

        Boolean result = userInternalController.exists(1L);

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-USER-INT-04: Получение списка всех пользователей")
    void listUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserAccountDto> result = userInternalController.listUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("testuser");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-USER-INT-05: Batch получение пользователей")
    void getUsers_batch() {
        when(userRepository.findAllById(any())).thenReturn(List.of(testUser));

        List<UserAccountDto> result = userInternalController.getUsers(List.of(1L, 2L));

        assertThat(result).hasSize(1);
        verify(userRepository).findAllById(List.of(1L, 2L));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-USER-INT-06: Обновление аватара")
    void updateAvatar_success() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        ResponseEntity<UserAccountDto> response = userInternalController.updateAvatar(1L, "new-avatar.jpg");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userRepository).save(any(UserEntity.class));
        assertThat(testUser.getAvatarKey()).isEqualTo("new-avatar.jpg");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-USER-INT-07: Изменение статуса enabled")
    void setEnabled_success() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        ResponseEntity<UserAccountDto> response = userInternalController.setEnabled(1L, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(testUser.isEnabled()).isFalse();
        verify(userRepository).save(testUser);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-USER-INT-10: Batch с пустым списком возвращает пустой результат")
    void getUsers_emptyList() {
        List<UserAccountDto> result = userInternalController.getUsers(List.of());

        assertThat(result).isEmpty();
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-USER-INT-11: Batch с null возвращает пустой результат")
    void getUsers_nullList() {
        List<UserAccountDto> result = userInternalController.getUsers(null);

        assertThat(result).isEmpty();
        verify(userRepository, never()).findAllById(any());
    }
}
