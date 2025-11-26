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
import ru.itmopai.backend.dto.IncidentMainInfoDto;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.IncidentEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.mapper.IncidentMainInfoMapper;
import ru.itmopai.backend.mapper.IncidentMapper;
import ru.itmopai.backend.repository.IncidentRepository;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncidentService Unit Tests")
class IncidentServiceTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private UserService userService;

  @Mock
  private IncidentMainInfoMapper incidentMainInfoMapper;

  @Mock
  private IncidentMapper incidentMapper;

  @InjectMocks
  private IncidentService incidentService;

  private UserEntity testUser;
  private IncidentEntity testIncident;

  @BeforeEach
  void setUp() {
    testUser = new UserEntity();
    testUser.setId(1L);
    testUser.setUsername("testuser");

    testIncident = new IncidentEntity();
    testIncident.setId(1L);
    testIncident.setApplicant("John Doe");
    testIncident.setType("Robbery");
    testIncident.setReason("Armed robbery at store");
    testIncident.setLocationLatitude(55.7558);
    testIncident.setLocationLongitude(37.6173);
    testIncident.setLocationAltitude(150.0);
    testIncident.setEstimatedTime(Instant.now());
    testIncident.setCreator(testUser);
    testIncident.setUpdator(testUser);
    testIncident.setCreatedAt(Instant.now());
    testIncident.setUpdatedAt(Instant.now());
  }

  @Nested
  @DisplayName("addIncident() method tests")
  class AddIncidentTests {

    @Test
    @DisplayName("Should successfully add a new incident")
    void shouldAddIncidentSuccessfully() {
      // Given
      Long userId = 1L;
      String applicant = "John Doe";
      String type = "Robbery";
      String reason = "Armed robbery at store";
      Double lat = 55.7558;
      Double lon = 37.6173;
      Double alt = 150.0;
      Instant estimatedTime = Instant.now();

      when(userService.getUser(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.save(any(IncidentEntity.class))).thenAnswer(invocation -> {
        IncidentEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      IncidentEntity result = incidentService.addIncident(userId, applicant, type, reason,
          lat, lon, alt, estimatedTime);

      // Then
      assertNotNull(result);
      assertEquals(1L, result.getId());
      assertEquals(applicant, result.getApplicant());
      assertEquals(type, result.getType());
      assertEquals(reason, result.getReason());
      assertEquals(lat, result.getLocationLatitude());
      assertEquals(lon, result.getLocationLongitude());
      assertEquals(alt, result.getLocationAltitude());
      assertEquals(estimatedTime, result.getEstimatedTime());
      assertEquals(testUser, result.getCreator());
      assertEquals(testUser, result.getUpdator());
      assertNotNull(result.getCreatedAt());
      assertNotNull(result.getUpdatedAt());

      verify(userService, times(1)).getUser(userId);
      verify(incidentRepository, times(1)).save(any(IncidentEntity.class));
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
      // Given
      Long userId = 999L;
      when(userService.getUser(userId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        incidentService.addIncident(userId, "Applicant", "Type", "Reason",
            55.0, 37.0, 150.0, Instant.now());
      });

      assertEquals("User Not Found", exception.getMessage());
      verify(userService, times(1)).getUser(userId);
      verify(incidentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set createdAt and updatedAt to same time")
    void shouldSetTimestampsCorrectly() {
      // Given
      Long userId = 1L;
      when(userService.getUser(userId)).thenReturn(Optional.of(testUser));

      ArgumentCaptor<IncidentEntity> captor = ArgumentCaptor.forClass(IncidentEntity.class);
      when(incidentRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      incidentService.addIncident(userId, "Applicant", "Type", "Reason",
          55.0, 37.0, 150.0, Instant.now());

      // Then
      IncidentEntity captured = captor.getValue();
      assertNotNull(captured.getCreatedAt());
      assertNotNull(captured.getUpdatedAt());
      assertEquals(captured.getCreatedAt(), captured.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle incident without applicant")
    void shouldAddIncidentWithoutApplicant() {
      // Given
      Long userId = 1L;
      when(userService.getUser(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.save(any(IncidentEntity.class))).thenAnswer(invocation -> {
        IncidentEntity saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      IncidentEntity result = incidentService.addIncident(userId, null, "Type", "Reason",
          55.0, 37.0, 150.0, Instant.now());

      // Then
      assertNotNull(result);
      assertNull(result.getApplicant());
    }
  }

  @Nested
  @DisplayName("updateIncident() method tests")
  class UpdateIncidentTests {

    @Test
    @DisplayName("Should successfully update incident")
    void shouldUpdateIncidentSuccessfully() {
      // Given
      Long incidentId = 1L;
      Long userId = 1L;
      String newApplicant = "Jane Smith";
      String newType = "Theft";
      String newReason = "Shoplifting";
      Double newLat = 56.0;
      Double newLon = 38.0;
      Double newAlt = 200.0;
      Instant newTime = Instant.now();

      when(userService.getUser(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(testIncident));
      when(incidentMainInfoMapper.partialUpdate(any(IncidentMainInfoDto.class), any(IncidentEntity.class)))
          .thenReturn(testIncident);
      when(incidentRepository.update(any(IncidentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      IncidentEntity result = incidentService.updateIncident(userId, incidentId,
          newApplicant, newType, newReason, newLat, newLon, newAlt, newTime);

      // Then
      assertNotNull(result);
      assertEquals(testUser, result.getUpdator());
      assertNotNull(result.getUpdatedAt());

      verify(userService, times(1)).getUser(userId);
      verify(incidentRepository, times(1)).findById(incidentId);
      verify(incidentMainInfoMapper, times(1)).partialUpdate(any(IncidentMainInfoDto.class), eq(testIncident));
      verify(incidentRepository, times(1)).update(any(IncidentEntity.class));
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when user not found on update")
    void shouldThrowExceptionWhenUserNotFoundOnUpdate() {
      // Given
      Long incidentId = 1L;
      Long userId = 999L;
      when(userService.getUser(userId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        incidentService.updateIncident(userId, incidentId, "Applicant", "Type", "Reason",
            55.0, 37.0, 150.0, Instant.now());
      });

      assertEquals("User Not Found", exception.getMessage());
      verify(userService, times(1)).getUser(userId);
      verify(incidentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when incident not found")
    void shouldThrowExceptionWhenIncidentNotFound() {
      // Given
      Long incidentId = 999L;
      Long userId = 1L;
      when(userService.getUser(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        incidentService.updateIncident(userId, incidentId, "Applicant", "Type", "Reason",
            55.0, 37.0, 150.0, Instant.now());
      });

      assertEquals("Incident Not Found", exception.getMessage());
      verify(userService, times(1)).getUser(userId);
      verify(incidentRepository, times(1)).findById(incidentId);
      verify(incidentRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should update updatedAt timestamp")
    void shouldUpdateTimestamp() {
      // Given
      Long incidentId = 1L;
      Long userId = 1L;
      Instant oldUpdatedAt = testIncident.getUpdatedAt();

      when(userService.getUser(userId)).thenReturn(Optional.of(testUser));
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(testIncident));
      when(incidentMainInfoMapper.partialUpdate(any(IncidentMainInfoDto.class), any(IncidentEntity.class)))
          .thenReturn(testIncident);

      ArgumentCaptor<IncidentEntity> captor = ArgumentCaptor.forClass(IncidentEntity.class);
      when(incidentRepository.update(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

      // When
      incidentService.updateIncident(userId, incidentId, "Applicant", "Type", "Reason",
          55.0, 37.0, 150.0, Instant.now());

      // Then
      IncidentEntity captured = captor.getValue();
      assertNotNull(captured.getUpdatedAt());
      assertTrue(captured.getUpdatedAt().isAfter(oldUpdatedAt) || captured.getUpdatedAt().equals(oldUpdatedAt));
    }
  }

  @Nested
  @DisplayName("getIncident() method tests")
  class GetIncidentTests {

    @Test
    @DisplayName("Should successfully get incident by id")
    void shouldGetIncidentSuccessfully() {
      // Given
      Long incidentId = 1L;
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(testIncident));

      // When
      IncidentEntity result = incidentService.getIncident(incidentId);

      // Then
      assertNotNull(result);
      assertEquals(incidentId, result.getId());
      assertEquals(testIncident.getType(), result.getType());
      assertEquals(testIncident.getReason(), result.getReason());

      verify(incidentRepository, times(1)).findById(incidentId);
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when incident not found")
    void shouldThrowExceptionWhenIncidentNotFound() {
      // Given
      Long incidentId = 999L;
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        incidentService.getIncident(incidentId);
      });

      assertEquals("Incident Not Found", exception.getMessage());
      verify(incidentRepository, times(1)).findById(incidentId);
    }
  }

  @Nested
  @DisplayName("getAllIncidents() method tests")
  class GetAllIncidentsTests {

    @Test
    @DisplayName("Should successfully get all incidents")
    void shouldGetAllIncidentsSuccessfully() {
      // Given
      IncidentEntity incident2 = new IncidentEntity();
      incident2.setId(2L);
      incident2.setType("Theft");
      incident2.setReason("Car theft");

      List<IncidentEntity> incidents = List.of(testIncident, incident2);
      when(incidentRepository.findAll()).thenReturn(incidents);

      // When
      List<IncidentEntity> result = incidentService.getAllIncidents();

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
      assertEquals(testIncident.getId(), result.get(0).getId());
      assertEquals(incident2.getId(), result.get(1).getId());

      verify(incidentRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no incidents found")
    void shouldReturnEmptyListWhenNoIncidentsFound() {
      // Given
      when(incidentRepository.findAll()).thenReturn(List.of());

      // When
      List<IncidentEntity> result = incidentService.getAllIncidents();

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());

      verify(incidentRepository, times(1)).findAll();
    }
  }

  @Nested
  @DisplayName("deleteIncident() method tests")
  class DeleteIncidentTests {

    @Test
    @DisplayName("Should successfully delete incident without related case")
    void shouldDeleteIncidentSuccessfully() {
      // Given
      Long incidentId = 1L;
      testIncident.setRelatedCase(null);
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(testIncident));
      doNothing().when(incidentRepository).delete(testIncident);

      // When
      incidentService.deleteIncident(incidentId);

      // Then
      verify(incidentRepository, times(1)).findById(incidentId);
      verify(incidentRepository, times(1)).delete(testIncident);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when incident has related case")
    void shouldThrowExceptionWhenIncidentHasRelatedCase() {
      // Given
      Long incidentId = 1L;
      CaseEntity relatedCase = new CaseEntity();
      relatedCase.setId(100L);
      testIncident.setRelatedCase(relatedCase);
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(testIncident));

      // When & Then
      IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
        incidentService.deleteIncident(incidentId);
      });

      assertEquals("Incident has related case", exception.getMessage());
      verify(incidentRepository, times(1)).findById(incidentId);
      verify(incidentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when incident not found")
    void shouldThrowExceptionWhenIncidentNotFound() {
      // Given
      Long incidentId = 999L;
      when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

      // When & Then
      NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
        incidentService.deleteIncident(incidentId);
      });

      assertEquals("Incident Not Found", exception.getMessage());
      verify(incidentRepository, times(1)).findById(incidentId);
      verify(incidentRepository, never()).delete(any());
    }
  }
}
