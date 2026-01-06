package ru.itmo.zavar.brainstop.video.mapper;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.model.VideoRenditionEntity;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = {ZoneOffset.class})
public interface FileMetadataToPublicUserVideoMapper {

    @Mapping(target = "thumbnailUrl",
            expression = "java(\"/api/streaming/video/thumbnail/\" + file.getUuid())")
    @Mapping(target = "uploadedAt",
            expression = "java(file.getUploadedAt() != null ? "
                    + "file.getUploadedAt().toInstant(ZoneOffset.UTC) : null)")
    @Mapping(target = "availableQualities", expression = "java(mapQualities(file))")
    @Mapping(target = "likedByMe", expression = "java(isLiked(file, context))")
    @Mapping(target = "uploaderAvatarUrl", expression = "java(resolveAvatar(file, context))")
    PublicUserVideoDto toDto(FileMetadataEntity file, @Context PublicUserVideoMapperContext context);

    List<PublicUserVideoDto> toDto(List<FileMetadataEntity> files, @Context PublicUserVideoMapperContext context);

    default List<Integer> mapQualities(FileMetadataEntity src) {
        if (src == null || src.getRenditions() == null) {
            return List.of();
        }
        return src.getRenditions().stream()
                .map(VideoRenditionEntity::getHeight)
                .sorted()
                .collect(Collectors.toList());
    }

    default boolean isLiked(FileMetadataEntity file, PublicUserVideoMapperContext context) {
        if (file == null || file.getId() == null || context == null || context.currentUserId() == null || context.likedVideoIds() == null) {
            return false;
        }
        return context.likedVideoIds().contains(file.getId());
    }

    default String resolveAvatar(FileMetadataEntity file, PublicUserVideoMapperContext context) {
        if (file == null || file.getUploaderId() == null || context == null || context.avatarByUserId() == null) {
            return null;
        }
        return context.avatarByUserId().get(file.getUploaderId());
    }
}
