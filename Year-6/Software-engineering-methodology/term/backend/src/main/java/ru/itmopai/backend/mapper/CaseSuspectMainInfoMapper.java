package ru.itmopai.backend.mapper;

import java.util.List;

import org.mapstruct.*;

import ru.itmopai.backend.dto.CaseSuspectMainInfoDto;
import ru.itmopai.backend.entity.CaseSuspectEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface CaseSuspectMainInfoMapper {
  CaseSuspectEntity toEntity(CaseSuspectMainInfoDto caseSuspectMainInfoDto);

  CaseSuspectMainInfoDto toDto(CaseSuspectEntity caseSuspectEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  CaseSuspectEntity partialUpdate(CaseSuspectMainInfoDto caseSuspectMainInfoDto,
      @MappingTarget CaseSuspectEntity caseSuspectEntity);

  List<CaseSuspectEntity> toEntity(List<CaseSuspectMainInfoDto> caseSuspectMainInfoDto);

  List<CaseSuspectMainInfoDto> toDto(List<CaseSuspectEntity> caseSuspectEntity);
}
