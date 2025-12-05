package ru.itmo.zavar.ticktokcoretest.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadataEntity, Long> {

    Optional<FileMetadataEntity> findByUuid(UUID fileUUID);

    List<FileMetadataEntity> findAllByUploaderId(Long uploaderId);

    List<FileMetadataEntity> findByUploaderId(Long uploaderId);

    void deleteByUuid(UUID fileUUID);
}
