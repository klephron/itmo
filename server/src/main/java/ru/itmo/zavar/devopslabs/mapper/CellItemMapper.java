package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itmo.zavar.devopslabs.dto.CellItemDto;
import ru.itmo.zavar.devopslabs.entity.CellItemEntity;

@Mapper(componentModel = "spring")
public interface CellItemMapper {

    @Mapping(source = "storageCell.id", target = "storageCellId")
    CellItemDto toDto(CellItemEntity cellItem);

    @Mapping(target = "storageCell", ignore = true)
    CellItemEntity fromDto(CellItemDto dto);
}
