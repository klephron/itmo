package ru.itmo.zavar.accountservice.mapper;

import org.mapstruct.*;
import ru.itmo.zavar.accountservice.dto.UserDto;
import ru.itmo.zavar.accountservice.entity.UserEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    UserEntity toEntity(UserDto userDto);

    UserDto toDto(UserEntity userEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserEntity partialUpdate(UserDto userDto, @MappingTarget UserEntity userEntity);
}