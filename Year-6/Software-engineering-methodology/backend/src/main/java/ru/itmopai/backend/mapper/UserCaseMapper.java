package ru.itmopai.backend.mapper;

import java.util.List;

import org.mapstruct.*;

import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import ru.itmopai.backend.dto.UserCaseDto;
import ru.itmopai.backend.entity.UserCaseEntity;
import ru.itmopai.backend.repository.UserCaseRepository;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA,
    uses = { UserCaseRepository.class })
public abstract class UserCaseMapper {

  @Inject
  private UserCaseRepository userCaseRepository;

  @Mapping(source = "idRelatedCaseId", target = "id.relatedCase.id")
  @Mapping(source = "idUserId", target = "id.user.id")
  @Mapping(source = "idRelatedCaseTitle", target = "id.relatedCase.title", ignore = true)
  public abstract UserCaseEntity toEntity(UserCaseDto userCaseDto);

  @InheritInverseConfiguration(name = "toEntity")
  public abstract UserCaseDto toDto(UserCaseEntity userCaseEntity);

  @InheritConfiguration(name = "toEntity")
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  public abstract UserCaseEntity partialUpdate(UserCaseDto userCaseDto,
      @MappingTarget UserCaseEntity userCaseEntity);

  public abstract List<UserCaseEntity> toEntity(List<UserCaseDto> userCaseDto);

  public abstract List<UserCaseDto> toDto(List<UserCaseEntity> userCaseEntity);

  @AfterMapping
  @Transactional(readOnly = true)
  protected void fillCaseTitle(UserCaseEntity entity, @MappingTarget UserCaseDto dto) {
    UserCaseEntity userCaseEntity = userCaseRepository.findById(entity.getId()).get();
    dto.setIdRelatedCaseTitle(userCaseEntity.getId().getRelatedCase().getTitle());
  }
}
