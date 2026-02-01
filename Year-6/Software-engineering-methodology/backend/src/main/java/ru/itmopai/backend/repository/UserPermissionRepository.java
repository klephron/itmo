package ru.itmopai.backend.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import ru.itmopai.backend.entity.UserPermissionEntity;

import java.util.List;

@Repository
public interface UserPermissionRepository
    extends CrudRepository<UserPermissionEntity, UserPermissionEntity.UserPermissionId> {
  // private static final Map<Long, List<String>> PERMISSIONS = Map.of(
  // 6L, List.of("document:42:read", "device:*:edit", "document:*:view"),
  // 7L, List.of("document:1:read", "document:*:delete")
  // );
  //
  // public List<String> getPermissionsForUser(Long userId) {
  // return PERMISSIONS.getOrDefault(userId, Collections.emptyList());
  // }
  List<UserPermissionEntity> findById_UserId(Long userId);
}
