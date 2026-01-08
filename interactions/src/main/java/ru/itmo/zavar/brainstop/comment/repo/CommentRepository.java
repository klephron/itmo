package ru.itmo.zavar.brainstop.comment.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.brainstop.comment.model.CommentEntity;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    List<CommentEntity> findByVideoUuidOrderByCreatedAtAsc(UUID videoUuid);

    Page<CommentEntity> findByVideoUuid(UUID videoUuid, Pageable pageable);

    long countByVideoUuid(UUID videoUuid);
}
