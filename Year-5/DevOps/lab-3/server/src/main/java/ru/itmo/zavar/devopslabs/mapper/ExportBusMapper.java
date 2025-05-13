package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itmo.zavar.devopslabs.dto.ExportBusDto;
import ru.itmo.zavar.devopslabs.entity.ExportBusEntity;

@Mapper(componentModel = "spring")
public interface ExportBusMapper {

    @Mapping(source = "network.id", target = "networkId")
    ExportBusDto toDto(ExportBusEntity entity);

    @Mapping(target = "network", ignore = true)
    ExportBusEntity fromDto(ExportBusDto dto);
}
