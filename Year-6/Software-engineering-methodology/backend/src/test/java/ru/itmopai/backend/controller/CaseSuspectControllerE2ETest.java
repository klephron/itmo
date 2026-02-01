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
import ru.itmopai.backend.dto.CaseSuspectDto;
import ru.itmopai.backend.dto.request.LoginRequest;
import ru.itmopai.backend.dto.request.UpdateCaseSuspectRequest;
import ru.itmopai.backend.dto.response.LoginResponse;
import ru.itmopai.backend.entity.*;
import ru.itmopai.backend.repository.*;
import ru.itmopai.backend.service.UserService;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP E2E тесты для CaseSuspectController
 * Тестируют контроллер через настоящие HTTP запросы с JWT аутентификацией
 */
@MicronautTest(transactional = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("CaseSuspect Controller HTTP E2E Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CaseSuspectControllerE2ETest {

  @Inject
  @Client("/")
  HttpClient client;

  @Inject
  UserRepository userRepository;

  @Inject
  CaseRepository caseRepository;

  @Inject
  IncidentRepository incidentRepository;

  @Inject
  CaseSuspectRepository caseSuspectRepository;

  @Inject
  UserPermissionRepository userPermissionRepository;

  @Inject
  PasswordEncoder passwordEncoder;

  @Inject
  UserService userService;

  private UserEntity testUser;
  private CaseEntity testCase;
  private String jwtToken;
  private CaseSuspectEntity testSuspect;

  @BeforeAll
  void setupTestData() {
    // Очищаем данные
    userPermissionRepository.deleteAll();
    caseSuspectRepository.deleteAll();
    caseRepository.deleteAll();
    incidentRepository.deleteAll();
    userRepository.deleteAll();

    // Создаем тестового пользователя с паролем
    testUser = new UserEntity();
    testUser.setUsername("suspect_e2e_user");
    testUser.setName("HTTP");
    testUser.setSurname("Tester");
    testUser.setPassword(passwordEncoder.encode("testpassword123"));
    testUser.setLocationLatitude(55.7558);
    testUser.setLocationLongitude(37.6173);
    testUser.setLocationAltitude(150.0);
    testUser = userRepository.save(testUser);

    // Создаем тестовый инцидент
    IncidentEntity testIncident = new IncidentEntity();
    testIncident.setType("HTTP E2E Test Incident");
    testIncident.setReason("HTTP E2E Testing");
    testIncident.setCreator(testUser);
    testIncident.setUpdator(testUser);
    testIncident.setCreatedAt(Instant.now());
    testIncident.setUpdatedAt(Instant.now());
    testIncident.setLocationLatitude(55.7558);
    testIncident.setLocationLongitude(37.6173);
    testIncident.setLocationAltitude(150.0);
    testIncident.setEstimatedTime(Instant.now());
    testIncident = incidentRepository.save(testIncident);

    // Создаем тестовое дело
    testCase = new CaseEntity();
    testCase.setTitle("HTTP E2E Test Case");
    testCase.setDescription("Case for HTTP E2E testing");
    testCase.setStatus(CaseEntity.Status.OPENED);
    testCase.setCreator(testUser);
    testCase.setUpdator(testUser);
    testCase.setCreatedAt(Instant.now());
    testCase.setUpdatedAt(Instant.now());
    testCase.setIncident(testIncident);
    testCase = caseRepository.save(testCase);

    // Создаем права доступа для пользователя
    userService.setUserPermissions(testUser.getId(), List.of(
        "CASES:" + testCase.getId() + ":READ",
        "CASES:" + testCase.getId() + ":SUSPECTS_READ",
        "CASES:" + testCase.getId() + ":SUSPECTS_UPDATE",
        "CASES:" + testCase.getId() + ":SUSPECTS_DELETE"
    ));

    // Создаем тестового подозреваемого
    testSuspect = new CaseSuspectEntity();
    testSuspect.setName("John");
    testSuspect.setSurname("Doe");
    testSuspect.setPatronymic("Smith");
    testSuspect.setIdentifier("PASSPORT-E2E-" + System.currentTimeMillis());
    testSuspect.setStatus(CaseSuspectEntity.Status.SUSPECTED);
    testSuspect.setVerdict(null);
    testSuspect.setAccusation(null);
    testSuspect.setCreator(testUser);
    testSuspect.setUpdator(testUser);
    testSuspect.setRelatedCase(testCase);
    testSuspect.setCreatedAt(Instant.now());
    testSuspect.setUpdatedAt(Instant.now());
    testSuspect = caseSuspectRepository.save(testSuspect);

    // Получаем JWT токен через HTTP запрос к /login
    jwtToken = loginAndGetToken("suspect_e2e_user", "testpassword123");
  }

  @AfterAll
  void cleanupTestData() {
    // Удаляем в правильном порядке, соблюдая внешние ключи
    try {
      userPermissionRepository.deleteAll();
      caseSuspectRepository.deleteAll();
      caseRepository.deleteAll();
      incidentRepository.deleteAll();
      userRepository.deleteAll();
    } catch (Exception e) {
      System.err.println("Cleanup error: " + e.getMessage());
    }
  }

  /**
   * Вспомогательный метод для получения JWT токена через HTTP /login
   */
  private String loginAndGetToken(String username, String password) {
    LoginRequest loginRequest = new LoginRequest(username, password);
    HttpRequest<?> request = HttpRequest.POST("/login", loginRequest);

    HttpResponse<LoginResponse> response = client.toBlocking()
        .exchange(request, LoginResponse.class);

    assertEquals(HttpStatus.OK, response.status());
    assertNotNull(response.body());
    assertNotNull(response.body().accessToken());
    return response.body().accessToken();
  }

  @Nested
  @DisplayName("GET /suspects/{id} - Получение подозреваемого")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class GetCaseSuspectTests {

    private CaseSuspectEntity getSuspect;

    @BeforeEach
    void setupGetSuspect() {
      // Создаем отдельного подозреваемого для GET тестов
      getSuspect = new CaseSuspectEntity();
      getSuspect.setName("GET Test Name");
      getSuspect.setSurname("GET Test Surname");
      getSuspect.setPatronymic("GET Test Patronymic");
      getSuspect.setIdentifier("PASSPORT-GET-" + System.currentTimeMillis());
      getSuspect.setStatus(CaseSuspectEntity.Status.SUSPECTED);
      getSuspect.setCreator(testUser);
      getSuspect.setUpdator(testUser);
      getSuspect.setRelatedCase(testCase);
      getSuspect.setCreatedAt(Instant.now());
      getSuspect.setUpdatedAt(Instant.now());
      getSuspect = caseSuspectRepository.save(getSuspect);
    }

    @AfterEach
    void cleanupGetSuspect() {
      if (getSuspect != null && getSuspect.getId() != null) {
        try {
          caseSuspectRepository.deleteById(getSuspect.getId());
        } catch (Exception e) {
          // ignore
        }
      }
    }

    @Test
    @Order(1)
    @DisplayName("Should successfully get suspect with valid JWT token")
    void shouldGetSuspectWithValidToken() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/suspects/" + getSuspect.getId())
          .bearerAuth(jwtToken);

      // When
      HttpResponse<CaseSuspectDto> response = client.toBlocking()
          .exchange(request, CaseSuspectDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertEquals(getSuspect.getId(), response.body().id());
      assertEquals("GET Test Name", response.body().name());
      assertEquals("GET Test Surname", response.body().surname());
      assertEquals("GET Test Patronymic", response.body().patronymic());
      assertEquals(CaseSuspectEntity.Status.SUSPECTED, response.body().status());
    }

    @Test
    @Order(2)
    @DisplayName("Should return 401 when no JWT token provided")
    void shouldReturn401WithoutToken() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/suspects/" + getSuspect.getId());

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, CaseSuspectDto.class)
      );

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Should return 401 with invalid JWT token")
    void shouldReturn401WithInvalidToken() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/suspects/" + getSuspect.getId())
          .bearerAuth("invalid.jwt.token");

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, CaseSuspectDto.class)
      );

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when suspect not found")
    void shouldReturn404WhenSuspectNotFound() {
      // Given
      Long nonExistentId = 999999L;
      HttpRequest<?> request = HttpRequest.GET("/suspects/" + nonExistentId)
          .bearerAuth(jwtToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, CaseSuspectDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
  }

  @Nested
  @DisplayName("POST /suspects/{id} - Обновление подозреваемого")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class UpdateCaseSuspectTests {

    private CaseSuspectEntity updateSuspect;

    @BeforeEach
    void setupUpdateSuspect() {
      // Создаем подозреваемого для UPDATE тестов
      updateSuspect = new CaseSuspectEntity();
      updateSuspect.setName("Original Name");
      updateSuspect.setSurname("Original Surname");
      updateSuspect.setPatronymic("Original Patronymic");
      updateSuspect.setIdentifier("PASSPORT-UPDATE-" + System.currentTimeMillis());
      updateSuspect.setStatus(CaseSuspectEntity.Status.SUSPECTED);
      updateSuspect.setCreator(testUser);
      updateSuspect.setUpdator(testUser);
      updateSuspect.setRelatedCase(testCase);
      updateSuspect.setCreatedAt(Instant.now());
      updateSuspect.setUpdatedAt(Instant.now());
      updateSuspect = caseSuspectRepository.save(updateSuspect);
    }

    @AfterEach
    void cleanupUpdateSuspect() {
      if (updateSuspect != null && updateSuspect.getId() != null) {
        try {
          caseSuspectRepository.deleteById(updateSuspect.getId());
        } catch (Exception e) {
          // ignore
        }
      }
    }

    @Test
    @Order(1)
    @DisplayName("Should successfully update suspect with valid data")
    void shouldUpdateSuspectSuccessfully() {
      // Given
      UpdateCaseSuspectRequest updateRequest = new UpdateCaseSuspectRequest(
          "Updated Name",
          "Updated Surname",
          "Updated Patronymic",
          "PASSPORT-UPDATED-" + System.currentTimeMillis(),
          CaseSuspectEntity.Status.ACCUSED,
          "Guilty",
          "Robbery"
      );

      HttpRequest<?> request = HttpRequest.POST("/suspects/" + updateSuspect.getId(), updateRequest)
          .bearerAuth(jwtToken);

      // When
      HttpResponse<CaseSuspectDto> response = client.toBlocking()
          .exchange(request, CaseSuspectDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertEquals("Updated Name", response.body().name());
      assertEquals("Updated Surname", response.body().surname());
      assertEquals("Updated Patronymic", response.body().patronymic());
      assertEquals(CaseSuspectEntity.Status.ACCUSED, response.body().status());
      assertEquals("Guilty", response.body().verdict());
      assertEquals("Robbery", response.body().accusation());

      // Verify in database
      CaseSuspectEntity verified = caseSuspectRepository.findById(updateSuspect.getId()).orElse(null);
      assertNotNull(verified);
      assertEquals("Updated Name", verified.getName());
      assertTrue(verified.getUpdatedAt().isAfter(updateSuspect.getCreatedAt()));
    }

    @Test
    @Order(2)
    @DisplayName("Should return 401 when no JWT token provided")
    void shouldReturn401WithoutToken() {
      // Given
      UpdateCaseSuspectRequest updateRequest = new UpdateCaseSuspectRequest(
          "Updated Name", "Updated Surname", null, null, null, null, null
      );
      HttpRequest<?> request = HttpRequest.POST("/suspects/" + updateSuspect.getId(), updateRequest);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, CaseSuspectDto.class)
      );

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Should return 404 when suspect not found")
    void shouldReturn404WhenSuspectNotFound() {
      // Given
      Long nonExistentId = 999999L;
      UpdateCaseSuspectRequest updateRequest = new UpdateCaseSuspectRequest(
          "Updated Name", "Updated Surname", null, null, null, null, null
      );
      HttpRequest<?> request = HttpRequest.POST("/suspects/" + nonExistentId, updateRequest)
          .bearerAuth(jwtToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, CaseSuspectDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @Order(4)
    @DisplayName("Should update only provided fields")
    void shouldUpdateOnlyProvidedFields() {
      // Given - Update only name
      UpdateCaseSuspectRequest updateRequest = new UpdateCaseSuspectRequest(
          "Only Name Changed", null, null, null, null, null, null
      );

      HttpRequest<?> request = HttpRequest.POST("/suspects/" + updateSuspect.getId(), updateRequest)
          .bearerAuth(jwtToken);

      // When
      HttpResponse<CaseSuspectDto> response = client.toBlocking()
          .exchange(request, CaseSuspectDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertEquals("Only Name Changed", response.body().name());
      // Other fields should remain unchanged
      assertEquals("Original Surname", response.body().surname());
    }
  }

  @Nested
  @DisplayName("DELETE /suspects/{id} - Удаление подозреваемого")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class DeleteCaseSuspectTests {

    private CaseSuspectEntity deleteSuspect;

    @BeforeEach
    void setupDeleteSuspect() {
      // Создаем подозреваемого для DELETE тестов
      deleteSuspect = new CaseSuspectEntity();
      deleteSuspect.setName("Delete Test Name");
      deleteSuspect.setSurname("Delete Test Surname");
      deleteSuspect.setPatronymic("Delete Test Patronymic");
      deleteSuspect.setIdentifier("PASSPORT-DELETE-" + System.currentTimeMillis());
      deleteSuspect.setStatus(CaseSuspectEntity.Status.SUSPECTED);
      deleteSuspect.setCreator(testUser);
      deleteSuspect.setUpdator(testUser);
      deleteSuspect.setRelatedCase(testCase);
      deleteSuspect.setCreatedAt(Instant.now());
      deleteSuspect.setUpdatedAt(Instant.now());
      deleteSuspect = caseSuspectRepository.save(deleteSuspect);
    }

    @Test
    @Order(1)
    @DisplayName("Should successfully delete suspect")
    void shouldDeleteSuspectSuccessfully() {
      // Given
      Long suspectId = deleteSuspect.getId();
      assertTrue(caseSuspectRepository.findById(suspectId).isPresent());

      HttpRequest<?> request = HttpRequest.DELETE("/suspects/" + suspectId)
          .bearerAuth(jwtToken);

      // When
      HttpResponse<?> response = client.toBlocking()
          .exchange(request);

      // Then
      assertEquals(HttpStatus.OK, response.status());

      // Verify deletion
      assertFalse(caseSuspectRepository.findById(suspectId).isPresent());
    }

    @Test
    @Order(2)
    @DisplayName("Should return 401 when no JWT token provided")
    void shouldReturn401WithoutToken() {
      // Given
      HttpRequest<?> request = HttpRequest.DELETE("/suspects/" + deleteSuspect.getId());

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request)
      );

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Should not throw error when deleting non-existent suspect")
    void shouldNotThrowErrorWhenDeletingNonExistent() {
      // Given
      Long nonExistentId = 999999L;
      HttpRequest<?> request = HttpRequest.DELETE("/suspects/" + nonExistentId)
          .bearerAuth(jwtToken);

      // When
      HttpResponse<?> response = client.toBlocking()
          .exchange(request);

      // Then
      assertEquals(HttpStatus.OK, response.status());
    }

    @Test
    @Order(4)
    @DisplayName("Should handle double delete gracefully")
    void shouldHandleDoubleDeleteGracefully() {
      // Given
      Long suspectId = deleteSuspect.getId();
      HttpRequest<?> request = HttpRequest.DELETE("/suspects/" + suspectId)
          .bearerAuth(jwtToken);

      // When - Delete first time
      HttpResponse<?> response1 = client.toBlocking()
          .exchange(request);
      assertEquals(HttpStatus.OK, response1.status());

      // When - Delete second time
      HttpResponse<?> response2 = client.toBlocking()
          .exchange(request);

      // Then - Should still return OK
      assertEquals(HttpStatus.OK, response2.status());
    }
  }

  @Nested
  @DisplayName("Data Integrity Tests")
  class DataIntegrityTests {

    @Test
    @DisplayName("Should maintain referential integrity with case")
    void shouldMaintainReferentialIntegrityWithCase() {
      // Given
      CaseSuspectEntity suspect = new CaseSuspectEntity();
      suspect.setName("Integrity Test");
      suspect.setSurname("Suspect");
      suspect.setPatronymic("Test");
      suspect.setIdentifier("PASSPORT-INTEGRITY-" + System.currentTimeMillis());
      suspect.setStatus(CaseSuspectEntity.Status.SUSPECTED);
      suspect.setCreator(testUser);
      suspect.setUpdator(testUser);
      suspect.setRelatedCase(testCase);
      suspect.setCreatedAt(Instant.now());
      suspect.setUpdatedAt(Instant.now());
      suspect = caseSuspectRepository.save(suspect);

      // When
      HttpRequest<?> request = HttpRequest.GET("/suspects/" + suspect.getId())
          .bearerAuth(jwtToken);
      HttpResponse<CaseSuspectDto> response = client.toBlocking()
          .exchange(request, CaseSuspectDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertEquals(testCase.getId(), response.body().relatedCaseId());

      // Cleanup
      caseSuspectRepository.deleteById(suspect.getId());
    }

    @Test
    @DisplayName("Should track creator and updator correctly")
    void shouldTrackCreatorAndUpdatorCorrectly() {
      // Given
      CaseSuspectEntity suspect = new CaseSuspectEntity();
      suspect.setName("Creator Test");
      suspect.setSurname("Suspect");
      suspect.setPatronymic("Test");
      suspect.setIdentifier("PASSPORT-CREATOR-" + System.currentTimeMillis());
      suspect.setStatus(CaseSuspectEntity.Status.SUSPECTED);
      suspect.setCreator(testUser);
      suspect.setUpdator(testUser);
      suspect.setRelatedCase(testCase);
      suspect.setCreatedAt(Instant.now());
      suspect.setUpdatedAt(Instant.now());
      suspect = caseSuspectRepository.save(suspect);

      // When
      UpdateCaseSuspectRequest updateRequest = new UpdateCaseSuspectRequest(
          "Updated Creator Test", null, null, null, null, null, null
      );
      HttpRequest<?> request = HttpRequest.POST("/suspects/" + suspect.getId(), updateRequest)
          .bearerAuth(jwtToken);
      HttpResponse<CaseSuspectDto> response = client.toBlocking()
          .exchange(request, CaseSuspectDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertEquals(testUser.getId(), response.body().creatorId());
      assertEquals(testUser.getId(), response.body().updatorId());

      // Cleanup
      caseSuspectRepository.deleteById(suspect.getId());
    }
  }
}
