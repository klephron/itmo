package ru.itmopai.backend.service;

import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmopai.backend.dto.TemplateMainInfoDto;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.TemplateEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.mapper.TemplateMainInfoMapper;
import ru.itmopai.backend.repository.CaseRepository;
import ru.itmopai.backend.repository.ClueRepository;
import ru.itmopai.backend.repository.TemplateRepository;
import ru.itmopai.backend.repository.UserRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateService Unit Tests")
class TemplateServiceTest {

  @Mock
  private TemplateRepository templateRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CaseRepository caseRepository;

  @Mock
  private TemplateMainInfoMapper templateMainInfoMapper;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private ClueRepository clueRepository;

  @InjectMocks
  private TemplateService templateService;

  private UserEntity testUser;
  private CaseEntity testCase;
  private TemplateEntity testTemplate;
  private String validMetadata;

  @BeforeEach
  void setUp() {
    testUser = new UserEntity();
    testUser.setId(1L);
    testUser.setUsername("testuser");

    testCase = new CaseEntity();
    testCase.setId(100L);
    testCase.setTitle("Test Case");

    validMetadata = "{\"clueIds\":[1,2,3],\"suspectIds\":[10],\"employeeIds\":[5,6]}";

    testTemplate = new TemplateEntity();
    testTemplate.setId(1L);
    testTemplate.setTitle("Test Template");
    testTemplate.setStatus(TemplateEntity.Status.PENDING);
    testTemplate.setMetadata(validMetadata);
    testTemplate.setDescription("Test Description");
    testTemplate.setRelatedCase(testCase);
    testTemplate.setCreator(testUser);
    testTemplate.setUpdator(testUser);
    testTemplate.setCreatedAt(Instant.now());
    testTemplate.setUpdatedAt(Instant.now());
  }

  @Nested
  @DisplayName("addTemplate() method tests")
  class AddTemplateTests {

    @Test
    @DisplayName("Should successfully add a new template")
    void shouldAddTemplateSuccessfully() throws IOException {
      // Given
      Long userId = 1L;
      Long caseId = 100L;
      String title = "New Template";
      String description = "Description";

      TemplateEntity.Metadata metadata = new TemplateEntity.Metadata();
      metadata.setClueIds(List.of(1L, 2L, 3L));
      metadata.setSuspectIds(List.of(10L));
      metadata.setEmployeeIds(List.of(5L, 6L));

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));
      when(objectMapper.readValue(validMetadata, TemplateEntity.Metadata.class)).thenReturn(metadata);
      when(clueRepository.countByRelatedCase_IdAndIdIn(caseId, metadata.getClueIds())).thenReturn(3L);
      when(templateRepository.save(any(TemplateEntity.class))).thenAnswer(invocation -> {
        TemplateEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      TemplateEntity result = templateService.addTemplate(userId, caseId, title, validMetadata, description);

      // Then
      assertNotNull(result);
      assertEquals(1L, result.getId());
      assertEquals(title, result.getTitle());
      assertEquals(TemplateEntity.Status.PENDING, result.getStatus());
      assertEquals(validMetadata, result.getMetadata());
      assertEquals(description, result.getDescription());
      assertEquals(testCase, result.getRelatedCase());
      assertEquals(testUser, result.getCreator());
      assertEquals(testUser, result.getUpdator());
      assertNotNull(result.getCreatedAt());
      assertNotNull(result.getUpdatedAt());

      verify(userRepository, times(1)).findById(userId);
      verify(caseRepository, times(1)).findById(caseId);
      verify(objectMapper, times(1)).readValue(validMetadata, TemplateEntity.Metadata.class);
      verify(clueRepository, times(1)).countByRelatedCase_IdAndIdIn(caseId, metadata.getClueIds());
      verify(templateRepository, times(1)).save(any(TemplateEntity.class));
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
      // Given
      Long userId = 999L;
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        templateService.addTemplate(userId, 100L, "Title", validMetadata, "Description");
      });

