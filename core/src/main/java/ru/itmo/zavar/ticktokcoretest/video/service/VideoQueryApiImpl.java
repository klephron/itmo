package ru.itmo.zavar.ticktokcoretest.video.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.common.api.video.VideoQueryApi;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.ticktokcoretest.video.mapper.FileMetadataMapper;
import ru.itmo.zavar.ticktokcoretest.video.repo.FileMetadataRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoQueryApiImpl implements VideoQueryApi {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataMapper fileMetadataMapper;
    private final FeedService feedService;

    @Override
    public List<FileMetadataDto> listAll() {
        return this.fileMetadataMapper.toDto(this.fileMetadataRepository.findAll());
    }

    @Override
    public List<FileMetadataDto> listByUploader(Long userId) {
        return this.fileMetadataMapper.toDto(this.fileMetadataRepository.findAllByUploaderId(userId));
    }

    @Override
    public Page<FeedVideoDto> getFeed(Long currentUserId, Pageable pageable) {
        return this.feedService.getFeed(currentUserId, pageable);
    }
}
