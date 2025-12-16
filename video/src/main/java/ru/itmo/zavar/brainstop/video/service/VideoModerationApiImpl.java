package ru.itmo.zavar.brainstop.video.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.video.VideoModerationApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.UserVideoDto;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToUserVideoMapper;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoModerationApiImpl implements VideoModerationApi {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataToUserVideoMapper fileMetadataToUserVideoMapper;

    @Override
    public List<UserVideoDto> findByUploaderId(Long uploaderId) {
        return this.fileMetadataToUserVideoMapper.toDto(this.fileMetadataRepository.findByUploaderId(uploaderId));
    }
}
