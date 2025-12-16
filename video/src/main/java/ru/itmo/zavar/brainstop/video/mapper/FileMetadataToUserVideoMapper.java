package ru.itmo.zavar.brainstop.video.mapper;

import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummary;
import ru.itmo.zavar.brainstop.common.api.video.dto.UserVideoDto;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;

import java.time.ZoneOffset;
import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        imports = {ZoneOffset.class}
)
public abstract class FileMetadataToUserVideoMapper {

    @Autowired
    protected UserProfilePublicApi userProfilePublicApi;

    @Mapping(target = "uploadedAt",
            expression = "java(fileMetadataEntity.getUploadedAt() != null " +
                    "? fileMetadataEntity.getUploadedAt().toInstant(ZoneOffset.UTC) " +
                    ": null)")
    @Mapping(target = "thumbnailUrl", ignore = true)
    @Mapping(target = "uploaderAvatarUrl", ignore = true)
    public abstract UserVideoDto toDto(FileMetadataEntity fileMetadataEntity);

    public abstract List<UserVideoDto> toDto(List<FileMetadataEntity> fileMetadataEntity);

    @AfterMapping
    protected void enrich(FileMetadataEntity src, @MappingTarget UserVideoDto.UserVideoDtoBuilder target) {
        String thumbnailUrl = (src.getThumbnailKey() != null)
                ? "/api/streaming/video/thumbnail/" + src.getUuid() : null;
        target.thumbnailUrl(thumbnailUrl);

        if (src.getUploaderId() != null) {
            target.uploaderAvatarUrl(
                    userProfilePublicApi.findUserSummary(src.getUploaderId())
                            .map(UserSummary::avatarUrl)
                            .orElse(null)
            );
        }
    }
}
