package ru.itmopai.backend.repository;

import java.util.List;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import ru.itmopai.backend.entity.TemplateEntity;

@Repository
public interface TemplateRepository extends CrudRepository<TemplateEntity, Long> {
  List<TemplateEntity> findAllByRelatedCase_Id(Long id);
}
