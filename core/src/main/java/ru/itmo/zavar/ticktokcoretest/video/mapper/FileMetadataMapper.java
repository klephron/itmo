package ru.itmo.zavar.ticktokcoretest.video.mapper;

import org.mapstruct.*;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.ticktokcoretest.video.model.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.video.model.VideoRenditionEntity;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileMetadataMapper {
    FileMetadataEntity toEntity(FileMetadataDto fileMetadataDto);

    @Mapping(target = "availableQualities", expression = "java(mapQualities(fileMetadataEntity))")
    FileMetadataDto toDto(FileMetadataEntity fileMetadataEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    FileMetadataEntity partialUpdate(FileMetadataDto fileMetadataDto, @MappingTarget FileMetadataEntity fileMetadataEntity);

    List<FileMetadataEntity> toEntity(List<FileMetadataDto> fileMetadataDto);

    List<FileMetadataDto> toDto(List<FileMetadataEntity> fileMetadataEntity);

    default List<Integer> mapQualities(FileMetadataEntity entity) {
        if (entity == null || entity.getRenditions() == null) {
            return List.of();
        }
        return entity.getRenditions().stream()
                .map(VideoRenditionEntity::getHeight)
                .sorted()
                .collect(Collectors.toList());
    }
}
