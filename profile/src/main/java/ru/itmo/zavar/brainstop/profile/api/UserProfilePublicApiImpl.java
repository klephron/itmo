package ru.itmo.zavar.brainstop.profile.api;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.profile.repo.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProfilePublicApiImpl implements UserProfilePublicApi {

    private final UserRepository userRepository;

    @Override
    public Optional<UserSummaryDto> findUserSummary(Long userId) {
        return this.userRepository.findById(userId)
                .map(u -> new UserSummaryDto(
                        u.getId(),
                        u.getDisplayName(),
                        u.getAvatarKey() == null
                                ? null
                                : "/api/users/" + u.getId() + "/avatar"
                ));
    }
}
