package ru.itmo.zavar.ticktokcoretest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.itmo.zavar.ticktokcoretest.dto.PublicUserVideoDto;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;

import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = {ZoneOffset.class})
public interface FileMetadataToPublicUserVideoMapper {

    @Mapping(target = "thumbnailUrl",
            expression = "java(\"/streaming/video/thumbnail/\" + file.getUuid())")
    @Mapping(target = "uploadedAt",
            expression = "java(file.getUploadedAt() != null ? "
                    + "file.getUploadedAt().toInstant(ZoneOffset.UTC) : null)")
    PublicUserVideoDto toDto(FileMetadataEntity file);

    List<PublicUserVideoDto> toDto(List<FileMetadataEntity> files);
}

