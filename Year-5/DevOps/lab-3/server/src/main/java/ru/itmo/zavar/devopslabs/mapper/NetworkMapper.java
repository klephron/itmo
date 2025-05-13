package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import ru.itmo.zavar.devopslabs.dto.NetworkDto;
import ru.itmo.zavar.devopslabs.entity.NetworkEntity;

@Mapper(componentModel = "spring")
public interface NetworkMapper {
    NetworkDto toDto(NetworkEntity entity);
    NetworkEntity fromDto(NetworkDto dto);
}
