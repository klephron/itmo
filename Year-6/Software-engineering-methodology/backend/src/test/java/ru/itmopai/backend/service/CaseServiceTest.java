package ru.itmopai.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmopai.backend.entity.*;
import ru.itmopai.backend.mapper.CaseMainInfoMapper;
import ru.itmopai.backend.mapper.CaseMapper;
import ru.itmopai.backend.repository.CaseRepository;
import ru.itmopai.backend.repository.IncidentRepository;
import ru.itmopai.backend.repository.UserCaseRepository;
import ru.itmopai.backend.repository.UserRepository;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CaseService Unit Tests")
class CaseServiceTest {

  @Mock
  private CaseRepository caseRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private CaseMapper caseMapper;

  @Mock
  private CaseMainInfoMapper caseMainInfoMapper;

  @Mock
  private UserCaseRepository userCaseRepository;

  @InjectMocks
  private CaseService caseService;

  private UserEntity testUser;
  private IncidentEntity testIncident;
  private CaseEntity testCase;

  @BeforeEach
  void setUp() {
    testUser = new UserEntity();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setName("Test");
    testUser.setSurname("User");

    testIncident = new IncidentEntity();
    testIncident.setId(100L);
    testIncident.setType("Test Incident Type");
    testIncident.setReason("Test Reason");
    testIncident.setEstimatedTime(Instant.now());

    testCase = new CaseEntity();
    testCase.setId(1L);
    testCase.setTitle("Test Case");
    testCase.setDescription("Test Description");
    testCase.setStatus(CaseEntity.Status.OPENED);
    testCase.setCreator(testUser);
    testCase.setUpdator(testUser);
    testCase.setIncident(testIncident);
    testCase.setCreatedAt(Instant.now());
    testCase.setUpdatedAt(Instant.now());
  }

  @Nested
  @DisplayName("addCase() method tests")
  class AddCaseTests {

    @Test
    @DisplayName("Should successfully create case with users")
    void shouldCreateCaseWithUsers() {
      // Given
      Long userId = 1L;
      String title = "New Case";
      String description = "Description";
      Long incidentId = 100L;

      UserEntity user2 = new UserEntity();
      user2.setId(2L);
      user2.setUsername("user2");

      HashMap<Long, String> users = new HashMap<>();
      users.put(1L, "Lead Investigator");
      users.put(2L, "Assistant");

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(testIncident));
      when(userRepository.findAllByIdIn(anySet())).thenReturn(List.of(testUser, user2));
      when(caseRepository.save(any(CaseEntity.class))).thenAnswer(invocation -> {
        CaseEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });
      when(incidentRepository.update(any())).thenReturn(testIncident);
      when(userCaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      CaseEntity result = caseService.addCase(userId, title, description, incidentId, users);

      // Then
      assertNotNull(result);
      assertEquals(title, result.getTitle());
      assertEquals(description, result.getDescription());
      assertEquals(CaseEntity.Status.OPENED, result.getStatus());
      assertEquals(testUser, result.getCreator());
      assertEquals(testUser, result.getUpdator());
      assertNotNull(result.getCreatedAt());
      assertNotNull(result.getUpdatedAt());

      verify(caseRepository).save(any(CaseEntity.class));
      verify(userCaseRepository, times(2)).save(any(UserCaseEntity.class));
      verify(incidentRepository).update(testIncident);
    }

    @Test
    @DisplayName("Should throw exception when creator user not found")
    void shouldThrowExceptionWhenCreatorNotFound() {
      // Given
      Long nonExistentUserId = 999L;
      HashMap<Long, String> users = new HashMap<>();

      when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        caseService.addCase(nonExistentUserId, "Title", "Desc", 100L, users);
      });

      assertEquals("Creator user not found", exception.getMessage());
      verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when incident not found")
    void shouldThrowExceptionWhenIncidentNotFound() {
      // Given
      Long userId = 1L;
      Long nonExistentIncidentId = 999L;
      HashMap<Long, String> users = new HashMap<>();

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.findById(nonExistentIncidentId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        caseService.addCase(userId, "Title", "Desc", nonExistentIncidentId, users);
      });

