package ru.itmopai.backend.repository;

import java.util.List;
import java.util.Optional;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import ru.itmopai.backend.entity.UserCaseEntity;

@Repository
public interface UserCaseRepository
    extends CrudRepository<UserCaseEntity, UserCaseEntity.UserCaseId> {
  List<UserCaseEntity> findAllById_RelatedCaseId(Long id);

  List<UserCaseEntity> findAllById_UserId(Long id);

  Optional<UserCaseEntity> findById(UserCaseEntity.UserCaseId id);
}
