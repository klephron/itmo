package ru.itmopai.backend.service;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.itmopai.backend.entity.*;
import ru.itmopai.backend.repository.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("CaseService Integration Tests")
class CaseServiceIntegrationTest implements TestPropertyProvider {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  @Inject
  CaseService caseService;

  @Inject
  CaseRepository caseRepository;

  @Inject
  UserRepository userRepository;

  @Inject
  IncidentRepository incidentRepository;

  @Inject
  UserCaseRepository userCaseRepository;

  private UserEntity testUser;
  private UserEntity testUser2;
  private IncidentEntity testIncident;

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
    // Clean up - must clear bidirectional references first
    userCaseRepository.deleteAll();

    // Clear incident->case references to allow case deletion
    incidentRepository.findAll().forEach(incident -> {
      incident.setRelatedCase(null);
      incidentRepository.update(incident);
    });

    caseRepository.deleteAll();
    incidentRepository.deleteAll();
    userRepository.deleteAll();

    // Create test users
    testUser = new UserEntity();
    testUser.setUsername("case_test_user1");
    testUser.setName("Case");
    testUser.setSurname("Tester1");
    testUser.setPassword("password");
    testUser.setLocationLatitude(0.0);
    testUser.setLocationLongitude(0.0);
    testUser.setLocationAltitude(0.0);
    testUser = userRepository.save(testUser);

    testUser2 = new UserEntity();
    testUser2.setUsername("case_test_user2");
    testUser2.setName("Case");
    testUser2.setSurname("Tester2");
    testUser2.setPassword("password");
    testUser2.setLocationLatitude(0.0);
    testUser2.setLocationLongitude(0.0);
    testUser2.setLocationAltitude(0.0);
    testUser2 = userRepository.save(testUser2);

