package ru.itmo.zavar.account.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.zavar.account.entity.RadiatorEntity;
import ru.itmo.zavar.account.entity.UserEntity;

public interface RadiatorRepository extends MongoRepository<RadiatorEntity, String> {
}
