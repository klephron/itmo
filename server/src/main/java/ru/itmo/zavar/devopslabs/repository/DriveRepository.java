package ru.itmo.zavar.devopslabs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.devopslabs.entity.DriveEntity;

public interface DriveRepository extends JpaRepository<DriveEntity, Long> {
}