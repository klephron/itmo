package ru.itmo.zavar.devopslabs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.zavar.devopslabs.entity.CellItemEntity;

import java.util.Optional;

public interface CellItemRepository extends JpaRepository<CellItemEntity, Long> {
}