    // Create test incident
    testIncident = new IncidentEntity();
    testIncident.setType("Test Incident Type");
    testIncident.setReason("Test Reason for Case");
    testIncident.setCreator(testUser);
    testIncident.setUpdator(testUser);
    testIncident.setCreatedAt(Instant.now());
    testIncident.setUpdatedAt(Instant.now());
    testIncident.setEstimatedTime(Instant.now());
    testIncident.setLocationLatitude(0.0);
    testIncident.setLocationLongitude(0.0);
    testIncident.setLocationAltitude(0.0);
    testIncident = incidentRepository.save(testIncident);
  }

  @AfterEach
  void tearDown() {
    userCaseRepository.deleteAll();

    // Clear incident->case references to allow case deletion
    incidentRepository.findAll().forEach(incident -> {
      incident.setRelatedCase(null);
      incidentRepository.update(incident);
    });

    caseRepository.deleteAll();
    incidentRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Nested
  @DisplayName("addCase() integration tests")
  class AddCaseIntegrationTests {

    @Test
    @DisplayName("Should persist case to database with users")
    void shouldPersistCaseWithUsers() {
      // Given
      String title = "New Case";
      String description = "Case Description";
      HashMap<Long, String> users = new HashMap<>();
      users.put(testUser.getId(), "Lead Investigator");
      users.put(testUser2.getId(), "Assistant");

      // When
      CaseEntity savedCase = caseService.addCase(testUser.getId(), title, description, testIncident.getId(), users);

      // Then
      assertNotNull(savedCase.getId());

      // Verify in database
      CaseEntity foundCase = caseRepository.findById(savedCase.getId()).orElseThrow();
      assertEquals(title, foundCase.getTitle());
      assertEquals(description, foundCase.getDescription());
      assertEquals(CaseEntity.Status.OPENED, foundCase.getStatus());
      assertEquals(testUser.getId(), foundCase.getCreator().getId());
      assertEquals(testIncident.getId(), foundCase.getIncident().getId());

      // Verify UserCase entities created
      List<UserCaseEntity> userCases = userCaseRepository.findAllById_RelatedCaseId(savedCase.getId());
      assertEquals(2, userCases.size());
    }

    @Test
    @DisplayName("Should create UserCase with correct positions")
    void shouldCreateUserCaseWithPositions() {
      // Given
      HashMap<Long, String> users = new HashMap<>();
      users.put(testUser.getId(), "Senior Detective");

      // When
      CaseEntity savedCase = caseService.addCase(testUser.getId(), "Case", "Desc", testIncident.getId(), users);

      // Then
      List<UserCaseEntity> userCases = userCaseRepository.findAllById_RelatedCaseId(savedCase.getId());
      assertEquals(1, userCases.size());

      UserCaseEntity userCase = userCases.get(0);
      assertEquals("Senior Detective", userCase.getPosition());
      assertEquals(UserCaseEntity.Status.WORKING, userCase.getStatus());
      assertEquals(testUser.getId(), userCase.getId().getUser().getId());
    }

    @Test
    @DisplayName("Should link incident to case")
    void shouldLinkIncidentToCase() {
      // Given
      HashMap<Long, String> users = new HashMap<>();

      // When
      CaseEntity savedCase = caseService.addCase(testUser.getId(), "Case", "Desc", testIncident.getId(), users);

      // Then
      IncidentEntity updatedIncident = incidentRepository.findById(testIncident.getId()).orElseThrow();
      assertNotNull(updatedIncident.getRelatedCase());
      assertEquals(savedCase.getId(), updatedIncident.getRelatedCase().getId());
    }

    @Test
    @DisplayName("Should throw exception for non-existent users")
    void shouldThrowExceptionForNonExistentUsers() {
      // Given
      HashMap<Long, String> users = new HashMap<>();
      users.put(999L, "Position"); // Non-existent user

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        caseService.addCase(testUser.getId(), "Case", "Desc", testIncident.getId(), users);
      });

      assertTrue(exception.getMessage().contains("Some of users not found"));
      assertTrue(exception.getMessage().contains("999"));
    }

    @Test
    @DisplayName("Should set createdAt and updatedAt timestamps")
    void shouldSetTimestamps() {
      // Given
      HashMap<Long, String> users = new HashMap<>();
      Instant before = Instant.now().minusSeconds(1);

      // When
      CaseEntity savedCase = caseService.addCase(testUser.getId(), "Case", "Desc", testIncident.getId(), users);

      // Then
      Instant after = Instant.now().plusSeconds(1);
      CaseEntity foundCase = caseRepository.findById(savedCase.getId()).orElseThrow();
      assertTrue(foundCase.getCreatedAt().isAfter(before));
      assertTrue(foundCase.getCreatedAt().isBefore(after));
      assertEquals(foundCase.getCreatedAt(), foundCase.getUpdatedAt());
    }
  }

  @Nested
  @DisplayName("updateCase() integration tests")
  class UpdateCaseIntegrationTests {

    @Test
    @DisplayName("Should update case in database")
    void shouldUpdateCaseInDB() {
      // Given
      HashMap<Long, String> initialUsers = new HashMap<>();
      initialUsers.put(testUser.getId(), "Initial Position");

      CaseEntity originalCase = caseService.addCase(testUser.getId(), "Original Title", "Original Desc", testIncident.getId(), initialUsers);

      String newTitle = "Updated Title";
      String newDescription = "Updated Description";
      CaseEntity.Status newStatus = CaseEntity.Status.CLOSED;
      HashMap<Long, String> newUsers = new HashMap<>();
      newUsers.put(testUser2.getId(), "New Position");

      // When - Note: updateCase expects incident ID, not case ID (potential bug in CaseService)
      CaseEntity updatedCase = caseService.updateCase(testUser2.getId(), testIncident.getId(), newTitle, newDescription, newStatus, newUsers);

      // Then
      CaseEntity foundCase = caseRepository.findById(updatedCase.getId()).orElseThrow();
      assertEquals(newTitle, foundCase.getTitle());
      assertEquals(newDescription, foundCase.getDescription());
      assertEquals(newStatus, foundCase.getStatus());
      assertEquals(testUser2.getId(), foundCase.getUpdator().getId());
    }

    @Test
    @DisplayName("Should replace UserCase entities on update")
    void shouldReplaceUserCaseEntities() {
      // Given
      HashMap<Long, String> initialUsers = new HashMap<>();
      initialUsers.put(testUser.getId(), "Old Position");

      CaseEntity originalCase = caseService.addCase(testUser.getId(), "Case", "Desc", testIncident.getId(), initialUsers);

      HashMap<Long, String> newUsers = new HashMap<>();
      newUsers.put(testUser2.getId(), "New Position");

      // When - Note: updateCase expects incident ID, not case ID
      caseService.updateCase(testUser.getId(), testIncident.getId(), "Updated", "Updated", CaseEntity.Status.OPENED, newUsers);

      // Then
      List<UserCaseEntity> userCases = userCaseRepository.findAllById_RelatedCaseId(originalCase.getId());
      assertEquals(1, userCases.size());
      assertEquals(testUser2.getId(), userCases.get(0).getId().getUser().getId());
      assertEquals("New Position", userCases.get(0).getPosition());
    }

    @Test
    @DisplayName("Should update updatedAt timestamp")
    void shouldUpdateTimestamp() throws InterruptedException {
      // Given
      HashMap<Long, String> users = new HashMap<>();
      CaseEntity originalCase = caseService.addCase(testUser.getId(), "Case", "Desc", testIncident.getId(), users);

      Instant originalUpdatedAt = originalCase.getUpdatedAt();
      Thread.sleep(10); // Small delay to ensure timestamp difference

      // When - Note: updateCase expects incident ID
      caseService.updateCase(testUser.getId(), testIncident.getId(), "Updated", "Updated", CaseEntity.Status.OPENED, users);

      // Then
      CaseEntity updatedCase = caseRepository.findById(originalCase.getId()).orElseThrow();
      assertTrue(updatedCase.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    @DisplayName("Should update updator but keep original creator")
    void shouldUpdateUpdatorKeepCreator() {
      // Given
      HashMap<Long, String> users = new HashMap<>();
      CaseEntity originalCase = caseService.addCase(testUser.getId(), "Case", "Desc", testIncident.getId(), users);

      // When - Note: updateCase expects incident ID
      caseService.updateCase(testUser2.getId(), testIncident.getId(), "Updated", "Updated", CaseEntity.Status.OPENED, users);

      // Then
      CaseEntity updatedCase = caseRepository.findById(originalCase.getId()).orElseThrow();
      assertEquals(testUser.getId(), updatedCase.getCreator().getId()); // Creator unchanged
      assertEquals(testUser2.getId(), updatedCase.getUpdator().getId()); // Updator changed
    }
  }

  @Nested
  @DisplayName("getCaseById() integration tests")
  class GetCaseByIdIntegrationTests {

    @Test
    @DisplayName("Should retrieve case from database")
    void shouldRetrieveCaseFromDB() {
      // Given
      HashMap<Long, String> users = new HashMap<>();
      CaseEntity savedCase = caseService.addCase(testUser.getId(), "Case", "Desc", testIncident.getId(), users);

      // When
      CaseEntity foundCase = caseService.getCaseById(savedCase.getId());

      // Then
      assertNotNull(foundCase);
      assertEquals(savedCase.getId(), foundCase.getId());
      assertEquals("Case", foundCase.getTitle());
    }

    @Test
    @DisplayName("Should throw exception for non-existent case")
    void shouldThrowExceptionForNonExistentCase() {
      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        caseService.getCaseById(999L);
      });
    }
  }

  @Nested
  @DisplayName("getAllCasesByUserId() integration tests")
  class GetAllCasesByUserIdIntegrationTests {

    @Test
    @DisplayName("Should retrieve all cases created by user")
    void shouldRetrieveAllCasesByUser() {
      // Given
      HashMap<Long, String> users = new HashMap<>();
      caseService.addCase(testUser.getId(), "Case 1", "Desc", testIncident.getId(), users);

      // Create another incident for second case
      IncidentEntity incident2 = new IncidentEntity();
      incident2.setType("Incident Type 2");
      incident2.setReason("Reason 2");
      incident2.setCreator(testUser);
      incident2.setUpdator(testUser);
      incident2.setCreatedAt(Instant.now());
      incident2.setUpdatedAt(Instant.now());
      incident2.setEstimatedTime(Instant.now());
      incident2.setLocationLatitude(0.0);
      incident2.setLocationLongitude(0.0);
      incident2.setLocationAltitude(0.0);
      incident2 = incidentRepository.save(incident2);

      caseService.addCase(testUser.getId(), "Case 2", "Desc", incident2.getId(), users);

      // When
      List<CaseEntity> cases = caseService.getAllCasesByUserId(testUser.getId());

      // Then
      assertEquals(2, cases.size());
      assertTrue(cases.stream().anyMatch(c -> c.getTitle().equals("Case 1")));
      assertTrue(cases.stream().anyMatch(c -> c.getTitle().equals("Case 2")));
    }

    @Test
    @DisplayName("Should return empty list when user has no cases")
    void shouldReturnEmptyListWhenNoCases() {
      // When
      List<CaseEntity> cases = caseService.getAllCasesByUserId(testUser2.getId());

      // Then
      assertTrue(cases.isEmpty());
    }
  }

  @Nested
  @DisplayName("getAllUserCasesByUserId() integration tests")
  class GetAllUserCasesByUserIdIntegrationTests {

    @Test
    @DisplayName("Should retrieve all UserCase entities for user")
    void shouldRetrieveAllUserCases() {
      // Given
      HashMap<Long, String> users = new HashMap<>();
      users.put(testUser2.getId(), "Position 1");

      caseService.addCase(testUser.getId(), "Case 1", "Desc", testIncident.getId(), users);

      // When
      List<UserCaseEntity> userCases = caseService.getAllUserCasesByUserId(testUser2.getId());

      // Then
      assertEquals(1, userCases.size());
      assertEquals("Position 1", userCases.get(0).getPosition());
    }

    @Test
    @DisplayName("Should return empty list when user not assigned to any cases")
    void shouldReturnEmptyListWhenNoUserCases() {
      // When
      List<UserCaseEntity> userCases = caseService.getAllUserCasesByUserId(testUser2.getId());

      // Then
      assertTrue(userCases.isEmpty());
    }
  }

  @Nested
  @DisplayName("Full lifecycle tests")
  class FullLifecycleTests {

    @Test
    @DisplayName("Should support complete case lifecycle")
    void shouldSupportCompleteLifecycle() {
      // Create
      HashMap<Long, String> users = new HashMap<>();
      users.put(testUser.getId(), "Lead");
      users.put(testUser2.getId(), "Assistant");

      CaseEntity created = caseService.addCase(testUser.getId(), "Investigation Case", "Major case", testIncident.getId(), users);
      assertNotNull(created.getId());
      assertEquals(CaseEntity.Status.OPENED, created.getStatus());

      // Update - Note: updateCase expects incident ID, not case ID
      HashMap<Long, String> updatedUsers = new HashMap<>();
      updatedUsers.put(testUser2.getId(), "Lead Detective");

      CaseEntity updated = caseService.updateCase(testUser2.getId(), testIncident.getId(), "Updated Case", "Updated desc", CaseEntity.Status.CLOSED, updatedUsers);
      assertEquals("Updated Case", updated.getTitle());
      assertEquals(CaseEntity.Status.CLOSED, updated.getStatus());

      // Get by ID
      CaseEntity found = caseService.getCaseById(created.getId());
      assertEquals("Updated Case", found.getTitle());

      // Get all by creator
      List<CaseEntity> creatorCases = caseService.getAllCasesByUserId(testUser.getId());
      assertEquals(1, creatorCases.size());

      // Get UserCases
      List<UserCaseEntity> userCases = caseService.getAllUserCasesByUserId(testUser2.getId());
      assertEquals(1, userCases.size());
      assertEquals("Lead Detective", userCases.get(0).getPosition());
    }
  }
}
