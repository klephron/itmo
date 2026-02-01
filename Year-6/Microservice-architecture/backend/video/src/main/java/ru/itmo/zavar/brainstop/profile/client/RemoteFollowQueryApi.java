package ru.itmo.zavar.brainstop.profile.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.profile.FollowQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.dto.FollowCheckRequest;

import java.util.Collections;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RemoteFollowQueryApi implements FollowQueryApi {

    private final ProfileInternalClient profileInternalClient;

    @Override
    public Set<Long> getFollowingWithin(Long followerId, Set<Long> candidateIds) {
        if (followerId == null || candidateIds == null || candidateIds.isEmpty()) {
            return Collections.emptySet();
        }
        FollowCheckRequest request = new FollowCheckRequest(followerId, candidateIds);
        return this.profileInternalClient.checkFollowing(request);
    }
}
