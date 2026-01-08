package ru.itmo.zavar.brainstop.profile.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.profile.UserAccountDto;
import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class AuthInternalClientFallbackFactory implements FallbackFactory<AuthInternalClient> {

    @Override
    public AuthInternalClient create(Throwable cause) {
        log.warn("AuthInternalClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return new AuthInternalClient() {
            @Override
            public UserAccountDto getUser(Long id) {
                return null;
            }

            @Override
            public Boolean exists(Long id) {
                return null;
            }

            @Override
            public List<UserAccountDto> listUsers() {
                return List.of();
            }

            @Override
            public List<UserAccountDto> getUsers(List<Long> ids) {
                return List.of();
            }

            @Override
            public UserAccountDto updateAvatar(Long id, String avatarKey) {
                return null;
            }

            @Override
            public UserAccountDto setEnabled(Long id, boolean enabled) {
                return null;
            }

            @Override
            public UserAccountDto updateRoles(Long id, Set<Role> roles) {
                return null;
            }
        };
    }
}
