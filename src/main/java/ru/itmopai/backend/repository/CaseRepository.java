package ru.itmopai.backend.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import ru.itmopai.backend.entity.CaseEntity;

import java.util.List;

@Repository
public interface CaseRepository extends CrudRepository<CaseEntity, Long> {
  List<CaseEntity> findAllByCreatorId(Long creatorId);
}
