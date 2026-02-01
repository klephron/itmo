package ru.itmo.zavar.brainstop.video.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.brainstop.video.model.UserFeedTopicEntity;

import java.util.List;

public interface UserFeedTopicRepository extends JpaRepository<UserFeedTopicEntity, Long> {

    List<UserFeedTopicEntity> findByUserIdOrderByCreatedAtAsc(Long userId);

    void deleteByUserId(Long userId);
}
