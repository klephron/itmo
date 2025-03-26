package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import ru.itmo.zavar.devopslabs.dto.ItemDto;
import ru.itmo.zavar.devopslabs.entity.ItemEntity;

@Mapper(componentModel = "spring")
public interface ItemMapper {
    ItemDto toDto(ItemEntity itemEntity);
    ItemEntity fromDto(ItemDto dto);
}
