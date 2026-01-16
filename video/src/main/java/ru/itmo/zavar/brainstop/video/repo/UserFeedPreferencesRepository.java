package ru.itmo.zavar.brainstop.video.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.brainstop.video.model.UserFeedPreferencesEntity;

public interface UserFeedPreferencesRepository extends JpaRepository<UserFeedPreferencesEntity, Long> {
}
