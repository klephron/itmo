package ru.itmo.zavar.accountservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.zavar.accountservice.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserEntity, String> {
    Optional<UserEntity> findByUsername(String username);
}