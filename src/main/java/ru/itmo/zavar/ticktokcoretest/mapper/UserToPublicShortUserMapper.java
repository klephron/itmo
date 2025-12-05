package ru.itmo.zavar.ticktokcoretest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.itmo.zavar.ticktokcoretest.dto.PublicUserShortDto;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;

import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = {ZoneOffset.class})
public interface UserToPublicShortUserMapper {
    @Mapping(target = "avatarUrl",
            expression = "java(user.getAvatarKey() != null ? "
                    + "\"/api/users/\" + user.getId() + \"/avatar\" : null)")
    PublicUserShortDto toDto(UserEntity user);

    List<PublicUserShortDto> toDto(List<UserEntity> users);
}
