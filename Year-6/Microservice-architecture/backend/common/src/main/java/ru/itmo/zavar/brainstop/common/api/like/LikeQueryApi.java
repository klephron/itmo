package ru.itmo.zavar.brainstop.common.api.like;

import java.util.Collection;
import java.util.List;

public interface LikeQueryApi {

    List<Long> findLikedVideoIds(Long userId, Collection<Long> videoIds);
}
