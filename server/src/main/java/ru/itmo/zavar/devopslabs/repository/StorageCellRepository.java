package ru.itmo.zavar.devopslabs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.devopslabs.entity.StorageCellEntity;

public interface StorageCellRepository extends JpaRepository<StorageCellEntity, Long> {
}