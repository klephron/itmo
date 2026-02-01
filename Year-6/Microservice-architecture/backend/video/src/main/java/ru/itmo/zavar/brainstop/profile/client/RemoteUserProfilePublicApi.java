package ru.itmo.zavar.brainstop.profile.client;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RemoteUserProfilePublicApi implements UserProfilePublicApi {

    private final ProfileInternalClient profileInternalClient;

    @Override
    public Optional<UserSummaryDto> findUserSummary(Long userId) {
        try {
            return Optional.ofNullable(this.profileInternalClient.getSummary(userId));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        } catch (NoFallbackAvailableException | CallNotPermittedException e) {
            log.warn("Profile service unavailable (circuit open?) for user {}: {}", userId, e.getClass().getSimpleName());
            return Optional.empty();
        } catch (FeignException e) {
            log.warn("Profile service returned error for user {} (status {}): {}", userId, e.status(), e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to fetch profile summary for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }
}
