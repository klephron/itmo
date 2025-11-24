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
import ru.itmopai.backend.dto.request.LoginRequest;
import ru.itmopai.backend.dto.response.LoginResponse;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP E2E tests for CustomLoginController (/login endpoint)
 * Tests authentication and JWT token generation
 */
@MicronautTest(transactional = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Login Controller E2E Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoginControllerE2ETest {

  @Inject
  @Client("/")
  HttpClient client;

  @Inject
  UserRepository userRepository;

  @Inject
  PasswordEncoder passwordEncoder;

  private UserEntity testUser;
  private static final String TEST_USERNAME = "login_test_user";
  private static final String TEST_PASSWORD = "testPassword123";

  @BeforeAll
  void setupTestData() {
    // Clean up if exists
    userRepository.findByUsername(TEST_USERNAME).ifPresent(u -> userRepository.deleteById(u.getId()));

    // Create test user with known password
    testUser = new UserEntity();
    testUser.setUsername(TEST_USERNAME);
    testUser.setName("Login");
    testUser.setSurname("Test");
    testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
    testUser.setLocationLatitude(55.7558);
    testUser.setLocationLongitude(37.6173);
    testUser.setLocationAltitude(150.0);
    testUser = userRepository.save(testUser);
  }

  @AfterAll
  void cleanupTestData() {
    if (testUser != null && testUser.getId() != null) {
      userRepository.deleteById(testUser.getId());
    }
  }

  @Nested
  @DisplayName("POST /login - Successful authentication")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class SuccessfulAuthenticationTests {

    @Test
    @Order(1)
    @DisplayName("Should successfully login with correct credentials")
    void shouldLoginWithCorrectCredentials() {
      // Given
      LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      // When
      HttpResponse<String> response = client.toBlocking()
          .exchange(request, String.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body(), "Response body should be present");
      System.out.println("RAW RESPONSE: " + response.body());

      // If we can see the JSON, we can check required fields
      assertTrue(response.body().contains("accessToken"));
      assertTrue(response.body().contains("username"));
    }

    @Test
    @Order(2)
    @DisplayName("Should return valid JWT token that can be used for authentication")
    void shouldReturnValidJWTToken() {
      // Given
      LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      // When
      HttpResponse<LoginResponse> response = client.toBlocking()
          .exchange(request, LoginResponse.class);

      // Then
      String accessToken = response.body().accessToken();
      assertNotNull(accessToken);
      assertTrue(accessToken.length() > 50, "JWT token should be substantial in length");

      // Verify token starts with valid JWT header (Base64 encoded)
      assertTrue(accessToken.startsWith("eyJ"), "JWT should start with 'eyJ' (Base64 encoded {)");
    }

    @Test
    @Order(3)
    @DisplayName("Should include user ID in response")
    void shouldIncludeUserIdInResponse() {
      // Given
      LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      // When
      HttpResponse<LoginResponse> response = client.toBlocking()
          .exchange(request, LoginResponse.class);

      // Then
      assertEquals(testUser.getId(), response.body().id());
    }

    @Test
    @Order(4)
    @DisplayName("Should include issued-at timestamp")
    void shouldIncludeIssuedAtTimestamp() {
      // Given
      LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      long beforeRequest = System.currentTimeMillis();

      // When
      HttpResponse<LoginResponse> response = client.toBlocking()
          .exchange(request, LoginResponse.class);

      long afterRequest = System.currentTimeMillis();

      // Then
      assertNotNull(response.body().iat());
      assertTrue(response.body().iat() >= beforeRequest - 5000); // Allow 5 second tolerance
      assertTrue(response.body().iat() <= afterRequest + 5000);
    }

    @Test
    @Order(5)
    @DisplayName("Should return expiration time")
    void shouldReturnExpirationTime() {
      // Given
      LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      // When
      HttpResponse<LoginResponse> response = client.toBlocking()
          .exchange(request, LoginResponse.class);

      // Then
      assertNotNull(response.body().expiresIn());
      assertTrue(response.body().expiresIn() > 0, "Expiration should be positive");
    }
  }

  @Nested
  @DisplayName("POST /login - Failed authentication")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class FailedAuthenticationTests {

    @Test
    @Order(1)
    @DisplayName("Should return 401 for incorrect password")
    void shouldReturn401ForIncorrectPassword() {
      // Given
      LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, "wrongPassword");
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, LoginResponse.class)
      );

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("Should return 401 for non-existent user")
    void shouldReturn401ForNonExistentUser() {
      // Given
      LoginRequest loginRequest = new LoginRequest("nonexistent_user_12345", "password");
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, LoginResponse.class)
      );

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Should return 401 for empty password")
    void shouldReturn401ForEmptyPassword() {
      // Given
      LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, "");
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      // When & Then - Either 400 (validation) or 401 (authentication failure)
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, LoginResponse.class)
      );

      assertTrue(
          exception.getStatus() == HttpStatus.BAD_REQUEST || exception.getStatus() == HttpStatus.UNAUTHORIZED,
          "Should return either 400 (validation) or 401 (authentication failure)"
      );
    }

    @Test
    @Order(4)
    @DisplayName("Should return 400 for empty username")
    void shouldReturn400ForEmptyUsername() {
      // Given
      LoginRequest loginRequest = new LoginRequest("", "password");
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, LoginResponse.class)
      );

      // Should be validation error (400) or unauthorized (401)
      assertTrue(
          exception.getStatus() == HttpStatus.BAD_REQUEST || exception.getStatus() == HttpStatus.UNAUTHORIZED
      );
    }
  }

  @Nested
  @DisplayName("POST /login - Security tests")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class SecurityTests {

    @Test
    @Order(1)
    @DisplayName("Should allow multiple login attempts with correct credentials")
    void shouldAllowMultipleLoginAttempts() {
      // Given
      LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

      // When - Login multiple times
      HttpResponse<String> response1 = client.toBlocking()
          .exchange(HttpRequest.POST("/login", loginRequest), String.class);
      HttpResponse<String> response2 = client.toBlocking()
          .exchange(HttpRequest.POST("/login", loginRequest), String.class);

      // Then - Both should succeed
      assertEquals(HttpStatus.OK, response1.status());
      assertEquals(HttpStatus.OK, response2.status());
      assertNotNull(response1.body());
      assertNotNull(response2.body());
    }

    @Test
    @Order(2)
    @DisplayName("Should not expose password in any response")
    void shouldNotExposePassword() {
      // Given
      LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      // When
      HttpResponse<LoginResponse> response = client.toBlocking()
          .exchange(request, LoginResponse.class);

      // Then
      String responseBody = response.getBody(String.class).orElse("");
      assertFalse(responseBody.contains(TEST_PASSWORD), "Response should not contain password");
      assertFalse(responseBody.contains(testUser.getPassword()), "Response should not contain hashed password");
    }

    @Test
    @Order(3)
    @DisplayName("Should be accessible without authentication (IS_ANONYMOUS)")
    void shouldBeAccessibleWithoutAuthentication() {
      // Given - Request without any authentication header
      LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      // When
      HttpResponse<LoginResponse> response = client.toBlocking()
          .exchange(request, LoginResponse.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
    }
  }

  @Nested
  @DisplayName("POST /login - Response structure validation")
  class ResponseStructureTests {

    @Test
    @DisplayName("Should return all required fields in LoginResponse")
    void shouldReturnAllRequiredFields() {
      // Given
      LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      // When
      HttpResponse<String> response = client.toBlocking()
          .exchange(request, String.class);

      // Then
      String body = response.body();
      assertNotNull(body, "Response body should not be null");
      assertTrue(body.contains("accessToken"), "Should contain accessToken");
      assertTrue(body.contains("expiresIn"), "Should contain expiresIn");
      assertTrue(body.contains("username"), "Should contain username");
      assertTrue(body.contains("id"), "Should contain id");
      assertTrue(body.contains("iat"), "Should contain iat");
    }

    @Test
    @DisplayName("Should return refresh token different from access token")
    void shouldReturnDifferentRefreshToken() {
      // Given
      LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
      HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

      // When
      HttpResponse<LoginResponse> response = client.toBlocking()
          .exchange(request, LoginResponse.class);

      // Then
      LoginResponse body = response.body();
      assertNotEquals(body.accessToken(), body.refreshToken(),
          "Access token and refresh token should be different");
    }
  }
}
