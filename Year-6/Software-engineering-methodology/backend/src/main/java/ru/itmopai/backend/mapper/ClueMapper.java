package ru.itmopai.backend.mapper;

import org.mapstruct.*;

import ru.itmopai.backend.dto.ClueDto;
import ru.itmopai.backend.entity.ClueEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface ClueMapper {
  @Mapping(source = "relatedCaseId", target = "relatedCase.id")
  @Mapping(source = "updatorId", target = "updator.id")
  @Mapping(source = "creatorId", target = "creator.id")
  ClueEntity toEntity(ClueDto clueDto);

  @InheritInverseConfiguration(name = "toEntity")
  ClueDto toDto(ClueEntity clueEntity);

  @InheritConfiguration(name = "toEntity")
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  ClueEntity partialUpdate(ClueDto clueDto, @MappingTarget ClueEntity clueEntity);
}
