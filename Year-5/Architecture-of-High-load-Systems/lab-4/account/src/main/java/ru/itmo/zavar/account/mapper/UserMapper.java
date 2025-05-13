package ru.itmo.zavar.account.mapper;

import org.mapstruct.*;
import ru.itmo.zavar.account.dto.UserDto;
import ru.itmo.zavar.account.entity.UserEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    UserEntity toEntity(UserDto userDto);

    UserDto toDto(UserEntity userEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserEntity partialUpdate(UserDto userDto, @MappingTarget UserEntity userEntity);
}
