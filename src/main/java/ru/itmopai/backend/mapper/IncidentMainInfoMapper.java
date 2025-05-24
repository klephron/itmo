package ru.itmopai.backend.mapper;

import org.mapstruct.*;
import ru.itmopai.backend.dto.IncidentMainInfoDto;
import ru.itmopai.backend.entity.IncidentEntity;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface IncidentMainInfoMapper {
  IncidentEntity toEntity(IncidentMainInfoDto incidentMainInfoDto);

  IncidentMainInfoDto toDto(IncidentEntity incidentEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  IncidentEntity partialUpdate(IncidentMainInfoDto incidentMainInfoDto, @MappingTarget IncidentEntity incidentEntity);

  List<IncidentEntity> toEntity(List<IncidentMainInfoDto> incidentMainInfoDto);

  List<IncidentMainInfoDto> toDto(List<IncidentEntity> incidentEntity);
}
