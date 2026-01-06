package ru.itmo.zavar.brainstop.video.streaming.dto.response;

import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;

import java.util.List;

public record FeedSessionResponse(
        String feedId,
        List<FeedVideoDto> items,
        Long prevPos,
        Long nextPos
) {
}
