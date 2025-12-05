package ru.itmo.zavar.ticktokcoretest.mapper;

import org.mapstruct.*;
import ru.itmo.zavar.ticktokcoretest.dto.FileMetadataDto;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileMetadataMapper {
    FileMetadataEntity toEntity(FileMetadataDto fileMetadataDto);

    FileMetadataDto toDto(FileMetadataEntity fileMetadataEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    FileMetadataEntity partialUpdate(FileMetadataDto fileMetadataDto, @MappingTarget FileMetadataEntity fileMetadataEntity);

    List<FileMetadataEntity> toEntity(List<FileMetadataDto> fileMetadataDto);

    List<FileMetadataDto> toDto(List<FileMetadataEntity> fileMetadataEntity);
}