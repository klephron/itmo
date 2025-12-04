package ru.itmo.zavar.ticktokcoretest.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.ticktokcoretest.model.CommentEntity;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    List<ru.itmo.zavar.ticktokcoretest.model.CommentEntity> findByVideoUuidOrderByCreatedAtAsc(UUID videoUuid);

    long countByVideoUuid(UUID videoUuid);
}

