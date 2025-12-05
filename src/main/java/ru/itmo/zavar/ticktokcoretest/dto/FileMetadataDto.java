package ru.itmo.zavar.ticktokcoretest.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity}
 */
public record FileMetadataDto(Long id, String name, UUID uuid, String contentType, Long contentSize,
                              Long uploaderId, String uploaderName, LocalDateTime uploadedAt, Long likesCount,
                              String thumbnailKey) implements Serializable {
}