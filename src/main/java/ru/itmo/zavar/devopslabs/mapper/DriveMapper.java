package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itmo.zavar.devopslabs.dto.DriveEntityDto;
import ru.itmo.zavar.devopslabs.entity.DriveEntity;

@Mapper(componentModel = "spring")
public interface DriveMapper {
    @Mapping(source = "network.id", target = "networkId")
    DriveEntityDto toDto(DriveEntity driveEntity);

    @Mapping(target = "network", ignore = true)
    DriveEntity fromDto(DriveEntityDto driveDto);
}
