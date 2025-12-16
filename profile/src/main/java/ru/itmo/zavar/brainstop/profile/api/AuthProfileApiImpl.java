package ru.itmo.zavar.brainstop.profile.api;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.profile.auth.AuthProfileApi;
import ru.itmo.zavar.brainstop.common.api.profile.auth.AuthUser;
import ru.itmo.zavar.brainstop.profile.mapper.UserProfileMapper;
import ru.itmo.zavar.brainstop.profile.model.UserEntity;
import ru.itmo.zavar.brainstop.profile.repo.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthProfileApiImpl implements AuthProfileApi {

    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        return this.userRepository.findByUsername(username)
                .map(this.userProfileMapper::toAuthUser);
    }

    @Override
    public Optional<AuthUser> findById(Long id) {
        return this.userRepository.findById(id)
                .map(this.userProfileMapper::toAuthUser);
    }

    @Override
    public boolean existsByUsername(String username) {
        return this.userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return this.userRepository.existsByEmail(email);
    }

    @Override
    public AuthUser save(AuthUser user) {
        UserEntity entity = this.userProfileMapper.toEntity(user);
        UserEntity saved = this.userRepository.save(entity);
        return this.userProfileMapper.toAuthUser(saved);
    }
}
