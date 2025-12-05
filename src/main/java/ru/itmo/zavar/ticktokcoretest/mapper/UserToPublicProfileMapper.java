package ru.itmo.zavar.ticktokcoretest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.itmo.zavar.ticktokcoretest.dto.PublicUserProfileDto;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserToPublicProfileMapper {

    @Mapping(target = "avatarUrl",
            expression = "java( user.getAvatarKey() == null ? null : \"/api/users/\" + user.getId() + \"/avatar\" )")
    PublicUserProfileDto toDto(UserEntity user);

    List<PublicUserProfileDto> toDto(List<UserEntity> userEntity);
}
