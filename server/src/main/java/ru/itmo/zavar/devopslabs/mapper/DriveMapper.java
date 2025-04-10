package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itmo.zavar.devopslabs.dto.DriveDto;
import ru.itmo.zavar.devopslabs.entity.DriveEntity;

@Mapper(componentModel = "spring")
public interface DriveMapper {
    @Mapping(source = "network.id", target = "networkId")
    DriveDto toDto(DriveEntity driveEntity);

    @Mapping(target = "network", ignore = true)
    DriveEntity fromDto(DriveDto driveDto);
}
