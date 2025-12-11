package ru.itmo.zavar.ticktokcoretest.profile.api;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.FollowQueryApi;
import ru.itmo.zavar.ticktokcoretest.profile.service.FollowService;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class FollowQueryApiImpl implements FollowQueryApi {

    private final FollowService followService;

    @Override
    public Set<Long> getFollowingWithin(Long followerId, Set<Long> candidateIds) {
        return this.followService.getFollowingWithin(followerId, candidateIds);
    }
}
