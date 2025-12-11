package ru.itmo.zavar.ticktokcoretest.profile.client;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.ticktokcoretest.common.api.PageResponse;
import ru.itmo.zavar.ticktokcoretest.common.api.video.VideoProfileApi;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.PublicUserVideoDto;

@Component
@RequiredArgsConstructor
public class RemoteVideoProfileApi implements VideoProfileApi {

    private final VideoInternalClient videoInternalClient;

    @Override
    public Page<PublicUserVideoDto> findUserVideos(Long uploaderId, Pageable pageable) {
        PageResponse<PublicUserVideoDto> response = this.videoInternalClient.getUserVideos(
                uploaderId,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
        return new PageImpl<>(
                response.content(),
                pageable,
                response.totalElements()
        );
    }
}
