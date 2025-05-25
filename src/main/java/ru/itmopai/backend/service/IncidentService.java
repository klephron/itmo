package ru.itmopai.backend.service;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.IncidentDto;
import ru.itmopai.backend.dto.IncidentMainInfoDto;
import ru.itmopai.backend.entity.IncidentEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.mapper.IncidentMainInfoMapper;
import ru.itmopai.backend.mapper.IncidentMapper;
import ru.itmopai.backend.repository.IncidentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Singleton
@AllArgsConstructor
public class IncidentService {
  private final IncidentRepository incidentRepository;
  private final UserService userService;
  private final IncidentMainInfoMapper incidentMainInfoMapper;
  private final IncidentMapper incidentMapper;

  public IncidentEntity addIncident(Long userId, String applicant, String type, String reason,
      Double locationLatitude, Double locationLongitude, Double locationAltitude,
      LocalDateTime estimatedTime) throws NoSuchElementException {
    IncidentEntity incidentEntity = new IncidentEntity();
    incidentEntity.setApplicant(applicant);
    incidentEntity.setType(type);
    incidentEntity.setReason(reason);
    incidentEntity.setLocationLatitude(locationLatitude);
    incidentEntity.setLocationLongitude(locationLongitude);
    incidentEntity.setLocationAltitude(locationAltitude);
    incidentEntity.setEstimatedTime(estimatedTime);
    UserEntity userEntity =
        userService.getUser(userId).orElseThrow(() -> new NoSuchElementException("User Not Found"));
    incidentEntity.setCreator(userEntity);
    incidentEntity.setCreatedAt(LocalDateTime.now());
    incidentEntity.setUpdatedAt(LocalDateTime.now());
    incidentEntity.setUpdator(userEntity);
    return incidentRepository.save(incidentEntity);
  }

  public IncidentEntity updateIncident(Long userUpdatorId, Long id, String applicant, String type,
      String reason,
      Double locationLatitude, Double locationLongitude, Double locationAltitude,
      LocalDateTime estimatedTime) throws NoSuchElementException {
    IncidentMainInfoDto incidentMainInfoDto =
        new IncidentMainInfoDto(id, applicant, type, reason, locationLatitude, locationLongitude,
            locationAltitude, estimatedTime);

    UserEntity userEntity =
        userService.getUser(userUpdatorId)
            .orElseThrow(() -> new NoSuchElementException("User Not Found"));
    IncidentEntity incidentEntity =
        incidentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Incident Not Found"));

    IncidentEntity partialUpdate = incidentMainInfoMapper.partialUpdate(incidentMainInfoDto, incidentEntity);
    partialUpdate.setUpdator(userEntity);
    partialUpdate.setUpdatedAt(LocalDateTime.now());
    return incidentRepository.update(partialUpdate);
  }

  public List<IncidentEntity> getAllIncidents() {
    return incidentRepository.findAll();
  }

  public IncidentEntity getIncident(Long id) throws NoSuchElementException {
    return incidentRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Incident Not Found"));
  }

  public void deleteIncident(Long id) throws NoSuchElementException, IllegalStateException {
    IncidentEntity incidentEntity =
        incidentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Incident Not Found"));
    if (incidentEntity.getRelatedCase() == null) {
      incidentRepository.delete(incidentEntity);
    } else {
      throw new IllegalStateException("Incident has related case");
    }
  }

}
