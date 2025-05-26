package ru.itmopai.backend.mapper;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import ru.itmopai.backend.entity.UserCaseEntity;

import java.util.List;

@Repository
public interface UserCaseRepository
    extends CrudRepository<UserCaseEntity, UserCaseEntity.UserCaseId> {
  List<UserCaseEntity> findAllById_RelatedCaseId(Long id);
}
