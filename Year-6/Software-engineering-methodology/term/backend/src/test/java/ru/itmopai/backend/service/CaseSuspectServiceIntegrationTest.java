package ru.itmopai.backend.service;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.CaseSuspectEntity;
import ru.itmopai.backend.entity.IncidentEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.repository.CaseRepository;
import ru.itmopai.backend.repository.CaseSuspectRepository;
import ru.itmopai.backend.repository.IncidentRepository;
import ru.itmopai.backend.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("CaseSuspectService Integration Tests")
class CaseSuspectServiceIntegrationTest implements TestPropertyProvider {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  @Inject
  CaseSuspectService caseSuspectService;

  @Inject
  CaseSuspectRepository caseSuspectRepository;

  @Inject
  UserRepository userRepository;

  @Inject
  CaseRepository caseRepository;

  @Inject
  IncidentRepository incidentRepository;

  private UserEntity testUser;
  private CaseEntity testCase;
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
    // Clean up before each test (order matters due to foreign key constraints)
    caseSuspectRepository.deleteAll();
    caseRepository.deleteAll();
    incidentRepository.deleteAll();
    userRepository.deleteAll();

    // Create test user
    testUser = new UserEntity();
    testUser.setUsername("testuser_integration");
    testUser.setName("Test");
    testUser.setSurname("User");
    testUser.setPassword("hashedpassword");
    testUser.setLocationLatitude(55.7558);
    testUser.setLocationLongitude(37.6173);
    testUser.setLocationAltitude(150.0);
    testUser = userRepository.save(testUser);

    // Create test incident
    testIncident = new IncidentEntity();
    testIncident.setType("Test Incident Type");
    testIncident.setReason("Test Reason");
    testIncident.setCreator(testUser);
    testIncident.setUpdator(testUser);
    testIncident.setCreatedAt(Instant.now());
    testIncident.setUpdatedAt(Instant.now());
    testIncident.setLocationLatitude(55.7558);
    testIncident.setLocationLongitude(37.6173);
    testIncident.setLocationAltitude(150.0);
    testIncident.setEstimatedTime(Instant.now());
    testIncident = incidentRepository.save(testIncident);

