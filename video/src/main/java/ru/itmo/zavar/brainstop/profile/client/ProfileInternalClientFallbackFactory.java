package ru.itmo.zavar.brainstop.profile.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.api.profile.dto.FollowCheckRequest;
import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class ProfileInternalClientFallbackFactory implements FallbackFactory<ProfileInternalClient> {

    @Override
    public ProfileInternalClient create(Throwable cause) {
        log.warn("ProfileInternalClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return new ProfileInternalClient() {
            @Override
            public UserSummaryDto getSummary(Long id) {
                return null;
            }

            @Override
            public Set<Long> checkFollowing(FollowCheckRequest request) {
                return Set.of();
            }

            @Override
            public List<ProfileUserDetails> findAllUsers() {
                return List.of();
            }

            @Override
            public ProfileUserDetails findUser(Long id) {
                return null;
            }

            @Override
            public Boolean exists(Long id) {
                return null;
            }

            @Override
            public ProfileUserDetails setEnabled(Long id, boolean enabled) {
                return null;
            }

            @Override
            public ProfileUserDetails updateRoles(Long id, Set<Role> roles) {
                return null;
            }
        };
    }
}
