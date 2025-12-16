package ru.itmo.zavar.brainstop.profile.api;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileAdminApi;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.profile.mapper.UserProfileMapper;
import ru.itmo.zavar.brainstop.profile.model.UserEntity;
import ru.itmo.zavar.brainstop.profile.repo.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileAdminApiImpl implements ProfileAdminApi {

    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;

    @Override
    public List<ProfileUserDetails> findAllUsers() {
        return this.userRepository.findAll()
                .stream()
                .map(this.userProfileMapper::toProfileUserDetails)
                .toList();
    }

    @Override
    public Optional<ProfileUserDetails> findUser(Long id) {
        return this.userRepository.findById(id)
                .map(this.userProfileMapper::toProfileUserDetails);
    }

    @Override
    public boolean existsById(Long id) {
        return this.userRepository.existsById(id);
    }

    @Override
    public ProfileUserDetails setEnabled(Long id, boolean enabled) {
        UserEntity user = this.userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setEnabled(enabled);
        return this.userProfileMapper.toProfileUserDetails(this.userRepository.save(user));
    }

    @Override
    public ProfileUserDetails updateRoles(Long id, Set<Role> roles) {
        UserEntity user = this.userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.getRoles().clear();
        if (roles != null && !roles.isEmpty()) {
            user.getRoles().addAll(roles);
        } else {
            user.getRoles().add(Role.USER);
        }
        return this.userProfileMapper.toProfileUserDetails(this.userRepository.save(user));
    }
}
