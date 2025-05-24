package ru.itmopai.backend.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import ru.itmopai.backend.entity.IncidentEntity;

@Repository
public interface IncidentRepository extends CrudRepository<IncidentEntity, Long> {
}
