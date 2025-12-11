package ru.itmo.zavar.ticktokcoretest.profile.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.itmo.zavar.ticktokcoretest.common.security.Role;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.auth.AuthUser;
import ru.itmo.zavar.ticktokcoretest.profile.model.UserEntity;

import java.util.HashSet;
import java.util.Set;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserProfileMapper {

    @Mapping(target = "passwordHash", source = "password")
    AuthUser toAuthUser(UserEntity entity);

    @Mapping(target = "password", source = "passwordHash")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "roles", expression = "java(mapRoles(user.roles()))")
    UserEntity toEntity(AuthUser user);

    @Mapping(target = "avatarUrl", expression = "java(resolveAvatarUrl(entity))")
    ProfileUserDetails toProfileUserDetails(UserEntity entity);

    default Set<Role> mapRoles(Set<Role> roles) {
        return roles == null ? new HashSet<>() : new HashSet<>(roles);
    }

    default String resolveAvatarUrl(UserEntity entity) {
        if (entity == null || entity.getAvatarKey() == null) {
            return null;
        }
        return "/api/users/" + entity.getId() + "/avatar";
    }
}
