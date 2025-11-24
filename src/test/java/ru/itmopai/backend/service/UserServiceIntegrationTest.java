package ru.itmopai.backend.service;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.exception.UserExistsException;
import ru.itmopai.backend.repository.UserPermissionRepository;
import ru.itmopai.backend.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest implements TestPropertyProvider {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  @Inject
  UserService userService;

  @Inject
  UserRepository userRepository;

  @Inject
  UserPermissionRepository userPermissionRepository;

  @Inject
  BCryptPasswordEncoderService passwordEncoder;

  @Override
  public Map<String, String> getProperties() {
    postgres.start();
    return Map.of(
        "datasources.default.url", postgres.getJdbcUrl(),
        "datasources.default.username", postgres.getUsername(),
        "datasources.default.password", postgres.getPassword(),
        "datasources.default.driver-class-name", "org.postgresql.Driver"
    );
  }

  @BeforeEach
  void setUp() {
    // Clean up before each test
    userPermissionRepository.deleteAll();
    userRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    // Clean up after each test
    userPermissionRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Nested
  @DisplayName("register() integration tests")
  class RegisterIntegrationTests {

    @Test
    @DisplayName("Should persist user to database with hashed password")
    void shouldPersistUserWithHashedPassword() {
      // Given
      String username = "newuser";
      String rawPassword = "password123";
      String name = "New";
      String surname = "User";

      // When
      UserEntity savedUser = userService.register(username, rawPassword, name, surname, null, null);

      // Then
      assertNotNull(savedUser.getId());

      // Verify it's in the database
      UserEntity foundUser = userRepository.findById(savedUser.getId()).orElseThrow();
      assertEquals(username, foundUser.getUsername());
      assertEquals(name, foundUser.getName());
      assertEquals(surname, foundUser.getSurname());

      // Verify password is hashed
      assertNotEquals(rawPassword, foundUser.getPassword());
      assertTrue(foundUser.getPassword().startsWith("$2a$") || foundUser.getPassword().startsWith("$2b$"));
      assertTrue(passwordEncoder.matches(rawPassword, foundUser.getPassword()));
    }

    @Test
    @DisplayName("Should throw UserExistsException when registering duplicate username")
    void shouldThrowExceptionForDuplicateUsername() {
      // Given
      String username = "duplicate";
      userService.register(username, "pass1", "Name", "Surname", null, null);

      // When & Then
      assertThrows(UserExistsException.class, () -> {
        userService.register(username, "pass2", "Other", "Name", null, null);
      });

      // Verify only one user exists
      assertEquals(1, userRepository.count());
    }

    @Test
    @DisplayName("Should initialize location to 0,0,0")
    void shouldInitializeLocationToZero() {
      // When
      UserEntity savedUser = userService.register("user", "pass", "Name", "Surname", null, null);

      // Then
      UserEntity foundUser = userRepository.findById(savedUser.getId()).orElseThrow();
      assertEquals(0.0, foundUser.getLocationLatitude());
      assertEquals(0.0, foundUser.getLocationLongitude());
      assertEquals(0.0, foundUser.getLocationAltitude());
    }
  }

  @Nested
  @DisplayName("updateLocation() integration tests")
  class UpdateLocationIntegrationTests {

    @Test
    @DisplayName("Should update location in database")
    void shouldUpdateLocationInDB() {
      // Given
      UserEntity user = userService.register("user", "pass", "Name", "Surname", null, null);
      Double newLat = 55.7558;
      Double newLon = 37.6173;
      Double newAlt = 150.0;

      // When
      UserEntity updated = userService.updateLocation(user.getId(), newLat, newLon, newAlt);

      // Then
      UserEntity found = userRepository.findById(updated.getId()).orElseThrow();
      assertEquals(newLat, found.getLocationLatitude());
      assertEquals(newLon, found.getLocationLongitude());
      assertEquals(newAlt, found.getLocationAltitude());
    }

    @Test
    @DisplayName("Should throw exception for non-existent user")
    void shouldThrowExceptionForNonExistentUser() {
      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        userService.updateLocation(999L, 0.0, 0.0, 0.0);
      });
    }
  }

  @Nested
  @DisplayName("getUserPermissions() and setUserPermissions() integration tests")
  class PermissionsIntegrationTests {

    @Test
    @DisplayName("Should persist and retrieve user permissions")
    void shouldPersistAndRetrievePermissions() {
      // Given
      UserEntity user = userService.register("user", "pass", "Name", "Surname", null, null);
      List<String> permissions = List.of("USERS:*:READ", "CASES:*:WRITE", "CLUES:*:DELETE");

      // When
      userService.setUserPermissions(user.getId(), permissions);
      List<String> retrieved = userService.getUserPermissions(user.getId());

      // Then
      assertEquals(3, retrieved.size());
      assertTrue(retrieved.containsAll(permissions));
    }

    @Test
    @DisplayName("Should replace old permissions with new ones")
    void shouldReplaceOldPermissions() {
      // Given
      UserEntity user = userService.register("user", "pass", "Name", "Surname", null, null);
      List<String> oldPermissions = List.of("OLD:*:PERMISSION");
      List<String> newPermissions = List.of("NEW:*:PERMISSION", "ANOTHER:*:PERMISSION");

      userService.setUserPermissions(user.getId(), oldPermissions);

      // When
      userService.setUserPermissions(user.getId(), newPermissions);
      List<String> retrieved = userService.getUserPermissions(user.getId());

      // Then
      assertEquals(2, retrieved.size());
      assertTrue(retrieved.containsAll(newPermissions));
      assertFalse(retrieved.contains("OLD:*:PERMISSION"));
    }

    @Test
    @DisplayName("Should handle empty permissions list")
    void shouldHandleEmptyPermissionsList() {
      // Given
      UserEntity user = userService.register("user", "pass", "Name", "Surname", null, null);
      userService.setUserPermissions(user.getId(), List.of("SOME:*:PERMISSION"));

      // When
      userService.setUserPermissions(user.getId(), List.of());
      List<String> retrieved = userService.getUserPermissions(user.getId());

      // Then
      assertTrue(retrieved.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when setting permissions for non-existent user")
    void shouldThrowExceptionForNonExistentUser() {
      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        userService.setUserPermissions(999L, List.of("SOME:*:PERMISSION"));
      });
    }
  }

  @Nested
  @DisplayName("getAllUsers() and getUser() integration tests")
  class GetUsersIntegrationTests {

    @Test
    @DisplayName("Should retrieve all users from database")
    void shouldRetrieveAllUsers() {
      // Given
      userService.register("user1", "pass", "Name1", "Surname1", null, null);
      userService.register("user2", "pass", "Name2", "Surname2", null, null);
      userService.register("user3", "pass", "Name3", "Surname3", null, null);

      // When
      List<UserEntity> users = userService.getAllUsers();

      // Then
      assertEquals(3, users.size());
      assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("user1")));
      assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("user2")));
      assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("user3")));
    }

    @Test
    @DisplayName("Should retrieve specific user by ID")
    void shouldRetrieveUserById() {
      // Given
      UserEntity savedUser = userService.register("testuser", "pass", "Test", "User", null, null);

      // When
      Optional<UserEntity> found = userService.getUser(savedUser.getId());

      // Then
      assertTrue(found.isPresent());
      assertEquals("testuser", found.get().getUsername());
      assertEquals("Test", found.get().getName());
    }

    @Test
    @DisplayName("Should return empty Optional for non-existent user")
    void shouldReturnEmptyForNonExistentUser() {
      // When
      Optional<UserEntity> found = userService.getUser(999L);

      // Then
      assertFalse(found.isPresent());
    }
  }

  @Nested
  @DisplayName("removeUser() integration tests")
  class RemoveUserIntegrationTests {

    @Test
    @DisplayName("Should delete user from database")
    void shouldDeleteUserFromDB() {
      // Given
      UserEntity user = userService.register("user", "pass", "Name", "Surname", null, null);
      Long userId = user.getId();

      assertTrue(userRepository.findById(userId).isPresent());

      // When
      userService.removeUser(userId);

      // Then
      assertFalse(userRepository.findById(userId).isPresent());
    }

    @Test
    @DisplayName("Should require manual deletion of permissions before deleting user")
    void shouldRequireManualDeletionOfPermissions() {
      // Given
      UserEntity user = userService.register("user", "pass", "Name", "Surname", null, null);
      userService.setUserPermissions(user.getId(), List.of("SOME:*:PERMISSION"));

      assertEquals(1, userPermissionRepository.findById_UserId(user.getId()).size());

      // When - Must delete permissions first due to FK constraint
      userPermissionRepository.deleteAll(userPermissionRepository.findById_UserId(user.getId()));
      userService.removeUser(user.getId());

      // Then
      assertFalse(userRepository.findById(user.getId()).isPresent());
      assertTrue(userPermissionRepository.findById_UserId(user.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should not throw exception when deleting non-existent user")
    void shouldNotThrowWhenDeletingNonExistentUser() {
      // When & Then
      assertDoesNotThrow(() -> userService.removeUser(999L));
    }
  }

  @Nested
  @DisplayName("Full lifecycle tests")
  class FullLifecycleTests {

    @Test
    @DisplayName("Should support complete user lifecycle")
    void shouldSupportCompleteLifecycle() {
      // Create
      UserEntity created = userService.register("lifecycleuser", "password123", "Life", "Cycle", "Middle", "Engineer");
      assertNotNull(created.getId());
      assertEquals("lifecycleuser", created.getUsername());

      // Update location
      UserEntity updated = userService.updateLocation(created.getId(), 55.7558, 37.6173, 150.0);
      assertEquals(55.7558, updated.getLocationLatitude());

      // Add permissions
      List<String> permissions = List.of("USERS:*:READ", "CASES:*:WRITE");
      userService.setUserPermissions(created.getId(), permissions);
      List<String> retrievedPerms = userService.getUserPermissions(created.getId());
      assertEquals(2, retrievedPerms.size());

      // Get user
      Optional<UserEntity> found = userService.getUser(created.getId());
      assertTrue(found.isPresent());
      assertEquals("lifecycleuser", found.get().getUsername());

      // Delete - Must delete permissions first due to FK constraint
      userPermissionRepository.deleteAll(userPermissionRepository.findById_UserId(created.getId()));
      userService.removeUser(created.getId());
      assertFalse(userService.getUser(created.getId()).isPresent());
    }

    @Test
    @DisplayName("Should verify password authentication after registration")
    void shouldVerifyPasswordAuthentication() {
      // Given
      String rawPassword = "mySecurePassword123";
      UserEntity user = userService.register("authtest", rawPassword, "Auth", "Test", null, null);

      // When
      UserEntity found = userRepository.findById(user.getId()).orElseThrow();

      // Then
      assertTrue(passwordEncoder.matches(rawPassword, found.getPassword()));
      assertFalse(passwordEncoder.matches("wrongpassword", found.getPassword()));
    }
  }
}
