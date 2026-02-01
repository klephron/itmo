package ru.itmo.zavar.brainstop.like.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoIdsRequest;
import ru.itmo.zavar.brainstop.common.api.like.LikeQueryApi;
import ru.itmo.zavar.brainstop.interactions.client.InteractionsInternalClient;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeQueryApiImpl implements LikeQueryApi {

    private final InteractionsInternalClient interactionsInternalClient;

    @Override
    public List<Long> findLikedVideoIds(Long userId, Collection<Long> videoIds) {
        if (userId == null || videoIds == null || videoIds.isEmpty()) {
            return List.of();
        }
        List<Long> liked = interactionsInternalClient.getLikedVideoIds(new VideoIdsRequest(userId, videoIds));
        return liked != null ? liked : List.of();
    }
}
