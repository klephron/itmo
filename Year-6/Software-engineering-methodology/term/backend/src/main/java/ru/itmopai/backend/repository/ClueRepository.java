package ru.itmopai.backend.repository;

import java.util.List;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import ru.itmopai.backend.entity.ClueEntity;

@Repository
public interface ClueRepository extends CrudRepository<ClueEntity, Long> {
  List<ClueEntity> findAllByRelatedCase_Id(Long id);

  long countByRelatedCase_IdAndIdIn(Long caseId, List<Long> ids);

}