    // Create test case
    testCase = new CaseEntity();
    testCase.setTitle("Integration Test Case");
    testCase.setDescription("Test Description");
    testCase.setStatus(CaseEntity.Status.OPENED);
    testCase.setCreator(testUser);
    testCase.setUpdator(testUser);
    testCase.setCreatedAt(Instant.now());
    testCase.setUpdatedAt(Instant.now());
    testCase.setIncident(testIncident);
    testCase = caseRepository.save(testCase);
  }

  @AfterEach
  void tearDown() {
    // Clean up after each test (order matters due to foreign key constraints)
    caseSuspectRepository.deleteAll();
    caseRepository.deleteAll();
    incidentRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Nested
  @DisplayName("addCaseSuspect() integration tests")
  class AddCaseSuspectTests {

    @Test
    @DisplayName("Should add suspect to database and retrieve it")
    void shouldAddAndRetrieveSuspect() {
      // Given
      String name = "John";
      String surname = "Doe";
      String patronymic = "Smith";
      String identifier = "PASSPORT-123456-" + System.currentTimeMillis();
      CaseSuspectEntity.Status status = CaseSuspectEntity.Status.SUSPECTED;

      // When
      CaseSuspectEntity saved = caseSuspectService.addCaseSuspect(
          testUser.getId(), name, surname, patronymic, identifier, status,
          testCase.getId(), null, null);

      // Then
      assertNotNull(saved);
      assertNotNull(saved.getId());

      // Verify in database
      CaseSuspectEntity retrieved = caseSuspectRepository.findById(saved.getId()).orElse(null);
      assertNotNull(retrieved);
      assertEquals(name, retrieved.getName());
      assertEquals(surname, retrieved.getSurname());
      assertEquals(patronymic, retrieved.getPatronymic());
      assertEquals(identifier, retrieved.getIdentifier());
      assertEquals(status, retrieved.getStatus());
      assertEquals(testUser.getId(), retrieved.getCreator().getId());
      assertEquals(testCase.getId(), retrieved.getRelatedCase().getId());
    }

    @Test
    @DisplayName("Should fail with non-existent user")
    void shouldFailWithNonExistentUser() {
      // Given
      Long nonExistentUserId = 99999L;

      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        caseSuspectService.addCaseSuspect(nonExistentUserId, "John", "Doe", "Smith",
            "PASSPORT-999", CaseSuspectEntity.Status.SUSPECTED, testCase.getId(), null, null);
      });
    }

    @Test
    @DisplayName("Should fail with non-existent case")
    void shouldFailWithNonExistentCase() {
      // Given
      Long nonExistentCaseId = 99999L;

      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        caseSuspectService.addCaseSuspect(testUser.getId(), "John", "Doe", "Smith",
            "PASSPORT-999", CaseSuspectEntity.Status.SUSPECTED, nonExistentCaseId, null, null);
      });
    }

    @Test
    @DisplayName("Should add suspect with verdict and accusation")
    void shouldAddSuspectWithVerdictAndAccusation() {
      // Given
      String verdict = "Guilty";
      String accusation = "Theft";
      String identifier = "PASSPORT-VERDICT-" + System.currentTimeMillis();

      // When
      CaseSuspectEntity saved = caseSuspectService.addCaseSuspect(
          testUser.getId(), "Jane", "Smith", "Johnson", identifier,
          CaseSuspectEntity.Status.ACCUSED, testCase.getId(), verdict, accusation);

      // Then
      assertNotNull(saved);
      assertEquals(verdict, saved.getVerdict());
      assertEquals(accusation, saved.getAccusation());
    }
  }

  @Nested
  @DisplayName("updateCaseSuspect() integration tests")
  class UpdateCaseSuspectTests {

    @Test
    @DisplayName("Should update suspect in database")
    void shouldUpdateSuspectInDatabase() {
      // Given - Create a suspect first
      String identifier = "PASSPORT-UPDATE-" + System.currentTimeMillis();
      CaseSuspectEntity suspect = caseSuspectService.addCaseSuspect(
          testUser.getId(), "Original", "Name", "Patronymic", identifier,
          CaseSuspectEntity.Status.SUSPECTED, testCase.getId(), null, null);

      // When - Update the suspect
      String newName = "Updated";
      String newSurname = "Surname";
      String newPatronymic = "NewPatronymic";
      String newIdentifier = "PASSPORT-UPDATED-" + System.currentTimeMillis();
      CaseSuspectEntity.Status newStatus = CaseSuspectEntity.Status.ACCUSED;
      String verdict = "Guilty";
      String accusation = "Robbery";

      CaseSuspectEntity updated = caseSuspectService.updateCaseSuspect(
          suspect.getId(), testUser.getId(), newName, newSurname, newPatronymic,
          newIdentifier, newStatus, verdict, accusation);

      // Then - Verify the update
      assertNotNull(updated);
      assertEquals(suspect.getId(), updated.getId());

      // Verify in database
      CaseSuspectEntity retrieved = caseSuspectRepository.findById(suspect.getId()).orElse(null);
      assertNotNull(retrieved);
      assertEquals(newName, retrieved.getName());
      assertEquals(newSurname, retrieved.getSurname());
      assertEquals(newPatronymic, retrieved.getPatronymic());
      assertEquals(newIdentifier, retrieved.getIdentifier());
      assertEquals(newStatus, retrieved.getStatus());
      assertEquals(verdict, retrieved.getVerdict());
      assertEquals(accusation, retrieved.getAccusation());
      assertTrue(retrieved.getUpdatedAt().isAfter(suspect.getCreatedAt()));
    }

    @Test
    @DisplayName("Should fail to update non-existent suspect")
    void shouldFailToUpdateNonExistentSuspect() {
      // Given
      Long nonExistentId = 99999L;

      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        caseSuspectService.updateCaseSuspect(nonExistentId, testUser.getId(),
            "Name", "Surname", "Patronymic", "PASSPORT-999",
            CaseSuspectEntity.Status.ACCUSED, null, null);
      });
    }
  }

  @Nested
  @DisplayName("getCaseSuspect() integration tests")
  class GetCaseSuspectTests {

    @Test
    @DisplayName("Should retrieve suspect from database")
    void shouldRetrieveSuspectFromDatabase() {
      // Given
      String identifier = "PASSPORT-GET-" + System.currentTimeMillis();
      CaseSuspectEntity suspect = caseSuspectService.addCaseSuspect(
          testUser.getId(), "John", "Doe", "Smith", identifier,
          CaseSuspectEntity.Status.SUSPECTED, testCase.getId(), null, null);

      // When
      CaseSuspectEntity retrieved = caseSuspectService.getCaseSuspect(suspect.getId());

      // Then
      assertNotNull(retrieved);
      assertEquals(suspect.getId(), retrieved.getId());
      assertEquals("John", retrieved.getName());
      assertEquals("Doe", retrieved.getSurname());
    }

    @Test
    @DisplayName("Should fail to retrieve non-existent suspect")
    void shouldFailToRetrieveNonExistentSuspect() {
      // Given
      Long nonExistentId = 99999L;

      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        caseSuspectService.getCaseSuspect(nonExistentId);
      });
    }
  }

  @Nested
  @DisplayName("deleteCaseSuspect() integration tests")
  class DeleteCaseSuspectTests {

    @Test
    @DisplayName("Should delete suspect from database")
    void shouldDeleteSuspectFromDatabase() {
      // Given
      String identifier = "PASSPORT-DELETE-" + System.currentTimeMillis();
      CaseSuspectEntity suspect = caseSuspectService.addCaseSuspect(
          testUser.getId(), "John", "Doe", "Smith", identifier,
          CaseSuspectEntity.Status.SUSPECTED, testCase.getId(), null, null);

      Long suspectId = suspect.getId();
      assertTrue(caseSuspectRepository.findById(suspectId).isPresent());

      // When
      caseSuspectService.deleteCaseSuspect(suspectId);

      // Then
      assertFalse(caseSuspectRepository.findById(suspectId).isPresent());
    }

    @Test
    @DisplayName("Should not throw exception when deleting non-existent suspect")
    void shouldNotThrowExceptionWhenDeletingNonExistent() {
      // Given
      Long nonExistentId = 99999L;

      // When & Then - Should not throw exception
      assertDoesNotThrow(() -> {
        caseSuspectService.deleteCaseSuspect(nonExistentId);
      });
    }
  }

  @Nested
  @DisplayName("getAllCaseSuspectByCaseId() integration tests")
  class GetAllCaseSuspectByCaseIdTests {

    @Test
    @DisplayName("Should retrieve all suspects for a case")
    void shouldRetrieveAllSuspectsForCase() {
      // Given - Add multiple suspects to the case
      String identifier1 = "PASSPORT-ALL-1-" + System.currentTimeMillis();
      String identifier2 = "PASSPORT-ALL-2-" + System.currentTimeMillis();
      String identifier3 = "PASSPORT-ALL-3-" + System.currentTimeMillis();

      CaseSuspectEntity suspect1 = caseSuspectService.addCaseSuspect(
          testUser.getId(), "John", "Doe", "Smith", identifier1,
          CaseSuspectEntity.Status.SUSPECTED, testCase.getId(), null, null);

      CaseSuspectEntity suspect2 = caseSuspectService.addCaseSuspect(
          testUser.getId(), "Jane", "Smith", "Johnson", identifier2,
          CaseSuspectEntity.Status.ACCUSED, testCase.getId(), "Guilty", "Theft");

      CaseSuspectEntity suspect3 = caseSuspectService.addCaseSuspect(
          testUser.getId(), "Bob", "Brown", "Williams", identifier3,
          CaseSuspectEntity.Status.JUSTIFIED, testCase.getId(), "Not Guilty", null);

      // When
      List<CaseSuspectEntity> suspects = caseSuspectService.getAllCaseSuspectByCaseId(testCase.getId());

      // Then
      assertNotNull(suspects);
      assertEquals(3, suspects.size());
      assertTrue(suspects.stream().anyMatch(s -> s.getId().equals(suspect1.getId())));
      assertTrue(suspects.stream().anyMatch(s -> s.getId().equals(suspect2.getId())));
      assertTrue(suspects.stream().anyMatch(s -> s.getId().equals(suspect3.getId())));
    }

    @Test
    @DisplayName("Should return empty list when case has no suspects")
    void shouldReturnEmptyListWhenNoSuspects() {
      // When
      List<CaseSuspectEntity> suspects = caseSuspectService.getAllCaseSuspectByCaseId(testCase.getId());

      // Then
      assertNotNull(suspects);
      assertTrue(suspects.isEmpty());
    }

    @Test
    @DisplayName("Should not return suspects from other cases")
    void shouldNotReturnSuspectsFromOtherCases() {
      // Given - Create another incident for another case
      IncidentEntity anotherIncident = new IncidentEntity();
      anotherIncident.setType("Another Incident Type");
      anotherIncident.setReason("Another Reason");
      anotherIncident.setCreator(testUser);
      anotherIncident.setUpdator(testUser);
      anotherIncident.setCreatedAt(Instant.now());
      anotherIncident.setUpdatedAt(Instant.now());
      anotherIncident.setLocationLatitude(55.7558);
      anotherIncident.setLocationLongitude(37.6173);
      anotherIncident.setLocationAltitude(150.0);
      anotherIncident.setEstimatedTime(Instant.now());
      anotherIncident = incidentRepository.save(anotherIncident);

      // Create another case
      CaseEntity anotherCase = new CaseEntity();
      anotherCase.setTitle("Another Case");
      anotherCase.setDescription("Another Description");
      anotherCase.setStatus(CaseEntity.Status.OPENED);
      anotherCase.setCreator(testUser);
      anotherCase.setUpdator(testUser);
      anotherCase.setCreatedAt(Instant.now());
      anotherCase.setUpdatedAt(Instant.now());
      anotherCase.setIncident(anotherIncident);
      anotherCase = caseRepository.save(anotherCase);

      // Add suspects to both cases
      String identifier1 = "PASSPORT-CASE1-" + System.currentTimeMillis();
      String identifier2 = "PASSPORT-CASE2-" + System.currentTimeMillis();

      CaseSuspectEntity suspect1 = caseSuspectService.addCaseSuspect(
          testUser.getId(), "John", "Doe", "Smith", identifier1,
          CaseSuspectEntity.Status.SUSPECTED, testCase.getId(), null, null);

      CaseSuspectEntity suspect2 = caseSuspectService.addCaseSuspect(
          testUser.getId(), "Jane", "Smith", "Johnson", identifier2,
          CaseSuspectEntity.Status.SUSPECTED, anotherCase.getId(), null, null);

      // When
      List<CaseSuspectEntity> suspectsCase1 = caseSuspectService.getAllCaseSuspectByCaseId(testCase.getId());
      List<CaseSuspectEntity> suspectsCase2 = caseSuspectService.getAllCaseSuspectByCaseId(anotherCase.getId());

      // Then
      assertEquals(1, suspectsCase1.size());
      assertEquals(1, suspectsCase2.size());
      assertEquals(suspect1.getId(), suspectsCase1.get(0).getId());
      assertEquals(suspect2.getId(), suspectsCase2.get(0).getId());
    }
  }
}
