package ru.itmopai.backend.service;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.ClueEntity;
import ru.itmopai.backend.entity.IncidentEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.repository.CaseRepository;
import ru.itmopai.backend.repository.ClueRepository;
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
@DisplayName("ClueService Integration Tests")
class ClueServiceIntegrationTest implements TestPropertyProvider {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  @Inject
  ClueService clueService;

  @Inject
  ClueRepository clueRepository;

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
    clueRepository.deleteAll();
    caseRepository.deleteAll();
    incidentRepository.deleteAll();
    userRepository.deleteAll();

    // Create test user
    testUser = new UserEntity();
    testUser.setUsername("testuser");
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
    clueRepository.deleteAll();
    caseRepository.deleteAll();
    incidentRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Nested
  @DisplayName("addClue() integration tests")
  class AddClueIntegrationTests {

    @Test
    @DisplayName("Should persist clue to database")
    void shouldPersistClueToDB() {
      // Given
      String title = "Crime Scene Photo";
      String description = "Photo from the crime scene";
      ClueEntity.Status status = ClueEntity.Status.PENDING;

      // When
      ClueEntity savedClue = clueService.addClue(
          testUser.getId(),
          title,
          description,
          status,
          testCase.getId()
      );

      // Then
      assertNotNull(savedClue.getId());

      // Verify it's actually in the database
      ClueEntity foundClue = clueRepository.findById(savedClue.getId()).orElse(null);
      assertNotNull(foundClue);
      assertEquals(title, foundClue.getTitle());
      assertEquals(description, foundClue.getDescription());
      assertEquals(status, foundClue.getStatus());
      assertEquals(testUser.getId(), foundClue.getCreator().getId());
      assertEquals(testCase.getId(), foundClue.getRelatedCase().getId());
      assertEquals("{}", foundClue.getMetadata());
    }

    @Test
    @DisplayName("Should set timestamps correctly in database")
    void shouldSetTimestampsInDB() {
      // Given
      Instant beforeCreation = Instant.now().minusSeconds(1);

      // When
      ClueEntity savedClue = clueService.addClue(
          testUser.getId(),
          "Test Clue",
          "Description",
          ClueEntity.Status.PENDING,
          testCase.getId()
      );

      Instant afterCreation = Instant.now().plusSeconds(1);

      // Then
      ClueEntity foundClue = clueRepository.findById(savedClue.getId()).orElse(null);
      assertNotNull(foundClue);
      assertTrue(foundClue.getCreatedAt().isAfter(beforeCreation));
      assertTrue(foundClue.getCreatedAt().isBefore(afterCreation));
      assertEquals(foundClue.getCreatedAt(), foundClue.getUpdatedAt());
    }

    @Test
    @DisplayName("Should throw exception for non-existent user")
    void shouldThrowExceptionForNonExistentUser() {
      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        clueService.addClue(999L, "Title", "Desc", ClueEntity.Status.PENDING, testCase.getId());
      });

