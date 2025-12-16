package ru.itmo.zavar.brainstop.profile.client;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummary;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RemoteUserProfilePublicApi implements UserProfilePublicApi {

    private final ProfileInternalClient profileInternalClient;

    @Override
    public Optional<UserSummary> findUserSummary(Long userId) {
        try {
            return Optional.ofNullable(this.profileInternalClient.getSummary(userId));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }
}
