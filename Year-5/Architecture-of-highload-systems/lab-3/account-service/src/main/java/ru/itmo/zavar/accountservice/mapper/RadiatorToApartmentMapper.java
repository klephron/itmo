package ru.itmo.zavar.accountservice.mapper;

import org.mapstruct.*;
import ru.itmo.zavar.accountservice.dto.RadiatorToApartmentDto;
import ru.itmo.zavar.accountservice.entity.RadiatorToApartmentEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface RadiatorToApartmentMapper {
    RadiatorToApartmentEntity toEntity(RadiatorToApartmentDto radiatorToApartmentDto);

    RadiatorToApartmentDto toDto(RadiatorToApartmentEntity radiatorToApartmentEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    RadiatorToApartmentEntity partialUpdate(RadiatorToApartmentDto radiatorToApartmentDto, @MappingTarget RadiatorToApartmentEntity radiatorToApartmentEntity);
}