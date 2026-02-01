package ru.itmo.zavar.brainstop.common.api.profile;

import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProfileAdminApi {

    List<ProfileUserDetails> findAllUsers();

    Optional<ProfileUserDetails> findUser(Long id);

    boolean existsById(Long id);

    ProfileUserDetails setEnabled(Long id, boolean enabled);

    ProfileUserDetails updateRoles(Long id, Set<Role> roles);
}
