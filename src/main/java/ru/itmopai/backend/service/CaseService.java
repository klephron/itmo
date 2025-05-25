package ru.itmopai.backend.service;

import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.CaseMainInfoDto;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.IncidentEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.mapper.CaseMainInfoMapper;
import ru.itmopai.backend.mapper.CaseMapper;
import ru.itmopai.backend.repository.CaseRepository;
import ru.itmopai.backend.repository.IncidentRepository;
import ru.itmopai.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Singleton
@AllArgsConstructor
public class CaseService {
  private final CaseRepository caseRepository;
  private final UserRepository userRepository;
  private final IncidentRepository incidentRepository;
  private final CaseMapper caseMapper;
  private final CaseMainInfoMapper caseMainInfoMapper;

  @Transactional
  public CaseEntity addCase(Long userId, String title, String description, Long incidentId)
      throws NoSuchElementException {
    CaseEntity caseEntity = new CaseEntity();
    UserEntity userEntity =
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));
    IncidentEntity incidentEntity =
        incidentRepository.findById(incidentId)
            .orElseThrow(() -> new NoSuchElementException("Incident not found"));
    caseEntity.setTitle(title);
    caseEntity.setDescription(description);
    caseEntity.setCreator(userEntity);
    caseEntity.setIncident(incidentEntity);
    caseEntity.setUpdator(userEntity);
    caseEntity.setCreatedAt(LocalDateTime.now());
    caseEntity.setUpdatedAt(LocalDateTime.now());
    caseEntity.setStatus(CaseEntity.Status.OPENED);
    incidentEntity.setRelatedCase(caseEntity);
    CaseEntity saved = caseRepository.save(caseEntity);
    incidentRepository.update(incidentEntity);
    return saved;
  }

  @Transactional
  public CaseEntity updateCase(Long userId, Long id, String title, String description,
      CaseEntity.Status status) throws NoSuchElementException {
    UserEntity userEntity =
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));
    IncidentEntity incidentEntity =
      incidentRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Incident not found"));
    CaseMainInfoDto caseMainInfoDto = new CaseMainInfoDto(incidentEntity.getRelatedCase().getId(), title, description, status);
    CaseEntity partialUpdate = caseMainInfoMapper.partialUpdate(caseMainInfoDto, incidentEntity.getRelatedCase());
    partialUpdate.setUpdatedAt(LocalDateTime.now());
    partialUpdate.setUpdator(userEntity);
    return caseRepository.update(partialUpdate);
  }

  public List<CaseEntity> getAllCasesByUserId(Long userId) {
    return caseRepository.findAllByCreatorId(userId);
  }

}
