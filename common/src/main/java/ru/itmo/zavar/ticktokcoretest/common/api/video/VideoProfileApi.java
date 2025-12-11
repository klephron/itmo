package ru.itmo.zavar.ticktokcoretest.common.api.video;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.PublicUserVideoDto;

public interface VideoProfileApi {

    Page<PublicUserVideoDto> findUserVideos(Long uploaderId, Pageable pageable);
}
