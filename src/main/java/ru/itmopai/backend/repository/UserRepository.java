package ru.itmopai.backend.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import java.util.Optional;
import ru.itmopai.backend.entity.UserEntity;

@Repository
public interface UserRepository extends CrudRepository<UserEntity, Long> {
  Optional<UserEntity> findByUsername(String username);
}
