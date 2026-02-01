package ru.itmopai.backend.repository;

import java.time.Instant;
import java.util.List;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.NotificationEntity;

@Repository
public interface NotificationRepository extends CrudRepository<NotificationEntity, Long> {
  List<NotificationEntity> findAllByReadAtIsNotNullAndCreatedAtBetween(@NotNull Instant from,
      @NotNull Instant to);

  List<NotificationEntity> findAllByReadAtIsNullAndCreatedAtBetween(@NotNull Instant from,
      @NotNull Instant to);

  List<NotificationEntity> findAllByCreatedAtBetween(@NotNull Instant from,
      @NotNull Instant to);

  List<NotificationEntity> findAllByReadAtIsNotNullAndCreatedAtGreaterThanEquals(
      @NotNull Instant from);

  List<NotificationEntity> findAllByReadAtIsNotNullAndCreatedAtLessThanEquals(
      @NotNull Instant to);

  List<NotificationEntity> findAllByReadAtIsNotNull();

  List<NotificationEntity> findAllByReadAtIsNullAndCreatedAtGreaterThanEquals(
      @NotNull Instant from);

  List<NotificationEntity> findAllByReadAtIsNullAndCreatedAtLessThanEquals(
      @NotNull Instant to);

  List<NotificationEntity> findAllByReadAtIsNull();

  List<NotificationEntity> findAllByCreatedAtGreaterThanEquals(@NotNull Instant from);

  List<NotificationEntity> findAllByCreatedAtLessThanEquals(@NotNull Instant to);

}
