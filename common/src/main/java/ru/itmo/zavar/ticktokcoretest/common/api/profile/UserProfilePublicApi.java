package ru.itmo.zavar.ticktokcoretest.common.api.profile;

import java.util.Optional;

public interface UserProfilePublicApi {

    Optional<UserSummary> findUserSummary(Long userId);
}
