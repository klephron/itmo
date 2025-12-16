package ru.itmo.zavar.brainstop.profile.client;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileAdminApi;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RemoteProfileAdminApi implements ProfileAdminApi {

    private final ProfileInternalClient profileInternalClient;

    @Override
    public List<ProfileUserDetails> findAllUsers() {
        return this.profileInternalClient.findAllUsers();
    }

    @Override
    public Optional<ProfileUserDetails> findUser(Long id) {
        try {
            return Optional.ofNullable(this.profileInternalClient.findUser(id));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(Long id) {
        Boolean exists = this.profileInternalClient.exists(id);
        return exists != null && exists;
    }

    @Override
    public ProfileUserDetails setEnabled(Long id, boolean enabled) {
        return this.profileInternalClient.setEnabled(id, enabled);
    }

    @Override
    public ProfileUserDetails updateRoles(Long id, Set<Role> roles) {
        return this.profileInternalClient.updateRoles(id, roles);
    }
}
