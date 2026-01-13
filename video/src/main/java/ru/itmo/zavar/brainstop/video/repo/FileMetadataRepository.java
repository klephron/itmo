package ru.itmo.zavar.brainstop.video.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import com.pgvector.PGvector;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadataEntity, Long> {

    Optional<FileMetadataEntity> findByUuid(UUID fileUUID);

    List<FileMetadataEntity> findAllByUploaderId(Long uploaderId);

    Page<FileMetadataEntity> findByUploaderId(Long uploaderId, Pageable pageable);

    List<FileMetadataEntity> findByUploaderId(Long uploaderId);

    List<FileMetadataEntity> findByUuidIn(Collection<UUID> uuids);

    void deleteByUuid(UUID fileUUID);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FileMetadataEntity f where f.id = :id")
    Optional<FileMetadataEntity> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "renditions")
    Optional<FileMetadataEntity> findWithRenditionsByUuid(UUID fileUUID);

    @Query(
            value = "select * from file_meta where combined_embedding is not null " +
                    "and combined_embedding <=> CAST(:vec AS vector) <= :maxDistance " +
                    "order by combined_embedding <=> CAST(:vec AS vector)",
            countQuery = "select count(*) from file_meta where combined_embedding is not null " +
                    "and combined_embedding <=> CAST(:vec AS vector) <= :maxDistance",
            nativeQuery = true
    )
    Page<FileMetadataEntity> searchByEmbedding(@Param("vec") String queryVecLiteral,
                                               @Param("maxDistance") double maxDistance,
                                               Pageable pageable);

    Page<FileMetadataEntity> findByCombinedEmbeddingIsNotNull(Pageable pageable);
}
