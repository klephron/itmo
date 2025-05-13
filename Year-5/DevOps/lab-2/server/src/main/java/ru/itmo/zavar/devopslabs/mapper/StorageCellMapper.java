package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itmo.zavar.devopslabs.dto.StorageCellDto;
import ru.itmo.zavar.devopslabs.entity.StorageCellEntity;

@Mapper(componentModel = "spring")
public interface StorageCellMapper {

    @Mapping(source = "drive.id", target = "driveId")
    StorageCellDto toDto(StorageCellEntity cell);

    @Mapping(target = "drive", ignore = true)
    StorageCellEntity fromDto(StorageCellDto dto);
}