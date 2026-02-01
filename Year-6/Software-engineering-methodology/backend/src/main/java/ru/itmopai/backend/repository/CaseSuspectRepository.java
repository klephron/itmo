package ru.itmopai.backend.repository;

import java.util.List;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import ru.itmopai.backend.entity.CaseSuspectEntity;

@Repository
public interface CaseSuspectRepository extends CrudRepository<CaseSuspectEntity, Long> {
  List<CaseSuspectEntity> findAllByRelatedCase_Id(Long id);
}
