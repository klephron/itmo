package ru.itmo.zavar.account.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.itmo.zavar.account.dto.ApartmentDto;
import ru.itmo.zavar.account.entity.ApartmentEntity;

@Mapper(componentModel = "spring")
public interface ApartmentMapper {
    ApartmentEntity toEntity(ApartmentDto apartmentDto);

    ApartmentDto toDto(ApartmentEntity apartmentEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ApartmentEntity partialUpdate(ApartmentDto apartmentDto, @MappingTarget ApartmentEntity apartmentEntity);
}
