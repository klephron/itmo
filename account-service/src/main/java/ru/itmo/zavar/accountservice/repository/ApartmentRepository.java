package ru.itmo.zavar.accountservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.zavar.accountservice.entity.ApartmentEntity;
import ru.itmo.zavar.accountservice.entity.UserEntity;

import java.util.Optional;

public interface ApartmentRepository extends MongoRepository<ApartmentEntity, String> {
    Optional<ApartmentEntity> findByUserId(String userId);
}
