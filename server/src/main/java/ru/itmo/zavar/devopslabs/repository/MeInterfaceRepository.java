package ru.itmo.zavar.devopslabs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.devopslabs.entity.MeInterfaceEntity;

public interface MeInterfaceRepository extends JpaRepository<MeInterfaceEntity, Long> {
}