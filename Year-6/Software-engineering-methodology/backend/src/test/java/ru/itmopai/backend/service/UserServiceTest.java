package ru.itmopai.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.entity.UserPermissionEntity;
import ru.itmopai.backend.exception.UserExistsException;
import ru.itmopai.backend.mapper.UserMapper;
import ru.itmopai.backend.repository.UserPermissionRepository;
import ru.itmopai.backend.repository.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserPermissionRepository userPermissionRepo;

  @Mock
  private BCryptPasswordEncoderService encoder;

  @Mock
  private UserMapper userMapper;

  @InjectMocks
  private UserService userService;

  private UserEntity testUser;

  @BeforeEach
  void setUp() {
    testUser = new UserEntity();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setName("Test");
    testUser.setSurname("User");
    testUser.setPassword("$2a$10$hashedpassword");
    testUser.setLocationLatitude(0.0);
    testUser.setLocationLongitude(0.0);
    testUser.setLocationAltitude(0.0);
  }

  @Nested
  @DisplayName("register() method tests")
  class RegisterTests {

    @Test
    @DisplayName("Should successfully register a new user")
    void shouldRegisterNewUserSuccessfully() {
      // Given
      String username = "newuser";
      String rawPassword = "password123";
      String name = "New";
      String surname = "User";
      String patronymic = "Patronymic";
      String primaryPosition = "Developer";

      when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
      when(encoder.encode(rawPassword)).thenReturn("$2a$10$encoded");
      when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
        UserEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      UserEntity result = userService.register(username, rawPassword, name, surname, patronymic, primaryPosition);

      // Then
      assertNotNull(result);
      assertEquals(username, result.getUsername());
      assertEquals(name, result.getName());
      assertEquals(surname, result.getSurname());
      assertEquals(patronymic, result.getPatronymic());
      assertEquals(primaryPosition, result.getPrimaryPosition());
      assertEquals("$2a$10$encoded", result.getPassword());
      assertEquals(0.0, result.getLocationLatitude());
      assertEquals(0.0, result.getLocationLongitude());
      assertEquals(0.0, result.getLocationAltitude());

      verify(userRepository).findByUsername(username);
      verify(encoder).encode(rawPassword);
      verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should throw UserExistsException when user already exists")
    void shouldThrowExceptionWhenUserExists() {
      // Given
      String existingUsername = "existinguser";
      when(userRepository.findByUsername(existingUsername)).thenReturn(Optional.of(testUser));

      // When & Then
      UserExistsException exception = assertThrows(UserExistsException.class, () -> {
        userService.register(existingUsername, "password", "Name", "Surname", null, null);
      });

      assertEquals("User already exists", exception.getMessage());
      verify(userRepository).findByUsername(existingUsername);
      verify(encoder, never()).encode(anyString());
      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should hash password during registration")
    void shouldHashPasswordDuringRegistration() {
      // Given
      String rawPassword = "plainTextPassword";
      String hashedPassword = "$2a$10$hashedversion";

      when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
      when(encoder.encode(rawPassword)).thenReturn(hashedPassword);

      ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
      when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      userService.register("user", rawPassword, "Name", "Surname", null, null);

      // Then
      UserEntity savedUser = userCaptor.getValue();
      assertEquals(hashedPassword, savedUser.getPassword());
      assertNotEquals(rawPassword, savedUser.getPassword());
      verify(encoder).encode(rawPassword);
    }

    @Test
    @DisplayName("Should initialize location to 0,0,0 by default")
    void shouldInitializeLocationToZero() {
      // Given
      when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
      when(encoder.encode(anyString())).thenReturn("hashed");

      ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
      when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      userService.register("user", "pass", "Name", "Surname", null, null);

      // Then
      UserEntity savedUser = userCaptor.getValue();
      assertEquals(0.0, savedUser.getLocationLatitude());
      assertEquals(0.0, savedUser.getLocationLongitude());
      assertEquals(0.0, savedUser.getLocationAltitude());
    }
  }

  @Nested
  @DisplayName("getAllUsers() method tests")
  class GetAllUsersTests {

    @Test
    @DisplayName("Should return all users")
    void shouldReturnAllUsers() {
      // Given
      UserEntity user1 = new UserEntity();
      user1.setId(1L);
      user1.setUsername("user1");

      UserEntity user2 = new UserEntity();
      user2.setId(2L);
      user2.setUsername("user2");

      List<UserEntity> users = List.of(user1, user2);
      when(userRepository.findAll()).thenReturn(users);

      // When
      List<UserEntity> result = userService.getAllUsers();

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
      assertEquals("user1", result.get(0).getUsername());
      assertEquals("user2", result.get(1).getUsername());
      verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void shouldReturnEmptyListWhenNoUsers() {
      // Given
      when(userRepository.findAll()).thenReturn(List.of());

      // When
      List<UserEntity> result = userService.getAllUsers();

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
      verify(userRepository).findAll();
    }
  }

  @Nested
  @DisplayName("getUser() method tests")
  class GetUserTests {

    @Test
    @DisplayName("Should return user when exists")
    void shouldReturnUserWhenExists() {
      // Given
      Long userId = 1L;
      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

      // When
      Optional<UserEntity> result = userService.getUser(userId);

      // Then
      assertTrue(result.isPresent());
      assertEquals(testUser.getId(), result.get().getId());
      assertEquals(testUser.getUsername(), result.get().getUsername());
      verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should return empty Optional when user not found")
    void shouldReturnEmptyWhenUserNotFound() {
      // Given
      Long userId = 999L;
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When
      Optional<UserEntity> result = userService.getUser(userId);

      // Then
      assertFalse(result.isPresent());
      verify(userRepository).findById(userId);
    }
  }

  @Nested
  @DisplayName("removeUser() method tests")
  class RemoveUserTests {

    @Test
    @DisplayName("Should delete user by id")
    void shouldDeleteUserById() {
      // Given
      Long userId = 1L;
      doNothing().when(userRepository).deleteById(userId);

      // When
      assertDoesNotThrow(() -> userService.removeUser(userId));

      // Then
      verify(userRepository).deleteById(userId);
    }
  }

  @Nested
  @DisplayName("updateLocation() method tests")
  class UpdateLocationTests {

    @Test
    @DisplayName("Should successfully update user location")
    void shouldUpdateLocationSuccessfully() {
      // Given
      Long userId = 1L;
      Double newLat = 55.7558;
      Double newLon = 37.6173;
      Double newAlt = 150.0;

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(userRepository.update(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      UserEntity result = userService.updateLocation(userId, newLat, newLon, newAlt);

      // Then
      assertNotNull(result);
      assertEquals(newLat, result.getLocationLatitude());
      assertEquals(newLon, result.getLocationLongitude());
      assertEquals(newAlt, result.getLocationAltitude());
      verify(userRepository).findById(userId);
      verify(userRepository).update(testUser);
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
      // Given
      Long userId = 999L;
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        userService.updateLocation(userId, 0.0, 0.0, 0.0);
      });

      verify(userRepository).findById(userId);
      verify(userRepository, never()).update(any());
    }
  }

  @Nested
  @DisplayName("getUserPermissions() method tests")
  class GetUserPermissionsTests {

    @Test
    @DisplayName("Should return user permissions")
    void shouldReturnUserPermissions() {
      // Given
      Long userId = 1L;
      UserPermissionEntity.UserPermissionId id1 =
          new UserPermissionEntity.UserPermissionId(testUser, "USERS:*:READ");
      UserPermissionEntity.UserPermissionId id2 =
          new UserPermissionEntity.UserPermissionId(testUser, "CASES:*:READ");

      UserPermissionEntity perm1 = new UserPermissionEntity(id1);
      UserPermissionEntity perm2 = new UserPermissionEntity(id2);

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(userPermissionRepo.findById_UserId(userId)).thenReturn(List.of(perm1, perm2));

      // When
      List<String> result = userService.getUserPermissions(userId);

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
      assertTrue(result.contains("USERS:*:READ"));
      assertTrue(result.contains("CASES:*:READ"));
      verify(userRepository).findById(userId);
      verify(userPermissionRepo).findById_UserId(userId);
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
      // Given
      Long userId = 999L;
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        userService.getUserPermissions(userId);
      });

      verify(userRepository).findById(userId);
      verify(userPermissionRepo, never()).findById_UserId(any());
    }

    @Test
    @DisplayName("Should return empty list when user has no permissions")
    void shouldReturnEmptyListWhenNoPermissions() {
      // Given
      Long userId = 1L;
      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(userPermissionRepo.findById_UserId(userId)).thenReturn(List.of());

      // When
      List<String> result = userService.getUserPermissions(userId);

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
      verify(userRepository).findById(userId);
      verify(userPermissionRepo).findById_UserId(userId);
    }
  }

  @Nested
  @DisplayName("setUserPermissions() method tests")
  class SetUserPermissionsTests {

    @Test
    @DisplayName("Should set user permissions")
    void shouldSetUserPermissions() {
      // Given
      Long userId = 1L;
      List<String> newPermissions = List.of("USERS:*:READ", "CASES:*:WRITE");

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(userPermissionRepo.findById_UserId(userId)).thenReturn(List.of());
      doNothing().when(userPermissionRepo).deleteAll(any());
      when(userPermissionRepo.save(any(UserPermissionEntity.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // When
      assertDoesNotThrow(() -> userService.setUserPermissions(userId, newPermissions));

      // Then
      verify(userRepository).findById(userId);
      verify(userPermissionRepo).findById_UserId(userId);
      verify(userPermissionRepo).deleteAll(any());
      verify(userPermissionRepo, times(2)).save(any(UserPermissionEntity.class));
    }

    @Test
    @DisplayName("Should delete old permissions before adding new ones")
    void shouldDeleteOldPermissionsBeforeAddingNew() {
      // Given
      Long userId = 1L;
      List<String> newPermissions = List.of("NEW:*:PERMISSION");

      UserPermissionEntity.UserPermissionId oldId =
          new UserPermissionEntity.UserPermissionId(testUser, "OLD:*:PERMISSION");
      UserPermissionEntity oldPerm = new UserPermissionEntity(oldId);
      List<UserPermissionEntity> oldPermissions = List.of(oldPerm);

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(userPermissionRepo.findById_UserId(userId)).thenReturn(oldPermissions);
      doNothing().when(userPermissionRepo).deleteAll(oldPermissions);
      when(userPermissionRepo.save(any(UserPermissionEntity.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // When
      userService.setUserPermissions(userId, newPermissions);

      // Then
      verify(userPermissionRepo).deleteAll(oldPermissions);
      verify(userPermissionRepo).save(any(UserPermissionEntity.class));
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
      // Given
      Long userId = 999L;
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        userService.setUserPermissions(userId, List.of("SOME:*:PERMISSION"));
      });

      verify(userRepository).findById(userId);
      verify(userPermissionRepo, never()).deleteAll(any());
      verify(userPermissionRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should handle empty permissions list")
    void shouldHandleEmptyPermissionsList() {
      // Given
      Long userId = 1L;
      List<String> emptyPermissions = List.of();

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(userPermissionRepo.findById_UserId(userId)).thenReturn(List.of());
      doNothing().when(userPermissionRepo).deleteAll(any());

      // When
      userService.setUserPermissions(userId, emptyPermissions);

      // Then
      verify(userPermissionRepo).deleteAll(any());
      verify(userPermissionRepo, never()).save(any());
    }
  }
}
