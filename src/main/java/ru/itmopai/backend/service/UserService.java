package ru.itmopai.backend.service;

import jakarta.inject.Singleton;
import ru.itmopai.backend.entity.TestEntity;
import ru.itmopai.backend.repository.TestRepository;

@Singleton
public class UserService {
  private final TestRepository repo;
  private final BCryptPasswordEncoderService encoder;

  public UserService(TestRepository repo, BCryptPasswordEncoderService encoder) {
    this.repo = repo;
    this.encoder = encoder;
  }

  public TestEntity register(String username, String rawPassword) {
    TestEntity user = new TestEntity();
    user.setUsername(username);
    user.setPassword(encoder.encode(rawPassword));
    user.setLocationLatitude(0D);
    user.setLocationLongitude(0D);
    user.setLocationAltitude(0D);
    return repo.save(user);
  }
}
