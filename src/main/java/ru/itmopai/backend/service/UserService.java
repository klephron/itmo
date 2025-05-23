package ru.itmopai.backend.service;

import jakarta.inject.Singleton;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.exception.UserExistsException;
import ru.itmopai.backend.repository.UserRepository;

@Singleton
public class UserService {
  private final UserRepository repo;
  private final BCryptPasswordEncoderService encoder;

  public UserService(UserRepository repo, BCryptPasswordEncoderService encoder) {
    this.repo = repo;
    this.encoder = encoder;
  }

  public UserEntity register(String username, String rawPassword) throws UserExistsException {
    repo.findByUsername(username).ifPresent(userEntity -> { throw new UserExistsException("User already exists"); });
    UserEntity user = new UserEntity();
    user.setUsername(username);
    user.setPassword(encoder.encode(rawPassword));
    user.setLocationLatitude(0D);
    user.setLocationLongitude(0D);
    user.setLocationAltitude(0D);
    return repo.save(user);
  }
}