      // Verify nothing was saved
      assertEquals(0, clueRepository.count());
    }

    @Test
    @DisplayName("Should throw exception for non-existent case")
    void shouldThrowExceptionForNonExistentCase() {
      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        clueService.addClue(testUser.getId(), "Title", "Desc", ClueEntity.Status.PENDING, 999L);
      });

      // Verify nothing was saved
      assertEquals(0, clueRepository.count());
    }
  }

  @Nested
  @DisplayName("updateClue() integration tests")
  class UpdateClueIntegrationTests {

    @Test
    @DisplayName("Should update clue in database")
    void shouldUpdateClueInDB() {
      // Given
      ClueEntity originalClue = clueService.addClue(
          testUser.getId(),
          "Original Title",
          "Original Description",
          ClueEntity.Status.PENDING,
          testCase.getId()
      );

      Instant originalUpdatedAt = originalClue.getUpdatedAt();

      // Small delay to ensure timestamp difference
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      // When
      ClueEntity updatedClue = clueService.updateClue(
          testUser.getId(),
          originalClue.getId(),
          "Updated Title",
          "Updated Description",
          ClueEntity.Status.ACCEPTED
      );

      // Then
      ClueEntity foundClue = clueRepository.findById(updatedClue.getId()).orElse(null);
      assertNotNull(foundClue);
      assertEquals("Updated Title", foundClue.getTitle());
      assertEquals("Updated Description", foundClue.getDescription());
      assertEquals(ClueEntity.Status.ACCEPTED, foundClue.getStatus());
      assertTrue(foundClue.getUpdatedAt().isAfter(originalUpdatedAt));
      // Compare timestamps truncated to milliseconds due to DB precision
      assertEquals(
          originalClue.getCreatedAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
          foundClue.getCreatedAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
      );
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent clue")
    void shouldThrowExceptionForNonExistentClue() {
      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        clueService.updateClue(
            testUser.getId(),
            999L,
            "Title",
            "Desc",
            ClueEntity.Status.ACCEPTED
        );
      });
    }
  }

  @Nested
  @DisplayName("getClue() integration tests")
  class GetClueIntegrationTests {

    @Test
    @DisplayName("Should retrieve clue from database")
    void shouldRetrieveClueFromDB() {
      // Given
      ClueEntity savedClue = clueService.addClue(
          testUser.getId(),
          "Test Clue",
          "Test Description",
          ClueEntity.Status.PENDING,
          testCase.getId()
      );

      // When
      ClueEntity foundClue = clueService.getClue(savedClue.getId());

      // Then
      assertNotNull(foundClue);
      assertEquals(savedClue.getId(), foundClue.getId());
      assertEquals("Test Clue", foundClue.getTitle());
      assertEquals("Test Description", foundClue.getDescription());
    }

    @Test
    @DisplayName("Should throw exception for non-existent clue")
    void shouldThrowExceptionWhenClueNotFound() {
      // When & Then
      assertThrows(NoSuchElementException.class, () -> {
        clueService.getClue(999L);
      });
    }
  }

  @Nested
  @DisplayName("getAllClueByCase() integration tests")
  class GetAllClueByCaseIntegrationTests {

    @Test
    @DisplayName("Should retrieve all clues for a case")
    void shouldRetrieveAllCluesForCase() {
      // Given
      clueService.addClue(testUser.getId(), "Clue 1", "Desc 1", ClueEntity.Status.PENDING, testCase.getId());
      clueService.addClue(testUser.getId(), "Clue 2", "Desc 2", ClueEntity.Status.ACCEPTED, testCase.getId());
      clueService.addClue(testUser.getId(), "Clue 3", "Desc 3", ClueEntity.Status.REJECTED, testCase.getId());

      // When
      List<ClueEntity> clues = clueService.getAllClueByCase(testCase.getId());

      // Then
      assertNotNull(clues);
      assertEquals(3, clues.size());
      assertTrue(clues.stream().allMatch(c -> c.getRelatedCase().getId().equals(testCase.getId())));
    }

    @Test
    @DisplayName("Should return empty list for case with no clues")
    void shouldReturnEmptyListForCaseWithNoClues() {
      // When
      List<ClueEntity> clues = clueService.getAllClueByCase(testCase.getId());

      // Then
      assertNotNull(clues);
      assertTrue(clues.isEmpty());
    }

    @Test
    @DisplayName("Should only return clues for specific case")
    void shouldOnlyReturnCluesForSpecificCase() {
      // Given
      IncidentEntity anotherIncident = new IncidentEntity();
      anotherIncident.setType("Another Incident");
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

      clueService.addClue(testUser.getId(), "Case1 Clue1", "Desc", ClueEntity.Status.PENDING, testCase.getId());
      clueService.addClue(testUser.getId(), "Case1 Clue2", "Desc", ClueEntity.Status.PENDING, testCase.getId());
      clueService.addClue(testUser.getId(), "Case2 Clue1", "Desc", ClueEntity.Status.PENDING, anotherCase.getId());

      // When
      List<ClueEntity> case1Clues = clueService.getAllClueByCase(testCase.getId());
      List<ClueEntity> case2Clues = clueService.getAllClueByCase(anotherCase.getId());

      // Then
      assertEquals(2, case1Clues.size());
      assertEquals(1, case2Clues.size());
      assertTrue(case1Clues.stream().allMatch(c -> c.getTitle().startsWith("Case1")));
      assertTrue(case2Clues.stream().allMatch(c -> c.getTitle().startsWith("Case2")));
    }
  }

  @Nested
  @DisplayName("deleteClue() integration tests")
  class DeleteClueIntegrationTests {

    @Test
    @DisplayName("Should delete clue from database")
    void shouldDeleteClueFromDB() {
      // Given
      ClueEntity savedClue = clueService.addClue(
          testUser.getId(),
          "To Delete",
          "Description",
          ClueEntity.Status.PENDING,
          testCase.getId()
      );

      Long clueId = savedClue.getId();
      assertTrue(clueRepository.findById(clueId).isPresent());

      // When
      clueService.deleteClue(clueId);

      // Then
      assertFalse(clueRepository.findById(clueId).isPresent());
      assertEquals(0, clueRepository.count());
    }

    @Test
    @DisplayName("Should not throw exception when deleting non-existent clue")
    void shouldNotThrowWhenDeletingNonExistentClue() {
      // When & Then
      assertDoesNotThrow(() -> clueService.deleteClue(999L));
    }
  }

  @Nested
  @DisplayName("Full business cycle tests")
  class FullBusinessCycleTests {

    @Test
    @DisplayName("Should support complete clue lifecycle: create -> update -> get -> delete")
    void shouldSupportCompleteClueLifecycle() {
      // Create
      ClueEntity created = clueService.addClue(
          testUser.getId(),
          "Evidence Photo",
          "Photo from scene",
          ClueEntity.Status.PENDING,
          testCase.getId()
      );
      assertNotNull(created.getId());
      assertEquals(ClueEntity.Status.PENDING, created.getStatus());

      // Update
      ClueEntity updated = clueService.updateClue(
          testUser.getId(),
          created.getId(),
          "Evidence Photo - Enhanced",
          "Enhanced photo from scene",
          ClueEntity.Status.ACCEPTED
      );
      assertEquals("Evidence Photo - Enhanced", updated.getTitle());
      assertEquals(ClueEntity.Status.ACCEPTED, updated.getStatus());

      // Get
      ClueEntity retrieved = clueService.getClue(created.getId());
      assertEquals("Evidence Photo - Enhanced", retrieved.getTitle());
      assertEquals(ClueEntity.Status.ACCEPTED, retrieved.getStatus());

      // Delete
      clueService.deleteClue(created.getId());
      assertThrows(NoSuchElementException.class, () -> clueService.getClue(created.getId()));
    }

    @Test
    @DisplayName("Should handle multiple status transitions")
    void shouldHandleMultipleStatusTransitions() {
      // Create as PENDING
      ClueEntity clue = clueService.addClue(
          testUser.getId(),
          "Test Evidence",
          "Description",
          ClueEntity.Status.PENDING,
          testCase.getId()
      );
      assertEquals(ClueEntity.Status.PENDING, clue.getStatus());

      // Transition to ACCEPTED
      clue = clueService.updateClue(
          testUser.getId(),
          clue.getId(),
          "Test Evidence",
          "Description",
          ClueEntity.Status.ACCEPTED
      );
      assertEquals(ClueEntity.Status.ACCEPTED, clue.getStatus());

      // Transition to REJECTED
      clue = clueService.updateClue(
          testUser.getId(),
          clue.getId(),
          "Test Evidence",
          "Description",
          ClueEntity.Status.REJECTED
      );
      assertEquals(ClueEntity.Status.REJECTED, clue.getStatus());

      // Verify final state in DB
      ClueEntity finalClue = clueRepository.findById(clue.getId()).orElseThrow();
      assertEquals(ClueEntity.Status.REJECTED, finalClue.getStatus());
    }

    @Test
    @DisplayName("Should maintain data integrity across multiple clues")
    void shouldMaintainDataIntegrityAcrossMultipleClues() {
      // Create multiple clues
      ClueEntity clue1 = clueService.addClue(
          testUser.getId(), "Clue 1", "Desc 1",
          ClueEntity.Status.PENDING, testCase.getId()
      );

      ClueEntity clue2 = clueService.addClue(
          testUser.getId(), "Clue 2", "Desc 2",
          ClueEntity.Status.ACCEPTED, testCase.getId()
      );

      ClueEntity clue3 = clueService.addClue(
          testUser.getId(), "Clue 3", "Desc 3",
          ClueEntity.Status.REJECTED, testCase.getId()
      );

      // Update one
      clueService.updateClue(
          testUser.getId(),
          clue2.getId(),
          "Clue 2 Updated",
          "Desc 2 Updated",
          ClueEntity.Status.PENDING
      );

      // Delete one
      clueService.deleteClue(clue3.getId());

      // Verify final state
      List<ClueEntity> remainingClues = clueService.getAllClueByCase(testCase.getId());
      assertEquals(2, remainingClues.size());

      ClueEntity foundClue1 = clueService.getClue(clue1.getId());
      assertEquals("Clue 1", foundClue1.getTitle());

      ClueEntity foundClue2 = clueService.getClue(clue2.getId());
      assertEquals("Clue 2 Updated", foundClue2.getTitle());
      assertEquals(ClueEntity.Status.PENDING, foundClue2.getStatus());

      assertThrows(NoSuchElementException.class, () -> clueService.getClue(clue3.getId()));
    }

    @Test
    @DisplayName("Should persist and retrieve complex metadata")
    void shouldPersistComplexMetadata() {
      // Given
      ClueEntity clue = clueService.addClue(
          testUser.getId(),
          "Clue with Metadata",
          "Has complex metadata",
          ClueEntity.Status.PENDING,
          testCase.getId()
      );

      // When - Update metadata with complex data
      String complexMetadata = "{\"imageUris\":[\"http://example.com/img1.jpg\",\"http://example.com/img2.jpg\"],\"videoUris\":[\"http://example.com/video.mp4\"],\"reportUris\":[\"http://example.com/report.pdf\"],\"location\":{\"latitude\":55.7558,\"longitude\":37.6173,\"altitude\":150.0}}";
      clue.setMetadata(complexMetadata);
      ClueEntity updatedClue = clueRepository.update(clue);

      // Then - Retrieve and verify metadata persisted correctly
      ClueEntity retrievedClue = clueRepository.findById(updatedClue.getId()).orElseThrow();
      assertEquals(complexMetadata, retrievedClue.getMetadata());
      assertTrue(retrievedClue.getMetadata().contains("imageUris"));
      assertTrue(retrievedClue.getMetadata().contains("videoUris"));
      assertTrue(retrievedClue.getMetadata().contains("location"));
    }

    @Test
    @DisplayName("Should verify updator changes when different user updates clue")
    void shouldVerifyUpdatorChangesOnUpdate() {
      // Given - Create clue with first user
      ClueEntity originalClue = clueService.addClue(
          testUser.getId(),
          "Original Clue",
          "Created by testUser",
          ClueEntity.Status.PENDING,
          testCase.getId()
      );

      assertEquals(testUser.getId(), originalClue.getCreator().getId());
      assertEquals(testUser.getId(), originalClue.getUpdator().getId());

      // Create another user
      UserEntity anotherUser = new UserEntity();
      anotherUser.setUsername("another_user");
      anotherUser.setName("Another");
      anotherUser.setSurname("User");
      anotherUser.setPassword("password");
      anotherUser.setLocationLatitude(55.7558);
      anotherUser.setLocationLongitude(37.6173);
      anotherUser.setLocationAltitude(150.0);
      anotherUser = userRepository.save(anotherUser);

      // When - Another user updates the clue
      ClueEntity updatedClue = clueService.updateClue(
          anotherUser.getId(),
          originalClue.getId(),
          "Updated by Another User",
          "Description",
          ClueEntity.Status.ACCEPTED
      );

      // Then - Verify creator stays same but updator changes
      ClueEntity retrievedClue = clueRepository.findById(updatedClue.getId()).orElseThrow();
      assertEquals(testUser.getId(), retrievedClue.getCreator().getId());
      assertEquals(anotherUser.getId(), retrievedClue.getUpdator().getId());

      // Cleanup - Must delete clue first due to foreign key constraint
      clueRepository.deleteById(originalClue.getId());
      userRepository.deleteById(anotherUser.getId());
    }
  }
}
