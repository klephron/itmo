package ru.itmopai.backend.service;

import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.entity.UserPermissionEntity;
import ru.itmopai.backend.exception.UserExistsException;
import ru.itmopai.backend.repository.UserPermissionRepository;
import ru.itmopai.backend.repository.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Singleton
@AllArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final UserPermissionRepository userPermissionRepo;
  private final BCryptPasswordEncoderService encoder;

  public UserEntity register(String username, String rawPassword, String name, String surname, String patronymic) throws UserExistsException {
    userRepository.findByUsername(username).ifPresent(userEntity -> { throw new UserExistsException("User already exists"); });
    UserEntity user = new UserEntity();
    user.setUsername(username);
    user.setPassword(encoder.encode(rawPassword));
    user.setLocationLatitude(0D);
    user.setLocationLongitude(0D);
    user.setLocationAltitude(0D);
    user.setName(name);
    user.setSurname(surname);
    user.setPatronymic(patronymic);
    return userRepository.save(user);
  }

  public List<UserEntity> getAllUsers() {
    return userRepository.findAll();
  }

  public Optional<UserEntity> getUser(Long id) {
    return userRepository.findById(id);
  }

  public void removeUser(Long id) {
    userRepository.deleteById(id);
  }

  public UserEntity updateLocation(Long id, Double latitude, Double longitude, Double altitude) throws NoSuchElementException {
    UserEntity user = userRepository.findById(id).orElseThrow();
    user.setLocationLatitude(latitude);
    user.setLocationLongitude(longitude);
    user.setLocationAltitude(altitude);
    return userRepository.update(user);
  }

  public List<String> getUserPermissions(Long userId) throws NoSuchElementException {
    userRepository.findById(userId).orElseThrow();
    return userPermissionRepo.findById_UserId(userId).stream().map(userPermissionEntity -> userPermissionEntity.getId().getPrivilege()).toList();
  }

  @Transactional
  public void setUserPermissions(Long userId, List<String> privileges) throws NoSuchElementException {
    UserEntity userEntity = userRepository.findById(userId).orElseThrow();
    userPermissionRepo.deleteAll(userPermissionRepo.findById_UserId(userId));
    for (String privilege : privileges) {
      UserPermissionEntity.UserPermissionId userPermissionId = new UserPermissionEntity.UserPermissionId(userEntity, privilege);
      userPermissionRepo.save(new UserPermissionEntity(userPermissionId));
    }
  }

}
