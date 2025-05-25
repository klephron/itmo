package ru.itmopai.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.ClueMainInfoDto;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.ClueEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.mapper.ClueMainInfoMapper;
import ru.itmopai.backend.repository.ClueRepository;
import ru.itmopai.backend.repository.UserRepository;

@Singleton
@AllArgsConstructor
public class ClueService {
  private final ClueRepository clueRepository;
  private final UserRepository userRepository;
  private final CaseService caseService;
  private final ClueMainInfoMapper clueMainInfoMapper;

  public ClueEntity addClue(Long userId, String title, String description, ClueEntity.Status status,
      Long relatedCaseId) throws NoSuchElementException {
    UserEntity userEntity =
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));

    CaseEntity caseById = caseService.getCaseById(relatedCaseId);
    ClueEntity clueEntity = new ClueEntity();
    clueEntity.setTitle(title);
    clueEntity.setDescription(description);
    clueEntity.setStatus(status);
    clueEntity.setRelatedCase(caseById);
    clueEntity.setCreator(userEntity);
    clueEntity.setUpdator(userEntity);
    clueEntity.setCreatedAt(LocalDateTime.now());
    clueEntity.setUpdatedAt(LocalDateTime.now());
    clueEntity.setMetadata("{}");
    return clueRepository.save(clueEntity);
  }

  public ClueEntity updateClue(Long userId, Long id, String title, String description,
      ClueEntity.Status status) throws NoSuchElementException {
    ClueEntity clueEntity = getClue(id);
    ClueMainInfoDto clueMainInfoDto = new ClueMainInfoDto(id, title, description, status);
    ClueEntity partialUpdate = clueMainInfoMapper.partialUpdate(clueMainInfoDto, clueEntity);
    partialUpdate.setUpdatedAt(LocalDateTime.now());
    partialUpdate.setUpdator(userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found")));
    return clueRepository.update(partialUpdate);
  }

  public ClueEntity getClue(Long id) throws NoSuchElementException {
    return clueRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Clue not found"));
  }

  public List<ClueEntity> getAllClueByCase(Long id) {
    return clueRepository.findAllByRelatedCase_Id(id);
  }

  public void deleteClue(Long id) {
    clueRepository.deleteById(id);
  }

}
