package ru.itmo.zavar.brainstop.common.api.video;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;

import java.util.List;

public interface VideoQueryApi {

    List<FileMetadataDto> listAll();

    List<FileMetadataDto> listByUploader(Long userId);

    Page<FeedVideoDto> getFeed(Long currentUserId, Pageable pageable);
}
