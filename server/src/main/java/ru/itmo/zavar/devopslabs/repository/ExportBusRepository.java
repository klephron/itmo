package ru.itmo.zavar.devopslabs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.devopslabs.entity.ExportBusEntity;

public interface ExportBusRepository extends JpaRepository<ExportBusEntity, Long> {
}