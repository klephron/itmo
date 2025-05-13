package ru.itmo.zavar.accountservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.zavar.accountservice.entity.ApartmentEntity;
import ru.itmo.zavar.accountservice.entity.RadiatorToApartmentEntity;

import java.util.List;

public interface RadiatorToApartmentRepository extends MongoRepository<RadiatorToApartmentEntity, String> {
    List<RadiatorToApartmentEntity> findByApartmentId(String apartmentId);
    void deleteByApartmentId(String apartmentId);
}
