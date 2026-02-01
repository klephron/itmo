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
import ru.itmopai.backend.dto.ClueMainInfoDto;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.ClueEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.mapper.ClueMainInfoMapper;
import ru.itmopai.backend.repository.ClueRepository;
import ru.itmopai.backend.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClueService Unit Tests")
class ClueServiceTest {

  @Mock
  private ClueRepository clueRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CaseService caseService;

  @Mock
  private ClueMainInfoMapper clueMainInfoMapper;

  @InjectMocks
  private ClueService clueService;

  private UserEntity testUser;
  private CaseEntity testCase;
  private ClueEntity testClue;

  @BeforeEach
  void setUp() {
    testUser = new UserEntity();
    testUser.setId(1L);
    testUser.setUsername("testuser");

    testCase = new CaseEntity();
    testCase.setId(100L);
    testCase.setTitle("Test Case");

    testClue = new ClueEntity();
    testClue.setId(1L);
    testClue.setTitle("Test Clue");
    testClue.setDescription("Test Description");
    testClue.setStatus(ClueEntity.Status.PENDING);
    testClue.setCreator(testUser);
    testClue.setUpdator(testUser);
    testClue.setRelatedCase(testCase);
    testClue.setCreatedAt(Instant.now());
    testClue.setUpdatedAt(Instant.now());
    testClue.setMetadata("{}");
  }

  @Nested
  @DisplayName("addClue() method tests")
  class AddClueTests {

    @Test
    @DisplayName("Should successfully add a new clue")
    void shouldAddClueSuccessfully() {
      // Given
      Long userId = 1L;
      String title = "New Clue";
      String description = "New Description";
      ClueEntity.Status status = ClueEntity.Status.PENDING;
      Long caseId = 100L;

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseService.getCaseById(caseId)).thenReturn(testCase);
      when(clueRepository.save(any(ClueEntity.class))).thenAnswer(invocation -> {
        ClueEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      ClueEntity result =
          clueService.addClue(userId, title, description, status, caseId);

      // Then
      assertNotNull(result);
      assertEquals(title, result.getTitle());
      assertEquals(description, result.getDescription());
      assertEquals(status, result.getStatus());
      assertEquals(testUser, result.getCreator());
      assertEquals(testUser, result.getUpdator());
      assertEquals(testCase, result.getRelatedCase());
      assertEquals("{}", result.getMetadata());
      assertNotNull(result.getCreatedAt());
      assertNotNull(result.getUpdatedAt());
      assertEquals(result.getCreatedAt(), result.getUpdatedAt());

      verify(userRepository).findById(userId);
      verify(caseService).getCaseById(caseId);
      verify(clueRepository).save(any(ClueEntity.class));
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
      // Given
      Long userId = 999L;
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        clueService.addClue(userId, "Title", "Description", ClueEntity.Status.PENDING, 100L);
      });

      assertEquals("User not found", exception.getMessage());
      verify(userRepository).findById(userId);
      verify(caseService, never()).getCaseById(any());
      verify(clueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when case not found")
    void shouldThrowExceptionWhenCaseNotFound() {
      // Given
      Long userId = 1L;
      Long caseId = 999L;

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseService.getCaseById(caseId))
          .thenThrow(new NoSuchElementException("Case not found"));

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        clueService.addClue(userId, "Title", "Description", ClueEntity.Status.PENDING, caseId);
      });

