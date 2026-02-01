package ru.itmopai.backend.mapper;

import org.mapstruct.*;

import ru.itmopai.backend.dto.CaseSuspectDto;
import ru.itmopai.backend.entity.CaseSuspectEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface CaseSuspectMapper {
  @Mapping(source = "relatedCaseId", target = "relatedCase.id")
  @Mapping(source = "updatorId", target = "updator.id")
  @Mapping(source = "creatorId", target = "creator.id")
  CaseSuspectEntity toEntity(CaseSuspectDto caseSuspectDto);

  @InheritInverseConfiguration(name = "toEntity")
  CaseSuspectDto toDto(CaseSuspectEntity caseSuspectEntity);

  @InheritConfiguration(name = "toEntity")
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  CaseSuspectEntity partialUpdate(CaseSuspectDto caseSuspectDto,
      @MappingTarget CaseSuspectEntity caseSuspectEntity);
}
