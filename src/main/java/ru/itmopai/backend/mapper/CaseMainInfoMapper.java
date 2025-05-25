package ru.itmopai.backend.mapper;

import org.mapstruct.*;
import ru.itmopai.backend.dto.CaseMainInfoDto;
import ru.itmopai.backend.entity.CaseEntity;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface CaseMainInfoMapper {
  CaseEntity toEntity(CaseMainInfoDto caseMainInfoDto);

  CaseMainInfoDto toDto(CaseEntity caseEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  CaseEntity partialUpdate(CaseMainInfoDto caseMainInfoDto, @MappingTarget CaseEntity caseEntity);

  List<CaseEntity> toEntity(List<CaseMainInfoDto> caseMainInfoDto);

  List<CaseMainInfoDto> toDto(List<CaseEntity> caseEntity);
}
