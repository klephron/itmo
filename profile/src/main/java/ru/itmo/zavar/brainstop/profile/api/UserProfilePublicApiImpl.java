package ru.itmo.zavar.brainstop.profile.api;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.profile.UserAccountDto;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.profile.cache.UserSummaryCache;
import ru.itmo.zavar.brainstop.profile.client.AuthInternalClient;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProfilePublicApiImpl implements UserProfilePublicApi {

    private final AuthInternalClient authInternalClient;
    private final UserSummaryCache userSummaryCache;

    @Override
    public Optional<UserSummaryDto> findUserSummary(Long userId) {
        Optional<UserSummaryDto> cached = userSummaryCache.get(userId);
        if (cached.isPresent()) {
            return cached;
        }

        UserAccountDto account;
        try {
            account = this.authInternalClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }

        UserSummaryDto summary = new UserSummaryDto(
                account.id(),
                account.displayName(),
                account.avatarKey() == null
                        ? null
                        : "/api/users/" + account.id() + "/avatar"
        );

        userSummaryCache.put(summary);
        return Optional.of(summary);
    }
}
