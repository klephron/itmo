package ru.itmo.zavar.ticktokcoretest.video.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import ru.itmo.zavar.ticktokcoretest.video.model.FileMetadataEntity;
import com.pgvector.PGvector;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadataEntity, Long> {

    Optional<FileMetadataEntity> findByUuid(UUID fileUUID);

    List<FileMetadataEntity> findAllByUploaderId(Long uploaderId);

    Page<FileMetadataEntity> findByUploaderId(Long uploaderId, Pageable pageable);

    List<FileMetadataEntity> findByUploaderId(Long uploaderId);

    void deleteByUuid(UUID fileUUID);

    @EntityGraph(attributePaths = "renditions")
    Optional<FileMetadataEntity> findWithRenditionsByUuid(UUID fileUUID);

    @Query(
            value = "select * from file_meta where combined_embedding is not null " +
                    "order by combined_embedding <-> CAST(?1 AS vector)",
            countQuery = "select count(*) from file_meta where combined_embedding is not null",
            nativeQuery = true
    )
    Page<FileMetadataEntity> searchByEmbedding(String queryVecLiteral, Pageable pageable);
}
