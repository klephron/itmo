package ru.itmo.zavar.ticktokcoretest.mapper;

import org.mapstruct.*;
import ru.itmo.zavar.ticktokcoretest.dto.FeedVideoDto;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;

import java.time.ZoneOffset;
import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {ZoneOffset.class}
)
public interface FileMetadataToFeedMapper {
    @Mapping(target = "uploaderAvatarUrl", ignore = true) // заполним вручную
    @Mapping(target = "uploadedAt",
            expression = "java( src.getUploadedAt() != null ? src.getUploadedAt().toInstant(ZoneOffset.UTC) : null)")
    @Mapping(target = "likedByMe", ignore = true)
    @Mapping(target = "subscribedToAuthor", ignore = true)
    FeedVideoDto toDto(FileMetadataEntity src);

    List<FeedVideoDto> toDto(List<FileMetadataEntity> src);
}
