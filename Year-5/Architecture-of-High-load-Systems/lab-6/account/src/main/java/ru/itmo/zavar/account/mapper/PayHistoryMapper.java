package ru.itmo.zavar.account.mapper;

import org.mapstruct.*;
import ru.itmo.zavar.account.dto.PayHistoryDto;
import ru.itmo.zavar.account.entity.PayHistoryEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface PayHistoryMapper {
    PayHistoryEntity toEntity(PayHistoryDto payHistoryDto);

    PayHistoryDto toDto(PayHistoryEntity payHistoryEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    PayHistoryEntity partialUpdate(PayHistoryDto payHistoryDto, @MappingTarget PayHistoryEntity payHistoryEntity);
}