      assertEquals("Creator user not found", exception.getMessage());
      verify(userRepository, times(1)).findById(userId);
      verify(templateRepository, never()).save(any());
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
        templateService.addTemplate(userId, caseId, "Title", validMetadata, "Description");
      });

      assertEquals("Case not found", exception.getMessage());
      verify(userRepository, times(1)).findById(userId);
      verify(caseRepository, times(1)).findById(caseId);
      verify(templateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when metadata is invalid JSON")
    void shouldThrowExceptionWhenMetadataIsInvalidJson() throws IOException {
      // Given
      Long userId = 1L;
      Long caseId = 100L;
      String invalidMetadata = "{invalid json}";

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));
      when(objectMapper.readValue(invalidMetadata, TemplateEntity.Metadata.class))
          .thenThrow(new IOException("Invalid JSON"));

      // When & Then
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        templateService.addTemplate(userId, caseId, "Title", invalidMetadata, "Description");
      });

      assertEquals("Can't convert metadata to json", exception.getMessage());
      verify(templateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when clue IDs don't match")
    void shouldThrowExceptionWhenClueIdsDontMatch() throws IOException {
      // Given
      Long userId = 1L;
      Long caseId = 100L;

      TemplateEntity.Metadata metadata = new TemplateEntity.Metadata();
      metadata.setClueIds(List.of(1L, 2L, 3L));

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));
      when(objectMapper.readValue(validMetadata, TemplateEntity.Metadata.class)).thenReturn(metadata);
      when(clueRepository.countByRelatedCase_IdAndIdIn(caseId, metadata.getClueIds())).thenReturn(2L); // Only 2 instead of 3

      // When & Then
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        templateService.addTemplate(userId, caseId, "Title", validMetadata, "Description");
      });

      assertEquals("Check clue's ids", exception.getMessage());
      verify(templateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set createdAt and updatedAt to same time")
    void shouldSetTimestampsCorrectly() throws IOException {
      // Given
      Long userId = 1L;
      Long caseId = 100L;

      TemplateEntity.Metadata metadata = new TemplateEntity.Metadata();
      metadata.setClueIds(List.of(1L));

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));
      when(objectMapper.readValue(anyString(), eq(TemplateEntity.Metadata.class))).thenReturn(metadata);
      when(clueRepository.countByRelatedCase_IdAndIdIn(anyLong(), any())).thenReturn(1L);

      ArgumentCaptor<TemplateEntity> captor = ArgumentCaptor.forClass(TemplateEntity.class);
      when(templateRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      templateService.addTemplate(userId, caseId, "Title", validMetadata, "Description");

      // Then
      TemplateEntity captured = captor.getValue();
      assertNotNull(captured.getCreatedAt());
      assertNotNull(captured.getUpdatedAt());
      assertEquals(captured.getCreatedAt(), captured.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle template without description")
    void shouldAddTemplateWithoutDescription() throws IOException {
      // Given
      Long userId = 1L;
      Long caseId = 100L;

      TemplateEntity.Metadata metadata = new TemplateEntity.Metadata();
      metadata.setClueIds(List.of(1L));

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));
      when(objectMapper.readValue(anyString(), eq(TemplateEntity.Metadata.class))).thenReturn(metadata);
      when(clueRepository.countByRelatedCase_IdAndIdIn(anyLong(), any())).thenReturn(1L);
      when(templateRepository.save(any(TemplateEntity.class))).thenAnswer(invocation -> {
        TemplateEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      TemplateEntity result = templateService.addTemplate(userId, caseId, "Title", validMetadata, null);

      // Then
      assertNotNull(result);
      assertNull(result.getDescription());
    }
  }

  @Nested
  @DisplayName("updateTemplate() method tests")
  class UpdateTemplateTests {

    @Test
    @DisplayName("Should successfully update template")
    void shouldUpdateTemplateSuccessfully() throws IOException {
      // Given
      Long templateId = 1L;
      Long userId = 1L;
      String newTitle = "Updated Title";
      TemplateEntity.Status newStatus = TemplateEntity.Status.CREATED;
      String newDescription = "Updated Description";

      TemplateEntity.Metadata metadata = new TemplateEntity.Metadata();
      metadata.setClueIds(List.of(1L, 2L));

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
      when(objectMapper.readValue(validMetadata, TemplateEntity.Metadata.class)).thenReturn(metadata);
      when(clueRepository.countByRelatedCase_IdAndIdIn(testCase.getId(), metadata.getClueIds())).thenReturn(2L);
      when(templateMainInfoMapper.partialUpdate(any(TemplateMainInfoDto.class), any(TemplateEntity.class)))
          .thenReturn(testTemplate);
      when(templateRepository.update(any(TemplateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      TemplateEntity result = templateService.updateTemplate(userId, templateId,
          newTitle, newStatus, validMetadata, newDescription);

      // Then
      assertNotNull(result);
      assertEquals(testUser, result.getUpdator());
      assertNotNull(result.getUpdatedAt());

      verify(userRepository, times(1)).findById(userId);
      verify(templateRepository, times(1)).findById(templateId);
      verify(objectMapper, times(1)).readValue(validMetadata, TemplateEntity.Metadata.class);
      verify(templateMainInfoMapper, times(1)).partialUpdate(any(TemplateMainInfoDto.class), eq(testTemplate));
      verify(templateRepository, times(1)).update(any(TemplateEntity.class));
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when user not found on update")
    void shouldThrowExceptionWhenUserNotFoundOnUpdate() {
      // Given
      Long templateId = 1L;
      Long userId = 999L;
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        templateService.updateTemplate(userId, templateId, "Title",
            TemplateEntity.Status.PENDING, validMetadata, "Description");
      });

      assertEquals("User not found", exception.getMessage());
      verify(userRepository, times(1)).findById(userId);
      verify(templateRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when template not found")
    void shouldThrowExceptionWhenTemplateNotFound() {
      // Given
      Long templateId = 999L;
      Long userId = 1L;
      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        templateService.updateTemplate(userId, templateId, "Title",
            TemplateEntity.Status.PENDING, validMetadata, "Description");
      });

      assertEquals("Template not found", exception.getMessage());
      verify(userRepository, times(1)).findById(userId);
      verify(templateRepository, times(1)).findById(templateId);
      verify(templateRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when metadata is invalid on update")
    void shouldThrowExceptionWhenMetadataIsInvalidOnUpdate() throws IOException {
      // Given
      Long templateId = 1L;
      Long userId = 1L;
      String invalidMetadata = "{invalid}";

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
      when(objectMapper.readValue(invalidMetadata, TemplateEntity.Metadata.class))
          .thenThrow(new IOException("Invalid JSON"));

      // When & Then
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        templateService.updateTemplate(userId, templateId, "Title",
            TemplateEntity.Status.PENDING, invalidMetadata, "Description");
      });

      assertEquals("Can't convert metadata to json", exception.getMessage());
      verify(templateRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should update updatedAt timestamp")
    void shouldUpdateTimestamp() throws IOException {
      // Given
      Long templateId = 1L;
      Long userId = 1L;
      Instant oldUpdatedAt = testTemplate.getUpdatedAt();

      TemplateEntity.Metadata metadata = new TemplateEntity.Metadata();
      metadata.setClueIds(List.of(1L));

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
      when(objectMapper.readValue(anyString(), eq(TemplateEntity.Metadata.class))).thenReturn(metadata);
      when(clueRepository.countByRelatedCase_IdAndIdIn(anyLong(), any())).thenReturn(1L);
      when(templateMainInfoMapper.partialUpdate(any(TemplateMainInfoDto.class), any(TemplateEntity.class)))
          .thenReturn(testTemplate);

      ArgumentCaptor<TemplateEntity> captor = ArgumentCaptor.forClass(TemplateEntity.class);
      when(templateRepository.update(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      templateService.updateTemplate(userId, templateId, "Title",
          TemplateEntity.Status.PENDING, validMetadata, "Description");

      // Then
      TemplateEntity captured = captor.getValue();
      assertNotNull(captured.getUpdatedAt());
      assertTrue(captured.getUpdatedAt().isAfter(oldUpdatedAt) || captured.getUpdatedAt().equals(oldUpdatedAt));
    }
  }

  @Nested
  @DisplayName("getTemplate() method tests")
  class GetTemplateTests {

    @Test
    @DisplayName("Should successfully get template by id")
    void shouldGetTemplateSuccessfully() {
      // Given
      Long templateId = 1L;
      when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));

      // When
      TemplateEntity result = templateService.getTemplate(templateId);

      // Then
      assertNotNull(result);
      assertEquals(templateId, result.getId());
      assertEquals(testTemplate.getTitle(), result.getTitle());
      assertEquals(testTemplate.getStatus(), result.getStatus());

      verify(templateRepository, times(1)).findById(templateId);
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when template not found")
    void shouldThrowExceptionWhenTemplateNotFound() {
      // Given
      Long templateId = 999L;
      when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        templateService.getTemplate(templateId);
      });

      assertEquals("Template not found", exception.getMessage());
      verify(templateRepository, times(1)).findById(templateId);
    }
  }

  @Nested
  @DisplayName("deleteTemplate() method tests")
  class DeleteTemplateTests {

    @Test
    @DisplayName("Should successfully delete template")
    void shouldDeleteTemplateSuccessfully() {
      // Given
      Long templateId = 1L;
      doNothing().when(templateRepository).deleteById(templateId);

      // When
      templateService.deleteTemplate(templateId);

      // Then
      verify(templateRepository, times(1)).deleteById(templateId);
    }

    @Test
    @DisplayName("Should handle deletion of non-existent template")
    void shouldHandleNonExistentTemplateDeletion() {
      // Given
      Long templateId = 999L;
      doNothing().when(templateRepository).deleteById(templateId);

      // When
      templateService.deleteTemplate(templateId);

      // Then
      verify(templateRepository, times(1)).deleteById(templateId);
    }
  }

  @Nested
  @DisplayName("getTemplatesByCaseId() method tests")
  class GetTemplatesByCaseIdTests {

    @Test
    @DisplayName("Should successfully get all templates for a case")
    void shouldGetAllTemplatesSuccessfully() {
      // Given
      Long caseId = 100L;
      TemplateEntity template2 = new TemplateEntity();
      template2.setId(2L);
      template2.setTitle("Template 2");
      template2.setStatus(TemplateEntity.Status.CREATED);

      List<TemplateEntity> templates = List.of(testTemplate, template2);
      when(templateRepository.findAllByRelatedCase_Id(caseId)).thenReturn(templates);

      // When
      List<TemplateEntity> result = templateService.getTemplatesByCaseId(caseId);

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
      assertEquals(testTemplate.getId(), result.get(0).getId());
      assertEquals(template2.getId(), result.get(1).getId());

      verify(templateRepository, times(1)).findAllByRelatedCase_Id(caseId);
    }

    @Test
    @DisplayName("Should return empty list when no templates found for case")
    void shouldReturnEmptyListWhenNoTemplatesFound() {
      // Given
      Long caseId = 100L;
      when(templateRepository.findAllByRelatedCase_Id(caseId)).thenReturn(List.of());

      // When
      List<TemplateEntity> result = templateService.getTemplatesByCaseId(caseId);

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());

      verify(templateRepository, times(1)).findAllByRelatedCase_Id(caseId);
    }

    @Test
    @DisplayName("Should handle case with multiple templates")
    void shouldHandleMultipleTemplates() {
      // Given
      Long caseId = 100L;
      TemplateEntity template2 = new TemplateEntity();
      template2.setId(2L);
      template2.setTitle("Template 2");

      TemplateEntity template3 = new TemplateEntity();
      template3.setId(3L);
      template3.setTitle("Template 3");

      List<TemplateEntity> templates = List.of(testTemplate, template2, template3);
      when(templateRepository.findAllByRelatedCase_Id(caseId)).thenReturn(templates);

      // When
      List<TemplateEntity> result = templateService.getTemplatesByCaseId(caseId);

      // Then
      assertNotNull(result);
      assertEquals(3, result.size());
      verify(templateRepository, times(1)).findAllByRelatedCase_Id(caseId);
    }
  }
}
