package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itmo.zavar.devopslabs.dto.MeInterfaceDto;
import ru.itmo.zavar.devopslabs.entity.MeInterfaceEntity;

@Mapper(componentModel = "spring")
public interface MeInterfaceMapper {

    @Mapping(source = "network.id", target = "networkId")
    MeInterfaceDto toDto(MeInterfaceEntity entity);

    @Mapping(target = "network", ignore = true)
    MeInterfaceEntity fromDto(MeInterfaceDto dto);
}