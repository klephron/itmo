package ru.itmopai.backend.mapper;

import java.util.HashMap;
import java.util.List;

import org.mapstruct.*;

import jakarta.inject.Inject;
import ru.itmopai.backend.dto.CaseDto;
import ru.itmopai.backend.entity.CaseEntity;
import ru.itmopai.backend.entity.UserCaseEntity;
import ru.itmopai.backend.repository.UserCaseRepository;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA)
public abstract class CaseMapper {

  @Inject
  protected UserCaseRepository userCaseRepository;

  @Mapping(source = "incidentId", target = "incident.id")
  @Mapping(source = "updatorId", target = "updator.id")
  @Mapping(source = "creatorId", target = "creator.id")
  public abstract CaseEntity toEntity(CaseDto caseDto);

  @InheritInverseConfiguration(name = "toEntity")
  @Mapping(target = "users", ignore = true)
  public abstract CaseDto toDto(CaseEntity caseEntity);

  @InheritConfiguration(name = "toEntity")
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  public abstract CaseEntity partialUpdate(CaseDto caseDto, @MappingTarget CaseEntity caseEntity);

  @AfterMapping
  protected void fillUsersMetaData(CaseEntity entity, @MappingTarget CaseDto dto) {
    List<UserCaseEntity> allByIdRelatedCaseId =
        userCaseRepository.findAllById_RelatedCaseId(entity.getId());
    HashMap<Long, String> hashMap = new HashMap<>();
    allByIdRelatedCaseId.forEach(userCaseEntity -> {
      hashMap.put(userCaseEntity.getId().getUser().getId(), userCaseEntity.getPosition());
    });

    dto.setUsers(hashMap);
  }

}
