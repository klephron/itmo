package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itmo.zavar.devopslabs.dto.ImportBusDto;
import ru.itmo.zavar.devopslabs.entity.ImportBusEntity;

@Mapper(componentModel = "spring")
public interface ImportBusMapper {

    @Mapping(source = "network.id", target = "networkId")
    ImportBusDto toDto(ImportBusEntity entity);

    @Mapping(target = "network", ignore = true)
    ImportBusEntity fromDto(ImportBusDto dto);
}
