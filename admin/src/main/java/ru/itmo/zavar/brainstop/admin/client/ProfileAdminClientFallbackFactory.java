package ru.itmo.zavar.brainstop.admin.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class ProfileAdminClientFallbackFactory implements FallbackFactory<ProfileAdminClient> {

    @Override
    public ProfileAdminClient create(Throwable cause) {
        log.warn("ProfileAdminClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return new ProfileAdminClient() {
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
