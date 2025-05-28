package ru.itmopai.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.CaseSuspectMainInfoDto;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.CaseSuspectEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.mapper.CaseSuspectMainInfoMapper;
import ru.itmopai.backend.repository.CaseRepository;
import ru.itmopai.backend.repository.CaseSuspectRepository;
import ru.itmopai.backend.repository.UserRepository;

@Singleton
@AllArgsConstructor
public class CaseSuspectService {
  private final CaseSuspectRepository caseSuspectRepository;
  private final CaseRepository caseRepository;
  private final UserRepository userRepository;
  private final CaseSuspectMainInfoMapper caseSuspectMainInfoMapper;

  public CaseSuspectEntity addCaseSuspect(Long userId, String name, String surname,
      String patronymic, String identifier, CaseSuspectEntity.Status status, Long caseId,
      String verdict, String accusation)
      throws NoSuchElementException {
    UserEntity userEntity =
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("Creator user not found"));

    CaseEntity caseEntity =
        caseRepository.findById(caseId)
            .orElseThrow(() -> new NoSuchElementException("Case not found"));

    CaseSuspectEntity caseSuspectEntity = new CaseSuspectEntity();
    caseSuspectEntity.setCreator(userEntity);
    caseSuspectEntity.setUpdator(userEntity);
    caseSuspectEntity.setCreatedAt(Instant.now());
    caseSuspectEntity.setUpdatedAt(caseSuspectEntity.getCreatedAt());
    caseSuspectEntity.setName(name);
    caseSuspectEntity.setSurname(surname);
    caseSuspectEntity.setPatronymic(patronymic);
    caseSuspectEntity.setIdentifier(identifier);
    caseSuspectEntity.setStatus(status);
    caseSuspectEntity.setRelatedCase(caseEntity);
    caseSuspectEntity.setVerdict(verdict);
    caseSuspectEntity.setAccusation(accusation);
    return caseSuspectRepository.save(caseSuspectEntity);
  }

  public CaseSuspectEntity updateCaseSuspect(Long id, Long userId, String name, String surname,
      String patronymic, String identifier, CaseSuspectEntity.Status status, String verdict,
      String accusation)
      throws NoSuchElementException {
    UserEntity userEntity =
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));

    CaseSuspectEntity caseSuspectEntity =
        caseSuspectRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Case suspect not found"));

    CaseSuspectMainInfoDto caseSuspectMainInfoDto =
        new CaseSuspectMainInfoDto(id, name, surname, patronymic, identifier, status, verdict,
            accusation);

    CaseSuspectEntity partialUpdate =
        caseSuspectMainInfoMapper.partialUpdate(caseSuspectMainInfoDto, caseSuspectEntity);

    partialUpdate.setUpdatedAt(Instant.now());
    partialUpdate.setUpdator(userEntity);

    return caseSuspectRepository.update(partialUpdate);

  }

  public CaseSuspectEntity getCaseSuspect(Long id) throws NoSuchElementException {
    return caseSuspectRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Case suspect not found"));
  }

  public void deleteCaseSuspect(Long id) {
    caseSuspectRepository.deleteById(id);
  }

  public List<CaseSuspectEntity> getAllCaseSuspectByCaseId(Long caseId) {
    return caseSuspectRepository.findAllByRelatedCase_Id(caseId);
  }

}
