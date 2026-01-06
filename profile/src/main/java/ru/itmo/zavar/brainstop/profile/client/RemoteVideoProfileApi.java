package ru.itmo.zavar.brainstop.profile.client;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.video.VideoProfileApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;

@Component
@RequiredArgsConstructor
public class RemoteVideoProfileApi implements VideoProfileApi {

    private final VideoInternalClient videoInternalClient;

    @Override
    public Page<PublicUserVideoDto> findUserVideos(Long uploaderId, Long currentUserId, Pageable pageable) {
        String sortParam = pageable.getSort().isSorted()
                ? pageable.getSort().iterator().next().getProperty() + "," + pageable.getSort().iterator().next().getDirection().name().toLowerCase()
                : "uploadedAt,desc";
        PageResponse<PublicUserVideoDto> response = this.videoInternalClient.getUserVideos(
                uploaderId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortParam,
                currentUserId
        );
        return new PageImpl<>(
                response.content(),
                pageable,
                response.totalElements()
        );
    }
}
