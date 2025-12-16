package ru.itmo.zavar.brainstop.admin.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.itmo.zavar.brainstop.admin.dto.UserDetailDto;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProfileModerationMapper {

    UserDetailDto toDto(ProfileUserDetails user);
}
