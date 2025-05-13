package ru.itmo.zavar.task;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;

@FunctionalInterface
public interface MongoClientFactory {
    MongoClient create(MongoClientSettings settings);
}

