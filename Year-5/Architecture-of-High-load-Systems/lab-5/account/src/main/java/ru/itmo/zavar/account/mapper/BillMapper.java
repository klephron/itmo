package ru.itmo.zavar.account.mapper;

import org.mapstruct.*;
import ru.itmo.zavar.account.dto.BillDto;
import ru.itmo.zavar.account.entity.BillEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface BillMapper {
    BillEntity toEntity(BillDto billDto);

    BillDto toDto(BillEntity billEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    BillEntity partialUpdate(BillDto billDto, @MappingTarget BillEntity billEntity);
}
