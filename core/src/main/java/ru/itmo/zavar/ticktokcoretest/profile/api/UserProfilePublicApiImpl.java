package ru.itmo.zavar.ticktokcoretest.profile.api;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.UserSummary;
import ru.itmo.zavar.ticktokcoretest.profile.repo.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProfilePublicApiImpl implements UserProfilePublicApi {

    private final UserRepository userRepository;

    @Override
    public Optional<UserSummary> findUserSummary(Long userId) {
        return this.userRepository.findById(userId)
                .map(u -> new UserSummary(
                        u.getId(),
                        u.getDisplayName(),
                        u.getAvatarKey() == null
                                ? null
                                : "/api/users/" + u.getId() + "/avatar"
                ));
    }
}
