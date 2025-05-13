package ru.itmo.zavar.accountservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.zavar.accountservice.entity.RadiatorEntity;
import ru.itmo.zavar.accountservice.entity.UserEntity;

public interface RadiatorRepository extends MongoRepository<RadiatorEntity, String> {
}
