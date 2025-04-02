package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itmo.zavar.devopslabs.dto.StorageCellEntityDto;
import ru.itmo.zavar.devopslabs.entity.StorageCellEntity;

@Mapper(componentModel = "spring")
public interface StorageCellMapper {

    @Mapping(source = "drive.id", target = "driveId")
    StorageCellEntityDto toDto(StorageCellEntity cell);

    @Mapping(target = "drive", ignore = true)
    StorageCellEntity fromDto(StorageCellEntityDto dto);
}