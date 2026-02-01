package ru.itmopai.backend.service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.CaseMainInfoDto;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.IncidentEntity;
import ru.itmopai.backend.entity.UserCaseEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.mapper.CaseMainInfoMapper;
import ru.itmopai.backend.mapper.CaseMapper;
import ru.itmopai.backend.repository.CaseRepository;
import ru.itmopai.backend.repository.IncidentRepository;
import ru.itmopai.backend.repository.UserCaseRepository;
import ru.itmopai.backend.repository.UserRepository;

@Singleton
@AllArgsConstructor
public class CaseService {
  private final CaseRepository caseRepository;
  private final UserRepository userRepository;
  private final IncidentRepository incidentRepository;
  private final CaseMapper caseMapper;
  private final CaseMainInfoMapper caseMainInfoMapper;
  private final UserCaseRepository userCaseRepository;

  @Transactional
  public CaseEntity addCase(Long userId, String title, String description, Long incidentId,
      HashMap<Long, String> users)
      throws NoSuchElementException {
    CaseEntity caseEntity = new CaseEntity();
    UserEntity userEntity =
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("Creator user not found"));
    IncidentEntity incidentEntity =
        incidentRepository.findById(incidentId)
            .orElseThrow(() -> new NoSuchElementException("Incident not found"));

    List<UserEntity> usersEntityList = userRepository.findAllByIdIn(users.keySet());
    Set<Long> foundIds =
        usersEntityList.stream().map(UserEntity::getId).collect(Collectors.toSet());
    Set<Long> missingIds = new HashSet<>(users.keySet());
    missingIds.removeAll(foundIds);

    if (!missingIds.isEmpty()) {
      String missingIdsStr =
          missingIds.stream()
              .map(String::valueOf)
              .collect(Collectors.joining(", "));
      throw new NoSuchElementException("Some of users not found: " + missingIdsStr);
    }

    caseEntity.setTitle(title);
    caseEntity.setDescription(description);
    caseEntity.setCreator(userEntity);
    caseEntity.setIncident(incidentEntity);
    caseEntity.setUpdator(userEntity);
    caseEntity.setCreatedAt(Instant.now());
    caseEntity.setUpdatedAt(caseEntity.getCreatedAt());
    caseEntity.setStatus(CaseEntity.Status.OPENED);
    incidentEntity.setRelatedCase(caseEntity);
    CaseEntity saved = caseRepository.save(caseEntity);
    incidentRepository.update(incidentEntity);

    usersEntityList.forEach(entity -> {
      UserCaseEntity.UserCaseId userCaseId = new UserCaseEntity.UserCaseId();
      userCaseId.setRelatedCase(saved);
      userCaseId.setUser(entity);

      UserCaseEntity userCaseEntity = new UserCaseEntity();
      userCaseEntity.setId(userCaseId);
      userCaseEntity.setPosition(users.get(entity.getId()));
      userCaseEntity.setStatus(UserCaseEntity.Status.WORKING);
      userCaseRepository.save(userCaseEntity);
    });
    return saved;
  }

  @Transactional
  public CaseEntity updateCase(Long userId, Long id, String title, String description,
      CaseEntity.Status status, HashMap<Long, String> users) throws NoSuchElementException {
    UserEntity userEntity =
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("Creator user not found"));
    IncidentEntity incidentEntity =
        incidentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Incident not found"));
    List<UserEntity> usersEntityList = userRepository.findAllByIdIn(users.keySet());
    Set<Long> foundIds =
        usersEntityList.stream().map(UserEntity::getId).collect(Collectors.toSet());
    Set<Long> missingIds = new HashSet<>(users.keySet());
    missingIds.removeAll(foundIds);

    if (!missingIds.isEmpty()) {
      String missingIdsStr =
          missingIds.stream()
              .map(String::valueOf)
              .collect(Collectors.joining(", "));
      throw new NoSuchElementException("Some of users not found: " + missingIdsStr);
    }

    CaseMainInfoDto caseMainInfoDto =
        new CaseMainInfoDto(incidentEntity.getRelatedCase().getId(), title, description, status);
    CaseEntity partialUpdate =
        caseMainInfoMapper.partialUpdate(caseMainInfoDto, incidentEntity.getRelatedCase());
    partialUpdate.setUpdatedAt(Instant.now());
    partialUpdate.setUpdator(userEntity);

    CaseEntity updated = caseRepository.update(partialUpdate);

    List<UserCaseEntity> allByIdRelatedCaseId =
        userCaseRepository.findAllById_RelatedCaseId(partialUpdate.getId());
    userCaseRepository.deleteAll(allByIdRelatedCaseId);

    usersEntityList.forEach(entity -> {
      UserCaseEntity.UserCaseId userCaseId = new UserCaseEntity.UserCaseId();
      userCaseId.setRelatedCase(partialUpdate);
      userCaseId.setUser(entity);

      UserCaseEntity userCaseEntity = new UserCaseEntity();
      userCaseEntity.setId(userCaseId);
      userCaseEntity.setPosition(users.get(entity.getId()));
      userCaseEntity.setStatus(UserCaseEntity.Status.WORKING);
      userCaseRepository.save(userCaseEntity);
    });

    return updated;
  }

  public List<CaseEntity> getAllCasesByUserId(Long userId) {
    return caseRepository.findAllByCreatorId(userId);
  }

  public List<UserCaseEntity> getAllUserCasesByUserId(Long userId) {
    return userCaseRepository.findAllById_UserId(userId);
  }

  public CaseEntity getCaseById(Long id) throws NoSuchElementException {
    return caseRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Case not found"));
  }

}
