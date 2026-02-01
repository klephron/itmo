package ru.itmopai.backend.mapper;

import org.mapstruct.*;

import ru.itmopai.backend.dto.TemplateDto;
import ru.itmopai.backend.entity.TemplateEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface TemplateMapper {
  @Mapping(source = "relatedCaseId", target = "relatedCase.id")
  @Mapping(source = "updatorId", target = "updator.id")
  @Mapping(source = "creatorId", target = "creator.id")
  TemplateEntity toEntity(TemplateDto templateDto);

  @InheritInverseConfiguration(name = "toEntity")
  TemplateDto toDto(TemplateEntity templateEntity);

  @InheritConfiguration(name = "toEntity")
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  TemplateEntity partialUpdate(TemplateDto templateDto,
      @MappingTarget TemplateEntity templateEntity);
}
