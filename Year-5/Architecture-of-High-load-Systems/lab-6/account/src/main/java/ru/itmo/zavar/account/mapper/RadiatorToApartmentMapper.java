package ru.itmo.zavar.account.mapper;

import org.mapstruct.*;
import ru.itmo.zavar.account.dto.RadiatorToApartmentDto;
import ru.itmo.zavar.account.entity.RadiatorToApartmentEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface RadiatorToApartmentMapper {
    RadiatorToApartmentEntity toEntity(RadiatorToApartmentDto radiatorToApartmentDto);

    RadiatorToApartmentDto toDto(RadiatorToApartmentEntity radiatorToApartmentEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    RadiatorToApartmentEntity partialUpdate(RadiatorToApartmentDto radiatorToApartmentDto, @MappingTarget RadiatorToApartmentEntity radiatorToApartmentEntity);
}
