package ru.itmo.zavar.brainstop.comment.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.zavar.brainstop.comment.model.CommentEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    List<CommentEntity> findByVideoUuidOrderByCreatedAtAsc(UUID videoUuid);

    Page<CommentEntity> findByVideoUuid(UUID videoUuid, Pageable pageable);

    long countByVideoUuid(UUID videoUuid);

    @Query("""
            select c.videoUuid as videoUuid, count(c) as count
            from CommentEntity c
            where c.videoUuid in :uuids
            group by c.videoUuid
            """)
    List<CommentCountView> countByVideoUuidIn(@Param("uuids") Collection<UUID> uuids);

    interface CommentCountView {
        UUID getVideoUuid();

        long getCount();
    }
}
