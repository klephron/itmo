package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itmo.zavar.devopslabs.dto.ExportBusEntityDto;
import ru.itmo.zavar.devopslabs.entity.ExportBusEntity;

@Mapper(componentModel = "spring")
public interface ExportBusMapper {

    @Mapping(source = "network.id", target = "networkId")
    ExportBusEntityDto toDto(ExportBusEntity entity);

    @Mapping(target = "network", ignore = true)
    ExportBusEntity fromDto(ExportBusEntityDto dto);
}
