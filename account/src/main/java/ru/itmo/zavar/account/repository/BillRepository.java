package ru.itmo.zavar.account.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.zavar.account.entity.BillEntity;

import java.util.List;

public interface BillRepository extends MongoRepository<BillEntity, String> {
    List<BillEntity> findByUserId(String userId);
}
