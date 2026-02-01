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
import ru.itmopai.backend.dto.CaseSuspectMainInfoDto;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.CaseSuspectEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.mapper.CaseSuspectMainInfoMapper;
import ru.itmopai.backend.repository.CaseRepository;
import ru.itmopai.backend.repository.CaseSuspectRepository;
import ru.itmopai.backend.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CaseSuspectService Unit Tests")
class CaseSuspectServiceTest {

  @Mock
  private CaseSuspectRepository caseSuspectRepository;

  @Mock
  private CaseRepository caseRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CaseSuspectMainInfoMapper caseSuspectMainInfoMapper;

  @InjectMocks
  private CaseSuspectService caseSuspectService;

  private UserEntity testUser;
  private CaseEntity testCase;
  private CaseSuspectEntity testCaseSuspect;

  @BeforeEach
  void setUp() {
    testUser = new UserEntity();
    testUser.setId(1L);
    testUser.setUsername("testuser");

    testCase = new CaseEntity();
    testCase.setId(100L);
    testCase.setTitle("Test Case");

    testCaseSuspect = new CaseSuspectEntity();
    testCaseSuspect.setId(1L);
    testCaseSuspect.setName("John");
    testCaseSuspect.setSurname("Doe");
    testCaseSuspect.setPatronymic("Smith");
    testCaseSuspect.setIdentifier("PASSPORT-123456");
    testCaseSuspect.setStatus(CaseSuspectEntity.Status.SUSPECTED);
    testCaseSuspect.setVerdict(null);
    testCaseSuspect.setAccusation(null);
    testCaseSuspect.setCreator(testUser);
    testCaseSuspect.setUpdator(testUser);
    testCaseSuspect.setRelatedCase(testCase);
    testCaseSuspect.setCreatedAt(Instant.now());
    testCaseSuspect.setUpdatedAt(Instant.now());
  }

  @Nested
  @DisplayName("addCaseSuspect() method tests")
  class AddCaseSuspectTests {

    @Test
    @DisplayName("Should successfully add a new case suspect")
    void shouldAddCaseSuspectSuccessfully() {
      // Given
      Long userId = 1L;
      String name = "John";
      String surname = "Doe";
      String patronymic = "Smith";
      String identifier = "PASSPORT-123456";
      CaseSuspectEntity.Status status = CaseSuspectEntity.Status.SUSPECTED;
      Long caseId = 100L;
      String verdict = null;
      String accusation = null;

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));
      when(caseSuspectRepository.save(any(CaseSuspectEntity.class))).thenAnswer(invocation -> {
        CaseSuspectEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      CaseSuspectEntity result = caseSuspectService.addCaseSuspect(userId, name, surname,
          patronymic, identifier, status, caseId, verdict, accusation);

      // Then
      assertNotNull(result);
      assertEquals(1L, result.getId());
      assertEquals(name, result.getName());
      assertEquals(surname, result.getSurname());
      assertEquals(patronymic, result.getPatronymic());
      assertEquals(identifier, result.getIdentifier());
      assertEquals(status, result.getStatus());
      assertEquals(testUser, result.getCreator());
      assertEquals(testUser, result.getUpdator());
      assertEquals(testCase, result.getRelatedCase());
      assertNotNull(result.getCreatedAt());
      assertNotNull(result.getUpdatedAt());

      verify(userRepository, times(1)).findById(userId);
      verify(caseRepository, times(1)).findById(caseId);
      verify(caseSuspectRepository, times(1)).save(any(CaseSuspectEntity.class));
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
      // Given
      Long userId = 999L;
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        caseSuspectService.addCaseSuspect(userId, "John", "Doe", "Smith",
            "PASSPORT-123456", CaseSuspectEntity.Status.SUSPECTED, 100L, null, null);
      });

