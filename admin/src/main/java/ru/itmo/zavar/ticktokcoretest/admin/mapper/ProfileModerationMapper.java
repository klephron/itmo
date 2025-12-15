package ru.itmo.zavar.ticktokcoretest.admin.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.zavar.ticktokcoretest.admin.dto.UserDetailDto;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.ProfileUserDetails;

@Component
public class ProfileModerationMapper {
    public UserDetailDto toDto(ProfileUserDetails user) {
        return new UserDetailDto(
                user.id(),
                user.username(),
                user.displayName(),
                user.email(),
                user.enabled(),
                user.roles(),
                user.createdAt()
        );
    }
}