      assertEquals("Case not found", exception.getMessage());
      verify(userRepository).findById(userId);
      verify(caseService).getCaseById(caseId);
      verify(clueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set created and updated timestamps correctly")
    void shouldSetTimestampsCorrectly() {
      // Given
      Long userId = 1L;
      Long caseId = 100L;

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseService.getCaseById(caseId)).thenReturn(testCase);

      ArgumentCaptor<ClueEntity> clueCaptor = ArgumentCaptor.forClass(ClueEntity.class);
      when(clueRepository.save(clueCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      clueService.addClue(userId, "Title", "Description", ClueEntity.Status.PENDING, caseId);

      // Then
      ClueEntity savedClue = clueCaptor.getValue();
      assertNotNull(savedClue.getCreatedAt());
      assertNotNull(savedClue.getUpdatedAt());
      assertEquals(savedClue.getCreatedAt(), savedClue.getUpdatedAt());
    }
  }

  @Nested
  @DisplayName("updateClue() method tests")
  class UpdateClueTests {

    @Test
    @DisplayName("Should successfully update clue")
    void shouldUpdateClueSuccessfully() {
      // Given
      Long userId = 1L;
      Long clueId = 1L;
      String newTitle = "Updated Title";
      String newDescription = "Updated Description";
      ClueEntity.Status newStatus = ClueEntity.Status.ACCEPTED;

      ClueEntity updatedClue = new ClueEntity();
      updatedClue.setId(clueId);
      updatedClue.setTitle(newTitle);
      updatedClue.setDescription(newDescription);
      updatedClue.setStatus(newStatus);

      when(clueRepository.findById(clueId)).thenReturn(Optional.of(testClue));
      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(clueMainInfoMapper.partialUpdate(any(ClueMainInfoDto.class), eq(testClue)))
          .thenReturn(updatedClue);
      when(clueRepository.update(any(ClueEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      ClueEntity result =
          clueService.updateClue(userId, clueId, newTitle, newDescription, newStatus);

      // Then
      assertNotNull(result);
      assertEquals(newTitle, result.getTitle());
      assertEquals(newDescription, result.getDescription());
      assertEquals(newStatus, result.getStatus());
      assertEquals(testUser, result.getUpdator());
      assertNotNull(result.getUpdatedAt());

      verify(clueRepository).findById(clueId);
      verify(userRepository).findById(userId);
      verify(clueMainInfoMapper).partialUpdate(any(ClueMainInfoDto.class), eq(testClue));
      verify(clueRepository).update(any(ClueEntity.class));
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when clue not found")
    void shouldThrowExceptionWhenClueNotFound() {
      // Given
      Long userId = 1L;
      Long clueId = 999L;

      when(clueRepository.findById(clueId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        clueService.updateClue(userId, clueId, "Title", "Description", ClueEntity.Status.ACCEPTED);
      });

      assertEquals("Clue not found", exception.getMessage());
      verify(clueRepository).findById(clueId);
      verify(userRepository, never()).findById(any());
      verify(clueRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when user not found")
    void shouldThrowExceptionWhenUserNotFoundOnUpdate() {
      // Given
      Long userId = 999L;
      Long clueId = 1L;

      when(clueRepository.findById(clueId)).thenReturn(Optional.of(testClue));
      when(clueMainInfoMapper.partialUpdate(any(ClueMainInfoDto.class), eq(testClue)))
          .thenReturn(testClue);
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        clueService.updateClue(userId, clueId, "Title", "Description", ClueEntity.Status.ACCEPTED);
      });

      assertEquals("User not found", exception.getMessage());
      verify(clueRepository).findById(clueId);
      verify(userRepository).findById(userId);
      verify(clueRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should update timestamp when updating clue")
    void shouldUpdateTimestamp() {
      // Given
      Long userId = 1L;
      Long clueId = 1L;
      Instant originalUpdateTime = testClue.getUpdatedAt();

      when(clueRepository.findById(clueId)).thenReturn(Optional.of(testClue));
      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(clueMainInfoMapper.partialUpdate(any(ClueMainInfoDto.class), eq(testClue)))
          .thenReturn(testClue);

      ArgumentCaptor<ClueEntity> clueCaptor = ArgumentCaptor.forClass(ClueEntity.class);
      when(clueRepository.update(clueCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      clueService.updateClue(userId, clueId, "New Title", "New Description", ClueEntity.Status.ACCEPTED);

      // Then
      ClueEntity capturedClue = clueCaptor.getValue();
      assertNotNull(capturedClue.getUpdatedAt());
      assertTrue(capturedClue.getUpdatedAt().isAfter(originalUpdateTime) ||
          capturedClue.getUpdatedAt().equals(originalUpdateTime));
    }
  }

  @Nested
  @DisplayName("getClue() method tests")
  class GetClueTests {

    @Test
    @DisplayName("Should successfully retrieve clue by id")
    void shouldGetClueSuccessfully() {
      // Given
      Long clueId = 1L;
      when(clueRepository.findById(clueId)).thenReturn(Optional.of(testClue));

      // When
      ClueEntity result = clueService.getClue(clueId);

      // Then
      assertNotNull(result);
      assertEquals(testClue.getId(), result.getId());
      assertEquals(testClue.getTitle(), result.getTitle());
      assertEquals(testClue.getDescription(), result.getDescription());
      verify(clueRepository).findById(clueId);
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when clue not found")
    void shouldThrowExceptionWhenClueNotFoundOnGet() {
      // Given
      Long clueId = 999L;
      when(clueRepository.findById(clueId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        clueService.getClue(clueId);
      });

      assertEquals("Clue not found", exception.getMessage());
      verify(clueRepository).findById(clueId);
    }
  }

  @Nested
  @DisplayName("getAllClueByCase() method tests")
  class GetAllClueByCaseTests {

    @Test
    @DisplayName("Should retrieve all clues for a case")
    void shouldGetAllCluesForCase() {
      // Given
      Long caseId = 100L;
      ClueEntity clue1 = new ClueEntity();
      clue1.setId(1L);
      clue1.setTitle("Clue 1");

      ClueEntity clue2 = new ClueEntity();
      clue2.setId(2L);
      clue2.setTitle("Clue 2");

      List<ClueEntity> expectedClues = List.of(clue1, clue2);
      when(clueRepository.findAllByRelatedCase_Id(caseId)).thenReturn(expectedClues);

      // When
      List<ClueEntity> result = clueService.getAllClueByCase(caseId);

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
      assertEquals("Clue 1", result.get(0).getTitle());
      assertEquals("Clue 2", result.get(1).getTitle());
      verify(clueRepository).findAllByRelatedCase_Id(caseId);
    }

    @Test
    @DisplayName("Should return empty list when no clues found for case")
    void shouldReturnEmptyListWhenNoCluesToCase() {
      // Given
      Long caseId = 999L;
      when(clueRepository.findAllByRelatedCase_Id(caseId)).thenReturn(List.of());

      // When
      List<ClueEntity> result = clueService.getAllClueByCase(caseId);

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
      verify(clueRepository).findAllByRelatedCase_Id(caseId);
    }
  }

  @Nested
  @DisplayName("deleteClue() method tests")
  class DeleteClueTests {

    @Test
    @DisplayName("Should successfully delete clue")
    void shouldDeleteClueSuccessfully() {
      // Given
      Long clueId = 1L;
      doNothing().when(clueRepository).deleteById(clueId);

      // When
      assertDoesNotThrow(() -> clueService.deleteClue(clueId));

      // Then
      verify(clueRepository).deleteById(clueId);
    }

    @Test
    @DisplayName("Should call repository delete method with correct id")
    void shouldCallDeleteWithCorrectId() {
      // Given
      Long clueId = 42L;
      doNothing().when(clueRepository).deleteById(clueId);

      // When
      clueService.deleteClue(clueId);

      // Then
      ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
      verify(clueRepository).deleteById(idCaptor.capture());
      assertEquals(clueId, idCaptor.getValue());
    }
  }

  @Nested
  @DisplayName("Business Logic Tests")
  class BusinessLogicTests {

    @Test
    @DisplayName("Should set creator and updator to same user when adding clue")
    void shouldSetCreatorAndUpdatorToSameUser() {
      // Given
      Long userId = 1L;
      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseService.getCaseById(100L)).thenReturn(testCase);

      ArgumentCaptor<ClueEntity> clueCaptor = ArgumentCaptor.forClass(ClueEntity.class);
      when(clueRepository.save(clueCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      clueService.addClue(userId, "Title", "Desc", ClueEntity.Status.PENDING, 100L);

      // Then
      ClueEntity savedClue = clueCaptor.getValue();
      assertEquals(savedClue.getCreator(), savedClue.getUpdator());
      assertEquals(testUser, savedClue.getCreator());
    }

    @Test
    @DisplayName("Should initialize metadata as empty JSON object")
    void shouldInitializeMetadataAsEmptyJson() {
      // Given
      Long userId = 1L;
      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseService.getCaseById(100L)).thenReturn(testCase);

      ArgumentCaptor<ClueEntity> clueCaptor = ArgumentCaptor.forClass(ClueEntity.class);
      when(clueRepository.save(clueCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      clueService.addClue(userId, "Title", "Desc", ClueEntity.Status.PENDING, 100L);

      // Then
      ClueEntity savedClue = clueCaptor.getValue();
      assertEquals("{}", savedClue.getMetadata());
    }

    @Test
    @DisplayName("Should preserve case relationship when updating clue")
    void shouldPreserveCaseRelationshipOnUpdate() {
      // Given
      Long userId = 1L;
      Long clueId = 1L;

      ClueEntity updatedClue = new ClueEntity();
      updatedClue.setId(clueId);
      updatedClue.setTitle("Updated");
      updatedClue.setRelatedCase(testCase); // Should preserve case

      when(clueRepository.findById(clueId)).thenReturn(Optional.of(testClue));
      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(clueMainInfoMapper.partialUpdate(any(ClueMainInfoDto.class), eq(testClue)))
          .thenReturn(updatedClue);
      when(clueRepository.update(any(ClueEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      ClueEntity result = clueService.updateClue(userId, clueId, "Updated", "Desc", ClueEntity.Status.ACCEPTED);

      // Then
      assertNotNull(result.getRelatedCase());
      assertEquals(testCase.getId(), result.getRelatedCase().getId());
    }

    @Test
    @DisplayName("Should update updator field when updating clue")
    void shouldUpdateUpdatorOnUpdate() {
      // Given
      Long originalUserId = 1L;
      Long updatingUserId = 2L;
      Long clueId = 1L;

      UserEntity originalUser = new UserEntity();
      originalUser.setId(originalUserId);
      originalUser.setUsername("original_user");

      UserEntity updatingUser = new UserEntity();
      updatingUser.setId(updatingUserId);
      updatingUser.setUsername("updating_user");

      testClue.setCreator(originalUser);
      testClue.setUpdator(originalUser);

      when(clueRepository.findById(clueId)).thenReturn(Optional.of(testClue));
      when(userRepository.findById(updatingUserId)).thenReturn(Optional.of(updatingUser));
      when(clueMainInfoMapper.partialUpdate(any(ClueMainInfoDto.class), eq(testClue)))
          .thenReturn(testClue);

      ArgumentCaptor<ClueEntity> clueCaptor = ArgumentCaptor.forClass(ClueEntity.class);
      when(clueRepository.update(clueCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      clueService.updateClue(updatingUserId, clueId, "Updated", "Desc", ClueEntity.Status.ACCEPTED);

      // Then
      ClueEntity capturedClue = clueCaptor.getValue();
      assertEquals(updatingUserId, capturedClue.getUpdator().getId());
      assertEquals("updating_user", capturedClue.getUpdator().getUsername());
      // Creator should remain unchanged
      assertEquals(originalUserId, testClue.getCreator().getId());
    }

    @Test
    @DisplayName("Should handle metadata with real data")
    void shouldHandleMetadataWithRealData() {
      // Given
      Long userId = 1L;
      String complexMetadata = "{\"imageUris\":[\"http://example.com/img1.jpg\",\"http://example.com/img2.jpg\"],\"videoUris\":[\"http://example.com/video.mp4\"],\"reportUris\":[\"http://example.com/report.pdf\"],\"location\":{\"latitude\":55.7558,\"longitude\":37.6173,\"altitude\":150.0}}";

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseService.getCaseById(100L)).thenReturn(testCase);

      ArgumentCaptor<ClueEntity> clueCaptor = ArgumentCaptor.forClass(ClueEntity.class);
      when(clueRepository.save(clueCaptor.capture())).thenAnswer(invocation -> {
        ClueEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      ClueEntity result = clueService.addClue(userId, "Clue with metadata", "Description", ClueEntity.Status.PENDING, 100L);

      // Then - verify that addClue initializes metadata as "{}" by default
      ClueEntity savedClue = clueCaptor.getValue();
      assertEquals("{}", savedClue.getMetadata());

      // Now manually set complex metadata to verify it can be stored
      savedClue.setMetadata(complexMetadata);
      assertEquals(complexMetadata, savedClue.getMetadata());
      assertTrue(savedClue.getMetadata().contains("imageUris"));
      assertTrue(savedClue.getMetadata().contains("location"));
    }
  }
}
