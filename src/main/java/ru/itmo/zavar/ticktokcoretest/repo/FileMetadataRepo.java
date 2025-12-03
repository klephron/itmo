package ru.itmo.zavar.ticktokcoretest.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.zavar.ticktokcoretest.model.FileMetadata;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepo extends JpaRepository<FileMetadata, Long> {

    @Query("select f from FileMetadata f where f.uuid = :fileUUID")
    Optional<FileMetadata> findByUuid(UUID fileUUID);

    List<FileMetadata> findAllByUploaderId(Long uploaderId);

    List<FileMetadata> findByUploaderId(Long uploaderId);


    void deleteByUuid(UUID fileUUID);
}
