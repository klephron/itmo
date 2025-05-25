package ru.itmopai.backend.service;

import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.ClueEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.repository.CaseRepository;
import ru.itmopai.backend.repository.ClueRepository;
import ru.itmopai.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Singleton
@AllArgsConstructor
public class ClueService {
  private final ClueRepository clueRepository;
  private final UserRepository userRepository;
  private final CaseService caseService;

  public ClueEntity addClue(Long userId, String title, String description, ClueEntity.Status status,
      Long relatedCase) throws NoSuchElementException {
    UserEntity userEntity =
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));

    CaseEntity caseById = caseService.getCaseById(relatedCase);
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

}
