package ru.itmo.zavar.brainstop.video.streaming.dto.response;

import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;

import java.util.List;

public record FeedSessionResponse(
        String feedId,
        List<FeedVideoDto> items,
        long fromIndex,
        long toIndex,
        long pos,
        long baseIndex,
        boolean hasPrev,
        boolean hasNext
) {
}