      assertEquals("Creator user not found", exception.getMessage());
      verify(userRepository, times(1)).findById(userId);
      verify(caseRepository, never()).findById(any());
      verify(caseSuspectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when case not found")
    void shouldThrowExceptionWhenCaseNotFound() {
      // Given
      Long userId = 1L;
      Long caseId = 999L;
      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseRepository.findById(caseId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        caseSuspectService.addCaseSuspect(userId, "John", "Doe", "Smith",
            "PASSPORT-123456", CaseSuspectEntity.Status.SUSPECTED, caseId, null, null);
      });

      assertEquals("Case not found", exception.getMessage());
      verify(userRepository, times(1)).findById(userId);
      verify(caseRepository, times(1)).findById(caseId);
      verify(caseSuspectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set createdAt and updatedAt to same time")
    void shouldSetTimestampsCorrectly() {
      // Given
      Long userId = 1L;
      Long caseId = 100L;

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));

      ArgumentCaptor<CaseSuspectEntity> captor = ArgumentCaptor.forClass(CaseSuspectEntity.class);
      when(caseSuspectRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      caseSuspectService.addCaseSuspect(userId, "John", "Doe", "Smith",
          "PASSPORT-123456", CaseSuspectEntity.Status.SUSPECTED, caseId, null, null);

      // Then
      CaseSuspectEntity captured = captor.getValue();
      assertNotNull(captured.getCreatedAt());
      assertNotNull(captured.getUpdatedAt());
      assertEquals(captured.getCreatedAt(), captured.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle case suspect with verdict and accusation")
    void shouldAddCaseSuspectWithVerdictAndAccusation() {
      // Given
      Long userId = 1L;
      Long caseId = 100L;
      String verdict = "Guilty";
      String accusation = "Theft";

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));
      when(caseSuspectRepository.save(any(CaseSuspectEntity.class))).thenAnswer(invocation -> {
        CaseSuspectEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      CaseSuspectEntity result = caseSuspectService.addCaseSuspect(userId, "John", "Doe", "Smith",
          "PASSPORT-123456", CaseSuspectEntity.Status.ACCUSED, caseId, verdict, accusation);

      // Then
      assertNotNull(result);
      assertEquals(verdict, result.getVerdict());
      assertEquals(accusation, result.getAccusation());
      assertEquals(CaseSuspectEntity.Status.ACCUSED, result.getStatus());
    }
  }

  @Nested
  @DisplayName("updateCaseSuspect() method tests")
  class UpdateCaseSuspectTests {

    @Test
    @DisplayName("Should successfully update case suspect")
    void shouldUpdateCaseSuspectSuccessfully() {
      // Given
      Long suspectId = 1L;
      Long userId = 1L;
      String newName = "Jane";
      String newSurname = "Smith";
      String newPatronymic = "Johnson";
      String newIdentifier = "PASSPORT-789012";
      CaseSuspectEntity.Status newStatus = CaseSuspectEntity.Status.ACCUSED;
      String verdict = "Guilty";
      String accusation = "Robbery";

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseSuspectRepository.findById(suspectId)).thenReturn(Optional.of(testCaseSuspect));
      when(caseSuspectMainInfoMapper.partialUpdate(any(CaseSuspectMainInfoDto.class), any(CaseSuspectEntity.class)))
          .thenReturn(testCaseSuspect);
      when(caseSuspectRepository.update(any(CaseSuspectEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      CaseSuspectEntity result = caseSuspectService.updateCaseSuspect(suspectId, userId,
          newName, newSurname, newPatronymic, newIdentifier, newStatus, verdict, accusation);

      // Then
      assertNotNull(result);
      assertEquals(testUser, result.getUpdator());
      assertNotNull(result.getUpdatedAt());

      verify(userRepository, times(1)).findById(userId);
      verify(caseSuspectRepository, times(1)).findById(suspectId);
      verify(caseSuspectMainInfoMapper, times(1)).partialUpdate(any(CaseSuspectMainInfoDto.class), eq(testCaseSuspect));
      verify(caseSuspectRepository, times(1)).update(any(CaseSuspectEntity.class));
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when user not found on update")
    void shouldThrowExceptionWhenUserNotFoundOnUpdate() {
      // Given
      Long suspectId = 1L;
      Long userId = 999L;
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        caseSuspectService.updateCaseSuspect(suspectId, userId, "Jane", "Smith", "Johnson",
            "PASSPORT-789012", CaseSuspectEntity.Status.ACCUSED, "Guilty", "Robbery");
      });

      assertEquals("User not found", exception.getMessage());
      verify(userRepository, times(1)).findById(userId);
      verify(caseSuspectRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when suspect not found")
    void shouldThrowExceptionWhenSuspectNotFound() {
      // Given
      Long suspectId = 999L;
      Long userId = 1L;
      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseSuspectRepository.findById(suspectId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        caseSuspectService.updateCaseSuspect(suspectId, userId, "Jane", "Smith", "Johnson",
            "PASSPORT-789012", CaseSuspectEntity.Status.ACCUSED, "Guilty", "Robbery");
      });

      assertEquals("Case suspect not found", exception.getMessage());
      verify(userRepository, times(1)).findById(userId);
      verify(caseSuspectRepository, times(1)).findById(suspectId);
      verify(caseSuspectRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should update updatedAt timestamp")
    void shouldUpdateTimestamp() {
      // Given
      Long suspectId = 1L;
      Long userId = 1L;
      Instant oldUpdatedAt = testCaseSuspect.getUpdatedAt();

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseSuspectRepository.findById(suspectId)).thenReturn(Optional.of(testCaseSuspect));
      when(caseSuspectMainInfoMapper.partialUpdate(any(CaseSuspectMainInfoDto.class), any(CaseSuspectEntity.class)))
          .thenReturn(testCaseSuspect);

      ArgumentCaptor<CaseSuspectEntity> captor = ArgumentCaptor.forClass(CaseSuspectEntity.class);
      when(caseSuspectRepository.update(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      caseSuspectService.updateCaseSuspect(suspectId, userId, "Jane", "Smith", "Johnson",
          "PASSPORT-789012", CaseSuspectEntity.Status.ACCUSED, "Guilty", "Robbery");

      // Then
      CaseSuspectEntity captured = captor.getValue();
      assertNotNull(captured.getUpdatedAt());
      assertTrue(captured.getUpdatedAt().isAfter(oldUpdatedAt) || captured.getUpdatedAt().equals(oldUpdatedAt));
    }
  }

  @Nested
  @DisplayName("getCaseSuspect() method tests")
  class GetCaseSuspectTests {

    @Test
    @DisplayName("Should successfully get case suspect by id")
    void shouldGetCaseSuspectSuccessfully() {
      // Given
      Long suspectId = 1L;
      when(caseSuspectRepository.findById(suspectId)).thenReturn(Optional.of(testCaseSuspect));

      // When
      CaseSuspectEntity result = caseSuspectService.getCaseSuspect(suspectId);

      // Then
      assertNotNull(result);
      assertEquals(suspectId, result.getId());
      assertEquals(testCaseSuspect.getName(), result.getName());
      assertEquals(testCaseSuspect.getSurname(), result.getSurname());

      verify(caseSuspectRepository, times(1)).findById(suspectId);
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when suspect not found")
    void shouldThrowExceptionWhenSuspectNotFound() {
      // Given
      Long suspectId = 999L;
      when(caseSuspectRepository.findById(suspectId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        caseSuspectService.getCaseSuspect(suspectId);
      });

      assertEquals("Case suspect not found", exception.getMessage());
      verify(caseSuspectRepository, times(1)).findById(suspectId);
    }
  }

  @Nested
  @DisplayName("deleteCaseSuspect() method tests")
  class DeleteCaseSuspectTests {

    @Test
    @DisplayName("Should successfully delete case suspect")
    void shouldDeleteCaseSuspectSuccessfully() {
      // Given
      Long suspectId = 1L;
      doNothing().when(caseSuspectRepository).deleteById(suspectId);

      // When
      caseSuspectService.deleteCaseSuspect(suspectId);

      // Then
      verify(caseSuspectRepository, times(1)).deleteById(suspectId);
    }

    @Test
    @DisplayName("Should handle deletion of non-existent suspect")
    void shouldHandleNonExistentSuspectDeletion() {
      // Given
      Long suspectId = 999L;
      doNothing().when(caseSuspectRepository).deleteById(suspectId);

      // When
      caseSuspectService.deleteCaseSuspect(suspectId);

      // Then
      verify(caseSuspectRepository, times(1)).deleteById(suspectId);
    }
  }

  @Nested
  @DisplayName("getAllCaseSuspectByCaseId() method tests")
  class GetAllCaseSuspectByCaseIdTests {

    @Test
    @DisplayName("Should successfully get all suspects for a case")
    void shouldGetAllSuspectsSuccessfully() {
      // Given
      Long caseId = 100L;
      CaseSuspectEntity suspect2 = new CaseSuspectEntity();
      suspect2.setId(2L);
      suspect2.setName("Jane");
      suspect2.setSurname("Doe");
      suspect2.setIdentifier("PASSPORT-654321");
      suspect2.setStatus(CaseSuspectEntity.Status.JUSTIFIED);
      suspect2.setRelatedCase(testCase);

      List<CaseSuspectEntity> suspects = List.of(testCaseSuspect, suspect2);
      when(caseSuspectRepository.findAllByRelatedCase_Id(caseId)).thenReturn(suspects);

      // When
      List<CaseSuspectEntity> result = caseSuspectService.getAllCaseSuspectByCaseId(caseId);

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
      assertEquals(testCaseSuspect.getId(), result.get(0).getId());
      assertEquals(suspect2.getId(), result.get(1).getId());

      verify(caseSuspectRepository, times(1)).findAllByRelatedCase_Id(caseId);
    }

    @Test
    @DisplayName("Should return empty list when no suspects found for case")
    void shouldReturnEmptyListWhenNoSuspectsFound() {
      // Given
      Long caseId = 100L;
      when(caseSuspectRepository.findAllByRelatedCase_Id(caseId)).thenReturn(List.of());

      // When
      List<CaseSuspectEntity> result = caseSuspectService.getAllCaseSuspectByCaseId(caseId);

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());

      verify(caseSuspectRepository, times(1)).findAllByRelatedCase_Id(caseId);
    }

    @Test
    @DisplayName("Should handle case with multiple suspects")
    void shouldHandleMultipleSuspects() {
      // Given
      Long caseId = 100L;
      CaseSuspectEntity suspect2 = new CaseSuspectEntity();
      suspect2.setId(2L);
      suspect2.setName("Jane");

      CaseSuspectEntity suspect3 = new CaseSuspectEntity();
      suspect3.setId(3L);
      suspect3.setName("Bob");

      List<CaseSuspectEntity> suspects = List.of(testCaseSuspect, suspect2, suspect3);
      when(caseSuspectRepository.findAllByRelatedCase_Id(caseId)).thenReturn(suspects);

      // When
      List<CaseSuspectEntity> result = caseSuspectService.getAllCaseSuspectByCaseId(caseId);

      // Then
      assertNotNull(result);
      assertEquals(3, result.size());
      verify(caseSuspectRepository, times(1)).findAllByRelatedCase_Id(caseId);
    }
  }
}