      assertEquals("Incident not found", exception.getMessage());
      verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when some users not found")
    void shouldThrowExceptionWhenSomeUsersNotFound() {
      // Given
      Long userId = 1L;
      Long incidentId = 100L;
      HashMap<Long, String> users = new HashMap<>();
      users.put(1L, "Position1");
      users.put(999L, "Position2"); // Non-existent user

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(testIncident));
      when(userRepository.findAllByIdIn(anySet())).thenReturn(List.of(testUser)); // Only user 1 found

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        caseService.addCase(userId, "Title", "Desc", incidentId, users);
      });

      assertTrue(exception.getMessage().contains("Some of users not found"));
      assertTrue(exception.getMessage().contains("999"));
      verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set status to OPENED by default")
    void shouldSetStatusToOpened() {
      // Given
      Long userId = 1L;
      Long incidentId = 100L;
      HashMap<Long, String> users = new HashMap<>();

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(testIncident));
      when(userRepository.findAllByIdIn(anySet())).thenReturn(List.of());

      ArgumentCaptor<CaseEntity> caseCaptor = ArgumentCaptor.forClass(CaseEntity.class);
      when(caseRepository.save(caseCaptor.capture())).thenAnswer(invocation -> {
        CaseEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });
      when(incidentRepository.update(any())).thenReturn(testIncident);

      // When
      caseService.addCase(userId, "Title", "Desc", incidentId, users);

      // Then
      CaseEntity savedCase = caseCaptor.getValue();
      assertEquals(CaseEntity.Status.OPENED, savedCase.getStatus());
    }

    @Test
    @DisplayName("Should set creator and updator to same user")
    void shouldSetCreatorAndUpdatorToSameUser() {
      // Given
      Long userId = 1L;
      Long incidentId = 100L;
      HashMap<Long, String> users = new HashMap<>();

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(testIncident));
      when(userRepository.findAllByIdIn(anySet())).thenReturn(List.of());

      ArgumentCaptor<CaseEntity> caseCaptor = ArgumentCaptor.forClass(CaseEntity.class);
      when(caseRepository.save(caseCaptor.capture())).thenAnswer(invocation -> {
        CaseEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });
      when(incidentRepository.update(any())).thenReturn(testIncident);

      // When
      caseService.addCase(userId, "Title", "Desc", incidentId, users);

      // Then
      CaseEntity savedCase = caseCaptor.getValue();
      assertEquals(testUser, savedCase.getCreator());
      assertEquals(testUser, savedCase.getUpdator());
    }

    @Test
    @DisplayName("Should create UserCase entities with correct positions")
    void shouldCreateUserCaseWithPositions() {
      // Given
      Long userId = 1L;
      Long incidentId = 100L;
      HashMap<Long, String> users = new HashMap<>();
      users.put(1L, "Lead Detective");

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(testIncident));
      when(userRepository.findAllByIdIn(anySet())).thenReturn(List.of(testUser));
      when(caseRepository.save(any(CaseEntity.class))).thenAnswer(invocation -> {
        CaseEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });
      when(incidentRepository.update(any())).thenReturn(testIncident);

      ArgumentCaptor<UserCaseEntity> userCaseCaptor = ArgumentCaptor.forClass(UserCaseEntity.class);
      when(userCaseRepository.save(userCaseCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      caseService.addCase(userId, "Title", "Desc", incidentId, users);

      // Then
      UserCaseEntity savedUserCase = userCaseCaptor.getValue();
      assertEquals("Lead Detective", savedUserCase.getPosition());
      assertEquals(UserCaseEntity.Status.WORKING, savedUserCase.getStatus());
    }
  }

  @Nested
  @DisplayName("updateCase() method tests")
  class UpdateCaseTests {

    @Test
    @DisplayName("Should successfully update case")
    void shouldUpdateCase() {
      // Given
      Long userId = 1L;
      Long caseId = 1L;
      String newTitle = "Updated Title";
      String newDescription = "Updated Description";
      CaseEntity.Status newStatus = CaseEntity.Status.CLOSED;
      HashMap<Long, String> users = new HashMap<>();
      users.put(1L, "Lead");

      testIncident.setRelatedCase(testCase);

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.findById(caseId)).thenReturn(Optional.of(testIncident));
      when(userRepository.findAllByIdIn(anySet())).thenReturn(List.of(testUser));
      when(caseMainInfoMapper.partialUpdate(any(), any())).thenReturn(testCase);
      when(caseRepository.update(any(CaseEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(userCaseRepository.findAllById_RelatedCaseId(anyLong())).thenReturn(List.of());
      when(userCaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      CaseEntity result = caseService.updateCase(userId, caseId, newTitle, newDescription, newStatus, users);

      // Then
      assertNotNull(result);
      verify(caseRepository).update(any(CaseEntity.class));
      verify(userCaseRepository).deleteAll(any());
      verify(userCaseRepository).save(any(UserCaseEntity.class));
    }

    @Test
    @DisplayName("Should update updator and updatedAt")
    void shouldUpdateUpdatorAndTimestamp() {
      // Given
      Long userId = 1L;
      Long caseId = 1L;
      HashMap<Long, String> users = new HashMap<>();

      testIncident.setRelatedCase(testCase);

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.findById(caseId)).thenReturn(Optional.of(testIncident));
      when(userRepository.findAllByIdIn(anySet())).thenReturn(List.of());
      when(caseMainInfoMapper.partialUpdate(any(), any())).thenReturn(testCase);

      ArgumentCaptor<CaseEntity> caseCaptor = ArgumentCaptor.forClass(CaseEntity.class);
      when(caseRepository.update(caseCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
      when(userCaseRepository.findAllById_RelatedCaseId(anyLong())).thenReturn(List.of());

      // When
      caseService.updateCase(userId, caseId, "Title", "Desc", CaseEntity.Status.OPENED, users);

      // Then
      CaseEntity updatedCase = caseCaptor.getValue();
      assertEquals(testUser, updatedCase.getUpdator());
      assertNotNull(updatedCase.getUpdatedAt());
    }

    @Test
    @DisplayName("Should delete old UserCase entities before creating new ones")
    void shouldDeleteOldUserCaseEntities() {
      // Given
      Long userId = 1L;
      Long caseId = 1L;
      HashMap<Long, String> users = new HashMap<>();
      users.put(1L, "New Position");

      testIncident.setRelatedCase(testCase);

      UserCaseEntity oldUserCase = new UserCaseEntity();
      List<UserCaseEntity> oldUserCases = List.of(oldUserCase);

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.findById(caseId)).thenReturn(Optional.of(testIncident));
      when(userRepository.findAllByIdIn(anySet())).thenReturn(List.of(testUser));
      when(caseMainInfoMapper.partialUpdate(any(), any())).thenReturn(testCase);
      when(caseRepository.update(any())).thenReturn(testCase);
      when(userCaseRepository.findAllById_RelatedCaseId(caseId)).thenReturn(oldUserCases);
      when(userCaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      caseService.updateCase(userId, caseId, "Title", "Desc", CaseEntity.Status.OPENED, users);

      // Then
      verify(userCaseRepository).deleteAll(oldUserCases);
      verify(userCaseRepository).save(any(UserCaseEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when updating user not found")
    void shouldThrowExceptionWhenUpdatingUserNotFound() {
      // Given
      Long userId = 999L;
      HashMap<Long, String> users = new HashMap<>();

      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        caseService.updateCase(userId, 1L, "Title", "Desc", CaseEntity.Status.OPENED, users);
      });

      assertEquals("Creator user not found", exception.getMessage());
      verify(caseRepository, never()).update(any());
    }
  }

  @Nested
  @DisplayName("getCaseById() method tests")
  class GetCaseByIdTests {

    @Test
    @DisplayName("Should return case when exists")
    void shouldReturnCaseWhenExists() {
      // Given
      Long caseId = 1L;
      when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));

      // When
      CaseEntity result = caseService.getCaseById(caseId);

      // Then
      assertNotNull(result);
      assertEquals(testCase.getId(), result.getId());
      assertEquals(testCase.getTitle(), result.getTitle());
      verify(caseRepository).findById(caseId);
    }

    @Test
    @DisplayName("Should throw exception when case not found")
    void shouldThrowExceptionWhenCaseNotFound() {
      // Given
      Long caseId = 999L;
      when(caseRepository.findById(caseId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        caseService.getCaseById(caseId);
      });

      assertEquals("Case not found", exception.getMessage());
      verify(caseRepository).findById(caseId);
    }
  }

  @Nested
  @DisplayName("getAllCasesByUserId() method tests")
  class GetAllCasesByUserIdTests {

    @Test
    @DisplayName("Should return all cases created by user")
    void shouldReturnAllCasesByUser() {
      // Given
      Long userId = 1L;
      CaseEntity case1 = new CaseEntity();
      case1.setId(1L);
      case1.setTitle("Case 1");

      CaseEntity case2 = new CaseEntity();
      case2.setId(2L);
      case2.setTitle("Case 2");

      List<CaseEntity> cases = List.of(case1, case2);
      when(caseRepository.findAllByCreatorId(userId)).thenReturn(cases);

      // When
      List<CaseEntity> result = caseService.getAllCasesByUserId(userId);

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
      assertEquals("Case 1", result.get(0).getTitle());
      assertEquals("Case 2", result.get(1).getTitle());
      verify(caseRepository).findAllByCreatorId(userId);
    }

    @Test
    @DisplayName("Should return empty list when user has no cases")
    void shouldReturnEmptyListWhenNoCases() {
      // Given
      Long userId = 1L;
      when(caseRepository.findAllByCreatorId(userId)).thenReturn(List.of());

      // When
      List<CaseEntity> result = caseService.getAllCasesByUserId(userId);

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
      verify(caseRepository).findAllByCreatorId(userId);
    }
  }

  @Nested
  @DisplayName("getAllUserCasesByUserId() method tests")
  class GetAllUserCasesByUserIdTests {

    @Test
    @DisplayName("Should return all UserCase entities for user")
    void shouldReturnAllUserCases() {
      // Given
      Long userId = 1L;
      UserCaseEntity userCase1 = new UserCaseEntity();
      UserCaseEntity userCase2 = new UserCaseEntity();

      List<UserCaseEntity> userCases = List.of(userCase1, userCase2);
      when(userCaseRepository.findAllById_UserId(userId)).thenReturn(userCases);

      // When
      List<UserCaseEntity> result = caseService.getAllUserCasesByUserId(userId);

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
      verify(userCaseRepository).findAllById_UserId(userId);
    }

    @Test
    @DisplayName("Should return empty list when user not assigned to any cases")
    void shouldReturnEmptyListWhenNoUserCases() {
      // Given
      Long userId = 1L;
      when(userCaseRepository.findAllById_UserId(userId)).thenReturn(List.of());

      // When
      List<UserCaseEntity> result = caseService.getAllUserCasesByUserId(userId);

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
      verify(userCaseRepository).findAllById_UserId(userId);
    }
  }
}
