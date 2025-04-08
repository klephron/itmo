package ru.itmo.zavar.accountservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.zavar.accountservice.entity.BillEntity;

import java.util.List;

public interface BillRepository extends MongoRepository<BillEntity, String> {
    List<BillEntity> findByUserId(String userId);
}
