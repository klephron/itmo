package ru.itmo.zavar.ticktokcoretest.mapper;

import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.zavar.ticktokcoretest.dto.UserVideoDto;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;

import java.time.ZoneOffset;
import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        imports = {ZoneOffset.class}
)
public abstract class FileMetadataToUserVideoMapper {

    @Autowired
    protected UserRepository userRepository;

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
                ? "/streaming/video/thumbnail/" + src.getUuid() : null;
        target.thumbnailUrl(thumbnailUrl);

        String avatarUrl = null;
        if (src.getUploaderId() != null) {
            UserEntity uploader = userRepository.findById(src.getUploaderId()).orElse(null);
            if (uploader != null && uploader.getAvatarKey() != null) {
                avatarUrl = "/api/users/" + uploader.getId() + "/avatar";
            }
        }
        target.uploaderAvatarUrl(avatarUrl);
    }
}
