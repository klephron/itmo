package ru.itmopai.backend.mapper;

import org.mapstruct.*;
import ru.itmopai.backend.dto.TestDTO;
import ru.itmopai.backend.entity.TestEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface TestMapper {
  TestEntity toEntity(TestDTO dto);

  TestDTO toDto(TestEntity testEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  TestEntity partialUpdate(TestDTO userDto, @MappingTarget TestEntity testEntity);
}
