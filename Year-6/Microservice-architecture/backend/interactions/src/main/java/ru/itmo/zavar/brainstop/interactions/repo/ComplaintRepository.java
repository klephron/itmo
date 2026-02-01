package ru.itmo.zavar.brainstop.interactions.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintEntity;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintStatus;

import java.util.List;
import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<ComplaintEntity, UUID> {
    List<ComplaintEntity> findByStatusIn(List<ComplaintStatus> statuses);
}
