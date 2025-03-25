package ru.itmo.zavar.devopslabs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.devopslabs.entity.CellItemEntity;

public interface CellItemRepository extends JpaRepository<CellItemEntity, Long> {
}