package ru.itmo.zavar.brainstop.profile.api;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileAdminApi;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.api.profile.UserAccountDto;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.profile.client.AuthInternalClient;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileAdminApiImpl implements ProfileAdminApi {

    private final AuthInternalClient authInternalClient;

    @Override
    public List<ProfileUserDetails> findAllUsers() {
        return this.authInternalClient.listUsers().stream()
                .map(this::toDetails)
                .toList();
    }

    @Override
    public Optional<ProfileUserDetails> findUser(Long id) {
        try {
            return Optional.of(toDetails(this.authInternalClient.getUser(id)));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(Long id) {
        try {
            return Boolean.TRUE.equals(this.authInternalClient.exists(id));
        } catch (FeignException e) {
            return false;
        }
    }

    @Override
    public ProfileUserDetails setEnabled(Long id, boolean enabled) {
        try {
            UserAccountDto updated = this.authInternalClient.setEnabled(id, enabled);
            return toDetails(updated);
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("User not found: " + id);
        }
    }

    @Override
    public ProfileUserDetails updateRoles(Long id, Set<Role> roles) {
        try {
            UserAccountDto updated = this.authInternalClient.updateRoles(id, roles);
            return toDetails(updated);
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("User not found: " + id);
        }
    }


    private ProfileUserDetails toDetails(UserAccountDto user) {
        if (user == null) {
            return null;
        }
        String avatarUrl = user.avatarKey() == null ? null : "/api/users/" + user.id() + "/avatar";
        return new ProfileUserDetails(
                user.id(),
                user.username(),
                user.displayName(),
                user.email(),
                user.enabled(),
                user.roles(),
                user.createdAt(),
                avatarUrl
        );
    }
}
