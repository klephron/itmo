package ru.itmopai.backend.mapper;

import org.mapstruct.*;
import ru.itmopai.backend.dto.response.UserInfoResponse;
import ru.itmopai.backend.entity.UserEntity;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface UserInfoMapper {
  UserEntity toEntity(UserInfoResponse userInfoResponse);

  UserInfoResponse toDto(UserEntity userEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  UserEntity partialUpdate(UserInfoResponse userInfoResponse, @MappingTarget UserEntity userEntity);

  List<UserEntity> toEntity(List<UserInfoResponse> userInfoResponse);

  List<UserInfoResponse> toDto(List<UserEntity> userEntity);
}
