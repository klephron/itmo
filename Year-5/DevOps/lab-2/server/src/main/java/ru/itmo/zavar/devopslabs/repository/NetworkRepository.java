package ru.itmo.zavar.devopslabs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.devopslabs.entity.NetworkEntity;

public interface NetworkRepository extends JpaRepository<NetworkEntity, Long> {
}