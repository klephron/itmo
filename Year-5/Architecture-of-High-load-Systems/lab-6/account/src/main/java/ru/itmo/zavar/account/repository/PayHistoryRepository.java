package ru.itmo.zavar.account.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.zavar.account.entity.PayHistoryEntity;

import java.util.List;

public interface PayHistoryRepository extends MongoRepository<PayHistoryEntity, String> {
    List<PayHistoryEntity> findAllByUserId(String userId);
}
