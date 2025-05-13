package ru.itmo.zavar.account.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.zavar.account.entity.ApartmentEntity;
import ru.itmo.zavar.account.entity.RadiatorToApartmentEntity;

import java.util.List;

public interface RadiatorToApartmentRepository extends MongoRepository<RadiatorToApartmentEntity, String> {
    List<RadiatorToApartmentEntity> findByApartmentId(String apartmentId);
    void deleteByApartmentId(String apartmentId);
}
