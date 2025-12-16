package ru.itmo.zavar.brainstop.profile.mapper;

import lombok.Setter;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.zavar.brainstop.profile.dto.PublicUserProfileDto;
import ru.itmo.zavar.brainstop.profile.model.UserEntity;
import ru.itmo.zavar.brainstop.profile.service.FollowService;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class UserToPublicProfileMapper {
    @Autowired
    protected FollowService followService;

    @Setter
    protected Long currentUserId;

    @Mapping(target = "avatarUrl",
            expression = "java( user.getAvatarKey() == null ? null : \"/api/users/\" + user.getId() + \"/avatar\" )")
    @Mapping(target = "followersCount", ignore = true)
    @Mapping(target = "followingCount", ignore = true)
    @Mapping(target = "following", ignore = true)
    public abstract PublicUserProfileDto toDto(UserEntity user);

    public abstract List<PublicUserProfileDto> toDto(List<UserEntity> users);

    @AfterMapping
    protected void enrich(UserEntity user, @MappingTarget PublicUserProfileDto.PublicUserProfileDtoBuilder builder) {

        long followers = followService.countFollowers(user.getId());
        long following = followService.countFollowing(user.getId());
        boolean isFollowing = currentUserId != null && followService.isFollowing(currentUserId, user.getId());

        builder.followersCount(followers);
        builder.followingCount(following);
        builder.following(isFollowing);
    }}
