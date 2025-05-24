package ru.itmopai.backend.mapper;

import org.mapstruct.*;
import ru.itmopai.backend.dto.CaseMainInfoDto;
import ru.itmopai.backend.entity.CaseEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface CaseMainInfoMapper {
  CaseEntity toEntity(CaseMainInfoDto caseMainInfoDto);

  CaseMainInfoDto toDto(CaseEntity caseEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  CaseEntity partialUpdate(CaseMainInfoDto caseMainInfoDto, @MappingTarget CaseEntity caseEntity);
}
