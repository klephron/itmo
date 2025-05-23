package ru.itmopai.backend.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import java.util.Optional;
import ru.itmopai.backend.entity.TestEntity;

@Repository
public interface TestRepository extends CrudRepository<TestEntity, Long> {
  Optional<TestEntity> findByUsername(String username);
}
