package ru.itmopai.backend.mapper;

import java.util.List;

import org.mapstruct.*;

import ru.itmopai.backend.dto.TemplateMainInfoDto;
import ru.itmopai.backend.entity.TemplateEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface TemplateMainInfoMapper {
  TemplateEntity toEntity(TemplateMainInfoDto templateMainInfoDto);

  TemplateMainInfoDto toDto(TemplateEntity templateEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  TemplateEntity partialUpdate(TemplateMainInfoDto templateMainInfoDto,
      @MappingTarget TemplateEntity templateEntity);

  List<TemplateEntity> toEntity(List<TemplateMainInfoDto> templateMainInfoDto);

  List<TemplateMainInfoDto> toDto(List<TemplateEntity> templateEntity);
}
