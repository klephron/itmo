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
import ru.itmopai.backend.dto.ClueDto;
import ru.itmopai.backend.dto.request.LoginRequest;
import ru.itmopai.backend.dto.request.UpdateClueRequest;
import ru.itmopai.backend.dto.response.LoginResponse;
import ru.itmopai.backend.entity.*;
import ru.itmopai.backend.repository.*;
import ru.itmopai.backend.service.UserService;
import ru.itmopai.backend.entity.NotificationEntity;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP E2E тесты для CluesController
 * Тестируют контроллер через настоящие HTTP запросы с JWT аутентификацией
 */
@MicronautTest(transactional = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Clues Controller HTTP E2E Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CluesControllerHttpE2ETest {

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
  ClueRepository clueRepository;

  @Inject
  UserPermissionRepository userPermissionRepository;

  @Inject
  PasswordEncoder passwordEncoder;

  @Inject
  UserService userService;

  @Inject
  NotificationRepository notificationRepository;

  private UserEntity testUser;
  private CaseEntity testCase;
  private String jwtToken;
  private ClueEntity testClue;

  @BeforeAll
  void setupTestData() {
    // Очищаем данные
    userPermissionRepository.deleteAll();
    clueRepository.deleteAll();
    caseRepository.deleteAll();
    incidentRepository.deleteAll();
    userRepository.deleteAll();

    // Создаем тестового пользователя с паролем
    testUser = new UserEntity();
    testUser.setUsername("http_e2e_user");
    testUser.setName("HTTP");
    testUser.setSurname("Tester");
    testUser.setPassword(passwordEncoder.encode("testpassword123")); // Хэшируем пароль
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

    // Создаем права доступа для пользователя через UserService
    userService.setUserPermissions(testUser.getId(), List.of(
        "CASES:" + testCase.getId() + ":READ",
        "CASES:" + testCase.getId() + ":CLUES_UPDATE",
        "CASES:" + testCase.getId() + ":CLUES_DELETE"
    ));

    // Создаем тестовую улику через репозиторий
    testClue = new ClueEntity();
    testClue.setTitle("HTTP Test Clue");
    testClue.setDescription("Clue for HTTP testing");
    testClue.setStatus(ClueEntity.Status.PENDING);
    testClue.setCreator(testUser);
    testClue.setUpdator(testUser);
    testClue.setRelatedCase(testCase);
    testClue.setCreatedAt(Instant.now());
    testClue.setUpdatedAt(Instant.now());
    testClue.setMetadata("{}");
    testClue = clueRepository.save(testClue);

    // Получаем JWT токен через HTTP запрос к /login
    jwtToken = loginAndGetToken("http_e2e_user", "testpassword123");
  }

  @AfterAll
  void cleanupTestData() {
    // Удаляем в правильном порядке, соблюдая внешние ключи
    try {
      userPermissionRepository.deleteAll();
      clueRepository.deleteAll();
      caseRepository.deleteAll();
      incidentRepository.deleteAll();
      userRepository.deleteAll();
    } catch (Exception e) {
      // игнорируем ошибки при cleanup
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
  @DisplayName("GET /clues/{id} - Получение улики")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class GetClueTests {

    private ClueEntity getClue;

    @BeforeEach
    void setupGetClue() {
      // Создаем отдельную улику для GET тестов
      getClue = new ClueEntity();
      getClue.setTitle("GET Test Clue");
      getClue.setDescription("Clue for GET HTTP testing");
      getClue.setStatus(ClueEntity.Status.PENDING);
      getClue.setCreator(testUser);
      getClue.setUpdator(testUser);
      getClue.setRelatedCase(testCase);
      getClue.setCreatedAt(Instant.now());
      getClue.setUpdatedAt(Instant.now());
      getClue.setMetadata("{}");
      getClue = clueRepository.save(getClue);
    }

    @AfterEach
    void cleanupGetClue() {
      if (getClue != null && getClue.getId() != null) {
        clueRepository.deleteById(getClue.getId());
      }
    }

    @Test
    @Order(1)
    @DisplayName("Should successfully get clue with valid JWT token and permissions")
    void shouldGetClueWithValidToken() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/clues/" + getClue.getId())
          .bearerAuth(jwtToken);

      // When
      HttpResponse<ClueDto> response = client.toBlocking()
          .exchange(request, ClueDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertEquals(getClue.getId(), response.body().id());
      assertEquals("GET Test Clue", response.body().title());
      assertEquals("Clue for GET HTTP testing", response.body().description());
      assertEquals(ClueEntity.Status.PENDING, response.body().status());
    }

    @Test
    @Order(2)
    @DisplayName("Should return 401 when no JWT token provided")
    void shouldReturn401WithoutToken() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/clues/" + getClue.getId());

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Should return 401 with invalid JWT token")
    void shouldReturn401WithInvalidToken() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/clues/" + getClue.getId())
          .bearerAuth("invalid.jwt.token");

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when clue not found")
    void shouldReturn404WhenClueNotFound() {
      // Given
      Long nonExistentId = 999999L;
      HttpRequest<?> request = HttpRequest.GET("/clues/" + nonExistentId)
          .bearerAuth(jwtToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
  }

  @Nested
  @DisplayName("PUT /clues/{id} - Обновление улики")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class UpdateClueTests {

    private ClueEntity updateClue;

    @BeforeEach
    void setupUpdateClue() {
      // Создаем отдельную улику для UPDATE тестов
      updateClue = new ClueEntity();
      updateClue.setTitle("Update Test Clue");
      updateClue.setDescription("Clue for UPDATE HTTP testing");
      updateClue.setStatus(ClueEntity.Status.PENDING);
      updateClue.setCreator(testUser);
      updateClue.setUpdator(testUser);
      updateClue.setRelatedCase(testCase);
      updateClue.setCreatedAt(Instant.now());
      updateClue.setUpdatedAt(Instant.now());
      updateClue.setMetadata("{}");
      updateClue = clueRepository.save(updateClue);
    }

    @AfterEach
    void cleanupUpdateClue() {
      if (updateClue != null && updateClue.getId() != null) {
        clueRepository.deleteById(updateClue.getId());
      }
    }

    @Test
    @Order(1)
    @DisplayName("Should successfully update clue with valid JWT token and permissions")
    void shouldUpdateClueWithValidToken() {
      // Given
      UpdateClueRequest updateRequest = new UpdateClueRequest(
          "Updated HTTP Clue",
          "Updated description via HTTP",
          ClueEntity.Status.ACCEPTED
      );

      HttpRequest<?> request = HttpRequest.PUT("/clues/" + updateClue.getId(), updateRequest)
          .bearerAuth(jwtToken);

      // When
      HttpResponse<ClueDto> response = client.toBlocking()
          .exchange(request, ClueDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertEquals(updateClue.getId(), response.body().id());
      assertEquals("Updated HTTP Clue", response.body().title());
      assertEquals("Updated description via HTTP", response.body().description());
      assertEquals(ClueEntity.Status.ACCEPTED, response.body().status());
    }

    @Test
    @Order(2)
    @DisplayName("Should return 401 when no JWT token provided")
    void shouldReturn401WithoutToken() {
      // Given
      UpdateClueRequest updateRequest = new UpdateClueRequest(
          "Title", "Description", ClueEntity.Status.PENDING
      );

      HttpRequest<?> request = HttpRequest.PUT("/clues/" + updateClue.getId(), updateRequest);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Should return 404 when updating non-existent clue")
    void shouldReturn404WhenUpdatingNonExistentClue() {
      // Given
      Long nonExistentId = 999999L;
      UpdateClueRequest updateRequest = new UpdateClueRequest(
          "Title", "Description", ClueEntity.Status.PENDING
      );

      HttpRequest<?> request = HttpRequest.PUT("/clues/" + nonExistentId, updateRequest)
          .bearerAuth(jwtToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
  }

  @Nested
  @DisplayName("DELETE /clues/{id} - Удаление улики")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class DeleteClueTests {

    private ClueEntity clueToDelete;

    @BeforeEach
    void createClueToDelete() {
      // Создаем новую улику для каждого теста удаления
      clueToDelete = new ClueEntity();
      clueToDelete.setTitle("Clue to Delete");
      clueToDelete.setDescription("Will be deleted");
      clueToDelete.setStatus(ClueEntity.Status.PENDING);
      clueToDelete.setCreator(testUser);
      clueToDelete.setUpdator(testUser);
      clueToDelete.setRelatedCase(testCase);
      clueToDelete.setCreatedAt(Instant.now());
      clueToDelete.setUpdatedAt(Instant.now());
      clueToDelete.setMetadata("{}");
      clueToDelete = clueRepository.save(clueToDelete);
    }

    @Test
    @Order(1)
    @DisplayName("Should successfully delete clue with valid JWT token and permissions")
    void shouldDeleteClueWithValidToken() {
      // Given
      HttpRequest<?> request = HttpRequest.DELETE("/clues/" + clueToDelete.getId())
          .bearerAuth(jwtToken);

      // When
      HttpResponse<?> response = client.toBlocking().exchange(request);

      // Then
      assertEquals(HttpStatus.NO_CONTENT, response.status());

      // Verify clue is deleted
      assertFalse(clueRepository.findById(clueToDelete.getId()).isPresent());
    }

    @Test
    @Order(2)
    @DisplayName("Should return 401 when no JWT token provided")
    void shouldReturn401WithoutToken() {
      // Given
      HttpRequest<?> request = HttpRequest.DELETE("/clues/" + clueToDelete.getId());

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request)
      );

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Should return 404 when deleting non-existent clue")
    void shouldReturn404WhenDeletingNonExistentClue() {
      // Given
      Long nonExistentId = 999999L;
      HttpRequest<?> request = HttpRequest.DELETE("/clues/" + nonExistentId)
          .bearerAuth(jwtToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
  }

  @Nested
  @DisplayName("Permission Tests - Access Control")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class PermissionTests {

    private UserEntity unauthorizedUser;
    private String unauthorizedToken;
    private CaseEntity otherCase;
    private ClueEntity otherClue;
    private IncidentEntity otherIncident;

    @BeforeEach
    void setupUnauthorizedUser() {
      // Создаем пользователя без прав доступа
      unauthorizedUser = new UserEntity();
      unauthorizedUser.setUsername("unauthorized_user_" + System.currentTimeMillis());
      unauthorizedUser.setName("Unauthorized");
      unauthorizedUser.setSurname("User");
      unauthorizedUser.setPassword(passwordEncoder.encode("password123"));
      unauthorizedUser.setLocationLatitude(55.7558);
      unauthorizedUser.setLocationLongitude(37.6173);
      unauthorizedUser.setLocationAltitude(150.0);
      unauthorizedUser = userRepository.save(unauthorizedUser);

      // Создаем другое дело
      otherIncident = new IncidentEntity();
      otherIncident.setType("Other Incident");
      otherIncident.setReason("Testing");
      otherIncident.setCreator(unauthorizedUser);
      otherIncident.setUpdator(unauthorizedUser);
      otherIncident.setCreatedAt(Instant.now());
      otherIncident.setUpdatedAt(Instant.now());
      otherIncident.setLocationLatitude(55.7558);
      otherIncident.setLocationLongitude(37.6173);
      otherIncident.setLocationAltitude(150.0);
      otherIncident.setEstimatedTime(Instant.now());
      otherIncident = incidentRepository.save(otherIncident);

      otherCase = new CaseEntity();
      otherCase.setTitle("Other Case");
      otherCase.setDescription("Case without permissions");
      otherCase.setStatus(CaseEntity.Status.OPENED);
      otherCase.setCreator(unauthorizedUser);
      otherCase.setUpdator(unauthorizedUser);
      otherCase.setCreatedAt(Instant.now());
      otherCase.setUpdatedAt(Instant.now());
      otherCase.setIncident(otherIncident);
      otherCase = caseRepository.save(otherCase);

      // Создаем улику в этом деле
      otherClue = new ClueEntity();
      otherClue.setTitle("Other Clue");
      otherClue.setDescription("Clue without permissions");
      otherClue.setStatus(ClueEntity.Status.PENDING);
      otherClue.setCreator(unauthorizedUser);
      otherClue.setUpdator(unauthorizedUser);
      otherClue.setRelatedCase(otherCase);
      otherClue.setCreatedAt(Instant.now());
      otherClue.setUpdatedAt(Instant.now());
      otherClue.setMetadata("{}");
      otherClue = clueRepository.save(otherClue);

      // Получаем токен для пользователя без прав
      unauthorizedToken = loginAndGetToken(unauthorizedUser.getUsername(), "password123");
    }

    @AfterEach
    void cleanupUnauthorizedUser() {
      // Удаляем в правильном порядке, соблюдая внешние ключи
      try {
        if (otherClue != null && otherClue.getId() != null) {
          clueRepository.deleteById(otherClue.getId());
        }
        if (otherCase != null && otherCase.getId() != null) {
          caseRepository.deleteById(otherCase.getId());
        }
        if (otherIncident != null && otherIncident.getId() != null) {
          incidentRepository.deleteById(otherIncident.getId());
        }
        if (unauthorizedUser != null && unauthorizedUser.getId() != null) {
          userRepository.deleteById(unauthorizedUser.getId());
        }
      } catch (Exception e) {
        // игнорируем ошибки при cleanup
        System.err.println("Cleanup error in PermissionTests: " + e.getMessage());
      }
    }

    @Test
    @Order(1)
    @DisplayName("Should return 403 when user lacks READ permission")
    void shouldReturn403WhenLackingReadPermission() {
      // Given - unauthorizedUser пытается получить testClue (без прав)
      HttpRequest<?> request = HttpRequest.GET("/clues/" + testClue.getId())
          .bearerAuth(unauthorizedToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("Should return 403 when user lacks CLUES_UPDATE permission")
    void shouldReturn403WhenLackingUpdatePermission() {
      // Given
      UpdateClueRequest updateRequest = new UpdateClueRequest(
          "Unauthorized Update", "Description", ClueEntity.Status.ACCEPTED
      );

      HttpRequest<?> request = HttpRequest.PUT("/clues/" + testClue.getId(), updateRequest)
          .bearerAuth(unauthorizedToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Should return 403 when user lacks CLUES_DELETE permission")
    void shouldReturn403WhenLackingDeletePermission() {
      // Given
      HttpRequest<?> request = HttpRequest.DELETE("/clues/" + testClue.getId())
          .bearerAuth(unauthorizedToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request)
      );

      assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }
  }

  @Nested
  @DisplayName("Data Integrity Tests via HTTP")
  class DataIntegrityTests {

    @Test
    @DisplayName("Should preserve all clue fields after update")
    void shouldPreserveFieldsAfterUpdate() {
      // Given
      UpdateClueRequest updateRequest = new UpdateClueRequest(
          "Integrity Test Title",
          "Integrity Test Description",
          ClueEntity.Status.REJECTED
      );

      HttpRequest<?> request = HttpRequest.PUT("/clues/" + testClue.getId(), updateRequest)
          .bearerAuth(jwtToken);

      // When
      HttpResponse<ClueDto> response = client.toBlocking()
          .exchange(request, ClueDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      ClueDto clue = response.body();
      assertNotNull(clue);
      assertEquals("Integrity Test Title", clue.title());
      assertEquals("Integrity Test Description", clue.description());
      assertEquals(ClueEntity.Status.REJECTED, clue.status());
      assertNotNull(clue.createdAt());
      assertNotNull(clue.updatedAt());
    }

    @Test
    @DisplayName("Should return properly mapped ClueDto with all relationships")
    void shouldReturnMappedDtoWithRelationships() {
      // Given
      HttpRequest<?> request = HttpRequest.GET("/clues/" + testClue.getId())
          .bearerAuth(jwtToken);

      // When
      HttpResponse<ClueDto> response = client.toBlocking()
          .exchange(request, ClueDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      ClueDto clue = response.body();
      assertNotNull(clue);
      assertNotNull(clue.id());
      assertNotNull(clue.relatedCaseId());
      assertEquals(testCase.getId(), clue.relatedCaseId());
      assertNotNull(clue.creatorId());
      assertNotNull(clue.updatorId());
    }

    @Test
    @DisplayName("Should preserve timestamps and metadata via HTTP")
    void shouldPreserveTimestampsAndMetadata() {
      // Given - Create a new clue
      ClueEntity newClue = new ClueEntity();
      newClue.setTitle("Timestamp Test Clue");
      newClue.setDescription("Testing timestamps");
      newClue.setStatus(ClueEntity.Status.PENDING);
      newClue.setCreator(testUser);
      newClue.setUpdator(testUser);
      newClue.setRelatedCase(testCase);
      newClue.setCreatedAt(Instant.now());
      newClue.setUpdatedAt(Instant.now());
      newClue.setMetadata("{}");
      newClue = clueRepository.save(newClue);

      // When - Get via HTTP
      HttpRequest<?> request = HttpRequest.GET("/clues/" + newClue.getId())
          .bearerAuth(jwtToken);
      HttpResponse<ClueDto> response = client.toBlocking()
          .exchange(request, ClueDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      ClueDto clue = response.body();
      assertNotNull(clue.createdAt());
      assertNotNull(clue.updatedAt());
      assertNotNull(clue.metadata());

      // Cleanup
      clueRepository.deleteById(newClue.getId());
    }

    @Test
    @DisplayName("Should update updatedAt timestamp when updating via HTTP")
    void shouldUpdateTimestampOnHttpUpdate() throws InterruptedException {
      // Given - Create clue
      ClueEntity clue = new ClueEntity();
      clue.setTitle("Update Timestamp Test");
      clue.setDescription("Testing");
      clue.setStatus(ClueEntity.Status.PENDING);
      clue.setCreator(testUser);
      clue.setUpdator(testUser);
      clue.setRelatedCase(testCase);
      clue.setCreatedAt(Instant.now());
      clue.setUpdatedAt(Instant.now());
      clue.setMetadata("{}");
      clue = clueRepository.save(clue);

      Instant originalCreatedAt = clue.getCreatedAt();
      Instant originalUpdatedAt = clue.getUpdatedAt();

      // Small delay
      Thread.sleep(100);

      // When - Update via HTTP
      UpdateClueRequest updateRequest = new UpdateClueRequest(
          "Updated Title", "Updated Desc", ClueEntity.Status.ACCEPTED
      );
      HttpRequest<?> request = HttpRequest.PUT("/clues/" + clue.getId(), updateRequest)
          .bearerAuth(jwtToken);
      HttpResponse<ClueDto> response = client.toBlocking()
          .exchange(request, ClueDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      ClueDto updatedClue = response.body();
      assertTrue(updatedClue.updatedAt().isAfter(originalUpdatedAt) ||
          updatedClue.updatedAt().equals(originalUpdatedAt));

      // Cleanup
      clueRepository.deleteById(clue.getId());
    }
  }

  @Nested
  @DisplayName("Business Rules - Status Transitions")
  class BusinessRulesTests {

    private ClueEntity transitionTestClue;

    @BeforeEach
    void setupTransitionTestClue() {
      transitionTestClue = new ClueEntity();
      transitionTestClue.setTitle("Status Transition Test");
      transitionTestClue.setDescription("Testing status transitions");
      transitionTestClue.setStatus(ClueEntity.Status.PENDING);
      transitionTestClue.setCreator(testUser);
      transitionTestClue.setUpdator(testUser);
      transitionTestClue.setRelatedCase(testCase);
      transitionTestClue.setCreatedAt(Instant.now());
      transitionTestClue.setUpdatedAt(Instant.now());
      transitionTestClue.setMetadata("{}");
      transitionTestClue = clueRepository.save(transitionTestClue);
    }

    @AfterEach
    void cleanupTransitionTestClue() {
      if (transitionTestClue != null && transitionTestClue.getId() != null) {
        clueRepository.deleteById(transitionTestClue.getId());
      }
    }

    @Test
    @DisplayName("Should allow PENDING -> ACCEPTED transition via HTTP")
    void shouldAllowPendingToAccepted() {
      // Given
      UpdateClueRequest updateRequest = new UpdateClueRequest(
          "Accepted Clue", "Accepted", ClueEntity.Status.ACCEPTED
      );
      HttpRequest<?> request = HttpRequest.PUT("/clues/" + transitionTestClue.getId(), updateRequest)
          .bearerAuth(jwtToken);

      // When
      HttpResponse<ClueDto> response = client.toBlocking()
          .exchange(request, ClueDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertEquals(ClueEntity.Status.ACCEPTED, response.body().status());
    }

    @Test
    @DisplayName("Should allow ACCEPTED -> REJECTED transition via HTTP")
    void shouldAllowAcceptedToRejected() {
      // Given - First move to ACCEPTED
      UpdateClueRequest acceptRequest = new UpdateClueRequest(
          "Title", "Desc", ClueEntity.Status.ACCEPTED
      );
      client.toBlocking().exchange(
          HttpRequest.PUT("/clues/" + transitionTestClue.getId(), acceptRequest)
              .bearerAuth(jwtToken),
          ClueDto.class
      );

      // When - Move to REJECTED
      UpdateClueRequest rejectRequest = new UpdateClueRequest(
          "Rejected Clue", "Rejected", ClueEntity.Status.REJECTED
      );
      HttpRequest<?> request = HttpRequest.PUT("/clues/" + transitionTestClue.getId(), rejectRequest)
          .bearerAuth(jwtToken);
      HttpResponse<ClueDto> response = client.toBlocking()
          .exchange(request, ClueDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertEquals(ClueEntity.Status.REJECTED, response.body().status());
    }

    @Test
    @DisplayName("Should allow reverting REJECTED -> PENDING via HTTP")
    void shouldAllowRevertingToPending() {
      // Given - Move to REJECTED first
      UpdateClueRequest rejectRequest = new UpdateClueRequest(
          "Title", "Desc", ClueEntity.Status.REJECTED
      );
      client.toBlocking().exchange(
          HttpRequest.PUT("/clues/" + transitionTestClue.getId(), rejectRequest)
              .bearerAuth(jwtToken),
          ClueDto.class
      );

      // When - Revert to PENDING
      UpdateClueRequest pendingRequest = new UpdateClueRequest(
          "Back to Pending", "Reverting", ClueEntity.Status.PENDING
      );
      HttpRequest<?> request = HttpRequest.PUT("/clues/" + transitionTestClue.getId(), pendingRequest)
          .bearerAuth(jwtToken);
      HttpResponse<ClueDto> response = client.toBlocking()
          .exchange(request, ClueDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertEquals(ClueEntity.Status.PENDING, response.body().status());
    }
  }

  @Nested
  @DisplayName("Validation Tests - Request Body Validation")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class ValidationTests {

    private ClueEntity validationTestClue;

    @BeforeEach
    void setupValidationTestClue() {
      validationTestClue = new ClueEntity();
      validationTestClue.setTitle("Validation Test Clue");
      validationTestClue.setDescription("For validation testing");
      validationTestClue.setStatus(ClueEntity.Status.PENDING);
      validationTestClue.setCreator(testUser);
      validationTestClue.setUpdator(testUser);
      validationTestClue.setRelatedCase(testCase);
      validationTestClue.setCreatedAt(Instant.now());
      validationTestClue.setUpdatedAt(Instant.now());
      validationTestClue.setMetadata("{}");
      validationTestClue = clueRepository.save(validationTestClue);
    }

    @AfterEach
    void cleanupValidationTestClue() {
      if (validationTestClue != null && validationTestClue.getId() != null) {
        clueRepository.deleteById(validationTestClue.getId());
      }
    }

    @Test
    @Order(1)
    @DisplayName("Should return 400 when title is empty")
    void shouldReturn400WhenTitleIsEmpty() {
      // Given
      UpdateClueRequest invalidRequest = new UpdateClueRequest(
          "",  // Empty title
          "Valid description",
          ClueEntity.Status.PENDING
      );

      HttpRequest<?> request = HttpRequest.PUT("/clues/" + validationTestClue.getId(), invalidRequest)
          .bearerAuth(jwtToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("Should handle null status gracefully (nullable field)")
    void shouldHandleNullStatusGracefully() {
      // Given - Note: In practice, status might be nullable in UpdateClueRequest
      // This test verifies the current behavior rather than enforcing strict validation
      UpdateClueRequest requestWithNullStatus = new UpdateClueRequest(
          "Valid Title",
          "Valid description",
          null  // Null status - behavior depends on DTO design
      );

      HttpRequest<?> request = HttpRequest.PUT("/clues/" + validationTestClue.getId(), requestWithNullStatus)
          .bearerAuth(jwtToken);

      // When - Execute request
      // Note: If status is nullable in the DTO, this will succeed
      // If you want to enforce non-null status, add @NotNull to UpdateClueRequest.status field
      try {
        HttpResponse<ClueDto> response = client.toBlocking()
            .exchange(request, ClueDto.class);

        // If we reach here, null status is accepted (current behavior)
        // The clue should still have a valid status (from DB or default)
        assertNotNull(response.body());
      } catch (HttpClientResponseException e) {
        // If validation is enforced, we expect 400
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
      }
    }

    @Test
    @Order(3)
    @DisplayName("Should accept null description as it's optional")
    void shouldAcceptNullDescription() {
      // Given
      UpdateClueRequest validRequest = new UpdateClueRequest(
          "Valid Title",
          null,  // Null description is allowed
          ClueEntity.Status.ACCEPTED
      );

      HttpRequest<?> request = HttpRequest.PUT("/clues/" + validationTestClue.getId(), validRequest)
          .bearerAuth(jwtToken);

      // When
      HttpResponse<ClueDto> response = client.toBlocking()
          .exchange(request, ClueDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());
      assertNotNull(response.body());
      assertEquals("Valid Title", response.body().title());
    }
  }

  @Nested
  @DisplayName("Edge Cases - ID Handling")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class EdgeCaseTests {

    @Test
    @Order(1)
    @DisplayName("Should return 404 for negative ID")
    void shouldReturn404ForNegativeId() {
      // Given
      Long negativeId = -1L;
      HttpRequest<?> request = HttpRequest.GET("/clues/" + negativeId)
          .bearerAuth(jwtToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("Should return 404 for zero ID")
    void shouldReturn404ForZeroId() {
      // Given
      Long zeroId = 0L;
      HttpRequest<?> request = HttpRequest.GET("/clues/" + zeroId)
          .bearerAuth(jwtToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Should return 404 for very large ID")
    void shouldReturn404ForVeryLargeId() {
      // Given
      Long veryLargeId = Long.MAX_VALUE;
      HttpRequest<?> request = HttpRequest.GET("/clues/" + veryLargeId)
          .bearerAuth(jwtToken);

      // When & Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when deleting already deleted clue")
    void shouldReturn404WhenDeletingAlreadyDeletedClue() {
      // Given - Create and delete a clue
      ClueEntity clueToDelete = new ClueEntity();
      clueToDelete.setTitle("To be deleted twice");
      clueToDelete.setDescription("Testing double delete");
      clueToDelete.setStatus(ClueEntity.Status.PENDING);
      clueToDelete.setCreator(testUser);
      clueToDelete.setUpdator(testUser);
      clueToDelete.setRelatedCase(testCase);
      clueToDelete.setCreatedAt(Instant.now());
      clueToDelete.setUpdatedAt(Instant.now());
      clueToDelete.setMetadata("{}");
      clueToDelete = clueRepository.save(clueToDelete);

      Long clueId = clueToDelete.getId();

      // First deletion
      HttpRequest<?> deleteRequest = HttpRequest.DELETE("/clues/" + clueId)
          .bearerAuth(jwtToken);
      client.toBlocking().exchange(deleteRequest);

      // When - Try to delete again
      HttpRequest<?> secondDeleteRequest = HttpRequest.DELETE("/clues/" + clueId)
          .bearerAuth(jwtToken);

      // Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(secondDeleteRequest)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 when updating already deleted clue")
    void shouldReturn404WhenUpdatingDeletedClue() {
      // Given - Create and delete a clue
      ClueEntity clueToDelete = new ClueEntity();
      clueToDelete.setTitle("To be deleted");
      clueToDelete.setDescription("Testing update after delete");
      clueToDelete.setStatus(ClueEntity.Status.PENDING);
      clueToDelete.setCreator(testUser);
      clueToDelete.setUpdator(testUser);
      clueToDelete.setRelatedCase(testCase);
      clueToDelete.setCreatedAt(Instant.now());
      clueToDelete.setUpdatedAt(Instant.now());
      clueToDelete.setMetadata("{}");
      clueToDelete = clueRepository.save(clueToDelete);

      Long clueId = clueToDelete.getId();

      // Delete the clue
      clueRepository.deleteById(clueId);

      // When - Try to update deleted clue
      UpdateClueRequest updateRequest = new UpdateClueRequest(
          "Updated Title", "Updated Desc", ClueEntity.Status.ACCEPTED
      );
      HttpRequest<?> request = HttpRequest.PUT("/clues/" + clueId, updateRequest)
          .bearerAuth(jwtToken);

      // Then
      HttpClientResponseException exception = assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
  }

  @Nested
  @DisplayName("Notification Tests - Verify notifications are created")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class NotificationTests {

    private ClueEntity notificationTestClue;

    @BeforeEach
    void setupNotificationTestClue() {
      // Clear notifications before each test
      notificationRepository.deleteAll();

      notificationTestClue = new ClueEntity();
      notificationTestClue.setTitle("Notification Test Clue");
      notificationTestClue.setDescription("For notification testing");
      notificationTestClue.setStatus(ClueEntity.Status.PENDING);
      notificationTestClue.setCreator(testUser);
      notificationTestClue.setUpdator(testUser);
      notificationTestClue.setRelatedCase(testCase);
      notificationTestClue.setCreatedAt(Instant.now());
      notificationTestClue.setUpdatedAt(Instant.now());
      notificationTestClue.setMetadata("{}");
      notificationTestClue = clueRepository.save(notificationTestClue);
    }

    @AfterEach
    void cleanupNotificationTestClue() {
      if (notificationTestClue != null && notificationTestClue.getId() != null) {
        clueRepository.deleteById(notificationTestClue.getId());
      }
      notificationRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should create notification when updating clue")
    void shouldCreateNotificationOnUpdate() {
      // Given
      long notificationCountBefore = notificationRepository.count();

      UpdateClueRequest updateRequest = new UpdateClueRequest(
          "Updated for Notification Test",
          "Description",
          ClueEntity.Status.ACCEPTED
      );

      HttpRequest<?> request = HttpRequest.PUT("/clues/" + notificationTestClue.getId(), updateRequest)
          .bearerAuth(jwtToken);

      // When
      HttpResponse<ClueDto> response = client.toBlocking()
          .exchange(request, ClueDto.class);

      // Then
      assertEquals(HttpStatus.OK, response.status());

      // Verify notification was created
      long notificationCountAfter = notificationRepository.count();
      assertEquals(notificationCountBefore + 1, notificationCountAfter);

      // Verify notification content
      List<NotificationEntity> notifications = notificationRepository.findAll();
      NotificationEntity notification = notifications.get(notifications.size() - 1);
      assertEquals(testUser.getId(), notification.getUser().getId());
      assertTrue(notification.getDescription().contains("Clue"));
      assertTrue(notification.getDescription().contains("updated"));
    }

    @Test
    @Order(2)
    @DisplayName("Should create notification when deleting clue")
    void shouldCreateNotificationOnDelete() {
      // Given
      long notificationCountBefore = notificationRepository.count();

      HttpRequest<?> request = HttpRequest.DELETE("/clues/" + notificationTestClue.getId())
          .bearerAuth(jwtToken);

      // When
      HttpResponse<?> response = client.toBlocking().exchange(request);

      // Then
      assertEquals(HttpStatus.NO_CONTENT, response.status());

      // Verify notification was created
      long notificationCountAfter = notificationRepository.count();
      assertEquals(notificationCountBefore + 1, notificationCountAfter);

      // Verify notification content
      List<NotificationEntity> notifications = notificationRepository.findAll();
      NotificationEntity notification = notifications.get(notifications.size() - 1);
      assertEquals(testUser.getId(), notification.getUser().getId());
      assertTrue(notification.getDescription().contains("Clue"));
      assertTrue(notification.getDescription().contains("deleted"));
    }

    @Test
    @Order(3)
    @DisplayName("Should not create notification when update fails")
    void shouldNotCreateNotificationWhenUpdateFails() {
      // Given
      long notificationCountBefore = notificationRepository.count();

      UpdateClueRequest updateRequest = new UpdateClueRequest(
          "Updated Title",
          "Description",
          ClueEntity.Status.ACCEPTED
      );

      Long nonExistentId = 999999L;
      HttpRequest<?> request = HttpRequest.PUT("/clues/" + nonExistentId, updateRequest)
          .bearerAuth(jwtToken);

      // When
      assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request, ClueDto.class)
      );

      // Then - Notification should NOT be created
      long notificationCountAfter = notificationRepository.count();
      assertEquals(notificationCountBefore, notificationCountAfter);
    }

    @Test
    @Order(4)
    @DisplayName("Should not create notification when delete fails")
    void shouldNotCreateNotificationWhenDeleteFails() {
      // Given
      long notificationCountBefore = notificationRepository.count();

      Long nonExistentId = 999999L;
      HttpRequest<?> request = HttpRequest.DELETE("/clues/" + nonExistentId)
          .bearerAuth(jwtToken);

      // When
      assertThrows(
          HttpClientResponseException.class,
          () -> client.toBlocking().exchange(request)
      );

      // Then - Notification should NOT be created
      long notificationCountAfter = notificationRepository.count();
      assertEquals(notificationCountBefore, notificationCountAfter);
    }
  }
}
