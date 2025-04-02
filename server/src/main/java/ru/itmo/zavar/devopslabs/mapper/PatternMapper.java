package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.itmo.zavar.devopslabs.dto.PatternEntityDto;
import ru.itmo.zavar.devopslabs.entity.ItemEntity;
import ru.itmo.zavar.devopslabs.entity.MeInterfaceEntity;
import ru.itmo.zavar.devopslabs.entity.PatternEntity;

@Mapper(componentModel = "spring")
public interface PatternMapper {

    @Mapping(source = "outputItem.id", target = "outputItemId")
    @Mapping(source = "meInterface.id", target = "meInterfaceId")
    PatternEntityDto toDto(PatternEntity pattern);

    @Mapping(target = "outputItem", source = "outputItemId", qualifiedByName = "mapOutputItem")
    @Mapping(target = "meInterface", source = "meInterfaceId", qualifiedByName = "mapMeInterface")
    PatternEntity fromDto(PatternEntityDto dto);

    @Named("mapOutputItem")
    default ItemEntity mapOutputItem(Long id) {
        if (id == null) return null;
        ItemEntity item = new ItemEntity();
        item.setId(id);
        return item;
    }

    @Named("mapMeInterface")
    default MeInterfaceEntity mapMeInterface(Long id) {
        if (id == null) return null;
        MeInterfaceEntity iface = new MeInterfaceEntity();
        iface.setId(id);
        return iface;
    }
}
