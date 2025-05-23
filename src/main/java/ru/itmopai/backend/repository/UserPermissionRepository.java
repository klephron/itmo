package ru.itmopai.backend.repository;

import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Singleton
public class UserPermissionRepository {

  private static final Map<Long, List<String>> PERMISSIONS = Map.of(
    6L, List.of("document:42:read", "device:*:edit", "document:*:view"),
    7L, List.of("document:1:read", "document:*:delete")
  );

  public List<String> getPermissionsForUser(Long userId) {
    return PERMISSIONS.getOrDefault(userId, Collections.emptyList());
  }
}
