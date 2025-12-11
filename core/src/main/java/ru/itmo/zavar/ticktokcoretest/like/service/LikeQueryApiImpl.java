package ru.itmo.zavar.ticktokcoretest.like.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.common.api.like.LikeQueryApi;
import ru.itmo.zavar.ticktokcoretest.like.repo.VideoLikeRepository;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeQueryApiImpl implements LikeQueryApi {

    private final VideoLikeRepository videoLikeRepository;

    @Override
    public List<Long> findLikedVideoIds(Long userId, Collection<Long> videoIds) {
        if (userId == null || videoIds == null || videoIds.isEmpty()) {
            return List.of();
        }
        return this.videoLikeRepository.findLikedVideoIdsByUserIdAndVideoIdIn(userId, videoIds);
    }
}
