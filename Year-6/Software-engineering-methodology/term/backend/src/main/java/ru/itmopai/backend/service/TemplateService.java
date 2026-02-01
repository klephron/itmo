package ru.itmopai.backend.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.TemplateMainInfoDto;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.TemplateEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.mapper.TemplateMainInfoMapper;
import ru.itmopai.backend.repository.CaseRepository;
import ru.itmopai.backend.repository.ClueRepository;
import ru.itmopai.backend.repository.TemplateRepository;
import ru.itmopai.backend.repository.UserRepository;

@Singleton
@AllArgsConstructor
public class TemplateService {
  private final TemplateRepository templateRepository;
  private final UserRepository userRepository;
  private final CaseRepository caseRepository;
  private final TemplateMainInfoMapper templateMainInfoMapper;
  private final ObjectMapper objectMapper;
  private final ClueRepository clueRepository;

  public TemplateEntity addTemplate(Long userId, Long caseId, String title,
      String metadata, String description)
      throws NoSuchElementException, IllegalArgumentException {
    UserEntity userEntity =
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("Creator user not found"));

    CaseEntity caseEntity =
        caseRepository.findById(caseId)
            .orElseThrow(() -> new NoSuchElementException("Case not found"));

    try {
      TemplateEntity.Metadata readValue =
          objectMapper.readValue(metadata, TemplateEntity.Metadata.class);
      if (readValue != null) {
        long count = clueRepository.countByRelatedCase_IdAndIdIn(caseId, readValue.getClueIds());
        if (count != readValue.getClueIds().size()) {
          throw new IllegalArgumentException("Check clue's ids");
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Can't convert metadata to json", e);
    }

    TemplateEntity templateEntity = new TemplateEntity();
    templateEntity.setTitle(title);
    templateEntity.setStatus(TemplateEntity.Status.PENDING);
    templateEntity.setMetadata(metadata);
    templateEntity.setDescription(description);
    templateEntity.setRelatedCase(caseEntity);
    templateEntity.setCreator(userEntity);
    templateEntity.setUpdator(userEntity);
    templateEntity.setCreatedAt(Instant.now());
    templateEntity.setUpdatedAt(templateEntity.getCreatedAt());
    return templateRepository.save(templateEntity);
  }

  public TemplateEntity updateTemplate(Long userId, Long templateId, String title,
      TemplateEntity.Status status, String metadata,
      String description) throws NoSuchElementException, IllegalArgumentException {
    UserEntity userEntity =
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));

    TemplateEntity templateEntity =
        templateRepository.findById(templateId)
            .orElseThrow(() -> new NoSuchElementException("Template not found"));

    try {
      TemplateEntity.Metadata readValue =
          objectMapper.readValue(metadata, TemplateEntity.Metadata.class);
      if (readValue != null) {
        long count =
            clueRepository.countByRelatedCase_IdAndIdIn(templateEntity.getRelatedCase().getId(),
                readValue.getClueIds());
        if (count != readValue.getClueIds().size()) {
          throw new IllegalArgumentException("Check clue's ids");
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Can't convert metadata to json", e);
    }

    TemplateMainInfoDto templateMainInfoDto =
        new TemplateMainInfoDto(templateId, title, status, metadata, description);

    TemplateEntity partialUpdate =
        templateMainInfoMapper.partialUpdate(templateMainInfoDto, templateEntity);
    partialUpdate.setUpdatedAt(Instant.now());
    partialUpdate.setUpdator(userEntity);
    return templateRepository.update(partialUpdate);
  }

  public void deleteTemplate(Long templateId) {
    templateRepository.deleteById(templateId);
  }

  public TemplateEntity getTemplate(Long templateId) throws NoSuchElementException {
    return templateRepository.findById(templateId)
        .orElseThrow(() -> new NoSuchElementException("Template not found"));
  }

  public List<TemplateEntity> getTemplatesByCaseId(Long caseId) {
    return templateRepository.findAllByRelatedCase_Id(caseId);
  }

}
