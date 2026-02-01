package ru.itmo.zavar.brainstop.common.api.profile.auth;

import java.util.Optional;

public interface AuthProfileApi {

    Optional<AuthUser> findByUsername(String username);

    Optional<AuthUser> findById(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    AuthUser save(AuthUser user);
}
