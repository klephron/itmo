package ru.itmopai.backend.mapper;

import java.util.List;

import org.mapstruct.*;

import ru.itmopai.backend.dto.NotificationDto;
import ru.itmopai.backend.entity.NotificationEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface NotificationMapper {
  @Mapping(source = "userId", target = "user.id")
  NotificationEntity toEntity(NotificationDto notificationDto);

  @Mapping(source = "user.id", target = "userId")
  NotificationDto toDto(NotificationEntity notificationEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(source = "userId", target = "user.id")
  NotificationEntity partialUpdate(NotificationDto notificationDto,
      @MappingTarget NotificationEntity notificationEntity);

  List<NotificationEntity> toEntity(List<NotificationDto> notificationDto);

  List<NotificationDto> toDto(List<NotificationEntity> notificationEntity);
}
