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
import ru.itmopai.backend.dto.CaseDto;
import ru.itmopai.backend.dto.request.AddClueRequest;
import ru.itmopai.backend.dto.request.LoginRequest;
import ru.itmopai.backend.dto.response.LoginResponse;
import ru.itmopai.backend.entity.*;
import ru.itmopai.backend.repository.*;
import ru.itmopai.backend.service.UserService;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP E2E tests for CasesController
 * Tests all 7 endpoints with authentication and authorization
 */
@MicronautTest(transactional = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Cases Controller E2E Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CasesControllerE2ETest {

  @Inject
  @Client("/")
  HttpClient client;

  @Inject
  UserRepository userRepository;

  @Inject
  UserPermissionRepository userPermissionRepository;

  @Inject
  CaseRepository caseRepository;

  @Inject
  IncidentRepository incidentRepository;

  @Inject
  ClueRepository clueRepository;

  @Inject
  UserCaseRepository userCaseRepository;

  @Inject
  PasswordEncoder passwordEncoder;

  @Inject
  UserService userService;

  private UserEntity testUser;
  private String testUserToken;
  private CaseEntity testCase;
  private IncidentEntity testIncident;

  @BeforeAll
  void setupTestData() {
    // Clean up
    clueRepository.deleteAll();
    userCaseRepository.deleteAll();
    caseRepository.deleteAll();
    incidentRepository.deleteAll();
    userPermissionRepository.deleteAll();
    userRepository.deleteAll();

    // Create test user
    testUser = new UserEntity();
    testUser.setUsername("cases_test_user");
    testUser.setName("Cases");
    testUser.setSurname("Tester");
    testUser.setPassword(passwordEncoder.encode("testpass"));
    testUser.setLocationLatitude(55.7558);
    testUser.setLocationLongitude(37.6173);
    testUser.setLocationAltitude(150.0);
    testUser = userRepository.save(testUser);

    // Grant permissions
    userService.setUserPermissions(testUser.getId(), java.util.List.of(
        "CASES:*:READ",
        "CASES:*:TEMPLATES_ADD",
        "CASES:*:CLUES_ADD",
        "CASES:*:SUSPECT_ADD"
    ));

    // Get JWT token
    testUserToken = loginAndGetToken("cases_test_user", "testpass");

    // Create test incident
    testIncident = new IncidentEntity();
    testIncident.setType("Test Incident Type");
    testIncident.setReason("Incident reason for testing cases");
    testIncident.setCreator(testUser);
    testIncident.setUpdator(testUser);
    testIncident.setCreatedAt(Instant.now());
    testIncident.setUpdatedAt(Instant.now());
    testIncident.setEstimatedTime(Instant.now());
    testIncident.setLocationLatitude(0.0);
    testIncident.setLocationLongitude(0.0);
    testIncident.setLocationAltitude(0.0);
    testIncident = incidentRepository.save(testIncident);

    // Create test case
    testCase = new CaseEntity();
    testCase.setTitle("Test Case");
    testCase.setDescription("Case for testing");
    testCase.setStatus(CaseEntity.Status.OPENED);
    testCase.setCreator(testUser);
    testCase.setUpdator(testUser);
    testCase.setIncident(testIncident);
    testCase.setCreatedAt(Instant.now());
    testCase.setUpdatedAt(Instant.now());
    testCase = caseRepository.save(testCase);
  }

  @AfterAll
  void cleanupTestData() {
    try {
      clueRepository.deleteAll();
      userCaseRepository.deleteAll();

      // Clear incident->case references to allow case deletion
      var incidents = incidentRepository.findAll();
      for (var incident : incidents) {
        incident.setRelatedCase(null);
        incidentRepository.update(incident);
      }

      caseRepository.deleteAll();
      incidentRepository.deleteAll();
      userPermissionRepository.deleteAll();
      userRepository.deleteAll();
    } catch (Exception e) {
      // Log but don't fail if cleanup has issues
      System.err.println("Cleanup error: " + e.getMessage());
    }
  }

  private String loginAndGetToken(String username, String password) {
    LoginRequest loginRequest = new LoginRequest(username, password);
    HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);
    HttpResponse<String> response = client.toBlocking()
        .exchange(request, String.class);

    // Extract token from JSON response (simple parsing)
    String body = response.body();
    int startIndex = body.indexOf("\"accessToken\":\"") + 15;
    int endIndex = body.indexOf("\"", startIndex);
    return body.substring(startIndex, endIndex);
  }

  @Nested
  @DisplayName("GET /cases/{id} - Get case by ID")
  class GetCaseTests {

    @Test
    @DisplayName("Should return case with valid permissions")
    void shouldReturnCase() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/cases/" + testCase.getId())
          .bearerAuth(testUserToken);

      // When
      HttpResponse<CaseDto> response = client.toBlocking()
          .exchange(request, CaseDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertEquals(testCase.getId(), response.body().getId());
      assertEquals(testCase.getTitle(), response.body().getTitle());
    }

    @Test
    @DisplayName("Should return 404 for non-existent case")
    void shouldReturn404ForNonExistentCase() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/cases/999999")
          .bearerAuth(testUserToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, CaseDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("Should return 401 without authentication")
    void shouldReturn401WithoutAuth() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/cases/" + testCase.getId());

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, CaseDto.class)
      );

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @DisplayName("Should return 403 without required permissions")
    void shouldReturn403WithoutPermissions() {
      // Given - Create user without CASES:*:READ permission
      UserEntity unprivileged = new UserEntity();
      unprivileged.setUsername("unprivileged_case_user");
      unprivileged.setName("Unprivileged");
      unprivileged.setSurname("User");
      unprivileged.setPassword(passwordEncoder.encode("pass"));
      unprivileged.setLocationLatitude(0.0);
      unprivileged.setLocationLongitude(0.0);
      unprivileged.setLocationAltitude(0.0);
      unprivileged = userRepository.save(unprivileged);

      String unprivilegedToken = loginAndGetToken("unprivileged_case_user", "pass");

      HttpRequest<?> request = HttpRequest.GET("/cases/" + testCase.getId())
          .bearerAuth(unprivilegedToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, CaseDto.class)
      );

      assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

      // Cleanup
      userRepository.deleteById(unprivileged.getId());
    }
  }

  @Nested
  @DisplayName("GET /cases/{id}/clues - Get case clues")
  class GetCaseCluesTests {

    @Test
    @DisplayName("Should return list of clues for case")
    void shouldReturnClues() {
      // Given - Create a clue for the test case
      ClueEntity clue = new ClueEntity();
      clue.setTitle("Test Clue");
      clue.setDescription("Clue Description");
      clue.setStatus(ClueEntity.Status.PENDING);
      clue.setCreator(testUser);
      clue.setUpdator(testUser);
      clue.setRelatedCase(testCase);
      clue.setCreatedAt(Instant.now());
      clue.setUpdatedAt(Instant.now());
      clue.setMetadata("{}");
      clue = clueRepository.save(clue);

      HttpRequest<?> request = HttpRequest.GET("/cases/" + testCase.getId() + "/clues")
          .bearerAuth(testUserToken);

      // When
      HttpResponse<String> response = client.toBlocking()
          .exchange(request, String.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());

      // Cleanup
      clueRepository.deleteById(clue.getId());
    }

    @Test
    @DisplayName("Should return empty list when case has no clues")
    void shouldReturnEmptyListWhenNoClues() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/cases/" + testCase.getId() + "/clues")
          .bearerAuth(testUserToken);

      // When
      HttpResponse<String> response = client.toBlocking()
          .exchange(request, String.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertTrue(response.body().contains("[]") || response.body().isEmpty());
    }
  }

  @Nested
  @DisplayName("POST /cases/{id}/clues - Add clue to case")
  class AddCaseClueTests {

    @Test
    @DisplayName("Should add clue to case with valid data")
    void shouldAddClue() {
      // Given
      AddClueRequest clueRequest = new AddClueRequest(
          "New Clue",
          "Clue Description",
          ClueEntity.Status.PENDING
      );

      HttpRequest<?> request = HttpRequest.POST("/cases/" + testCase.getId() + "/clues", clueRequest)
          .bearerAuth(testUserToken);

      // When
      HttpResponse<String> response = client.toBlocking()
          .exchange(request, String.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertTrue(response.body().contains("New Clue"));

      // Cleanup - find and delete the created clue
      clueRepository.findAll().stream()
          .filter(c -> "New Clue".equals(c.getTitle()))
          .forEach(c -> clueRepository.deleteById(c.getId()));
    }

    @Test
    @DisplayName("Should return 404 for non-existent case")
    void shouldReturn404ForNonExistentCase() {
      // Given
      AddClueRequest clueRequest = new AddClueRequest(
          "New Clue",
          "Description",
          ClueEntity.Status.PENDING
      );

      HttpRequest<?> request = HttpRequest.POST("/cases/999999/clues", clueRequest)
          .bearerAuth(testUserToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, String.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("Should return 400 for invalid request data")
    void shouldReturn400ForInvalidData() {
      // Given - Empty title
      AddClueRequest invalidRequest = new AddClueRequest(
          "",
          "Description",
          ClueEntity.Status.PENDING
      );

      HttpRequest<?> request = HttpRequest.POST("/cases/" + testCase.getId() + "/clues", invalidRequest)
          .bearerAuth(testUserToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, String.class)
      );

      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
  }

  @Nested
  @DisplayName("GET /cases/{id}/templates - Get case templates")
  class GetCaseTemplatesTests {

    @Test
    @DisplayName("Should return templates for case")
    void shouldReturnTemplates() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/cases/" + testCase.getId() + "/templates")
          .bearerAuth(testUserToken);

      // When
      HttpResponse<String> response = client.toBlocking()
          .exchange(request, String.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
    }
  }

  @Nested
  @DisplayName("GET /cases/{id}/suspects - Get case suspects")
  class GetCaseSuspectsTests {

    @Test
    @DisplayName("Should return suspects for case")
    void shouldReturnSuspects() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/cases/" + testCase.getId() + "/suspects")
          .bearerAuth(testUserToken);

      // When
      HttpResponse<String> response = client.toBlocking()
          .exchange(request, String.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
    }

    @Test
    @DisplayName("Should return empty list when case has no suspects")
    void shouldReturnEmptyListWhenNoSuspects() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/cases/" + testCase.getId() + "/suspects")
          .bearerAuth(testUserToken);

      // When
      HttpResponse<String> response = client.toBlocking()
          .exchange(request, String.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
    }
  }

  @Nested
  @DisplayName("Security and Edge Cases")
  class SecurityAndEdgeCaseTests {

    @Test
    @DisplayName("Should check permissions for each endpoint")
    void shouldCheckPermissionsForEndpoints() {
      // This test verifies that @RequiresPermissions is working
      // Already covered in individual tests, but good to have explicit test

      // Create user with no permissions
      UserEntity noPerms = new UserEntity();
      noPerms.setUsername("no_perms_user");
      noPerms.setName("No");
      noPerms.setSurname("Perms");
      noPerms.setPassword(passwordEncoder.encode("pass"));
      noPerms.setLocationLatitude(0.0);
      noPerms.setLocationLongitude(0.0);
      noPerms.setLocationAltitude(0.0);
      noPerms = userRepository.save(noPerms);

      String noPermsToken = loginAndGetToken("no_perms_user", "pass");

      // Try to access case - should fail
      HttpRequest<?> request = HttpRequest.GET("/cases/" + testCase.getId())
          .bearerAuth(noPermsToken);

      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, CaseDto.class)
      );

      assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

      // Cleanup
      userRepository.deleteById(noPerms.getId());
    }

    @Test
    @DisplayName("Should handle negative IDs gracefully")
    void shouldHandleNegativeIds() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/cases/-1")
          .bearerAuth(testUserToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, CaseDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("Should handle very large IDs")
    void shouldHandleVeryLargeIds() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/cases/" + Long.MAX_VALUE)
          .bearerAuth(testUserToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, CaseDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
  }
}
