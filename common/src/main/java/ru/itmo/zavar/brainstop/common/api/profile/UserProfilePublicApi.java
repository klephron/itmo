package ru.itmo.zavar.brainstop.common.api.profile;

import java.util.Optional;

public interface UserProfilePublicApi {

    Optional<UserSummaryDto> findUserSummary(Long userId);
}
