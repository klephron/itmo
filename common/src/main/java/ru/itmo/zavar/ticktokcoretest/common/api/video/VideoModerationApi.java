package ru.itmo.zavar.ticktokcoretest.common.api.video;

import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.UserVideoDto;

import java.util.List;

public interface VideoModerationApi {

    List<UserVideoDto> findByUploaderId(Long uploaderId);
}
