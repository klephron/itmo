package ru.itmopai.backend.mapper;

import java.util.List;

import org.mapstruct.*;

import ru.itmopai.backend.dto.ClueMainInfoDto;
import ru.itmopai.backend.entity.ClueEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface ClueMainInfoMapper {
  ClueEntity toEntity(ClueMainInfoDto clueMainInfoDto);

  ClueMainInfoDto toDto(ClueEntity clueEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  ClueEntity partialUpdate(ClueMainInfoDto clueMainInfoDto, @MappingTarget ClueEntity clueEntity);

  List<ClueEntity> toEntity(List<ClueMainInfoDto> clueMainInfoDto);

  List<ClueMainInfoDto> toDto(List<ClueEntity> clueEntity);
}
