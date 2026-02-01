package ru.itmo.zavar.brainstop.video.streaming.dto.request;

import java.util.List;

public record FeedTopicsUpdateRequest(
        List<FeedTopicUpdateRequest> topics
) {
}
