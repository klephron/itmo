package ru.itmopai.backend.security;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationProviderUserPassword Unit Tests")
class AuthenticationProviderUserPasswordTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private AuthenticationProviderUserPassword authProvider;

  private UserEntity testUser;
  private HttpRequest<?> mockRequest;

  @BeforeEach
  void setUp() {
    testUser = new UserEntity();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setPassword("$2a$10$hashedpassword");
    testUser.setName("Test");
    testUser.setSurname("User");
    testUser.setLocationLatitude(0.0);
    testUser.setLocationLongitude(0.0);
    testUser.setLocationAltitude(0.0);

    mockRequest = mock(HttpRequest.class);
  }

  @Nested
  @DisplayName("authenticate() method tests")
  class AuthenticateTests {

    @Test
    @DisplayName("Should successfully authenticate with correct credentials")
    void shouldAuthenticateWithCorrectCredentials() {
      // Given
      String username = "testuser";
      String password = "correctpassword";
      AuthenticationRequest<String, String> authRequest =
          new UsernamePasswordCredentials(username, password);

      when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);

      // When
      AuthenticationResponse response = authProvider.authenticate(mockRequest, authRequest);

      // Then
      assertTrue(response.isAuthenticated());
      assertTrue(response.getAuthentication().isPresent());
      assertEquals(username, response.getAuthentication().get().getName());
      assertEquals(testUser.getId(), response.getAuthentication().get().getAttributes().get("id"));

      verify(userRepository).findByUsername(username);
      verify(passwordEncoder).matches(password, testUser.getPassword());
    }

    @Test
    @DisplayName("Should fail authentication with incorrect password")
    void shouldFailAuthenticationWithIncorrectPassword() {
      // Given
      String username = "testuser";
      String wrongPassword = "wrongpassword";
      AuthenticationRequest<String, String> authRequest =
          new UsernamePasswordCredentials(username, wrongPassword);

      when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches(wrongPassword, testUser.getPassword())).thenReturn(false);

      // When & Then
      assertThrows(AuthenticationException.class, () -> {
        authProvider.authenticate(mockRequest, authRequest);
      });

      verify(userRepository).findByUsername(username);
      verify(passwordEncoder).matches(wrongPassword, testUser.getPassword());
    }

    @Test
    @DisplayName("Should fail authentication with non-existent user")
    void shouldFailAuthenticationWithNonExistentUser() {
      // Given
      String username = "nonexistent";
      String password = "password";
      AuthenticationRequest<String, String> authRequest =
          new UsernamePasswordCredentials(username, password);

      when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

      // When & Then
      assertThrows(AuthenticationException.class, () -> {
        authProvider.authenticate(mockRequest, authRequest);
      });

      verify(userRepository).findByUsername(username);
      verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should include user ID in authentication attributes")
    void shouldIncludeUserIdInAuthenticationAttributes() {
      // Given
      String username = "testuser";
      String password = "password";
      Long expectedUserId = 42L;

      testUser.setId(expectedUserId);
      AuthenticationRequest<String, String> authRequest =
          new UsernamePasswordCredentials(username, password);

      when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);

      // When
      AuthenticationResponse response = authProvider.authenticate(mockRequest, authRequest);

      // Then
      assertTrue(response.isAuthenticated());
      assertTrue(response.getAuthentication().isPresent());

      Object userId = response.getAuthentication().get().getAttributes().get("id");
      assertNotNull(userId);
      assertEquals(expectedUserId, userId);
    }

    @Test
    @DisplayName("Should handle null request context gracefully")
    void shouldHandleNullRequestContext() {
      // Given
      String username = "testuser";
      String password = "password";
      AuthenticationRequest<String, String> authRequest =
          new UsernamePasswordCredentials(username, password);

      when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);

      // When
      AuthenticationResponse response = authProvider.authenticate(null, authRequest);

      // Then
      assertTrue(response.isAuthenticated());
      verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Should use username as principal name")
    void shouldUseUsernameAsPrincipalName() {
      // Given
      String username = "john.doe";
      String password = "password";
      AuthenticationRequest<String, String> authRequest =
          new UsernamePasswordCredentials(username, password);

      when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);

      // When
      AuthenticationResponse response = authProvider.authenticate(mockRequest, authRequest);

      // Then
      assertTrue(response.isAuthenticated());
      assertEquals(username, response.getAuthentication().get().getName());
    }

    @Test
    @DisplayName("Should validate password using encoder")
    void shouldValidatePasswordUsingEncoder() {
      // Given
      String username = "testuser";
      String rawPassword = "mySecretPassword";
      String hashedPassword = "$2a$10$differenthash";
      testUser.setPassword(hashedPassword);

      AuthenticationRequest<String, String> authRequest =
          new UsernamePasswordCredentials(username, rawPassword);

      when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);

      // When
      AuthenticationResponse response = authProvider.authenticate(mockRequest, authRequest);

      // Then
      assertTrue(response.isAuthenticated());
      verify(passwordEncoder).matches(rawPassword, hashedPassword);
    }
  }

  @Nested
  @DisplayName("Edge cases and security tests")
  class SecurityTests {

    @Test
    @DisplayName("Should fail with empty username")
    void shouldFailWithEmptyUsername() {
      // Given
      String emptyUsername = "";
      String password = "password";
      AuthenticationRequest<String, String> authRequest =
          new UsernamePasswordCredentials(emptyUsername, password);

      when(userRepository.findByUsername(emptyUsername)).thenReturn(Optional.empty());

      // When & Then
      assertThrows(AuthenticationException.class, () -> {
        authProvider.authenticate(mockRequest, authRequest);
      });

      verify(userRepository).findByUsername(emptyUsername);
    }

    @Test
    @DisplayName("Should fail with empty password")
    void shouldFailWithEmptyPassword() {
      // Given
      String username = "testuser";
      String emptyPassword = "";
      AuthenticationRequest<String, String> authRequest =
          new UsernamePasswordCredentials(username, emptyPassword);

      when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches(emptyPassword, testUser.getPassword())).thenReturn(false);

      // When & Then
      assertThrows(AuthenticationException.class, () -> {
        authProvider.authenticate(mockRequest, authRequest);
      });

      verify(passwordEncoder).matches(emptyPassword, testUser.getPassword());
    }

    @Test
    @DisplayName("Should not expose password in successful authentication")
    void shouldNotExposePasswordInSuccessfulAuth() {
      // Given
      String username = "testuser";
      String password = "password";
      AuthenticationRequest<String, String> authRequest =
          new UsernamePasswordCredentials(username, password);

      when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
      when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);

      // When
      AuthenticationResponse response = authProvider.authenticate(mockRequest, authRequest);

      // Then
      assertTrue(response.isAuthenticated());
      assertFalse(response.getAuthentication().get().getAttributes().containsKey("password"));
    }
  }
}
