package ru.itmo.zavar.ticktokcoretest.mapper;

import org.mapstruct.*;
import ru.itmo.zavar.ticktokcoretest.dto.UserDetailDto;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    UserEntity toEntity(UserDetailDto userDetailDto);

    UserDetailDto toDto(UserEntity userEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserEntity partialUpdate(UserDetailDto userDetailDto, @MappingTarget UserEntity userEntity);

    List<UserEntity> toEntity(List<UserDetailDto> userDetailDto);

    List<UserDetailDto> toDto(List<UserEntity> userEntity);
}