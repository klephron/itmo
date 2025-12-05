package ru.itmo.zavar.ticktokcoretest.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.ticktokcoretest.entity.UserFollowEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollowEntity, Long> {

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    Optional<UserFollowEntity> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    long countByFolloweeId(Long followeeId);   // сколько подписчиков

    long countByFollowerId(Long followerId);   // сколько подписок

    List<UserFollowEntity> findByFollowerId(Long followerId); // на кого подписан

    List<UserFollowEntity> findByFolloweeId(Long followeeId); // кто подписан

    List<UserFollowEntity> findByFollowerIdAndFolloweeIdIn(Long followerId, Collection<Long> followeeIds);
}
