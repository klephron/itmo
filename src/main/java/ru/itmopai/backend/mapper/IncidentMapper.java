package ru.itmopai.backend.mapper;

import org.mapstruct.*;
import ru.itmopai.backend.dto.IncidentDto;
import ru.itmopai.backend.entity.IncidentEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface IncidentMapper {
  @Mapping(source = "relatedCaseIncidentId", target = "relatedCase.incident.id")
  @Mapping(source = "relatedCaseStatus", target = "relatedCase.status")
  @Mapping(source = "relatedCaseUpdatedAt", target = "relatedCase.updatedAt")
  @Mapping(source = "relatedCaseUpdatorId", target = "relatedCase.updator.id")
  @Mapping(source = "relatedCaseCreatedAt", target = "relatedCase.createdAt")
  @Mapping(source = "relatedCaseCreatorId", target = "relatedCase.creator.id")
  @Mapping(source = "relatedCaseDescription", target = "relatedCase.description")
  @Mapping(source = "relatedCaseTitle", target = "relatedCase.title")
  @Mapping(source = "relatedCaseId", target = "relatedCase.id")
  @Mapping(source = "updatorId", target = "updator.id")
  @Mapping(source = "creatorId", target = "creator.id")
  IncidentEntity toEntity(IncidentDto incidentDto);

  @InheritInverseConfiguration(name = "toEntity")
  IncidentDto toDto(IncidentEntity incidentEntity);

  @InheritConfiguration(name = "toEntity")
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  IncidentEntity partialUpdate(IncidentDto incidentDto, @MappingTarget IncidentEntity incidentEntity);
}
