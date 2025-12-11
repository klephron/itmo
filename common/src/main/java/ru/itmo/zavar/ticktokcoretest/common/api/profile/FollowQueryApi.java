package ru.itmo.zavar.ticktokcoretest.common.api.profile;

import java.util.Set;

public interface FollowQueryApi {

    Set<Long> getFollowingWithin(Long followerId, Set<Long> candidateIds);
}
