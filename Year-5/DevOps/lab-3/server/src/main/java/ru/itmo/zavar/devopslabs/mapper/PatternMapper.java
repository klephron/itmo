package ru.itmo.zavar.devopslabs.mapper;

import org.mapstruct.*;
import ru.itmo.zavar.devopslabs.dto.PatternDto;
import ru.itmo.zavar.devopslabs.entity.PatternEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface PatternMapper {
    PatternEntity toEntity(PatternDto patternDto);

    PatternDto toDto(PatternEntity patternEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    PatternEntity partialUpdate(PatternDto patternDto, @MappingTarget PatternEntity patternEntity);
}