package ru.itmopai.backend.mapper;

import org.mapstruct.*;
import ru.itmopai.backend.dto.UserDto;
import ru.itmopai.backend.entity.UserEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface UserMapper {
  UserEntity toEntity(UserDto userDto);

  UserDto toDto(UserEntity userEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  UserEntity partialUpdate(UserDto userDto, @MappingTarget UserEntity userEntity);
}
