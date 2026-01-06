package ru.itmo.zavar.brainstop.common.api.video;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;

public interface VideoProfileApi {

    Page<PublicUserVideoDto> findUserVideos(Long uploaderId, Long currentUserId, Pageable pageable);
}
