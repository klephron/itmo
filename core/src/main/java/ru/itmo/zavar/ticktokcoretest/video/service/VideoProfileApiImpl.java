package ru.itmo.zavar.ticktokcoretest.video.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.common.api.video.VideoProfileApi;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.PublicUserVideoDto;
import ru.itmo.zavar.ticktokcoretest.video.mapper.FileMetadataToPublicUserVideoMapper;
import ru.itmo.zavar.ticktokcoretest.video.repo.FileMetadataRepository;

@Service
@RequiredArgsConstructor
public class VideoProfileApiImpl implements VideoProfileApi {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataToPublicUserVideoMapper fileMetadataToPublicUserVideoMapper;

    @Override
    public Page<PublicUserVideoDto> findUserVideos(Long uploaderId, Pageable pageable) {
        return this.fileMetadataRepository
                .findByUploaderId(uploaderId, pageable)
                .map(this.fileMetadataToPublicUserVideoMapper::toDto);
    }
}
