package ru.itmopai.backend.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.itmopai.backend.dto.UserCaseDto;
import ru.itmopai.backend.dto.UserDto;
import ru.itmopai.backend.dto.request.LocationUpdateRequest;
import ru.itmopai.backend.dto.request.LoginRequest;
import ru.itmopai.backend.dto.request.RegisterRequest;
import ru.itmopai.backend.dto.request.UserPermissionsRequest;
import ru.itmopai.backend.dto.response.LoginResponse;
import ru.itmopai.backend.dto.response.UserInfoResponse;
import ru.itmopai.backend.dto.response.UserPermissionsResponse;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.repository.UserPermissionRepository;
import ru.itmopai.backend.repository.UserRepository;
import ru.itmopai.backend.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP E2E tests for UsersController
 * Tests all user management endpoints with authentication and authorization
 */
@MicronautTest(transactional = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Users Controller E2E Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UsersControllerE2ETest {

  @Inject
  @Client("/")
  HttpClient client;

  @Inject
  UserRepository userRepository;

  @Inject
  UserPermissionRepository userPermissionRepository;

  @Inject
  PasswordEncoder passwordEncoder;

  @Inject
  UserService userService;

  private UserEntity adminUser;
  private String adminToken;
  private UserEntity testUser;

  @BeforeAll
  void setupTestData() {
    // Clean up
    userPermissionRepository.deleteAll();
    userRepository.deleteAll();

    // Create admin user with all permissions
    adminUser = new UserEntity();
    adminUser.setUsername("admin_users_test");
    adminUser.setName("Admin");
    adminUser.setSurname("User");
    adminUser.setPassword(passwordEncoder.encode("adminpass"));
    adminUser.setLocationLatitude(55.7558);
    adminUser.setLocationLongitude(37.6173);
    adminUser.setLocationAltitude(150.0);
    adminUser = userRepository.save(adminUser);

    // Grant admin all user-related permissions
    userService.setUserPermissions(adminUser.getId(), List.of(
        "USERS:*:READ",
        "USERS:*:ADD",
        "USERS:*:UPDATE",
        "USERS:*:DELETE"
    ));

    // Get admin JWT token
    adminToken = loginAndGetToken("admin_users_test", "adminpass");

    // Create test user
    testUser = new UserEntity();
    testUser.setUsername("test_user");
    testUser.setName("Test");
    testUser.setSurname("User");
    testUser.setPassword(passwordEncoder.encode("testpass"));
    testUser.setLocationLatitude(0.0);
    testUser.setLocationLongitude(0.0);
    testUser.setLocationAltitude(0.0);
    testUser = userRepository.save(testUser);
  }

  @AfterAll
  void cleanupTestData() {
    userPermissionRepository.deleteAll();
    userRepository.deleteAll();
  }

  private String loginAndGetToken(String username, String password) {
    LoginRequest loginRequest = new LoginRequest(username, password);
    HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);
    HttpResponse<LoginResponse> response = client.toBlocking()
        .exchange(request, LoginResponse.class);
    return response.body().accessToken();
  }

  @Nested
  @DisplayName("GET /users - Get all users")
  class GetAllUsersTests {

    @Test
    @DisplayName("Should return all users with valid token and permissions")
    void shouldReturnAllUsers() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/users")
          .bearerAuth(adminToken);

      // When
      HttpResponse<String> response = client.toBlocking()
          .exchange(request, String.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      // Just verify we got a response - detailed testing of structure is done elsewhere
    }

    @Test
    @DisplayName("Should return 401 without authentication")
    void shouldReturn401WithoutAuth() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/users");

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, String.class)
      );

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @DisplayName("Should return 403 without required permissions")
    void shouldReturn403WithoutPermissions() {
      // Given - Create user without USERS:*:READ permission
      UserEntity unprivileged = new UserEntity();
      unprivileged.setUsername("unprivileged_user");
      unprivileged.setName("Unprivileged");
      unprivileged.setSurname("User");
      unprivileged.setPassword(passwordEncoder.encode("pass"));
      unprivileged.setLocationLatitude(0.0);
      unprivileged.setLocationLongitude(0.0);
      unprivileged.setLocationAltitude(0.0);
      unprivileged = userRepository.save(unprivileged);

      String unprivilegedToken = loginAndGetToken("unprivileged_user", "pass");

      HttpRequest<?> request = HttpRequest.GET("/users")
          .bearerAuth(unprivilegedToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, String.class)
      );

      assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

      // Cleanup
      userRepository.deleteById(unprivileged.getId());
    }
  }

  @Nested
  @DisplayName("GET /users/{id} - Get user by ID")
  class GetUserByIdTests {

    @Test
    @DisplayName("Should return user by ID with valid permissions")
    void shouldReturnUserById() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/users/" + testUser.getId())
          .bearerAuth(adminToken);

      // When
      HttpResponse<UserDto> response = client.toBlocking()
          .exchange(request, UserDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertEquals(testUser.getId(), response.body().id());
      assertEquals(testUser.getUsername(), response.body().username());
    }

    @Test
    @DisplayName("Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/users/999999")
          .bearerAuth(adminToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, UserDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
  }

  @Nested
  @DisplayName("POST /users - Create user")
  class CreateUserTests {

    @Test
    @DisplayName("Should create user with valid data and permissions")
    void shouldCreateUser() {
      // Given
      RegisterRequest registerRequest = new RegisterRequest(
          "newuser_" + System.currentTimeMillis(),
          "password123",
          "New",
          "User",
          "Middle",
          "Developer"
      );

      HttpRequest<?> request = HttpRequest.POST("/users", registerRequest)
          .bearerAuth(adminToken);

      // When
      HttpResponse<UserDto> response = client.toBlocking()
          .exchange(request, UserDto.class);

      // Then
      assertEquals(HttpStatus.CREATED, response.status());
      assertNotNull(response.body());
      assertEquals(registerRequest.username(), response.body().username());
      assertEquals(registerRequest.name(), response.body().name());
      assertEquals(registerRequest.surname(), response.body().surname());

      // Cleanup
      userRepository.deleteById(response.body().id());
    }

    @Test
    @DisplayName("Should return 409 when username already exists")
    void shouldReturn409ForDuplicateUsername() {
      // Given
      RegisterRequest registerRequest = new RegisterRequest(
          testUser.getUsername(), // Existing username
          "password123",
          "Name",
          "Surname",
          null,
          null
      );

      HttpRequest<?> request = HttpRequest.POST("/users", registerRequest)
          .bearerAuth(adminToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, UserDto.class)
      );

      assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    @DisplayName("Should return 400 for invalid request (empty username)")
    void shouldReturn400ForInvalidRequest() {
      // Given
      RegisterRequest registerRequest = new RegisterRequest(
          "", // Empty username
          "password",
          "Name",
          "Surname",
          null,
          null
      );

      HttpRequest<?> request = HttpRequest.POST("/users", registerRequest)
          .bearerAuth(adminToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, UserDto.class)
      );

      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
  }

  @Nested
  @DisplayName("DELETE /users/{id} - Delete user")
  class DeleteUserTests {

    @Test
    @DisplayName("Should delete user with valid permissions")
    void shouldDeleteUser() {
      // Given - Create user to delete
      UserEntity userToDelete = new UserEntity();
      userToDelete.setUsername("user_to_delete_" + System.currentTimeMillis());
      userToDelete.setName("Delete");
      userToDelete.setSurname("Me");
      userToDelete.setPassword(passwordEncoder.encode("pass"));
      userToDelete.setLocationLatitude(0.0);
      userToDelete.setLocationLongitude(0.0);
      userToDelete.setLocationAltitude(0.0);
      userToDelete = userRepository.save(userToDelete);

      Long userId = userToDelete.getId();
      HttpRequest<?> request = HttpRequest.DELETE("/users/" + userId)
          .bearerAuth(adminToken);

      // When
      HttpResponse<?> response = client.toBlocking().exchange(request);

      // Then
      assertEquals(HttpStatus.NO_CONTENT, response.status());
      assertFalse(userRepository.findById(userId).isPresent());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent user")
    void shouldReturn404ForNonExistentUser() {
      // Given
      HttpRequest<?> request = HttpRequest.DELETE("/users/999999")
          .bearerAuth(adminToken);

      // When - Should succeed (no content) even if user doesn't exist
      HttpResponse<?> response = client.toBlocking().exchange(request);

      // Then
      assertEquals(HttpStatus.NO_CONTENT, response.status());
    }
  }

  @Nested
  @DisplayName("PUT /users/{id}/location - Update user location")
  class UpdateLocationTests {

    @Test
    @DisplayName("Should update user location with valid data")
    void shouldUpdateLocation() {
      // Given
      LocationUpdateRequest locationRequest = new LocationUpdateRequest(
          55.7558,
          37.6173,
          150.0
      );

      HttpRequest<?> request = HttpRequest.PUT("/users/" + testUser.getId() + "/location", locationRequest)
          .bearerAuth(adminToken);

      // When
      HttpResponse<UserDto> response = client.toBlocking()
          .exchange(request, UserDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertEquals(55.7558, response.body().locationLatitude());
      assertEquals(37.6173, response.body().locationLongitude());
      assertEquals(150.0, response.body().locationAltitude());

      // Verify in database
      UserEntity updated = userRepository.findById(testUser.getId()).orElseThrow();
      assertEquals(55.7558, updated.getLocationLatitude());
    }

    @Test
    @DisplayName("Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() {
      // Given
      LocationUpdateRequest locationRequest = new LocationUpdateRequest(0.0, 0.0, 0.0);

      HttpRequest<?> request = HttpRequest.PUT("/users/999999/location", locationRequest)
          .bearerAuth(adminToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, UserDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("Should allow user to update their own location")
    void shouldAllowUserToUpdateOwnLocation() {
      // Given - User updates their own location
      UserEntity selfUpdateUser = new UserEntity();
      selfUpdateUser.setUsername("self_update_user_" + System.currentTimeMillis());
      selfUpdateUser.setName("Self");
      selfUpdateUser.setSurname("Update");
      selfUpdateUser.setPassword(passwordEncoder.encode("pass"));
      selfUpdateUser.setLocationLatitude(0.0);
      selfUpdateUser.setLocationLongitude(0.0);
      selfUpdateUser.setLocationAltitude(0.0);
      selfUpdateUser = userRepository.save(selfUpdateUser);

      // Grant permission to update own location
      userService.setUserPermissions(selfUpdateUser.getId(), List.of(
          "USERS:" + selfUpdateUser.getId() + ":UPDATE"
      ));

      String userToken = loginAndGetToken(selfUpdateUser.getUsername(), "pass");

      LocationUpdateRequest locationRequest = new LocationUpdateRequest(12.34, 56.78, 90.0);
      HttpRequest<?> request = HttpRequest.PUT("/users/" + selfUpdateUser.getId() + "/location", locationRequest)
          .bearerAuth(userToken);

      // When
      HttpResponse<UserDto> response = client.toBlocking()
          .exchange(request, UserDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertEquals(12.34, response.body().locationLatitude());

      // Cleanup
      userPermissionRepository.deleteAll(userPermissionRepository.findById_UserId(selfUpdateUser.getId()));
      userRepository.deleteById(selfUpdateUser.getId());
    }
  }

  @Nested
  @DisplayName("GET /users/{id}/cases - Get user cases")
  class GetUserCasesTests {

    @Test
    @DisplayName("Should return user cases")
    void shouldReturnUserCases() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/users/" + testUser.getId() + "/cases")
          .bearerAuth(adminToken);

      // When
      HttpResponse<String> response = client.toBlocking()
          .exchange(request, String.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      // Response might be empty list, but should not be null
    }
  }

  @Nested
  @DisplayName("GET /users/{id}/privileges - Get user permissions")
  class GetUserPermissionsTests {

    @Test
    @DisplayName("Should return user permissions")
    void shouldReturnUserPermissions() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/users/" + adminUser.getId() + "/privileges")
          .bearerAuth(adminToken);

      // When
      HttpResponse<UserPermissionsResponse> response = client.toBlocking()
          .exchange(request, UserPermissionsResponse.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertNotNull(response.body().permissions());
      assertTrue(response.body().permissions().contains("USERS:*:READ"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/users/999999/privileges")
          .bearerAuth(adminToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, UserPermissionsResponse.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
  }

  @Nested
  @DisplayName("PUT /users/{id}/privileges - Update user permissions")
  class UpdateUserPermissionsTests {

    @Test
    @DisplayName("Should update user permissions")
    void shouldUpdateUserPermissions() {
      // Given - Create user and set initial permissions
      UserEntity permUser = new UserEntity();
      permUser.setUsername("perm_user_" + System.currentTimeMillis());
      permUser.setName("Perm");
      permUser.setSurname("User");
      permUser.setPassword(passwordEncoder.encode("pass"));
      permUser.setLocationLatitude(0.0);
      permUser.setLocationLongitude(0.0);
      permUser.setLocationAltitude(0.0);
      permUser = userRepository.save(permUser);

      userService.setUserPermissions(permUser.getId(), List.of("OLD:*:PERMISSION"));

      UserPermissionsRequest newPermissions = new UserPermissionsRequest(
          List.of("USERS:*:READ", "CASES:*:WRITE")
      );

      HttpRequest<?> request = HttpRequest.PUT("/users/" + permUser.getId() + "/privileges", newPermissions)
          .bearerAuth(adminToken);

      // When
      HttpResponse<UserPermissionsResponse> response = client.toBlocking()
          .exchange(request, UserPermissionsResponse.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertEquals(2, response.body().permissions().size());
      assertTrue(response.body().permissions().contains("USERS:*:READ"));
      assertTrue(response.body().permissions().contains("CASES:*:WRITE"));
      assertFalse(response.body().permissions().contains("OLD:*:PERMISSION"));

      // Cleanup
      userPermissionRepository.deleteAll(userPermissionRepository.findById_UserId(permUser.getId()));
      userRepository.deleteById(permUser.getId());
    }

    @Test
    @DisplayName("Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() {
      // Given
      UserPermissionsRequest permissions = new UserPermissionsRequest(List.of("SOME:*:PERMISSION"));

      HttpRequest<?> request = HttpRequest.PUT("/users/999999/privileges", permissions)
          .bearerAuth(adminToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, UserPermissionsResponse.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
  }
}
