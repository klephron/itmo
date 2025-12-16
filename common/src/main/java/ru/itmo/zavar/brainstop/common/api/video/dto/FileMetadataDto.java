package ru.itmo.zavar.brainstop.common.api.video.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FileMetadataDto(Long id, String name, UUID uuid, String contentType, Long contentSize,
                              Long uploaderId, String uploaderName, LocalDateTime uploadedAt, Long likesCount,
                              String thumbnailKey, Integer defaultHeight, List<Integer> availableQualities,
                              String description) implements Serializable {
}
