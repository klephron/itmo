package ru.itmo.zavar.ticktokcoretest.video.mapper;

import org.mapstruct.*;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.ticktokcoretest.video.model.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.video.model.VideoRenditionEntity;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {ZoneOffset.class}
)
public interface FileMetadataToFeedMapper {
    @Mapping(target = "uploaderAvatarUrl", expression = "java(resolveAvatar(src, context))")
    @Mapping(target = "uploadedAt",
            expression = "java( src.getUploadedAt() != null ? src.getUploadedAt().toInstant(ZoneOffset.UTC) : null)")
    @Mapping(target = "likedByMe", expression = "java(isLiked(src, context))")
    @Mapping(target = "subscribedToAuthor", expression = "java(isSubscribed(src, context))")
    @Mapping(target = "availableQualities", expression = "java(mapQualities(src))")
    @Mapping(target = "description", source = "description")
    FeedVideoDto toDto(FileMetadataEntity src, @Context FeedMapperContext context);

    List<FeedVideoDto> toDto(List<FileMetadataEntity> src, @Context FeedMapperContext context);

    default List<Integer> mapQualities(FileMetadataEntity src) {
        if (src == null || src.getRenditions() == null) {
            return List.of();
        }
        return src.getRenditions().stream()
                .map(VideoRenditionEntity::getHeight)
                .sorted()
                .collect(Collectors.toList());
    }

    default String resolveAvatar(FileMetadataEntity src, @Context FeedMapperContext context) {
        if (src == null || src.getUploaderId() == null || context == null) {
            return null;
        }
        return context.avatarByUserId().get(src.getUploaderId());
    }

    default boolean isLiked(FileMetadataEntity src, @Context FeedMapperContext context) {
        if (src == null || src.getId() == null || context == null || context.currentUserId() == null) {
            return false;
        }
        return context.likedVideoIds().contains(src.getId());
    }

    default boolean isSubscribed(FileMetadataEntity src, @Context FeedMapperContext context) {
        if (src == null || src.getUploaderId() == null || context == null || context.currentUserId() == null) {
            return false;
        }
        return context.followingUserIds().contains(src.getUploaderId());
    }
}
