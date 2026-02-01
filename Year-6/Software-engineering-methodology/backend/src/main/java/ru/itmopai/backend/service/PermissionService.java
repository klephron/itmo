package ru.itmopai.backend.service;

import java.util.List;

import io.micronaut.security.authentication.Authentication;
import jakarta.inject.Singleton;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.exception.PermissionDeniedException;
import ru.itmopai.backend.repository.UserPermissionRepository;
import ru.itmopai.backend.repository.UserRepository;

@Singleton
public class PermissionService {

  private final UserPermissionRepository repository;
  private final UserRepository userRepository;

  public PermissionService(UserPermissionRepository repository, UserRepository userRepository) {
    this.repository = repository;
    this.userRepository = userRepository;
  }

  public boolean hasPermission(Authentication auth, String key) {
    String username = auth.getName();
    UserEntity userEntity =
        userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    List<String> permissions =
        repository.findById_UserId(userEntity.getId()).stream()
            .map(userPermissionEntity -> userPermissionEntity.getId().getPrivilege()).toList();
    return permissions.stream().anyMatch(p -> matchObjectIdWildcard(p, key));
  }

  public boolean hasPermission(Authentication auth, String scope, Object objectId, String priv) {
    return hasPermission(auth, scope + ":" + objectId + ":" + priv);
  }

  public void checkOrThrow(Authentication auth, String scope, Object objectId, String priv)
      throws PermissionDeniedException {
    if (!hasPermission(auth, scope, objectId, priv)) {
      throw new PermissionDeniedException(
          "Permission denied: " + scope + ":" + objectId + ":" + priv);
    }
  }

  private boolean matchObjectIdWildcard(String pattern, String actual) {
    String[] patParts = pattern.split(":");
    String[] actParts = actual.split(":");
    if (patParts.length != 3 || actParts.length != 3)
      return false;

    boolean scopeMatches = patParts[0].equals("*") || patParts[0].equals(actParts[0]);
    boolean privMatches = patParts[2].equals("*") || patParts[2].equals(actParts[2]);
    boolean objMatches = patParts[1].equals("*") || patParts[1].equals(actParts[1]);

    return scopeMatches && objMatches && privMatches;
  }
}
