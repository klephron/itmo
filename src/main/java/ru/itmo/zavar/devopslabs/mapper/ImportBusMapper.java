package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itmo.zavar.devopslabs.dto.ImportBusEntityDto;
import ru.itmo.zavar.devopslabs.entity.ImportBusEntity;

@Mapper(componentModel = "spring")
public interface ImportBusMapper {

    @Mapping(source = "network.id", target = "networkId")
    ImportBusEntityDto toDto(ImportBusEntity entity);

    @Mapping(target = "network", ignore = true)
    ImportBusEntity fromDto(ImportBusEntityDto dto);
}
