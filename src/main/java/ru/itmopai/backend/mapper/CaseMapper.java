package ru.itmopai.backend.mapper;

import org.mapstruct.*;
import ru.itmopai.backend.dto.CaseDto;
import ru.itmopai.backend.entity.CaseEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface CaseMapper {
  @Mapping(source = "incidentId", target = "incident.id")
  @Mapping(source = "updatorId", target = "updator.id")
  @Mapping(source = "creatorId", target = "creator.id")
  CaseEntity toEntity(CaseDto caseDto);

  @InheritInverseConfiguration(name = "toEntity")
  CaseDto toDto(CaseEntity caseEntity);

  @InheritConfiguration(name = "toEntity")
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  CaseEntity partialUpdate(CaseDto caseDto, @MappingTarget CaseEntity caseEntity);
}
